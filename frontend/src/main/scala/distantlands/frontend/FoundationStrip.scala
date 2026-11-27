package distantlands.frontend

import distantlands.protocol.projection.FoundationProjection
import distantlands.protocol.art.ArtCatalog
import org.scalajs.dom

/** The six foundation setup cards as a read-only strip (Foundations design,
  * slice 1), drawn by `SetupCardStrip` (Chronicle Details design, slice 2):
  * one row a card, its numeral, the name of the face in use and, on an
  * altered card, the word. A click on a card reads its face's rules in the
  * overlay.
  */
private[frontend] object FoundationStrip:
  private val Numerals = Vector("I", "II", "III", "IV", "V", "VI")

  /** "IV" for 4; a number no card prints reads as its digits. */
  def numeral(number: Int): String =
    Numerals.lift(number - 1).getOrElse(number.toString)

  def ofGame(foundations: Vector[FoundationProjection])
      : Vector[SetupCardStrip.Card] =
    foundations.map { foundation =>
      val altered = foundation.face == "altered"
      SetupCardStrip.Card(Some(numeral(foundation.number)), foundation.name,
        Option.when(altered)("Altered"),
        title(foundation.number, foundation.name),
        label(foundation.number, foundation.name, altered),
        Vector(foundation.rulesText), Some("foundation"),
        ArtCatalog.foundation(foundation.number, foundation.name))
    }

  /** "Foundation I · Wide Horizons": the overlay's title and the hover
    * note. */
  def title(number: Int, name: String): String =
    s"Foundation ${numeral(number)} · $name"

  /** "Foundation I, Wide Horizons, altered": a table card's label. */
  def label(number: Int, name: String, altered: Boolean): String =
    s"Foundation ${numeral(number)}, $name" +
      (if altered then ", altered" else "")

  def render(cards: Vector[SetupCardStrip.Card]): dom.Element =
    SetupCardStrip.render("Foundations", "foundation", cards)
