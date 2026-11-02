package oathdigital.gameplay.powers

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powerresolver.ContributingPower
import oathdigital.gameplay.powers.economy.{AnimalPlaymates, AwaitedReturn,
  Birdsong, CupOfPlenty, Curfew, Defame, Downtrodden, FriendlyFamiliar,
  GolemLegions, InitiationRite, MapLibrary, MountedLibrary, MovingMarket,
  OldSongs, Pressgangs, RowdyPub, SecretSignal, SkilledMerchants,
  SmallFriends, SpitefulMirror, TheOldOak, VillageIdiot, VowOfBeastkin,
  VowOfPoverty, VowOfWandering, VowOfWanderingMuster}
import oathdigital.gameplay.powers.recover.{RelicWorship, SilverCharm}
import oathdigital.gameplay.powers.search.{Augury, Baron, CropRotation,
  Disciples, ErrandBoy, ForcedLabor, ForestWarden, HuntingParty, LandWarden,
  Mushrooms, NewGrowth, NewsFromAfar, Observatory, PaintedTrumpet,
  SpinningBee, TruthfulHarp, WineOfWelcome}

/** The Search, Trade, Muster and Recover modifiers, registered together with
  * the rules that change those actions: Initiation Rite's Muster cost, the
  * tolls of Curfew and Forced Labor, the Vows' limits and extra warbands,
  * and Vow of Wandering's ban on placing warbands. Each names its card, so
  * all are always present.
  */
object ActionModifiers:
  def forCatalog(catalog: ExecutableCatalog): Vector[ContributingPower] =
    Vector(Augury) ++
      Vector(TruthfulHarp) ++
      Vector(Disciples) ++
      Vector(CropRotation) ++
      Vector(Baron) ++
      Vector(NewGrowth.forCatalog(catalog)) ++
      Vector(ForestWarden.forCatalog(catalog)) ++
      Vector(NewsFromAfar) ++
      Vector(HuntingParty) ++
      Vector(Observatory) ++
      Vector(ErrandBoy) ++
      Vector(Mushrooms) ++
      Vector(WineOfWelcome) ++
      Vector(PaintedTrumpet) ++
      Vector(SpinningBee.forCatalog(catalog)) ++
      Vector(LandWarden) ++
      Vector(CupOfPlenty.forCatalog(catalog)) ++
      Vector(AnimalPlaymates.forCatalog(catalog)) ++
      Vector(Birdsong.forCatalog(catalog)) ++
      Vector(TheOldOak) ++
      Vector(RowdyPub) ++
      Vector(VillageIdiot) ++
      Vector(Downtrodden.forCatalog(catalog)) ++
      Vector(SpitefulMirror) ++
      Vector(MovingMarket.forCatalog(catalog)) ++
      Vector(AwaitedReturn) ++
      Vector(MountedLibrary.forCatalog(catalog)) ++
      Vector(Defame.forCatalog(catalog)) ++
      Vector(GolemLegions) ++
      Vector(MapLibrary.forCatalog(catalog)) ++
      Vector(SkilledMerchants.forCatalog(catalog)) ++
      Vector(SmallFriends.forCatalog(catalog),
        FriendlyFamiliar.forCatalog(catalog),
        SecretSignal) ++
      Vector(Pressgangs) ++
      Vector(OldSongs) ++
      Vector(Curfew.forCatalog(catalog)) ++
      Vector(ForcedLabor) ++
      Vector(InitiationRite) ++
      Vector(VowOfBeastkin.forCatalog(catalog)) ++
      Vector(VowOfWanderingMuster, VowOfWandering) ++
      Vector(VowOfPoverty) ++
      Vector(RelicWorship) ++
      Vector(SilverCharm)
