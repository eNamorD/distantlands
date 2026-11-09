package oathdigital.gameplay.walker

import oathdigital.gameplay.powerresolver.{AsksBeforeBatch, BatchAsk,
  BatchContext, Contribution, ExecutedBatch, ExecutedTrigger, Replacement}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.model.TestGameFixtures.sites
import oathdigital.testkit.Outcomes.{finished, parked}
import oathdigital.testkit.ProcedureWalkerFixture._
import oathdigital.testkit.RecordedOps
import oathdigital.testkit.WalkerStubs.{continueOption, stopOption}

/** The walker's batch questions (catalog batch 11, slice 4): a power asks
  * about an operation of a `BuildOps` batch before it runs, and an answer
  * may put a replacement in the operation's place. */
class BatchQuestionsSuite extends munit.FunSuite:
  /** A test power: before a batch, asks about each of the actor's Supply
    * spends, by amount, while the actor's pawn is not at S3. Continue moves
    * the pawn to S3 instead of the spend; Stop keeps it. `watches` says
    * whether it looks at all. */
  private final class AsksBeforeEachSupplySpend(watches: Boolean)
      extends AsksBeforeBatch:
    val id: PowerId = PowerId("test.asks-before-each-supply-spend")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    private val notes = NoteSet(id)
    val movedInstead: NoteKey = notes.key("moved-instead",
      Vector(NotePart.Text("Moved instead of spending.")))
    override def noteKeys: Vector[NoteKey] = notes.declared
    def watching(ready: ReadyGame): Boolean =
      watches && !pawnSiteOf(ready).contains(sites(2))
    def askBefore(batch: BatchContext, operation: CoreOperation)
        : Option[BatchAsk] = operation match
      case spend: SpendSupply if spend.player == actor => Some(BatchAsk(
        Decide(spendDecision(spend.amount), actor, DecisionQuery.ChooseOne(
          Vector(DecisionOption.Button(continueOption, "Move instead"),
            DecisionOption.Button(stopOption, "Spend")))),
        answer => Option.when(answer == ChooseOneAnswer(continueOption))(
          Replacement(Vector(movePawnToS3),
            movedInstead(PowerSourceRef.Card(DenizenId("D1")))))))
      case _ => None

  private def spendDecision(amount: Int): String = s"test.spend.$amount"

  /** A test trigger: after a batch that moves the actor's pawn, asks the
    * fixture's continue-or-stop question, then spends a Supply. */
  private object AsksAfterThePawnMoves extends ExecutedTrigger:
    val id: PowerId = PowerId("test.asks-after-the-pawn-moves")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(batch.executed.contains(movePawnToS3))(Sequence(decide,
        BuildOps((_, _) => Right(Vector[CoreOperation](spendOneSupply)))))

  private val watching = WalkerPowers(Vector(
    AsksBeforeEachSupplySpend(watches = true)))
  private val notWatching = WalkerPowers(Vector(
    AsksBeforeEachSupplySpend(watches = false)))

  /** One batch, at child 0, that spends `amounts` of Supply in order. */
  private def batchSpending(amounts: Int*): Operation =
    Sequence(Vector[Operation](BuildOps((_, _) => Right(
      amounts.toVector.map(SpendSupply(actor, _): CoreOperation)))))

  private def steps(events: Vector[OathEvent]): Vector[WalkerStepRecorded] =
    events.collect { case step: WalkerStepRecorded => step }

  /** The recorded steps that run operations: an answer's step runs none,
    * and the fixture's replay refuses an empty batch. */
  private def stepsWithOperations(events: Vector[OathEvent])
      : Vector[WalkerStepRecorded] =
    steps(events).filter(step => RecordedOps.of(step).nonEmpty)

  private def answered(amount: Int, option: DecisionOptionRef): Answered =
    Answered(spendDecision(amount), ChooseOneAnswer(option), actor)

  /** The position of the question on operation `index` of the batch at
    * child 0. */
  private def questionAt(index: Int): Vector[String] =
    Vector("0", BatchQuestions.segment, "0", index.toString, "0")

  test("a watched batch parks on its question under the leaf's node, and " +
      "nothing runs"):
    val park = parked(ProcedureWalker.advance(ready, batchSpending(1), None,
      watching))
    assertEquals(park.tree.at, questionAt(0))
    assertEquals(park.events, Vector.empty[OathEvent])
    assertEquals(ProcedureWalker.parkedDecide(ready, batchSpending(1),
      PendingTree(at = park.tree.at, answered = Vector.empty), watching)
      .map(_.decisionId), Some(spendDecision(1)))

  test("the answer that takes the replacement writes its line, then runs " +
      "the replacement in the operation's place"):
    val tree = batchSpending(1)
    val park = parked(ProcedureWalker.advance(ready, tree, None, watching))
    val done = finished(ProcedureWalker.resolve(applyEvents(ready,
      park.events), tree, park.tree, answered(1, continueOption), watching))
    assertEquals(steps(done.events).map(_.nodeId),
      Vector("0.asked.0.0.0", "0.asked.1"))
    assertEquals(RecordedOps.of(steps(done.events)(1)),
      Vector[CoreOperation](movePawnToS3))
    assertEquals(done.events.count(_.isInstanceOf[LogNoted]), 1)
    assert(done.events.indexWhere(_.isInstanceOf[LogNoted]) <
      done.events.indexOf(steps(done.events)(1)))
    assertEquals(pawnSiteOf(done.treeless), Some(sites(2)))
    assertEquals(supplyOf(done.treeless), supplyOf(ready))

  test("the other answer runs the batch as built, with no line"):
    val tree = batchSpending(1)
    val park = parked(ProcedureWalker.advance(ready, tree, None, watching))
    val done = finished(ProcedureWalker.resolve(applyEvents(ready,
      park.events), tree, park.tree, answered(1, stopOption), watching))
    assertEquals(RecordedOps.of(steps(done.events)(1)),
      Vector[CoreOperation](spendOneSupply))
    assertEquals(done.events.count(_.isInstanceOf[LogNoted]), 0)
    assertEquals(supplyOf(done.treeless), supplyOf(ready) - 1)

  test("two watched operations ask in order, and each answer replaces its " +
      "own operation only"):
    val tree = batchSpending(1, 2)
    val first = parked(ProcedureWalker.advance(ready, tree, None, watching))
    val second = parked(ProcedureWalker.resolve(applyEvents(ready,
      first.events), tree, first.tree, answered(1, continueOption), watching))
    assertEquals(second.tree.at, questionAt(1))
    val done = finished(ProcedureWalker.resolve(applyEvents(ready,
      stepsWithOperations(first.events ++ second.events)), tree, second.tree,
      answered(2, stopOption), watching))
    assertEquals(RecordedOps.of(steps(done.events).last),
      Vector[CoreOperation](movePawnToS3, SpendSupply(actor, 2)))
    assertEquals(steps(done.events).last.nodeId, "0.asked.1")

  test("the journal's steps replay to the state the walk reached, " +
      "through the walker's own replay, which reads their node ids"):
    val tree = batchSpending(1)
    val park = parked(ProcedureWalker.advance(ready, tree, None, watching))
    val done = finished(ProcedureWalker.resolve(applyEvents(ready,
      park.events), tree, park.tree, answered(1, continueOption), watching))
    val replayed = stepsWithOperations(park.events ++ done.events)
      .foldLeft[Either[OathViolation, OathState]](Right(OathState.Ready(
        ready)))((state, step) => state.flatMap(
        ProcedureWalker.applyRecorded(_, step)))
    val after = replayed match
      case Right(OathState.Ready(state)) => state
      case other => fail(s"the steps did not replay: $other")
    assertEquals(pawnSiteOf(after), pawnSiteOf(done.treeless))
    assertEquals(supplyOf(after), supplyOf(done.treeless))

  test("with no power watching, the batch runs at its own node"):
    val done = finished(ProcedureWalker.advance(ready, batchSpending(1), None,
      notWatching))
    assertEquals(steps(done.events).map(_.nodeId), Vector("0"))
    assertEquals(supplyOf(done.treeless), supplyOf(ready) - 1)

  test("a trigger's follow-up after the answered batch parks under that " +
      "batch's node, and resumes there though the power no longer asks"):
    val powers = WalkerPowers(Vector(
      AsksBeforeEachSupplySpend(watches = true), AsksAfterThePawnMoves))
    val tree = batchSpending(1)
    val park = parked(ProcedureWalker.advance(ready, tree, None, powers))
    val followed = parked(ProcedureWalker.resolve(applyEvents(ready,
      park.events), tree, park.tree, answered(1, continueOption), powers))
    assertEquals(followed.tree.at,
      Vector("0", BatchQuestions.segment, "1", "0", "0"))
    // The pawn is at S3 now, where the power watches nothing.
    val atPark = applyEvents(ready,
      stepsWithOperations(park.events ++ followed.events))
    assertEquals(ProcedureWalker.parkedDecide(atPark, tree, followed.tree,
      powers), Some(decide))
    val done = finished(ProcedureWalker.resolve(atPark, tree, followed.tree,
      Answered(decide.decisionId, ChooseOneAnswer(continueOption), actor),
      powers))
    assertEquals(supplyOf(done.treeless), supplyOf(ready) - 1)
    assertEquals(pawnSiteOf(done.treeless), Some(sites(2)))
