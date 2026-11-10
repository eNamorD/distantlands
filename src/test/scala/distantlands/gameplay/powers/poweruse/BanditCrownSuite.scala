package distantlands.gameplay.powers.poweruse

import distantlands.gameplay.{BanditCrownCard, PassMap, PowerAccess,
  RuleSourceFace}
import distantlands.gameplay.cards.{AncientCitySite, DeepWoodsSite,
  NarrowPassSite}
import distantlands.gameplay.operations.{HallOfMinisters, HallOfMinistersCard}
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.gameplay.powers.ConsentPowers
import distantlands.gameplay.powers.action.{RovingTerror, RovingTerrorCard}
import distantlands.gameplay.powers.cardplay.{SecretPolice, SecretPoliceCard}
import distantlands.gameplay.powers.economy.{Curfew, CurfewCard}
import distantlands.gameplay.powers.recover.{RelicPatrols, RelicPatrolsCard}
import distantlands.gameplay.powers.search.{ForcedLabor, ForcedLaborCard}
import distantlands.gameplay.powers.travel.{ForbiddenForest,
  ForbiddenForestCard, GraspingVines, GraspingVinesCard, Tents, TentsCard,
  TollRoads, TollRoadsCard}
import distantlands.gameplay.powers.wake.{HiddenPassages, HiddenPassagesCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{MenuPlay, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Bandit Crown (R11): its holder uses the cards at bandit-ruled sites as
  * if they ruled them, and the bandits still rule (catalog batch 11,
  * rulings 1 and 2). */
class BanditCrownSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val terror = RovingTerrorCard.id

  /** p2 is the Chancellor and p1 a Citizen; p3, an Exile, stands at Deep
    * Woods with 1 favor. Bandits rule Ancient City, where Roving Terror
    * and Tents stand. It is p3's Act. */
  private def banditCardsAwayFromP3: Table =
    Table.start.imperial(p2, p1).turn(p3, Phase.Act).pawn(p3, at = woods)
      .favor(p3, 1).bandits(home, 2).denizen(terror, at = home)
      .denizen(TentsCard.id, at = home)

  private def p3HoldsTheCrown: Table =
    banditCardsAwayFromP3.relic(p3, BanditCrownCard)

  private def tentsSelectable(ready: ReadyGame): Boolean =
    val window = PowerWindow.TravelModifierSelection
    Tents.applicable(PowerCtx(catalog, ready, p3, Tents.source, window,
      Vector.empty, Sequence(Vector.empty, Some(window))))

  test("the holder uses a bandit card's power from the phase menu"):
    assert(MenuPlay.availableInMenu(catalog, p3HoldsTheCrown.ready,
      ActionRef.UsePower(RovingTerror.id),
      Vector(DecisionOptionRef.Denizen(terror))))

  test("the holder may select a bandit card's modifier"):
    assert(tentsSelectable(p3HoldsTheCrown.ready))

  test("without the Crown, the cards are out of reach"):
    val ready = banditCardsAwayFromP3.ready
    assert(!PowerAccess.accessible(RuleSourceRef.SiteCard(home, terror),
      RuleSourceFace.FaceUp, ready, p3))
    assert(!tentsSelectable(ready))

  test("a facedown Crown reaches nothing"):
    val ready = banditCardsAwayFromP3
      .relic(p3, BanditCrownCard, facedown = true).ready
    assertEquals(PowerAccess.locate(ready, p3, terror), None)

  test("the bandits still rule, and the holder rules nothing more"):
    val ready = p3HoldsTheCrown.ready
    assertEquals(PowerAccess.ruledSites(ready, p3), Set.empty[SiteId])
    assertEquals(SiteRule.rulingPlayer(ready, home), Some(SiteRuler.Bandits))

  private val consent = ConsentPowers.default

  /** Bandits rule Ancient City, where `placed` puts the card; p2 holds the
    * Crown when `crown` is set. */
  private def atTheBanditSite(placed: Table => Table, crown: Boolean)
      : ReadyGame =
    val table = placed(Table.start.bandits(home, 2))
    (if crown then table.relic(p2, BanditCrownCard) else table).ready

  /** Each waivable power of a card at a site, and how to place its card. */
  private val cardPowers: Vector[(PowerId, Table => Table)] = Vector(
    SpellBreaker.id -> (_.denizen(SpellBreakerCard.id, at = home)),
    ForestCouncil.id -> (_.denizen(ForestCouncilCard.id, at = home)),
    CityWall.id -> (_.denizen(CityWallCard.id, at = home)),
    SecretPolice.id -> (_.denizen(SecretPoliceCard.id, at = home)),
    Curfew.id -> (_.denizen(CurfewCard.id, at = home)),
    ForcedLabor.id -> (_.denizen(ForcedLaborCard.id, at = home)),
    TollRoads.id -> (_.denizen(TollRoadsCard.id, at = home)),
    GraspingVines.id -> (_.denizen(GraspingVinesCard.id, at = home)),
    HiddenPassages.id -> (_.edifice(HiddenPassagesCard, EdificeSide.Intact,
      at = home)),
    RelicPatrols.id -> (_.edifice(RelicPatrolsCard, EdificeSide.Intact,
      at = home)),
    ForbiddenForest.id -> (_.edifice(ForbiddenForestCard, EdificeSide.Intact,
      at = home)),
    HallOfMinisters.id -> (_.edifice(HallOfMinistersCard, EdificeSide.Intact,
      at = home)))

  test("the holder may waive every waivable power of a bandit card"):
    cardPowers.foreach { (power, placed) =>
      assertEquals(consent.granter(atTheBanditSite(placed, crown = true),
        power), Some(p2), power.value)
      assertEquals(consent.granter(atTheBanditSite(placed, crown = false),
        power), None, power.value)
    }

  test("the holder rules the waivable powers of bandit cards in the grid"):
    val ready = atTheBanditSite(_.denizen(CityWallCard.id, at = home),
      crown = true)
    assertEquals(consent.ruledBy(ready, p2), Vector(CityWall.id))

  test("a site's own power stays the bandits'"):
    val onPassMap = PassMap.withPawnAt(Table.start.relic(p2, BanditCrownCard)
      .ready, p1, PassMap.ancientCity)
    val ready = Table.from(onPassMap).bandits(PassMap.pass, 2).ready
    assertEquals(consent.granter(ready, NarrowPassSite.pass), None)
