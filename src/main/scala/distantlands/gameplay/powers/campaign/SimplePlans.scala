package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.powerresolver.ContributingPower

/** The battle plans that change the dice, or pay after the Campaign, and need
  * nothing beyond the plan window: Mercenaries, Wrestlers, Fearsome Shield, the
  * two faces of the Rampart, Battle Honors, Longbows, Black Sword, Fire
  * Talkers, Nature Worship, Cracked Sage, Village Constable, Banner Breakers,
  * Cracking Ground, Walled Garden, Bandit Standard, Extra Provisions,
  * Encirclement, Disgraced Captain, Battle Axes, Field Promotion, Tribute
  * Spoils, Military Parade, Scouts, Zealots (the sacrifice's worth recorded
  * when applied), Bear Traps, Signal Trees (the top of a discard pile revealed
  * and discarded), Second Wind, the Grand Scepter and Martial Culture,
  * registered together. Each names its card, so all are always present.
  */
object SimplePlans:
  val all: Vector[ContributingPower] =
    Vector(Mercenaries, Wrestlers, FearsomeShield,
      ToweringRampart, CrackedRampart, SpiralCastle, SpiralDungeons,
      BattleHonors, Longbows, BlackSword,
      FireTalkers, NatureWorship,
      CrackedSage, VillageConstable, BannerBreakers,
      CrackingGround, WalledGarden, BanditStandard,
      ExtraProvisions, Encirclement, DisgracedCaptain,
      BattleAxes, FieldPromotion,
      TributeSpoils, MilitaryParade,
      Scouts, SecondWind, Zealots, BearTraps,
      SignalTrees, GrandScepterPlan, MartialCulture)
