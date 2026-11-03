package oathdigital.gameplay.operations

import oathdigital.gameplay.powers.action.{FaeMerchantCard, SecondChanceCard}
import oathdigital.gameplay.powers.whenplayed.ThreateningRoarCard
import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Unwrap.accepted

class ShuffleOperationSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private val cradle = ShufflePile.Search(SearchSource.RegionalDiscard(Region.Cradle))

  /** The Cradle discard pile holds three cards from the world deck. */
  private val discarded = Table.start.discarded(Region.Cradle,
    ThreateningRoarCard, FaeMerchantCard, SecondChanceCard).ready
  private def zones(ready: ReadyGame): CardZones = ready.game.current.commonCards
  private val pile = zones(discarded).discard(Region.Cradle)
  private val deck = zones(discarded).worldDeck

  test("a shuffle writes the world deck's new order"):
    val done = executor.execute(discarded,
      Shuffle(ShufflePile.Search(SearchSource.WorldDeck), Some(deck.reverse)))
    assertEquals(done.map(zones(_).worldDeck), Right(deck.reverse))

  test("a shuffle writes a discard pile's new order and leaves the rest"):
    val done =
      accepted(executor.execute(discarded, Shuffle(cradle, Some(pile.reverse))))
    assertEquals(zones(done).discard(Region.Cradle), pile.reverse)
    assertEquals(zones(done).worldDeck, deck)

  test("an order that drops, adds or swaps a card is refused"):
    Vector(pile.tail, pile :+ deck.last, pile.tail :+ deck.last).foreach(
      order => assertEquals(executor.execute(discarded,
        Shuffle(cradle, Some(order))).left.toOption,
        Some(OperationError.InvalidDescription(
          "a shuffle of the Cradle discard pile must hold exactly its cards")),
        order.toString))

  test("a shuffle the walker never ordered is refused"):
    assertEquals(executor.execute(discarded, Shuffle(ShufflePile.Search(SearchSource.WorldDeck)))
      .left.toOption, Some(OperationError.InvalidDescription(
        "a shuffle of the world deck has no order")))

  test("a pile is named for a log line or a label"):
    assertEquals(SearchSource.name(SearchSource.WorldDeck), "world deck")
    assertEquals(SearchSource.name(cradle.source), "Cradle discard pile")

  test("a shuffle forgets its pile's cards, since no one knows which is where"):
    val peeker = discarded.game.current.players.head.player
    val peeked = pile.foldLeft(discarded)((ready, card) =>
      accepted(executor.execute(ready,
        Peek(peeker, card, Location.RegionalDiscard(Region.Cradle)))))
    assert(pile.forall(peeked.knowledge.advisers(peeker).contains))
    val shuffled =
      accepted(executor.execute(peeked, Shuffle(cradle, Some(pile.reverse))))
    assert(shuffled.knowledge.advisers.values.forall(known =>
      pile.forall(!known.contains(_))), shuffled.knowledge.advisers.toString)

  test("a shuffle writes the edifice deck's new order"):
    val edifices = zones(discarded).edificeDeck
    assert(edifices.size > 1, edifices)
    val done = accepted(executor.execute(discarded,
      Shuffle(ShufflePile.EdificeDeck, Some(edifices.reverse))))
    assertEquals(zones(done).edificeDeck, edifices.reverse)

  test("an edifice deck order that drops an edifice is refused"):
    val edifices = zones(discarded).edificeDeck
    assertEquals(executor.execute(discarded,
      Shuffle(ShufflePile.EdificeDeck, Some(edifices.tail))).left.toOption,
      Some(OperationError.InvalidDescription(
        "a shuffle of the edifice deck must hold exactly its cards")))

  test("a shuffle writes the set-aside relics' new order"):
    val relics = zones(discarded).relicDeck.take(2)
    val aside = discarded.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        relicDeck = current.commonCards.relicDeck.drop(2)),
      setAsideRelics = relics))
    val done = accepted(executor.execute(aside,
      Shuffle(ShufflePile.SetAsideRelics, Some(relics.reverse))))
    assertEquals(done.game.current.setAsideRelics, relics.reverse)

  test("a shuffle writes the gathering pile's new order"):
    val three = zones(discarded).worldDeck.take(3)
    val gathered = discarded.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        worldDeck = current.commonCards.worldDeck.drop(3)),
      chronicleGathering = three))
    val done = accepted(executor.execute(gathered,
      Shuffle(ShufflePile.ChronicleGathering, Some(three.reverse))))
    assertEquals(done.game.current.chronicleGathering, three.reverse)

  test("a shuffle writes a suited reserve's new order"):
    val denizens = zones(discarded).worldDeck.collect { case id: DenizenId => id }
    val suit = FirstGameSetupFixture.catalog.suitOf(denizens.head).get
    val three = denizens.filter(FirstGameSetupFixture.catalog.suitOf(_).get == suit).take(3)
    assertEquals(three.size, 3)
    val reserved = discarded.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        worldDeck = current.commonCards.worldDeck.filterNot(three.contains))))
      .updateCampaign(_.copy(suitedReserves = Map(suit -> three)))
    val done = accepted(executor.execute(reserved,
      Shuffle(ShufflePile.SuitedReserve(suit), Some(three.reverse))))
    assertEquals(done.game.campaign.suitedReserves(suit), three.reverse)

  test("a shuffle writes the Reliquary's new order"):
    val three = zones(discarded).relicDeck.take(3)
    val stocked = discarded.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        relicDeck = current.commonCards.relicDeck.drop(3))))
      .updateCampaign(_.copy(reliquary = three))
    val done = accepted(executor.execute(stocked,
      Shuffle(ShufflePile.Reliquary, Some(three.reverse))))
    assertEquals(done.game.campaign.reliquary, three.reverse)
