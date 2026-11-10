package distantlands.gameplay

import distantlands.gameplay.actions.RecoverRules
import distantlands.gameplay.cards.{BrokenPeaksSite, DeepWoodsSite}
import distantlands.gameplay.powers.recover.RelicWorshipCard
import distantlands.gameplay.setup.FirstGameSetupFixture._
import distantlands.model._
import distantlands.testkit.{MenuPlay, Table}
import distantlands.testkit.Table.p1

/** Covers Recover's availability in the Act menu: the menu delegates to the
  * same Recover procedure builder as command execution, and relic
  * availability does not gate the action.
  */
class RecoverEligibilitySuite extends munit.FunSuite:
  /** p1, in Act, stands at Broken Peaks, whose Recover difficulty is 4. On
    * the quiet table the site holds no relic and no card, Catacombs
    * included. */
  private val recoverSite = BrokenPeaksSite.id
  private def atRecoverSite: Table = Table.start.pawn(p1, at = recoverSite)

  private def recoverAvailable(ready: ReadyGame): Boolean =
    MenuPlay.availableInMenu(catalog, ready, ActionRef.Recover)

  test("Recover is available without a relic or Catacombs"):
    assertEquals(RecoverRules.difficulty(catalog, recoverSite), Some(4))
    assert(recoverAvailable(atRecoverSite.ready))

  test("Recover is not available without the Supply to pay for it"):
    def withSupply(supply: Int) = atRecoverSite.supply(p1, supply).ready
    assert(recoverAvailable(withSupply(1)))
    assert(!recoverAvailable(withSupply(0)))

  test("Recover is not available at a site with no Recover difficulty, " +
      "whether or not a modifier could be picked"):
    assertEquals(RecoverRules.difficulty(catalog, DeepWoodsSite.id), None)
    val atDeepWoods = Table.start.pawn(p1, at = DeepWoodsSite)
    def menuReason(table: Table): Option[String] =
      MenuPlay.menuOptionFor(catalog, table.ready, ActionRef.Recover) match
        case DecisionOption.Unavailable(_, OptionReason.Written(text)) =>
          Some(text)
        case _ => None
    assertEquals(menuReason(atDeepWoods), Some("Cannot recover here"))
    // Relic Worship may be picked, so the modifier decision would be asked.
    assertEquals(menuReason(atDeepWoods.adviser(p1, RelicWorshipCard)
      .secrets(p1, faceUp = 1)), Some("Cannot recover here"))
