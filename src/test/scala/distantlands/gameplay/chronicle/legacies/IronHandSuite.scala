package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.chronicle.{BuildEdifice, ChronicleWalk,
  OfferCitizenship, ShapeEmpire}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.economy.VillageIdiotCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Iron Hand (L01), its Chronicle half: "If you are Imperial, discard Iron
  * Hand. ... Score your legacies in addition to the winner, and perform
  * Shape Empire and Build Edifice (Throne). Your sites are stored in place
  * of the Empire (Stars)." (rulings 18-21). */
class IronHandSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty,
      after: TriggeredProcedureRef = TriggeredProcedureRef.ChronicleBegin)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, procedure, answers, after)

  private def holds(ready: ReadyGame, player: PlayerId): Boolean =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies.exists(_.id == IronHandCard.id)

  private def lines(run: ChronicleWalk.Run, procedure: TriggeredProcedureRef)
      : Vector[String] =
    val keys = IronHand.noteKeys(procedure)
    NoteText.said(ProcedureNotes.id(procedure), keys, run.events)
      .filter(said => keys.exists(_.name == said.key)).map(_.text)

  private val discardLine = s"p3 discarded ${IronHandCard.id.value}"

  test("Iron Hand is implemented"):
    assert(PowerImplementationStatus.implemented(IronHandCard.power.id))

  test("an Imperial holding Iron Hand active discards it at the start of " +
      "the Sun (ruling 18)"):
    val run = through(ChronicleWalk.chronicleWonByP1.imperial(p2, p3)
      .legacy(p3, IronHandCard), TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p3))
    assertEquals(lines(run, TriggeredProcedureRef.ChronicleSun).take(1),
      Vector(discardLine))

  test("a dormant Iron Hand an Imperial holds stays until Flip Legacies " +
      "activates it, then the Crown checkpoint discards it (R29)"):
    // p1, the Chancellor, wins; the Empire rules Ancient City, so the
    // Imperials meet Iron Hand's goal, and p3 stays a Citizen at the Crown.
    val run = through(ChronicleWalk.chronicleWonByP1.imperial(p1, p3)
      .warbandsAt(AncientCitySite, p1, 1)
      .legacy(p3, IronHandCard, active = false),
      TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p3))
    val said = NoteText.said(ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleSun),
      distantlands.gameplay.chronicle.ChronicleSunProcedure.noteKeys,
      run.events).map(_.text)
    assert(said.indexOf(s"p3 activated ${IronHandCard.id.value}") <
      said.indexOf(discardLine), said)

  test("an Exile winner holding Iron Hand discards it after Crown " +
      "Chancellor (ruling 18)"):
    // p1 rules Ancient City alone, so keeps Iron Hand active in step 3.
    val run = through(ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 1).legacy(p1, IronHandCard),
      TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p1))
    assert(lines(run, TriggeredProcedureRef.ChronicleSun).contains(
      s"p1 discarded ${IronHandCard.id.value}"))

  test("an Exile who becomes a Citizen at Offer Citizenship discards Iron " +
      "Hand, and the Empire shapes itself again (ruling 18)"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BuriedGiantSite, p3, 1)
      .legacy(p3, IronHandCard)
    val run = through(table, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == OfferCitizenship.offerDecisionId =>
        DecisionAnswer.ChooseManyAnswer(Vector(DecisionOptionRef.Player(p3)))
      case decide if decide.decisionId ==
          OfferCitizenship.answerDecisionId(p3) =>
        DecisionAnswer.ChooseOneAnswer(OfferCitizenship.accept)
    }, after = TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p3))
    assert(lines(run, TriggeredProcedureRef.ChronicleThrone)
      .contains(discardLine))

  test("a boxed lineage's Iron Hand does nothing (ruling 1)"):
    val boxed = LineageState(LineageId("lineage:boxed"), None, Role.Citizen,
      Vector(LegacyState(IronHandCard.id, active = true)), Vector.empty)
    val table = ChronicleWalk.chronicleWonByP1.update(_.updateCampaign(
      campaign => campaign.copy(baseline = campaign.baseline.copy(
        lineages = Vector(boxed)))))
    val run = through(table, TriggeredProcedureRef.ChronicleSun)
    assertEquals(lines(run, TriggeredProcedureRef.ChronicleSun), Vector.empty)

  /** p1, the Chancellor, holds the Throne, and the Empire rules Ancient
    * City and Deep Woods, which holds Fae Merchant; p3, an Exile, holds
    * Iron Hand and rules Broken Peaks and Buried Giant, which holds
    * Threatening Roar. */
  private def ironThrone: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1).warbandsAt(DeepWoodsSite, p1, 1)
    .warbandsAt(BrokenPeaksSite, p3, 1).warbandsAt(BuriedGiantSite, p3, 1)
    .denizen(FaeMerchantCard, at = DeepWoodsSite)
    .denizen(ThreateningRoarCard, at = BuriedGiantSite)
    .legacy(p3, IronHandCard)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  test("the holder performs Shape Empire over the sites they rule, and the " +
      "Empire's is skipped (ruling 19; R31)"):
    val run = through(ironThrone, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val removal = run.asked.filter(_.decisionId ==
      ShapeEmpire.removeDecisionId)
    assertEquals(removal.map(_.owner), Vector(p3))
    // The holder rules Buried Giant, so Threatening Roar stays; Deep Woods
    // is the Empire's, so its Fae Merchant is discarded.
    assert(onMap(run.ready, ThreateningRoarCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))

  test("the holder performs Build Edifice over the sites they rule " +
      "(ruling 19)"):
    val suit = catalog.denizen(ThreateningRoarCard.id).get.suit
    val run = through(ironThrone, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val build = run.asked.find(_.decisionId == BuildEdifice.decisionId(suit))
      .get
    assertEquals(build.owner, p3)
    assertEquals(build.query.asInstanceOf[DecisionQuery.ChooseMany].options
      .map(_.ref), Vector(DecisionOptionRef.Denizen(ThreateningRoarCard.id)))

  test("an Imperial Great Architect still builds over the Empire's sites, " +
      "then the holder over theirs, under ids of their own (R32)"):
    // p2 is a Citizen holding Great Architect; Village Idiot, at Enduring
    // Ancient City, survives the holder's Shape Empire.
    val table = ironThrone.imperial(p1, p2)
      .denizen(VillageIdiotCard, at = AncientCitySite)
      .legacy(p2, GreatArchitectCard)
    val suit = catalog.denizen(ThreateningRoarCard.id).get.suit
    val other = Suit.all.find(_ != suit).get
    val run = through(table, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == BuildEdifice.decisionId(other) =>
        DecisionAnswer.ChooseManyAnswer(Vector(
          DecisionOptionRef.Denizen(VillageIdiotCard.id)))
    }, after = TriggeredProcedureRef.ChronicleSun)
    val builds = run.asked.filter(_.decisionId.startsWith(
      "chronicle.build-edifice."))
    assert(builds.exists(decide => decide.owner == p2 &&
      decide.decisionId == BuildEdifice.decisionId(other)))
    assert(builds.exists(decide => decide.owner == p3 &&
      decide.decisionId == BuildEdifice.ironHandDecisionId(suit)))
    assertEquals(builds.map(_.decisionId).distinct.size, builds.size)
    // Great Architect built the other suit's edifice: the holder is not
    // offered it.
    assert(!builds.exists(_.decisionId ==
      BuildEdifice.ironHandDecisionId(other)))

  test("a walk resumed at the holder's removal question finds the same " +
      "sites"):
    val started = ChronicleWalk.started(rules, ironThrone,
      TriggeredProcedureRef.ChronicleSun)
    val parked = ChronicleWalk.walkedTo(rules, started,
      ShapeEmpire.removeDecisionId)
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.owner, p3)
    val replayed = distantlands.testkit.Unwrap.accepted(parked.events
      .foldLeft[Either[OathViolation, OathState]](Right(ironThrone.state))(
        (state, event) => state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
