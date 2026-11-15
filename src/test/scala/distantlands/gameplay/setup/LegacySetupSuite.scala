package distantlands.gameplay.setup

import distantlands.gameplay.setup.legacies.SeerCard
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

  /** A later game with a Chancellor on l1, where stored legacies stay: a
    * first game played exile-only wipes them (R40). */
  private val later = LaterGameFixture.chronicle(LineageId("l1"),
    LineageId("l2"))

  private def publicAmbitions(base: Chronicle): Chronicle = base.copy(
    foundations = base.foundations.updated(FoundationNumber.III,
      FoundationState(FoundationFace.Altered, Set.empty)))

  test("Quiet Ambitions' game start deals no legacy: each seated lineage " +
      "starts from its record and the deck holds the whole order"):
    val seated = seats.head.lineageId
    val kept = LegacyState(SeerCard.id, active = true)
    val keeping = later.copy(lineages = later.lineages.map(lineage =>
      if lineage.id == seated then lineage.copy(legacies = Vector(kept))
      else lineage))
    val order = LegacyDeal.deck(catalog, keeping)
    val ready = accepted(GameStartRules.evolve(catalog, keeping,
      LaterGameFixture.orders(keeping, seats.head.playerId)
        .copy(legacyOrder = order)))
    seats.foreach(seat => assertEquals(
      ready.game.campaign.lineages(seat.lineageId).legacies,
      if seat.lineageId == seated then Vector(kept) else Vector.empty,
      seat.playerId.value))
    assertEquals(ready.game.current.commonCards.legacyDeck, order)
    assertEquals(ready.game.current.commonCards.publicLegacies, Vector.empty)

  test("Public Ambitions deals 4 legacies faceup to the row and nobody " +
      "draws"):
    val ready = accepted(GameStartRules.evolve(catalog,
      publicAmbitions(chronicle), orders))
    assertEquals(ready.game.current.commonCards.publicLegacies,
      orders.legacyOrder.take(4))
    assert(seats.forall(seat =>
      ready.game.campaign.lineages(seat.lineageId).legacies.isEmpty))

  private def drawn(player: PlayerId): Vector[LegacyId] =
    SetupLegacyFixture.drawn(orders, player)

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
      assertEquals(ready.game.cardIndex.toOption.get.get(returned)
        .map(_.location.container), Some(CardContainer.Deck(CardDeck.Legacy)))
    }
    assert(seats.forall(seat =>
      ready.game.current.legacyHands.getOrElse(seat.playerId, Vector.empty)
        .isEmpty), "every hand is empty after the keeps")
    // Each returned legacy went to the bottom, below the deal's rest.
    assertEquals(ready.game.current.commonCards.legacyDeck
      .takeRight(seats.size).toSet, seats.map(seat => drawn(seat.playerId)(1)).toSet)

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

  test("a short deck draws in keep order, and a later keep can draw a " +
      "legacy an earlier one returned (R9; catalog batch 13, ruling 6)"):
    // Box lineages hold every legacy but 3, so the derived deck is short.
    // A later game with a Chancellor keeps them; a first game played
    // exile-only would wipe them back into the deck (R40).
    val held = catalog.legacies.map(_.id).dropRight(3)
    val boxed = held.grouped(12).zipWithIndex.map((ids, n) => LineageState(
      LineageId(s"boxed-$n"), None, Role.Exile,
      ids.map(LegacyState(_, active = true)), Vector.empty)).toVector
    val short = later.copy(lineages = later.lineages ++ boxed)
    val shortOrders = LaterGameFixture.orders(short, seats.head.playerId)
      .copy(legacyOrder = LegacyDeal.deck(catalog, short))
    val Vector(a, b, c) = shortOrders.legacyOrder
    val started = accepted(GameStartRules.evolve(catalog, short, shortOrders))
    val Vector(first, second, third) = seats
    // The default answer keeps the first option: the first keep takes a
    // and returns b to the bottom, under c; the second draws c and b,
    // keeps c and returns b; the third draws b alone and keeps it unasked.
    val setup = walked(started)
    assertEquals(legacyQuestions(setup.asked).map(_.owner),
      Vector(first.playerId, second.playerId))
    def holding(seat: FirstGameParticipant) =
      setup.ready.game.campaign.lineages(seat.lineageId).legacies
    assertEquals(holding(first), Vector(LegacyState(a, active = false)))
    assertEquals(holding(second), Vector(LegacyState(c, active = false)))
    assertEquals(holding(third), Vector(LegacyState(b, active = false)))
    assertEquals(setup.ready.game.current.commonCards.legacyDeck, Vector.empty)
    val kept = notes(setup.events).filter(_.key ==
      SetupProcedure.keptLegacy.name).map(_.args)
    assertEquals(kept.toSet, seats.map(seat =>
      Vector[NoteArg](NoteArg.Player(seat.playerId))).toSet)

  test("under Public Ambitions nobody is asked, and Setup first names the " +
      "row"):
    // An altered Foundation III is history, so this starts an exile-only
    // game.
    val started = accepted(GameStartRules.evolve(catalog,
      publicAmbitions(chronicle), orders))
    val row = orders.legacyOrder.take(LegacyDeal.PublicRow)
    val setup = walked(started)
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
