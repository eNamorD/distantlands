package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.catalog.Locked
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseOneAnswer, PartitionAnswer}
import oathdigital.testkit.{Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** The World task (CR p. 38). p1 is the Chancellor; p2 holds the World. */
class WorldTaskSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val tasksAssigned: Table = ChronicleWalk.throneHeldByChancellorP1

  private val worldDenizens: Vector[DenizenId] =
    tasksAssigned.ready.game.current.commonCards.worldDeck
      .collect { case id: DenizenId => id }

  /** The walk from the end of the Throne: the driver starts the World. */
  private def worldStarted(table: Table)(using munit.Location)
      : OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleThrone))

  private def parkedOnProtect(table: Table)(using munit.Location)
      : OathTransition = ChronicleWalk.walkedTo(rules, worldStarted(table),
    ProtectAndDispossess.protectDecisionId)

  private def gathered(transition: OathTransition): Vector[WorldCardId] =
    Outcomes.ready(transition.state).game.current.chronicleGathering

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.players.find(_.player == player).get.lineage

  test("every Vision in play leaves it"):
    val table = tasksAssigned.revealedVision(p1, VisionRules.Conquest)
      .discarded(Region.Cradle, VisionRules.Faith)
    val ids = Outcomes.ready(parkedOnProtect(table).state).game.cardIndex
      .toOption.get.ids
    assert(!ids.contains(VisionRules.Conquest))
    assert(!ids.contains(VisionRules.Faith))

  test("advisers and discards are gathered; nine are dealt and the rest " +
      "go to the world deck"):
    val discards = worldDenizens.take(8)
    val table = tasksAssigned.discarded(Region.Provinces, discards*)
      .adviser(p1, worldDenizens(8)).adviser(p2, worldDenizens(9))
    val walked = parkedOnProtect(table)
    val ready = Outcomes.ready(walked.state)
    val pooled = (discards :+ worldDenizens(8) :+ worldDenizens(9))
      .toSet[WorldCardId]
    assertEquals(gathered(walked).size, 9)
    assert(gathered(walked).forall(pooled.contains))
    assert((pooled -- gathered(walked)).forall(
      ready.game.current.commonCards.worldDeck.contains))
    assert(ready.game.current.players.forall(_.advisers.isEmpty))
    assert(Region.all.forall(region =>
      ready.game.current.commonCards.discard(region).isEmpty))

  test("with fewer than nine, the difference comes from the world deck top"):
    val table = tasksAssigned.withoutAdvisers(p1).withoutAdvisers(p2)
      .withoutAdvisers(Table.p3).discarded(Region.Cradle,
        worldDenizens.slice(20, 24)*)
    val top = table.ready.game.current.commonCards.worldDeck.take(5)
    assertEquals(gathered(parkedOnProtect(table)).toSet,
      (worldDenizens.slice(20, 24) ++ top).toSet[WorldCardId])

  test("the World holder protects a faceup stack and keeps one secretly; " +
      "the other six are dispossessed when the step ends"):
    val walked = parkedOnProtect(tasksAssigned)
    val decide = ChronicleWalk.parked(walked.state).get
    assertEquals(decide.owner, p2)
    val nine = gathered(walked)
    decide.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), nine.take(6).map(card =>
          DecisionOptionRef.Denizen(card.asInstanceOf[DenizenId])))
      case other => fail(other.toString)
    val second = nine.slice(3, 6)
    val protectedStep = accepted(rules.resolveWalker(walked.state, p2,
      ProtectAndDispossess.protectDecisionId, ChooseOneAnswer(
        DecisionOptionRef.Denizen(second(1).asInstanceOf[DenizenId]))))
    val parked = Outcomes.ready(protectedStep.state)
    assertEquals(parked.game.current.temporaryHands.get(p2), Some(second))
    assertEquals(parked.game.current.chronicleGathering,
      nine.take(3) ++ nine.drop(6))
    val kept = second.last
    val done = accepted(rules.resolveWalker(protectedStep.state, p2,
      ProtectAndDispossess.keepDecisionId, PartitionAnswer(second.map(card =>
        DecisionPlacement(DecisionOptionRef.Denizen(
          card.asInstanceOf[DenizenId]),
          if card == kept then StartingAdviserChoice.keepKey
          else StartingAdviserChoice.deckKey)))))
    val after = Outcomes.ready(done.state)
    assertEquals(after.game.campaign.lineages(lineageOf(after, p2))
      .startingAdvisers.map(_.id), Vector(kept))
    assertEquals(after.game.current.commonCards.worldDeck.takeRight(2).toSet,
      second.init.toSet)
    assertEquals(after.game.current.temporaryHands.getOrElse(p2, Vector.empty),
      Vector.empty)
    assertEquals(after.game.current.chronicleGathering, Vector.empty)
    assertEquals(after.game.current.newlyDispossessed,
      nine.take(3) ++ nine.drop(6))

  test("a faceup locked adviser is gathered too (Chronicle ruling 16)"):
    val locked = catalog.denizens.collectFirst {
      case card: Locked => DenizenId(card.id.value) }.get
    val walked = parkedOnProtect(tasksAssigned.adviser(p1, locked))
    assert(Outcomes.ready(walked.state).game.current.players
      .forall(_.advisers.isEmpty))

  test("an adviser's favor returns to its bank and its secrets to its " +
      "holder, facedown"):
    val card = worldDenizens.head
    val table = tasksAssigned.adviser(p1, card).tokens(card, favor = 2,
      secrets = 1)
    val suit = catalog.suitOf(card).get
    val before = table.ready
    val after = Outcomes.ready(parkedOnProtect(table).state)
    assertEquals(after.banks.favor.getOrElse(suit, 0),
      before.banks.favor.getOrElse(suit, 0) + 2)
    val board = (ready: ReadyGame) =>
      ready.game.current.players.find(_.player == p1).get.board
    assertEquals(board(after).faceDownSecrets, board(before).faceDownSecrets + 1)
