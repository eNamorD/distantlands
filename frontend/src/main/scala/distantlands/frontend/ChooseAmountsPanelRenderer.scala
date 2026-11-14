package distantlands.frontend

import org.scalajs.dom

/** The ChooseAmounts panel (Legacy Setup powers ruling 15; R25): a row per
  * slot with its card, `−` and `+` steppers around a number field, and the
  * slot's maximum; a line for each amount two slots share; and Confirm,
  * disabled while any is shared. The field stages on `change`, so typing
  * never redraws the panel, and the draft lives in `SessionDrafts`, so a
  * redraw keeps a typed value. */
private[frontend] object ChooseAmountsPanelRenderer:
  import ServerUiSupport.{button, element, text}

  val BoundTooltip = "Shift+click: to the end"

  def render(surface: ParkedDecision.Surface.ChooseAmounts,
      amounts: Option[WalkerChooseAmountsDraft], canControl: Boolean,
      panel: dom.Element, controls: TableControls): Unit =
    val decision = surface.decision
    val query = surface.query
    panel.appendChild(text("h2", "", WalkerPanelSupport.decisionHeading(query)))
    WalkerPanelSupport.statusFacts(decision.status, panel)
    amounts.filter(_.decisionId == decision.decisionId).foreach { draft =>
      val rows = element("div", "amounts-slots")
      query.slots.foreach(slot =>
        rows.appendChild(row(slot, draft, canControl, controls)))
      panel.appendChild(rows)
      draft.state.clashes.foreach(position =>
        panel.appendChild(text("p", "amounts-clash",
          s"Two cards share position $position")))
      val confirm = button(query.confirmLabel, "amounts-confirm")
      confirm.disabled = !canControl || !draft.canConfirm
      confirm.onclick = _ => draft.command.foreach(controls.submitCommand)
      panel.appendChild(confirm)
    }

  private def row(slot: DecisionSlotState, draft: WalkerChooseAmountsDraft,
      canControl: Boolean, controls: TableControls): dom.Element =
    val item = WalkerPartitionDraft.itemId(slot.option)
    val node = element("div", "amounts-slot")
    node.setAttribute("data-option-id", item)
    node.appendChild(slot.option.card.fold[dom.Element](
      text("span", "amounts-label", slot.option.label))(CardFace.render))
    node.appendChild(stepper("−", s"Earlier ${slot.option.label}",
      "amounts-decrement", canControl, controls,
      shift => if shift then draft.drain(item) else draft.decrement(item)))
    node.appendChild(field(item, slot.option.label, draft, canControl,
      controls))
    node.appendChild(stepper("+", s"Later ${slot.option.label}",
      "amounts-increment", canControl, controls,
      shift => if shift then draft.fill(item) else draft.increment(item)))
    node.appendChild(text("span", "amounts-maximum", s"of ${slot.maximum}"))
    node

  /** The amount as an editable number: a typed integer is clamped to the
    * slot's bounds, written back and staged; other text is undone. */
  private def field(item: String, label: String,
      draft: WalkerChooseAmountsDraft, canControl: Boolean,
      controls: TableControls): dom.Element =
    val input = dom.document.createElement("input")
      .asInstanceOf[dom.html.Input]
    input.className = "amounts-field"
    input.`type` = "text"
    input.value = draft.state.amount(item).toString
    input.disabled = !canControl
    input.setAttribute("inputmode", "numeric")
    input.setAttribute("autocomplete", "off")
    input.setAttribute("aria-label", s"Position of $label")
    // The field always shows what Confirm sends: a typed amount is clamped
    // and written back before it is staged, since a redraw keeps the
    // focused field's text; text that is no number puts the amount back.
    input.onchange = _ => input.value.trim.toIntOption match
      case Some(value) =>
        val next = draft.set(item, value)
        input.value = next.state.amount(item).toString
        controls.stage(Draft.ChooseAmounts(next))
      case None => input.value = draft.state.amount(item).toString
    input

  private def stepper(label: String, name: String, className: String,
      canControl: Boolean, controls: TableControls,
      next: Boolean => WalkerChooseAmountsDraft): dom.Element =
    val control = button(label, className)
    control.setAttribute("aria-label", name)
    HoverTip.set(control, BoundTooltip)
    control.disabled = !canControl
    control.onclick = (event: dom.MouseEvent) =>
      controls.stage(Draft.ChooseAmounts(next(event.shiftKey)))
    control
