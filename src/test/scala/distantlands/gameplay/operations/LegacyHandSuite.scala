package distantlands.gameplay.operations

import distantlands.gameplay.chronicle.legacies.RivalCard
import distantlands.gameplay.setup.legacies.{PopulistCard, SeerCard}
import distantlands.model._
import distantlands.testkit.{RawOperationBatch, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Legacies drawn into a hand (catalog batch 13, ruling 6): `legacyHands`,
  * at the same `Location.Hand` as the world cards a Search draws. */
class LegacyHandSuite extends munit.FunSuite:
  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  private def handOf(ready: ReadyGame, player: PlayerId): Vector[LegacyId] =
    ready.game.current.legacyHands.getOrElse(player, Vector.empty)

  test("a draw takes the top legacies into the drawer's hand, in order"):
    val ready = Table.start.legacyDeck(SeerCard, PopulistCard, RivalCard).ready
    val after = accepted(RawOperationBatch.executeAll(ready, Vector(Draw(p1,
      Vector(SeerCard.id, PopulistCard.id), Location.Deck(CardDeck.Legacy),
      Location.Hand(p1)))))
    assertEquals(handOf(after, p1), Vector(SeerCard.id, PopulistCard.id))
    assertEquals(after.game.current.commonCards.legacyDeck,
      Vector(RivalCard.id))
    assertEquals(after.game.current.temporaryHands.getOrElse(p1, Vector.empty),
      Vector.empty)
    assertEquals(after.game.cardIndex.toOption.get.locationOf(PopulistCard.id),
      Some(CardLocation(CardContainer.Player(p1, PlayerCardArea.Hand), 1)))

  test("a draw cannot skip the top legacy"):
    val ready = Table.start.legacyDeck(SeerCard, PopulistCard).ready
    assert(RawOperationBatch.executeAll(ready, Vector(Draw(p1,
      Vector(PopulistCard.id), Location.Deck(CardDeck.Legacy),
      Location.Hand(p1)))).isLeft)

  test("a legacy kept from a hand joins the lineage dormant, and one " +
      "discarded goes to the bottom of the deck"):
    val ready = Table.start.legacyDeck(RivalCard)
      .legacyHand(p1, SeerCard, PopulistCard).ready
    val lineage = lineageOf(ready, p1)
    val hand = PositionedLocation(Location.Hand(p1))
    val after = accepted(RawOperationBatch.executeAll(ready, Vector(
      Move(Piece.Card(SeerCard.id), hand,
        PositionedLocation(Location.Legacies(lineage))),
      Move(Piece.Card(PopulistCard.id), hand,
        PositionedLocation.legacyDeckBottom))))
    assertEquals(handOf(after, p1), Vector.empty)
    assertEquals(after.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(SeerCard.id, active = false)))
    assertEquals(after.game.current.commonCards.legacyDeck,
      Vector(RivalCard.id, PopulistCard.id))
