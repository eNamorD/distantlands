package distantlands.gameplay.operations

import distantlands.catalog.holding.SeerCard
import distantlands.gameplay.chronicle.legacies.RivalCard
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

  test("a held legacy moved to the public row leaves the lineage"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    val lineage = lineageOf(ready, p1)
    val after = applied(ready, moved(SeerCard.id, Location.Legacies(lineage),
      Location.PublicLegacies))
    assertEquals(after.game.campaign.lineages(lineage).legacies, Vector.empty)
    assertEquals(after.game.current.commonCards.publicLegacies,
      Vector(SeerCard.id))

  test("a public legacy returned to the deck leaves play"):
    val ready = Table.start.publicLegacies(RivalCard).ready
    val after = applied(ready, moved(RivalCard.id, Location.PublicLegacies,
      Location.Deck(CardDeck.Legacy)))
    assertEquals(after.game.current.commonCards.publicLegacies, Vector.empty)
    assertEquals(after.game.cardIndex.toOption.get.get(RivalCard.id), None)

  test("a legacy moved between lineages takes the move's face, dormant " +
      "when none is given"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    val (from, to) = (lineageOf(ready, p1), lineageOf(ready, p2))
    val dormant = applied(ready, moved(SeerCard.id, Location.Legacies(from),
      Location.Legacies(to)))
    assertEquals(dormant.game.campaign.lineages(from).legacies, Vector.empty)
    assertEquals(dormant.game.campaign.lineages(to).legacies,
      Vector(LegacyState(SeerCard.id, active = false)))
    val active = applied(ready, moved(SeerCard.id, Location.Legacies(from),
      Location.Legacies(to), Some(Orientation.FaceUp)))
    assertEquals(active.game.campaign.lineages(to).legacies,
      Vector(LegacyState(SeerCard.id, active = true)))

  test("a legacy cannot enter a lineage the campaign does not hold"):
    val ready = Table.start.publicLegacies(RivalCard).ready
    assert(RawOperationBatch.executeAll(ready, Vector(moved(RivalCard.id,
      Location.PublicLegacies, Location.Legacies(LineageId("nobody")))))
      .left.toOption.exists(_.isInstanceOf[OperationError.InvalidDestination]))

  test("a legacy goes to the public row or the deck faceup or with no " +
      "face, never facedown"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    val from = Location.Legacies(lineageOf(ready, p1))
    Vector(Location.PublicLegacies, Location.Deck(CardDeck.Legacy)).foreach {
      to =>
        assertEquals(RawOperationBatch.executeAll(ready, Vector(moved(
          SeerCard.id, from, to, Some(Orientation.FaceDown)))),
          Left(OperationError.UnsupportedOrientation(SeerCard.id, to)), s"$to")
        assert(RawOperationBatch.executeAll(ready, Vector(moved(SeerCard.id,
          from, to, Some(Orientation.FaceUp)))).isRight, s"$to")
    }

  test("the batch's card inventory check lets a legacy leave play into the " +
      "legacy deck"):
    val ready = Table.start.legacy(p1, SeerCard).ready
    val from = Location.Legacies(lineageOf(ready, p1))
    val run = OperationPipeline.run(ready, Vector(moved(SeerCard.id, from,
      Location.Deck(CardDeck.Legacy))), OperationPolicy.Permissive,
      Vector.empty)(Right(_))
    assertEquals(run.map(_.state.game.cardIndex.toOption.get.get(SeerCard.id)),
      Right(None))
