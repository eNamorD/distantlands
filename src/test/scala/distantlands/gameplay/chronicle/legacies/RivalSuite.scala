package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** Rival (L17), its Throne half: "In Shape Empire, you may prevent any
  * number of denizens from being discarded." (ruling 9). p1, the
  * Chancellor, holds the Throne and the Empire rules Ancient City; two
  * denizens and a ruined edifice lie at sites it does not rule. */
class RivalSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def outside: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1)
    .denizen(ThreateningRoarCard, at = BrokenPeaksSite)
    .denizen(FaeMerchantCard, at = BuriedGiantSite)
    .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = AncientCitySite)

  private def throne(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def keeping(refs: DecisionOptionRef*)
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Rival.decisionId(p2) =>
      DecisionAnswer.ChooseManyAnswer(refs.toVector)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.throneNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone),
      keys, run.events).filter(said => keys.exists(_.name == said.key))
      .map(_.text)

  test("Rival is implemented"):
    assert(PowerImplementationStatus.implemented(RivalCard.power.id))

  test("the holder is offered every card Shape Empire would discard"):
    // Ancient City is Enduring, so its ruined edifice stays anyway.
    val run = throne(outside.legacy(p2, RivalCard))
    val asked = run.asked.find(_.decisionId == Rival.decisionId(p2)).get
    val query = asked.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((asked.owner, query.min, query.max), (p2, 0, 2))
    assertEquals(query.options.map(_.ref).toSet, Set[DecisionOptionRef](
      DecisionOptionRef.Denizen(ThreateningRoarCard.id),
      DecisionOptionRef.Denizen(FaeMerchantCard.id)))

  test("the kept denizens stay and the rest are discarded as before " +
      "(ruling 9)"):
    val run = throne(outside.legacy(p2, RivalCard),
      keeping(DecisionOptionRef.Denizen(FaeMerchantCard.id)))
    assert(onMap(run.ready, FaeMerchantCard.id))
    assert(!onMap(run.ready, ThreateningRoarCard.id))
    assertEquals(lines(run), Vector(
      s"p2 protected 1 denizen with ${RivalCard.id.value}"))

  test("keeping none discards everything and writes no line"):
    val run = throne(outside.legacy(p2, RivalCard))
    assert(!onMap(run.ready, FaeMerchantCard.id))
    assertEquals(lines(run), Vector.empty)

  test("a ruined edifice outside the Empire may be kept"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = BuriedGiantSite)
      .legacy(p2, RivalCard)
    val run = throne(table,
      keeping(DecisionOptionRef.Edifice(SchoolOfVinesCard.id)))
    assert(onMap(run.ready, SchoolOfVinesCard.id))

  test("nothing to discard asks nothing, and a dormant Rival asks nothing " +
      "(ruling 1)"):
    val empire = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
    assert(!throne(empire.legacy(p2, RivalCard)).asked.exists(
      _.decisionId == Rival.decisionId(p2)))
    assert(!throne(outside.legacy(p2, RivalCard, active = false)).asked
      .exists(_.decisionId == Rival.decisionId(p2)))

  test("a walk resumed at Rival's question keeps what was chosen, after " +
      "the warband removal changed the map"):
    // Two Imperial warbands: Shape Empire first asks p1 to remove some.
    val table = outside.warbandsAt(BrokenPeaksSite, p1, 1)
      .legacy(p2, RivalCard)
    val removing: PartialFunction[Decide, DecisionAnswer] =
      case decide if decide.decisionId ==
          distantlands.gameplay.chronicle.ShapeEmpire.removeDecisionId =>
        DecisionAnswer.DistributeAnswer(Vector(
          DistributeAmount(DecisionOptionRef.Site(BrokenPeaksSite.id), 1),
          DistributeAmount(DecisionOptionRef.Site(AncientCitySite.id), 0)))
    val run = throne(table, removing.orElse(
      keeping(DecisionOptionRef.Denizen(ThreateningRoarCard.id))))
    assert(onMap(run.ready, ThreateningRoarCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))
