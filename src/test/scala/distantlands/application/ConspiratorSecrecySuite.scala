package distantlands.application

import distantlands.application.gamelog.LogScripts.{formatter, texts}
import distantlands.gameplay.powers.legacies.{ConspiratorCard,
  ConspiratorPrediction}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection.GameProjectionCodec
import distantlands.testkit.{Commands, MenuPlay, Table}
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
    val (service, _) = table.service()
    val asked = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p1, PhaseTransitionRef.BeginRest)))
    val round = resolving(service, asked, ConspiratorPrediction.roundDecisionId,
      DecisionAnswer.ChooseAmountAnswer(6))
    val winner = resolving(service, round,
      ConspiratorPrediction.winnerDecisionId,
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(p3)))
    (service, Vector(asked, round, winner))

  private def lines(service: GameApplicationService, viewer: PlayerId)
      : Vector[String] =
    texts(formatter.format(present(accepted(service.history(gameId)),
      "a stored game").steps, Some(viewer)))

  test("while the questions wait, another seat sees only whom they wait on"):
    val (_, steps) = written
    steps.take(2).foreach { at =>
      val loaded = LoadedGame(at.state, at.nextSequence)
      Vector(projector.project(gameId, loaded, p1),
          projector.projectPublic(gameId, loaded)).foreach { view =>
        assertEquals(view.walkerDecision, None)
        assertEquals(view.walkerWaiting.map(_.playerIds), Some(Vector("p2")))
        assert(!GameProjectionCodec.encode(view).contains("round 6"))
      }
      assert(projector.project(gameId, loaded, p2).walkerDecision.nonEmpty)
    }

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

  test("the stored prediction reaches no other viewer's projection"):
    val (_, steps) = written
    val at = steps.last
    val loaded = LoadedGame(at.state, at.nextSequence)
    val view = GameProjectionCodec.encode(projector.project(gameId, loaded,
      p1))
    assert(!view.contains("prediction") && !view.contains("predicted"))
