package distantlands.frontend

import distantlands.model.PlayerColor
import distantlands.protocol.projection.{PublicPreviewProjection,
  PublicPreviewSection}

import org.scalajs.dom

/** Cards waiting in the temporary hand are drawn in the action panel, so the
  * player who drew them can read them before anything asks them a question.
  */
class TemporaryHandPreviewSuite extends munit.FunSuite:
  private def panel(preview: Vector[CardDetails],
      publicPreview: Option[PublicPreviewProjection] = None,
      routed: ParkedDecision.Routed = ParkedDecision.Routed(None, None))
      : dom.Element =
    ActionDecisionRenderer.actionsPanel(
      GameProjection("game", 1L, "act", Some("red"),
        Vector(GamePlayer("red", "Red", "Exile", PlayerColor.Red)),
        Vector.empty, Vector.empty, ready = true,
        completed = false, temporaryHandPreview = preview,
        publicPreview = publicPreview),
      ServerUiSupport.ViewerPresentation(showGameplayControls = true, Vector.empty,
        Vector.empty, playerId = "red"),
      routed, canControl = true, SessionDrafts.empty,
      new RecordingControls())

  private val card = CardDetails("denizen:a", "denizen", "Hearth Guard",
    orientation = Some("face-up"))

  test("every card held in the hand is drawn"):
    val drawn = panel(Vector(card, card.copy(cardId = "denizen:b")))
      .querySelectorAll(".temporary-hand-preview .card-face").toVector
      .map(_.asInstanceOf[dom.Element].getAttribute("data-card-id"))
    assertEquals(drawn, Vector("denizen:a", "denizen:b"))

  test("an empty hand draws no section at all"):
    assertEquals(panel(Vector.empty)
      .querySelectorAll(".temporary-hand-preview").length, 0)

  test("the hand's cards and a public preview's cards split into even rows"):
    val shown = panel(Vector(card), Some(PublicPreviewProjection(Vector(
      PublicPreviewSection("Available edifices", Vector(card))))))
    val rows = shown.querySelectorAll(".temporary-hand-preview > .decision-cards")
      .toVector.map(_.asInstanceOf[dom.Element])
    assertEquals(rows.map(_.classList.contains("even-rows")),
      Vector(true, true))

  test("a public preview is drawn under its own title, whatever the hand"):
    val shown = panel(Vector.empty, Some(PublicPreviewProjection(Vector(
      PublicPreviewSection("Available edifices",
        Vector(card, card.copy(cardId = "edifice:b")))))))
    assertEquals(shown.querySelector(".public-preview > h3").textContent,
      "Available edifices")
    assertEquals(shown.querySelectorAll(".public-preview .card-face").toVector
      .map(_.asInstanceOf[dom.Element].getAttribute("data-card-id")),
      Vector("denizen:a", "edifice:b"))

  test("each section of a public preview is drawn as its own titled group"):
    val shown = panel(Vector.empty, Some(PublicPreviewProjection(Vector(
      PublicPreviewSection("Stack 1", Vector(card)),
      PublicPreviewSection("Stack 2", Vector(card.copy(cardId = "denizen:b"),
        card.copy(cardId = "denizen:c")))))))
    val groups = shown.querySelectorAll(".public-preview").toVector
      .map(_.asInstanceOf[dom.Element])
    assertEquals(groups.map(_.querySelector("h3").textContent),
      Vector("Stack 1", "Stack 2"))
    assertEquals(groups.map(_.querySelectorAll(".card-face").length),
      Vector(1, 2))

  test("a public preview with no cards draws no section"):
    assertEquals(panel(Vector.empty, Some(PublicPreviewProjection(Vector(
      PublicPreviewSection("None", Vector.empty))))).querySelectorAll(
      ".public-preview").length, 0)

  private val faith = CardDetails("vision:vision-of-faith", "vision",
    "Vision of Faith", orientation = Some("face-up"))

  /** Placing `faith`, with red's revealed Vision spot among the options. */
  private val placingFaith = ParkedDecision.Routed(Some(
    ParkedDecision.Surface.BoardDecision(WalkerDecisionState("search",
      "cardplay.place.vision", presentation = "board"),
      DecisionQueryState.ChooseOne(Vector(
        DecisionOptionState("revealed-vision", "red", "Vision"),
        DecisionOptionState("discard-pile", "cradle", "Discard"))))), None)

  /** A stand-in for the lit spot the player boards draw. */
  private def litSpot(): dom.Element =
    val spot = dom.document.createElement("span")
    spot.setAttribute("data-option-ref", "revealed-vision:red")
    dom.document.body.appendChild(spot)
    spot

  private var inspected = Vector.empty[String]

  override def beforeEach(context: BeforeEach): Unit =
    inspected = Vector.empty
    CardInspection.onOpen:
      case CardInspection.Request.Card(card, _) => inspected :+= card.name
      case _ => ()

  override def afterEach(context: AfterEach): Unit =
    CardInspection.clear()
    dom.document.body.innerHTML = ""

  private def handVision(shown: dom.Element): dom.html.Button =
    shown.querySelector(".temporary-hand-preview .card-face-vision")
      .asInstanceOf[dom.html.Button]

  test("a Vision in hand points at its lit spot instead of opening"):
    val spot = litSpot()
    val shown = panel(Vector(faith), routed = placingFaith)
    dom.document.body.appendChild(shown)
    handVision(shown).click()
    assert(spot.classList.contains("board-option-hover"))
    assertEquals(inspected, Vector.empty)

  test("hovering a Vision in hand lights its spot"):
    val spot = litSpot()
    val shown = panel(Vector(faith), routed = placingFaith)
    handVision(shown).dispatchEvent(new dom.Event("mouseenter"))
    assert(spot.classList.contains("board-option-hover"))
    handVision(shown).dispatchEvent(new dom.Event("mouseleave"))
    assert(!spot.classList.contains("board-option-hover"))

  test("a Vision in hand with a lit spot is read through its inspect button"):
    val shown = panel(Vector(faith), routed = placingFaith)
    val inspect = shown.querySelector(
      ".temporary-hand-preview .board-option-inspect")
      .asInstanceOf[dom.html.Button]
    assertEquals(inspect.getAttribute("aria-label"), "Inspect Vision of Faith")
    inspect.click()
    assertEquals(inspected, Vector("Vision of Faith"))

  test("a Vision in hand with no lit spot opens as any card does"):
    val shown = panel(Vector(faith))
    assertEquals(shown.querySelectorAll(".board-option-inspect").length, 0)
    handVision(shown).click()
    assertEquals(inspected, Vector("Vision of Faith"))
