package distantlands.frontend

import distantlands.model.PlayerColor
import distantlands.protocol.{ChronicleLegacy, ChronicleSummary}
import org.scalajs.dom
import ServerUiSupport.{element, text}

/** One lineage's score in one game's column: its running total, and its
  * Imperial role at that game's end, if any. */
private[frontend] final case class ScoresheetMark(score: Int,
    role: Option[String])

/** One lineage's row: its cells in the shown columns, `None` where it sat
  * a game out, and its current total. */
private[frontend] final case class ScoresheetRow(lineage: String,
    legacies: Vector[ChronicleLegacy], cells: Vector[Option[ScoresheetMark]],
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
    val shown = summary.scorepad.zipWithIndex.takeRight(ShownGames)
    Scoresheet(summary.scorepad.size > ShownGames, shown.map(_._2 + 1),
      summary.scores.map(score => ScoresheetRow(score.lineage,
        score.legacies, shown.map { case (column, _) =>
          column.find(_.lineage == score.lineage)
            .map(mark => ScoresheetMark(mark.score, mark.role))
        }, score.score)))

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
    if row.legacies.nonEmpty then
      cell.appendChild(text("span", "scoresheet-legacies",
        ChronicleText.legacies(row.legacies)))
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
