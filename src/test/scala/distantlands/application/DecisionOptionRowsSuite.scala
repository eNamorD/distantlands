package oathdigital.application

import oathdigital.gameplay.cards.BrokenPeaksSite
import oathdigital.gameplay.powers.campaign.StickyFireCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.p1

/** How one option reads as a row, through [[DecisionOptionRows]]'s own
  * interface and no walker park: the wrappers an option carries (price,
  * badge) and the words a board object is named by. `SlotOptionRowsSuite`
  * covers the card-slot rows the same way.
  */
class DecisionOptionRowsSuite extends munit.FunSuite:
  private val rows = new DecisionOptionRows(catalog,
    new GamePresentationProjector(catalog))

  /** Sticky Fire lies facedown at Broken Peaks, and p1's pawn stands there,
    * which is what lets p1 identify a facedown site relic. */
  private val board: ReadyGame = Table.start
    .relicAt(StickyFireCard, at = BrokenPeaksSite)
    .pawn(p1, at = BrokenPeaksSite).ready

  private def rowOf(ready: ReadyGame, option: DecisionOption) =
    rows.project(ready, Some(p1), CardIndex.from(ready.game).toOption, option)

  test("a badged option projects its badge and keeps its price"):
    val option = DecisionOption.Badged(DecisionOption.Priced(
      DecisionOption.Relic(DecisionOptionRef.Relic(StickyFireCard.id)),
      OptionPrice(favor = 1)), "Battle Plan")
    val projected = rowOf(board, option)
    assertEquals(projected.flatMap(_.badge), Some("Battle Plan"))
    assertEquals(projected.map(_.details), Some(Vector("Cost: 1 [favor]")))

  test("a banner is named as it is printed, and counts what it holds"):
    val option = DecisionOption.Banner(DecisionOptionRef.Banner(Banner.DarkestSecret))
    val unclaimed = Table.start.darkestSecret(None, secrets = 3).ready
    val row = rowOf(unclaimed, option)
      .getOrElse(fail("an unclaimed banner must project"))
    // No owner in the label: the banner is one object on the table, and who
    // holds it is read off the board. The count names the resource it takes.
    assertEquals((row.kind, row.id, row.label, row.details),
      ("banner", "darkest-secret", "Darkest Secret",
        Vector("Currently 3 secrets")))
