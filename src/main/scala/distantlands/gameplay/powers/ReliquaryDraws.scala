package distantlands.gameplay.powers

import distantlands.model._

/** A random relic from the Reliquary, taken facedown (Bewitch, Ancient
  * Pact, Royal Ambitions). The draw shuffles the Reliquary, so the walker
  * journals its order, then takes the first relic. An empty Reliquary takes
  * nothing, and its line says so (ruling R22).
  */
object ReliquaryDraws:
  /** The keys this object owns, which no one power owns. A power that
    * writes one includes this set. */
  val notes: NoteSet = NoteSet.unowned("ReliquaryDraws")
  /** "The Reliquary was empty." */
  val empty: NoteKey = notes.key("reliquary-empty",
    Vector(NotePart.Text("The Reliquary was empty.")))

  /** The draw for `player`, written under `power` from `source`. The steps
    * read the Reliquary when they run, not when the tree is built. */
  def steps(power: PowerId, player: PlayerId, source: PowerSourceRef)
      : Vector[Operation] = Vector(Branch((live, _) =>
    if live.game.campaign.reliquary.isEmpty then
      Vector(Note(power, _ => Some(empty(source))))
    else Vector(
      Shuffle(ShufflePile.Reliquary),
      BuildOps((ready, _) => Right(ready.game.campaign.reliquary.headOption
        .toVector.map(relic => Move(Piece.Card(relic),
          PositionedLocation(Location.Reliquary),
          PositionedLocation(Location.PlayArea(player)),
          resultingOrientation = Some(Orientation.FaceDown)))),
        required = true))))
