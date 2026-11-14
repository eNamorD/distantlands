package distantlands.gameplay.powers.legacies

import distantlands.gameplay.OathRules
import distantlands.gameplay.powers.{PhasePowerCatalog, WalkerPowerCatalog}
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{QuestToDistantLands,
  QuestToDistantLandsCard}
import distantlands.gameplay.walker.ParkedDecisionAssertions
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import distantlands.model.OathState.Ready
import distantlands.testkit.{Answers, Journal, MenuPlay, Outcomes, ParkedNode,
  Situation, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Conspirator (L26): at their first Wake its holder secretly writes down
  * the round the game will end in and who will win (Legacy in-play powers
  * rulings 1 and 25; R28). */
class ConspiratorPredictionSuite extends munit.FunSuite:
  private val phasePowers = PhasePowerCatalog.default
  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = walkerPowers, phasePowerCatalog = phasePowers)
  private val parked = new ParkedDecisionAssertions(catalog, walkerPowers,
    phasePowers)
  private val round = ConspiratorPrediction.roundDecisionId
  private val winner = ConspiratorPrediction.winnerDecisionId

  /** Round `at`, p1's Act; p2, who wakes next, holds Conspirator, active
    * unless `active` says otherwise. */
  private def table(at: Int = 1, active: Boolean = true): ReadyGame =
    Table.start.legacy(p2, ConspiratorCard, active)
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = at)))).ready

  /** p1 rests from `ready`, and p2's Wake begins. */
  private def rested(ready: ReadyGame): OathTransition = accepted(
    rules.startWalker(Ready(MenuPlay.withoutMenu(ready)),
      PhaseTransitionRef.BeginRest, p1))

  private def answer(from: OathTransition, id: String,
      reply: DecisionAnswer): OathTransition =
    accepted(rules.resolveWalker(from.state, p2, id, reply))

  private def queryOf(state: OathState): Option[DecisionQuery] =
    accepted(ParkedNode.of(state, catalog, walkerPowers, phasePowers))
      .collect { case ParkedNode.Decision(_, decide, _) => decide.query }

  private def awaits(state: OathState, id: String): Boolean =
    parked.parkedDecision(state).exists(facts =>
      facts.decision == id && facts.awaiting == p2)

  test("at their round-1 Wake the holder is asked a round from 1 to 8, " +
      "then a winner among the seated players, themselves included"):
    val asked = rested(table())
    assert(awaits(asked.state, round))
    assertEquals(queryOf(asked.state), Some(
      DecisionQuery.ChooseAmount(1, 8,
        Some("Conspirator: secretly write down the round the game ends in"),
        "Write down")))
    val second = answer(asked, round, ChooseAmountAnswer(6))
    assert(awaits(second.state, winner))
    queryOf(second.state) match
      case Some(one: DecisionQuery.ChooseOne) =>
        assertEquals(one.options.map(_.ref),
          Vector(p1, p2, p3).map(DecisionOptionRef.Player(_)))
      case other => fail(s"expected a choose-one, got $other")

  test("after both answers the prediction is stored and the Wake goes on"):
    val ready = table()
    val asked = rested(ready)
    val second = answer(asked, round, ChooseAmountAnswer(6))
    val done = answer(second, winner,
      ChooseOneAnswer(DecisionOptionRef.Player(p3)))
    val after = Outcomes.ready(done.state)
    assertEquals(after.game.current.predictions, Map(p2 -> Prediction(6, p3)))
    assertEquals(after.game.current.turn.activePlayer, p2)
    assert(!awaits(done.state, round) && !awaits(done.state, winner))
    assertEquals(accepted(Journal.evolved(rules, ready,
      asked.events ++ second.events ++ done.events)), done.state)

  test("a walk resumed at each question stores one prediction"):
    val ready = table()
    val asked = rested(ready)
    val atRound = OathTransition(accepted(Journal.evolved(rules, ready,
      asked.events)), Vector.empty)
    val second = answer(atRound, round, ChooseAmountAnswer(2))
    val atWinner = OathTransition(accepted(Journal.evolved(rules, ready,
      asked.events ++ second.events)), Vector.empty)
    val done = answer(atWinner, winner,
      ChooseOneAnswer(DecisionOptionRef.Player(p2)))
    assertEquals(Outcomes.ready(done.state).game.current.predictions,
      Map(p2 -> Prediction(2, p2)))

  test("after round 1 nothing is asked: a Conspirator The Mouth activates " +
      "later never writes (ruling 25)"):
    val asked = rested(table(at = 2))
    assert(!awaits(asked.state, round))
    assertEquals(Outcomes.ready(asked.state).game.current.predictions,
      Map.empty)

  test("a dormant Conspirator asks nothing (ruling 1)"):
    assert(!awaits(rested(table(active = false)).state, round))

  test("a Quest to Distant Lands holder places their pawn, then writes"):
    val (l1, l2) = (LineageId("l1"), LineageId("l2"))
    val record = SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2),
      l2, Role.Citizen, QuestToDistantLandsCard.id -> true,
      ConspiratorCard.id -> true)
    val stayingOff: Answers =
      case park if park.decisionId == SetupProcedure.pawnDecisionId(p2) =>
        ChooseOneAnswer(QuestToDistantLands.stayOff)
    val atRound = Situation.act(Situation.productionRules(catalog)
      .withAnswers(stayingOff), p1, record,
      LaterGameFixture.orders(record, p1)).parkedAt(round)(
      MenuPlay.start(p1, PhaseTransitionRef.BeginRest))
    assert(atRound.ready.game.current.pawnSite(p2).nonEmpty)
    assertEquals(atRound.ready.game.current.turn.activePlayer, p2)
