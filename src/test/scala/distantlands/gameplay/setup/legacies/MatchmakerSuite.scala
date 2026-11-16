package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus}
import distantlands.gameplay.setup.{GameStartRules, LaterGameFixture,
  LegacyDeal, LegacyKeep, SetupLegacyFixture, SetupProcedure,
  SetupWalkDriver}
import distantlands.gameplay.walker.WalkerPowers
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Matchmaker in Quiet Ambitions: "Whenever players draw legacies from the
  * deck and choose one to keep, you draw and choose for them instead."
  * (spec ruling 13). l3, an Exile, holds it. */
class MatchmakerSuite extends munit.FunSuite:
  private val (l1, l2, l3) = (LineageId("l1"), LineageId("l2"),
    LineageId("l3"))
  private val (p1, p2, p3) = (PlayerId("p1"), PlayerId("p2"),
    PlayerId("p3"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
    MatchmakerCard.id -> true)
  private val orders = LaterGameFixture.orders(record, p1)
  private def drawn(player: PlayerId): Vector[LegacyId] =
    SetupLegacyFixture.drawn(orders, player)

  private def parkedAt(decisionId: String): Situation =
    Situation.start(Situation.rules(catalog)).parkedAt(decisionId)(
      GameCommand.Begin(record, orders))

  private def owner(situation: Situation): PlayerId =
    present(accepted(ParkedNode.of(situation.state, catalog)), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide.owner
      case other => fail(s"expected the keep, parked on $other")

  test("the holder answers every other player's keep, under its own id"):
    Vector(p1, p2).foreach(player => assertEquals(
      owner(parkedAt(LegacyKeep.decisionId(player))), p3))

  test("the holder keeps their own draw"):
    assertEquals(owner(parkedAt(LegacyKeep.decisionId(p3))), p3)

  test("the holder answers every keep in Foundation III, before any pawn " +
      "(catalog batch 13, ruling 3)"):
    val at = parkedAt(LegacyKeep.decisionId(p2))
    assertEquals(owner(at), p3)
    Vector(p1, p2, p3).foreach(player =>
      assertEquals(at.ready.game.current.pawnSite(player), None, player))
    assert(at.ready.game.campaign.lineages(l1).legacies.exists(!_.active),
      "p1's keep came first")

  test("a walk resumed at the keep keeps the holder's choice for its owner"):
    val kept = drawn(p2).last
    val done = parkedAt(LegacyKeep.decisionId(p2)).withAnswers {
      case park if park.decisionId == LegacyKeep.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Legacy(kept))
    }.after()
    val legacies = done.ready.game.campaign.lineages(l2).legacies
    assert(legacies.contains(LegacyState(kept, active = false)), legacies)
    assert(!legacies.exists(_.id == drawn(p2).head), legacies)

  test("the line names the keeper and the owner, never the legacy"):
    val woken = Situation.wake(Situation.rules(catalog), record, orders)
    val said = NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, woken.events).map(_.text)
    assert(said.contains(s"p3 kept a legacy for p1 with " +
      MatchmakerCard.id.value), said)
    assert(said.contains(s"p3 kept a legacy for p2 with " +
      MatchmakerCard.id.value), said)
    assert(said.contains("p3 kept a legacy"), said)

  test("a player who drew one legacy keeps it with no question"):
    // Box lineages hold every legacy but Matchmaker and 3 others, so the
    // derived deck is short. Each keep returns one legacy to the bottom, so
    // p1 and p2 draw 2 each and p3 draws the 1 left (Legacies R9; catalog
    // batch 13, ruling 6). Matchmaker's holder p3 answers both questions
    // and keeps their own single legacy unasked. A later game with a Chancellor keeps them; a first game played
    // exile-only would wipe them back into the deck (R40).
    val held = catalog.legacies.map(_.id).filterNot(_ == MatchmakerCard.id)
      .dropRight(3)
    val boxed = held.grouped(12).zipWithIndex.map((ids, n) => LineageState(
      LineageId(s"boxed-$n"), None, Role.Exile,
      ids.map(LegacyState(_, active = true)), Vector.empty)).toVector
    val later = LaterGameFixture.chronicle(l1, l2)
    val short = SetupLegacyFixture.holding(
      later.copy(lineages = later.lineages ++ boxed), l3, Role.Exile,
      MatchmakerCard.id -> true)
    val shortOrders = LaterGameFixture.orders(short, p1).copy(
      legacyOrder = LegacyDeal.deck(catalog, short))
    assertEquals(shortOrders.legacyOrder.size, 3)
    val ready = accepted(GameStartRules.evolve(catalog, short, shortOrders))
    val setup = SetupWalkDriver.driveWithEvents(ready, accepted(
      SetupProcedure.build(catalog, ready, ready.game.current.turn
        .activePlayer, Vector.empty)), TriggeredProcedureRef.Setup,
      WalkerPowers.empty)
    assertEquals(setup.asked.filter(_.decisionId.startsWith(
      "setup.legacy-choice.")).map(decide => decide.decisionId ->
      decide.owner), Vector(LegacyKeep.decisionId(p1) -> p3,
      LegacyKeep.decisionId(p2) -> p3))
    val said = NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, setup.events).map(_.text)
    assert(said.contains("p3 kept a legacy"), said)
    Vector("p1", "p2").foreach(owner => assert(said.contains(
      s"p3 kept a legacy for $owner with ${MatchmakerCard.id.value}"), said))

  test("under Public Ambitions nobody draws, so the power does nothing"):
    val public = record.copy(foundations = record.foundations.updated(
      FoundationNumber.III, FoundationState(FoundationFace.Altered,
        Set.empty)))
    val said = NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, Situation.wake(Situation.rules(catalog),
        public, LaterGameFixture.orders(public, p1)).events).map(_.text)
    assert(!said.exists(_.contains(MatchmakerCard.id.value)), said)

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(Matchmaker))
    assert(PowerImplementationStatus.implemented(MatchmakerCard.power.id))
