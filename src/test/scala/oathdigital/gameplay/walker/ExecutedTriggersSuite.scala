package oathdigital.gameplay.walker

import oathdigital.gameplay.powerresolver.{Contribution, ExecutedBatch,
  ExecutedTrigger}
import oathdigital.testkit.RecordedOps
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.Outcomes.{finished, parked, recorded}
import oathdigital.testkit.ProcedureWalkerFixture._
import oathdigital.testkit.WalkerStubs.continueOption

class ExecutedTriggersSuite extends munit.FunSuite:
  /** Whether `executed` moves the fixture actor's pawn. */
  private def movesThePawn(executed: Vector[CoreOperation]): Boolean =
    executed.exists {
      case Move(Piece.Pawn(player), _, _, _, _) => player == actor
      case _ => false
    }

  /** A test trigger: after a batch that moves the actor's pawn, asks the
    * fixture's continue-or-stop question, then spends a Supply. */
  private object AsksAfterThePawnMoves extends ExecutedTrigger:
    val id: PowerId = PowerId("test.asks-after-the-pawn-moves")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(movesThePawn(batch.executed))(Sequence(decide,
        BuildOps((_, _) => Right(Vector[CoreOperation](spendOneSupply)))))

  /** The move back from S3 to where the fixture's pawn starts. */
  private val movePawnBack: Move = Move(Piece.Pawn(actor), movePawnToS3.to,
    movePawnToS3.from)

  /** A test trigger: after a batch that moves the actor's pawn to S3,
    * moves it back, asking nothing. It reads the batch, not the state, so a
    * resume rebuilds it after the move back; and its own move back is not
    * a move to S3, so it does not start itself again. */
  private object MovesThePawnBackFromS3 extends ExecutedTrigger:
    val id: PowerId = PowerId("test.moves-the-pawn-back-from-s3")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(batch.executed.contains(movePawnToS3))(
        BuildOps((_, _) => Right(Vector[CoreOperation](movePawnBack))))

  /** A test trigger: after a batch that moves the actor's pawn back from
    * S3, asks the fixture's question, then spends a Supply. With `nests`
    * it answers a follow-up's batches, where that move happens. */
  private class AsksAfterTheReturn(nests: Boolean) extends ExecutedTrigger:
    val id: PowerId = PowerId(s"test.asks-after-the-return.$nests")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    override def answersFollowUps: Boolean = nests
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(batch.executed.contains(movePawnBack))(Sequence(decide,
        BuildOps((_, _) => Right(Vector[CoreOperation](spendOneSupply)))))
  private val asksNested = AsksAfterTheReturn(nests = true)

  /** A test trigger that answers every pawn move with another, so its
    * follow-ups would nest without end. */
  private object MovesThePawnForever extends ExecutedTrigger:
    val id: PowerId = PowerId("test.moves-the-pawn-forever")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    override def answersFollowUps: Boolean = true
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(movesThePawn(batch.executed))(BuildOps((_, _) =>
        Right(Vector[CoreOperation](
          if batch.executed.contains(movePawnToS3) then movePawnBack
          else movePawnToS3))))

  private val asking = WalkerPowers(Vector(AsksAfterThePawnMoves))
  /** The pawn moves to S3, then a Supply is spent. */
  private val movedThenSpent: Operation = Sequence(movePawnToS3,
    spendOneSupply)
  private val heldMove: CoreOperation =
    Defer(ExecutedTriggers.keyAt(0), Vector(movePawnToS3))

  test("a batch that moves the pawn holds its operations in its own step " +
      "and parks on the follow-up's question, under the batch's node"):
    val park = parked(ProcedureWalker.advance(ready, movedThenSpent, None,
      asking))
    assertEquals(park.tree.at, Vector("0", "0", "0"))
    assertEquals(park.events.map(e => RecordedOps.of(recorded(e))),
      Vector(Vector[CoreOperation](movePawnToS3, heldMove)))
    assertEquals(park.events.map(recorded(_).nodeId), Vector("0"))

  test("an answer resumes inside the follow-up, which runs before the " +
      "rest of the walk and then takes the held batch"):
    val park = parked(ProcedureWalker.advance(ready, movedThenSpent, None,
      asking))
    val atPark = applyEvents(ready, park.events)
    assert(ExecutedTriggers.holds(atPark, 0))
    val done = finished(ProcedureWalker.resolve(atPark, movedThenSpent,
      park.tree, Answered(decide.decisionId, ChooseOneAnswer(continueOption),
        actor), asking))
    assertEquals(done.events.map(e => RecordedOps.of(recorded(e))), Vector(
      Vector.empty[CoreOperation], Vector[CoreOperation](spendOneSupply),
      Vector[CoreOperation](TakeDeferred(ExecutedTriggers.keyAt(0))),
      Vector[CoreOperation](spendOneSupply)))
    assertEquals(done.events.map(recorded(_).nodeId),
      Vector("0.0.0", "0.0.1", "0.1", "1"))
    assertEquals(done.events.map(recorded(_).contributions).take(2),
      Vector.fill(2)(Vector(AsksAfterThePawnMoves.id)))
    assertEquals(supplyOf(done.treeless), supplyOf(ready) - 2)

  test("a follow-up's batch that no trigger watches starts no follow-up"):
    val done = finished(ProcedureWalker.advance(ready, movedThenSpent, None,
      WalkerPowers(Vector(MovesThePawnBackFromS3))))
    assertEquals(done.events.map(e => RecordedOps.of(recorded(e))), Vector(
      Vector[CoreOperation](movePawnToS3, heldMove),
      Vector[CoreOperation](movePawnBack),
      Vector[CoreOperation](TakeDeferred(ExecutedTriggers.keyAt(0))),
      Vector[CoreOperation](spendOneSupply)))
    assertEquals(pawnSiteOf(done.treeless), pawnSiteOf(ready))

  test("the position parked inside a follow-up resolves to its question, " +
      "which only a held batch makes reachable"):
    val park = parked(ProcedureWalker.advance(ready, movedThenSpent, None,
      asking))
    val atPark = applyEvents(ready, park.events)
    val pending = PendingTree(at = park.tree.at, answered = Vector.empty)
    assertEquals(ProcedureWalker.parkedDecide(atPark, movedThenSpent, pending,
      asking), Some(decide))
    assertEquals(ProcedureWalker.parkedDecide(ready, movedThenSpent, pending,
      asking), None)

  /** The pawn moves to S3; the first trigger moves it back, and that
    * return starts the second trigger's question, one level down. */
  private val nesting = WalkerPowers(Vector(MovesThePawnBackFromS3,
    asksNested))
  /** `trigger`'s place among the triggers a batch at `depth` asks. */
  private def indexAt(depth: Int, trigger: ExecutedTrigger): String =
    ExecutedTriggers.of(nesting, depth).indexOf(trigger).toString
  /** Where the take sits after the follow-ups of a batch at `depth`. */
  private def takeAt(depth: Int): String =
    ExecutedTriggers.of(nesting, depth).size.toString

  test("a trigger that does not answer follow-ups ignores a follow-up's " +
      "batch"):
    val done = finished(ProcedureWalker.advance(ready, movedThenSpent, None,
      WalkerPowers(Vector(MovesThePawnBackFromS3,
        AsksAfterTheReturn(nests = false)))))
    assertEquals(done.events.map(e => RecordedOps.of(recorded(e))), Vector(
      Vector[CoreOperation](movePawnToS3, heldMove),
      Vector[CoreOperation](movePawnBack),
      Vector[CoreOperation](TakeDeferred(ExecutedTriggers.keyAt(0))),
      Vector[CoreOperation](spendOneSupply)))

  test("the depth-0 key is the one saved games hold"):
    assertEquals(ExecutedTriggers.keyAt(0),
      DeferralKey("walker.executed-triggers"))
    assertNotEquals(ExecutedTriggers.keyAt(1), ExecutedTriggers.keyAt(0))

  test("a follow-up's batch that a trigger watches starts a follow-up one " +
      "level down, holding its operations under that level's key"):
    val park = parked(ProcedureWalker.advance(ready, movedThenSpent, None,
      nesting))
    val back = indexAt(0, MovesThePawnBackFromS3)
    val asks = indexAt(1, asksNested)
    assertEquals(park.tree.at, Vector("0", back, asks, "0"))
    assertEquals(park.events.map(e => RecordedOps.of(recorded(e))), Vector(
      Vector[CoreOperation](movePawnToS3, heldMove),
      Vector[CoreOperation](movePawnBack,
        Defer(ExecutedTriggers.keyAt(1), Vector(movePawnBack)))))
    assertEquals(park.events.map(recorded(_).nodeId), Vector("0", s"0.$back"))
    val atPark = applyEvents(ready, park.events)
    assert(ExecutedTriggers.holds(atPark, 0))
    assert(ExecutedTriggers.holds(atPark, 1))

  test("an answer two levels down resumes there and takes each level's " +
      "batch on the way out"):
    val park = parked(ProcedureWalker.advance(ready, movedThenSpent, None,
      nesting))
    val atPark = applyEvents(ready, park.events)
    val done = finished(ProcedureWalker.resolve(atPark, movedThenSpent,
      park.tree, Answered(decide.decisionId, ChooseOneAnswer(continueOption),
        actor), nesting))
    val back = indexAt(0, MovesThePawnBackFromS3)
    val asks = indexAt(1, asksNested)
    assertEquals(done.events.map(e => RecordedOps.of(recorded(e))), Vector(
      Vector.empty[CoreOperation], Vector[CoreOperation](spendOneSupply),
      Vector[CoreOperation](TakeDeferred(ExecutedTriggers.keyAt(1))),
      Vector[CoreOperation](TakeDeferred(ExecutedTriggers.keyAt(0))),
      Vector[CoreOperation](spendOneSupply)))
    assertEquals(done.events.map(recorded(_).nodeId), Vector(
      s"0.$back.$asks.0", s"0.$back.$asks.1", s"0.$back.${takeAt(1)}",
      s"0.${takeAt(0)}", "1"))
    assertEquals(pawnSiteOf(done.treeless), pawnSiteOf(ready))
    assertEquals(supplyOf(done.treeless), supplyOf(ready) - 2)

  test("a position parked two levels down resolves to its question"):
    val park = parked(ProcedureWalker.advance(ready, movedThenSpent, None,
      nesting))
    val atPark = applyEvents(ready, park.events)
    val pending = PendingTree(at = park.tree.at, answered = Vector.empty)
    assertEquals(ProcedureWalker.parkedDecide(atPark, movedThenSpent, pending,
      nesting), Some(decide))

  test("follow-ups nested deeper than the cap stop the walk"):
    val refused = ProcedureWalker.advance(ready, movedThenSpent, None,
      WalkerPowers(Vector(MovesThePawnForever)))
    assertEquals(refused.left.toOption, Some(OathViolation.InvalidEventOrder(
      "walker contract violation: follow-ups nested deeper than " +
        s"${ExecutedTriggers.MaxDepth}")))
