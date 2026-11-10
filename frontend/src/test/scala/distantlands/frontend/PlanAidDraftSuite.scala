package distantlands.frontend

import distantlands.protocol.{GameIntent, WalkerStartArgWire}
import distantlands.protocol.projection.PlanAidOfferProjection

/** An Imperial ally's draft offer (Empire phase design, "Offer panel"). */
class PlanAidDraftSuite extends munit.FunSuite:
  private val context = BoardSelectionContext("game", "p2", 4L)
  private val shieldWall = DecisionOptionState("denizen", "108",
    "Shield Wall")
  private val secret = DecisionOptionState("banner", "darkest-secret",
    "Darkest Secret")
  private def prompt(saved: PlanAidOfferProjection = PlanAidOfferProjection(),
      used: PlanAidOfferProjection = PlanAidOfferProjection())
      : PlanAidPromptState = PlanAidPromptState("p3", "defender",
    Vector(shieldWall), Vector(secret), saved, used, favorHeld = 3,
    secretsHeld = 1)

  test("a fresh draft starts from the saved offer"):
    val saved = PlanAidOfferProjection(Vector("denizen:108"), favor = 1)
    val draft = PlanAidDraft.fresh(context, prompt(saved))
    assertEquals((draft.plans, draft.favor), (Vector("denizen:108"), 1))
    assert(draft.matchesSaved(prompt(saved)))

  test("clicking an offerable card adds it, clicking again removes it"):
    val open = prompt()
    val draft = PlanAidDraft.fresh(context, open)
    val added = draft.toggled(open, "denizen:108").toggled(open,
      "banner:darkest-secret")
    assertEquals((added.plans, added.requirements),
      (Vector("denizen:108"), Vector("banner:darkest-secret")))
    assertEquals(added.toggled(open, "denizen:108").plans, Vector.empty)

  test("a used item cannot be toggled off"):
    val saved = PlanAidOfferProjection(Vector("denizen:108"))
    val open = prompt(saved, used = saved)
    val draft = PlanAidDraft.fresh(context, open)
    assertEquals(draft.toggled(open, "denizen:108").plans,
      Vector("denizen:108"))
    assertEquals(PlanAidDraft.views(open, draft)("denizen:108").state,
      OptionState.Locked)

  test("the draft is kept for the same window and resets for another"):
    val open = prompt()
    val draft = PlanAidDraft.fresh(context, open).toggled(open, "denizen:108")
    val later = context.copy(sequence = 9L)
    assertEquals(PlanAidDraft.reconcile(Some(draft), later, Some(open)),
      Some(draft))
    assertEquals(PlanAidDraft.reconcile(Some(draft), later,
      Some(open.copy(side = "attacker"))).map(_.plans), Some(Vector.empty))
    assertEquals(PlanAidDraft.reconcile(Some(draft), later, None), None)

  test("the command carries the whole draft"):
    val open = prompt()
    val draft = PlanAidDraft.fresh(context, open).toggled(open, "denizen:108")
      .toggled(open, "banner:darkest-secret").copy(favor = 2)
    assertEquals(draft.command, GameIntent.SavePlanAid(
      Vector(WalkerStartArgWire("denizen", "108")),
      Vector(WalkerStartArgWire("banner", "darkest-secret")), 2, 0, 0, 0))
