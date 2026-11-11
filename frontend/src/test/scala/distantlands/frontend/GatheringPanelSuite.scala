package distantlands.frontend

import distantlands.protocol.{DecisionAnswerWire, GameIntent}
import distantlands.testkit.Unwrap.as
import org.scalajs.dom

/** The Gathering's open question: who is there, a box per player the
  * viewer may name, Negotiate, and Pass or Take back pass. Runs under
  * jsdom. */
class GatheringPanelSuite extends munit.FunSuite:
  private def query(passed: Vector[String] = Vector.empty) =
    DecisionQueryState.Gathering(Vector("red", "blue", "green"), passed,
      Vector("blue", "green"), Some("The Gathering at Ancient City"))

  private def render(value: DecisionQueryState.Gathering,
      ui: RecordingControls, canControl: Boolean = true,
      playerId: String = "red"): dom.Element =
    val panel = dom.document.createElement("div")
    val decision = WalkerDecisionState("search", "gathering.open",
      query = Some(value))
    GatheringPanel.render(ParkedDecision.Surface.Gathering(decision, value),
      playerId, canControl, panel, ui)
    panel

  private def button(root: dom.Element, selector: String): dom.html.Button =
    as[dom.html.Button](Dom.one(root, selector))

  private def box(root: dom.Element, name: String): dom.html.Input =
    as[dom.html.Input](Dom.one(root,
      s"input[aria-label='Negotiate with $name']"))

  test("the panel names who is there and who has passed"):
    val panel = render(query(passed = Vector("green")), new RecordingControls())
    assertEquals(panel.querySelector("h2").textContent,
      "The Gathering at Ancient City")
    assertEquals(panel.querySelector(".gathering-present").textContent,
      "red · blue · green (passed)")

  test("Negotiate opens a deal with the checked players, and waits for one"):
    val ui = new RecordingControls()
    val panel = render(query(), ui)
    assert(button(panel, "button.gathering-open").disabled)
    val green = box(panel, "green")
    green.checked = true
    green.dispatchEvent(new dom.Event("change"))
    assert(!button(panel, "button.gathering-open").disabled)
    button(panel, "button.gathering-open").click()
    assertEquals(ui.submitted, Vector(GameIntent.ResolveWalker("gathering.open",
      DecisionAnswerWire.OpenDealWire(Vector("green")))))

  test("Pass, and Take back pass once passed"):
    val ui = new RecordingControls()
    button(render(query(), ui), "button.gathering-pass").click()
    button(render(query(passed = Vector("red")), ui),
      "button.gathering-unpass").click()
    assertEquals(ui.submitted, Vector(
      GameIntent.ResolveWalker("gathering.open",
        DecisionAnswerWire.PassGatheringWire),
      GameIntent.ResolveWalker("gathering.open",
        DecisionAnswerWire.UnpassGatheringWire)))

  test("a viewer out of control gets disabled controls, and one not there " +
      "gets none"):
    val idle = render(query(), new RecordingControls(), canControl = false)
    assert(button(idle, "button.gathering-pass").disabled)
    assert(box(idle, "blue").disabled)
    val away = render(query(), new RecordingControls(), playerId = "purple")
    assertEquals(away.querySelectorAll("button").length, 0)
