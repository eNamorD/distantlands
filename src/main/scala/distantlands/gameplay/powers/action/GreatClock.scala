package distantlands.gameplay.powers.action

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.powers.setup.{GreatClockCard, RoundMarker}
import distantlands.model._

/** Great Clock (E12, intact), ACTION: advance the round marker once, if
  * possible. "Do not roll the round die. This cannot end the game."
  * (catalog batch 10, slice 2).
  *
  * The advance is [[RoundMarker]]'s, which takes the Usurper Limiter off
  * past round 3 (ruling 1). On round 8 the option shows unavailable with
  * [[LastSpace]]. The end die is rolled only at a round's end, from the
  * round then, so a jump past rounds 5 to 7 rolls none. Its line: "Advanced
  * the round marker to round 4."
  */
case object GreatClock extends PaidAction(GreatClockCard.intact.power):
  val LastSpace: String = "The round marker is on its last space."

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Advanced the round marker to round {4}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Advanced the round marker to round "), NotePart.Arg(0),
    NotePart.Text(".")))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(RoundMarker.advance(ready).isEmpty)(LastSpace)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      BuildOps((live, _) => Right(RoundMarker.advance(live))),
      Note(id, states => RoundMarker.reached(states).map(round =>
        used(PowerSourceRef.Card(GreatClockCard.id),
          NoteArg.Number(round)))))))
