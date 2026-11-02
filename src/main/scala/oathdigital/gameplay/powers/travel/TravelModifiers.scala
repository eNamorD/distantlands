package oathdigital.gameplay.powers.travel

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powerresolver.ContributingPower

/** The Travel modifiers and rules that are not terrain, registered together.
  * Each names its card, so all are always present. Terrain is
  * [[TravelSitePowers]]. Forest Paths and Hospitality read other cards'
  * suits, so they alone take the catalog.
  */
object TravelModifiers:
  def forCatalog(catalog: ExecutableCatalog): Vector[ContributingPower] =
    Vector(Tents, RoyalStables, ForestPaths.forCatalog(catalog),
      DragonskinDrum, TollRoads, GraspingVines, AFastSteed, SpecialEnvoy,
      WayStation, Hospitality.forCatalog(catalog), Tyrant)
