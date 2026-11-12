package distantlands.application

import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Unwrap.accepted

/** The map as the table draws it: each region's sites in slot order, the
  * slots that hold none (Foundations design, slice 3), and the site the
  * festival marker lies over (slice 6). */
class GamePresentationProjectorMapSuite extends munit.FunSuite:
  private val projector = new GamePresentationProjector(catalog)
  private val p1 = PlayerId("p1")

  test("a full map projects eight sites and no empty slot"):
    val world = projector.readyWorld(FirstGameSetupFixture.freshReady,
      Some(p1))
    assertEquals(world.map(_.regionId),
      Vector("cradle", "provinces", "hinterland"))
    assertEquals(world.map(_.sites.size), Vector(2, 3, 3))
    assertEquals(world.map(_.emptySlots), Vector(0, 0, 0))

  test("a Wide Horizons map projects each region's sites and the slots " +
      "past them"):
    // l1 is the Chancellor of an Empire of four sites, and no slot past
    // them is filled.
    val record = LaterGameFixture.chronicle(LineageId("l1"), world = 4).copy(
      foundations = Map(FoundationNumber.I ->
        FoundationState(FoundationFace.Altered, Set.empty)))
    val ready = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    val world = projector.readyWorld(ready, Some(p1))
    assertEquals(world.flatMap(_.sites.map(_.siteId)),
      record.world.map(_.site.value))
    assertEquals(world.map(_.sites.size), Vector(2, 2, 0))
    assertEquals(world.map(_.emptySlots), Vector(0, 1, 3))

  test("the festival marker is projected on the one site it lies over, " +
      "and on none while it is beside the map"):
    def marked(table: Table): Vector[String] =
      projector.readyWorld(table.ready, Some(p1)).flatMap(_.sites)
        .filter(_.festival).map(_.siteId)
    val festival = Table.start.darkestSecret(Some(p1), 1,
      DarkestSecretFace.Festival)
    assertEquals(marked(festival.festival(BrokenPeaksSite)),
      Vector(BrokenPeaksSite.id.value))
    assertEquals(marked(festival), Vector.empty)
