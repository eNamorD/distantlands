package oathdigital.gameplay.powers

import oathdigital.gameplay.{GrandMaskCard, OathRules, SiteRulers}
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powerresolver.PowerUse
import oathdigital.gameplay.powers.action.{RovingTerror, RovingTerrorCard}
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.powers.dice.{Jinx, JinxCard}
import oathdigital.gameplay.powers.economy.{Curfew, CurfewCard}
import oathdigital.gameplay.powers.poweruse.{SpellBreaker, SpellBreakerCard}
import oathdigital.gameplay.powers.targeting.ForgottenVaultCard
import oathdigital.gameplay.powers.wake.{Quartermaster, QuartermasterCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{EconomyActions, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

/** The rules that read a card's ruler, under the Grand Mask (catalog batch
  * 11, ruling 7): during the wearer's turn they read the wearer for every
  * card at an Imperial site but a battle plan. */
class GrandMaskRulesSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val terror = RovingTerrorCard.id
  private val wrestlers = WrestlersCard.id
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))

  /** p2 is the Chancellor and p1 a Citizen, with no advisers. The Empire
    * holds Ancient City (Roving Terror, and Wrestlers, an Order battle
    * plan) and Broken Peaks (Curfew, Spell Breaker). p3, an Exile, stands
    * at Ancient City with the Mask `facedown` or faceup, beside the Beast
    * bank's 3 favor. It is p3's Act. */
  private def empireWithMaskHeldByP3(facedown: Boolean = false): Table =
    Table.start.imperial(p2, p1).withoutAdvisers(p2).withoutAdvisers(p3)
      .turn(p3, Phase.Act).pawn(p3, at = home)
      .warbandsAt(home, p2, 2).warbandsAt(peaks, p2, 1)
      .denizen(terror, at = home).denizen(wrestlers, at = home)
      .denizen(CurfewCard.id, at = peaks)
      .denizen(SpellBreakerCard.id, at = peaks)
      .bankFavor(Suit.Beast, 3)
      .relic(p3, GrandMaskCard, facedown = facedown)

  private def rovingTerrorUseCostingSecret(by: PlayerId): PowerUse =
    PowerUse(by, RovingTerror.id, Some(terror), persistent = false,
      costsSecret = true)

  test("the wearer trades with a masked card free of Curfew"):
    def traded(ready: ReadyGame) = EconomyActions.trade(rules, ready, p3,
      Vector.empty, "favor", DecisionOptionRef.Denizen(terror))
    def parkedOn(done: EconomyActions.Completed): String =
      EconomyActions.parkedDecision(OathState.Ready(done.result)).decisionId
    // The Trade ran to its end, back at the Act menu.
    val done = traded(empireWithMaskHeldByP3().ready)
    assertEquals(parkedOn(done), PhaseMenu.decisionId)
    assertEquals(NoteText.said(Curfew.id, Curfew.noteKeys,
      done.journal.events), Vector.empty)
    // With the Mask facedown, Curfew is the Chancellor's: p3 owes it, and
    // the Trade stops to ask.
    val bare = empireWithMaskHeldByP3(facedown = true).ready
    assertEquals(RulerToll.enemyRuler(bare, CurfewCard.id, p3),
      Some(SiteRuler.Player(p2)))
    assertEquals(parkedOn(traded(bare)), Curfew.decisionId)

  test("a toll owed under the Mask is owed to the wearer"):
    val ready = empireWithMaskHeldByP3().ready
    assertEquals(RulerToll.enemyRuler(ready, CurfewCard.id, p2),
      Some(SiteRuler.Player(p3)))
    assertEquals(RulerToll.enemyRuler(ready, CurfewCard.id, p3), None)
    assertEquals(PayOrAsk.letOffBy(ready, SiteRuler.Player(p3), p2),
      Some(p3))

  test("Spell Breaker's ban turns on the wearer's enemies"):
    def refusal(ready: ReadyGame, by: PlayerId) =
      SpellBreaker.refusal(catalog, ready, rovingTerrorUseCostingSecret(by))
    val ready = empireWithMaskHeldByP3().ready
    assertEquals(refusal(ready, p3), None)
    assertEquals(refusal(ready, p2).map(_.power), Some(SpellBreaker.id))
    assertEquals(refusal(ready, p2).flatMap(_.reason.waiver)
      .flatMap(_.granter), Some(p3))
    assert(refusal(empireWithMaskHeldByP3(facedown = true).ready, p3)
      .nonEmpty)

  test("the wearer's ruled cards count the masked ones, and the " +
      "Chancellor keeps the battle plan"):
    def order(ready: ReadyGame, player: PlayerId): Vector[CardId] =
      RuledCards.of(catalog, ready, SiteRuler.Player(player), Suit.Order)
    val ready = empireWithMaskHeldByP3().ready
    assert(order(ready, p3).contains(CurfewCard.id))
    assert(!order(ready, p3).contains(wrestlers))
    assert(order(ready, p2).contains(wrestlers))
    assert(!order(ready, p2).contains(CurfewCard.id))
    assert(RuledCards.of(catalog, ready, SiteRuler.Player(p3), Suit.Beast)
      .contains(terror))
    val bare = empireWithMaskHeldByP3(facedown = true).ready
    assert(order(bare, p2).contains(CurfewCard.id))
    assert(!order(bare, p3).contains(CurfewCard.id))

  test("the wearer rules a masked Jinx"):
    val ready = Table.from(empireWithMaskHeldByP3().ready)
      .denizen(JinxCard.id, at = peaks).ready
    assert(Jinx.rules(ready, p3))
    assert(!Jinx.rules(ready, p2))

  test("an Empire's protection protects the wearer alone"):
    val ready = Table.from(empireWithMaskHeldByP3().ready)
      .denizen(ForgottenVaultCard.id, at = home).ready
    assertEquals(SiteRulers.protectedBy(ready, home, ForgottenVaultCard.id),
      Set(p3))
    val bare = Table.from(empireWithMaskHeldByP3(facedown = true).ready)
      .denizen(ForgottenVaultCard.id, at = home).ready
    assertEquals(SiteRulers.protectedBy(bare, home, ForgottenVaultCard.id),
      Set(p1, p2))

  test("the wearer may use Quartermaster in their Wake"):
    def withQuartermaster(facedown: Boolean): ReadyGame =
      empireWithMaskHeldByP3(facedown).turn(p3, Phase.Wake)
        .denizen(QuartermasterCard.id, at = peaks).ready
    val source = DecisionOptionRef.Denizen(QuartermasterCard.id)
    assert(Quartermaster.usable(catalog, withQuartermaster(false), p3,
      source))
    assert(!Quartermaster.usable(catalog, withQuartermaster(false), p2,
      source))
    assert(!Quartermaster.usable(catalog, withQuartermaster(true), p3,
      source))
