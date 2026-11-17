package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.setup.{AtlasPlacement, GameStartRules,
  ImperialMaps, LaterGameFixture, SetupLegacyFixture, SetupProcedure,
  SetupWalkDriver}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** World Crafter at Imperial Maps: "you fill site slots, using any mix of
  * Recent and Forgotten Sites (after resolving the Rival legacy)" (spec
  * ruling 10; R11-R13). l1 is the Chancellor, l2 a Citizen, l3 an Exile
  * holding World Crafter; the Empire holds two sites and the Atlas six. */
class WorldCrafterSetupSuite extends munit.FunSuite:
  private val (l1, l2, l3) = (LineageId("l1"), LineageId("l2"),
    LineageId("l3"))
  private val (p1, p3) = (PlayerId("p1"), PlayerId("p3"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
    WorldCrafterCard.id -> true)
  private val atlas = record.atlasBox.map(_.site)

  /** Slot 2 from the Forgotten end, every other slot from the Recent end. */
  private val mixing: Answers =
    case park if park.decisionId == WorldCrafter.slotDecisionId(p3, 2) =>
      ChooseOneAnswer(AtlasPlacement.endOption(AtlasEnd.Forgotten))

  private def woken(answers: Answers): Situation =
    Situation.wake(Situation.rules(catalog).withAnswers(answers), record,
      LaterGameFixture.orders(record, p1))

  private def lines(situation: Situation): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, situation.events).map(_.text)

  /** The decision `situation` is parked on. */
  private def parkedDecide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog)), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  /** Every decision Setup asks, each answered by default. */
  private def asked: Vector[Decide] =
    val ready = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    SetupWalkDriver.driveWithEvents(ready, accepted(SetupProcedure.build(
      catalog, ready, ready.game.current.turn.activePlayer, Vector.empty)),
      TriggeredProcedureRef.Setup, WalkerPowers.empty).asked

  test("the holder fills each slot from the end chosen for it, an Exile " +
      "in the Chancellor's place"):
    val map = woken(mixing).ready.game.current.map
    // Slot 1 Recent, slot 2 Forgotten (the Atlas's last site), then Recent.
    assertEquals(map.inPlay.drop(2), Vector(atlas(0), atlas(5), atlas(1),
      atlas(2), atlas(3), atlas(4)))

  test("each slot is its own question for the holder while two or more " +
      "Atlas sites remain (R12)"):
    val crafter = asked.filter(_.decisionId.startsWith("setup.legacy.world"))
    assertEquals(crafter.map(_.decisionId),
      (1 to 5).toVector.map(WorldCrafter.slotDecisionId(p3, _)))
    assert(crafter.forall(_.owner == p3), crafter)
    assert(!asked.exists(_.decisionId == ImperialMaps.decisionId), asked)

  test("one line per slot names the site and its end (R11)"):
    val said = lines(woken(mixing))
    assertEquals(said.filter(_.contains(WorldCrafterCard.id.value)),
      Vector(atlas(0), atlas(5), atlas(1), atlas(2), atlas(3), atlas(4))
        .zip(Vector("Recent", "Forgotten", "Recent", "Recent", "Recent",
          "Recent")).map((site, end) => s"p3 placed ${site.value} from " +
          s"$end Sites with ${WorldCrafterCard.id.value}"))
    assert(!said.exists(_.contains("filled the map from")), said)
    assert(!said.exists(_.endsWith("on the map")), said)

  test("the Recent option names the Atlas's front site when asked"):
    val parked = Situation.start(Situation.rules(catalog).withAnswers(mixing))
      .parkedAt(WorldCrafter.slotDecisionId(p3, 3))(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    val front = catalog.site(atlas(1)).get.name
    assertEquals(parked.ready.game.current.map.inPlay.size, 4)
    parkedDecide(parked).query match
      case one: DecisionQuery.ChooseOne => assertEquals(
        one.options.collect { case DecisionOption.Button(_, label) => label },
        Vector(s"Recent Sites ($front first)", "Forgotten Sites"))
      case other => fail(s"expected a choose-one, got $other")

  test("a walk resumed at a slot's question places the earlier slots once " +
      "and asks the next slot next"):
    val parked = Situation.start(Situation.rules(catalog).withAnswers(mixing))
      .parkedAt(WorldCrafter.slotDecisionId(p3, 2))(GameCommand.Begin(record,
        LaterGameFixture.orders(record, p1)))
    assertEquals(parked.ready.game.current.map.inPlay.size, 3)
    val next = parked.parkedAt(WorldCrafter.slotDecisionId(p3, 3))()
    assertEquals(next.ready.game.current.map.inPlay.drop(2),
      Vector(atlas(0), atlas(5)))

  test("after Rival's sites the holder fills only the slots left (card: " +
      "\"after resolving the Rival legacy\")"):
    val both = SetupLegacyFixture.holding(record, l3, Role.Exile,
      WorldCrafterCard.id -> true,
      distantlands.gameplay.chronicle.legacies.RivalCard.id -> true)
    val map = Situation.wake(Situation.rules(catalog), both,
      LaterGameFixture.orders(both, p1)).ready.game.current.map
    assertEquals(map.inPlay.size, MapState.slots)
    assertEquals(map.inPlay.slice(2, 5), atlas.take(3))

  test("a Chancellor holding World Crafter fills slot by slot (R13)"):
    val chancellor = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l1, Role.Chancellor,
      WorldCrafterCard.id -> true)
    val ready = accepted(GameStartRules.evolve(catalog, chancellor,
      LaterGameFixture.orders(chancellor, p1)))
    val asked = SetupWalkDriver.driveWithEvents(ready, accepted(
      SetupProcedure.build(catalog, ready, ready.game.current.turn
        .activePlayer, Vector.empty)), TriggeredProcedureRef.Setup,
      WalkerPowers.empty).asked.map(_.decisionId)
    assertEquals(asked.filter(_.startsWith("setup.legacy.world")),
      (1 to 5).toVector.map(WorldCrafter.slotDecisionId(p1, _)))
    assert(!asked.contains(ImperialMaps.decisionId), asked)

  test("a dormant World Crafter leaves Imperial Maps to the Chancellor " +
      "(ruling 1)"):
    val dormant = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      WorldCrafterCard.id -> false)
    val situation = Situation.wake(Situation.rules(catalog), dormant,
      LaterGameFixture.orders(dormant, p1))
    assert(lines(situation).contains("p1 filled the map from Recent Sites"))

  test("World Crafter is registered among the setup legacies"):
    assert(LegacySetupPowers.all.contains(WorldCrafter))
