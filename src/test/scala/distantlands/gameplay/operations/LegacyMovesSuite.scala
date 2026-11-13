package distantlands.gameplay.operations

import distantlands.catalog.holding.{RivalCard, SeerCard}
import distantlands.gameplay.powers.action.AlchemistCard
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted
import distantlands.testkit.RawOperationBatch

/** `Move` between a lineage's legacies, the public row and the derived
  * legacy deck (Legacies design, slice 1). */
class LegacyMovesSuite extends munit.FunSuite:
  private def applied(ready: ReadyGame, ops: CoreOperation*)(
      using munit.Location): ReadyGame =
    accepted(RawOperationBatch.executeAll(ready, ops.toVector))

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  private def moved(card: CardId, from: Location, to: Location,
      facing: Option[Orientation] = None): Move =
    Move(Piece.Card(card), PositionedLocation(from), PositionedLocation(to),
      resultingOrientation = facing)

  test("a public legacy taken faceup joins the lineage active"):
    val ready = Table.start.publicLegacies(RivalCard).ready
    val lineage = lineageOf(ready, p2)
    val after = applied(ready, moved(RivalCard.id, Location.PublicLegacies,
      Location.Legacies(lineage), Some(Orientation.FaceUp)))
    assertEquals(after.game.current.commonCards.publicLegacies, Vector.empty)
    assertEquals(after.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(RivalCard.id, active = true)))

  test("a legacy entering a lineage with no orientation is dormant"):
    val ready = Table.start.publicLegacies(RivalCard).ready
    val lineage = lineageOf(ready, p2)
    val after = applied(ready, moved(RivalCard.id, Location.PublicLegacies,
      Location.Legacies(lineage)))
    assertEquals(after.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(RivalCard.id, active = false)))

  test("a legacy returned to the deck leaves play"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    val lineage = lineageOf(ready, p1)
    val after = applied(ready, moved(SeerCard.id, Location.Legacies(lineage),
      Location.Deck(CardDeck.Legacy)))
    assertEquals(after.game.campaign.lineages(lineage).legacies, Vector.empty)
    assertEquals(after.game.cardIndex.toOption.get.get(SeerCard.id), None)

  test("only a legacy enters a lineage's legacies or the public row"):
    val ready = Table.start.hand(p1, AlchemistCard).ready
    val lineage = lineageOf(ready, p1)
    Vector(Location.Legacies(lineage), Location.PublicLegacies).foreach(to =>
      assert(RawOperationBatch.executeAll(ready, Vector(moved(
        AlchemistCard.id, Location.Hand(p1), to))).left.toOption
        .exists(_.isInstanceOf[OperationError.InvalidDestination]), s"$to"))

  test("a dormant legacy flipped faceup becomes active, and back"):
    val ready = Table.start.legacy(p1, SeerCard, active = false).ready
    val lineage = lineageOf(ready, p1)
    val active = applied(ready, Flip(SeerCard.id, Location.Legacies(lineage),
      Orientation.FaceUp))
    assertEquals(active.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(SeerCard.id, active = true)))
    val dormant = applied(active, Flip(SeerCard.id,
      Location.Legacies(lineage), Orientation.FaceDown))
    assertEquals(dormant.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(SeerCard.id, active = false)))

  test("a public legacy has no face to flip"):
    val ready = Table.start.publicLegacies(RivalCard).ready
    assert(RawOperationBatch.executeAll(ready, Vector(Flip(RivalCard.id,
      Location.PublicLegacies, Orientation.FaceDown))).isLeft)
