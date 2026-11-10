package distantlands.gameplay.phases.rest

import distantlands.catalog.ExecutableCatalog
import distantlands.model.OathViolation._
import distantlands.model._

/** Begin Rest: the supported-state gate and the phase change, nothing else.
  *
  * Cleanup waits for Finish Rest so REST powers can be used in between.
  */
object BeginRestProcedure:
  def validateSupportedState(catalog: ExecutableCatalog,
      ready: ReadyGame): Either[OathViolation, Unit] =
    val missing = ready.game.current.players
      .flatMap(PlayerForceKind.of(ready, _)).distinct
      .filterNot(ready.banks.warbandSupply.contains)
    if missing.nonEmpty then Left(UnsupportedRestState(
      s"no bounded warband supply for ${missing.mkString(", ")}"))
    else Right(())

  /** One leaf, so it finishes in the command that starts it; resume never
    * reaches this.
    */
  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] = for
    _ <- Either.cond(args.isEmpty, (), InvalidEventOrder(
      "Begin Rest selects nothing"))
    _ <- validateSupportedState(catalog, ready)
  yield Sequence(Vector(EnterPhase(Phase.Rest)))
