package distantlands.gameplay.phases.rest

import distantlands.application.{GameApplicationService, GameAccepted,
  GameProjector, LoadedGame, ParkedServiceFixture, TreeDecision}
import distantlands.application.gamelog.LogScripts.{formatter, texts}
import distantlands.gameplay.chronicle.ChronicleBeginProcedure
import distantlands.gameplay.chronicle.legacies.{RoyalLine, RoyalLineCard}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.walker.WalkerCompleted
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathEvent.{EndDieRolled, RoundEnded}
import distantlands.testkit.{Commands, MenuPlay, Outcomes, ParkedNode,
  RecordedOps, RoundEndChoices, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}

/** Arbiter (L23): before the end die is rolled, its holder may modify it
  * by -1 to +3 (Legacy in-play powers ruling 21; R11-R13), played through
  * the service on its round-end port. */
class ArbiterSuite extends munit.FunSuite:
  private val gameId = "arbiter"
  private val win = GameResult(p1, VictoryKind.StableRegime)

  /** `round`'s last Act: p1 the Chancellor, p2 a Citizen holding the
    * title of the People, p3 an Exile in their Act, the round's last seat.
    * `arbiter` is p3's Arbiter: active, dormant or not held. */
  private def lastActOfRound(round: Int, holder: PlayerId = p2,
      arbiter: Option[Boolean] = Some(true)): Table =
    val table = Table.start.imperial(p1, p2)
      .oathkeeperGoal(OathkeeperGoal.ThePeople).oathkeeper(Some(holder))
      .turn(p3, Phase.Act)
      // No Table step sets the round.
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = round, usurperLimited = false))))
    arbiter.fold(table)(active => table.legacy(p3, ArbiterCard, active))

  /** p3 rests from `table`, the round's end on `roundEnd`. */
  private def restedOn(table: Table, roundEnd: RoundEndRandomPort)
      : (GameApplicationService, GameAccepted) =
    val (service, _) = table.service(roundEnd = roundEnd)
    (service, accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest))))

  /** p3 rests from `table`, the end die to roll `face`. */
  private def rested(table: Table, face: Int)
      : (GameApplicationService, GameAccepted) =
    restedOn(table, RoundEndChoices.endDie(face))

  private def answering(service: GameApplicationService, at: GameAccepted,
      ref: DecisionOptionRef): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence, Commands.resolveWalker(
      p3, TreeDecision(ArbiterStep.decisionId(p3),
        DecisionAnswer.ChooseOneAnswer(ref)))))

  private def parkedId(state: OathState): Option[String] =
    accepted(ParkedNode.of(state, catalog,
      WalkerPowerCatalog.default(catalog))).map(_.decisionId)

  private def lines(service: GameApplicationService): Vector[String] =
    texts(formatter.format(present(accepted(service.history(gameId)),
      "a stored game").steps, Some(p1)))

  private def endDice(events: Vector[OathEvent]): Vector[EndDieRolled] =
    events.collect { case rolled: EndDieRolled => rolled }

  test("before the roll the holder is asked, Decline first, then -1, +1, " +
      "+2, +3, and the table sees whom it waits on"):
    val (_, at) = rested(lastActOfRound(5), face = 4)
    present(accepted(ParkedNode.of(at.state, catalog,
      WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, awaiting) =>
        assertEquals(decide.decisionId, ArbiterStep.decisionId(p3))
        assertEquals(awaiting, p3)
        decide.query match
          case one: DecisionQuery.ChooseOne =>
            assertEquals(one.options.map(_.ref), ArbiterStep.decline +:
              Vector(-1, 1, 2, 3).map(ArbiterStep.by))
          case other => fail(s"expected a choose-one, got $other")
      case other => fail(s"expected a decision, parked on $other")
    assertEquals(endDice(at.events), Vector.empty)
    val loaded = LoadedGame(at.state, at.nextSequence)
    assertEquals(new GameProjector(catalog).projectPublic(gameId, loaded)
      .walkerWaiting.map(_.playerIds), Some(Vector("p3")))

  test("+2 turns a round-five 4 into the Chancellor's win, and both lines " +
      "are written"):
    val (service, at) = rested(lastActOfRound(5), face = 4)
    val done = answering(service, at, ArbiterStep.by(2))
    assert(RecordedOps.all(done.events).contains(SetEndDieModifier(2)))
    assertEquals(endDice(done.events), Vector(EndDieRolled(5, 4, Some(win),
      2)))
    assertEquals(Outcomes.ready(done.state).game.current.result, Some(win))
    val written = lines(service)
    assert(written.contains("p3 modified the end die by +2 with Arbiter"),
      written)
    assert(written.contains(
      "The end die rolled 4, modified by +2 to 6. Stable Regime needed 6."),
      written)

  test("-1 turns a round-seven 3 into a loss, and round 8 begins"):
    val (service, at) = rested(lastActOfRound(7), face = 3)
    val done = answering(service, at, ArbiterStep.by(-1))
    assertEquals(endDice(done.events), Vector(EndDieRolled(7, 3, None, -1)))
    assert(done.events.contains(RoundEnded(7, Some(8))))
    val current = Outcomes.ready(done.state).game.current
    assertEquals((current.result, current.tracks.round,
      current.endDieModifier), (None, 8, 0))
    assert(lines(service).contains(
      "p3 modified the end die by -1 with Arbiter"), lines(service))

  test("Decline rolls the plain die and writes no Arbiter line"):
    val (service, at) = rested(lastActOfRound(5), face = 4)
    val done = answering(service, at, ArbiterStep.decline)
    assert(!RecordedOps.all(done.events).exists {
      case _: SetEndDieModifier => true
      case _ => false })
    assertEquals(endDice(done.events), Vector(EndDieRolled(5, 4, None)))
    assertEquals(Outcomes.ready(done.state).game.current.tracks.round, 6)
    assert(!lines(service).exists(_.contains("modified the end die")),
      lines(service))

  test("no step when the end die is not rolled: round 4, round 8, an Exile " +
      "Oathkeeper"):
    val (_, round4) = restedOn(lastActOfRound(4),
      RoundEndChoices.firstCandidate)
    assertNotEquals(parkedId(round4.state), Some(ArbiterStep.decisionId(p3)))
    assertEquals(endDice(round4.events), Vector.empty)
    assertEquals(Outcomes.ready(round4.state).game.current.tracks.round, 5)
    val (_, exile) = restedOn(lastActOfRound(5, holder = p3),
      RoundEndChoices.firstCandidate)
    assertNotEquals(parkedId(exile.state), Some(ArbiterStep.decisionId(p3)))
    assertEquals(endDice(exile.events), Vector.empty)
    val (_, round8) = restedOn(lastActOfRound(8),
      RoundEndChoices.firstCandidate)
    assertNotEquals(parkedId(round8.state), Some(ArbiterStep.decisionId(p3)))
    assertEquals(endDice(round8.events), Vector.empty)
    assert(Outcomes.ready(round8.state).game.current.result.nonEmpty)

  test("a dormant Arbiter, or none, asks nothing and the die rolls " +
      "(ruling 1)"):
    Vector(Some(false), None).foreach { arbiter =>
      val (_, at) = rested(lastActOfRound(5, arbiter = arbiter), face = 4)
      assertNotEquals(parkedId(at.state), Some(ArbiterStep.decisionId(p3)),
        clue(arbiter))
      assertEquals(endDice(at.events), Vector(EndDieRolled(5, 4, None)),
        clue(arbiter))
    }

  test("a walk resumed at Arbiter's question rolls once, with the chosen " +
      "modifier, and the whole journal replays"):
    val table = lastActOfRound(5)
    val (service, repository) =
      table.service(roundEnd = RoundEndChoices.endDie(4))
    val at = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    assertEquals(parkedId(at.state), Some(ArbiterStep.decisionId(p3)))
    // The parked game reloads from its journal, then the answer resumes it.
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(present(accepted(reloaded.load(gameId)), "a stored game")
      .state, at.state)
    val done = answering(service, at, ArbiterStep.by(3))
    val history = present(accepted(service.history(gameId)), "a stored game")
    assertEquals(endDice(history.steps.map(_.event.event)),
      Vector(EndDieRolled(5, 4, Some(win), 3)))
    assertEquals(present(accepted(ParkedServiceFixture.replayingSecond(
      service, repository).load(gameId)), "a stored game").state, done.state)

  test("an Arbiter-modified game end settles the final winner once, across " +
      "a reload at Royal Line's question and into the Chronicle (R36)"):
    // p2, the Citizen holding the title, also holds Royal Line.
    val table = lastActOfRound(5).legacy(p2, RoyalLineCard)
    val (service, repository) =
      table.service(roundEnd = RoundEndChoices.endDie(4))
    val rested = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    val raised = answering(service, rested, ArbiterStep.by(2))
    assertEquals(parkedId(raised.state), Some(RoyalLine.decisionId(p2)))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    val loaded = present(accepted(reloaded.load(gameId)), "a stored game")
    assertEquals(loaded.state, raised.state)
    val declined = accepted(reloaded.handle(gameId, loaded.nextSequence,
      Commands.resolveWalker(p2, TreeDecision(RoyalLine.decisionId(p2),
        DecisionAnswer.ChooseOneAnswer(RoyalLine.decline)))))
    assertEquals(parkedId(declined.state),
      Some(ChronicleBeginProcedure.decisionId))
    val begun = accepted(reloaded.handle(gameId, declined.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(
        ChronicleBeginProcedure.decisionId,
        DecisionAnswer.ChooseOneAnswer(ChronicleBeginProcedure.start)))))
    assertEquals(Outcomes.ready(begun.state).game.current.turn.phase,
      Phase.Chronicle)
    val completed = present(accepted(reloaded.history(gameId)),
      "a stored game").steps.map(_.event.event).collect {
        case WalkerCompleted(procedure) => procedure }
    assertEquals(completed.count(_ == TriggeredProcedureRef.ChronicleWinner),
      1)
