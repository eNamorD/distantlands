package oathdigital.gameplay

import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.operations.PayCostSettlement
import oathdigital.gameplay.phases.RestCleanupPlan
import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.gameplay.powers.RuledCards
import oathdigital.gameplay.powers.action.{AlchemistCard, HomesteadersCard}
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2}

/** `EffectiveSuit`: a card's suit as the rules see it. A faceup Marriage
  * counts as two advisers of its suit (catalog batch 5 rulings, "Counts as
  * applies everywhere"), Pledge to Discord makes its holder's faceup
  * advisers Discord (catalog batch 8 rulings, slice 8), and a picked Acting
  * Troupe counts as the suit its user named (catalog batch 10, slice 7). */
class EffectiveSuitSuite extends munit.FunSuite:
  private val marriage = MarriageCard.id
  private val homesteaders = HomesteadersCard.id
  private val wrestlers = WrestlersCard.id
  private val pledge = PledgeToDiscordCard.id
  private val mirror = SpitefulMirrorCard.id
  private val troupe = ActingTroupeCard.id

  /** The walk's answers once Acting Troupe is picked for a Trade and acts
    * as `suit`. */
  private def troupeActingAs(suit: Suit): Vector[Answered] = Vector(
    Answered(ModifierPicks.decisionId(PowerWindow.TradeModifierSelection),
      ChooseOneAnswer(DecisionOptionRef.Denizen(troupe)), p1),
    Answered(ActingTroupeCard.suitDecision,
      ChooseOneAnswer(ActingTroupeCard.button(suit)), p1))

  test("a faceup Marriage counts as two Hearth advisers, and facedown as " +
      "none"):
    val faceup = Table.start.adviser(p1, MarriageCard)
      .adviser(p1, HomesteadersCard).ready
    assertEquals(EffectiveSuit.count(catalog, faceup, p1, Suit.Hearth), 3)
    assertEquals(EffectiveSuit.count(catalog, faceup, p1, Suit.Beast), 0)
    val facedown = Table.start.adviser(p1, MarriageCard, facedown = true)
      .adviser(p1, HomesteadersCard).ready
    assertEquals(EffectiveSuit.count(catalog, facedown, p1, Suit.Hearth), 1)

  test("a ruler's count weighs Marriage, while its list names it once"):
    val ready = Table.start.adviser(p1, MarriageCard).ready
    val ruler = SiteRuler.Player(p1)
    val listed = RuledCards.of(catalog, ready, ruler, Suit.Hearth)
    assertEquals(listed.count(_ == marriage), 1)
    assertEquals(RuledCards.count(catalog, ready, ruler, Suit.Hearth),
      listed.size + 1)

  test("with Pledge to Discord faceup, its holder's faceup advisers are " +
      "Discord, Marriage as two, while other cards keep their suit"):
    val ready = Table.start.adviser(p1, pledge).adviser(p1, marriage)
      .adviser(p1, wrestlers).adviser(p2, homesteaders)
      .denizen(AlchemistCard, at = AncientCitySite).ready
    // Pledge 1, Marriage 2, Wrestlers 1.
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Discord), 4)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Hearth), 0)
    assertEquals(EffectiveSuit.of(catalog, ready, wrestlers),
      Some(Suit.Discord))
    assertEquals(EffectiveSuit.of(catalog, ready, homesteaders),
      Some(Suit.Hearth))
    assertEquals(EffectiveSuit.of(catalog, ready,
      AlchemistCard.id), Some(Suit.Arcane))

  test("a facedown Pledge changes nothing, and a facedown adviser beside " +
      "a faceup Pledge counts for no suit"):
    val facedownPledge = Table.start.adviser(p1, pledge, facedown = true)
      .adviser(p1, wrestlers).ready
    assertEquals(EffectiveSuit.of(catalog, facedownPledge, wrestlers),
      Some(Suit.Order))
    val facedownAdviser = Table.start.adviser(p1, pledge)
      .adviser(p1, wrestlers, facedown = true).ready
    assertEquals(EffectiveSuit.count(catalog, facedownAdviser, p1,
      Suit.Discord), 1)

  test("a Pledge holder rules its advisers as Discord"):
    val ready = Table.start.adviser(p1, pledge).adviser(p1, wrestlers).ready
    val ruler = SiteRuler.Player(p1)
    assertEquals(RuledCards.of(catalog, ready, ruler, Suit.Discord),
      Vector[CardId](pledge, wrestlers))
    assertEquals(RuledCards.of(catalog, ready, ruler, Suit.Order),
      Vector.empty[CardId])

  test("the traded suit is Discord once Spiteful Mirror is picked"):
    val ready = Table.start.denizen(wrestlers, at = AncientCitySite).ready
    val source = DecisionOptionRef.Denizen(wrestlers)
    val picked = Vector(Answered(
      ModifierPicks.decisionId(PowerWindow.TradeModifierSelection),
      ChooseOneAnswer(DecisionOptionRef.Relic(mirror)), p1))
    assertEquals(EffectiveSuit.traded(catalog, ready, Vector.empty, source),
      Some(Suit.Order))
    assertEquals(EffectiveSuit.traded(catalog, ready, picked, source),
      Some(Suit.Discord))

  test("a picked Acting Troupe counts as the answered suit instead of " +
      "Arcane, and unpicked as Arcane"):
    val ready = Table.start.adviser(p1, troupe).adviser(p1, wrestlers).ready
    val beast = troupeActingAs(Suit.Beast)
    assertEquals(EffectiveSuit.troupe(beast), Some(Suit.Beast))
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Beast, beast), 1)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Arcane, beast),
      0)
    // Wrestlers is Order and counts either way.
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Order,
      troupeActingAs(Suit.Order)), 2)
    assertEquals(EffectiveSuit.troupe(Vector.empty), None)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Arcane), 1)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Beast), 0)

  test("a facedown Acting Troupe counts for no suit, picked or not"):
    val ready = Table.start.adviser(p1, troupe, facedown = true).ready
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Beast,
      troupeActingAs(Suit.Beast)), 0)

  test("a picked Acting Troupe beats Pledge to Discord for the Trade"):
    // Owner ruling, 2026-11-05.
    val ready = Table.start.adviser(p1, pledge).adviser(p1, troupe).ready
    val order = troupeActingAs(Suit.Order)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Order, order), 1)
    // Only Pledge itself is Discord now.
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Discord, order),
      1)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Discord), 2)

  test("an off-turn payment onto a Pledge holder's adviser settles to the " +
      "Discord bank"):
    // p1 is the active player on the starting table; p2 pays off turn.
    val ready = Table.start.adviser(p2, pledge).adviser(p2, wrestlers).ready
    val pay = PayCost(p2, Location.OnCard(wrestlers), Cost(favor = 1),
      intoOccupied = true, matchingBank = Some(Suit.Order))
    assertEquals(PayCostSettlement.prepare(ready, pay),
      Right(pay.copy(matchingBank = Some(Suit.Discord), offTurn = true)))

  test("Rest returns the favor on a Pledge holder's adviser to the Discord " +
      "bank"):
    val ready = Table.start.adviser(p1, pledge).adviser(p1, wrestlers)
      .tokens(wrestlers, favor = 2).ready
    assertEquals(RestCleanupPlan.derive(catalog, ready, p1)
      .map(_.returnedFavor), Right(Map(Suit.Discord -> 2)))
