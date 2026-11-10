package distantlands.gameplay.powers.travel

import distantlands.gameplay.powerresolver.ContributingPower

/** The Travel modifiers and rules that are not terrain, registered together.
  * Each names its card, so all are always present. Terrain is
  * [[TravelSitePowers]].
  */
object TravelModifiers:
  val all: Vector[ContributingPower] =
    Vector(Tents, RoyalStables, ForestPaths,
      DragonskinDrum, TollRoads, GraspingVines, AFastSteed, SpecialEnvoy,
      WayStation, Hospitality, Tyrant)
