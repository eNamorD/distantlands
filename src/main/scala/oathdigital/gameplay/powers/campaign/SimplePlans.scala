package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powerresolver.ContributingPower

/** The battle plans that change the dice, or pay after the Campaign, and need
  * nothing beyond the plan window: Mercenaries, Wrestlers, Fearsome Shield, the
  * two faces of the Rampart, Battle Honors, Longbows, Black Sword, Fire
  * Talkers, Nature Worship, Cracked Sage, Village Constable, Banner Breakers,
  * Cracking Ground, Walled Garden, Bandit Standard, Extra Provisions,
  * Encirclement, Disgraced Captain, Battle Axes, Field Promotion, Tribute
  * Spoils, Military Parade, Scouts, Zealots (the sacrifice's worth recorded
  * when applied), Bear Traps, Signal Trees (the top of a discard pile revealed
  * and discarded) and Second Wind, registered together. Each names its
  * card, so all are always present.
  */
object SimplePlans:
  def forCatalog(catalog: ExecutableCatalog): Vector[ContributingPower] =
    Vector(Mercenaries, Wrestlers, FearsomeShield,
      ToweringRampart, CrackedRampart, SpiralCastle, SpiralDungeons,
      BattleHonors, Longbows, BlackSword,
      FireTalkers, NatureWorship.forCatalog(catalog),
      CrackedSage.forCatalog(catalog), VillageConstable, BannerBreakers,
      CrackingGround, WalledGarden.forCatalog(catalog), BanditStandard,
      ExtraProvisions, Encirclement, DisgracedCaptain.forCatalog(catalog),
      BattleAxes.forCatalog(catalog), FieldPromotion,
      TributeSpoils.forCatalog(catalog), MilitaryParade.forCatalog(catalog),
      Scouts, SecondWind, Zealots, BearTraps,
      SignalTrees.forCatalog(catalog))
