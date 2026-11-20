package distantlands.frontend

import distantlands.frontend.ServerUiSupport.{button, element, text}
import org.scalajs.dom

/** The requests block of the consent column (reconsider design,
  * "Frontend", "Requests list"): under the preference rows, the turn's
  * requests about the power as the viewer answered them, newest first.
  * Each entry says what was asked and how it stands, and carries one
  * button that changes the answer. A button that cannot change it is
  * withheld, with the reason in its hover note. The block is the one part
  * of the overlay redrawn from each projection. */
private[frontend] object ConsentRequestsView:
  /** The block for `requests`: hidden and empty while there are none. */
  def mount(requests: ConsentRequests): dom.Element =
    val block = element("div", "consent-requests")
    block.setAttribute("data-power-id", requests.powerId)
    fill(block, requests)
    block

  /** `block` redrawn from `requests`, keeping the reader's place: the
    * scroll of `scroller`, and keyboard focus on the same entry's button,
    * whatever it now reads. When that entry is gone, focus goes to
    * `fallback`. It follows `PanelContent.replace`, which finds a rebuilt
    * control by its text and so would lose a button that turns from
    * "Allow" to "Refuse". */
  def redraw(block: dom.Element, requests: ConsentRequests,
      scroller: dom.Element, fallback: dom.html.Element): Unit =
    val focused = Option(dom.document.activeElement).filter(block.contains)
      .flatMap(node => Option(node.closest("[data-request-key]")))
      .map(_.getAttribute("data-request-key"))
    val (left, top) = (scroller.scrollLeft, scroller.scrollTop)
    fill(block, requests)
    focused.foreach { key =>
      val entries = block.querySelectorAll("[data-request-key]")
      val twin = (0 until entries.length)
        .map(entries(_).asInstanceOf[dom.Element])
        .find(_.getAttribute("data-request-key") == key)
        .flatMap(entry => Option(entry.querySelector(
          ".consent-request-change")))
      twin.fold(fallback)(_.asInstanceOf[dom.html.Element]).focus()
    }
    scroller.scrollLeft = left
    scroller.scrollTop = top

  private def fill(block: dom.Element, requests: ConsentRequests): Unit =
    while block.firstChild != null do block.removeChild(block.firstChild)
    if requests.entries.isEmpty then block.setAttribute("hidden", "")
    else
      block.removeAttribute("hidden")
      block.appendChild(text("h4", "consent-grid-heading",
        "Requests this turn"))
      val list = element("div", "consent-request-list")
      requests.entries.foreach(entry =>
        list.appendChild(item(entry, requests.change)))
      block.appendChild(list)

  private def key(row: ConsentRequestRowState): String =
    Vector(row.requesterPlayerId, row.decisionId, row.optionKind,
      row.optionId).mkString("|")

  private def item(entry: ConsentRequestEntry,
      change: Option[(ConsentRequestEntry, Boolean) => Unit]): dom.Element =
    val row = entry.row
    val allowed = row.answer == "allowed"
    // "Blue asked to select Deep Woods"; for an automatic power, "Blue's
    // Tyrant: deactivated" or "Blue's Tyrant: let work" (R18).
    val asked = row.use.fold(
      s"${entry.requester} asked to select ${row.label}")(card =>
      s"${entry.requester}'s $card: " +
        (if allowed then "let work" else "deactivated"))
    val item = element("div", "consent-request")
    // A group named by its first line, so one "Allow" is told from
    // another (implementation ruling R22).
    item.setAttribute("role", "group")
    item.setAttribute("aria-label", asked)
    item.setAttribute("data-request-key", key(row))
    item.setAttribute("data-change", row.change)
    item.appendChild(text("p", "consent-request-asked", asked))
    if row.alsoPowers.nonEmpty then
      item.appendChild(text("p", "consent-request-also",
        s"Also answers ${ConsentAsking.listed(row.alsoPowers)}"))
    val answer = element("div", "consent-request-answer")
    // An automatic power's first line already says how it stands.
    if row.use.isEmpty then
      answer.appendChild(text("span", "consent-request-state",
        if !allowed then "Refused"
        else if row.change == "used" then "Allowed, used" else "Allowed"))
    val other = button(row.use.fold(if allowed then "Refuse" else "Allow")(
      _ => if allowed then "Deactivate" else "Let it work"),
      "consent-request-change")
    row.change match
      case "open" => change match
        case Some(send) => other.onclick = _ =>
          // One command a click: the button waits for the next redraw
          // (implementation ruling R20). It waits as `aria-disabled`, not
          // `disabled`: a browser takes focus from a disabled button, and
          // the redraw finds the reader's place by the focused entry.
          other.setAttribute("aria-disabled", "true")
          other.onclick = null
          send(entry, !allowed)
        case None => other.disabled = true
      case "used" => HoverTip.withhold(other, s"${entry.requester} has " +
        s"acted on this consent. It unlocks if ${entry.requester} undoes " +
        "that.")
      case "waiting" => HoverTip.withhold(other,
        "A request about this option is waiting for an answer.")
      case "lapsed" => HoverTip.withhold(other,
        "You no longer rule every power this answer covers.")
      // An automatic power's answer, and any value a newer server sends.
      case _ => HoverTip.withhold(other, "This answer cannot be changed.")
    answer.appendChild(other)
    item.appendChild(answer)
    item
