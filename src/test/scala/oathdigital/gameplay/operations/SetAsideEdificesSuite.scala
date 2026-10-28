package oathdigital.gameplay.operations

import oathdigital.model._
import oathdigital.testkit.{CatalogNames, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Chronicle ruling 9: a discarded ruined edifice joins the set-aside list,
  * in discard order, and not the edifice deck. */
class SetAsideEdificesSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private val market = CatalogNames.edifice("Bandit Market")
  private val site = CatalogNames.site("Ancient City")

  /** The quiet table with Bandit Market, ruined, at Ancient City. */
  private val ruinedMarketAtAncientCity: ReadyGame = Table.start
    .edifice("Bandit Market", EdificeSide.Ruined, at = "Ancient City").ready

  private def discardMarket(ready: ReadyGame) = executor.execute(ready,
    Discard.RuinedEdifice(market, PositionedLocation(Location.Site(site)),
      Suit.Discord, favor = 0, secrets = 0, actingPlayer = p1))

  test("a discarded ruined edifice joins the set-aside list, not the deck"):
    val deck = ruinedMarketAtAncientCity.game.current.commonCards.edificeDeck
    val after = accepted(discardMarket(ruinedMarketAtAncientCity))
    assertEquals(after.game.current.setAsideEdifices, Vector(market))
    assertEquals(after.game.current.commonCards.edificeDeck, deck)

  test("the card index finds a set-aside edifice"):
    val after = accepted(discardMarket(ruinedMarketAtAncientCity))
    assertEquals(after.game.cardIndex.toOption.flatMap(_.locationOf(market))
      .map(_.container), Some(CardContainer.SetAsideEdifices))

  test("a set-aside edifice can return to the bottom of the edifice deck"):
    val aside = accepted(discardMarket(ruinedMarketAtAncientCity))
    val after = accepted(executor.execute(aside, Move(Piece.Card(market),
      PositionedLocation(Location.SetAsideEdifices),
      PositionedLocation(Location.Deck(CardDeck.Edifice), StackPosition.Bottom))))
    assertEquals(after.game.current.setAsideEdifices, Vector.empty)
    assertEquals(after.game.current.commonCards.edificeDeck.last, market)
