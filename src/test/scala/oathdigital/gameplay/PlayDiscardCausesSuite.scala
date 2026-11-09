package oathdigital.gameplay

import oathdigital.gameplay.actions.{CardPlayRules, DiscardPermission}
import oathdigital.gameplay.actions.cardplay.PlayDiscardCauses
import oathdigital.model._
import oathdigital.testkit.Table.p1

/** The causes of a card play's discards (catalog batch 11, rulings 20 and
  * 22). */
class PlayDiscardCausesSuite extends munit.FunSuite:
  private val site = SiteId("site")
  private val played = DenizenId("played")
  private val replaced = DenizenId("replaced")
  private val atSite = PositionedLocation(Location.Site(site))
  private val crop = PowerId("denizen.crop-rotation")
  private val searchParty = PowerId("denizen.search-party")

  private def siteDiscard(card: String): Discard.Denizen = Discard.Denizen(
    DenizenId(card), atSite, Region.Provinces, Suit.Nomad, 0, 0, p1)
  private val adviserReplaced = Discard.Denizen(replaced,
    PositionedLocation(Location.PlayArea(p1)), Region.Provinces, Suit.Nomad,
    0, 0, p1)
  private val playedDiscarded = Discard.Denizen(played,
    PositionedLocation(Location.RegionalDiscard(Region.Cradle)),
    Region.Provinces, Suit.Nomad, 0, 0, p1)
  private val cropPermission = DiscardPermission.lined(crop,
    PowerSourceRef.Card(DenizenId("128")))

  test("a site discard names the power whose permission it spends, " +
      "making room when the site was full"):
    val ops = Vector[CoreOperation](siteDiscard("a"), siteDiscard("b"))
    val permits = Vector(CardPlayRules.homeland(site), cropPermission)
    assertEquals(PlayDiscardCauses.mark(ops, played, None, permits,
      siteFull = false), Vector[CoreOperation](siteDiscard("a"),
      siteDiscard("b").copy(cause = Some(DiscardCause(crop)))))
    assertEquals(PlayDiscardCauses.mark(Vector(siteDiscard("a")), played,
      None, Vector(cropPermission), siteFull = true),
      Vector[CoreOperation](siteDiscard("a").copy(cause =
        Some(DiscardCause(crop, makesRoom = true)))))

  test("a play a power made names that power on its replaced adviser, " +
      "making room, and on its played card"):
    assertEquals(PlayDiscardCauses.mark(
      Vector(adviserReplaced, playedDiscarded), played, Some(searchParty),
      Vector.empty, siteFull = false), Vector[CoreOperation](
      adviserReplaced.copy(cause =
        Some(DiscardCause(searchParty, makesRoom = true))),
      playedDiscarded.copy(cause = Some(DiscardCause(searchParty)))))

  test("a plain play's discards name nothing"):
    val ops = Vector[CoreOperation](adviserReplaced, playedDiscarded)
    assertEquals(PlayDiscardCauses.mark(ops, played, None, Vector.empty,
      siteFull = false), ops)
