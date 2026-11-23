package distantlands.frontend

import distantlands.protocol.projection.DieFace

import distantlands.frontend.Dom.{all, click}
import distantlands.model.PlayerColor
import distantlands.protocol.{GameIntent => Intent, WalkerStartArgWire}
import distantlands.testkit.Unwrap.as
import ParkedDecision.{RecoverStep, Surface}
import org.scalajs.dom

/** Recover's controls.
  *
  * The walker rolls as it walks and parks only on what it has to ask, so
  * one answer buys the next two dice and rolls them. The label says both,
  * because both happen on the click.
  */
class RecoverPanelSuite extends munit.FunSuite:
  /** The panel takes the routed surface; the projection rides along only
    * for the acting player's supply, which gates buying more dice.
    */
  private def render(surface: Surface.Recover, supply: Int = 5): dom.Element =
    val panel = dom.document.createElement("div")
    WalkerPanelSupport.renderRecoverPanel(surface,
      TestProjections.actingProjection().copy(activePlayerResources = Some(
        ActivePlayerResources(favor = 0, faceUpSecrets = 0, faceDownSecrets = 0,
          committedSecrets = 0, totalSecrets = 0, supply = supply))),
      canControl = true, panel, new RecordingControls())
    panel

  private val choiceQuery = DecisionQueryState.ChooseOne(
    Vector(DecisionOptionState("button", "continue", "Continue"),
      DecisionOptionState("button", "stop", "Stop")),
    heading = Some("Recover"))

  private def choice(rollOutcome: Option[WalkerRollOutcomeState] = None)
      : Surface.Recover =
    Surface.Recover(WalkerDecisionState("recover",
      ParkedDecision.recoverChoiceDecisionId,
      query = Some(choiceQuery), rollOutcome = rollOutcome),
      RecoverStep.Choice(choiceQuery))

  /** One click buys the dice and throws them, so the label names both the
    * cost and the roll rather than promising a second button.
    */
  test("continuing says it buys dice and rolls them"):
    val panel = render(choice())
    assertEquals(all(panel, ".recover-add").map(_.textContent),
      Vector("Roll two more dice (1 Supply)"))
    assertEquals(all(panel, ".recover-stop").map(_.textContent),
      Vector("Stop Recover"))

  test("without the supply to buy them, the control is withheld with its reason"):
    val ui = new RecordingControls()
    val panel = dom.document.createElement("div")
    WalkerPanelSupport.renderRecoverPanel(choice(),
      TestProjections.actingProjection().copy(activePlayerResources = Some(
        ActivePlayerResources(favor = 0, faceUpSecrets = 0, faceDownSecrets = 0,
          committedSecrets = 0, totalSecrets = 0, supply = 0))),
      canControl = true, panel, ui)
    val add = as[dom.html.Button](all(panel, ".recover-add").head)
    assertEquals(add.getAttribute("aria-disabled"), "true")
    assertEquals(add.getAttribute("data-tip"), "No Supply left.")
    add.click()
    assertEquals(ui.submitted, Vector.empty)
    // The label costs 1 Supply, so 1 is enough.
    val affordable = as[dom.html.Button](
      all(render(choice(), supply = 1), ".recover-add").head)
    assert(!affordable.disabled)
    assert(!affordable.hasAttribute("aria-disabled"))

  test("Recover shows its dice and how close the score is to the target"):
    val outcome = WalkerRollOutcomeState("recover",
      Vector(DieFace.OneShield, DieFace.TwoShields), 3, Some(4),
      Vector.empty)
    val panel = render(choice(rollOutcome = Some(outcome)))
    assertEquals(all(panel, ".walker-roll-totals").map(_.textContent),
      Vector("Shields 3 · need 4"))
    assertEquals(all(panel, ".walker-roll-faces").map(_.getAttribute("aria-label")),
      Vector("Rolled shield, two shields -- 3 shields so far (need 4)."))
    assertEquals(all(panel, ".walker-roll-faces .die-faces").size, 1)

  test("with no dice rolled yet, Recover shows the target and no dice"):
    val outcome = WalkerRollOutcomeState("recover", Vector.empty, 0, Some(4))
    val panel = render(choice(rollOutcome = Some(outcome)))
    assertEquals(all(panel, ".walker-roll-faces").size, 0)
    assertEquals(all(panel, ".walker-roll-totals").map(_.textContent),
      Vector("Shields 0 · need 4"))

  /** An option that needs a granter's consent is asked for, never picked:
    * the engine refuses a pick of an option it does not offer. */
  test("an askable Recover option asks its granter before it sends"):
    val ask = ConsentAskState(Vector("red"), Vector("Relic Patrols"))
    val relics = DecisionQueryState.ChooseOne(Vector(
      DecisionOptionState("relic", "relic:crown", "Ancient Crown",
        state = "askable", reason = Some("Relic Patrols: needs consent"),
        consent = Some(ask))), heading = Some("Take a relic"))
    val surface: Surface.Recover = Surface.Recover(WalkerDecisionState("recover",
      ParkedDecision.recoverRelicDecisionId, query = Some(relics)),
      RecoverStep.Relic(relics))
    val ui = new RecordingControls()
    var opened = Vector.empty[CardInspection.Request]
    CardInspection.onOpen(request => opened :+= request)
    try
      val panel = dom.document.createElement("div")
      WalkerPanelSupport.renderRecoverPanel(surface,
        TestProjections.actingProjection(players = Vector(
          GamePlayer("red", "Red", "Citizen", PlayerColor.Red)),
          activePlayer = Some("red")), canControl = true, panel, ui)
      click(all(panel, ".recover-relic-choice").head)
      assertEquals(ui.submitted, Vector.empty)
      opened match
        case Vector(confirm: CardInspection.Request.Confirm) =>
          assertEquals(confirm.question,
            "Ask Red for consent to ignore Relic Patrols?")
          confirm.confirm()
        case other => fail(s"expected one confirmation, got $other")
      assertEquals(ui.submitted, Vector(Intent.RequestConsent(
        ParkedDecision.recoverRelicDecisionId,
        WalkerStartArgWire("relic", "relic:crown"))))
    finally CardInspection.clear()
