package distantlands.gameplay.walker

import distantlands.model._

/** `ChooseAmounts`: one amount per slot, each within its bounds, and with
  * `distinct` no two equal (Legacy Setup powers design, ruling 15; R18). */
class ChooseAmountsQuerySuite extends munit.FunSuite:
  private val (a, b, c) = (DecisionOptionRef.Vision(VisionId("vision:a")),
    DecisionOptionRef.Vision(VisionId("vision:b")),
    DecisionOptionRef.Vision(VisionId("vision:c")))
  private def slot(ref: DecisionOptionRef, suggested: Int) =
    DistributeSlot(ref, 1, 5, Some(suggested))
  private val query = DecisionQuery.ChooseAmounts(
    Vector(slot(a, 1), slot(b, 2), slot(c, 3)), distinct = true,
    heading = Some("Place"), confirmLabel = "Place")
  private def answer(amounts: (DecisionOptionRef, Int)*) =
    DecisionAnswer.DistributeAnswer(amounts.toVector.map(DistributeAmount(_, _)))
  private val p1 = PlayerId("p1")

  private def accepts(q: DecisionQuery, answered: DecisionAnswer) =
    DecisionQueries.accepts("d", q, answered, p1)

  test("a well-formed query"):
    assertEquals(DecisionQueries.wellFormed("d", query), Right(()))

  test("distinct amounts within their bounds are accepted"):
    assertEquals(accepts(query, answer(a -> 5, b -> 1, c -> 3)), Right(()))

  test("two equal amounts are refused when distinct is set"):
    assert(accepts(query, answer(a -> 2, b -> 2, c -> 3)).isLeft)
    assertEquals(accepts(query.copy(distinct = false),
      answer(a -> 2, b -> 2, c -> 3)), Right(()))

  test("an amount outside its bounds, a missing or a repeated slot is " +
      "refused"):
    assert(accepts(query, answer(a -> 6, b -> 1, c -> 3)).isLeft)
    assert(accepts(query, answer(a -> 1, b -> 2)).isLeft)
    assert(accepts(query, answer(a -> 1, a -> 2, c -> 3)).isLeft)

  test("another shape's answer is refused"):
    assert(accepts(query, DecisionAnswer.ChooseAmountAnswer(1)).isLeft)

  test("a malformed query is refused"):
    Vector(
      query.copy(heading = None),
      query.copy(confirmLabel = " "),
      query.copy(slots = Vector.empty),
      query.copy(slots = Vector(slot(a, 1), slot(a, 2))),
      query.copy(slots = Vector(DistributeSlot(a, 3, 2, None))),
      // Three distinct amounts cannot fit in 1..2.
      query.copy(slots = Vector(a, b, c).map(DistributeSlot(_, 1, 2, None))),
      // Every slot fixed: nothing to decide.
      query.copy(slots = Vector(DistributeSlot(a, 1, 1, None))),
      query.copy(slots = Vector(slot(a, 1), slot(b, 1), slot(c, 3))),
      query.copy(slots = Vector(slot(a, 1), DistributeSlot(b, 1, 5, None)))
    ).foreach(bad => assert(DecisionQueries.wellFormed("d", bad).isLeft, bad))
