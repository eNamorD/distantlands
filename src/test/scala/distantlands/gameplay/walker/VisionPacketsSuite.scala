package distantlands.gameplay.walker

import distantlands.gameplay.WalkerDiceFixture
import distantlands.model._

/** Secret Society's reshuffle (spec ruling 14; R19): the denizens keep
  * their order, two Visions go among the first 12 cards and two among the
  * next 17, or, with fewer than 25 denizens, anywhere. */
class VisionPacketsSuite extends munit.FunSuite:
  private val denizens: Vector[CardId] =
    (1 to 30).toVector.map(n => DenizenId(s"d$n"))
  private val visions: Vector[CardId] =
    (1 to 4).toVector.map(n => VisionId(s"vision:$n"))
  private def isVision(card: CardId) = card.isInstanceOf[VisionId]

  private def laid(deck: Vector[CardId]): Vector[CardId] =
    WalkerShuffles.packets(deck, WalkerDiceFixture.reversingShuffle)
      .fold(problem => fail(problem.toString), identity)

  test("two Visions in the top 12 cards, two in the next 17, the denizens " +
      "in their order"):
    val deck = laid(visions ++ denizens)
    assertEquals(deck.take(12).count(isVision), 2)
    assertEquals(deck.slice(12, 29).count(isVision), 2)
    assertEquals(deck.filterNot(isVision), denizens)
    assertEquals(deck.filter(isVision).toSet, visions.toSet)

  test("with fewer than 25 denizens every card stays, the denizens in order"):
    val short = denizens.take(10)
    val deck = laid(short ++ visions)
    assertEquals(deck.size, 14)
    assertEquals(deck.filterNot(isVision), short)

  test("a deck of Visions only is just reordered (Teeming World)"):
    assertEquals(laid(visions).toSet, visions.toSet)
