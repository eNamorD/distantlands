package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.legacies.OathsOfLoyaltyCard
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Quest to Distant Lands in Setup: "You may choose to not place your
  * pawn. If you do, you may choose one card from the Dispossessed and swap
  * it with a starting adviser or card you drew in Foundation IV." (spec
  * rulings 18 and 19; R30, R31). l2, a Citizen, holds it; two denizens wait
  * in the Dispossessed. */
class QuestSetupSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val rules = Situation.productionRules(catalog)

  /** `legacies` on l2, two Dispossessed denizens, and, when `adviser`, one
    * stored starting adviser for l2. */
  private def chronicle(adviser: Boolean,
      legacies: (LegacyId, Boolean)*): Chronicle =
    val base = LaterGameFixture.chronicle(l1, l2)
    val (kept, deck) = base.worldDeck.splitAt(3)
    val held = SetupLegacyFixture.holding(base.copy(worldDeck = deck,
      dispossessed = kept.take(2)), l2, Role.Citizen, legacies*)
    if !adviser then held
    else held.copy(lineages = held.lineages.map(lineage =>
      if lineage.id != l2 then lineage
      else lineage.copy(startingAdvisers = Vector(DenizenState(kept(2),
        Orientation.FaceDown, Tokens.empty)))))
  private val record = chronicle(adviser = false,
    QuestToDistantLandsCard.id -> true)
  private val dispossessed = record.dispossessed

  private val stayingOff: Answers =
    case park if park.decisionId == SetupProcedure.pawnDecisionId(p2) =>
      ChooseOneAnswer(QuestToDistantLands.stayOff)

  private def parkedAt(decisionId: String, answers: Answers = stayingOff,
      from: Chronicle = record): Situation =
    Situation.start(rules.withAnswers(answers)).parkedAt(decisionId)(
      GameCommand.Begin(from, LaterGameFixture.orders(from, p1)))

  private def decide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog,
      WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  private def refs(situation: Situation): Vector[DecisionOptionRef] =
    decide(situation).query match
      case one: DecisionQuery.ChooseOne => one.options.map(_.ref)
      case other => fail(s"expected a choose-one, got $other")

  /** `answers`, failing the walk if any Quest question is asked. */
  private def askingNoQuest(answers: Answers): Answers =
    answers.orElse {
      case park if park.decisionId.startsWith("setup.legacy.quest") =>
        fail(s"asked ${park.decisionId}")
    }

  private def lines(situation: Situation): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, situation.events).map(_.text)

  test("the holder's pawn question offers the sites and Don't place your " +
      "pawn, last; another player's offers only sites"):
    val own = refs(parkedAt(SetupProcedure.pawnDecisionId(p2),
      PartialFunction.empty))
    assertEquals(own.last, QuestToDistantLands.stayOff)
    assert(own.init.forall(_.isInstanceOf[DecisionOptionRef.Site]), own)
    assert(refs(parkedAt(SetupProcedure.pawnDecisionId(p1),
      PartialFunction.empty)).forall(_.isInstanceOf[DecisionOptionRef.Site]))

  test("staying off: no pawn, the line, and no Choose Adviser or Reveal " +
      "Cards for the holder; the hand stays drawn"):
    val woken = Situation.wake(rules.withAnswers(stayingOff), record,
      LaterGameFixture.orders(record, p1))
    val ready = woken.ready
    assertEquals(ready.game.current.pawnSite(p2), None)
    assert(ready.game.current.temporaryHands.getOrElse(p2, Vector.empty)
      .nonEmpty)
    assertEquals(ready.game.current.player(p2).get.advisers, Vector.empty)
    assert(lines(woken).contains(s"p2 placed no pawn with " +
      QuestToDistantLandsCard.id.value), lines(woken))

  test("staying off, The Standard Bearer and Wealthy still act for the " +
      "holder (ruling 18)"):
    val all = chronicle(adviser = false, QuestToDistantLandsCard.id -> true,
      TheStandardBearerCard.id -> true, WealthyCard.id -> true)
    val at = parkedAt(TheStandardBearer.decisionId(p2), stayingOff, all)
    assertEquals(at.ready.game.current.pawnSite(p2), None)
    val wealthy = at.parkedAt(Wealthy.decisionId(p2))()
    assertEquals(decide(wealthy).owner, p2)

  test("the swap: Decline first, then the Dispossessed in catalog order, " +
      "and a walk resumed at the question asks for the card to give"):
    val at = parkedAt(QuestToDistantLands.swapDecisionId(p2))
    assertEquals(refs(at), QuestToDistantLands.decline +: dispossessed
      .sortBy(_.value).map(DecisionOptionRef.Denizen(_)))
    val taking = stayingOff.orElse {
      case park if park.decisionId == QuestToDistantLands.swapDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(dispossessed.head))
    }
    val give = at.withAnswers(taking)
      .parkedAt(QuestToDistantLands.giveDecisionId(p2))()
    assertEquals(decide(give).owner, p2)

  test("with a Ties hand, the replacement joins the hand and the given " +
      "card goes to the Dispossessed"):
    val hand = parkedAt(QuestToDistantLands.swapDecisionId(p2)).ready.game
      .current.temporaryHands(p2)
    val handed = hand.head
    val answers = stayingOff.orElse {
      case park if park.decisionId == QuestToDistantLands.swapDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(dispossessed.head))
      case park if park.decisionId == QuestToDistantLands.giveDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(handed.asInstanceOf[DenizenId]))
    }
    val ready = Situation.wake(rules.withAnswers(answers), record,
      LaterGameFixture.orders(record, p1)).ready
    assert(ready.game.current.temporaryHands(p2).contains(dispossessed.head))
    assert(!ready.game.current.temporaryHands(p2).contains(handed))
    assert(ready.game.campaign.dispossessed.contains(handed))
    assert(!ready.game.campaign.dispossessed.contains(dispossessed.head))

  test("with a facedown starting adviser, the replacement is a facedown " +
      "adviser and Oaths of Loyalty does not bury it (ruling 19)"):
    val oaths = chronicle(adviser = true, QuestToDistantLandsCard.id -> true,
      OathsOfLoyaltyCard.id -> true)
    val stored = oaths.lineages.find(_.id == l2).get.startingAdvisers.head.id
    val answers = stayingOff.orElse {
      case park if park.decisionId == QuestToDistantLands.swapDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(oaths.dispossessed.head))
      case park if park.decisionId == QuestToDistantLands.giveDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.AdviserSlot(p2, 0))
    }
    val ready = Situation.wake(rules.withAnswers(answers), oaths,
      LaterGameFixture.orders(oaths, p1)).ready
    assertEquals(ready.game.current.player(p2).get.advisers,
      Vector(DenizenState(oaths.dispossessed.head, Orientation.FaceDown,
        Tokens.empty)))
    assert(ready.game.campaign.dispossessed.contains(stored))

  test("with nothing to give, no swap is asked and the Dispossessed stay " +
      "unseen (R37)"):
    val held = chronicle(adviser = false, QuestToDistantLandsCard.id -> true,
      OathsOfLoyaltyCard.id -> true)
    val teeming = held.copy(foundations = held.foundations.updated(
      FoundationNumber.IV, FoundationState(FoundationFace.Altered, Set.empty)))
    val ready = Situation.wake(rules.withAnswers(askingNoQuest(stayingOff)),
      teeming, LaterGameFixture.orders(teeming, p1)).ready
    assertEquals(ready.game.current.pawnSite(p2), None)
    assertEquals(ready.game.current.player(p2).get.advisers, Vector.empty)
    assertEquals(ready.game.current.temporaryHands.getOrElse(p2, Vector.empty),
      Vector.empty)
    val known = ready.knowledge.advisers.getOrElse(p2, Vector.empty)
    assert(teeming.dispossessed.forall(card => !known.contains(card)), known)

  test("placing the pawn: no swap is offered"):
    val ready = Situation.wake(rules.withAnswers(
      askingNoQuest(PartialFunction.empty)), record,
      LaterGameFixture.orders(record, p1)).ready
    assert(ready.game.current.pawnSite(p2).nonEmpty)

  test("the line names no card"):
    val answers = stayingOff.orElse {
      case park if park.decisionId == QuestToDistantLands.swapDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(dispossessed.head))
    }
    val woken = Situation.wake(rules.withAnswers(answers), record,
      LaterGameFixture.orders(record, p1))
    assert(lines(woken).contains(s"p2 swapped a card with the Dispossessed " +
      s"with ${QuestToDistantLandsCard.id.value}"), lines(woken))

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(QuestToDistantLands))
    assert(PowerImplementationStatus.implemented(
      QuestToDistantLandsCard.power.id))
