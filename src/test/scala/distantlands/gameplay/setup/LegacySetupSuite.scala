package distantlands.gameplay.setup

import distantlands.catalog.holding.SeerCard
import distantlands.gameplay.walker.{LogNoted, WalkerPowers}
import distantlands.model._
import distantlands.testkit.{Answers, Outcomes, Situation}
import distantlands.testkit.Unwrap.{accepted, as}

/** Foundation III at game start and in Setup (Legacies design, slice 2). */
class LegacySetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val chronicle = FirstGameSetupFixture.chronicle
  private val orders = FirstGameSetupFixture.orders
  private val seats = orders.participants

  private def publicAmbitions(base: Chronicle): Chronicle = base.copy(
    foundations = base.foundations.updated(FoundationNumber.III,
      FoundationState(FoundationFace.Altered, Set.empty)))

  test("Quiet Ambitions deals each seated lineage 2 dormant legacies, " +
      "after those it holds"):
    val seated = seats.head.lineageId
    val kept = LegacyState(SeerCard.id, active = true)
    val keeping = chronicle.copy(lineages = Vector(LineageState(seated,
      None, Role.Exile, Vector(kept), Vector.empty)))
    val order = LegacyDeal.deck(catalog, keeping)
    val ready = accepted(GameStartRules.evolve(catalog, keeping,
      orders.copy(legacyOrder = order)))
    val deal = LegacyDeal.of(keeping, seats)
    seats.foreach(seat => assertEquals(
      ready.game.campaign.lineages(seat.lineageId).legacies,
      (if seat.lineageId == seated then Vector(kept) else Vector.empty) ++
        deal.drawn(order, seat.playerId).map(LegacyState(_, active = false)),
      seat.playerId.value))
    assertEquals(ready.game.current.commonCards.publicLegacies, Vector.empty)

  test("Public Ambitions deals 4 legacies faceup to the row and nobody " +
      "draws"):
    val ready = accepted(GameStartRules.evolve(catalog,
      publicAmbitions(chronicle), orders))
    assertEquals(ready.game.current.commonCards.publicLegacies,
      orders.legacyOrder.take(4))
    assert(seats.forall(seat =>
      ready.game.campaign.lineages(seat.lineageId).legacies.isEmpty))

  private val deal = LegacyDeal.of(chronicle, seats)
  private def drawn(player: PlayerId): Vector[LegacyId] =
    deal.drawn(orders.legacyOrder, player)

  private def notes(events: Vector[OathEvent]): Vector[LogNote] =
    events.collect { case LogNoted(_, note, _) => note }

  /** `ready`'s Setup walked to its end, every park answered by default. */
  private def walked(ready: ReadyGame): SetupWalkDriver.SetupWalked =
    SetupWalkDriver.driveWithEvents(ready, accepted(SetupProcedure.build(
      catalog, ready, ready.game.current.turn.activePlayer, Vector.empty)),
      TriggeredProcedureRef.Setup, WalkerPowers.empty)

  private def legacyQuestions(asked: Vector[Decide]): Vector[Decide] =
    asked.filter(decide =>
      decide.decisionId == SetupProcedure.legacyDecisionId(decide.owner))

  test("in a first game each player keeps one dormant legacy and returns " +
      "the other to the deck"):
    // The default answer keeps the first option.
    val ready = Outcomes.ready(FirstGameSetupFixture.setupThroughWake().state)
    seats.foreach { seat =>
      val Vector(kept, returned) = drawn(seat.playerId)
      assertEquals(ready.game.campaign.lineages(seat.lineageId).legacies,
        Vector(LegacyState(kept, active = false)), seat.playerId.value)
      assertEquals(ready.game.cardIndex.toOption.get.get(returned), None)
    }

  test("each player is asked to keep one of exactly the two legacies they " +
      "drew, and keeps the one chosen"):
    var asked = Vector.empty[Decide]
    val keepSecond: Answers =
      case park if park.decisionId ==
          SetupProcedure.legacyDecisionId(park.awaiting) =>
        asked = asked :+ park.decide
        DecisionAnswer.ChooseOneAnswer(
          as[DecisionQuery.ChooseOne](park.decide.query).options(1).ref)
    val woken = Situation.wake(Situation.rules(catalog).withAnswers(keepSecond))
    assertEquals(asked.map(_.owner).toSet, seats.map(_.playerId).toSet)
    asked.foreach(decide => assertEquals(
      as[DecisionQuery.ChooseOne](decide.query).options.map(_.ref),
      drawn(decide.owner).map(DecisionOptionRef.Legacy(_))))
    seats.foreach(seat => assertEquals(
      woken.ready.game.campaign.lineages(seat.lineageId).legacies,
      Vector(LegacyState(drawn(seat.playerId)(1), active = false))))

  test("a player whose short draw is one legacy keeps it unasked, and one " +
      "who drew none is asked nothing (R9)"):
    val Vector(one, none, two) = seats.map(_.lineageId)
    val fresh = FirstGameSetupFixture.freshReady
    val short = fresh.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.updatedWith(one)(_.map(state =>
        state.copy(legacies = state.legacies.take(1))))
        .updatedWith(none)(_.map(_.copy(legacies = Vector.empty)))))
    val setup = walked(short)
    val questions = legacyQuestions(setup.asked)
    assertEquals(questions.map(_.owner),
      Vector(seats.find(_.lineageId == two).get.playerId))
    assertEquals(setup.ready.game.campaign.lineages(one).legacies,
      short.game.campaign.lineages(one).legacies)
    assertEquals(setup.ready.game.campaign.lineages(none).legacies,
      Vector.empty)
    val kept = notes(setup.events).filter(_.key ==
      SetupProcedure.keptLegacy.name).map(_.args)
    assertEquals(kept.toSet, Set(one, two).map(lineage =>
      Vector[NoteArg](NoteArg.Player(seats.find(_.lineageId == lineage)
        .get.playerId))))

  test("under Public Ambitions nobody is asked, and Setup first names the " +
      "row"):
    val fresh = FirstGameSetupFixture.freshReady
    val row = orders.legacyOrder.take(LegacyDeal.PublicRow)
    // The state Public Ambitions leaves at game start (tested above):
    // nobody drew, and the row is dealt.
    val dealt = fresh.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.view.mapValues(_.copy(legacies = Vector.empty)).toMap))
      .updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(publicLegacies = row)))
    val setup = walked(dealt)
    assertEquals(legacyQuestions(setup.asked), Vector.empty)
    assertEquals(setup.ready.game.current.commonCards.publicLegacies, row)
    assertEquals(notes(setup.events).headOption, Some(
      SetupProcedure.dealtPublicLegacies.sourceless(NoteArg.Cards(row))))

  test("each player's keep is logged naming the player and no card"):
    val setup = FirstGameSetupFixture.setupThroughWake()
    val kept = notes(setup.events).filter(_.key ==
      SetupProcedure.keptLegacy.name)
    assertEquals(kept.map(_.args).toSet,
      seats.map(seat => Vector[NoteArg](NoteArg.Player(seat.playerId))).toSet)
    assertEquals(kept.size, seats.size)
