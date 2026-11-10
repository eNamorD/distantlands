package distantlands.frontend

import distantlands.protocol.projection.PlanAidOfferProjection
import org.scalajs.dom
import ServerUiSupport.{button, element, text}

/** An Imperial ally's offer panel while another Imperial player's plan
  * window is parked (Empire phase design, "Offer panel"). Plans and
  * requirements are toggled on the board; the panel lists them, with the
  * four amounts and the button that saves the whole draft.
  *
  * A pending addition has no mark, a saved item still drafted has a check,
  * a pending removal is struck through, and a used item is locked. */
private[frontend] object PlanAidPanel:
  /** "You may offer your battle plans, to pay costs and to meet
    * requirements for Blue". */
  def heading(value: GameProjection, prompt: PlanAidPromptState): String =
    "You may offer your battle plans, to pay costs and to meet " +
      s"requirements for ${ConsentAsking.name(value, prompt.userPlayerId)}"

  /** "Offer", "Save changes" or "Offered", and whether it may be pressed. */
  def action(prompt: PlanAidPromptState, draft: PlanAidDraft)
      : (String, Boolean) =
    if prompt.saved == PlanAidOfferProjection() then
      ("Offer", !draft.matchesSaved(prompt))
    else if draft.matchesSaved(prompt) then ("Offered", false)
    else ("Save changes", true)

  def render(value: GameProjection, canControl: Boolean, panel: dom.Element,
      drafts: SessionDrafts, controls: TableControls): Unit =
    for
      prompt <- value.planAidPrompt
      draft <- drafts.aid
    do
      val section = element("section", "plan-aid-prompt")
      section.appendChild(text("p", "plan-aid-heading", heading(value, prompt)))
      def toggle(key: String): Unit =
        controls.stage(Draft.Aid(Some(draft.toggled(prompt, key))))
      section.appendChild(list("Plans", prompt.plans, draft.plans,
        prompt.saved.plans, prompt.used.plans, canControl, toggle))
      section.appendChild(list("Requirements", prompt.requirements,
        draft.requirements, prompt.saved.requirements,
        prompt.used.requirements, canControl, toggle))
      section.appendChild(amounts(prompt, draft, canControl, controls))
      val (label, enabled) = action(prompt, draft)
      val save = button(label, "plan-aid-save")
      save.disabled = !canControl || !enabled
      save.onclick = _ => controls.submitCommand(draft.command)
      section.appendChild(save)
      panel.appendChild(section)

  /** The drafted and saved items, in the prompt's order. A button option,
    * the title's defense, has nothing on the board to click, so it is
    * listed always, with its own toggle. */
  private def list(title: String, options: Vector[DecisionOptionState],
      drafted: Vector[String], saved: Vector[String], used: Vector[String],
      canControl: Boolean, toggle: String => Unit): dom.Element =
    val block = element("div", "plan-aid-list")
    block.appendChild(text("h3", "plan-aid-list-title", title))
    val rows = element("ul", "plan-aid-items")
    options.filter(option => option.kind == "button" ||
      drafted.contains(BoardOptionViews.key(option)) ||
      saved.contains(BoardOptionViews.key(option))).foreach { option =>
      val key = BoardOptionViews.key(option)
      val listed = drafted.contains(key) || saved.contains(key)
      val (state, mark) =
        if used.contains(key) then ("plan-aid-used", Some("🔒 used"))
        else if !listed then ("plan-aid-offerable", None)
        else if !drafted.contains(key) then ("plan-aid-removed", None)
        else if saved.contains(key) then ("plan-aid-saved", Some("✓"))
        else ("plan-aid-pending", None)
      val row = element("li", s"plan-aid-item $state")
      row.appendChild(text("span", "plan-aid-name", option.label))
      mark.foreach(word => row.appendChild(text("span", "plan-aid-mark",
        word)))
      if option.kind == "button" && !used.contains(key) then
        val chosen = drafted.contains(key)
        val flip = button(if chosen then "Take out" else "Offer it",
          "plan-aid-toggle")
        flip.setAttribute("aria-pressed", chosen.toString)
        flip.disabled = !canControl
        flip.onclick = _ => toggle(key)
        row.appendChild(flip)
      rows.appendChild(row)
    }
    if rows.childNodes.length == 0 then
      block.appendChild(text("p", "plan-aid-empty",
        "Choose on the board what to offer."))
    else block.appendChild(rows)
    block

  /** The four amounts, each between what was used and what the ally can
    * still give. */
  private def amounts(prompt: PlanAidPromptState, draft: PlanAidDraft,
      canControl: Boolean, controls: TableControls): dom.Element =
    val block = element("div", "plan-aid-amounts")
    block.appendChild(text("h3", "plan-aid-list-title", "Amounts"))
    val saved = prompt.saved
    val used = prompt.used
    def left(pay: Int, payUsed: Int, burn: Int, burnUsed: Int) =
      (pay - payUsed) + (burn - burnUsed)
    val favorRoom = prompt.favorHeld - left(draft.favor, used.favor,
      draft.favorBurnt, used.favorBurnt)
    val secretRoom = prompt.secretsHeld - left(draft.secrets, used.secrets,
      draft.secretsBurnt, used.secretsBurnt)
    Vector(
      ("Favor to pay", draft.favor, saved.favor, used.favor, favorRoom,
        (n: Int) => draft.copy(favor = n)),
      ("Secrets to pay", draft.secrets, saved.secrets, used.secrets,
        secretRoom, (n: Int) => draft.copy(secrets = n)),
      ("Favor to burn", draft.favorBurnt, saved.favorBurnt, used.favorBurnt,
        favorRoom, (n: Int) => draft.copy(favorBurnt = n)),
      ("Secrets to burn", draft.secretsBurnt, saved.secretsBurnt,
        used.secretsBurnt, secretRoom, (n: Int) => draft.copy(secretsBurnt = n)))
      .foreach { (name, amount, offered, spent, room, set) =>
        val row = element("div", "plan-aid-amount")
        row.appendChild(text("span", "plan-aid-amount-name", name))
        if offered > 0 then
          row.appendChild(text("span", "plan-aid-tag", s"$offered offered"))
        if spent > 0 then
          row.appendChild(text("span", "plan-aid-tag", s"$spent used"))
        val less = button("−", "plan-aid-step")
        less.setAttribute("aria-label", s"Less $name")
        less.disabled = !canControl || amount <= spent
        less.onclick = _ => controls.stage(Draft.Aid(Some(set(amount - 1))))
        val more = button("+", "plan-aid-step")
        more.setAttribute("aria-label", s"More $name")
        more.disabled = !canControl || room <= 0
        more.onclick = _ => controls.stage(Draft.Aid(Some(set(amount + 1))))
        row.appendChild(less)
        row.appendChild(text("span", "plan-aid-amount-value", amount.toString))
        row.appendChild(more)
        block.appendChild(row)
      }
    block
