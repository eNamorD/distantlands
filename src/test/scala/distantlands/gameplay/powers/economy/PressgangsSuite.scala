package distantlands.gameplay.powers.economy

import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.action.AlchemistCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.testkit.MenuPlay
import distantlands.gameplay.ModifierPlay
import distantlands.gameplay.actions.economy.MusterProcedure
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.ModifierOffers
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, EconomyActions, Look,
  PhasePowerHarness, PowerRules, Refusals, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

class PressgangsSuite extends munit.FunSuite:
  private val pressgangs = PressgangsCard.id
  private val golem = GolemLegionsCard.id
  private val selected = Vector(Pressgangs.id)
  private val home = AncientCitySite.id
  /** A plain Arcane denizen. */
  private val alchemist = AlchemistCard.id
  /** A plain Beast denizen. */
  private val errandBoy = ErrandBoyCard.id
  private val rules = PowerRules.withWalkerPowers

  /** p1 at Ancient City holding Pressgangs faceup, with 2 favor and 7
    * Supply, beside Alchemist, which holds 1 favor and 1 secret. */
  private def occupiedAlchemist: Table = Table.start.pawn(p1, at = home)
    .adviser(p1, pressgangs).favor(p1, 2)
    .denizen(alchemist, at = home).tokens(alchemist, favor = 1, secrets = 1)

  private val musterModifiers =
    ModifierPicks.decisionId(PowerWindow.MusterModifierSelection)

  private def musterStarted(ready: ReadyGame, modifiers: Vector[PowerId])
      : OathTransition =
    accepted(ModifierPlay.started(rules, Ready(ready), ActionRef.Muster, p1,
      PowerWindow.MusterModifierSelection, modifiers))

  private def states(started: OathTransition)
      : Option[Vector[(String, String)]] =
    PhasePowerHarness.queryOf(started, p1).map(_.offeredOptions
      .map(option => (option.id, option.state)))

  private def modifiers(ready: ReadyGame, action: ActionRef)
      : Vector[PowerId] =
    accepted(ModifierOffers.offerable(WalkerPowerCatalog.default(catalog),
      ready, p1, action)).map(_.id)

  test("without it, a card holding favor or secrets is shown unavailable " +
      "and refused"):
    val ready = occupiedAlchemist.denizen(errandBoy, at = home).ready
    val started = musterStarted(ready, Vector.empty)
    assertEquals(states(started), Some(Vector(
      alchemist.value -> "unavailable", errandBoy.value -> "available")))
    assertEquals(rules.resolveWalker(started.state, p1,
      MusterProcedure.decisionId,
      ChooseOneAnswer(DecisionOptionRef.Denizen(alchemist))).left.toOption,
      Some(Refusals.notOffered(MusterProcedure.decisionId)))

  test("with only such a card at the site, Muster cannot start until it is " +
      "picked"):
    val ready = occupiedAlchemist.ready
    assertEquals(states(accepted(rules.startWalker(Ready(ready),
      ActionRef.Muster, p1))), Some(Vector("start" -> "unavailable",
      pressgangs.value -> "available")))
    assert(MenuPlay.availableInMenu(catalog, ready, ActionRef.Muster))
    assertEquals(states(musterStarted(ready, selected)),
      Some(Vector(alchemist.value -> "available")))

  test("selected, Muster places its favor beside the tokens already there"):
    val ready = occupiedAlchemist.ready
    val done = EconomyActions.muster(rules, ready, p1, selected,
      DecisionOptionRef.Denizen(alchemist))
    val after = Look(done.result)
    assertEquals(after.tokensOn(alchemist), Tokens(2, 1))
    // 1: Pressgangs is Order, not Arcane.
    assertEquals(after.boardWarbands(p1), Look(ready).boardWarbands(p1) + 1)
    assertEquals(ActPowerHarness.replayed(rules, ready, done.journal.events),
      done.result)
    assertWireRoundTrips(done.journal.events)

  test("with Golem Legions, Golem Legions is mustered on twice in one turn"):
    val ready = Table.start.pawn(p1, at = home).adviser(p1, golem)
      .adviser(p1, pressgangs).favor(p1, 2).ready
    val first = EconomyActions.muster(rules, ready, p1,
      Vector(GolemLegions.id), DecisionOptionRef.Denizen(golem))
    // Golem Legions now holds favor, so with it picked alone the Muster
    // cannot start: Start waits for Pressgangs.
    val again = accepted(rules.startWalker(Ready(MenuPlay.withoutMenu(first.result)),
      ActionRef.Muster, p1))
    val golemPicked = accepted(rules.resolveWalker(again.state, p1,
      musterModifiers, ModifierPlay.pick(GolemLegions.id)))
    assertEquals(states(golemPicked), Some(Vector(
      "start" -> "unavailable", pressgangs.value -> "available")))
    val second = EconomyActions.muster(rules, first.result, p1,
      Vector(GolemLegions.id, Pressgangs.id), DecisionOptionRef.Denizen(golem))
    assertEquals(Look(second.result).tokensOn(golem), Tokens(2, 0))

  test("it is a Muster modifier only"):
    val ready = occupiedAlchemist.ready
    assert(modifiers(ready, ActionRef.Muster).contains(Pressgangs.id))
    assert(!modifiers(ready, ActionRef.Trade).contains(Pressgangs.id))
