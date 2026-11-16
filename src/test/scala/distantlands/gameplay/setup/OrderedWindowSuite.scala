package distantlands.gameplay.setup

import distantlands.gameplay.WalkerRecordedOpsReducer
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferResolution}
import distantlands.gameplay.walker.{ChoicePayload, ProcedureWalker,
  WalkerPowers, WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Outcomes.{finished, parked}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.present

/** `OrderedWindow`, the host of Setup's pawn window (catalog batch 13,
  * ruling 4), over stub powers that gain p1 supply. p1 starts with 1
  * supply, so each gain shows below the supply limit of 7, and a power may stand only at some supply,
  * so what an earlier power gained decides whether it applies. */
class OrderedWindowSuite extends munit.FunSuite
    with WalkerRecordedOpsReducer:
  private val start = Table.start.supply(p1, 1).ready
  private val window = OrderedWindow(p1, Vector.empty)

  private def supply(ready: ReadyGame): Int = Look(ready).supply(p1)

  /** Stub of ContributingPower: offers `steps`, under the button `name`,
    * while `stands` holds of p1's supply. */
  private final case class Offering(name: String, stands: Int => Boolean,
      steps: Vector[Operation]) extends ContributingPower:
    def id: PowerId = PowerId(s"test.$name")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
      PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
        Option.when(stands(Look(ctx.state).supply(p1)))(ResolutionOffer(
          DecisionOption.Button(DecisionOptionRef.Button(name), name),
          steps)))))

  private def gaining(name: String, amount: Int,
      stands: Int => Boolean = _ => true): Offering =
    Offering(name, stands, Vector(GainSupply(p1, amount)))

  private def pick(round: Int, name: String): Answered =
    Answered(OrderedWindow.decisionId(p1, round),
      ChooseOneAnswer(DecisionOptionRef.Button(name)), p1)

  private def choices(events: Vector[OathEvent])
      : Vector[(String, DecisionAnswer)] = events.collect {
    case WalkerStepRecorded(_, ChoicePayload(id, answer, _), _) => id -> answer
  }

  test("two standing offers ask the placer which resolves first, in " +
      "power order"):
    val powers = WalkerPowers(Vector(gaining("b", 10), gaining("a", 1)))
    val pending = parked(ProcedureWalker.advance(start, window, None, powers))
    val decide = present(ProcedureWalker.parkedDecide(start, window,
      pending.tree, powers), "the question")
    assertEquals(decide.decisionId, OrderedWindow.decisionId(p1, 0))
    assertEquals(decide.owner, p1)
    decide.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector(
          DecisionOptionRef.Button("a"), DecisionOptionRef.Button("b")))
        assertEquals(one.heading, Some(OrderedWindow.heading))
      case other => fail(s"expected a choose-one, got $other")

  test("the chosen power resolves, then the window is gathered again: a " +
      "power that no longer applies drops out"):
    val powers = WalkerPowers(Vector(gaining("a", 1),
      gaining("b", 10, stands = _ == 1)))
    val pending = parked(ProcedureWalker.advance(start, window, None, powers))
    val done = finished(ProcedureWalker.resolve(start, window, pending.tree,
      pick(0, "a"), powers))
    assertEquals(supply(done.treeless), 2)

  test("a power the first one made applicable joins; one power left " +
      "resolves without a question; each resolves once"):
    val powers = WalkerPowers(Vector(gaining("a", 1),
      gaining("c", 3, stands = _ >= 2)))
    val done = finished(ProcedureWalker.advance(start, window, None, powers))
    assertEquals(supply(done.treeless), 5)
    assertEquals(choices(done.events), Vector(
      OrderedWindow.decisionId(p1, 0) ->
        ChooseOneAnswer(DecisionOptionRef.Button("a")),
      OrderedWindow.decisionId(p1, 1) ->
        ChooseOneAnswer(DecisionOptionRef.Button("c"))))

  test("with nothing offered the window asks and records nothing"):
    val done = finished(ProcedureWalker.advance(start, window, None,
      WalkerPowers(Vector(gaining("a", 1, stands = _ => false)))))
    assertEquals(done.events, Vector.empty[OathEvent])

  test("a walk resumed inside a power that joined after one resolved alone " +
      "rebuilds every round the same way"):
    // At the resume p1 has 2 supply, so the first round now sees both
    // offers; its recorded pick still names the one it resolved.
    val asks = Offering("j", _ >= 2, Vector(
      Decide("test.j", p1, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(DecisionOptionRef.Button("x"), "x"),
        DecisionOption.Button(DecisionOptionRef.Button("y"), "y")))),
      GainSupply(p1, 3)))
    val powers = WalkerPowers(Vector(gaining("a", 1), asks))
    val parkedIn = parked(ProcedureWalker.advance(start, window, None,
      powers))
    val there = foldRecordedOps(start, parkedIn.events, "the walk to j")
    assertEquals(supply(there), 2)
    val done = finished(ProcedureWalker.resolve(there, window,
      parkedIn.tree, Answered("test.j",
        ChooseOneAnswer(DecisionOptionRef.Button("x")), p1), powers))
    assertEquals(supply(done.treeless), 5)
