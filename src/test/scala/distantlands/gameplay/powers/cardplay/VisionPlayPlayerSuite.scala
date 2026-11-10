package distantlands.gameplay.powers.cardplay

import distantlands.gameplay.SacredGroundCard
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powerresolver.{PowerCtx, Restriction}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** A faceup Vision play is judged for the player the hook names, not for
  * the walk's actor (catalog batch 11, slice 7): False Prophet's claimant
  * reveals inside the Vision owner's walk. */
class VisionPlayPlayerSuite extends munit.FunSuite:
  private val vision = VisionRules.Faith

  /** p1's turn, Sacred Ground intact at `sacred`; p1 stands at Ancient
    * City and p2 at Broken Peaks; Faith lies in the Provinces discard. */
  private def pawnsApartWithSacredGroundAt(sacred: SiteId): ReadyGame =
    Table.start.pawn(p1, at = AncientCitySite).pawn(p2, at = BrokenPeaksSite)
      .edifice(SacredGroundCard.id, EdificeSide.Intact, at = sacred)
      .discarded(Region.Provinces, vision).ready

  /** Sacred Ground's refusal of `player`'s reveal of Faith in p1's walk. */
  private def sacredGroundRefusal(sacred: SiteId, player: PlayerId)
      : Option[OathViolation] =
    val hook = CardPlayedFaceup(vision, player,
      RuleSourceRef.Adviser(player, vision))
    val ctx = PowerCtx(catalog, pawnsApartWithSacredGroundAt(sacred), p1,
      SacredGround.source, PowerWindow.ActionCardPlayedFaceup, Vector.empty,
      hook, None)
    SacredGround.contributions(PowerWindow.ActionCardPlayedFaceup).collectFirst {
      case Restriction(fn, _, _) => fn(ctx, hook)
    }.flatten

  test("p2's reveal on p1's turn is refused where p2's pawn is not"):
    // Sacred Ground stands at p1's site: p1 could reveal, p2 cannot.
    assertEquals(sacredGroundRefusal(AncientCitySite.id, p1), None)
    assertEquals(sacredGroundRefusal(AncientCitySite.id, p2),
      Some(VisionPlay.forbidden("Sacred Ground")))

  test("p2's reveal on p1's turn is allowed where p2's pawn is"):
    assertEquals(sacredGroundRefusal(BrokenPeaksSite.id, p2), None)

  test("Book Binders rewards its holder when another player reveals in " +
      "the holder's walk"):
    // p1 holds Book Binders and is the walk's actor; p2 reveals Faith.
    val ready = Table.start.adviser(p1, BookBindersCard.id)
      .discarded(Region.Provinces, vision).ready
    def applies(player: PlayerId): Boolean =
      val hook = CardPlayedFaceup(vision, player,
        RuleSourceRef.Adviser(player, vision))
      BookBinders.applicable(PowerCtx(catalog, ready, p1, BookBinders.source,
        PowerWindow.ActionCardPlayedFaceup, Vector.empty, hook, None))
    assert(applies(p2))
    assert(!applies(p1))
