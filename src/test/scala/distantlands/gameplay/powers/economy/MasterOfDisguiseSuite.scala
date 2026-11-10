package oathdigital.gameplay.powers.economy

import oathdigital.gameplay.actions.economy.TradeProcedure
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.campaign.{BattleHonorsCard, LongbowsCard,
  ScoutsCard, WrestlersCard}
import oathdigital.gameplay.powers.search.ErrandBoyCard
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{ActPowerHarness, EconomyActions, Look, MenuPlay,
  Outcomes, PhasePowerHarness, PowerRules, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** Master of Disguise (card 78, ruling 8): picked first in a Trade, it asks
  * whose advisers its holder acts with, and for the rest of that Trade
  * those advisers are the holder's and the holder's own do nothing. */
class MasterOfDisguiseSuite extends munit.FunSuite:
  private val disguise = MasterOfDisguiseCard.id
  private val power = MasterOfDisguise
  /** p1's pawn site. */
  private val home = AncientCitySite.id
  /** A site in play in another region. */
  private val away = Table.siteInAnotherRegionThan(home)
  /** A plain Order denizen to Trade with. */
  private val wrestlers = WrestlersCard.id
  /** Plain Order denizens, held as advisers. */
  private val longbows = LongbowsCard.id
  private val scouts = ScoutsCard.id
  private val battleHonors = BattleHonorsCard.id
  /** A plain Beast denizen. */
  private val errandBoy = ErrandBoyCard.id
  private val signal = SecretSignalCard.id
  private val familiar = FriendlyFamiliarCard.id
  private val poverty = VowOfPovertyCard.id
  private val cup = CupOfPlentyCard.id
  private val modifiers =
    ModifierPicks.decisionId(PowerWindow.TradeModifierSelection)
  private val rules = PowerRules.withWalkerPowers

  /** p1 at Ancient City holding Master of Disguise, with Wrestlers beside
    * them, `secrets` faceup secrets, and the Order bank holding 5 favor. */
  private def disguiseHolderBesideWrestlers(secrets: Int = 2): Table =
    Table.start.pawn(p1, at = home).adviser(p1, disguise)
      .denizen(wrestlers, at = home).secrets(p1, faceUp = secrets)
      .bankFavor(Suit.Order, 5)

  /** p1 starts a Trade for favor, parked on the modifier decision. */
  private def tradeForFavorBegun(ready: ReadyGame): OathTransition =
    accepted(MenuPlay.started(rules, Ready(ready), p1, ActionRef.Trade,
      Vector(DecisionOptionRef.Button("favor"))))

  /** `from`, then p1's answer `ref` at `decisionId`; the events run from
    * the Trade's start. */
  private def answeredWith(from: OathTransition, decisionId: String,
      ref: DecisionOptionRef): OathTransition =
    val next = accepted(rules.resolveWalker(from.state, p1, decisionId,
      ChooseOneAnswer(ref)))
    next.copy(events = from.events ++ next.events)

  /** `from` with Master of Disguise picked and `chosen` named. */
  private def disguisedAs(from: OathTransition, chosen: PlayerId)
      : OathTransition =
    answeredWith(answeredWith(from, modifiers,
      DecisionOptionRef.Denizen(disguise)), MasterOfDisguise.decisionId,
      DecisionOptionRef.Player(chosen))

  /** Each option's id and state at the parked decision, by id. */
  private def optionStates(at: OathTransition): Option[Map[String, String]] =
    PhasePowerHarness.queryOf(at, p1).map(_.offeredOptions
      .map(option => option.id -> option.state).toMap)

  /** The reason the parked decision gives for the option `id`. */
  private def reasonFor(at: OathTransition, id: String): Option[String] =
    PhasePowerHarness.queryOf(at, p1)
      .flatMap(_.offeredOptions.find(_.id == id)).flatMap(_.reason)

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(power.id, power.noteKeys, events)

  test("it is offered only as the first pick, and asks which other " +
      "player"):
    val begun = tradeForFavorBegun(disguiseHolderBesideWrestlers()
      .adviser(p1, signal).ready)
    assertEquals(optionStates(begun), Some(Map("start" -> "available",
      disguise.value -> "available", signal.value -> "available")))
    val signalled = answeredWith(begun, modifiers,
      DecisionOptionRef.Denizen(signal))
    assertEquals(optionStates(signalled).flatMap(_.get(disguise.value)),
      Some("unavailable"))
    assertEquals(reasonFor(signalled, disguise.value),
      Some(MasterOfDisguise.notFirst))
    val asked = answeredWith(begun, modifiers,
      DecisionOptionRef.Denizen(disguise))
    assertEquals(PhasePowerHarness.offered(asked, p1),
      Some(Vector("player" -> p2.value, "player" -> p3.value)))

  test("the chosen player's advisers count toward the Trade and the " +
      "holder's own do not, and the line names the player"):
    // p1's own Battle Honors is an Order card; so are p2's Longbows and
    // Scouts.
    val ready = disguiseHolderBesideWrestlers().adviser(p1, battleHonors)
      .adviser(p2, longbows).adviser(p2, scouts).ready
    // No other modifier to pick, so the Trade goes on to its source.
    val acting = disguisedAs(tradeForFavorBegun(ready), p2)
    assertEquals(Outcomes.ready(acting.state).game.current.walkerAsIfAdvisers,
      Some(p2))
    val done = answeredWith(acting, TradeProcedure.decisionId,
      DecisionOptionRef.Denizen(wrestlers))
    val end = Look(done.state)
    // 1, and 1 more for each of p2's two Order advisers.
    assertEquals(end.favor(p1), Look(ready).favor(p1) + 3)
    assertEquals(end.adviserTokens(p1, disguise), Tokens(0, 1))
    assertEquals(Outcomes.ready(done.state).game.current.walkerAsIfAdvisers,
      None)
    assertEquals(said(done.events), Vector(NoteText.Said("acted",
      s"Acted as if holding the advisers of ${p2.value}.", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events),
      Outcomes.ready(done.state))
    assertWireRoundTrips(done.events)

  test("the chosen player's Trade modifier is offered and the holder pays " +
      "for it, and the holder's own is not offered"):
    // Three secrets: Master of Disguise, Friendly Familiar and the Trade.
    val ready = disguiseHolderBesideWrestlers(secrets = 3).adviser(p1, signal)
      .adviser(p2, familiar).denizen(errandBoy, at = away).ready
    val acting = disguisedAs(tradeForFavorBegun(ready), p2)
    assertEquals(optionStates(acting), Some(Map("start" -> "available",
      familiar.value -> "available")))
    val picked = answeredWith(acting, modifiers,
      DecisionOptionRef.Denizen(familiar))
    val now = Look(picked.state)
    assertEquals(now.adviserTokens(p2, familiar), Tokens(0, 1))
    assertEquals(now.faceUpSecrets(p1), 1)
    assertEquals(now.faceUpSecrets(p2), Look(ready).faceUpSecrets(p2))
    assertEquals(PhasePowerHarness.offered(picked, p1),
      Some(Vector("site" -> away.value)))

  test("the holder's relic modifier stays, and reads the chosen advisers"):
    // p1's own Battle Honors would match Wrestlers; p2's Secret Signal, an
    // Arcane card, does not.
    val ready = disguiseHolderBesideWrestlers().adviser(p1, battleHonors)
      .relic(p1, cup).adviser(p2, signal).ready
    val acting = disguisedAs(tradeForFavorBegun(ready), p2)
    assertEquals(optionStates(acting), Some(Map("start" -> "available",
      cup.value -> "available", signal.value -> "available")))
    val cupPicked = answeredWith(acting, modifiers,
      DecisionOptionRef.Relic(cup))
    val started = answeredWith(cupPicked, modifiers, ModifierPicks.start)
    val done = answeredWith(started, TradeProcedure.decisionId,
      DecisionOptionRef.Denizen(wrestlers))
    val end = Look(done.state)
    // No adviser of p2 matches the Order card: no Supply spent, and 1
    // favor gained.
    assertEquals(end.supply(p1), Look(ready).supply(p1))
    assertEquals(end.favor(p1), Look(ready).favor(p1) + 1)

  test("the chosen player's Vow of Poverty binds the holder"):
    val ready = disguiseHolderBesideWrestlers().adviser(p2, poverty)
      .adviser(p2, signal).ready
    val asked = answeredWith(tradeForFavorBegun(ready), modifiers,
      DecisionOptionRef.Denizen(disguise))
    // With p2's advisers every Trade for favor is refused, so the
    // restriction look-ahead hides p2, as it hides a refused source.
    assertEquals(PhasePowerHarness.offered(asked, p1),
      Some(Vector("player" -> p3.value)))
    // The Vow refuses only favor: in a Trade for secrets p2 is offered.
    // A Trade for secrets costs favor, so p1 holds some.
    val forSecrets = answeredWith(accepted(MenuPlay.started(rules,
      Ready(Table.from(ready).favor(p1, 2).ready), p1, ActionRef.Trade,
      Vector(DecisionOptionRef.Button("secret")))), modifiers,
      DecisionOptionRef.Denizen(disguise))
    assertEquals(PhasePowerHarness.offered(forSecrets, p1),
      Some(Vector("player" -> p2.value, "player" -> p3.value)))

  test("the holder's own Vow of Poverty is lifted"):
    val ready = disguiseHolderBesideWrestlers().adviser(p1, poverty).ready
    // The Vow refuses every Trade for favor without the pick, so the
    // look-ahead hides the start and only the pick leads on.
    assertEquals(EconomyActions.optionStatesAtStart(rules, ready, p1,
      ActionRef.Trade, Vector(DecisionOptionRef.Button("favor"))),
      Some(Vector(disguise.value -> "available")))
    val done = answeredWith(disguisedAs(tradeForFavorBegun(ready), p2),
      TradeProcedure.decisionId, DecisionOptionRef.Denizen(wrestlers))
    assertEquals(Look(done.state).favor(p1), Look(ready).favor(p1) + 1)

  test("a Vow of Kinship holder disguised as another player gains the " +
      "Trade's favor on their board, and one disguised as a holder gains " +
      "it in the Nomad bank"):
    // p1 holds the Vow; disguised as p2, it is lifted for the Trade.
    val lifted = disguiseHolderBesideWrestlers()
      .adviser(p1, oathdigital.gameplay.VowOfKinshipCard)
      .bankFavor(Suit.Nomad, 0).ready
    val liftedDone = answeredWith(disguisedAs(tradeForFavorBegun(lifted),
      p2), TradeProcedure.decisionId, DecisionOptionRef.Denizen(wrestlers))
    assertEquals(Look(liftedDone.state).favor(p1), Look(lifted).favor(p1) + 1)
    assertEquals(Look(liftedDone.state).bankFavor(Suit.Nomad), 0)
    // p2 holds the Vow; disguised as p2, p1 acts with it.
    val lent = disguiseHolderBesideWrestlers()
      .adviser(p2, oathdigital.gameplay.VowOfKinshipCard)
      .bankFavor(Suit.Nomad, 0).ready
    val lentDone = answeredWith(disguisedAs(tradeForFavorBegun(lent), p2),
      TradeProcedure.decisionId, DecisionOptionRef.Denizen(wrestlers))
    assertEquals(Look(lentDone.state).favor(p1), Look(lent).favor(p1))
    assertEquals(Look(lentDone.state).bankFavor(Suit.Nomad), 1)
    assertEquals(ActPowerHarness.replayed(rules, lent, lentDone.events),
      Outcomes.ready(lentDone.state))

  test("a game parked after the choice and rebuilt from its events keeps " +
      "it"):
    val ready = disguiseHolderBesideWrestlers().adviser(p2, longbows).ready
    val acting = disguisedAs(tradeForFavorBegun(ready), p2)
    val rebuilt = ActPowerHarness.replayed(rules, ready, acting.events)
    assertEquals(rebuilt.game.current.walkerAsIfAdvisers, Some(p2))
    assertEquals(rebuilt, Outcomes.ready(acting.state))
