package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** A relic a Throne power keeps for the next game: Heirloom's (Legacy
  * Chronicle powers design, ruling 13; R18-R20) and Hiding Place's (Last
  * Chronicle powers design, ruling 4). Any relic its holder has but the
  * Grand Scepter may be kept. Its secrets return to the holder facedown
  * and any warbands on it go back to their supply, as Stock Reliquary
  * returns them; then `StartingRelicKept` keeps it. Lines, naming the
  * card that keeps it, one line each viewer reads by what they know:
  * "p2 kept Cup of Plenty with Heirloom" for the holder, and "p2 kept a
  * Relic with Heirloom" for everyone else. */
object RelicKeep:
  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val kept: NoteKey = notes.key("relic-kept", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  /** The line a keep writes, on the Throne's note id. */
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Every relic `holder` holds but the Grand Scepter. */
  def keepable(state: ReadyGame, holder: PlayerId): Vector[RelicState] =
    state.game.current.player(holder).toVector.flatMap(_.relics)
      .filterNot(_.id == TheGrandScepterCard.id)

  /** The relic's secrets back to `holder`, facedown, and its warbands to
    * their supply, then the keep and its line, naming `card`. */
  def keep(holder: PlayerId, relic: RelicId, card: CardId)
      : Vector[Operation] =
    Vector(BuildOps((live, _) => Right(secretsBack(live, holder, relic) ++
        ReliquaryAndRelics.warbandsOff(live, relic) :+
        StartingRelicKept(holder, relic)), required = true),
      Note(noteId, _ => Some(kept.sourceless(NoteArg.Player(holder),
        NoteArg.Card(relic), NoteArg.Card(card)))))

  private def secretsBack(state: ReadyGame, holder: PlayerId,
      relic: RelicId): Vector[CoreOperation] =
    val secrets = state.game.current.player(holder).toVector
      .flatMap(_.relics.find(_.id == relic)).map(_.tokens.secrets).sum
    if secrets == 0 then Vector.empty
    else Vector(Move(Piece.Secrets(secrets),
        PositionedLocation(Location.OnCard(relic)),
        PositionedLocation(Location.PlayArea(holder))),
      FlipSecrets(holder, secrets, SecretSide.FaceUp, SecretSide.FaceDown))
