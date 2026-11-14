package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.ChronicleAccounting
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p3}

/** Revolutionary (L15): "After Build Edifice, you may discard any number
  * of denizens at Imperial sites." (ruling 11). p1, the Chancellor, holds
  * the Throne; the Empire rules Ancient City and Broken Peaks; p3, an
  * Exile, holds Revolutionary. */
class RevolutionarySuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def empire: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BrokenPeaksSite, p1, 1)
    .denizen(ThreateningRoarCard, at = AncientCitySite)
    .denizen(FaeMerchantCard, at = BrokenPeaksSite)
    .edifice(GreatMarketCard, EdificeSide.Intact, at = AncientCitySite)
    .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = AncientCitySite)
    .legacy(p3, RevolutionaryCard)

  private def throne(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def discarding(refs: DecisionOptionRef*)
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Revolutionary.decisionId(p3) =>
      DecisionAnswer.ChooseManyAnswer(refs.toVector)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.throneNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone),
      keys, run.events).filter(said => keys.exists(_.name == said.key))
      .map(_.text)

  test("Revolutionary is implemented"):
    assert(PowerImplementationStatus.implemented(RevolutionaryCard.power.id))

  test("the holder is offered the denizens and ruined edifices at Imperial " +
      "sites, after Build Edifice"):
    val run = throne(empire)
    val asked = run.asked.find(_.decisionId == Revolutionary.decisionId(p3)).get
    assertEquals(asked.owner, p3)
    assertEquals(asked.query.asInstanceOf[DecisionQuery.ChooseMany].options
      .map(_.ref).toSet, Set[DecisionOptionRef](
        DecisionOptionRef.Denizen(ThreateningRoarCard.id),
        DecisionOptionRef.Denizen(FaeMerchantCard.id),
        DecisionOptionRef.Edifice(SchoolOfVinesCard.id)))
    val order = run.asked.map(_.decisionId)
    assert(order.lastIndexWhere(_.startsWith("chronicle.build-edifice.")) <
      order.indexOf(Revolutionary.decisionId(p3)))

  test("choosing none discards nothing and writes no line"):
    val run = throne(empire)
    assert(onMap(run.ready, ThreateningRoarCard.id) &&
      onMap(run.ready, FaeMerchantCard.id))
    assertEquals(lines(run), Vector.empty)

  test("several chosen are discarded where Shape Empire sends them " +
      "(ruling 11)"):
    val run = throne(empire, discarding(
      DecisionOptionRef.Denizen(ThreateningRoarCard.id),
      DecisionOptionRef.Denizen(FaeMerchantCard.id),
      DecisionOptionRef.Edifice(SchoolOfVinesCard.id)))
    Vector(ThreateningRoarCard.id, FaeMerchantCard.id, SchoolOfVinesCard.id)
      .foreach(card => assert(!onMap(run.ready, card), card))
    assert(onMap(run.ready, GreatMarketCard.id))
    val discards = run.ready.game.current.commonCards.regionalDiscards
      .values.flatten.toSet
    assert(discards.contains(ThreateningRoarCard.id) &&
      discards.contains(FaeMerchantCard.id))
    // The ruined edifice returns to the edifice deck, so nothing is left
    // set aside after Build Edifice (R16).
    assert(run.ready.game.current.commonCards.edificeDeck
      .contains(SchoolOfVinesCard.id))
    assertEquals(run.ready.game.current.setAsideEdifices, Vector.empty)
    assertEquals(lines(run), Vector(
      s"p3 discarded 3 denizens with ${RevolutionaryCard.id.value}"))

  test("a ruined edifice discarded after Build Edifice leaves a stored " +
      "Chronicle that passes game start's card accounting (R14)"):
    // The whole Chronicle, from p1's win: p1 rules Ancient City, so the
    // crowned Empire holds its ruined edifice; p3 holds the People's Favor,
    // so keeps Revolutionary active through the Sun.
    val table = ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 1)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = AncientCitySite)
      .peoplesFavor(Some(p3), 1).legacy(p3, RevolutionaryCard)
    val run = ChronicleWalk.through(rules, table,
      TriggeredProcedureRef.ChronicleFinish,
      discarding(DecisionOptionRef.Edifice(SchoolOfVinesCard.id)))
    assert(!onMap(run.ready, SchoolOfVinesCard.id))
    val record = run.ready.game.campaign.evolved.get
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, record),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(
      run.ready.game.campaign.baseline, record), Right(()))

  test("a dormant Revolutionary asks nothing (ruling 1)"):
    val run = throne(empire.update(_.updateCampaign(campaign =>
      campaign.copy(lineages = campaign.lineages.view.mapValues(lineage =>
        lineage.copy(legacies = lineage.legacies.map(_.copy(active = false))))
        .toMap))))
    assert(!run.asked.exists(_.decisionId == Revolutionary.decisionId(p3)))

  test("a walk resumed at Revolutionary's question after a build applies " +
      "the answer"):
    // Build Edifice first buries Threatening Roar for its suit's edifice.
    val suit = catalog.denizen(ThreateningRoarCard.id).get.suit
    val building: PartialFunction[Decide, DecisionAnswer] =
      case decide if decide.decisionId ==
          distantlands.gameplay.chronicle.BuildEdifice.decisionId(suit) =>
        DecisionAnswer.ChooseManyAnswer(Vector(
          DecisionOptionRef.Denizen(ThreateningRoarCard.id)))
    val run = throne(empire, building.orElse(
      discarding(DecisionOptionRef.Denizen(FaeMerchantCard.id))))
    assert(!onMap(run.ready, FaeMerchantCard.id))
    val asked = run.asked.find(_.decisionId == Revolutionary.decisionId(p3)).get
    assert(!asked.query.asInstanceOf[DecisionQuery.ChooseMany].options
      .exists(_.ref == DecisionOptionRef.Denizen(ThreateningRoarCard.id)))
