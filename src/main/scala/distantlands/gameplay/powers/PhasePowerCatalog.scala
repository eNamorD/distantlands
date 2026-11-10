package distantlands.gameplay.powers

import distantlands.gameplay.powerresolver.{PhasePower, PhasePowers}
import distantlands.gameplay.powers.action.{DiceAndRelicDrawPowers, Elders,
  GrandScepterAction, ImperialSeal, MagicWaterskin, MovementPowers,
  OtherActionPowers, SelfActionPowers, StonePortal, TargetPowers, WaysideInn,
  WorldActionPowers}
import distantlands.gameplay.powers.banner.BannerFacePowers
import distantlands.gameplay.powers.cardplay.VowOfObedience
import distantlands.gameplay.powers.economy.VowOfPoverty
import distantlands.gameplay.powers.rest.{Insomnia, Naysayers, SilverTongue}
import distantlands.gameplay.powers.wake.{HiddenPassages, Hunger,
  MarbleFountains, Quartermaster, RiverSitePower, SinkingPassages,
  TrackerAction, TrackerWake}

/** The production phase powers, beside [[WalkerPowerCatalog]]. Every power
  * names its card, so all are always present.
  */
object PhasePowerCatalog:
  val default: PhasePowers =
    PhasePowers(Vector(SilverTongue) ++
      Vector(Insomnia, Naysayers) ++
      Vector(VowOfObedience) ++
      Vector(VowOfPoverty) ++
      Vector[PhasePower](WaysideInn, Elders, MagicWaterskin, MarbleFountains,
        GrandScepterAction, ImperialSeal,
        Quartermaster, StonePortal, SinkingPassages, HiddenPassages,
        TrackerAction, TrackerWake) ++
      DiceAndRelicDrawPowers.powers ++
      TargetPowers.all ++
      MovementPowers.all ++
      SelfActionPowers.all ++
      OtherActionPowers.all ++
      WorldActionPowers.all ++
      BannerFacePowers.phasePowers ++
      Vector(Hunger) ++
      RiverSitePower.all,
      WalkerPowerCatalog.useRestrictions)
