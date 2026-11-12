package distantlands.gameplay.operations

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.model._
import distantlands.testkit.{RawOperationBatch, Table}
import distantlands.testkit.Unwrap.{accepted, rejected}

/** The festival marker on the map (Foundations design, slice 6):
  * `SetFestivalMarker` puts it over a site's cards or takes it off. */
class FestivalMarkerOperationsSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id

  private def run(ready: ReadyGame, ops: CoreOperation*) =
    RawOperationBatch.executeAll(ready, ops.toVector)

  /** The quiet table with the Darkest Secret unheld on its Festival face,
    * its marker beside the map. */
  private def festivalUnplaced: Table =
    Table.start.darkestSecret(None, 1, DarkestSecretFace.Festival)

  private def marker(ready: ReadyGame): Option[SiteId] =
    ready.game.current.banners.darkestSecret.festival

  test("placing the marker puts it over a site on the map, and placing it " +
      "again moves it"):
    val placed = accepted(run(festivalUnplaced.ready,
      SetFestivalMarker(Some(home))))
    assertEquals(marker(placed), Some(home))
    assertEquals(marker(accepted(run(placed, SetFestivalMarker(Some(peaks))))),
      Some(peaks))
    assertEquals(marker(accepted(run(placed, SetFestivalMarker(Some(home))))),
      Some(home))

  test("removing takes it off the map, and needs it on the map"):
    val placed = festivalUnplaced.festival(home).ready
    val removed = accepted(run(placed, SetFestivalMarker(None)))
    assertEquals(marker(removed), None)
    rejected(run(removed, SetFestivalMarker(None)))

  test("it goes only on a site on the map, and only on the Festival face"):
    rejected(run(festivalUnplaced.ready,
      SetFestivalMarker(Some(SiteId("no-such-site")))))
    rejected(run(Table.start.ready, SetFestivalMarker(Some(home))))
