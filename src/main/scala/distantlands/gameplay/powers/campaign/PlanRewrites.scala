package distantlands.gameplay.powers.campaign

import distantlands.gameplay.powerresolver.ContributingPower

/** The battle plans that rewrite the Campaign around them (catalog batch
  * 6, slice 2, and batch 7, slice 4), registered together: Peace Envoy
  * decides the battle, Wild Mounts replaces the discard of Nomad plans,
  * Careful Plans reorders the battle's steps, and Hearts and Minds decides
  * the battle for the defender. Each names its card, so all are always
  * present.
  */
object PlanRewrites:
  val all: Vector[ContributingPower] =
    Vector(PeaceEnvoy, WildMounts, CarefulPlans,
      HeartsAndMinds)
