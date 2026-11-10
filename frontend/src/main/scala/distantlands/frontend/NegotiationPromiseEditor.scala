package distantlands.frontend

import org.scalajs.dom

/** The editor of one author's promises of one kind (catalog batch 12,
  * slice 1): a one-line text field per promise, with its character count
  * and a Remove button, and an Add button while fewer than `MaxPerKind`
  * stand. `values` leaves blank fields out, trims the rest and sends a
  * repeated promise once; the server refuses what breaks the other limits.
  * The count is of characters (code points), as the server counts; the
  * field's `maxLength` counts UTF-16 units, so it can only stop short of the
  * server's limit, never past it. The text is only ever a field's value,
  * never markup.
  */
private[frontend] final class NegotiationPromiseEditor(heading: String,
    noun: String, initial: Vector[String], panel: dom.Element,
    sectionClass: String):
  import NegotiationPromiseEditor.{MaxLength, MaxPerKind}
  import ServerUiSupport.{button, text}

  private val section = text("div", sectionClass, "")
  private val list = text("div", "negotiation-promise-list", "")
  /** Each promise's field and its Remove button, in row order. */
  private val rows = scala.collection.mutable.ArrayBuffer.empty[
    (dom.html.Input, dom.html.Button)]
  private val adder = button(s"Add $noun", "negotiation-add-promise")

  section.appendChild(text("h3", "", heading))
  section.appendChild(list)
  section.appendChild(adder)
  panel.appendChild(section)
  adder.onclick = _ => add("")
  initial.foreach(add)
  refresh()

  def values: Vector[String] =
    rows.toVector.map(_._1.value.strip).filter(_.nonEmpty).distinct

  private def add(value: String): Unit =
    val row = text("div", "negotiation-item negotiation-promise-item", "")
    val field =
      dom.document.createElement("input").asInstanceOf[dom.html.Input]
    field.`type` = "text"
    field.maxLength = MaxLength
    field.value = value
    val count = text("span", "negotiation-promise-count", "")
    val counted = () => count.textContent =
      s"${field.value.codePointCount(0, field.value.length)}/$MaxLength"
    field.oninput = _ => counted()
    counted()
    val remove = button("Remove", "negotiation-remove-promise")
    remove.onclick = _ =>
      rows -= field -> remove
      list.removeChild(row)
      refresh()
    row.appendChild(field)
    row.appendChild(count)
    row.appendChild(remove)
    list.appendChild(row)
    rows += field -> remove
    refresh()

  /** Numbers the fields' and Remove buttons' labels and closes Add at the
    * limit. */
  private def refresh(): Unit =
    adder.disabled = rows.size >= MaxPerKind
    rows.zipWithIndex.foreach { case ((field, remove), index) =>
      field.setAttribute("aria-label", s"$noun ${index + 1}")
      remove.setAttribute("aria-label", s"Remove $noun ${index + 1}")
    }

private[frontend] object NegotiationPromiseEditor:
  /** The server's limits (`NegotiationPromise` in the model). */
  val MaxLength: Int = 160
  val MaxPerKind: Int = 5
