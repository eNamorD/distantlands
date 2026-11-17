package distantlands.frontend

import distantlands.protocol.{ChronicleLegacy, ChronicleSummary}
import scala.scalajs.js

/** How the Chronicle pages word a Chronicle's summary. */
private[frontend] object ChronicleText:
  /** A generated lineage is `<color>-lineage`; it reads as its color. */
  def lineage(id: String): String = id.stripSuffix("-lineage").capitalize

  /** "Game 3 · Era: First to 20": the scores are the scoresheet's. */
  def summaryLine(summary: ChronicleSummary): String =
    headline(summary).mkString(" · ")

  /** "Seer, Iron Hand (dormant)": a lineage's legacies, the dormant ones
    * marked (Legacies implementation ruling R39). */
  def legacies(held: Vector[ChronicleLegacy]): String =
    held.map(legacy =>
      if legacy.active then legacy.name else s"${legacy.name} (dormant)")
      .mkString(", ")

  /** "Game 3 · Era: First to 20 · saved 2026-10-29". */
  def rowDetails(saved: SavedChronicle): String =
    (headline(saved.entry.summary) :+ s"saved ${localDate(saved.savedAt)}")
      .mkString(" · ")

  /** The reader's own date for an ISO timestamp, as YYYY-MM-DD; a timestamp
    * that does not parse is shown by its first ten characters. */
  def localDate(timestamp: String): String =
    val date = new js.Date(timestamp)
    if date.getTime().isNaN then timestamp.take(10)
    else f"${date.getFullYear().toInt}%04d-${date.getMonth().toInt + 1}%02d-" +
      f"${date.getDate().toInt}%02d"

  private def headline(summary: ChronicleSummary): Vector[String] = Vector(
    summary.game.fold("Game unknown")(game => s"Game $game"),
    s"Era: First to ${summary.eraTarget}")
