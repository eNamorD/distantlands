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

  /** `faces` after Setup With No Empire's "Foundations Erode": Foundation I
    * lies on Wide Horizons and Foundation V on Mob's Favor. A face that
    * turns has no alteration source; a face that already lies there is
    * kept, sources and all. */
  def eroded(faces: Map[FoundationNumber, FoundationState])
      : Map[FoundationNumber, FoundationState] =
    Vector(FoundationNumber.I -> FoundationFace.Altered,
      FoundationNumber.V -> FoundationFace.Normal).foldLeft(faces) {
      case (turned, (number, face)) =>
        if turned.get(number).exists(_.face == face) then turned
        else turned.updated(number, FoundationState(face, Set.empty))
    }

  /** The slice of the Foundations design each altered face arrives with.
    * The slice that makes a face work deletes its line. Foundation III has
    * none: both its faces wait for Legacies and do nothing (ruling 1). */
  private val arrivesWith: Vector[(FoundationNumber, Int)] = Vector(
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
