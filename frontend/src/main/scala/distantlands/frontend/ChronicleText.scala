package distantlands.frontend

import distantlands.protocol.ChronicleSummary
import scala.scalajs.js

/** How the Chronicle pages word a Chronicle's summary. */
private[frontend] object ChronicleText:
  /** A generated lineage is `<color>-lineage`; it reads as its color. */
  def lineage(id: String): String = id.stripSuffix("-lineage").capitalize

  /** "Game 3 · Era: 20 to win · Red 9, Blue 6". */
  def summaryLine(summary: ChronicleSummary): String =
    (headline(summary) ++ Option.when(summary.scores.nonEmpty)(
      summary.scores.map(score => s"${lineage(score.lineage)} ${score.score}")
        .mkString(", "))).mkString(" · ")

  /** "Game 3 · Era: 20 to win · saved 2026-10-29". */
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
    s"Era: ${summary.eraTarget} to win")
