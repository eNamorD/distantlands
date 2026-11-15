package distantlands.gameplay.operations

import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules,
  LegacyDeal}
import distantlands.gameplay.setup.legacies.{PopulistCard, SeerCard}
import distantlands.model._
import distantlands.testkit.{RawOperationBatch, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** The stored legacy deck (catalog batch 13, rulings 1 and 2): game start
  * keeps what Foundation III's deal leaves, a draw takes from the top, and
  * a returned or discarded legacy goes to the bottom. */
class LegacyDeckSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val chronicle = FirstGameSetupFixture.chronicle
  private val orders = FirstGameSetupFixture.orders

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  private def deckOf(ready: ReadyGame): Vector[LegacyId] =
    ready.game.current.commonCards.legacyDeck

  test("Quiet Ambitions' game start keeps the whole order as the deck"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle, orders))
    assertEquals(deckOf(ready), orders.legacyOrder)
    assertEquals(ready.game.cardIndex.toOption.get.get(deckOf(ready).head)
      .map(_.location), Some(CardLocation(CardContainer.Deck(CardDeck.Legacy), 0)))

  test("Public Ambitions' game start keeps the order the row leaves"):
    val altered = chronicle.copy(foundations = chronicle.foundations.updated(
      FoundationNumber.III, FoundationState(FoundationFace.Altered, Set.empty)))
    val ready = accepted(GameStartRules.evolve(catalog, altered, orders))
    assertEquals(deckOf(ready), orders.legacyOrder.drop(LegacyDeal.PublicRow))

  test("a legacy returned to the deck goes to its bottom"):
    val ready = Table.start.legacy(p1, SeerCard).legacyDeck(RivalCard).ready
    val after = accepted(RawOperationBatch.executeAll(ready, Vector(Move(
      Piece.Card(SeerCard.id),
      PositionedLocation(Location.Legacies(lineageOf(ready, p1))),
      PositionedLocation.legacyDeckBottom))))
    assertEquals(deckOf(after), Vector(RivalCard.id, SeerCard.id))
    assertEquals(after.game.campaign.lineages(lineageOf(ready, p1)).legacies,
      Vector.empty)

  test("a move into the legacy deck names the top or the bottom"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    assertEquals(RawOperationBatch.executeAll(ready, Vector(Move(
      Piece.Card(SeerCard.id),
      PositionedLocation(Location.Legacies(lineageOf(ready, p1))),
      PositionedLocation(Location.Deck(CardDeck.Legacy))))).left.toOption,
      Some(OperationError.InvalidStackPosition(Location.Deck(CardDeck.Legacy),
        "stack destination must specify top or bottom")))

  test("a legacy taken from the top of the deck joins a lineage dormant"):
    val ready = Table.start.legacyDeck(RivalCard, PopulistCard).ready
    val lineage = lineageOf(ready, p1)
    val after = accepted(RawOperationBatch.executeAll(ready, Vector(Move(
      Piece.Card(RivalCard.id),
      PositionedLocation(Location.Deck(CardDeck.Legacy), StackPosition.Top),
      PositionedLocation(Location.Legacies(lineage))))))
    assertEquals(after.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(RivalCard.id, active = false)))
    assertEquals(deckOf(after), Vector(PopulistCard.id))

  test("only the top legacy can be taken from the top"):
    val ready = Table.start.legacyDeck(RivalCard, PopulistCard).ready
    assertEquals(RawOperationBatch.executeAll(ready, Vector(Move(
      Piece.Card(PopulistCard.id),
      PositionedLocation(Location.Deck(CardDeck.Legacy), StackPosition.Top),
      PositionedLocation(Location.Legacies(lineageOf(ready, p1)))))).left
      .toOption, Some(OperationError.InvalidStackPosition(
        Location.Deck(CardDeck.Legacy),
        "card does not match requested stack position")))
