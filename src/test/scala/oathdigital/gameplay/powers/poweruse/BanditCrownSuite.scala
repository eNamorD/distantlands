package oathdigital.gameplay.powers.poweruse

import oathdigital.gameplay.{BanditCrownCard, PowerAccess, RuleSourceFace}
import oathdigital.gameplay.cards.{AncientCitySite, DeepWoodsSite}
import oathdigital.gameplay.powerresolver.PowerCtx
import oathdigital.gameplay.powers.action.{RovingTerror, RovingTerrorCard}
import oathdigital.gameplay.powers.travel.{Tents, TentsCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{MenuPlay, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

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
