package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.walker.ProcedureStart
import distantlands.model._

/** The winner's Start Chronicle decision (Chronicle steps design, "Explicit
  * start"). The driver starts it once the game has a result, so the winner
  * can look at the final board before the Chronicle changes it. Only the
  * winner owns the decision; everyone else waits on it. Answering it enters
  * the Chronicle phase.
  */
object ChronicleBeginProcedure:
  val decisionId: String = "chronicle.begin"
  val start: DecisionOptionRef.Button =
    DecisionOptionRef.Button("chronicle.start")
  val heading: String = "Start Chronicle"

  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- ProcedureStart.noSelection("Start Chronicle", args)
      result <- ready.game.current.result.toRight(
        OathViolation.InvalidEventOrder("the game has no result"))
    yield Sequence(Vector(
      Decide(decisionId, result.winner, DecisionQuery.ChooseOne(
        Vector(DecisionOption.Button(start, heading)),
        heading = Some(heading)),
        presentation = DecisionPresentation.Panel),
      BuildOps((_, _) => Right(Vector(EnterPhase(Phase.Chronicle))),
        required = true)))
