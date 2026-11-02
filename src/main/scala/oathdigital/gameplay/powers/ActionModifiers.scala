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
    Vector[ContributingPower](Augury, TruthfulHarp, Disciples, CropRotation,
      Baron, NewGrowth, ForestWarden,
      NewsFromAfar, HuntingParty, Observatory, ErrandBoy, Mushrooms,
      WineOfWelcome, PaintedTrumpet, SpinningBee,
      LandWarden, CupOfPlenty.forCatalog(catalog),
      AnimalPlaymates.forCatalog(catalog), Birdsong.forCatalog(catalog),
      TheOldOak, RowdyPub, VillageIdiot, Downtrodden.forCatalog(catalog),
      SpitefulMirror, MovingMarket.forCatalog(catalog), AwaitedReturn,
      MountedLibrary.forCatalog(catalog), Defame.forCatalog(catalog),
      GolemLegions, MapLibrary.forCatalog(catalog),
      SkilledMerchants.forCatalog(catalog), SmallFriends.forCatalog(catalog),
      FriendlyFamiliar.forCatalog(catalog), SecretSignal, Pressgangs,
      OldSongs, Curfew.forCatalog(catalog), ForcedLabor, InitiationRite,
      VowOfBeastkin.forCatalog(catalog), VowOfWanderingMuster,
      VowOfWandering, VowOfPoverty, RelicWorship, SilverCharm)
