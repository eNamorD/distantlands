package distantlands.frontend

import distantlands.protocol.{DecisionAnswerWire, DistributeAmountWire,
  GameIntent => Intent}
import Dom.{click, one}
import ParkedDecision.Surface
import org.scalajs.dom

/** The ChooseAmounts panel at the DOM (Legacy Setup powers design, ruling
  * 15; R25): a row per slot with steppers, and Confirm disabled while two
  * amounts are equal. */
class ChooseAmountsPanelRenderSuite extends munit.FunSuite:
  private def vision(id: String, label: String) =
    DecisionOptionState("vision", id, label)
  private val query = DecisionQueryState.ChooseAmounts(
    Vector(DecisionSlotState(vision("vision:a", "Vision of Faith"), 1, 4,
        Some(1)),
      DecisionSlotState(vision("vision:b", "Vision of Conquest"), 1, 4,
        Some(3))),
    distinct = true, confirmLabel = "Place the Visions",
    heading = Some("Seer: place the Visions in the world deck"))
  private val parked = WalkerDecisionState("setup",
    "setup.legacy.seer.p2", query = Some(query))

  private def opened(): Option[WalkerChooseAmountsDraft] =
    WalkerChooseAmountsDraft.reconcile(None,
      BoardSelectionContext("game", "blue", 9), Some(parked))

  private def render(ui: RecordingControls,
      draft: Option[WalkerChooseAmountsDraft],
      canControl: Boolean = true): dom.Element =
    val panel = dom.document.createElement("div")
    ChooseAmountsPanelRenderer.render(Surface.ChooseAmounts(parked, query),
      draft, canControl, panel, ui)
    panel

  private def amount(panel: dom.Element, item: String): String =
    one(panel, s"""[data-option-id="$item"] .amounts-field""")
      .asInstanceOf[dom.html.Input].value

  test("a row per slot at its suggested amount, the heading above"):
    val panel = render(new RecordingControls(), opened())
    assertEquals(one(panel, "h2").textContent,
      "Seer: place the Visions in the world deck")
    assertEquals(amount(panel, "vision:vision:a"), "1")
    assertEquals(amount(panel, "vision:vision:b"), "3")

  test("a stepper stages the draft one step within the slot's bounds"):
    val ui = new RecordingControls()
    click(one(render(ui, opened()),
      """[data-option-id="vision:vision:a"] .amounts-increment"""))
    assertEquals(ui.drafts.chooseAmounts.map(_.state.amount(
      "vision:vision:a")), Some(2))

  test("a Shift+click jumps to the slot's bound"):
    val ui = new RecordingControls()
    click(one(render(ui, opened()),
      """[data-option-id="vision:vision:a"] .amounts-increment"""),
      shift = true)
    assertEquals(ui.drafts.chooseAmounts.map(_.state.amount(
      "vision:vision:a")), Some(4))

  test("two equal amounts disable Confirm and name the clash"):
    val clashing = opened().map(draft => draft.copy(state =
      draft.state.increment("vision:vision:a").increment("vision:vision:a")))
    val panel = render(new RecordingControls(), clashing)
    assert(one(panel, ".amounts-confirm").asInstanceOf[dom.html.Button]
      .disabled)
    assertEquals(one(panel, ".amounts-clash").textContent,
      "Two cards share position 3")

  test("Confirm submits one amount per slot as a distribution"):
    val ui = new RecordingControls()
    click(one(render(ui, opened()), ".amounts-confirm"))
    assertEquals(ui.submitted, Vector(Intent.ResolveWalker(
      "setup.legacy.seer.p2", DecisionAnswerWire.DistributeWire(Vector(
        DistributeAmountWire("vision", "vision:a", 1),
        DistributeAmountWire("vision", "vision:b", 3))))))

  test("a typed amount stages the draft, clamped to the slot's bounds (R25)"):
    val ui = new RecordingControls()
    val field = one(render(ui, opened()),
      """[data-option-id="vision:vision:b"] .amounts-field""")
      .asInstanceOf[dom.html.Input]
    field.value = "9"
    field.dispatchEvent(new dom.Event("change"))
    assertEquals(ui.drafts.chooseAmounts.map(_.state.amount(
      "vision:vision:b")), Some(4))

  test("a viewer who may not answer sees the steppers disabled"):
    val panel = render(new RecordingControls(), opened(), canControl = false)
    assert(one(panel, """[data-option-id="vision:vision:a"] .amounts-increment""")
      .asInstanceOf[dom.html.Button].disabled)
    assert(one(panel, """[data-option-id="vision:vision:a"] .amounts-field""")
      .asInstanceOf[dom.html.Input].disabled)
    assert(one(panel, ".amounts-confirm").asInstanceOf[dom.html.Button]
      .disabled)

  /** Types `text` into a slot's field and redraws as the table does, the
    * field keeping focus through `PanelContent.replace`. */
  private def typedAndRedrawn(item: String, text: String)
      : (dom.html.Input, RecordingControls) =
    val ui = new RecordingControls()
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    root.appendChild(render(ui, opened()))
    val field = one(root, s"""[data-option-id="$item"] .amounts-field""")
      .asInstanceOf[dom.html.Input]
    field.focus()
    field.value = text
    field.dispatchEvent(new dom.Event("change"))
    val heading = dom.document.createElement("h2").asInstanceOf[dom.html.Element]
    PanelContent.replace(root, render(ui, ui.drafts.chooseAmounts.orElse(
      opened())), heading)
    val redrawn = one(root, s"""[data-option-id="$item"] .amounts-field""")
      .asInstanceOf[dom.html.Input]
    dom.document.body.removeChild(root)
    redrawn -> ui

  test("an out-of-range amount is clamped in the field too, so the field " +
      "shows what Confirm sends"):
    val (field, ui) = typedAndRedrawn("vision:vision:b", "9")
    assertEquals(field.value, "4")
    assertEquals(ui.drafts.chooseAmounts.map(_.state.amount(
      "vision:vision:b")), Some(4))

  test("text that is no number puts the amount back and stages nothing"):
    val (field, ui) = typedAndRedrawn("vision:vision:b", "abc")
    assertEquals(field.value, "3")
    assertEquals(ui.staged, Vector.empty)

  test("set clamps to the slot's bounds, even from the extreme integers"):
    val state = ChooseAmountsDecisionState.opened(Vector(
      DistributeSlotBounds("b", 1, 4)), distinct = true, None)
    assertEquals(state.set("b", Int.MinValue).amount("b"), 1)
    assertEquals(state.set("b", Int.MaxValue).amount("b"), 4)
    assertEquals(state.set("b", 3).amount("b"), 3)
