package distantlands.gameplay.powers

import distantlands.gameplay.powerresolver.ContributingPower
import distantlands.gameplay.powers.challenge.ChallengeModifiers
import distantlands.gameplay.powers.economy.{ActingTroupe, AnimalPlaymates,
  AwaitedReturn, Birdsong, CupOfPlenty, Curfew, Defame, Downtrodden,
  FriendlyFamiliar, GolemLegions, InitiationRite, MapLibrary,
  MasterOfDisguise, MountedLibrary, MovingMarket, OldSongs, Pressgangs,
  RowdyPub, SecretSignal, SkilledMerchants, SmallFriends, SpitefulMirror,
  TheOldOak, VillageIdiot, VowOfBeastkin, VowOfPoverty, VowOfWandering,
  VowOfWanderingMuster}
import distantlands.gameplay.powers.recover.{RelicWorship, SilverCharm}
import distantlands.gameplay.powers.search.{Augury, Baron, Bracken,
  CrackedHorn, CropRotation, Disciples, ErrandBoy, ForcedLabor, ForestWarden,
  HuntingParty, LandWarden, Mushrooms, NewGrowth, NewsFromAfar, Observatory,
  PaintedTrumpet, RingOfDevotion, SpinningBee, TruthfulHarp, WineOfWelcome}

/** The Search, Trade, Muster, Recover and Challenge modifiers, registered
  * together with the rules that change those actions: Initiation Rite's
  * Muster cost, the tolls of Curfew and Forced Labor, the Vows' limits and
  * extra warbands, and Vow of Wandering's ban on placing warbands. Each
  * names its card, so all are always present.
  */
object ActionModifiers:
  val all: Vector[ContributingPower] =
    Vector[ContributingPower](Augury, TruthfulHarp, Disciples, CropRotation,
      Baron, NewGrowth, ForestWarden,
      NewsFromAfar, HuntingParty, Observatory, ErrandBoy, Mushrooms,
      WineOfWelcome, PaintedTrumpet, SpinningBee, RingOfDevotion,
      CrackedHorn, Bracken,
      LandWarden, CupOfPlenty,
      AnimalPlaymates, Birdsong,
      TheOldOak, RowdyPub, VillageIdiot, Downtrodden,
      SpitefulMirror, ActingTroupe, MovingMarket, AwaitedReturn,
      MountedLibrary, Defame,
      GolemLegions, MapLibrary,
      SkilledMerchants, SmallFriends, MasterOfDisguise,
      FriendlyFamiliar, SecretSignal, Pressgangs,
      OldSongs, Curfew, ForcedLabor, InitiationRite,
      VowOfBeastkin, VowOfWanderingMuster,
      VowOfWandering, VowOfPoverty, RelicWorship, SilverCharm) ++
      ChallengeModifiers.all
