package distantlands.gameplay.powers.legacies

import distantlands.application.{GameAccepted, GameApplicationService,
  GameProjector, ParkedServiceFixture, TreeDecision}
import distantlands.application.gamelog.LogScripts.{formatter, texts}
import distantlands.gameplay.actions.negotiation.HighPriestCard
import distantlands.gameplay.chronicle.ChronicleBeginProcedure
import distantlands.gameplay.chronicle.legacies.{RoyalLine, RoyalLineCard}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.TheStandardBearerCard
import distantlands.model._
import distantlands.testkit.{Commands, MenuPlay, Outcomes, RoundEndChoices,
  Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}

/** Conspirator in the Sun: the prediction is compared, right after the
  * Sun's first window, with the round the game ended in and its final
  * winner, after any Successor and any Royal Line choice, and revealed
  * either way (Legacy in-play powers ruling 27; R24, R27, R29).
  * Played through the service on its round-end port. */
class ConspiratorRevealSuite extends munit.FunSuite:
  private val gameId = "conspirator-reveal"
  private val successor = GameResult(p2, VictoryKind.Successor)
  private val projector = new GameProjector(catalog)

  /** Round 5's last Act, p3's. p1, the Chancellor, holds the Oathkeeper
    * title of Devotion and the Darkest Secret; p2, a Citizen, holds the
    * Grand Scepter, Devotion's Successor goal; p3, an Exile, holds
    * Conspirator (`active`, dormant, or none) and has written down
    * `prediction`. */
  private def lastAct(prediction: Prediction,
      conspirator: Option[Boolean] = Some(true)): Table =
    val table = Table.start.imperial(p1, p2)
      .oathkeeperGoal(OathkeeperGoal.Devotion).oathkeeper(Some(p1))
      .darkestSecret(Some(p1), secrets = 1).relic(p2, TheGrandScepterCard)
      .turn(p3, Phase.Act)
      // No Table step sets the round or a prediction.
      .update(_.updateCurrent(c => c.copy(
        tracks = c.tracks.copy(round = 5, usurperLimited = false),
        predictions = Map(p3 -> prediction))))
    conspirator.fold(table)(active => table.legacy(p3, ConspiratorCard,
      active))

  /** p3 rests from `table`; the end die rolls 6, p1's Stable Regime win
    * goes to p2 as Successor, and p2 starts the Chronicle: the Sun opens,
    * and with no Royal Line the prediction is compared at once. */
  private def ended(table: Table)
      : (GameApplicationService, GameAccepted) =
    val (service, _) = table.service(roundEnd = RoundEndChoices.endDie(6))
    (service, started(service, accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))))

  /** The game's winner presses Start Chronicle. */
  private def started(service: GameApplicationService, at: GameAccepted)
      : GameAccepted =
    val winner = present(Outcomes.ready(at.state).game.current.result,
      "a result").winner
    accepted(service.handle(gameId, at.nextSequence, Commands.resolveWalker(
      winner, TreeDecision(ChronicleBeginProcedure.decisionId,
        DecisionAnswer.ChooseOneAnswer(ChronicleBeginProcedure.start)))))

  /** p2, a Citizen holding Royal Line, names `named` at the Sun's first
    * window. */
  private def naming(service: GameApplicationService, at: GameAccepted,
      named: PlayerId): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence, Commands.resolveWalker(
      p2, TreeDecision(RoyalLine.decisionId(p2),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(named))))))

  private def lines(service: GameApplicationService): Vector[String] =
    texts(formatter.format(present(accepted(service.history(gameId)),
      "a stored game").steps, Some(p1)))

  private def revealLine(winner: PlayerId, round: Int): String =
    s"p3 had written down ${winner.value} to win in round $round with " +
      ConspiratorCard.name

  test("a correct prediction beats a Successor win, and the Sun's later " +
      "steps read its holder as the winner"):
    val (service, done) = ended(lastAct(Prediction(5, p2)))
    val current = Outcomes.ready(done.state).game.current
    assertEquals(current.result, Some(GameResult(p3, VictoryKind.Conspirator)))
    assertEquals(current.predictions, Map.empty)
    val written = lines(service)
    val successorLine = written.indexOf("p2 won as Successor to the Chancellor")
    val reveal = written.indexOf(revealLine(p2, 5))
    val won = written.indexOf(s"p3 won with ${ConspiratorCard.name}")
    assert(successorLine >= 0 && reveal > successorLine && won > reveal,
      written)

  test("a wrong round changes nothing, and the prediction is still revealed"):
    val (service, done) = ended(lastAct(Prediction(6, p2)))
    assertEquals(Outcomes.ready(done.state).game.current.result,
      Some(successor))
    val written = lines(service)
    assert(written.contains(revealLine(p2, 6)), written)
    assert(!written.exists(_.contains("won with")), written)

  test("a wrong winner changes nothing"):
    val (_, done) = ended(lastAct(Prediction(5, p1)))
    assertEquals(Outcomes.ready(done.state).game.current.result,
      Some(successor))

  test("a Conspirator lost, or gone dormant, is void, and still revealed " +
      "with the same line (R29)"):
    Vector(None, Some(false)).foreach { conspirator =>
      val (service, done) = ended(lastAct(Prediction(5, p2), conspirator))
      assertEquals(Outcomes.ready(done.state).game.current.result,
        Some(successor), clue(conspirator))
      assert(lines(service).contains(revealLine(p2, 5)), lines(service))
    }

  test("the game's end and the Sun's comparison replay from the journal, " +
      "the store empty"):
    val table = lastAct(Prediction(5, p2))
    val (service, repository) =
      table.service(roundEnd = RoundEndChoices.endDie(6))
    val done = started(service, accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest))))
    val reloaded = present(accepted(ParkedServiceFixture.replayingSecond(
      service, repository).load(gameId)), "a stored game").state
    assertEquals(reloaded, done.state)
    assertEquals(Outcomes.ready(reloaded).game.current.predictions, Map.empty)

  test("with no prediction the game's end writes no reveal"):
    val plain = Table.start.imperial(p1, p2)
      .oathkeeperGoal(OathkeeperGoal.Devotion).oathkeeper(Some(p1))
      .darkestSecret(Some(p1), secrets = 1).relic(p2, TheGrandScepterCard)
      .turn(p3, Phase.Act)
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = 5, usurperLimited = false))))
    val (service, done) = ended(plain)
    assertEquals(Outcomes.ready(done.state).game.current.result,
      Some(successor))
    assert(!lines(service).exists(_.contains("had written down")))

  test("a Conspirator lost to The Mouth's cap in play voids the prediction " +
      "when the Sun compares it"):
    // p3 also holds The Mouth, Golden Key and High Priest active, The
    // Standard Bearer dormant, the People's Favor, which meets its goal,
    // and 2 favor: activating The Standard Bearer takes p3 to five active
    // legacies, and p3 discards Conspirator and Golden Key.
    val table = lastAct(Prediction(5, p2)).legacy(p3, TheMouthCard)
      .legacy(p3, GoldenKeyCard).legacy(p3, HighPriestCard)
      .legacy(p3, TheStandardBearerCard, active = false)
      .peoplesFavor(Some(p3), favor = 1).favor(p3, 2)
    val (service, _) = table.service(roundEnd = RoundEndChoices.endDie(6))
    val used = accepted(service.handle(gameId, 0L, MenuPlay.starting(p3,
      ActionRef.UsePower(TheMouth.id),
      Vector(DecisionOptionRef.Legacy(TheMouthCard.id)))))
    val chosen = accepted(service.handle(gameId, used.nextSequence,
      Commands.resolveWalker(p3, TreeDecision(TheMouth.decisionId,
        DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Legacy(TheStandardBearerCard.id))))))
    val capped = accepted(service.handle(gameId, chosen.nextSequence,
      Commands.resolveWalker(p3, TreeDecision(TheMouth.capDecisionId,
        DecisionAnswer.ChooseManyAnswer(Vector(
          DecisionOptionRef.Legacy(ConspiratorCard.id),
          DecisionOptionRef.Legacy(GoldenKeyCard.id)))))))
    val done = started(service, accepted(service.handle(gameId,
      capped.nextSequence, MenuPlay.starting(p3,
        PhaseTransitionRef.BeginRest))))
    val current = Outcomes.ready(done.state).game.current
    assertEquals(current.result, Some(successor))
    assertEquals(current.predictions, Map.empty)
    assert(lines(service).contains(revealLine(p2, 5)), lines(service))

  /** `lastAct`, with p2, the Citizen, also holding Royal Line. */
  private def withRoyalLine(prediction: Prediction): Table =
    lastAct(prediction).legacy(p2, RoyalLineCard)

  test("Royal Line's choice comes first: naming p1 the winner makes a " +
      "prediction of p1 hold (R24)"):
    val (service, opened) = ended(withRoyalLine(Prediction(5, p1)))
    val done = naming(service, opened, p1)
    assertEquals(Outcomes.ready(done.state).game.current.result,
      Some(GameResult(p3, VictoryKind.Conspirator)))
    val written = lines(service)
    val named = written.indexWhere(_.contains("the winner with Royal Line"))
    val reveal = written.indexOf(revealLine(p1, 5))
    val won = written.indexOf(s"p3 won with ${ConspiratorCard.name}")
    assert(named >= 0 && reveal > named && won > reveal, written)

  test("Royal Line's choice comes first: naming p1 the winner makes a " +
      "prediction of p2, the Successor, fail (R24)"):
    val (service, opened) = ended(withRoyalLine(Prediction(5, p2)))
    val done = naming(service, opened, p1)
    assertEquals(Outcomes.ready(done.state).game.current.result
      .map(_.winner), Some(p1))
    assert(lines(service).contains(revealLine(p2, 5)), lines(service))
    assert(!lines(service).exists(_.contains("won with")), lines(service))

  test("the Sun's steps after the comparison read the Conspirator winner"):
    val (service, _) = ended(lastAct(Prediction(5, p2)))
    val written = lines(service)
    val won = written.indexOf(s"p3 won with ${ConspiratorCard.name}")
    val crowned = written.indexWhere(line => line.startsWith("p3 ") &&
      line.contains("Chancellor"))
    assert(won >= 0 && crowned > won, written)

  test("a game parked at Royal Line's question reloads, and the comparison " +
      "reads the answer"):
    val table = withRoyalLine(Prediction(5, p1))
    val (service, repository) =
      table.service(roundEnd = RoundEndChoices.endDie(6))
    val opened = started(service, accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest))))
    assertEquals(Outcomes.ready(opened.state).game.current.predictions,
      Map(p3 -> Prediction(5, p1)))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(present(accepted(reloaded.load(gameId)), "a stored game")
      .state, opened.state)
    val done = naming(service, opened, p1)
    assertEquals(Outcomes.ready(done.state).game.current.result,
      Some(GameResult(p3, VictoryKind.Conspirator)))
    assertEquals(present(accepted(ParkedServiceFixture.replayingSecond(
      service, repository).load(gameId)), "a stored game").state, done.state)

  test("Royal Line declining leaves the Successor, and a prediction of the " +
      "Successor still wins"):
    val (service, opened) = ended(withRoyalLine(Prediction(5, p2)))
    val done = accepted(service.handle(gameId, opened.nextSequence,
      Commands.resolveWalker(p2, TreeDecision(RoyalLine.decisionId(p2),
        DecisionAnswer.ChooseOneAnswer(RoyalLine.decline)))))
    assertEquals(Outcomes.ready(done.state).game.current.result,
      Some(GameResult(p3, VictoryKind.Conspirator)))

  test("a game parked after the comparison, at Assign Tasks, reloads and " +
      "goes on, each reveal line written once"):
    val table = lastAct(Prediction(5, p2))
    val (service, repository) =
      table.service(roundEnd = RoundEndChoices.endDie(6))
    val opened = started(service, accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest))))
    val parked = Outcomes.ready(opened.state).game.current
    assertEquals(parked.result, Some(GameResult(p3, VictoryKind.Conspirator)))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    val loaded = present(accepted(reloaded.load(gameId)), "a stored game")
    assertEquals(loaded.state, opened.state)
    val world = "chronicle.assign.world"
    val asked = present(projector.project(gameId, loaded, p3).walkerDecision,
      "the Assign Tasks question")
    assertEquals(asked.decisionId, world)
    val option = present(asked.query.flatMap(_.offeredOptions.headOption),
      "an option")
    val answered = accepted(reloaded.handle(gameId, loaded.nextSequence,
      Commands.resolveWalker(p3, TreeDecision(world,
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(
          PlayerId(option.id)))))))
    assertEquals(Outcomes.ready(answered.state).game.current.result,
      Some(GameResult(p3, VictoryKind.Conspirator)))
    val written = texts(formatter.format(present(accepted(
      reloaded.history(gameId)), "a stored game").steps, Some(p1)))
    assertEquals(written.count(_ == revealLine(p2, 5)), 1, written)
    assertEquals(written.count(_ == s"p3 won with ${ConspiratorCard.name}"),
      1, written)
