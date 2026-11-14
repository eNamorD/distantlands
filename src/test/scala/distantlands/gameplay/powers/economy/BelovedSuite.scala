package distantlands.gameplay.powers.economy

import distantlands.gameplay.ModifierPlay
import distantlands.gameplay.actions.economy.TradeProcedure
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite}
import distantlands.gameplay.powers.action.AlchemistCard
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{EconomyActions, Journal, Look, PhasePowerHarness,
  PowerRules, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Beloved (L33): its holder trades on the faceup advisers of other
  * players at their site (Legacy in-play powers ruling 18; implementation
  * ruling R8). */
class BelovedSuite extends munit.FunSuite:
  private val rules = PowerRules.withWalkerPowers
  private val home = AncientCitySite.id
  /** A plain Arcane denizen. */
  private val alchemist = AlchemistCard.id
  /** A plain Order denizen. */
  private val wrestlers = WrestlersCard.id
  /** A plain Beast denizen. */
  private val errandBoy = ErrandBoyCard.id
  private val onWrestlers = DecisionOptionRef.Denizen(wrestlers)

  /** p1 at Ancient City beside Alchemist with the start's 1 faceup secret;
    * p2's pawn at Ancient City holding Wrestlers (`wrestlersFacedown` or
    * faceup); p3 at Buried Giant holding Errand Boy faceup; the Order bank
    * holds 3 favor. `legacies` sets the legacies. */
  private def trading(legacies: Table => Table,
      wrestlersFacedown: Boolean = false): Table =
    legacies(Table.start.pawn(p1, at = home).denizen(alchemist, at = home)
      .pawn(p2, at = home).adviser(p2, wrestlers, facedown = wrestlersFacedown)
      .pawn(p3, at = BuriedGiantSite).adviser(p3, errandBoy)
      .bankFavor(Suit.Order, 3))

  private val holder = trading(_.legacy(p1, BelovedCard))

  private def started(ready: ReadyGame): OathTransition =
    accepted(ModifierPlay.started(rules, Ready(ready), ActionRef.Trade, p1,
      PowerWindow.TradeModifierSelection, Vector.empty,
      startArgs = Vector(DecisionOptionRef.Button("favor"))))

  private def sources(ready: ReadyGame): Option[Vector[(String, String)]] =
    PhasePowerHarness.offered(started(ready), p1)

  test("the Trade's sources add p2's faceup adviser, not p3's"):
    assertEquals(sources(holder.ready), Some(Vector(
      "denizen" -> alchemist.value, "denizen" -> wrestlers.value)))

  test("trading on it places the secret on p2's adviser and takes favor " +
      "from its suit's bank"):
    val ready = holder.ready
    val done = EconomyActions.trade(rules, ready, p1, Vector.empty, "favor",
      onWrestlers)
    val after = Look(done.result)
    assertEquals(after.adviserTokens(p2, wrestlers), Tokens(0, 1))
    // 1: p1 holds no Order adviser.
    assertEquals(after.favor(p1), Look(ready).favor(p1) + 1)
    assertEquals(after.bankFavor(Suit.Order), 2)

  test("a facedown adviser is not a source"):
    assertEquals(sources(trading(_.legacy(p1, BelovedCard),
      wrestlersFacedown = true).ready),
      Some(Vector("denizen" -> alchemist.value)))

  test("an adviser holding tokens cannot be traded on, as at a site"):
    val occupied = holder.tokens(wrestlers, secrets = 1).ready
    assert(EconomyActions.attemptTrade(rules, occupied, p1, Vector.empty,
      "favor", onWrestlers).isLeft)

  test("without the legacy, or with it dormant, nothing is added " +
      "(ruling 1)"):
    val only = Some(Vector("denizen" -> alchemist.value))
    assertEquals(sources(trading(identity).ready), only)
    assertEquals(sources(trading(_.legacy(p1, BelovedCard,
      active = false)).ready), only)

  test("a walk resumed at the source decision trades on p2's adviser"):
    val ready = holder.ready
    val begun = started(ready)
    val resumed = accepted(Journal.evolved(rules, ready, begun.events))
    val done = accepted(rules.resolveWalker(resumed, p1,
      TradeProcedure.decisionId, DecisionAnswer.ChooseOneAnswer(onWrestlers)))
    assertEquals(Look(PhasePowerHarness.after(done)).adviserTokens(p2,
      wrestlers), Tokens(0, 1))
