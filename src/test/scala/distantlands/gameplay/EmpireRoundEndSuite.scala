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

  /** `roundEnd(round)` with `modifier` waiting for the roll. */
  private def modified(round: Int, modifier: Int): ReadyGame =
    roundEnd(round).updateCurrent(_.copy(endDieModifier = modifier))

  test("a modified round-five 4 is a 6, and the Chancellor wins " +
      "(Legacy in-play powers ruling 21)"):
    val won = GameResult(p1, VictoryKind.StableRegime)
    val events = ended(modified(5, 2), RoundEndChoices.endDie(4))
    assertEquals(events, Vector(EndDieRolled(5, 4, Some(won), 2)))
    val after = Outcomes.ready(accepted(replayed(modified(5, 2), events)))
    assertEquals(after.game.current.result, Some(won))
    assertEquals(after.game.current.endDieModifier, 0)

  test("a modified round-seven 3 is a 2, and the round goes on with the " +
      "modifier cleared"):
    val events = ended(modified(7, -1), RoundEndChoices.endDie(3))
    assertEquals(events, Vector(EndDieRolled(7, 3, None, -1),
      RoundEnded(7, Some(8))))
    val after = Outcomes.ready(accepted(replayed(modified(7, -1), events)))
    assertEquals(after.game.current.endDieModifier, 0)

  test("replay refuses a modifier other than the one waiting"):
    assert(replayed(modified(5, 2), Vector(EndDieRolled(5, 4,
      Some(GameResult(p1, VictoryKind.StableRegime)), 1))).isLeft)
    assert(replayed(roundEnd(5), Vector(EndDieRolled(5, 4,
      Some(GameResult(p1, VictoryKind.StableRegime)), 2))).isLeft)

  test("a round that ends with no roll clears a waiting modifier, so no " +
      "stale modifier reaches the next round's end die (R11)"):
    // Round 4 rolls no end die: the +2 left waiting is cleared as the
    // round ends, and round 5's unmodified 4 does not win.
    val round4 = modified(4, 2)
    val events = ended(round4, RoundEndChoices.firstCandidate)
    assertEquals(events, Vector(RoundEnded(4, Some(5))))
    val after = Outcomes.ready(accepted(replayed(round4, events)))
    assertEquals(after.game.current.endDieModifier, 0)
    val round5 = Table.from(after).turn(p1, Phase.RoundEnd).ready
    assertEquals(ended(round5, RoundEndChoices.endDie(4)),
      Vector(EndDieRolled(5, 4, None), RoundEnded(5, Some(6))))

  test("round 8's War Exhaustion clears a waiting modifier"):
    val round8 = modified(8, 3)
    val after = Outcomes.ready(accepted(replayed(round8,
      ended(round8, RoundEndChoices.firstCandidate))))
    assertEquals(after.game.current.endDieModifier, 0)

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

  /** The game `ready`'s round end ends, through the engine, with the
    * Empire win `empireWin` reads back. */
  private def empireWinAfter(ready: ReadyGame, random: RoundEndRandomPort)
      : (Option[GameResult], VictoryKind) =
    val after = Outcomes.ready(accepted(replayed(ready, ended(ready, random))))
    (after.game.current.result, EmpireEndings.empireWin(after))

  test("the Empire win read back after each end-die ending (R37)"):
    Vector(5 -> 6, 6 -> 5, 7 -> 3).foreach((round, face) =>
      assertEquals(empireWinAfter(roundEnd(round), RoundEndChoices.endDie(face)),
        (Some(GameResult(p1, VictoryKind.StableRegime)),
          VictoryKind.StableRegime), clue(round)))

  test("the Empire win read back after each round-eight ending (R37)"):
    // An Imperial Oathkeeper gives Stable Regime; an Exile's gives
    // Chancellor Exhaustion.
    assertEquals(empireWinAfter(roundEnd(8), RoundEndChoices.endDie(1)),
      (Some(GameResult(p1, VictoryKind.StableRegime)),
        VictoryKind.StableRegime))
    assertEquals(empireWinAfter(roundEnd(8, holder = Some(p3)),
      RoundEndChoices.endDie(1)),
      (Some(GameResult(p1, VictoryKind.ChancellorExhaustion)),
        VictoryKind.ChancellorExhaustion))

  test("a Successor's result still reads back the Chancellor's Empire win " +
      "(R37)"):
    val atEight = Table.from(roundEnd(8, holder = None))
      .darkestSecret(Some(p2), secrets = 1).ready
    assertEquals(empireWinAfter(atEight, RoundEndChoices.endDie(1)),
      (Some(GameResult(p2, VictoryKind.Successor)),
        VictoryKind.ChancellorExhaustion))
    val onTheDie = Table.from(roundEnd(7)).darkestSecret(Some(p2),
      secrets = 1).ready
    assertEquals(empireWinAfter(onTheDie, RoundEndChoices.endDie(3)),
      (Some(GameResult(p2, VictoryKind.Successor)), VictoryKind.StableRegime))
