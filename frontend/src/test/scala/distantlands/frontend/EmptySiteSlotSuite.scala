package distantlands.frontend

import distantlands.model.PlayerColor
import Dom.all
import TestProjections.actingProjection
import TestSites.quietSite
import org.scalajs.dom

/** A region's site slots that hold no site, drawn as frames after its
  * sites (Foundations design, slice 3). */
class EmptySiteSlotSuite extends munit.FunSuite:
  private val woods = quietSite("site:woods", "Deep Woods")

  /** The World pane of a table whose map is `regions`. */
  private def world(regions: GameRegion*): dom.Element =
    WorldBoardRenderer.world(actingProjection(
      players = Vector(GamePlayer("red", "Red", "Exile", PlayerColor.Red)),
      world = regions.toVector), BoardOptions.none)

  /** The class of each box in the region's column of sites, top first. */
  private def slotClasses(root: dom.Element): Vector[String] =
    all(root, ".sites > *").map(_.getAttribute("class"))

  test("a region draws a frame for each slot that holds no site, after " +
      "its sites"):
    val root = world(GameRegion("provinces", Vector(woods), emptySlots = 2))
    assertEquals(slotClasses(root),
      Vector("site site-readonly mat-slot", "site-slot-empty printed mat-slot",
        "site-slot-empty printed mat-slot"))
    val frames = all(root, ".site-slot-empty")
    assertEquals(frames.map(_.getAttribute("aria-label")),
      Vector.fill(2)("Empty site slot, Provinces"))
    assertEquals(frames.map(_.getAttribute("role")), Vector.fill(2)("img"))
    assertEquals(frames.map(_.childElementCount), Vector(0, 0))
    assertEquals(frames.map(_.textContent), Vector("", ""))

  test("a region with no site draws only its frames"):
    val root = world(GameRegion("hinterland", Vector.empty, emptySlots = 3))
    assertEquals(slotClasses(root), Vector.fill(3)("site-slot-empty printed mat-slot"))
    assertEquals(all(root, ".site").size, 0)

  test("a full region draws no frame"):
    val root = world(GameRegion("cradle", Vector(woods)))
    assertEquals(slotClasses(root), Vector("site site-readonly mat-slot"))

  test("an empty frame is no board option"):
    val root = world(GameRegion("hinterland", Vector.empty, emptySlots = 1))
    assertEquals(all(root, ".site-slot-empty[data-option-ref]").size, 0)
    assertEquals(all(root, ".site-slot-empty button").size, 0)
