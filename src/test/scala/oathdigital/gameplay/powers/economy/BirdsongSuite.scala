package oathdigital.gameplay.powers.economy

import oathdigital.gameplay.powers.SearchFixture
import oathdigital.gameplay.powers.action.AlchemistCard
import oathdigital.gameplay.powers.campaign.{RainBootsCard, WrestlersCard}
import oathdigital.gameplay.powers.search.ErrandBoyCard
import oathdigital.gameplay.powers.wake.AncientCitySite
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, EconomyActions, Look, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

class BirdsongSuite extends munit.FunSuite:
  import SearchFixture.rules

  private val birdsong = BirdsongCard.id
  private val modifiers = Vector(Birdsong.id)
  // Plain cards of each suit the tests trade with: none has a Muster or
  // Trade power of its own.
  private val beast = ErrandBoyCard.id
  private val nomad = RainBootsCard.id
  private val order = WrestlersCard.id

  /** p1 holds Birdsong as an adviser; p1's site, Ancient City, holds
    * Alchemist and `source`, token-free. p1 has 4 favor, 2 faceup secrets
    * and 7 Supply. */
  private def at(source: DenizenId, facedown: Boolean = false): ReadyGame =
    Table.start
      .adviser(p1, birdsong, facedown = facedown)
      .pawn(p1, at = AncientCitySite)
      .denizen(AlchemistCard, at = AncientCitySite)
      .denizen(source, at = AncientCitySite)
      .favor(p1, 4).secrets(p1, faceUp = 2).supply(p1, 7)
      .ready

  /** Trades for favor with `card`. */
  private def trade(ready: ReadyGame, selected: Vector[PowerId],
      card: DenizenId): EconomyActions.Completed =
    EconomyActions.trade(rules, ready, p1, selected, "favor",
      DecisionOptionRef.Denizen(card))

  test("trading with a beast card spends no Supply and otherwise costs the " +
      "same"):
    val ready = at(beast)
    val traded = trade(ready, modifiers, beast)
    val result = traded.result
    assertEquals(Look(result).supply(p1), 7)
    // The selection itself is free: favor and secrets match the same Trade
    // without it.
    val plain = trade(ready, Vector.empty, beast).result
    assertEquals(Look(result).favor(p1), Look(plain).favor(p1))
    assertEquals(Look(result).faceUpSecrets(p1), Look(plain).faceUpSecrets(p1))
    val events = traded.journal.events
    assertEquals(ActPowerHarness.replayed(rules, ready, events), result)
    assertWireRoundTrips(events)

  test("trading with a nomad card spends no Supply"):
    assertEquals(Look(trade(at(nomad), modifiers, nomad).result).supply(p1), 7)

  test("trading with a card of another suit pays the Supply"):
    // A Trade costs 1 Supply: 7 - 1.
    assertEquals(Look(trade(at(order), modifiers, order).result).supply(p1),
      7 - 1)

  test("without the selection a beast card pays the Supply"):
    assertEquals(Look(trade(at(beast), Vector.empty, beast).result).supply(p1),
      7 - 1)

  test("it may be selected whatever the site holds, and only for a Trade"):
    val offered = (action: ActionRef) => accepted(
      rules.offerableWalkerPowers(at(order), p1, action)).map(_.id)
    assert(offered(ActionRef.Trade).contains(Birdsong.id))
    assert(!offered(ActionRef.Muster).contains(Birdsong.id))
