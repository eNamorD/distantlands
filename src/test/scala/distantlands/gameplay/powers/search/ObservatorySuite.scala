package distantlands.gameplay.powers.search

import distantlands.gameplay.SearchPlay
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.SearchFixture
import distantlands.gameplay.powers.action.{FaeMerchantCard, HomesteadersCard,
  ScryerCard, SecondChanceCard}
import distantlands.gameplay.powers.campaign.{MercenariesCard, WrestlersCard}
import distantlands.gameplay.powers.economy.DefameCard
import distantlands.gameplay.powers.whenplayed.{BlackmailCard,
  ThreateningRoarCard}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Observatory (card 64): while the actor's pawn is at its site, every
  * region's discard pile is offered, each at 2 Supply (catalog batch 6,
  * slice 3). */
class ObservatorySuite extends munit.FunSuite:
  import SearchFixture.rules

  private val observatory = ObservatoryCard.id
  private val modifiers = Vector(Observatory.id)
  /** Three plain denizens in each region's discard pile; none is played. */
  private val piles: Map[Region, Vector[DenizenId]] = Map(
    Region.Cradle -> Vector(ThreateningRoarCard, FaeMerchantCard,
      SecondChanceCard),
    Region.Provinces -> Vector(WrestlersCard, MercenariesCard,
      HomesteadersCard),
    Region.Hinterland -> Vector(DefameCard, ScryerCard, BlackmailCard))
    .view.mapValues(_.map(_.id)).toMap
  /** The world deck, then each region's pile in region order. */
  private val everySource: Vector[DecisionOptionRef] =
    (SearchSource.WorldDeck +: Region.all.map(SearchSource.RegionalDiscard(_)))
      .map(SearchSource.ref)

  /** p1 at Ancient City (Cradle), which holds Observatory, with 5 Supply
    * and three cards in each region's discard pile. */
  private def besideObservatory: Table = Region.all.foldLeft(Table.start
      .pawn(p1, AncientCitySite).denizen(observatory, at = AncientCitySite)
      .supply(p1, 5))((table, region) => table.discarded(region,
    piles(region)*))

  test("picked, every region's discard pile is offered at 2 Supply"):
    val shown = ShownOptions.sourcesAfterPicking(besideObservatory.ready,
      modifiers)
    assertEquals(shown.map(option => (option.kind, option.id, option.state,
      option.badge)), everySource.map(ref => (ref.kind, ref.wireId,
      "available", Some("2 Supply"))))

  test("a Search of another region's pile draws its top three for 2 " +
      "Supply"):
    val searched = accepted(SearchPlay.searched(rules,
      Ready(besideObservatory.ready), p1,
      SearchSource.RegionalDiscard(Region.Provinces), modifiers))
    val after = SearchFixture.after(searched)
    assertEquals(after.game.current.temporaryHands(p1),
      piles(Region.Provinces).reverse)
    assertEquals(Look(after).supply(p1), 5 - 2)

  test("it is offered only while p1's pawn is at its site"):
    val offered = (ready: ReadyGame) =>
      accepted(rules.offerableWalkerPowers(ready, p1, ActionRef.Search))
        .map(_.id)
    assert(offered(besideObservatory.ready).contains(Observatory.id))
    assert(!offered(besideObservatory.pawn(p1, BrokenPeaksSite).ready)
      .contains(Observatory.id))
