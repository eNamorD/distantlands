package distantlands.gameplay

import distantlands.gameplay.phases.rest.RoundEndRandomPort
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathEvent._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Outcomes, RoundEndChoices, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** The end of a round in a game with an Empire (CR p. 19; Empire phase
  * design, "Slice 2"), from `endRound` and through replay. p1 is the
  * Chancellor, so their seat starts each round; p2 is a Citizen and p3 an
  * Exile. */
class EmpireRoundEndSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)

  /** The Empire's table at Round End of `round`, with `holder` holding the
    * title. */
  private def roundEnd(round: Int, holder: Option[PlayerId] = Some(p2))
      : ReadyGame =
    Table.start.imperial(p1, p2).oathkeeper(holder)
      .oathkeeperGoal(OathkeeperGoal.ThePeople).turn(p1, Phase.RoundEnd)
      // No Table step sets the round.
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = round, usurperLimited = false)))).ready

  private def ended(ready: ReadyGame, random: RoundEndRandomPort)
      : Vector[OathEvent] =
    accepted(StateBasedEvaluation.endRound(Ready(ready), random))

  /** The state after applying `events` to `ready` through the rules. */
  private def replayed(ready: ReadyGame, events: Vector[OathEvent])
      : Either[OathViolation, OathState] =
    events.foldLeft[Either[OathViolation, OathState]](Right(Ready(ready)))(
      (state, event) => state.flatMap(rules.evolve(_, event)))

  test("rounds 1 to 4 roll no end die"):
    (1 to 4).foreach { round =>
      assertEquals(ended(roundEnd(round), RoundEndChoices.firstCandidate),
        Vector(RoundEnded(round, Some(round + 1))), clue(round))
    }

  test("an Exile Oathkeeper rolls no end die"):
    assertEquals(ended(roundEnd(5, holder = Some(p3)),
      RoundEndChoices.firstCandidate), Vector(RoundEnded(5, Some(6))))

  test("a losing roll is journaled before the next round"):
    val events = ended(roundEnd(6), RoundEndChoices.endDie(4))
    assertEquals(events, Vector(EndDieRolled(6, 4, None),
      RoundEnded(6, Some(7))))
    val after = Outcomes.ready(accepted(replayed(roundEnd(6), events)))
    assertEquals(after.game.current.tracks.round, 7)
    assertEquals(after.game.current.result, None)

  test("a winning roll ends the game with no round after it"):
    val events = ended(roundEnd(7), RoundEndChoices.endDie(3))
    val won = GameResult(p1, VictoryKind.StableRegime)
    assertEquals(events, Vector(EndDieRolled(7, 3, Some(won))))
    val after = Outcomes.ready(accepted(replayed(roundEnd(7), events)))
    assertEquals(after.game.current.result, Some(won))

  test("replay refuses a round after a winning roll"):
    val won = EndDieRolled(7, 3, Some(GameResult(p1, VictoryKind.StableRegime)))
    assert(replayed(roundEnd(7), Vector(won, RoundEnded(7, Some(8)))).isLeft)

  test("replay refuses a changed face or win"):
    val ready = roundEnd(5)
    assert(replayed(ready, Vector(EndDieRolled(5, 5,
      Some(GameResult(p1, VictoryKind.StableRegime))))).isLeft)
    assert(replayed(ready, Vector(EndDieRolled(5, 6, None))).isLeft)
    assert(replayed(ready, Vector(EndDieRolled(5, 7, None))).isLeft)
    assert(replayed(ready, Vector(EndDieRolled(6, 6, None))).isLeft)

  test("replay refuses an end die where none is rolled"):
    assert(replayed(roundEnd(4), Vector(EndDieRolled(4, 6, None))).isLeft)
    assert(replayed(roundEnd(5, holder = Some(p3)),
      Vector(EndDieRolled(5, 6, None))).isLeft)
    val inAct = Table.from(roundEnd(5)).turn(p1, Phase.Act).ready
    assert(replayed(inAct, Vector(EndDieRolled(5, 6, None))).isLeft)

  test("round 8 resolves the Empire's War Exhaustion and replays it"):
    val ready = roundEnd(8, holder = Some(p3))
    val events = ended(ready, RoundEndChoices.endDie(1))
    val won = WarExhaustionResolved(p1, VictoryKind.ChancellorExhaustion,
      Vector.empty, Vector.empty)
    assertEquals(events, Vector(RoundEnded(8, None), won))
    val after = Outcomes.ready(accepted(replayed(ready, events)))
    assertEquals(after.game.current.result,
      Some(GameResult(p1, VictoryKind.ChancellorExhaustion)))

  test("round 8 gives the Successor the Chancellor's win"):
    val ready = Table.from(roundEnd(8, holder = None))
      .darkestSecret(Some(p2), secrets = 1).ready
    val events = ended(ready, RoundEndChoices.endDie(1))
    assertEquals(events.last, WarExhaustionResolved(p2,
      VictoryKind.Successor, Vector.empty, Vector.empty))
    assert(replayed(ready, events).isRight)

  test("replay refuses an Empire's War Exhaustion with another winner"):
    val ready = roundEnd(8, holder = Some(p3))
    assertEquals(rejected(replayed(ready, Vector(RoundEnded(8, None),
      WarExhaustionResolved(p3, VictoryKind.Oathkeeper, Vector.empty,
        Vector.empty)))).getClass, classOf[OathViolation.InvalidEventOrder])
