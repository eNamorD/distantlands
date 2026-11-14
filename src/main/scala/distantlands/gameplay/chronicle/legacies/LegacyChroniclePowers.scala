package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.powerresolver.ContributingPower

/** The legacies whose Chronicle powers are implemented (Legacy Chronicle
  * powers design), registered on the walker by `WalkerPowerCatalog`. */
object LegacyChroniclePowers:
  val all: Vector[ContributingPower] = Vector(Steadfast)
