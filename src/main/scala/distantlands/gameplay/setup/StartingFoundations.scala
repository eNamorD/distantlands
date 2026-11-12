package distantlands.gameplay.setup

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
