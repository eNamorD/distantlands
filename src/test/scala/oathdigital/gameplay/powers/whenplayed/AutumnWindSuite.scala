package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import oathdigital.gameplay.powers.{NoteText, SearchFixture,
  WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerDice
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Autumn Wind (card 213, catalog batch 10, slice 10), WHEN PLAYED: every
  * discard pile is shuffled together and dealt back out as evenly as
  * possible. When the cards do not divide by 3, p1 names the piles that
  * take the extra cards, before the shuffle. */
class AutumnWindSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[AutumnWind.type]
  private val played = hook(AutumnWind.cardId)
  private val keepingOrder: WalkerDice =
    WalkerDiceFixture.shufflingBy(count => (0 until count).toVector)
  /** Eight Discord denizens with no power that applies in a pile. */
  private val cards: Vector[WorldCardId] = SearchFixture.discordCards

  /** p1 holds Autumn Wind faceup, and the Cradle, Provinces and Hinterland
    * discard piles hold `cradle`, `provinces` and `hinterland`, bottom
    * first. */
  private def p1HoldsAutumnWindOverPiles(cradle: Vector[WorldCardId],
      provinces: Vector[WorldCardId], hinterland: Vector[WorldCardId])
      : ReadyGame =
    p1AtHomeSite.adviser(p1, AutumnWindCard)
      .discarded(Region.Cradle, cradle*)
      .discarded(Region.Provinces, provinces*)
      .discarded(Region.Hinterland, hinterland*).ready

  /** The three discard piles, bottom first, in region order. */
  private def piles(ready: ReadyGame): Vector[Vector[WorldCardId]] =
    Region.all.map(Look(ready).discardPile)

  private def pilesAnswer(regions: Region*): DecisionAnswer =
    DecisionAnswer.ChooseManyAnswer(
      regions.toVector.map(DecisionOptionRef.DiscardPile.apply))

  private def dealtLine(cradle: Int, provinces: Int, hinterland: Int)
      : Vector[NoteText.Said] =
    Vector(NoteText.Said("dealt", "Shuffled the discard piles together " +
      s"and dealt $cradle to the Cradle, $provinces to the Provinces and " +
      s"$hinterland to the Hinterland.", covers = false))

  /** Fails unless `outcome` is a refusal whose text includes `reason`. */
  private def assertRefused(outcome: Either[OathViolation, ?],
      reason: String)(using munit.Location): Unit =
    assert(clue(outcome).swap.exists(_.toString.contains(reason)))

  /** Seven cards: four in the Cradle, two in the Provinces, one in the
    * Hinterland. */
  private val sevenCards: ReadyGame = p1HoldsAutumnWindOverPiles(
    cards.take(4), cards.slice(4, 6), cards.slice(6, 7))

  test("an even total is dealt round-robin from the Cradle with no " +
      "question, a Vision among the cards"):
    val faith = VisionRules.Faith
    val ready = p1HoldsAutumnWindOverPiles(cards.take(3), cards.slice(3, 5),
      Vector(faith))
    val walked = walkShuffling(ready, power, played, keepingOrder)
    assertEquals(walked.asked, Vector.empty)
    assertEquals(piles(walked.end), Vector(
      Vector(cards(0), cards(3)), Vector(cards(1), cards(4)),
      Vector(cards(2), faith)))
    assertEquals(walked.end.game.current.cardGathering, Vector.empty)
    assertEquals(said(power, walked.events), dealtLine(2, 2, 2))

  test("with one card over, p1 names the one pile that takes it, before " +
      "anything is gathered"):
    val first = Outcomes.parked(fire(sevenCards, power, played,
      keepingOrder))
    assertEquals(ops(first.events), Vector.empty)
    val asked = question(sevenCards, power, played, first.tree)
    assertEquals(asked.decisionId, AutumnWind.decisionId)
    assertEquals(asked.query, DecisionQuery.ChooseMany(1, 1,
      Region.all.map(region =>
        DecisionOption.DiscardPile(DecisionOptionRef.DiscardPile(region))),
      heading = Some("Autumn Wind: choose the pile that takes the extra " +
        "card"),
      confirm = Some(CountedConfirm("Deal an extra card to", "pile",
        "piles"))))

  test("the named pile takes the extra card"):
    val walked = walkShuffling(sevenCards, power, played, keepingOrder,
      pilesAnswer(Region.Hinterland))
    assertEquals(piles(walked.end), Vector(
      Vector(cards(0), cards(3)), Vector(cards(1), cards(4)),
      Vector(cards(2), cards(5), cards(6))))
    assertEquals(said(power, walked.events), dealtLine(2, 2, 3))

  test("with two cards over, p1 names two piles"):
    val ready = p1HoldsAutumnWindOverPiles(cards.take(4), cards.slice(4, 8),
      Vector.empty)
    val walked = walkShuffling(ready, power, played, keepingOrder,
      pilesAnswer(Region.Cradle, Region.Hinterland))
    assertEquals(walked.asked.map(_.query).collect {
      case many: DecisionQuery.ChooseMany => (many.min, many.max, many.heading)
    }, Vector((2, 2,
      Some("Autumn Wind: choose the 2 piles that take the extra cards"))))
    assertEquals(piles(walked.end), Vector(
      Vector(cards(0), cards(3), cards(6)), Vector(cards(1), cards(4)),
      Vector(cards(2), cards(5), cards(7))))
    assertEquals(said(power, walked.events), dealtLine(3, 2, 3))

  test("an answer naming the wrong number of piles is refused"):
    val first = Outcomes.parked(fire(sevenCards, power, played,
      keepingOrder))
    assertRefused(resumeBy(sevenCards, power, played, first.tree,
      AutumnWind.decisionId, pilesAnswer(Region.Cradle, Region.Provinces),
      p1, keepingOrder), "selects 2 options instead of 1")

  test("an answer naming one pile twice is refused"):
    val ready = p1HoldsAutumnWindOverPiles(cards.take(4), cards.slice(4, 8),
      Vector.empty)
    val first = Outcomes.parked(fire(ready, power, played, keepingOrder))
    assertRefused(resumeBy(ready, power, played, first.tree,
      AutumnWind.decisionId, pilesAnswer(Region.Cradle, Region.Cradle), p1,
      keepingOrder), "selects an option more than once")

  test("a card already in the gathering pile stops the deal rather than " +
      "being stranded"):
    val strayGathered = sevenCards.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(
        worldDeck = current.commonCards.worldDeck.filterNot(_ == cards(7))),
      cardGathering = Vector(cards(7))))
    val first = Outcomes.parked(fire(strayGathered, power, played,
      keepingOrder))
    assertRefused(resumeBy(strayGathered, power, played, first.tree,
      AutumnWind.decisionId, pilesAnswer(Region.Cradle), p1, keepingOrder),
      "Autumn Wind deals only the cards it gathered")

  test("fewer cards than piles go only to the named piles"):
    val ready = p1HoldsAutumnWindOverPiles(cards.take(2), Vector.empty,
      Vector.empty)
    val walked = walkShuffling(ready, power, played, keepingOrder,
      pilesAnswer(Region.Provinces, Region.Hinterland))
    assertEquals(piles(walked.end), Vector(
      Vector.empty, Vector(cards(0)), Vector(cards(1))))
    assertEquals(said(power, walked.events), dealtLine(0, 1, 1))

  test("the shuffle sets the order of the deal"):
    val ready = p1HoldsAutumnWindOverPiles(cards.take(3), cards.slice(3, 5),
      cards.slice(5, 6))
    val walked = walkShuffling(ready, power, played,
      WalkerDiceFixture.reversingShuffle)
    assertEquals(piles(walked.end), Vector(
      Vector(cards(5), cards(2)), Vector(cards(4), cards(1)),
      Vector(cards(3), cards(0))))

  test("with every pile empty nothing is asked, shuffled or said"):
    val ready = p1HoldsAutumnWindOverPiles(Vector.empty, Vector.empty,
      Vector.empty)
    val done = Outcomes.finished(fire(ready, power, played))
    assertEquals(ops(done.events), Vector.empty)
    assertEquals(said(power, done.events), Vector.empty)

  test("nobody knows a dealt card, not even one known in its pile"):
    val known = cards(0)
    val ready = p1HoldsAutumnWindOverPiles(cards.take(3), Vector.empty,
      Vector.empty)
    val knowing = ready.copy(knowledge = ready.knowledge.copy(
      advisers = Map(p2 -> Vector(known))))
    val walked = walkShuffling(knowing, power, played, keepingOrder)
    assert(!Look(walked.end).knowsAdviser(p2, known))

  test("the journal rebuilds the end, and every step crosses the wire"):
    val walked = walkShuffling(sevenCards, power, played, keepingOrder,
      pilesAnswer(Region.Cradle))
    assertJournalRebuilds(sevenCards, walked.events, walked.end)
    assertWireRoundTrips(walked.events)

  test("searched, the Search's own discards are among the cards dealt"):
    import SearchFixture.{after, keep, place, staged, start}
    // The deal's shuffle needs a source, which `SearchFixture.rules` lacks.
    val shufflingRules = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = keepingOrder)
    val drawnBeside = cards.take(2)
    val ready = staged(AutumnWind.cardId +: drawnBeside, supply = 5)
    val placed = accepted(for
      started <- start(ready)
      chosen <- keep(started, AutumnWind.cardId)
      placedFaceup <- place(chosen, AutumnWind.cardId, PlayedAs.Faceup)
    yield placedFaceup)
    // The two unkept cards went to the Provinces before the play, so two
    // cards are dealt: the question asks for two piles.
    val done = accepted(shufflingRules.resolveWalker(placed.state, p1,
      AutumnWind.decisionId, pilesAnswer(Region.Cradle, Region.Hinterland)))
    val end = piles(after(done))
    assertEquals(end.map(_.size), Vector(1, 0, 1))
    assertEquals(end.flatten.toSet, drawnBeside.toSet)
