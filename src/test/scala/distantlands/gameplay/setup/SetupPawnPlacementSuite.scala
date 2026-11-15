package distantlands.gameplay.setup

import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer

/** Who placed, read inside `SetupPawnPlaced` (R27): the latest pawn
  * answer, whatever was answered after it in the window. */
class SetupPawnPlacementSuite extends munit.FunSuite:
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val site = SiteId("site:deep-woods")
  private def pawn(player: PlayerId, ref: DecisionOptionRef) =
    Answered(SetupProcedure.pawnDecisionId(player), ChooseOneAnswer(ref),
      player)

  test("the latest pawn answer names the placer and the site, though a " +
      "window's question was answered after it"):
    val answered = Vector(pawn(p1, DecisionOptionRef.Site(SiteId("site:a"))),
      pawn(p2, DecisionOptionRef.Site(site)),
      Answered("setup.legacy.wealthy.p2", ChooseOneAnswer(
        DecisionOptionRef.Button("bank")), p2))
    assertEquals(SetupPawnPlacement.latest(answered), Some(p2 -> Some(site)))

  test("a player who stayed off placed at no site"):
    assertEquals(SetupPawnPlacement.latest(Vector(pawn(p2,
      DecisionOptionRef.Button("stay-off")))), Some(p2 -> None))

  test("no pawn answer, no placer"):
    assertEquals(SetupPawnPlacement.latest(Vector.empty), None)
