package oathdigital.frontend

import oathdigital.protocol.ChronicleSummary

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
    (headline(saved.entry.summary) :+ s"saved ${saved.savedAt.take(10)}")
      .mkString(" · ")

  private def headline(summary: ChronicleSummary): Vector[String] = Vector(
    summary.game.fold("Game unknown")(game => s"Game $game"),
    s"Era: ${summary.eraTarget} to win")
