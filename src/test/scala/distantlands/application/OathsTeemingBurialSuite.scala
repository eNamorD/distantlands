package distantlands.application

import distantlands.gameplay.chronicle.legacies.OathsOfLoyaltyCard
import distantlands.gameplay.setup.{LaterGameFixture, SetupDeal,
  SetupLegacyFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{SecretSociety, SecretSocietyCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Dice, Situation}

/** Under Teeming World an Oaths of Loyalty holder's buried draws are named
  * to the holder alone (Legacy Chronicle powers R24, re-ruled): they lie in
  * the world deck, which nobody else knows. l1, played by p1, holds Oaths. */
class OathsTeemingBurialSuite extends munit.FunSuite:
  private val presentation = new GamePresentationProjector(catalog)
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))

  /** Teeming World, l1 holding Oaths and storing no starting adviser, and
    * l2, a Citizen, holding `legacies`. */
  private def teeming(legacies: LegacyId*): Chronicle =
    val record = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
      legacies.map(_ -> true)*)
    record.copy(
      foundations = record.foundations.updated(FoundationNumber.IV,
        FoundationState(FoundationFace.Altered, Set.empty)),
      lineages = record.lineages.map(lineage =>
        if lineage.id == l1 then lineage.copy(legacies =
          Vector(LegacyState(OathsOfLoyaltyCard.id, active = true)))
        else lineage))

  private def buried(chronicle: Chronicle): Vector[CardId] =
    val orders = LaterGameFixture.orders(chronicle, p1)
    SetupDeal.of(chronicle, orders.participants).buried(orders.denizenOrder, p1)

  /** Secret Society's holder takes the first Vision it is offered. */
  private val takingAVision: Answers =
    case park if park.decisionId == SecretSociety.decisionId(p2) =>
      park.decide.query match
        case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
        case other => fail(s"expected a choose-one, got $other")

  /** Setup walked through the production rules, its shuffles reversed. */
  private def woken(chronicle: Chronicle): ReadyGame =
    Situation.wake(Situation.productionRules(catalog,
      campaignDice = Dice.reversingShuffle).withAnswers(takingAVision),
      chronicle, LaterGameFixture.orders(chronicle, p1)).ready

  private def onlyTheHolderIdentifies(ready: ReadyGame,
      cards: Vector[CardId])(using munit.Location): Unit =
    cards.foreach { card =>
      assert(presentation.identifiesAt(ready, Some(p1), card), card)
      Vector(Some(p2), None).foreach(viewer =>
        assert(!presentation.identifiesAt(ready, viewer, card),
          s"$viewer $card"))
    }

  test("the holder identifies the cards it buried; another seat and the " +
      "public do not"):
    val chronicle = teeming()
    assertEquals(buried(chronicle).size, 3)
    val ready = Situation.wake(Situation.rules(catalog), chronicle,
      LaterGameFixture.orders(chronicle, p1)).ready
    onlyTheHolderIdentifies(ready, buried(chronicle))

  test("Secret Society's rebuild shuffles only the Visions and keeps the " +
      "burials at the bottom in their order (R49)"):
    val chronicle = teeming(SecretSocietyCard.id)
    val ready = woken(chronicle)
    assert(ready.game.current.player(p2).get.advisers.nonEmpty)
    val deck = ready.game.current.commonCards.worldDeck
    assertEquals(deck.takeRight(3), buried(chronicle))
    assert(deck.dropRight(3).forall(_.isInstanceOf[VisionId]), deck)
