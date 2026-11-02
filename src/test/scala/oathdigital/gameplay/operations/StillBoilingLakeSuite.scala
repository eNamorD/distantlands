package oathdigital.gameplay.operations

import oathdigital.gameplay.actions.{CardPlay, CardPlayRules}
import oathdigital.gameplay.actions.cardplay.CardSlots
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.powers.wake.AncientCitySite
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{PlacementPicks, Table}
import oathdigital.testkit.Table.p1

/** Still Boiling Lake (catalog batch 9, slice 5): no card may be played to
  * its site. */
class StillBoilingLakeSuite extends munit.FunSuite:
  private val home = AncientCitySite.id
  private val card = WrestlersCard.id
  private val reason = OptionReason.Written(
    "Still Boiling Lake: Cards cannot be played to this site.")

  /** p1 at Ancient City with Wrestlers in hand and the Lake on `side`
    * there. */
  private def lakeAtHome(side: EdificeSide): ReadyGame =
    Table.start.pawn(p1, at = home).hand(p1, card)
      .edifice(BoilingLakeCard.id, side, at = home).ready

  private def options(ready: ReadyGame): Vector[DecisionOption] =
    CardSlots.slots(catalog, ready, p1, card, CardPlay.Origin.TemporaryHand,
      CardPlayRules.default).map(_.option)

  test("the site's slot is shown unavailable with the Lake's reason, and " +
      "the adviser plays and the discard stay open"):
    val shown = options(lakeAtHome(EdificeSide.Ruined))
    val siteSlots = PlacementPicks.siteSlotOptions(shown)
    assert(siteSlots.nonEmpty)
    siteSlots.foreach(slot => assertEquals(slot,
      DecisionOption.Unavailable(slot match {
        case DecisionOption.Unavailable(inner, _) => inner
        case other => other
      }, reason)))
    assert(shown.exists {
      case DecisionOption.AdviserPlay(_) => true
      case _ => false
    })
    assert(shown.exists {
      case DecisionOption.DiscardPile(_) => true
      case _ => false
    })

  test("the intact Boiling Lake leaves the site open"):
    assert(PlacementPicks.siteSlotOptions(options(lakeAtHome(
      EdificeSide.Intact))).exists(_.available))

  test("the restriction refuses a play to the site only: a card moved " +
      "there, or played elsewhere, is not refused"):
    val ready = lakeAtHome(EdificeSide.Ruined)
    val rule = StillBoilingLake
    val from = PositionedLocation(Location.Hand(p1))
    assertEquals(rule.reason(ready, Play(card, from, Location.Site(home),
      Orientation.FaceUp)).map(_.code), Some(StillBoilingLake.code))
    assertEquals(rule.reason(ready, Play(card, from,
      Location.Site(SiteId("site:broken-peaks")), Orientation.FaceUp)), None)
    assertEquals(rule.reason(ready, Play(card, from, Location.PlayArea(p1),
      Orientation.FaceDown)), None)
    assertEquals(rule.reason(ready, Move(Piece.Card(card), from,
      PositionedLocation(Location.Site(home)))), None)
    assertEquals(StillBoilingLake.noteKeys, Vector.empty)
    assert(OperationRestrictions.printedRules.contains(StillBoilingLake))
