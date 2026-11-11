package distantlands.gameplay.setup

import distantlands.catalog.FoundationCards
import distantlands.model._

/** The foundation faces a game starts with (Foundations design, slice 1). */
object StartingFoundations:
  /** The Chronicle's faces with their alteration sources. A number the
    * Chronicle does not list lies normal, with no source. */
  def of(chronicle: Chronicle): Map[FoundationNumber, FoundationState] =
    FoundationNumber.all.map(number => number ->
      chronicle.foundations.getOrElse(number,
        FoundationState(FoundationFace.Normal, Set.empty))).toMap

  /** The slice of the Foundations design each altered face arrives with.
    * The slice that makes a face work deletes its line. Foundation III has
    * none: both its faces wait for Legacies and do nothing (ruling 1). */
  private val arrivesWith: Vector[(FoundationNumber, Int)] = Vector(
    FoundationNumber.I -> 3,
    FoundationNumber.V -> 5,
    FoundationNumber.VI -> 6)

  /** Why game start refuses `faces`, if it does: the first altered face, in
    * foundation order, that no slice has brought in yet. */
  def refusal(faces: Map[FoundationNumber, FoundationState]): Option[String] =
    arrivesWith.collectFirst:
      case (number, slice) if faces.get(number)
          .exists(_.face == FoundationFace.Altered) =>
        val name = FoundationCards.face(number, FoundationFace.Altered).name
        s"$name (altered Foundation $number) is not supported yet: it " +
          s"arrives with Foundations slice $slice"
