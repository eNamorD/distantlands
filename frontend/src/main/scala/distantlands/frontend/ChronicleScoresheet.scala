package distantlands.frontend

import distantlands.model.PlayerColor
import distantlands.protocol.{ChronicleLegacy, ChronicleScorepadEntry,
  ChronicleSummary}
import distantlands.protocol.projection.ChronicleDetailsProjection
import org.scalajs.dom
import ServerUiSupport.{element, text}

/** One lineage's score in one game's column: its running total, and its
  * Imperial role at that game's end, if any. */
private[frontend] final case class ScoresheetMark(score: Int,
    role: Option[String])

/** A lineage's legacies as a scoresheet row shows them: named in words on
  * the Chronicle page, as chips that open the card in Chronicle Details
  * (Chronicle Details design, slice 4). */
private[frontend] enum ScoresheetLegacies:
  case Named(held: Vector[ChronicleLegacy])
  case Cards(held: Vector[CardDetails])

/** One lineage's row: its cells in the shown columns, `None` where it sat
  * a game out, and its current total. */
private[frontend] final case class ScoresheetRow(lineage: String,
    legacies: ScoresheetLegacies, cells: Vector[Option[ScoresheetMark]],
    total: Int)

/** The era scorepad as the Chronicle page shows it: the last games' columns
  * by game number, and whether earlier ones are left out. */
private[frontend] final case class Scoresheet(earlierHidden: Boolean,
    games: Vector[Int], rows: Vector[ScoresheetRow])

/** The loaded Chronicle's scoresheet (Chronicle browsing design, "Chronicle
  * page"): a row per lineage, a column for each of the last five games
  * holding the running total, the Chancellor's score circled and Citizens'
  * underlined as on the paper scorepad, then the total. */
private[frontend] object ChronicleScoresheet:
  val ShownGames = 5

  def of(summary: ChronicleSummary): Scoresheet =
    sheet(summary.scorepad, summary.scores.map(score => (score.lineage,
      ScoresheetLegacies.Named(score.legacies), score.score)))

  /** The live scorepad of the game in play, its legacies as the viewer may
    * see them (Chronicle Details design, slice 4). */
  def of(details: ChronicleDetailsProjection): Scoresheet =
    sheet(details.scorepad, details.lineages.map(lineage =>
      (lineage.lineage, ScoresheetLegacies.Cards(lineage.legacies),
        lineage.score)))

  private def sheet(scorepad: Vector[Vector[ChronicleScorepadEntry]],
      lineages: Vector[(String, ScoresheetLegacies, Int)]): Scoresheet =
    val shown = scorepad.zipWithIndex.takeRight(ShownGames)
    Scoresheet(scorepad.size > ShownGames, shown.map(_._2 + 1),
      lineages.map((lineage, legacies, total) => ScoresheetRow(lineage,
        legacies, shown.map { case (column, _) =>
          column.find(_.lineage == lineage)
            .map(mark => ScoresheetMark(mark.score, mark.role))
        }, total)))

  def render(sheet: Scoresheet): dom.Element =
    val table = element("table", "chronicle-scoresheet")
    table.setAttribute("aria-label", "Scoresheet")
    val head = element("tr", "")
    head.appendChild(column("Lineage"))
    if sheet.earlierHidden then
      val blank = element("th", "scoresheet-earlier")
      blank.setAttribute("aria-label", "Previous games")
      head.appendChild(blank)
    sheet.games.foreach { game =>
      // A phone hides the word from sight, not from a screen reader.
      val heading = column("")
      heading.appendChild(text("span", "scoresheet-game-word", "Game "))
      heading.appendChild(dom.document.createTextNode(game.toString))
      head.appendChild(heading)
    }
    head.appendChild(column("Total"))
    table.appendChild(element("thead", "")).appendChild(head)
    val body = table.appendChild(element("tbody", ""))
    sheet.rows.foreach { row =>
      val line = element("tr", "")
      line.appendChild(lineageCell(row))
      if sheet.earlierHidden then
        val earlier = text("td", "scoresheet-earlier", "…")
        HoverTip.set(earlier, "Previous games not shown")
        line.appendChild(earlier)
      row.cells.foreach(cell => line.appendChild(scoreCell(cell)))
      line.appendChild(text("td", "scoresheet-total", row.total.toString))
      body.appendChild(line)
    }
    // A sheet wider than a narrow box scrolls inside it, not the page.
    val frame = element("div", "scoresheet-frame")
    frame.appendChild(table)
    frame

  private def column(words: String): dom.Element =
    val node = text("th", "", words)
    node.setAttribute("scope", "col")
    node

  private def lineageCell(row: ScoresheetRow): dom.Element =
    val cell = element("th", "scoresheet-lineage")
    cell.setAttribute("scope", "row")
    val name = element("span", "scoresheet-lineage-name")
    val color = PlayerColor.fromKey(row.lineage.stripSuffix("-lineage"))
    name.appendChild(element("span", s"color-swatch ${PlayerColorCss.of(color)}"))
    name.appendChild(text("span", "", ChronicleText.lineage(row.lineage)))
    cell.appendChild(name)
    row.legacies match
      case ScoresheetLegacies.Named(held) if held.nonEmpty =>
        cell.appendChild(text("span", "scoresheet-legacies",
          ChronicleText.legacies(held)))
      case ScoresheetLegacies.Cards(held) if held.nonEmpty =>
        val chips = element("span", "scoresheet-legacy-chips")
        held.foreach(card => chips.appendChild(LegacyStrip.chip(card)))
        cell.appendChild(chips)
      case _ => ()
    cell

  private def scoreCell(cell: Option[ScoresheetMark]): dom.Element =
    val node = element("td", "scoresheet-score")
    cell.foreach { mark =>
      val score = text("span", "scoresheet-mark", mark.score.toString)
      mark.role.foreach { role =>
        score.classList.add(s"scoresheet-$role")
        HoverTip.set(score, role.capitalize)
        score.setAttribute("aria-label", s"${mark.score}, ${role.capitalize}")
      }
      node.appendChild(score)
    }
    node
