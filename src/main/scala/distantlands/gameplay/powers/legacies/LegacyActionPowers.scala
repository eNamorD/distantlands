package distantlands.gameplay.powers.legacies

import distantlands.gameplay.powerresolver.PhasePower

/** The legacies' ACTION powers (Legacy in-play powers design, slice 1),
  * registered by `PhasePowerCatalog`. Each is offered with its legacy as
  * its source (ruling 3). */
object LegacyActionPowers:
  val all: Vector[PhasePower] = Vector(Ruthless, Peacemaker, GoldenKey)
