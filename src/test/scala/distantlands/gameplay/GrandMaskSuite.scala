package oathdigital.gameplay

import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite}
import oathdigital.gameplay.powers.action.{RovingTerror, RovingTerrorCard}
import oathdigital.gameplay.operations.HallOfMinistersCard
import oathdigital.gameplay.powers.campaign.{ToweringRampartCard,
  WrestlersCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{MenuPlay, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

/** Grand Mask (R05): its wearer, an Exile in their own turn, rules the
  * cards at Imperial sites except battle plans, and the Imperials do not;
  * the sites stay Imperial (catalog batch 11, rulings 7 and 18). */
class GrandMaskSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val woods = DeepWoodsSite.id
  private val terror = RovingTerrorCard.id
  private val wrestlers = WrestlersCard.id

  /** p2 is the Chancellor and p1 a Citizen; the Empire holds Ancient City,
    * where Roving Terror and Wrestlers (a battle plan) stand. p3, an
    * Exile, and p2 both stand at Deep Woods. It is p3's Act. */
  private def imperialCardsAwayFromP3: Table =
    Table.start.imperial(p2, p1).turn(p3, Phase.Act).pawn(p3, at = woods)
      .pawn(p2, at = woods).warbandsAt(home, p2, 2)
      .denizen(terror, at = home).denizen(wrestlers, at = home)

  private def p3WearsTheMask: Table =
    imperialCardsAwayFromP3.relic(p3, GrandMaskCard)

  private def terrorAccess(ready: ReadyGame, player: PlayerId): Boolean =
    PowerAccess.accessible(RuleSourceRef.SiteCard(home, terror),
      RuleSourceFace.FaceUp, ready, player)

  test("the wearer rules an Imperial site's card, and the Chancellor does " +
      "not"):
    val ready = p3WearsTheMask.ready
    assertEquals(CardRule.rulerOf(ready, terror),
      Some(SiteRuler.Player(p3)))
    assertEquals(CardRule.rulerAt(ready, home, terror),
      Some(SiteRuler.Player(p3)))

  test("a battle plan stays the Empire's"):
    assertEquals(CardRule.rulerOf(p3WearsTheMask.ready, wrestlers),
      Some(SiteRuler.Player(p2)))

  test("an edifice is a plan by its face: the wearer rules Hall of " +
      "Ministers, and Towering Rampart stays the Empire's"):
    val ready = p3WearsTheMask
      .edifice(HallOfMinistersCard, EdificeSide.Intact, at = home)
      .edifice(ToweringRampartCard, EdificeSide.Ruined, at = home).ready
    assertEquals(CardRule.rulerAt(ready, home, HallOfMinistersCard.id),
      Some(SiteRuler.Player(p3)))
    assertEquals(CardRule.rulerAt(ready, home, ToweringRampartCard.id),
      Some(SiteRuler.Player(p2)))

  test("the sites stay Imperial, and the wearer rules no site"):
    val ready = p3WearsTheMask.ready
    assertEquals(SiteRule.rulingPlayer(ready, home),
      Some(SiteRuler.Player(p2)))
    assert(SiteRulers.imperialSites(ready).contains(home))
    assertEquals(PowerAccess.ruledSites(ready, p3), Set.empty[SiteId])
    assert(PowerAccess.ruledSites(ready, p2).contains(home))

  test("the wearer uses a masked card from the phase menu"):
    assert(MenuPlay.availableInMenu(catalog, p3WearsTheMask.ready,
      ActionRef.UsePower(RovingTerror.id),
      Vector(DecisionOptionRef.Denizen(terror))))
    assert(PowerAccess.rulingSites(p3WearsTheMask.ready, p3).contains(home))

  test("the Chancellor loses ruler access, but keeps their pawn's site"):
    val ready = p3WearsTheMask.ready
    assert(!terrorAccess(ready, p2))
    assert(!PowerAccess.rulingSites(ready, p2).contains(home))
    assert(terrorAccess(Table.from(ready).pawn(p2, at = home).ready, p2))

  test("nothing changes on another player's turn, for a Citizen holder, " +
      "or with the Mask facedown"):
    val othersTurn = p3WearsTheMask.turn(p1, Phase.Act).ready
    val citizenHolder = imperialCardsAwayFromP3.relic(p1, GrandMaskCard)
      .turn(p1, Phase.Act).ready
    val facedown = imperialCardsAwayFromP3
      .relic(p3, GrandMaskCard, facedown = true).ready
    Vector(othersTurn, citizenHolder, facedown).foreach { ready =>
      assertEquals(CardRule.rulerOf(ready, terror),
        SiteRule.rulingPlayer(ready, home))
      assertEquals(GrandMaskRule.maskedSites(ready), Set.empty[SiteId])
    }
    assert(!terrorAccess(facedown, p3))

  test("it works through the wearer's Wake and Rest"):
    Vector(Phase.Wake, Phase.Rest).foreach { phase =>
      assertEquals(CardRule.rulerOf(p3WearsTheMask.turn(p3, phase).ready,
        terror), Some(SiteRuler.Player(p3)))
    }

  test("a site the Empire does not hold is read as before"):
    // A Citizen's warbands are Imperial, so bandits hold the other site.
    val ready = Table.start.imperial(p2, p1).turn(p3, Phase.Act)
      .bandits(peaks, 1).denizen(terror, at = peaks)
      .relic(p3, GrandMaskCard).ready
    assertEquals(GrandMaskRule.maskingAt(ready, peaks), None)
    assertEquals(CardRule.rulerAt(ready, peaks, terror),
      Some(SiteRuler.Bandits))
