package oathdigital.gameplay.powers

import oathdigital.gameplay.powerresolver.{PhasePower, PhasePowers}
import oathdigital.gameplay.powers.action.{DiceAndRelicDrawPowers, Elders, MagicWaterskin, MovementPowers, OtherActionPowers, SelfActionPowers, TargetPowers, WaysideInn,
  WorldActionPowers}
import oathdigital.gameplay.powers.banner.BannerFacePowers
import oathdigital.gameplay.powers.cardplay.VowOfObedience
import oathdigital.gameplay.powers.economy.VowOfPoverty
import oathdigital.gameplay.powers.rest.{Insomnia, SilverTongue}
import oathdigital.gameplay.powers.action.StonePortal
import oathdigital.gameplay.powers.wake.{HiddenPassages, Hunger,
  MarbleFountains, Quartermaster, RiverSitePower, SinkingPassages}

/** The production phase powers, beside [[WalkerPowerCatalog]]. Every power
  * names its card, so all are always present.
  */
object PhasePowerCatalog:
  val default: PhasePowers =
    PhasePowers(Vector(SilverTongue) ++
      Vector(Insomnia) ++
      Vector(VowOfObedience) ++
      Vector(VowOfPoverty) ++
      Vector[PhasePower](WaysideInn, Elders, MagicWaterskin, MarbleFountains,
        Quartermaster, StonePortal, SinkingPassages, HiddenPassages) ++
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
