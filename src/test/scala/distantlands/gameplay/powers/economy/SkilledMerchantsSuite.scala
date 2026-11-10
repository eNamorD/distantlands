package distantlands.gameplay.powers.economy

import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.economy.HallowedSpringCard
import distantlands.gameplay.ModifierPlay
import distantlands.gameplay.powers.action.AlchemistCard
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{EconomyActions, Look, PhasePowerHarness, PowerRules,
  Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

class SkilledMerchantsSuite extends munit.FunSuite:
  private val merchants = SkilledMerchantsCard.id
  private val library = MapLibraryCard.id
  /** p1's pawn site. */
  private val home = AncientCitySite.id
  /** Another site in play in the pawn's region. */
  private val near = Table.otherSiteInRegionOf(home)
  /** A site in play in another region. */
  private val away = Table.siteInAnotherRegionThan(home)
  /** A plain Arcane denizen. */
  private val alchemist = AlchemistCard.id
  /** A plain Order denizen. */
  private val wrestlers = WrestlersCard.id
  /** A plain Beast denizen. */
  private val errandBoy = ErrandBoyCard.id
  private val hallowedSpring = HallowedSpringCard.id
  private val rules = PowerRules.withWalkerPowers

  /** p1 at Ancient City beside Alchemist, holding Skilled Merchants faceup,
    * with the start's 1 faceup secret. Wrestlers and the intact Hallowed
    * Spring stand at the other site in the region, Errand Boy in another
    * region, and the Order bank holds 3 favor. */
  private def holdingMerchantsWithRegionCards: Table =
    Table.start.pawn(p1, at = home)
    .adviser(p1, merchants).denizen(alchemist, at = home)
    .denizen(wrestlers, at = near)
    .edifice(hallowedSpring, EdificeSide.Intact, at = near)
    .denizen(errandBoy, at = away).bankFavor(Suit.Order, 3)

  private def tradeStarted(ready: ReadyGame, modifiers: Vector[PowerId])
      : OathTransition =
    accepted(ModifierPlay.started(rules, Ready(ready), ActionRef.Trade, p1,
      PowerWindow.TradeModifierSelection, modifiers,
      startArgs = Vector(DecisionOptionRef.Button("favor"))))

  test("selected, a Trade offers every card at the sites in the pawn's " +
      "region, edifices too, and none elsewhere"):
    assertEquals(PhasePowerHarness.offered(tradeStarted(
      holdingMerchantsWithRegionCards.ready,
      Vector(SkilledMerchants.id)), p1), Some(Vector(
        "denizen" -> alchemist.value, "denizen" -> wrestlers.value,
        "edifice" -> hallowedSpring.value)))

  test("trading with a card at another site places the secret there"):
    val ready = holdingMerchantsWithRegionCards.ready
    val done = EconomyActions.trade(rules, ready, p1,
      Vector(SkilledMerchants.id), "favor",
      DecisionOptionRef.Denizen(wrestlers))
    val after = Look(done.result)
    assertEquals(after.tokensOn(wrestlers), Tokens(0, 1))
    // 1: Skilled Merchants is Hearth, not Order.
    assertEquals(after.favor(p1), Look(ready).favor(p1) + 1)

  test("with Map Library, each card is offered once"):
    val ready = holdingMerchantsWithRegionCards
      .denizen(library, at = home).ready
    assertEquals(PhasePowerHarness.offered(tradeStarted(ready,
      Vector(MapLibrary.id, SkilledMerchants.id)), p1), Some(Vector(
        "denizen" -> alchemist.value, "denizen" -> library.value,
        "denizen" -> wrestlers.value, "edifice" -> hallowedSpring.value)))
