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

  test("under Teeming World the holder draws no starting adviser"):
    val chronicle = holding(active = true, FoundationFace.Altered)
    val deal = SetupDeal.of(chronicle, FirstGameSetupFixture.participants)
    assertEquals(deal.draws.find(_._1 == p1).map(_._2), Some(0))
    assertEquals(woken(chronicle).game.current.player(p1).get.advisers,
      Vector.empty)
    val dormant = SetupDeal.of(holding(active = false,
      FoundationFace.Altered), FirstGameSetupFixture.participants)
    assertEquals(dormant.draws.find(_._1 == p1).map(_._2), Some(3))
