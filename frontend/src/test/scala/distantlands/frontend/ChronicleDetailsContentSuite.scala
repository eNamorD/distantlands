package distantlands.frontend

import Dom.{all, one}
import TestSites.*
import distantlands.protocol.projection.{ChronicleDetailsProjection,
  FoundationProjection}
import org.scalajs.dom

/** What Chronicle Details shows, top to bottom (Chronicle Details design,
  * slice 4): the quiet line, the Foundations, the era scorepad, the
  * Atlas's Recent site and the Chronicle Phase aids. */
class ChronicleDetailsContentSuite extends munit.FunSuite:
  private val deepWoodsStoringAKnownDenizen = quietSite("site:deep-woods",
    "Deep Woods").withDenizens(GameSiteCard("208", "Wizard's Conclave",
      Some(CardDetails("208", "denizen", "Wizard's Conclave",
        orientation = Some("face-up")))))

  /** A game with one foundation, no game on the scorepad, no lineage, and
    * `recent` at the Atlas's Recent end. */
  private def gameWithRecent(recent: Option[GameSite]): GameProjection =
    TestProjections.actingProjection().copy(
      foundations = Vector(FoundationProjection(1, "normal",
        "Imperial Maps", "Fill the map.")),
      chronicleDetails = Some(ChronicleDetailsProjection(20, Vector.empty,
        Vector.empty, recent)))

  test("the five sections stand in order, the quiet line naming the era " +
      "target"):
    val body = ChronicleDetailsContent.render(
      gameWithRecent(Some(deepWoodsStoringAKnownDenizen)))
    assertEquals(all(body, ":scope > *").map(_.getAttribute("class")),
      Vector("chronicle-details-subtitle", "foundations",
        "chronicle-details-scorepad", "chronicle-details-atlas",
        "chronicle-tasks"))
    assertEquals(one(body, ".chronicle-details-subtitle").textContent,
      "Chronicle Details · Era target 20")
    assertEquals(all(body, ":scope > * > h3").map(_.textContent),
      Vector("Foundations", "Era Scorepad", "Atlas · Recent",
        "Chronicle Phase"))

  test("the Recent site is a mat row in a mat-board box of the row's width " +
      "and ratio (R12)"):
    val body = ChronicleDetailsContent.render(
      gameWithRecent(Some(deepWoodsStoringAKnownDenizen)))
    val board = one(body,
      ".chronicle-details-atlas-frame > .chronicle-details-atlas-board")
    // The site rules apply under a .mat-board.
    assert(board.matches(".mat-board"), board.getAttribute("class"))
    val style = board.asInstanceOf[dom.html.Element].style
    // A row on the mat is 1789.15 of its 5787 pixels wide and a card's
    // 509 tall; the board is 2400px wide at 100%.
    assertEquals(style.getPropertyValue("--atlas-w"), "742.00px")
    assertEquals(style.getPropertyValue("--atlas-ratio"), "1789.15 / 509.00")
    val site = one(board, ":scope > article.site")
    assertEquals(site.getAttribute("aria-label"), "Deep Woods")
    assertEquals(all(site, ".site-pawns > *").size, 0)

  test("an empty Atlas reads that it holds no sites"):
    val atlas = one(ChronicleDetailsContent.render(gameWithRecent(None)),
      ".chronicle-details-atlas")
    assertEquals(all(atlas, ".chronicle-details-atlas-board").size, 0)
    assertEquals(one(atlas, "p.empty-state").textContent,
      "The Atlas holds no sites.")
