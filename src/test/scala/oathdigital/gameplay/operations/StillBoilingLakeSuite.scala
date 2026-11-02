package oathdigital.gameplay.operations

import oathdigital.gameplay.actions.{CardPlay, CardPlayRules}
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import oathdigital.gameplay.actions.cardplay.CardSlots
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.powers.{SearchFixture, WalkerPowerCatalog}
import oathdigital.gameplay.powers.action.{ArcaneBrokersCard,
  WardOfSilenceCard}
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.powers.search.ErrandBoyCard
import oathdigital.gameplay.powers.whenplayed.{Riots, RiotsCard,
  WhenPlayedHarness}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, PlacementPicks, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Still Boiling Lake (catalog batch 9, slice 5): no card may be played to
  * its site, until a discard effect takes the Lake away. */
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
      Location.Site(BrokenPeaksSite.id), Orientation.FaceUp)), None)
    assertEquals(rule.reason(ready, Play(card, from, Location.PlayArea(p1),
      Orientation.FaceDown)), None)
    assertEquals(rule.reason(ready, Move(Piece.Card(card), from,
      PositionedLocation(Location.Site(home)))), None)
    assertEquals(StillBoilingLake.noteKeys, Vector.empty)
    assert(OperationRestrictions.printedRules.contains(StillBoilingLake))

  // ---- Discarding the Lake (owner's ruling) ----

  private val lake = BoilingLakeCard.id
  private val walkerPowers = WalkerPowerCatalog.default(catalog)

  /** The parked placement's site slots at Ancient City, by the card each
    * replaces: `None` for the empty slot. */
  private def siteSlotsOf(at: OathTransition)
      : Vector[(Option[CardId], DecisionOption)] =
    PlacementPicks.siteSlotOptions(PlacementPicks.parkedOptions(at.state,
      walkerPowers)).map(option => PlacementPicks.replaces(option) -> option)

  test("the Mob of the People's Favor discards the Lake first, and the " +
      "play goes through to the site"):
    // p1 holds the Mob at Ancient City, the ruined Lake there, and Searches
    // a deck topped by Errand Boy, which they keep.
    val played = ErrandBoyCard.id
    val ready = Table.from(SearchFixture.staged(Vector(played,
        WardOfSilenceCard.id, ArcaneBrokersCard.id), supply = 5))
      .edifice(lake, EdificeSide.Ruined, at = home)
      .peoplesFavor(Some(p1), 1).ready
    val asked = accepted(SearchFixture.start(ready).flatMap(started =>
      SearchFixture.keep(started, played)))
    val slots = siteSlotsOf(asked)
    // The empty slot is refused by the Lake; the slot that discards the
    // Lake first is open.
    assertEquals(slots.map(_._1), Vector(None, Some(lake)))
    assertEquals(slots.head._2 match {
      case DecisionOption.Unavailable(_, shown) => Some(shown)
      case _ => None
    }, Some(reason))
    assert(slots(1)._2.available, slots(1)._2.toString)
    val done = accepted(SearchFixture.placeReplacing(asked, played,
      PlayedAs.AtSite, lake))
    val after = SearchFixture.after(done)
    assertEquals(Look(after).cardsAt(home), Vector[CardId](played))
    assert(after.game.current.commonCards.edificeDeck.contains(lake))
    // With the Lake gone, a plain play to the site is open again.
    assert(PlacementPicks.siteSlotOptions(options(Table.from(after)
      .hand(p1, card).ready)).exists(_.available))

  test("without the Mob the Lake's site slot is refused, and the Lake is " +
      "not offered as a Replace"):
    val ready = Table.from(SearchFixture.staged(Vector(ErrandBoyCard.id,
        WardOfSilenceCard.id, ArcaneBrokersCard.id), supply = 5))
      .edifice(lake, EdificeSide.Ruined, at = home).ready
    val asked = accepted(SearchFixture.start(ready).flatMap(started =>
      SearchFixture.keep(started, ErrandBoyCard.id)))
    assertEquals(siteSlotsOf(asked).map((replaced, option) =>
      replaced -> option.available), Vector(None -> false))

  test("Riots' region discard takes the ruined Lake, which no lock " +
      "protects, and the site opens to card play"):
    val ready = Table.start.pawn(p1, at = home).adviser(p1, RiotsCard)
      .edifice(lake, EdificeSide.Ruined, at = home).peoplesFavor(None, 0)
      .ready
    assert(!LockedCards.showing(ready, lake))
    assert(LockedCards.showing(lakeAtHome(EdificeSide.Intact), lake))
    val done = Outcomes.finished(WhenPlayedHarness.play(ready,
      WhenPlayedHarness.registered[Riots.type], RiotsCard.id))
    val after = done.treeless
    assertEquals(Look(after).cardsAt(home), Vector.empty)
    assert(after.game.current.commonCards.edificeDeck.contains(lake))
    assert(PlacementPicks.siteSlotOptions(options(Table.from(after)
      .hand(p1, card).ready)).exists(_.available))
