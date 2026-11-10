package distantlands.gameplay.phases.rest

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.StateBasedEvaluation
import distantlands.model.{OathTransition, OathViolation, OperationRestriction}

/** Round end after the last player's Rest: the end die, the next round or
  * War Exhaustion, with their randomness from `RoundEndRandomPort`.
  */
object TurnBoundary:
  def finishRound(catalog: ExecutableCatalog, transition: OathTransition,
      randomPort: RoundEndRandomPort,
      restrictions: Vector[OperationRestriction])
      : Either[OathViolation, OathTransition] =
    StateBasedEvaluation.endRound(transition.state, randomPort)
      .flatMap(_.foldLeft[Either[OathViolation, OathTransition]](
        Right(transition)) {
        case (Right(current), event) =>
          StateBasedEvaluation.evolve(catalog, current.state, event,
            restrictions).map { next =>
            current.copy(state = next, events = current.events :+ event)
          }
        case (failure @ Left(_), _) => failure
      })
