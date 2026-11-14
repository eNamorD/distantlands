package distantlands.gameplay.phases.wake

import distantlands.gameplay.actions.CardPlay
import distantlands.gameplay.chronicle.legacies.OathsOfLoyaltyCard
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{QuestToDistantLands,
  QuestToDistantLandsCard, SecretSociety, SecretSocietyCard}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.walker.WalkerParked
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseOneAnswer, PartitionAnswer}
import distantlands.testkit.{Answers, MenuPlay, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** The first Wake of a player who placed no pawn (spec ruling 20; R32):
  * the placement opens it, then the steps Setup deferred. l2, a Citizen,
  * holds Quest to Distant Lands and stays off; p1 plays a turn first. */
class QuestFirstWakeSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val rules = Situation.productionRules(catalog)
  private def holding(legacies: (LegacyId, Boolean)*): Chronicle =
    SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2), l2,
      Role.Citizen, legacies*)
  private val record = holding(QuestToDistantLandsCard.id -> true)
  private val stayingOff: Answers =
    case park if park.decisionId == SetupProcedure.pawnDecisionId(p2) =>
      ChooseOneAnswer(QuestToDistantLands.stayOff)

  /** p1's turn played out, parked at `decisionId` in p2's first Wake. */
  private def parkedAt(decisionId: String, from: Chronicle = record,
      answers: Answers = stayingOff): Situation =
    Situation.act(rules.withAnswers(answers), p1, from,
      LaterGameFixture.orders(from, p1)).parkedAt(decisionId)(
      MenuPlay.start(p1, PhaseTransitionRef.BeginRest))

  private def decide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog,
      WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  private def lines(situation: Situation): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.QuestFirstWake),
      QuestFirstWakeProcedure.noteKeys, situation.events).map(_.text)

  test("a pawnless player's Wake opens with the placement, any site in " +
      "play, no Decline"):
    val at = parkedAt(QuestFirstWakeProcedure.placeDecisionId(p2))
    val asked = decide(at)
    assertEquals(asked.owner, p2)
    assertEquals(at.ready.game.current.turn.activePlayer, p2)
    assertEquals(at.ready.game.current.turn.phase, Phase.Wake)
    asked.query match
      case one: DecisionQuery.ChooseOne => assertEquals(one.options.map(_.ref),
        at.ready.game.current.map.inPlay.map(DecisionOptionRef.Site(_)))
      case other => fail(s"expected a choose-one, got $other")

  test("the placement confirms as Place your pawn at, not Setup's Start at " +
      "(R35)"):
    decide(parkedAt(QuestFirstWakeProcedure.placeDecisionId(p2))).query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.confirmVerb, Some("Place your pawn at"))
      case other => fail(s"expected a choose-one, got $other")

  test("then Choose Adviser, discarding by the new pawn's region, then the " +
      "line; a walk resumed at each question places once"):
    val placed = parkedAt(QuestFirstWakeProcedure.adviserDecisionId(p2))
    val site = placed.ready.game.current.pawnSite(p2).get
    val region = placed.ready.game.current.map.regionOf(site).get
    val hand = placed.ready.game.current.temporaryHands(p2)
      .collect { case id: DenizenId => id }
    assertEquals(hand.size, 3)
    val after = placed.withAnswers(stayingOff.orElse {
      case park if park.decisionId ==
          QuestFirstWakeProcedure.adviserDecisionId(p2) =>
        PartitionAnswer(DecisionPlacement(DecisionOptionRef.Denizen(hand.head),
          "keep") +: hand.tail.map(id => DecisionPlacement(
          DecisionOptionRef.Denizen(id), "discard")))
    }).after()
    val ready = after.ready
    assertEquals(ready.game.current.pawnSite(p2), Some(site))
    assert(ready.game.current.player(p2).get.advisers.exists(_.id == hand.head))
    val next = ready.game.current.commonCards.discard(
      CardPlay.nextRegion(region))
    assert(hand.tail.forall(next.contains), next)
    assert(lines(after).contains(s"p2 placed their pawn at ${site.value}"),
      lines(after))

  test("Reveal Cards follows Choose Adviser"):
    val at = parkedAt(QuestFirstWakeProcedure.revealDecisionId(p2))
    assertEquals(decide(at).owner, p2)

  test("Secret Society's excess discard waits for the first Wake"):
    val both = holding(QuestToDistantLandsCard.id -> true,
      SecretSocietyCard.id -> true)
    val teeming = both.copy(foundations = both.foundations.updated(
      FoundationNumber.IV, FoundationState(FoundationFace.Altered, Set.empty)))
    val taking = stayingOff.orElse {
      case park if park.decisionId == SecretSociety.decisionId(p2) =>
        park.decide.query match
          case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options(1).ref)
          case other => fail(s"expected a choose-one, got $other")
    }
    val at = parkedAt(QuestFirstWakeProcedure.discardDecisionId(p2), teeming,
      taking)
    assertEquals(decide(at).owner, p2)
    assert(at.ready.game.current.pawnSite(p2).nonEmpty)

  test("a Ties hand card swapped in is buried at the Wake's Choose Adviser " +
      "under Oaths of Loyalty"):
    val oaths = holding(QuestToDistantLandsCard.id -> true,
      OathsOfLoyaltyCard.id -> true)
    val base = oaths.worldDeck
    val withDispossessed = oaths.copy(worldDeck = base.drop(1),
      dispossessed = base.take(1))
    val taken = base.head
    val answers = stayingOff.orElse {
      case park if park.decisionId == QuestToDistantLands.swapDecisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Denizen(taken))
      case park if park.decisionId == QuestToDistantLands.giveDecisionId(p2) =>
        park.decide.query match
          case one: DecisionQuery.ChooseOne => ChooseOneAnswer(one.options.head.ref)
          case other => fail(s"expected a choose-one, got $other")
      case park if park.decisionId ==
          QuestFirstWakeProcedure.adviserDecisionId(p2) =>
        park.decide.query match
          case partition: DecisionQuery.Partition =>
            PartitionAnswer(partition.options.map(option => DecisionPlacement(
              option.ref, if option.ref == DecisionOptionRef.Denizen(taken)
                then "keep" else "discard")))
          case other => fail(s"expected a partition, got $other")
    }
    // With the kept card buried there is nothing to reveal: the walk
    // settles past Reveal Cards.
    val ready = parkedAt(QuestFirstWakeProcedure.revealDecisionId(p2),
      withDispossessed, answers).ready
    assertEquals(ready.game.current.commonCards.worldDeck.lastOption,
      Some(taken))
    assert(!ready.game.current.player(p2).get.advisers.exists(_.id == taken))

  test("a player with a pawn is never asked"):
    val woken = Situation.rest(rules, p1, record,
      LaterGameFixture.orders(record, p1))
    assert(woken.ready.game.current.pawnSite(p2).nonEmpty)
    assert(!woken.events.exists {
      case WalkerParked(procedure, _, _, _, _) =>
        procedure == TriggeredProcedureRef.QuestFirstWake
      case _ => false
    })
