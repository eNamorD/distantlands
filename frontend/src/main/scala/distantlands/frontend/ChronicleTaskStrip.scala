package distantlands.frontend

import distantlands.protocol.projection.ChronicleTaskPresentation
import org.scalajs.dom

/** The Chronicle Phase aids: the five Chronicle task cards as a setup card
  * strip, headed "Chronicle Phase" (Chronicle Details design, slice 2).
  * A task card has no numeral and no tag; a click reads its lines, a
  * two-sided card's front, a "Back" line and its back. */
private[frontend] object ChronicleTaskStrip:
  /** "Chronicle task · The Sun": the inspector's title and the hover note
    * (implementation ruling R8). */
  def title(task: ChronicleTaskPresentation): String =
    s"Chronicle task · ${task.title}"

  val cards: Vector[SetupCardStrip.Card] =
    ChronicleTaskPresentation.all.map(task => SetupCardStrip.Card(None,
      task.title, None, title(task), s"Chronicle task, ${task.title}",
      task.lines, None))

  def render(): dom.Element =
    SetupCardStrip.render("Chronicle Phase", "chronicle-task", cards)
