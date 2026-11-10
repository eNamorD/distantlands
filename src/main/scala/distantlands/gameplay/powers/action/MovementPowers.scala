package distantlands.gameplay.powers.action

import distantlands.gameplay.powerresolver.PhasePower

/** The phase powers of slice 1d, registered by
  * [[distantlands.gameplay.powers.PhasePowerCatalog]] through this one object,
  * like [[DiceAndRelicDrawPowers]] and [[TargetPowers]].
  */
object MovementPowers:
  val all: Vector[PhasePower] =
    Vector[PhasePower](Whistle, MagicCarpet, BrassHorse,
      Palanquin)
