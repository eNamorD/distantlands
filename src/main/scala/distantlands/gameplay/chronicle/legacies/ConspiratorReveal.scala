package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powers.legacies.ConspiratorCard
import distantlands.model._

/** Conspirator at the game's end (Legacy in-play powers design, ruling
  * 27), a triggered walker procedure the driver starts once a result is
  * set, before the Chronicle opens (R24).
  *
  * {{{
  * Sequence(
  *   Note("p2 had written down p3 to win in round 6 with Conspirator"),
  *   Note("p2 won with Conspirator"),            // when fulfilled
  *   BuildOps(ResolvePrediction(p2, fulfilled)))
  * }}}
  *
  * The round the game ended in is `tracks.round`, and the winner the
  * result's, already after any Successor substitution. A holder who no
  * longer holds Conspirator active is void, and the prediction is still
  * revealed with the same line (R29). `ResolvePrediction` clears the store,
  * so the driver then opens the Chronicle for the result now set. The step
  * asks nothing, so it never resumes.
  */
object ConspiratorReveal:
  private val procedure = TriggeredProcedureRef.ConspiratorReveal
  private val noteId = ProcedureNotes.id(procedure)
  private val notes = NoteSet(noteId)
  /** "{p2} had written down {p3} to win in round {6} with Conspirator".
    * The card is named in words: a Conspirator lost to the deck, or gone
    * dormant, is a card no other viewer may identify, and the line names
    * it either way (R29). */
  val revealed: NoteKey = notes.key("revealed", Vector(NotePart.Arg(0),
    NotePart.Text(" had written down "), NotePart.Arg(1),
    NotePart.Text(" to win in round "), NotePart.Arg(2),
    NotePart.Text(s" with ${ConspiratorCard.name}")))
  /** "{p2} won with {Conspirator}". */
  val won: NoteKey = notes.key("won", Vector(NotePart.Arg(0),
    NotePart.Text(" won with "), NotePart.Arg(1)))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The holders whose prediction waits for the game's end, in seat order,
    * once a result is set and before the Chronicle. */
  def due(ready: ReadyGame): Vector[PlayerId] =
    val current = ready.game.current
    if current.result.isEmpty || current.turn.phase == Phase.Chronicle then
      Vector.empty
    else current.players.map(_.player).filter(current.predictions.contains)

  /** The prediction names the round the game ended in and its winner, and
    * its holder still holds Conspirator active. */
  def fulfilled(ready: ReadyGame, holder: PlayerId): Boolean =
    val current = ready.game.current
    current.predictions.get(holder).exists(prediction =>
      prediction.round == current.tracks.round &&
        current.result.exists(_.winner == prediction.winner)) &&
      ChronicleLegacyRules.holder(ready, ConspiratorCard.id).contains(holder)

  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- Either.cond(args.isEmpty, (), OathViolation.InvalidEventOrder(
        "Conspirator's reveal selects nothing"))
      holders <- Some(due(state)).filter(_.nonEmpty).toRight(
        OathViolation.InvalidEventOrder("no prediction waits for the end"))
    yield Sequence(holders.flatMap(holder => steps(state, holder)))

  private def steps(state: ReadyGame, holder: PlayerId): Vector[Operation] =
    val prediction = state.game.current.predictions(holder)
    val wins = fulfilled(state, holder)
    val card = NoteArg.Card(ConspiratorCard.id)
    Vector[Operation](Note(noteId, _ => Some(revealed.sourceless(
      NoteArg.Player(holder), NoteArg.Player(prediction.winner),
      NoteArg.Number(prediction.round))))) ++
      Option.when(wins)(Note(noteId, _ => Some(won.sourceless(
        NoteArg.Player(holder), card)))).toVector :+
      BuildOps((_, _) => Right(Vector(ResolvePrediction(holder, wins))),
        required = true)
