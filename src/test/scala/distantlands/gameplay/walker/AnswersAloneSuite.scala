package distantlands.gameplay.walker

import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Outcomes.{finished, parked}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.{accepted, present}

/** `Decide.answersAlone`: a choose-one the walker answers itself when one
  * option is choosable (catalog batch 13, ruling 4). p1 starts with 1
  * supply, and each answer the question has gains 1. */
class AnswersAloneSuite extends munit.FunSuite:
  private val start = Table.start.supply(p1, 1).ready
  private val powers = WalkerPowers(Vector.empty)
  private val id = "test.alone"

  private def button(key: String): DecisionOption =
    DecisionOption.Button(DecisionOptionRef.Button(key), key)

  /** The question, then a gain of 1 supply for each answer it has. */
  private def asking(options: Vector[DecisionOption], alone: Boolean)
      : Operation = Sequence(Vector(
    Decide(id, p1, DecisionQuery.ChooseOne(options), answersAlone = alone),
    Branch((_, pending) => pending.answered.filter(_.decisionId == id)
      .map(_ => GainSupply(p1, 1)))))

  private def choices(events: Vector[OathEvent])
      : Vector[(String, DecisionAnswer, PlayerId)] = events.collect {
    case WalkerStepRecorded(_, ChoicePayload(decision, answer, by), _) =>
      (decision, answer, by)
  }

  test("with one option the walker answers for the owner, records the " +
      "answer and walks on"):
    val done = finished(ProcedureWalker.advance(start,
      asking(Vector(button("only")), alone = true), None, powers))
    assertEquals(Look(done.treeless).supply(p1), 2)
    assertEquals(choices(done.events), Vector((id,
      ChooseOneAnswer(DecisionOptionRef.Button("only")), p1)))

  test("an unavailable option is not choosable, so the one available " +
      "option is answered alone"):
    val done = finished(ProcedureWalker.advance(start, asking(Vector(
      button("only"), DecisionOption.Unavailable(button("no"),
        OptionReason.Written("not now"))), alone = true), None, powers))
    assertEquals(choices(done.events).map(_._2), Vector(
      ChooseOneAnswer(DecisionOptionRef.Button("only"))))

  test("with two options it parks as any question does"):
    val tree = asking(Vector(button("a"), button("b")), alone = true)
    val pending = parked(ProcedureWalker.advance(start, tree, None, powers))
    assertEquals(present(ProcedureWalker.parkedDecide(start, tree,
      pending.tree, powers), "the question").decisionId, id)

  test("without the mark one option still parks"):
    val tree = asking(Vector(button("only")), alone = false)
    val pending = parked(ProcedureWalker.advance(start, tree, None, powers))
    assertEquals(present(ProcedureWalker.parkedDecide(start, tree,
      pending.tree, powers), "the question").decisionId, id)
    accepted(ProcedureWalker.resolve(start, tree, pending.tree,
      Answered(id, ChooseOneAnswer(DecisionOptionRef.Button("only")), p1),
      powers))
