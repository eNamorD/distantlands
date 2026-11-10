package distantlands.gameplay.powers.action

import distantlands.gameplay.powerresolver.PhasePower

/** The phase powers of catalog batch 3's slice 3c and catalog batch 4's
  * slices 2 and 3, actions on sites, banks and banners, registered by
  * [[distantlands.gameplay.powers.PhasePowerCatalog]] through this one object,
  * like [[OtherActionPowers]]. Each names its denizen card, so all are
  * always present.
  */
object WorldActionPowers:
  val all: Vector[PhasePower] =
    Vector[PhasePower](Storyteller, Firebrand, Levelers, MemoryOfHome,
      PlagueEngines, BanditPaymaster, TerrorSpells, AncientBinding, Convoys,
      TheRedSeer, MemoryOfNature,
      BallotBox, DarkEnforcer,
      TamingCharm, GreatFeast,
      RovingTerror, GiantPython,
      SingingMask, WildAllies,
      Captains, ObsidianCageAction)
