package distantlands.gameplay.powers.campaign

import distantlands.gameplay.powerresolver.ContributingPower

/** The battle plans and rules that restrict which plans a side of a
  * Campaign may choose (catalog batch 6, slice 1), registered together:
  * Specialist, Code of Honor, Marsh Spirit, Beast Tamer and True Names.
  * Each names its card, so all are always present.
  */
object PlanRestrictions:
  val all: Vector[ContributingPower] =
    Vector(Specialist, CodeOfHonor, MarshSpirit,
      BeastTamer, TrueNames)
