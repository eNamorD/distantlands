package distantlands.gameplay.powers.action

import distantlands.gameplay.powerresolver.PhasePower
import distantlands.gameplay.powers.wake.HornedMask

/** The phase powers of slice 1c, registered by
  * [[distantlands.gameplay.powers.PhasePowerCatalog]] through this one object,
  * like [[DiceAndRelicDrawPowers]], so that slices add their own group without
  * editing the same lines.
  */
object TargetPowers:
  val all: Vector[PhasePower] =
    Vector[PhasePower](Wolves, Alchemist, SleightOfHand, WardOfSilence,
      CrystalVial, IvoryEye, HornedMask)
