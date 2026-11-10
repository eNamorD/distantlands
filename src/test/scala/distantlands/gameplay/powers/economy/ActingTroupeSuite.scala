package distantlands.gameplay.powers.economy

import distantlands.gameplay.{ActingTroupeCard, PledgeToDiscordCard}
import distantlands.gameplay.actions.economy.{MusterProcedure, TradeProcedure}
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.action.AlchemistCard
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  PhasePowerHarness, PowerRules, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Acting Troupe (card 36), a selected Trade modifier: picked, it asks
  * Beast or Order, and the Trade counts it as that suit (catalog batch 10,
  * slice 7). */
class ActingTroupeSuite extends munit.FunSuite:
  private val troupe = ActingTroupeCard.id
  private val power = ActingTroupe
  private val home = AncientCitySite.id
  /** A plain Beast denizen. */
  private val errandBoy = ErrandBoyCard.id
  /** A plain Order denizen. */
  private val wrestlers = WrestlersCard.id
  /** A plain Arcane denizen. */
  private val alchemist = AlchemistCard.id
  private val modifiers =
    ModifierPicks.decisionId(PowerWindow.TradeModifierSelection)
  private val rules = PowerRules.withWalkerPowers
  private val parked = PowerRules.parkedUnderWalkerPowers

  /** p1 at Ancient City holding Acting Troupe faceup, with `card` at the
    * site, 2 favor, 1 faceup secret, 7 Supply, and 3 favor in the Beast,
    * Order and Arcane banks. */
  private def troupeBeside(card: DenizenId): Table = Table.start
    .pawn(p1, at = home).adviser(p1, troupe).denizen(card, at = home)
    .favor(p1, 2).secrets(p1, 1).supply(p1, 7)
    .bankFavor(Suit.Beast, 3).bankFavor(Suit.Order, 3)
    .bankFavor(Suit.Arcane, 3)

  private def tradeBegun(ready: ReadyGame, resource: String = "favor")
      : OathTransition = accepted(MenuPlay.started(rules, Ready(ready), p1,
    ActionRef.Trade, Vector(DecisionOptionRef.Button(resource))))

  /** `from`, then p1's answer `ref` at `decisionId`; the events run from
    * the action's start. */
  private def answeredWith(from: OathTransition, decisionId: String,
      ref: DecisionOptionRef): OathTransition =
    val next = accepted(rules.resolveWalker(from.state, p1, decisionId,
      ChooseOneAnswer(ref)))
    next.copy(events = from.events ++ next.events)

  /** `from` with the Troupe picked and acting as `suit`. The Troupe is the
    * only modifier, so the Trade then starts by itself. */
  private def troupeActingAs(from: OathTransition, suit: Suit)
      : OathTransition =
    answeredWith(answeredWith(from, modifiers,
      DecisionOptionRef.Denizen(troupe)), ActingTroupeCard.suitDecision,
      ActingTroupeCard.button(suit))

  /** `from` with "Start" answered and no modifier picked. */
  private def startedUnpicked(from: OathTransition): OathTransition =
    answeredWith(from, modifiers, ModifierPicks.start)

  private def tradedWith(from: OathTransition, card: DenizenId)
      : OathTransition =
    answeredWith(from, TradeProcedure.decisionId,
      DecisionOptionRef.Denizen(card))

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(power.id, power.noteKeys, events)

  test("picked, it asks Beast or Order"):
    val asked = answeredWith(tradeBegun(troupeBeside(errandBoy).ready),
      modifiers, DecisionOptionRef.Denizen(troupe))
    assertEquals(PhasePowerHarness.offered(asked, p1), Some(Vector(
      "button" -> "beast", "button" -> "order")))

  test("acting as a Beast card, it adds 1 favor to a Trade with a Beast " +
      "card, and the line says so"):
    val ready = troupeBeside(errandBoy).ready
    val done = tradedWith(troupeActingAs(tradeBegun(ready), Suit.Beast),
      errandBoy)
    // 1, and 1 for the Troupe.
    assertEquals(Look(done.state).favor(p1), 2 + 2)
    assertEquals(Look(done.state).bankFavor(Suit.Beast), 1)
    assertEquals(said(done.events), Vector(NoteText.Said("acted-beast",
      s"${troupe.value} acted as a Beast card.", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events),
      Outcomes.ready(done.state))
    assertWireRoundTrips(done.events)

  test("acting as an Order card, it gains a secret in a Trade for secrets " +
      "with an Order card, and the line says so"):
    val ready = troupeBeside(wrestlers).ready
    val done = tradedWith(troupeActingAs(tradeBegun(ready, "secret"),
      Suit.Order), wrestlers)
    // 1 faceup secret kept, 1 gained for the Troupe.
    assertEquals(Look(done.state).faceUpSecrets(p1), 2)
    assertEquals(said(done.events), Vector(NoteText.Said("acted-order",
      s"${troupe.value} acted as an Order card.", covers = false)))
    val unpicked = tradedWith(startedUnpicked(tradeBegun(ready, "secret")),
      wrestlers)
    assertEquals(Look(unpicked.state).faceUpSecrets(p1), 1)

  test("acting as an Order card beside a faceup Pledge to Discord, it " +
      "still counts as Order for the Trade"):
    // Owner ruling, 2026-11-05.
    val ready = troupeBeside(wrestlers).adviser(p1, PledgeToDiscordCard.id)
      .ready
    val done = tradedWith(troupeActingAs(tradeBegun(ready, "secret"),
      Suit.Order), wrestlers)
    assertEquals(Look(done.state).faceUpSecrets(p1), 2)
    // Unpicked, Pledge makes it Discord, so it matches nothing.
    val unpicked = tradedWith(startedUnpicked(tradeBegun(ready, "secret")),
      wrestlers)
    assertEquals(Look(unpicked.state).faceUpSecrets(p1), 1)

  test("unpicked, it counts as Arcane"):
    val ready = troupeBeside(alchemist).ready
    val arcane = tradedWith(startedUnpicked(tradeBegun(ready)), alchemist)
    assertEquals(Look(arcane.state).favor(p1), 2 + 2)
    assertEquals(said(arcane.events), Vector.empty)
    val beastReady = troupeBeside(errandBoy).ready
    val beast = tradedWith(startedUnpicked(tradeBegun(beastReady)), errandBoy)
    assertEquals(Look(beast.state).favor(p1), 2 + 1)

  test("acting as a Beast card, it no longer counts as Arcane"):
    val ready = troupeBeside(alchemist).ready
    val done = tradedWith(troupeActingAs(tradeBegun(ready), Suit.Beast),
      alchemist)
    assertEquals(Look(done.state).favor(p1), 2 + 1)

  test("with the Cup of Plenty, a Troupe acting as a Beast card no longer " +
      "matches an Arcane card, so the Trade costs no Supply"):
    val ready = troupeBeside(alchemist).relic(p1, CupOfPlentyCard.id).ready
    val cupPicked = answeredWith(
      troupeActingAs(tradeBegun(ready), Suit.Beast), modifiers,
      DecisionOptionRef.Relic(CupOfPlentyCard.id))
    assertEquals(Look(tradedWith(cupPicked, alchemist).state).supply(p1), 7)
    val unpicked = answeredWith(answeredWith(tradeBegun(ready), modifiers,
      DecisionOptionRef.Relic(CupOfPlentyCard.id)), modifiers,
      ModifierPicks.start)
    assertEquals(Look(tradedWith(unpicked, alchemist).state).supply(p1),
      7 - 1)

  test("a facedown Troupe is not offered, so the Trade asks for its card " +
      "at once"):
    val ready = Table.start.pawn(p1, at = home)
      .adviser(p1, troupe, facedown = true).denizen(errandBoy, at = home)
      .secrets(p1, 1).ready
    parked.assertParked(tradeBegun(ready).state, ActionRef.Trade,
      TradeProcedure.decisionId, p1)

  test("it is no Muster modifier, and a Muster counts it as Arcane"):
    val ready = troupeBeside(alchemist).ready
    val begun = accepted(MenuPlay.started(rules, Ready(ready), p1,
      ActionRef.Muster, Vector.empty))
    parked.assertParked(begun.state, ActionRef.Muster,
      MusterProcedure.decisionId, p1)
    val done = answeredWith(begun, MusterProcedure.decisionId,
      DecisionOptionRef.Denizen(alchemist))
    // 1, and 1 for the Troupe.
    assertEquals(Look(done.state).boardWarbands(p1),
      Look(ready).boardWarbands(p1) + 2)
