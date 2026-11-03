package oathdigital.gameplay.powers.campaign

import oathdigital.gameplay.powerresolver.ContributingPower

/** The battle plans that reach beyond the plan window, the rules that tax them,
  * and the persistent rules of a Campaign, registered together: Sticky Fire (a
  * question in the losses), Warning Signals (a decision of its own and a discard
  * at the end), Gleaming Armor and Insect Swarm (an added cost on the enemy's
  * plans), Horse Archers, Storm Caller, Rival Khan, Great Crusade and Pledge of
  * Defense (a discard at the end), Bag of Siegeworks (the defense scored again),
  * Rain Boots (the defense scored again and a discard at the end), The Great
  * Levy (the attack scored again), Garrison Armory (the targets' warbands added
  * again), Hospital (killed warbands saved until the end), Book Burning (secrets
  * burnt at the end), Rusting Ray and Fae Battalion (the attack scored again),
  * War Tortoise (the attack or the defense scored again), Lancers (the attack
  * scored again and a discard at the end), Fearsome General (the attack scored
  * again), Weeping Banner (the attack scored again, two warbands lost per
  * skull), Kindred Warriors and Rangers (the attack scored again), Mounted
  * Patrol (the attack pool halved before the roll and a discard at the end),
  * Shield Wall (the defender's whole force killed), Billowing Fog and Traveling
  * Doctor (the defeat's losses cancelled), Cursed Cauldron (warbands for the
  * enemy's dead), Salt the Earth (a targeted site's cards buried), Sealing Ward
  * (defense dice for targeted relics), Unstable Summon (an amount asked when
  * applied), Arcane Armor (defense dice set before the roll), Mountain Giant
  * (an amount asked when applied, and a discard at the end on three),
  * Herald (a favor after another player's Campaign), Sneak Attack (a
  * Campaign offered back against its attacker) and Vow of Union
  * (warbands moved at the start of any Campaign), Keep (a Raid's defeat
  * replaced) and Relic Hunter (relics targeted when applied, taken in a
  * won Conquest's placement). Each names its card, so all are always present.
  */
object PlanRules:
  val all: Vector[ContributingPower] =
    Vector(StickyFire, ImperialTactics, WarningSignals, GleamingArmor,
      InsectSwarm, HorseArchers, StormCaller, RivalKhan,
      GreatCrusade, PledgeOfDefense,
      BagOfSiegeworks, RainBoots, GreatLevy, GarrisonArmory, Hospital,
      BookBurning, RustingRay, FaeBattalion, WarTortoise, Lancers,
      WeepingBanner, FearsomeGeneral, KindredWarriors,
      Rangers, MasterAtArms, UnstableSummon, ArcaneArmor,
      MountainGiant, MountedPatrol, ShieldWall,
      BillowingFog, TravelingDoctor, CursedCauldron,
      SaltTheEarth, SealingWard, Herald, SneakAttack,
      VowOfUnion, KeepRule, RelicHunter, Glamor)
