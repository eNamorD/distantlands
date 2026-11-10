package distantlands.gameplay.powers.recover

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.FairIsleSite
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.action.DowsingSticksCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{CatacombsGame, Look, Outcomes, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Recover asks for its modifiers inside the walk (modifier walker design,
  * slice 4). */
class RecoverModifierChoiceSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.shields)

  private val worship = RelicWorshipCard.id

  /** p1 at Fair Isle, where Dowsing Sticks lies facedown, with `supply`
    * Supply, one faceup secret and Relic Worship as a faceup adviser; with
    * no Relic Worship unless `withWorship`. */
  private def atFairIsle(supply: Int = 4, withWorship: Boolean = true)
      : ReadyGame =
    val table = Table.start.pawn(p1, FairIsleSite)
      .relicAt(DowsingSticksCard, FairIsleSite).secrets(p1, faceUp = 1)
      .supply(p1, supply)
    (if withWorship then table.adviser(p1, worship) else table).ready

  private def started(ready: ReadyGame): OathTransition =
    accepted(rules.startWalker(Ready(ready), ActionRef.Recover, p1))

  test("a Recover starts on the modifier decision when a modifier can be " +
      "picked"):
    val begun = started(atFairIsle())
    // Nothing is spent or rolled until the Recover starts.
    assertEquals(Look(begun.state).supply(p1), 4)
    assertEquals(CatacombsGame.modifierOption(begun.state, worship),
      DecisionOption.Priced(
        DecisionOption.Denizen(DecisionOptionRef.Denizen(worship)),
        OptionPrice(secrets = 1)))

  test("with no modifier to pick, a Recover rolls at once"):
    val begun = started(atFairIsle(withWorship = false))
    assertEquals(Look(begun.state).supply(p1), 4 - 1)
    assertEquals(
      Outcomes.ready(begun.state).game.current.walkerPending.map(_.at),
      // The relic decision, after the modifier decision asked nothing.
      Some(Vector("1", "2", "0")))

  test("a Recover without the Supply to roll is refused at the start, as " +
      "before"):
    // No pick makes it free, so there is nothing to ask about.
    assertEquals(rules.startWalker(Ready(atFairIsle(supply = 0)),
      ActionRef.Recover, p1).left.map(_.getClass),
      Left(classOf[OathViolation.CoreOperationRejected]))
