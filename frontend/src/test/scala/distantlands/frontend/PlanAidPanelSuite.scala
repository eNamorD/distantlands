package distantlands.frontend

import distantlands.frontend.Dom.all
import distantlands.model.PlayerColor
import distantlands.protocol.GameIntent
import distantlands.protocol.projection.PlanAidOfferProjection
import org.scalajs.dom

/** The offer panel an Imperial ally builds (Empire phase design, "Offer
  * panel"). Blue, the Chancellor, may offer to Red's defense. */
class PlanAidPanelSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Citizen", PlayerColor.Red)
  private val blue = GamePlayer("blue", "Blue", "Chancellor",
    PlayerColor.Blue)
  private val context = BoardSelectionContext("game", "blue", 1L)
  private val shieldWall = DecisionOptionState("denizen", "108",
    "Shield Wall")
  private val wall = DecisionOptionState("denizen", "109", "Encirclement")
  private val secret = DecisionOptionState("banner", "darkest-secret",
    "Darkest Secret")

  private def prompt(saved: PlanAidOfferProjection = PlanAidOfferProjection(),
      used: PlanAidOfferProjection = PlanAidOfferProjection())
      : PlanAidPromptState = PlanAidPromptState("red", "defender",
    Vector(shieldWall, wall), Vector(secret), saved, used, favorHeld = 2,
    secretsHeld = 0)

  private def pane(open: PlanAidPromptState, draft: PlanAidDraft,
      ui: RecordingControls, canControl: Boolean = true): dom.Element =
    val value = TestProjections.actingProjection(players = Vector(red, blue))
      .copy(planAidPrompt = Some(open))
    val panel = dom.document.createElement("div")
    PlanAidPanel.render(value, canControl, panel,
      SessionDrafts.empty.copy(aid = Some(draft)), ui)
    panel

  private def saveButton(panel: dom.Element): dom.html.Button =
    panel.querySelector(".plan-aid-save").asInstanceOf[dom.html.Button]

  test("the heading names the user"):
    val open = prompt()
    assertEquals(pane(open, PlanAidDraft.fresh(context, open),
      new RecordingControls()).querySelector(".plan-aid-heading").textContent,
      "You may offer your battle plans, to pay costs and to meet " +
        "requirements for Red")

  test("a pending addition shows no mark, a saved item a check, a removal " +
      "is struck"):
    val open = prompt(PlanAidOfferProjection(Vector("denizen:108",
      "denizen:109")))
    val draft = PlanAidDraft.fresh(context, open)
      .toggled(open, "denizen:109").toggled(open, "banner:darkest-secret")
    val panel = pane(open, draft, new RecordingControls())
    assertEquals(all(panel, ".plan-aid-item").map(_.getAttribute("class")), Vector(
      "plan-aid-item plan-aid-saved", "plan-aid-item plan-aid-removed",
      "plan-aid-item plan-aid-pending"))
    assertEquals(all(panel, ".plan-aid-saved .plan-aid-mark")
      .map(_.textContent), Vector("✓"))
    assertEquals(all(panel, ".plan-aid-removed .plan-aid-mark"), Vector.empty)
    assertEquals(all(panel, ".plan-aid-pending .plan-aid-mark"), Vector.empty)

  test("a used item shows a lock and 'used'"):
    val saved = PlanAidOfferProjection(Vector("denizen:108"))
    val open = prompt(saved, used = saved)
    val panel = pane(open, PlanAidDraft.fresh(context, open),
      new RecordingControls())
    assertEquals(all(panel, ".plan-aid-used .plan-aid-mark")
      .map(_.textContent), Vector("🔒 used"))

  test("a saved amount shows how much is offered and used, and cannot go " +
      "below used"):
    val open = prompt(PlanAidOfferProjection(favor = 2),
      PlanAidOfferProjection(favor = 1))
    val panel = pane(open, PlanAidDraft.fresh(context, open).copy(favor = 1),
      new RecordingControls())
    val favor = all(panel, ".plan-aid-amount").head
    assertEquals(all(favor, ".plan-aid-tag").map(_.textContent),
      Vector("2 offered", "1 used"))
    val steps = all(favor, ".plan-aid-step")
      .map(_.asInstanceOf[dom.html.Button])
    assert(steps.head.disabled, "cannot go below what was used")
    assert(!steps.last.disabled)

  test("the button reads Offer, Save changes or Offered"):
    val fresh = prompt()
    val draft = PlanAidDraft.fresh(context, fresh)
    val untouched = saveButton(pane(fresh, draft, new RecordingControls()))
    assertEquals(untouched.textContent, "Offer")
    assert(untouched.disabled, "nothing to offer yet")
    assertEquals(saveButton(pane(fresh, draft.toggled(fresh, "denizen:108"),
      new RecordingControls())).textContent, "Offer")
    val saved = prompt(PlanAidOfferProjection(Vector("denizen:108")))
    val changed = PlanAidDraft.fresh(context, saved).copy(favor = 1)
    assertEquals(saveButton(pane(saved, changed, new RecordingControls()))
      .textContent, "Save changes")
    assert(saveButton(pane(saved, PlanAidDraft.fresh(context, saved),
      new RecordingControls())).disabled)
    assert(saveButton(pane(saved, changed, new RecordingControls(),
      canControl = false)).disabled)

  test("saving submits the whole draft"):
    val open = prompt()
    val draft = PlanAidDraft.fresh(context, open).toggled(open, "denizen:108")
      .copy(favor = 1)
    val ui = new RecordingControls()
    saveButton(pane(open, draft, ui)).click()
    assertEquals(ui.submitted, Vector[GameIntent](draft.command))

  test("a stepper stages the changed amount"):
    val open = prompt()
    val draft = PlanAidDraft.fresh(context, open)
    val ui = new RecordingControls()
    all(pane(open, draft, ui), ".plan-aid-step")(1)
      .asInstanceOf[dom.html.Button].click()
    assertEquals(ui.staged, Vector[Draft](Draft.Aid(Some(draft.copy(
      favor = 1)))))

  test("the title's defense is listed with its own toggle"):
    val title = DecisionOptionState("button", "title",
      "Oathkeeper title: add 1 defense die")
    val open = prompt().copy(plans = Vector(title))
    val draft = PlanAidDraft.fresh(context, open)
    val ui = new RecordingControls()
    val panel = pane(open, draft, ui)
    assertEquals(all(panel, ".plan-aid-offerable .plan-aid-name")
      .map(_.textContent), Vector("Oathkeeper title: add 1 defense die"))
    panel.querySelector(".plan-aid-toggle").asInstanceOf[dom.html.Button]
      .click()
    assertEquals(ui.staged, Vector[Draft](Draft.Aid(Some(draft.copy(
      plans = Vector("button:title"))))))
