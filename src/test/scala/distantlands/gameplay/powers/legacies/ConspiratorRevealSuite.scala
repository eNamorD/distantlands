package distantlands.gameplay.powers.legacies

import distantlands.application.{GameAccepted, GameApplicationService,
  ParkedServiceFixture, TreeDecision}
import distantlands.application.gamelog.LogScripts.{formatter, texts}
import distantlands.gameplay.actions.negotiation.HighPriestCard
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.setup.legacies.TheStandardBearerCard
import distantlands.model._
import distantlands.testkit.{Commands, MenuPlay, Outcomes, RoundEndChoices,
  Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}

/** Conspirator at the game's end: the prediction is compared with the
  * round the game ended in and its final winner, after any Successor, and
  * revealed either way (Legacy in-play powers ruling 27; R24, R27, R29).
  * Played through the service on its round-end port. */
class ConspiratorRevealSuite extends munit.FunSuite:
  private val gameId = "conspirator-reveal"
  private val successor = GameResult(p2, VictoryKind.Successor)

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

  /** p3 rests from `table`; the end die rolls 6, and p1's Stable Regime
    * win goes to p2 as Successor. */
  private def ended(table: Table)
      : (GameApplicationService, GameAccepted) =
    val (service, _) = table.service(roundEnd = RoundEndChoices.endDie(6))
    (service, accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest))))

  private def lines(service: GameApplicationService): Vector[String] =
    texts(formatter.format(present(accepted(service.history(gameId)),
      "a stored game").steps, Some(p1)))

  private def revealLine(winner: PlayerId, round: Int): String =
    s"p3 had written down ${winner.value} to win in round $round with " +
      ConspiratorCard.name

  test("a correct prediction beats a Successor win, and the Chronicle opens " +
      "for its holder"):
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

  test("the game's end replays from the journal, the store empty"):
    val table = lastAct(Prediction(5, p2))
    val (service, repository) =
      table.service(roundEnd = RoundEndChoices.endDie(6))
    val done = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(present(accepted(reloaded.load(gameId)), "a stored game")
      .state, done.state)

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
      "at the game's end"):
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
    val done = accepted(service.handle(gameId, capped.nextSequence,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest)))
    val current = Outcomes.ready(done.state).game.current
    assertEquals(current.result, Some(successor))
    assertEquals(current.predictions, Map.empty)
    assert(lines(service).contains(revealLine(p2, 5)), lines(service))
