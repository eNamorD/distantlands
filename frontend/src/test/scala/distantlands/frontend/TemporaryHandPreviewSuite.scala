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
      publicPreview: Option[PublicPreviewProjection] = None): dom.Element =
    ActionDecisionRenderer.actionsPanel(
      GameProjection("game", 1L, "act", Some("red"),
        Vector(GamePlayer("red", "Red", "Exile", PlayerColor.Red)),
        Vector.empty, Vector.empty, ready = true,
        completed = false, temporaryHandPreview = preview,
        publicPreview = publicPreview),
      ServerUiSupport.ViewerPresentation(showGameplayControls = true, Vector.empty,
        Vector.empty, playerId = "red"),
      ParkedDecision.Routed(None, None), canControl = true, SessionDrafts.empty,
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
