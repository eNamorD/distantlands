package distantlands.frontend

import distantlands.protocol.{DecisionAnswerWire, GameIntent => GameCommand}
import org.scalajs.dom
import ServerUiSupport.{button, text}

/** The Gathering's open question (catalog batch 12, slice 6b): who is at
  * the site and who has passed, then, for a player there, a box for each
  * player they may name, Negotiate, and Pass or Take back pass. Negotiate
  * waits until a box is checked. A viewer not at the site reads the
  * names only.
  */
private[frontend] object GatheringPanel:
  def render(surface: ParkedDecision.Surface.Gathering, playerId: String,
      canControl: Boolean, panel: dom.Element, controls: TableControls): Unit =
    val query = surface.query
    val decisionId = surface.decision.decisionId
    panel.appendChild(text("h2", "", query.heading.getOrElse("The Gathering")))
    panel.appendChild(text("p", "gathering-present",
      query.presentPlayerIds.map(id =>
        if query.passedPlayerIds.contains(id) then s"$id (passed)" else id)
        .mkString(" · ")))
    if query.presentPlayerIds.contains(playerId) then
      val boxes = query.reachablePlayerIds.map(id =>
        id -> nameBox(id, canControl, panel))
      val negotiate = button("Negotiate", "gathering-open")
      def refresh(): Unit =
        negotiate.disabled = !canControl || !boxes.exists(_._2.checked)
      boxes.foreach((_, check) => check.onchange = _ => refresh())
      refresh()
      negotiate.onclick = _ => controls.submitCommand(GameCommand.ResolveWalker(
        decisionId, DecisionAnswerWire.OpenDealWire(boxes.collect {
          case (id, check) if check.checked => id })))
      panel.appendChild(negotiate)
      val passed = query.passedPlayerIds.contains(playerId)
      val toggle =
        if passed then button("Take back pass", "gathering-unpass")
        else button("Pass", "gathering-pass")
      toggle.disabled = !canControl
      toggle.onclick = _ => controls.submitCommand(GameCommand.ResolveWalker(
        decisionId,
        if passed then DecisionAnswerWire.UnpassGatheringWire
        else DecisionAnswerWire.PassGatheringWire))
      panel.appendChild(toggle)

  /** One player the viewer may name: the box and the name, in one
    * `label`, named by its `aria-label`. */
  private def nameBox(id: String, canControl: Boolean,
      panel: dom.Element): dom.html.Input =
    val check = dom.document.createElement("input").asInstanceOf[dom.html.Input]
    check.`type` = "checkbox"
    check.setAttribute("aria-label", s"Negotiate with $id")
    check.disabled = !canControl
    val row = dom.document.createElement("label").asInstanceOf[dom.html.Label]
    row.className = "gathering-item"
    row.appendChild(check)
    row.appendChild(text("span", "gathering-item-label", id))
    panel.appendChild(row)
    check
