package distantlands.model

import distantlands.testkit.Table.p1

/** A discard's cause (catalog batch 11, slice 4): the power whose use
  * discards a card, and whether the discard makes room that power fills. */
class DiscardCauseSuite extends munit.FunSuite:
  private val atSite = PositionedLocation(Location.Site(SiteId("site")))
  private val denizenDiscard = Discard.Denizen(DenizenId("D1"), atSite,
    Region.Provinces, Suit.Nomad, 0, 0, p1)
  private val edificeDiscard = Discard.RuinedEdifice(EdificeId("E1"),
    atSite, Suit.Nomad, 0, 0, p1)
  private val visionDiscard = Discard.Vision(VisionId("vision:v1"),
    PositionedLocation(Location.PlayArea(p1)), Region.Cradle)
  private val cause = DiscardCause(PowerId("denizen.taming-charm"))

  test("a discard has no cause unless its builder gives one"):
    assertEquals(denizenDiscard.cause, None)
    assertEquals(edificeDiscard.cause, None)
    assertEquals(cause.makesRoom, false)

  test("marking gives every denizen and ruined-edifice discard the cause, " +
      "and leaves other operations alone"):
    val spend = SpendSupply(p1, 1)
    assertEquals(DiscardCause.mark(Vector(denizenDiscard, edificeDiscard,
      visionDiscard, spend), cause), Vector[CoreOperation](
      denizenDiscard.copy(cause = Some(cause)),
      edificeDiscard.copy(cause = Some(cause)), visionDiscard, spend))
