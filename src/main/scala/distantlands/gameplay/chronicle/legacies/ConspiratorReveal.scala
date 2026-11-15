package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powers.legacies.ConspiratorCard
import distantlands.model._

/** Conspirator at the game's end (Legacy in-play powers design, ruling 27):
  * the Sun's steps right after its first window, where Royal Line names
  * the winner, so the prediction is compared with the winner Royal Line
  * leaves (R24, as the owner re-ruled it).
  *
  * {{{
  * Note("p2 had written down p3 to win in round 6 with Conspirator"),
  * Note("p2 won with Conspirator"),            // when fulfilled
  * BuildOps(ResolvePrediction(p2, fulfilled))  // nothing without one
  * }}}
  *
  * The round the game ended in is `tracks.round`, and the winner the
  * result's, after any Successor substitution and any Royal Line choice. A
  * holder who no longer holds Conspirator active is void, and the
  * prediction is still revealed with the same line (R29). A fulfilled one
  * makes its holder the winner, which every later Chronicle step reads.
  * The steps have one shape, prediction or none, and ask nothing.
  */
object ConspiratorReveal:
  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun)
  private val notes = NoteSet(noteId)
  /** "{p2} had written down {p3} to win in round {6} with Conspirator".
    * The card is named in words: a Conspirator lost to the deck, or gone
    * dormant, is a card no other viewer may identify, and the line names
    * it either way (R29). */
  val revealed: NoteKey = notes.key("conspirator-revealed", Vector(
    NotePart.Arg(0), NotePart.Text(" had written down "), NotePart.Arg(1),
    NotePart.Text(" to win in round "), NotePart.Arg(2),
    NotePart.Text(s" with ${ConspiratorCard.name}")))
  /** "{p2} won with {Conspirator}". */
  val won: NoteKey = notes.key("conspirator-won", Vector(NotePart.Arg(0),
    NotePart.Text(" won with "), NotePart.Arg(1)))
  /** The Sun lines these steps write, on the Sun's note id. */
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The stored prediction, if any, in seat order: Conspirator is one card,
    * so one holder at most. */
  private def stored(ready: ReadyGame): Option[(PlayerId, Prediction)] =
    val current = ready.game.current
    current.players.map(_.player).collectFirst(Function.unlift(holder =>
      current.predictions.get(holder).map(holder -> _)))

  /** The prediction names the round the game ended in and its winner, and
    * its holder still holds Conspirator active. */
  def fulfilled(ready: ReadyGame, holder: PlayerId): Boolean =
    val current = ready.game.current
    current.predictions.get(holder).exists(prediction =>
      prediction.round == current.tracks.round &&
        current.result.exists(_.winner == prediction.winner)) &&
      ChronicleLegacyRules.holder(ready, ConspiratorCard.id).contains(holder)

  /** The reveal, the win when it holds, and the store cleared. */
  val steps: Vector[Operation] = Vector(
    Note(noteId, states => stored(states.now).map((holder, prediction) =>
      revealed.sourceless(NoteArg.Player(holder),
        NoteArg.Player(prediction.winner),
        NoteArg.Number(prediction.round)))),
    Note(noteId, states => stored(states.now).collect {
      case (holder, _) if fulfilled(states.now, holder) =>
        won.sourceless(NoteArg.Player(holder),
          NoteArg.Card(ConspiratorCard.id)) }),
    BuildOps((live, _) => Right(stored(live).toVector.map((holder, _) =>
      ResolvePrediction(holder, fulfilled(live, holder)))), required = true))
