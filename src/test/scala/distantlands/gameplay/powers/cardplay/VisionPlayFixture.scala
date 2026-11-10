package distantlands.gameplay.powers.cardplay

import distantlands.gameplay.FacedownAdviserPlay
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.powers.{PowerFixture, SearchFixture,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{PlacementPicks, Table}

/** The two ways to play a Vision, each stopped at its placement decision, for
  * the suites of the cards that forbid or reward a faceup Vision.
  */
object VisionPlayFixture:
  import PowerFixture._

  private def orFail(result: Either[OathViolation, OathTransition])
      : OathTransition =
    result.fold(error => throw new AssertionError(error.toString), identity)

  /** A Search that drew `vision`, parked on its placement. `ready` must come
    * from `SearchFixture.staged(Vector(vision), ...)`. The events are the whole
    * Search's so far: with one card drawn the placement is reached in the
    * command that starts it, and its notes are journaled there.
    */
  def searched(ready: ReadyGame, vision: VisionId): OathTransition =
    orFail(SearchFixture.start(ready).flatMap(started =>
      SearchFixture.keep(started, vision).map(kept =>
        kept.copy(events = started.events ++ kept.events))))

  /** The Play-Facedown-Adviser action on `vision`, which the actor is given as
    * a facedown adviser, parked on its placement. `ready` must be in the Act
    * phase.
    */
  def fromAdvisers(ready: ReadyGame, vision: VisionId)(
      using munit.Location): OathTransition =
    val holding = Table.from(ready).adviser(actor, vision, facedown = true)
      .ready
    orFail(FacedownAdviserPlay.parkedOnPlacement(SearchFixture.rules,
      Ready(holding), actor, vision))

  /** How the options of the placement the walk is parked on play the card,
    * after the restriction look-ahead. */
  def offered(from: OathTransition)(using munit.Location): Vector[PlayedAs] =
    PlacementPicks.offered(PlacementPicks.parkedOptions(from.state,
      WalkerPowerCatalog.default(catalog)))
