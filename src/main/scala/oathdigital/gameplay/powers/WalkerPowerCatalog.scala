package oathdigital.gameplay.powers

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.operations.OperationRestrictions
import oathdigital.gameplay.powerresolver.{ContributingPower,
  PowerUseRestriction}
import oathdigital.gameplay.powers.action._
import oathdigital.gameplay.powers.banner._
import oathdigital.gameplay.powers.burn._
import oathdigital.gameplay.powers.campaign._
import oathdigital.gameplay.powers.cardplay._
import oathdigital.gameplay.powers.dice._
import oathdigital.gameplay.powers.economy._
import oathdigital.gameplay.powers.poweruse._
import oathdigital.gameplay.powers.recover._
import oathdigital.gameplay.powers.rest._
import oathdigital.gameplay.powers.setup._
import oathdigital.gameplay.powers.targeting._
import oathdigital.gameplay.powers.title._
import oathdigital.gameplay.powers.travel._
import oathdigital.gameplay.powers.wake._
import oathdigital.gameplay.powers.whenplayed._
import oathdigital.gameplay.walker.WalkerPowers

/** The real catalog of `ContributingPower`s wired onto the generic walker
  * seam. Catalog-parameterized: a contribution that carries a
  * catalog-specific card id must be resolved against the same catalog the
  * caller is running. A site power whose site is absent from `catalog`
  * (e.g. a synthetic test catalog) is simply omitted, not a construction
  * failure. Every other card power names its card, so it is always present.
  *
  * A power carrying no catalog id is simply always present: Take Wealth's
  * once-per-turn limit states a rulebook clause about whichever site the pawn
  * stands on, so there is nothing to look up and nothing to omit. Discovery
  * keeps only powers that hook the window being gathered, so it acts only
  * where Take Wealth walks `PowerWindow.WakeTakeWealth`.
  * League Treaty is inert until Finish Rest walks its `RestReturnFavor` window.
  * Silver Tongue's, Insomnia's and Assassin's adviser limits, Lost
  * Tapestry's and Pied Piper's exemptions, and Family Wagon's lifted limit
  * and note are inert until a card play walks `SearchPlayAdviser`. Family
  * Wagon's bury runs from the walker's arrival pass.
  * Conspiracy's power carries no catalog id (a Vision has no catalog powers),
  * so like Take Wealth's limit it is always present and inert until a card
  * play runs `ActionCardPlayedFaceup` for Conspiracy.
  * Vow of Peace's restriction is inert until Campaign walks
  * `CampaignActionEligibility`.
  * Vow of Silence and Vow of Division claim enemies' burns whenever the walker
  * runs a batch, and are inert at the Challenge until a Challenge walks its
  * windows.
  * The battle plans are inert until a Campaign folds its plan windows: each
  * offers itself there, and the title's defense, which no card prints, is always
  * present.
  * Brass Army's restriction is inert until a batch moves its holder's pawn.
  * Secret Testament is inert until a Challenge walks `ChallengeRibbon`.
  * Yew Staff hooks no window: card powers read it through CardPowerBanners.
  * Jinx is inert until a roll step asks for rerolls.
  * `restrictionSet` gives every walker step the catalog's global operation
  * restrictions.
  */
object WalkerPowerCatalog:
  def default(catalog: ExecutableCatalog): WalkerPowers =
    val powers = contributing(catalog)
    WalkerPowers(powers,
      restrictionSet = OperationRestrictions.forCatalog(catalog),
      declared = Some(DeclaredNotes.of(powers, catalog)), catalog = catalog)

  /** The power-use restrictions the walker powers of `catalog` declare,
    * for the phase powers (catalog batch 8, slice 10). Read from
    * `contributing`, since `default` builds the phase powers' notes. */
  def useRestrictions(catalog: ExecutableCatalog)
      : Vector[PowerUseRestriction] =
    contributing(catalog).flatMap(_.powerUseRestrictions)

  /** The powers themselves. `DeclaredNotes` reads them to build the union a
    * `WalkerPowers` carries, so it is taken separately from `default`: a
    * union built from `default` would build a union to build a union. */
  private[powers] def contributing(catalog: ExecutableCatalog)
      : Vector[ContributingPower] =
    Vector(CatacombsContribution) ++
      Vector(RelicPatrols, BanditPatrols) ++
      Vector(VowOfPeaceContribution) ++
      Vector(VowOfSilence, VowOfDivision) ++
      Vector(BrassArmyMoves) ++
      Vector(SecretTestament) ++
      Vector(YewStaff) ++
      Vector(SpellBreaker) ++
      Vector(ForestCouncil) ++
      Vector(CityWall) ++
      Vector(GiantPythonRestriction) ++
      Vector(HiddenPassages) ++
      Vector(TempleOfAncestors.forCatalog(catalog)) ++
      TravelSitePowers.all ++
      Vector(BoilingLakeTravel, ForbiddenForest) ++
      Vector(HallowedSpring) ++
      TravelModifiers.all ++
      Vector(LeagueTreatyContribution) ++
      Vector(SilverTongue) ++
      Vector(Insomnia) ++
      Vector(Assassin.forCatalog(catalog)) ++
      Vector(LostTapestry) ++
      Vector(PiedPiper) ++
      Vector(FamilyWagon) ++
      Vector(VowOfUnionAdvisers) ++
      Vector(ASmallFavor) ++
      Vector(FaithfulFriend) ++
      Vector(Garrison) ++
      Vector(FamilyHeirloom) ++
      Vector(ShiftingFog) ++
      Vector(TwinBrother) ++
      WhenPlayedPowers.all ++
      Vector(ChaosCult) ++
      ActionModifiers.all ++
      TargetProtections.all ++
      Vector(KnightsErrant) ++
      Vector(Marriage) ++
      Vector(PledgeToDiscord) ++
      Vector(Jinx) ++
      BattlePlans.all ++
      PlanRules.all ++
      SimplePlans.all ++
      PlanRestrictions.all ++
      PlanRewrites.all ++
      CardPlayTriggers.all ++
      BannerFacePowers.contributions ++
      Vector(Dazzle, GreatMarket,
        BanditMarket, GreatForge,
        BrokenForge, ProvingGrounds,
        EmptyGrounds, HallOfMockery, LostGuardians,
        RuinedSchool, BrokenHorn, TakeWealthLimit,
        ConspiracyWhenPlayed)
