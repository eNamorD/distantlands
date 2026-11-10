package distantlands.gameplay.powers.economy

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, EconomyActions, Look, PowerRules,
  Table}
import distantlands.testkit.Table.p1

/** Hallowed Spring (catalog batch 9, slice 5): a Trade for secrets at its
  * site places both favor on the card instead of burning one. */
class HallowedSpringSuite extends munit.FunSuite:
  private val rules = PowerRules.withWalkerPowers
  private val home = AncientCitySite.id
  private val wrestlers = WrestlersCard.id
  private val source = DecisionOptionRef.Denizen(wrestlers)

  /** p1 in Act at Ancient City with 3 favor and 3 Supply beside Wrestlers,
    * and the Spring on `side` at `at`. */
  private def springAt(at: SiteId, side: EdificeSide = EdificeSide.Intact)
      : ReadyGame =
    Table.start.pawn(p1, at = home).denizen(wrestlers, at = home)
      .edifice(HallowedSpringCard.id, side, at = at)
      .favor(p1, 3).supply(p1, 3).ready

  test("a Trade for secrets at the Spring's site places 2 favor on the " +
      "card and burns none, and the journal replays"):
    val ready = springAt(home)
    val done = EconomyActions.trade(rules, ready, p1, Vector.empty, "secret",
      source)
    // Both favor left the board for the card: none went to the shared bank.
    assertEquals(Look(done.result).tokensOn(wrestlers), Tokens(2, 0))
    assertEquals(Look(done.result).favor(p1), 1)
    assertEquals(ActPowerHarness.replayed(rules, ready, done.journal.events),
      done.result)

  test("a Trade for favor is unchanged"):
    val done = EconomyActions.trade(rules, Table.from(springAt(home))
      .secrets(p1, faceUp = 1).ready, p1, Vector.empty, "favor", source)
    assertEquals(Look(done.result).tokensOn(wrestlers), Tokens(0, 1))
    // 3, plus the Order favor the Trade gained.
    assertEquals(Look(done.result).favor(p1), 4)

  test("a Trade elsewhere, or under the ruined face, pays the printed cost"):
    val elsewhere = EconomyActions.trade(rules, springAt(BrokenPeaksSite.id),
      p1, Vector.empty, "secret", source)
    // One favor placed, one burnt: the printed cost.
    assertEquals(Look(elsewhere.result).tokensOn(wrestlers), Tokens(1, 0))
    assertEquals(Look(elsewhere.result).favor(p1), 1)
    val ruined = EconomyActions.trade(rules, springAt(home, EdificeSide.Ruined),
      p1, Vector.empty, "secret", source)
    assertEquals(Look(ruined.result).tokensOn(wrestlers), Tokens(1, 0))
    assertEquals(HallowedSpring.noteKeys, Vector.empty)
