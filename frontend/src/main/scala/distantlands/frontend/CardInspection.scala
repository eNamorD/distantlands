package distantlands.frontend

import distantlands.protocol.art.ArtKey
import org.scalajs.dom

/** One slot connecting every rendered card to whatever is showing overlays.
  *
  * Cards reach `CardFace.render` through six renderer objects, only some of
  * which carry the session's controls. Threading a callback through the rest
  * for a single handler is more change than the feature is worth, and
  * `GameTableShell` already owns the other document-level chrome.
  */
private[frontend] object CardInspection:
  /** What the slot is asked to show. Sealed rather than two slots: the shell
    * wires exactly one handler, and a second slot could be left unwired.
    */
  sealed trait Request extends Product with Serializable:
    def origin: dom.html.Element
  object Request:
    final case class Card(card: CardDetails, origin: dom.html.Element)
        extends Request
    /** `banner`, here and on a Festival and a Power, is the banner the
      * words are about: the overlay shows its face before them (game art
      * ruling R43).
      * `art` is that card's image, when the catalog has one.
      * `plate` names the printed card behind the words, when there is one:
      * `foundation`, `oath-goal` or `oathkeeper-title`. */
    final case class Text(title: String, lines: Vector[String],
        origin: dom.html.Element, plate: Option[String] = None,
        art: Option[ArtKey] = None, banner: Option[BannerState] = None)
        extends Request
    /** The Darkest Secret on its Festival face: its printed lines, then a
      * copy of the festival marker and the site it lies at, `None` while
      * it lies beside the map, and the viewer's consent preferences for
      * the Festival when they hold it. */
    final case class Festival(title: String, lines: Vector[String],
        at: Option[String], grid: Option[ConsentGrid],
        origin: dom.html.Element, banner: Option[BannerState] = None)
        extends Request
    /** The festival marker as printed: its title and its text on the
      * plate, and the holder's consent preferences, as the banner shows
      * them. */
    final case class FestivalMarker(grid: Option[ConsentGrid],
        origin: dom.html.Element) extends Request
    /** A board's Supply: the bank, the Supply track, then the Rest. */
    final case class Supply(panel: SupplyAtRest.Panel,
        origin: dom.html.Element) extends Request
    /** Cards in order, as a log line's card list opens them (N5). */
    final case class Cards(title: String, cards: Vector[CardDetails],
        origin: dom.html.Element) extends Request
    /** A yes-or-no question whose Yes runs `confirm` (consent design,
      * "Frontend"). */
    final case class Confirm(question: String, confirm: () => Unit,
        origin: dom.html.Element) extends Request
    /** A question with one button per choice, then Cancel (catalog batch
      * 8, slice 15). */
    final case class Choices(question: String,
        choices: Vector[(String, () => Unit)], origin: dom.html.Element)
        extends Request
    /** A power's printed text, with the viewer's consent preferences for it
      * when they rule it (consent design, user story step 7). */
    final case class Power(title: String, lines: Vector[String],
        grid: Option[ConsentGrid], origin: dom.html.Element,
        banner: Option[BannerState] = None) extends Request
    /** A card that prints a power the viewer rules, with their consent
      * preferences for it (consent slice 3). */
    final case class RuledCard(card: CardDetails, grid: ConsentGrid,
        origin: dom.html.Element) extends Request

  private var handler = Option.empty[Request => Unit]

  def onOpen(value: Request => Unit): Unit = handler = Some(value)

  def clear(): Unit = handler = None

  def open(card: CardDetails, origin: dom.html.Element): Unit =
    handler.foreach(_(Request.Card(card, origin)))

  def openText(title: String, lines: Vector[String],
      origin: dom.html.Element, plate: Option[String] = None,
      art: Option[ArtKey] = None, banner: Option[BannerState] = None)
      : Unit =
    handler.foreach(_(Request.Text(title, lines, origin, plate, art,
      banner)))

  def openFestival(title: String, lines: Vector[String], at: Option[String],
      grid: Option[ConsentGrid], origin: dom.html.Element,
      banner: Option[BannerState] = None): Unit =
    handler.foreach(_(Request.Festival(title, lines, at, grid, origin,
      banner)))

  def openFestivalMarker(grid: Option[ConsentGrid],
      origin: dom.html.Element): Unit =
    handler.foreach(_(Request.FestivalMarker(grid, origin)))

  def openSupply(panel: SupplyAtRest.Panel, origin: dom.html.Element): Unit =
    handler.foreach(_(Request.Supply(panel, origin)))

  def openCards(title: String, cards: Vector[CardDetails],
      origin: dom.html.Element): Unit =
    handler.foreach(_(Request.Cards(title, cards, origin)))

  def openConfirm(question: String, confirm: () => Unit,
      origin: dom.html.Element): Unit =
    handler.foreach(_(Request.Confirm(question, confirm, origin)))

  def openChoices(question: String, choices: Vector[(String, () => Unit)],
      origin: dom.html.Element): Unit =
    handler.foreach(_(Request.Choices(question, choices, origin)))

  def openPower(title: String, lines: Vector[String],
      grid: Option[ConsentGrid], origin: dom.html.Element,
      banner: Option[BannerState] = None): Unit =
    handler.foreach(_(Request.Power(title, lines, grid, origin, banner)))

  def openRuled(card: CardDetails, grid: ConsentGrid,
      origin: dom.html.Element): Unit =
    handler.foreach(_(Request.RuledCard(card, grid, origin)))
