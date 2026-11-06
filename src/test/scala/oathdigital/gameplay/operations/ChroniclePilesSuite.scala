package oathdigital.gameplay.operations

import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted
import oathdigital.testkit.RawOperationBatch

/** The Chronicle's card piles: the gathering pile, the newly Dispossessed,
  * the suited reserves and a lineage's starting advisers. */
class ChroniclePilesSuite extends munit.FunSuite:
  private def applied(ready: ReadyGame, ops: CoreOperation*)(
      using munit.Location): ReadyGame =
    accepted(RawOperationBatch.executeAll(ready, ops.toVector))

  private def topDenizenOfWorldDeck(ready: ReadyGame): DenizenId =
    ready.game.current.commonCards.worldDeck.collectFirst {
      case id: DenizenId => id }.get

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  private def moved(card: CardId, from: Location, to: Location,
      facing: Option[Orientation] = None): Move =
    Move(Piece.Card(card), PositionedLocation(from), PositionedLocation(to),
      resultingOrientation = facing)

  test("a world card moves into and out of the gathering pile"):
    val start = Table.start.ready
    val card = topDenizenOfWorldDeck(start)
    val gathered = applied(start,
      moved(card, Location.Deck(CardDeck.World), Location.ChronicleGathering))
    assertEquals(gathered.game.current.chronicleGathering, Vector(card))
    val out = applied(gathered,
      moved(card, Location.ChronicleGathering, Location.Dispossessed))
    assertEquals(out.game.current.chronicleGathering, Vector.empty)
    assert(out.game.campaign.dispossessed.contains(card))

  test("a world card moves into the newly Dispossessed"):
    val start = Table.start.ready
    val card = topDenizenOfWorldDeck(start)
    val after = applied(start,
      moved(card, Location.Deck(CardDeck.World), Location.NewlyDispossessed))
    assertEquals(after.game.current.newlyDispossessed, Vector(card))

  test("a Dispossessed denizen returns to its suit's reserve and leaves it"):
    val quiet = Table.start.ready
    val card = topDenizenOfWorldDeck(quiet)
    val start = applied(quiet,
      moved(card, Location.Deck(CardDeck.World), Location.Dispossessed))
    val suit = FirstGameSetupFixture.catalog.suitOf(card).get
    val reserved = applied(start,
      moved(card, Location.Dispossessed, Location.SuitedReserve(suit)))
    assertEquals(reserved.game.campaign.suitedReserves.get(suit),
      Some(Vector(card)))
    val drawn = applied(reserved,
      moved(card, Location.SuitedReserve(suit), Location.Hand(p1)))
    assertEquals(drawn.game.campaign.suitedReserves.get(suit),
      Some(Vector.empty))
    assertEquals(drawn.game.current.temporaryHands.get(p1), Some(Vector(card)))

  test("a denizen from a hand becomes a facedown starting adviser, which " +
      "its owner still knows"):
    val card = topDenizenOfWorldDeck(Table.start.ready)
    val ready = Table.start.hand(p2, card).ready
    val lineage = lineageOf(ready, p2)
    val after = applied(ready, moved(card, Location.Hand(p2),
      Location.StartingAdvisers(lineage), Some(Orientation.FaceDown)))
    assertEquals(after.game.campaign.lineages(lineage).startingAdvisers,
      Vector(DenizenState(card, Orientation.FaceDown, Tokens.empty)))
    assert(after.knowledge.advisers.getOrElse(p2, Vector.empty)
      .contains(card))

  /** Whether moving the relic deck's top card to `to` is refused as a
    * destination its card type cannot enter. */
  private def relicRefusedAt(to: Location): Boolean =
    val start = Table.start.ready
    val relic = start.game.current.commonCards.relicDeck.head
    RawOperationBatch.executeAll(start, Vector(moved(relic,
      Location.Deck(CardDeck.Relic), to))).left.toOption
      .exists(_.isInstanceOf[OperationError.InvalidDestination])

  test("only world cards enter the gathering pile and the newly " +
      "Dispossessed; only denizens a reserve or starting advisers"):
    val lineage = lineageOf(Table.start.ready, p1)
    assert(Vector(Location.ChronicleGathering, Location.NewlyDispossessed,
      Location.SuitedReserve(Suit.Beast), Location.StartingAdvisers(lineage))
      .forall(relicRefusedAt))

  test("a Vision is a world card but no denizen, so no reserve takes it"):
    val ready = Table.start.revealedVision(p1, VisionRules.Conquest).ready
    assert(RawOperationBatch.executeAll(ready, Vector(moved(VisionRules.Conquest,
      Location.PlayArea(p1), Location.SuitedReserve(Suit.Beast)))).left.toOption
      .exists(_.isInstanceOf[OperationError.InvalidDestination]))
