package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

/** Protect and Dispossess, the World's third step (CR p. 38). The World
  * task holder protects one of the two faceup stacks, "Protect Stack 1" or
  * "Protect Stack 2", the cards of which every viewer sees beside the
  * question (`WalkerDecisionProjector.publicPreview`); the stack goes to
  * their temporary hand. They keep one secretly as a
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
  /** "Stack 1: A, B and C": a faceup stack, named before the holder
    * protects one. */
  val stackShown: NoteKey = notes.key("stack-shown", Vector(
    NotePart.Text("Stack "), NotePart.Arg(0), NotePart.Text(": "),
    NotePart.Arg(1)))
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
      states.now.game.current.cardGathering.nonEmpty)(
      dispossessed.sourceless(NoteArg.Cards(
        states.now.game.current.cardGathering)))),
    BuildOps((state, _) => Right(state.game.current.cardGathering.map(
      card => Move(Piece.Card(card),
        PositionedLocation(Location.CardGathering),
        PositionedLocation(Location.NewlyDispossessed)))), required = true))

  private def faceup(state: ReadyGame): Vector[DenizenId] =
    val pile = state.game.current.cardGathering
    pile.take(pile.size - stackSize).collect { case id: DenizenId => id }

  /** The faceup stacks in the order the buttons name them, the first
    * "Stack 1". */
  def faceupStacks(state: ReadyGame): Vector[Vector[DenizenId]] =
    faceup(state).grouped(stackSize).toVector

  private def stackRef(number: Int): DecisionOptionRef.Button =
    DecisionOptionRef.Button(s"protect-stack-$number")

  private def protect(state: ReadyGame, holder: PlayerId): Vector[Operation] =
    val stacks = faceupStacks(state)
    if stacks.isEmpty then Vector.empty
    else stacks.zipWithIndex.map((stack, at) =>
      Note(noteId, _ => Some(stackShown.sourceless(NoteArg.Number(at + 1),
        NoteArg.Cards(stack))))) ++ Vector(
      Decide(protectDecisionId, holder, DecisionQuery.ChooseOne(
        stacks.indices.map(at => DecisionOption.Button(stackRef(at + 1),
          s"Protect Stack ${at + 1}")).toVector,
        heading = Some("Protect a stack"))),
      BuildOps((live, pending) => chosen(live, pending).map(stack =>
        split(live, holder, stack)), required = true),
      Note(noteId, states => states.now.game.current.temporaryHands
        .get(holder).filter(_.nonEmpty).map(cards =>
          protectedStack.sourceless(NoteArg.Player(holder),
            NoteArg.Cards(cards)))))

  /** The index of the answered stack, counting from 0. */
  private def chosen(state: ReadyGame, pending: PendingTree)
      : Either[OathViolation, Int] =
    PowerAnswers.one(pending, protectDecisionId).flatMap(answer =>
      faceupStacks(state).indices.find(at => stackRef(at + 1) == answer))
      .toRight(PowerAnswers.missing(protectDecisionId))

  /** The protected stack to the holder's hand. The rest stay in the
    * gathering pile, the facedown stack still last. */
  private def split(state: ReadyGame, holder: PlayerId, stack: Int)
      : Vector[CoreOperation] =
    val pile = state.game.current.cardGathering
    pile.zipWithIndex.collect { case (id, at) if at / stackSize == stack =>
      Move(Piece.Card(id), PositionedLocation(Location.CardGathering),
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
