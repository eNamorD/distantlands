package distantlands.gameplay.setup

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.legacies.OathsOfLoyaltyCard
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Situation

/** Oaths of Loyalty in the next game's Setup: "When you would gain a
  * starting adviser, bury it." (ruling 16; R24). l1, played by p1, holds
  * Oaths of Loyalty active and stores no starting adviser. */
class OathsSetupSuite extends munit.FunSuite:
  private val p1 = PlayerId("p1")
  private val l1 = LineageId("l1")

  private def holding(active: Boolean, face: FoundationFace): Chronicle =
    val record = LaterGameFixture.chronicle(l1, LineageId("l2"))
    record.copy(
      foundations = record.foundations.updated(FoundationNumber.IV,
        FoundationState(face, Set.empty)),
      lineages = record.lineages.map(lineage =>
        if lineage.id == l1 then lineage.copy(legacies =
          Vector(LegacyState(OathsOfLoyaltyCard.id, active)))
        else lineage))

  private def wake(chronicle: Chronicle)(using munit.Location): Situation =
    Situation.wake(Situation.rules(catalog), chronicle,
      LaterGameFixture.orders(chronicle, p1))

  private def woken(chronicle: Chronicle)(using munit.Location): ReadyGame =
    wake(chronicle).ready

  test("under Ties of Fellowship the holder still picks from a hand of " +
      "three, and the kept card is buried at the world deck bottom"):
    val chronicle = holding(active = true, FoundationFace.Normal)
    val hand = SetupDeal.of(chronicle, FirstGameSetupFixture.participants)
      .hand(LaterGameFixture.orders(chronicle, p1).denizenOrder, p1)
    assertEquals(hand.size, 3)
    val situation = wake(chronicle)
    val ready = situation.ready
    assertEquals(ready.game.current.player(p1).get.advisers, Vector.empty)
    assert(ready.game.current.commonCards.worldDeck.takeRight(3)
      .exists(card => hand.contains(card)))
    val keys = SetupProcedure.noteKeys
    val buried = NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      keys, situation.events).filter(said => keys.exists(_.name == said.key))
      .map(_.text).filter(_.startsWith("p1 buried "))
    assertEquals(buried.size, 1)
    assert(hand.exists(card => buried.head ==
      s"p1 buried ${card.value} with ${OathsOfLoyaltyCard.id.value}"), buried)

  test("a dormant Oaths keeps the picked adviser as before (ruling 1)"):
    val ready = woken(holding(active = false, FoundationFace.Normal))
    assertEquals(ready.game.current.player(p1).get.advisers.size, 1)

  /** `holding` under Teeming World, l1 storing `stored` starting advisers
    * taken from the world deck. */
  private def teeming(stored: Int): Chronicle =
    val record = holding(active = true, FoundationFace.Altered)
    val (advisers, deck) = record.worldDeck.splitAt(stored)
    record.copy(worldDeck = deck, lineages = record.lineages.map(lineage =>
      if lineage.id != l1 then lineage
      else lineage.copy(startingAdvisers = advisers.map(id =>
        DenizenState(id, Orientation.FaceDown, Tokens.empty)))))

  Vector(0, 1, 3).foreach { stored =>
    test(s"under Teeming World a holder storing $stored starting advisers " +
        s"draws ${3 - stored} and buries every card drawn below the " +
        "Visions (R24 re-ruled)"):
      val chronicle = teeming(stored)
      val orders = LaterGameFixture.orders(chronicle, p1)
      val deal = SetupDeal.of(chronicle, orders.participants)
      assertEquals(deal.draws.find(_._1 == p1).map(_._2), Some(3 - stored))
      assertEquals(deal.kept(orders.denizenOrder, p1), Vector.empty)
      val buried = deal.buried(orders.denizenOrder, p1)
      assertEquals(buried.size, 3 - stored)
      val situation = wake(chronicle)
      val ready = situation.ready
      assertEquals(ready.game.current.player(p1).get.advisers.map(_.id),
        chronicle.lineages.find(_.id == l1).get.startingAdvisers.map(_.id))
      val deck = ready.game.current.commonCards.worldDeck
      assertEquals(deck.takeRight(buried.size), buried)
      assert(deck.dropRight(buried.size).forall(_.isInstanceOf[VisionId]),
        deck)
      val keys = SetupProcedure.noteKeys
      val lines = NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
        keys, situation.events).map(_.text).filter(_.startsWith("p1 buried "))
      assertEquals(lines, buried.map(card =>
        s"p1 buried ${card.value} with ${OathsOfLoyaltyCard.id.value}"))
  }

  test("under Teeming World the deal accounts for every denizen once"):
    val chronicle = teeming(1)
    val orders = LaterGameFixture.orders(chronicle, p1)
    val ready = woken(chronicle)
    val current = ready.game.current
    val placed = current.players.flatMap(_.advisers.map(_.id: CardId)) ++
      current.commonCards.worldDeck ++
      Region.all.flatMap(current.commonCards.discard) ++
      current.temporaryHands.values.flatten
    orders.denizenOrder.foreach(card =>
      assertEquals(placed.count(_ == card), 1, card))

  test("a dormant Oaths under Teeming World keeps every card drawn"):
    val chronicle = holding(active = false, FoundationFace.Altered)
    val orders = LaterGameFixture.orders(chronicle, p1)
    val dormant = SetupDeal.of(chronicle, orders.participants)
    assertEquals(dormant.draws.find(_._1 == p1).map(_._2), Some(3))
    val kept = dormant.kept(orders.denizenOrder, p1)
    assertEquals(kept.size, 3)
    assertEquals(dormant.buried(orders.denizenOrder, p1), Vector.empty)
    assertEquals(woken(chronicle).game.current.player(p1).get.advisers
      .map(_.id), kept)
