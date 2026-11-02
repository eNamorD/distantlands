package oathdigital.gameplay.powers.rest

import oathdigital.catalog.Denizen
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.p1

/** Silver Tongue staged in the Rest phase, shared by its own suite, the
  * projection suite and the pending-walker invariant (Task 13).
  */
object SilverTongueFixture:
  val tongue: DenizenId = SilverTongueCard.id

  /** p1's Rest at Ancient City, holding Silver Tongue as the only adviser.
    * Ancient City shows `siteCards` faceup. Every bank in `stocked` holds 3
    * favor and every other bank none. Broken Peaks holds two secrets and p2's
    * pawn, so p2, waking next, has wealth to take and the Wake waits. p3
    * stands apart at Buried Giant: an enemy pawn there would block the take.
    * No site holds a bandit, so the action boundary visibly refills the sites
    * with capacity.
    */
  def silverTongueRest(siteCards: Vector[Denizen], stocked: Set[Suit])
      : ReadyGame =
    val resting = Table.start.turn(p1, Phase.Rest).pawn(p1, AncientCitySite)
      .pawn(Table.p2, BrokenPeaksSite).pawn(Table.p3, BuriedGiantSite)
      .adviser(p1, tongue)
      .siteTokens(BrokenPeaksSite, secrets = 2)
    val shown = siteCards.foldLeft(resting)(_.denizen(_, at = AncientCitySite))
    shown.banksHoldingOnly(stocked.toSeq.map(_ -> 3)*).ready
