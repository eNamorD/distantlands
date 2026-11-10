package distantlands.application.gamelog

import distantlands.application.{GameHistory, GamePresentationProjector}
import distantlands.catalog.ExecutableCatalog
import distantlands.model.PlayerId
import distantlands.protocol.projection.LogPage

/** The log page one seat reads: the whole prefix formatted for that seat,
  * then every entry at or after `after` (game log design, "Routes" and
  * "Cost"). The formatter's entries are the wire's, so this projector maps
  * nothing. */
private[application] final class GameLogProjector(catalog: ExecutableCatalog,
    presentation: GamePresentationProjector):
  private val formatter = new GameLogFormatter(catalog, presentation)

  def page(gameId: String, history: GameHistory, after: Long,
      viewer: Option[PlayerId]): Option[LogPage] =
    Option.when(after >= 0 && after <= history.nextSequence)(LogPage(
      gameId, after, history.nextSequence,
      formatter.format(history.steps, viewer)
        .filter(_.sequence >= after)))
