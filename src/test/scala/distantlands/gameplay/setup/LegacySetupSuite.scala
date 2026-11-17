package distantlands.gameplay.setup

import distantlands.application.GameCommand
import distantlands.gameplay.setup.legacies.{Populist, PopulistCard, Seer,
  SeerCard}
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
      decide.decisionId == LegacyKeep.decisionId(decide.owner))

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

  test("each player sorts exactly the two legacies they drew into Keep, " +
      "one, and Discard, and keeps the one placed in Keep"):
    var asked = Vector.empty[Decide]
    val keepSecond: Answers =
      case park if park.decisionId ==
          LegacyKeep.decisionId(park.awaiting) =>
        asked = asked :+ park.decide
        val Vector(first, second) =
          as[DecisionQuery.Partition](park.decide.query).options.map(_.ref)
        DecisionAnswer.PartitionAnswer(Vector(
          DecisionPlacement(first, "discard"),
          DecisionPlacement(second, "keep")))
    val woken = Situation.wake(Situation.rules(catalog).withAnswers(keepSecond))
    assertEquals(asked.map(_.owner).toSet, seats.map(_.playerId).toSet)
    asked.foreach { decide =>
      val query = as[DecisionQuery.Partition](decide.query)
      assertEquals(query.options.map(_.ref),
        drawn(decide.owner).map(DecisionOptionRef.Legacy(_)))
      assertEquals(query.sections, Vector(
        DecisionSection("keep", "Keep", 1, Some(1)),
        DecisionSection("discard", "Discard", 0)))
      assertEquals(query.confirmLabel, Some("Keep Legacy"))
    }
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
      LegacyKeep.keptLegacy.name).map(_.args)
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
      LegacyKeep.keptLegacy.name)
    assertEquals(kept.map(_.args).toSet,
      seats.map(seat => Vector[NoteArg](NoteArg.Player(seat.playerId))).toSet)
    assertEquals(kept.size, seats.size)

  /** `seats` from `first` round the table. */
  private def inTurnOrder(first: PlayerId): Vector[PlayerId] =
    val ids = seats.map(_.playerId)
    val at = ids.indexOf(first)
    ids.drop(at) ++ ids.take(at)

  /** The keep and pawn questions `setup` asked, in order. */
  private def keepsAndPawns(setup: SetupWalkDriver.SetupWalked)
      : Vector[String] =
    setup.asked.map(_.decisionId).filter(id => seats.exists(seat =>
      id == LegacyKeep.decisionId(seat.playerId) ||
        id == SetupProcedure.pawnDecisionId(seat.playerId)))

  test("in a first game each player keeps right after placing their pawn " +
      "(CR p. 7)"):
    val ready = accepted(GameStartRules.evolve(catalog, chronicle, orders))
    assertEquals(keepsAndPawns(walked(ready)),
      inTurnOrder(orders.firstPlayer).flatMap(player => Vector(
        SetupProcedure.pawnDecisionId(player),
        LegacyKeep.decisionId(player))))

  test("in a first game every seat draws its two legacies before the first " +
      "pawn, so each player places holding them (CR p. 7)"):
    val first = orders.firstPlayer
    val atPawn = Situation.start(Situation.rules(catalog))
      .parkedAt(SetupProcedure.pawnDecisionId(first))(
        GameCommand.Begin(chronicle, orders))
    seats.foreach(seat => assertEquals(
      atPawn.ready.game.current.legacyHands.getOrElse(seat.playerId,
        Vector.empty), drawn(seat.playerId), seat.playerId.value))
    assert(seats.forall(seat => atPawn.ready.game.campaign
      .lineages(seat.lineageId).legacies.isEmpty), "nobody has kept yet")

  test("in a later game every seat keeps while Foundation III resolves, " +
      "in turn order, before any pawn (catalog batch 13, ruling 3)"):
    // With an Empire the Chancellor, on l1, goes first; with none seated,
    // Setup With No Empire lets the second seat go first, so the keeps
    // start there.
    val noEmpire = later.copy(lineages = Vector.empty)
    Vector(later -> seats.head.playerId, noEmpire -> seats(1).playerId)
      .foreach { (record, first) =>
        val ready = accepted(GameStartRules.evolve(catalog, record,
          LaterGameFixture.orders(record, first)))
        val order = inTurnOrder(first)
        assertEquals(keepsAndPawns(walked(ready)),
          order.map(LegacyKeep.decisionId) ++
            order.map(SetupProcedure.pawnDecisionId), first)
      }

  test("an exile-only game whose only history was stored legacies keeps " +
      "after each pawn, as a first game does (R41)"):
    // The wipe leaves a baseline with no history, so pawn placement and
    // the keep both read a first game.
    val legacyOnly = chronicle.copy(lineages = Vector(LineageState(
      seats.head.lineageId, None, Role.Exile,
      Vector(LegacyState(catalog.legacies.head.id, active = true)),
      Vector.empty)))
    val first = orders.firstPlayer
    val ready = accepted(GameStartRules.evolve(catalog, legacyOnly,
      LaterGameFixture.orders(legacyOnly, first)))
    assertEquals(keepsAndPawns(walked(ready)),
      inTurnOrder(first).flatMap(player => Vector(
        SetupProcedure.pawnDecisionId(player),
        LegacyKeep.decisionId(player))))

  test("in a later game the keeps follow the banks and come before the " +
      "world deck: Populist asks first and Seer after"):
    val holder = seats.last
    val record = SetupLegacyFixture.holding(later, holder.lineageId,
      Role.Exile, PopulistCard.id -> true, SeerCard.id -> true)
    var asked = Vector.empty[String]
    val recording: Answers =
      case park if { asked :+= park.decisionId; false } => ???
    Situation.wake(Situation.productionRules(catalog).withAnswers(recording),
      record, LaterGameFixture.orders(record, seats.head.playerId))
    val keeps = seats.map(seat => asked.indexOf(
      LegacyKeep.decisionId(seat.playerId)))
    assert(keeps.forall(_ >= 0), asked)
    val populist = asked.indexOf(Populist.decisionId(holder.playerId))
    val seer = asked.indexOf(Seer.decisionId(holder.playerId))
    assert(populist >= 0 && populist < keeps.min, asked)
    assert(seer > keeps.max, asked)
