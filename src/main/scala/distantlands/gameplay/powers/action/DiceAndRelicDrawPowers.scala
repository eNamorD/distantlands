package distantlands.gameplay.powers.action

import distantlands.gameplay.powerresolver.PhasePower

/** The phase powers of slice 1b, registered by [[distantlands.gameplay.powers.PhasePowerCatalog]]
  * through this one object so that later slices add their own group without
  * editing the same lines.
  */
object DiceAndRelicDrawPowers:
  val powers: Vector[PhasePower] =
    Vector[PhasePower](GamblingHall, BoneDice, MurkyFountain, DowsingSticks,
      FaeMerchant)
