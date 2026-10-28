package oathdigital.gameplay.chronicle

import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

/** Protect and Dispossess, the World's third step (CR p. 38). The World
  * task holder protects one of the two faceup stacks by naming a card in
  * it; the stack goes to their temporary hand. They keep one secretly as a
  * facedown starting adviser and the other two go to the world deck bottom.
  * The other six gathered cards wait in the gathering pile until the step
  * ends, then are newly Dispossessed with one log line. Nobody knows the
  * facedown three, so the line names only the faceup ones. */
object ProtectAndDispossess:
  val protectDecisionId: String = "chronicle.world.protect"
  val keepDecisionId: String = "chronicle.world.keep"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleWorld)
  private val notes = NoteSet(noteId)
  val protectedStack: NoteKey = notes.key("protected", Vector(
    NotePart.Arg(0), NotePart.Text(" protected the stack of "),
    NotePart.Arg(1)))
  val keptAdviser: NoteKey = notes.key("kept-adviser", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1),
    NotePart.Text(" as a starting adviser")))
  val dispossessed: NoteKey = notes.key("dispossessed", Vector(
    NotePart.Arg(0), NotePart.Text(" were dispossessed")))
  def noteKeys: Vector[NoteKey] = notes.declared

  private val stackSize = 3

  val steps: Vector[Operation] = Vector(
    Branch((state, _) => ChronicleTasks.forHolder(state, ChronicleTask.World)(
      holder => protect(state, holder))),
    Branch((state, _) => ChronicleTasks.forHolder(state, ChronicleTask.World)(
      holder => keep(state, holder))),
    // The note reads the pile before the move, while the faceup cards are
    // still identifiable where they lie.
    Note(noteId, states => Option.when(
      states.now.game.current.chronicleGathering.nonEmpty)(
      dispossessed.sourceless(NoteArg.Cards(
        states.now.game.current.chronicleGathering)))),
    BuildOps((state, _) => Right(state.game.current.chronicleGathering.map(
      card => Move(Piece.Card(card),
        PositionedLocation(Location.ChronicleGathering),
        PositionedLocation(Location.NewlyDispossessed)))), required = true))

  private def faceup(state: ReadyGame): Vector[DenizenId] =
    val pile = state.game.current.chronicleGathering
    pile.take(pile.size - stackSize).collect { case id: DenizenId => id }

  private def protect(state: ReadyGame, holder: PlayerId): Vector[Operation] =
    if faceup(state).isEmpty then Vector.empty
    else Vector(
      Decide(protectDecisionId, holder, DecisionQuery.ChooseOne(
        faceup(state).map(card =>
          DecisionOption.Denizen(DecisionOptionRef.Denizen(card))),
        heading = Some("Protect a stack: choose any card in it"),
        confirmVerb = Some("Protect"))),
      BuildOps((live, pending) => chosen(pending).map(card =>
        split(live, holder, card)), required = true),
      Note(noteId, states => states.now.game.current.temporaryHands
        .get(holder).filter(_.nonEmpty).map(cards =>
          protectedStack.sourceless(NoteArg.Player(holder),
            NoteArg.Cards(cards)))))

  private def chosen(pending: PendingTree): Either[OathViolation, DenizenId] =
    PowerAnswers.one(pending, protectDecisionId).collect {
      case DecisionOptionRef.Denizen(card) => card
    }.toRight(PowerAnswers.missing(protectDecisionId))

  /** The protected stack to the holder's hand. The rest stay in the
    * gathering pile, the facedown stack still last. */
  private def split(state: ReadyGame, holder: PlayerId, card: DenizenId)
      : Vector[CoreOperation] =
    val pile = state.game.current.chronicleGathering
    val stack = pile.indexOf(card) / stackSize
    pile.zipWithIndex.collect { case (id, at) if at / stackSize == stack =>
      Move(Piece.Card(id), PositionedLocation(Location.ChronicleGathering),
        PositionedLocation(Location.Hand(holder)))
    }

  private def keep(state: ReadyGame, holder: PlayerId): Vector[Operation] =
    val hand = state.game.current.temporaryHands.getOrElse(holder, Vector.empty)
      .collect { case id: DenizenId => id }
    ChronicleTasks.lineageOf(state, holder) match
      case _ if hand.isEmpty => Vector.empty
      case None => Vector(ChronicleTasks.failing(OathViolation
        .InvalidEventOrder(s"${holder.value} has no lineage")))
      case Some(lineage) => StartingAdviserChoice.steps(holder, lineage, hand,
        keepDecisionId, "Keep a starting adviser", keptAdviser, noteId)
