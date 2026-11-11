package distantlands.application

import distantlands.catalog.FoundationCards
import distantlands.model.{Chronicle, FoundationFace, FoundationNumber,
  FoundationState}
import distantlands.protocol.ChronicleFoundation
import distantlands.protocol.projection.FoundationProjection

/** The six foundation setup cards as a client reads them (Foundations
  * design, slice 1): each by the face in use, named from
  * [[FoundationCards]]. */
object FoundationPresentation:
  /** The face's wire spelling. */
  def faceKey(face: FoundationFace): String = face match
    case FoundationFace.Normal => "normal"
    case FoundationFace.Altered => "altered"

  /** A game's six cards, I to VI, each with the text of its face in use. */
  def ofGame(foundations: Map[FoundationNumber, FoundationState])
      : Vector[FoundationProjection] =
    FoundationNumber.all.flatMap(number => foundations.get(number).map {
      state =>
        val printed = FoundationCards.face(number, state.face)
        FoundationProjection(number.value, faceKey(state.face), printed.name,
          printed.text)
    })

  /** A Chronicle's six cards, I to VI. A number the Chronicle does not
    * list lies normal, as game start reads it. */
  def ofChronicle(chronicle: Chronicle): Vector[ChronicleFoundation] =
    FoundationNumber.all.map { number =>
      val face = chronicle.foundations.get(number)
        .fold(FoundationFace.Normal)(_.face)
      ChronicleFoundation(number.value, faceKey(face),
        FoundationCards.face(number, face).name)
    }
