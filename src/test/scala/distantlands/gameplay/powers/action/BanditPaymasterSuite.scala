package distantlands.gameplay.powers.action

import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

class BanditPaymasterSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val paymaster = BanditPaymasterCard.id
  private val source = DecisionOptionRef.Denizen(paymaster)
  private val home = AncientCitySite.id

  /** p1's Act at Ancient City holding Bandit Paymaster, with `bandits`
    * bandit warbands at the site. */
  private def staged(bandits: Int, favor: Int = 1): Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, home).adviser(p1, paymaster)
      .favor(p1, favor).bandits(home, bandits)

  test("at a site bandits rule, the player gains 2 warbands and no bandit " +
      "is removed"):
    val ready = staged(1).ready
    val t = accepted(use(ready, BanditPaymaster, source))
    parked.assertResumed(t.state, Phase.Act, p1)
    val end = after(t)
    assertEquals(Look(end).tokensOn(paymaster), Tokens(1, 0))
    assertEquals(Look(end).forces(home),
      SiteForces.Occupied(ForceKind.Bandit, 1))
    assertEquals(Look(end).boardWarbands(p1), Look(ready).boardWarbands(p1) + 2)
    assertEquals(NoteText.said(BanditPaymaster, t.events), Vector(
      NoteText.Said(NoteKey.Used, s"${p1.value} gained 2 warbands.",
        covers = false)))
    assertEquals(replayed(ready, t.events), Right(t.state))
    assertWireRoundTrips(t.events)

  test("at a site bandits do not rule, nothing is gained, and the line " +
      "says so"):
    val ready = staged(0).ready
    val t = accepted(use(ready, BanditPaymaster, source))
    val end = after(t)
    assertEquals(Look(end).tokensOn(paymaster), Tokens(1, 0))
    assertEquals(Look(end).boardWarbands(p1), Look(ready).boardWarbands(p1))
    assertEquals(NoteText.said(BanditPaymaster, t.events), Vector(
      NoteText.Said("used.none", s"${home.value} is not ruled by bandits.",
        covers = false)))

  test("a short supply gains what it holds"):
    val base = staged(1).ready
    val kind = Look(base).forceKind(p1)
    // The board holds the printed supply less 1, so the bank holds 1.
    val ready = staged(1).boardWarbands(p1, base.banks.warbandSupply(kind) - 1)
      .ready
    val t = accepted(use(ready, BanditPaymaster, source))
    assertEquals(Look(after(t)).boardWarbands(p1), Look(ready).boardWarbands(p1) + 1)
    assertEquals(NoteText.said(BanditPaymaster, t.events), Vector(
      NoteText.Said(NoteKey.Used, s"${p1.value} gained 1 warband.",
        covers = false)))

  test("it is unusable without a favor"):
    val broke = staged(1, favor = 0).ready
    assert(!usableNow(broke).exists(_.power.id == BanditPaymaster.id))
    assertEquals(use(broke, BanditPaymaster, source).left.toOption,
      Some(OathViolation.InsufficientFavor(1, 0)))
