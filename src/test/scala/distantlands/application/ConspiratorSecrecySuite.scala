package distantlands.application

import distantlands.application.gamelog.LogScripts.{formatter, texts}
import distantlands.gameplay.powers.legacies.{ConspiratorCard,
  ConspiratorPrediction}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection.GameProjection
import distantlands.testkit.{Commands, MenuPlay, Outcomes, RoundEndChoices,
  Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}

/** Conspirator's prediction is hidden from every other viewer until the
  * game ends: projections, the log, and the "chose" lines `DetailLines`
  * writes (Legacy in-play powers ruling 26; R26-R28). Played through the
  * service. */
class ConspiratorSecrecySuite extends munit.FunSuite:
  private val gameId = "conspirator-secrecy"
  private val projector = new GameProjector(catalog)

  /** Round 1, p1's Act; p2, who wakes next, holds Conspirator. */
  private val table: Table = Table.start.legacy(p2, ConspiratorCard)

  private def resolving(service: GameApplicationService, at: GameAccepted,
      id: String, answer: DecisionAnswer): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence, Commands.resolveWalker(
      p2, TreeDecision(id, answer))))

  /** p1 rests, and p2 writes round 6 and p3. */
  private def written: (GameApplicationService, Vector[GameAccepted]) =
    writing(6, p3)

  /** p1 rests, and p2 writes `round` and `winner`. */
  private def writing(roundWritten: Int, winnerWritten: PlayerId)
      : (GameApplicationService, Vector[GameAccepted]) =
    val (service, _) = table.service()
    val asked = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, PhaseTransitionRef.BeginRest)))
    val round = resolving(service, asked, ConspiratorPrediction.roundDecisionId,
      DecisionAnswer.ChooseAmountAnswer(roundWritten))
    val winner = resolving(service, round,
      ConspiratorPrediction.winnerDecisionId,
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(winnerWritten)))
    (service, Vector(asked, round, winner))

  private def lines(service: GameApplicationService, viewer: PlayerId)
      : Vector[String] =
    texts(formatter.format(present(accepted(service.history(gameId)),
      "a stored game").steps, Some(viewer)))

  /** Every view but the holder's: p1's, p3's and the public one. */
  private def othersViews(at: GameAccepted): Vector[GameProjection] =
    val loaded = LoadedGame(at.state, at.nextSequence)
    Vector(projector.project(gameId, loaded, p1),
      projector.project(gameId, loaded, p3),
      projector.projectPublic(gameId, loaded))

  test("while the questions wait, every other viewer sees only whom they " +
      "wait on: no decision, so no answered options"):
    val (_, steps) = written
    steps.take(2).foreach { at =>
      othersViews(at).foreach { view =>
        assertEquals(view.walkerDecision, None)
        assertEquals(view.walkerWaiting.map(_.playerIds), Some(Vector("p2")))
      }
      val loaded = LoadedGame(at.state, at.nextSequence)
      assert(projector.project(gameId, loaded, p2).walkerDecision.nonEmpty)
    }

  test("every other viewer's projection is the same whatever round and " +
      "winner the holder writes (R28)"):
    val (_, first) = writing(6, p3)
    val (_, second) = writing(2, p1)
    first.zip(second).foreach((one, other) =>
      assertEquals(othersViews(one), othersViews(other)))

  test("every other viewer's log reads only that a prediction was written; " +
      "the holder's also reads it, and no line names an answer (R28)"):
    val (service, _) = written
    val public = s"p2 wrote down a prediction with ${ConspiratorCard.name}"
    val secret = "p2 predicted p3 wins in round 6"
    Vector(p1, p3).foreach { viewer =>
      val seen = lines(service, viewer)
      assert(seen.contains(public), seen)
      assert(!seen.exists(line => line.contains("predicted") ||
        line.contains("chose") || line.contains("round 6")), seen)
    }
    val own = lines(service, p2)
    assert(own.contains(public) && own.contains(secret), own)
    assert(!own.exists(_.contains("chose")), own)


  /** Round 5's last Act, p3's, at the end of which the end die rolls 6 and
    * p2 wins as the Successor: p1, the Chancellor, holds the Oathkeeper
    * title of Devotion and the Darkest Secret; p2, a Citizen, holds the
    * Grand Scepter; p3, an Exile, holds Conspirator and has written down
    * `prediction`. */
  private def endedAt(prediction: Prediction)
      : (GameApplicationService, GameAccepted) =
    val table = Table.start.imperial(p1, p2)
      .oathkeeperGoal(OathkeeperGoal.Devotion).oathkeeper(Some(p1))
      .darkestSecret(Some(p1), secrets = 1).relic(p2, TheGrandScepterCard)
      .legacy(p3, ConspiratorCard).turn(p3, Phase.Act)
      // No Table step sets the round or a prediction.
      .update(_.updateCurrent(c => c.copy(
        tracks = c.tracks.copy(round = 5, usurperLimited = false),
        predictions = Map(p3 -> prediction))))
    val (service, _) = table.service(roundEnd = RoundEndChoices.endDie(6))
    (service, accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p3, PhaseTransitionRef.BeginRest))))

  test("at the Start Chronicle park every other viewer's projection and log " +
      "are the same whatever the prediction, with no reveal (R24, R28)"):
    val (oneService, one) = endedAt(Prediction(5, p2))
    val (otherService, other) = endedAt(Prediction(2, p1))
    Vector(one, other).foreach(at => assertEquals(
      Outcomes.ready(at.state).game.current.turn.phase, Phase.RoundEnd))
    val viewsOf = (at: GameAccepted) =>
      val loaded = LoadedGame(at.state, at.nextSequence)
      Vector(projector.project(gameId, loaded, p1),
        projector.project(gameId, loaded, p2),
        projector.projectPublic(gameId, loaded))
    assertEquals(viewsOf(one), viewsOf(other))
    assertEquals(viewsOf(one).head.walkerWaiting.map(_.playerIds),
      Some(Vector("p2")))
    Vector(p1, p2).foreach { viewer =>
      val seen = lines(oneService, viewer)
      assertEquals(seen, lines(otherService, viewer))
      assert(!seen.exists(_.contains("had written down")), seen)
    }
