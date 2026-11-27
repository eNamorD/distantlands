package distantlands.frontend

import distantlands.protocol.projection.ChronicleDetailsProjection
import org.scalajs.dom
import ServerUiSupport.{element, text}

/** What Chronicle Details shows, top to bottom (Chronicle Details design,
  * slice 4): the quiet line under the heading, the Foundations, the era
  * scorepad, the Atlas's Recent site and the Chronicle Phase aids.
  * Read-only: every card is drawn with `BoardOptions.none`, so a parked
  * decision marks nothing here. */
private[frontend] object ChronicleDetailsContent:
  /** The row the Atlas's site is drawn in. Every mat row lays out its face
    * and slots alike, so the Cradle's first serves. */
  private val AtlasRow = MatLayout.siteRowWithCard(0, 0)

  def render(value: GameProjection): dom.Element =
    val body = element("div", "chronicle-details-body")
    value.chronicleDetails.foreach { details =>
      body.appendChild(text("p", "chronicle-details-subtitle",
        s"Chronicle Details · Era target ${details.eraTarget}"))
      body.appendChild(FoundationStrip.render(
        FoundationStrip.ofGame(value.foundations)))
      body.appendChild(scorepad(details))
      body.appendChild(atlas(details))
      body.appendChild(ChronicleTaskStrip.render())
    }
    body

  private def section(className: String, heading: String): dom.Element =
    val node = element("section", className)
    node.setAttribute("aria-label", heading)
    node.appendChild(text("h3", "chronicle-details-title", heading))
    node

  private def scorepad(details: ChronicleDetailsProjection): dom.Element =
    val node = section("chronicle-details-scorepad", "Era Scorepad")
    node.appendChild(ChronicleScoresheet.render(
      ChronicleScoresheet.of(details)))
    node

  /** The Recent site in a `.mat-board` box, so the map's site rules apply,
    * at the width and ratio a row has on the map at 100% (implementation
    * rulings R12 and R13). No pawns, no festival marker, no site-power
    * links: its powers are read in the inspector, not used. */
  private def atlas(details: ChronicleDetailsProjection): dom.Element =
    val node = section("chronicle-details-atlas", "Atlas · Recent")
    details.atlasRecent match
      case Some(site) =>
        val frame = element("div", "chronicle-details-atlas-frame")
        val board = element("div", "mat-board chronicle-details-atlas-board")
        val style = board.asInstanceOf[dom.html.Element].style
        val width = AtlasRow.w / MatLayout.Width * MatLayout.DesignWidthPx
        style.setProperty("--atlas-w", f"$width%.2fpx")
        // Two decimals each, as the width: one form on every platform.
        style.setProperty("--atlas-ratio",
          f"${AtlasRow.w}%.2f / ${AtlasRow.h}%.2f")
        board.appendChild(WorldBoardRenderer.siteRow(site, 0, 0, AtlasRow,
          BoardOptions.none, SitePowerLinks.none, Vector.empty))
        frame.appendChild(board)
        node.appendChild(frame)
      case None =>
        node.appendChild(text("p", "empty-state",
          "The Atlas holds no sites."))
    node
