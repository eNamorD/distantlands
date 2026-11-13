package distantlands.frontend

import distantlands.model.PlayerColor
import Dom.{all, one}
import TestSites.*
import org.scalajs.dom

/** The map is the mat (real proportions design): one board, with every
  * piece at the rectangle `MatLayout` gives it.
  */
class MatBoardSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val tracks = distantlands.protocol.projection.GameTracksProjection(
    round = 1, visionsDrawn = 2, usurperLimited = false, limiterRound = 4,
    firstPlayerId = "red")
  private val relic = CardDetails("r1", "relic", "Brass Horse",
    orientation = Some("face-up"))
  private val woods = quietSite("woods", "Deep Woods")
    .withDenizenCapacity(2).withRelicCapacity(1).withRelics(1, relic)
  private val table = TestProjections.actingProjection(players = Vector(red),
    world = Vector(
      GameRegion("cradle", Vector(quietSite("keep", "The Keep"))),
      GameRegion("provinces", Vector(quietSite("ford", "Old Ford"), woods),
        emptySlots = 1),
      GameRegion("hinterland", Vector.empty, emptySlots = 3))).copy(
    worldDeckCount = 53, relicDeckCount = 9, tracks = Some(tracks),
    favorBanks = MatLayout.FavorBankSuits.map(FavorBankState(_, 3)))

  private val world = WorldBoardRenderer.world(table, BoardOptions.none)

  /** The four placement properties of `node`, as its inline style has
    * them. */
  private def box(node: dom.Element): Vector[(String, String)] =
    val style = node.asInstanceOf[dom.html.Element].style
    Vector("left", "top", "width", "height")
      .map(name => name -> style.getPropertyValue(name))

  private def at(rect: MatRect, parent: MatRect = MatLayout.Mat)
      : Vector[(String, String)] = rect.percentOf(parent)

  test("the world is the mat board and then the table's edge"):
    assertEquals(all(world, ":scope > *").map(_.getAttribute("class")),
      Vector(null, "mat-board", "table-edge"))
    assertEquals(all(one(world, ".mat-board"), ":scope > *")
      .map(_.getAttribute("class")), Vector("regions", "favor-banks"))

  test("a site is placed at its row, with its face first"):
    val site = one(world, """.site[aria-label="Deep Woods"]""")
    val row = MatLayout.siteRow(1, 1)
    assert(site.classList.contains("mat-slot"))
    assertEquals(box(site), at(row))
    val face = site.firstElementChild
    assert(face.classList.contains("site-face"))
    assertEquals(box(face), at(MatLayout.siteCard(1, 1), row))
    val style = site.asInstanceOf[dom.html.Element].style
    val faceBox = at(MatLayout.siteCard(1, 1), row).toMap
    assertEquals(style.getPropertyValue("--face-left"), faceBox("left"))
    assertEquals(style.getPropertyValue("--face-w"), faceBox("width"))

  test("a site's cards stand in the three slots to its right, a relic " +
      "in the square top of its slot"):
    val site = one(world, """.site[aria-label="Deep Woods"]""")
    val row = MatLayout.siteRow(1, 1)
    val cards = all(site, ".site-cards > *")
    assertEquals(cards.size, 3)
    assertEquals(box(cards(0)), at(MatLayout.cardSlot(1, 1, 0), row))
    assertEquals(box(cards(1)), at(MatLayout.cardSlot(1, 1, 1), row))
    assertEquals(box(cards(2)), at(MatLayout.relicSlot(1, 1, 2), row))

  test("an empty site slot is the mat's frame"):
    val frames = all(world, ".site-slot-empty")
    assertEquals(frames.size, 4)
    assertEquals(box(frames(0)), at(MatLayout.siteFrame(1, 2)))
    assertEquals(box(frames(1)), at(MatLayout.siteFrame(2, 0)))

  test("a region's name and discard are placed in its column"):
    val provinces = one(world, """section.region[aria-label="Provinces"]""")
    assertEquals(box(one(provinces, ".region-label")),
      at(MatLayout.regionName(1)))
    assertEquals(box(one(provinces, ".region-header > .deck-figure")),
      at(MatLayout.discard(1)))

  test("the Cradle's foot holds the wheel, the track and both decks"):
    val foot = one(world, """section.region[aria-label="Cradle"] .deck-row""")
    assertEquals(all(foot, ":scope > *").map(_.classList.item(0)),
      Vector("round-tracker", "visions-drawn", "deck-figure", "deck-figure"))
    assertEquals(box(one(foot, ".round-tracker svg")),
      at(MatLayout.roundWheel))
    assertEquals(box(one(foot, ".track-note")), at(MatLayout.trackNote))
    val decks = all(foot, ":scope > .deck-figure")
    assertEquals(box(decks(0)), at(MatLayout.worldDeck))
    assertEquals(box(decks(1)), at(MatLayout.relicDeck))
    assertEquals(decks.map(deck => one(deck, ".deck-caption").textContent),
      Vector("World deck", "Relic deck"))
    assertEquals(one(decks(1), ".pile-count").textContent, "×9")

  test("Visions Drawn is a marker on the mat's six cells"):
    val track = one(world, ".visions-drawn")
    assertEquals(track.getAttribute("aria-label"), "Visions Drawn 2")
    val cells = all(track, ".vision-cell")
    assertEquals(cells.map(_.textContent), Vector("0", "1", "2", "3", "4", "5"))
    assertEquals(cells.map(box), MatLayout.visionCells.map(at(_)))
    assertEquals(box(one(track, ".vision-marker")),
      at(MatLayout.visionCells(2)))

  test("a count past the track's end stands on its last cell"):
    val many = WorldBoardRenderer.world(table.copy(tracks =
      Some(tracks.copy(visionsDrawn = 7))), BoardOptions.none)
    assertEquals(box(one(many, ".vision-marker")),
      at(MatLayout.visionCells(5)))

  test("each favor bank stands on its circle"):
    val banks = all(world, ".favor-banks > .favor-bank")
    assertEquals(banks.map(box),
      MatLayout.FavorBankSuits.flatMap(MatLayout.favorBank).map(at(_)))

  test("the shared bank is on the table's edge and holds no relic deck"):
    val bank = one(world, ".table-edge > .shared-bank")
    assertEquals(all(bank, ".pile-display"), Vector.empty)

  test("what the mat and a site print is marked printed"):
    assertEquals(all(world, ".region-label.printed").size, 3)
    val site = one(world, """.site[aria-label="Deep Woods"]""")
    assertEquals(all(site, ".site-name.printed").size, 1)
    assertEquals(all(site, ".site-defense.printed").size, 1)
    assertEquals(all(site, ".site-footer.printed").size, 1)
    assertEquals(all(site, ".site-tokens.printed, .site-pawns.printed"),
      Vector.empty)
