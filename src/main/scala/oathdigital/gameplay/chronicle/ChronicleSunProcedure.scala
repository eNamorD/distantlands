package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.model._

/** The Sun task (CR p. 36, `docs/rules/chronicle.md` task 1). Each step is
  * its own power window. Steps 2-5 resolve legacies, which arrive with the
  * Legacies slice: they are empty windows here. */
object ChronicleSunProcedure:
  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun)
  private val notes = NoteSet(noteId)
  val empireBroke: NoteKey = notes.key("empire-broke", Vector(
    NotePart.Text("The Empire broke")))
  val crowned: NoteKey = notes.key("crowned", Vector(
    NotePart.Arg(0), NotePart.Text(" became Chancellor")))
  private val tookKeys: Map[ChronicleTask, NoteKey] =
    ChronicleTask.all.map(task => task -> notes.key(s"took-${task.key}",
      Vector(NotePart.Arg(0), NotePart.Text(s" took the ${name(task)} task"))))
      .toMap
  def tookTask(task: ChronicleTask): NoteKey = tookKeys(task)
  def noteKeys: Vector[NoteKey] = notes.declared

  private def name(task: ChronicleTask): String = task match
    case ChronicleTask.Throne => "Throne"
    case ChronicleTask.World => "World"
    case ChronicleTask.Beacon => "Beacon"

  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- Either.cond(args.isEmpty, (), OathViolation.InvalidEventOrder(
        "the Sun takes no start selection"))
      winner <- ChronicleTasks.winner(ready)
    yield Sequence(steps(winner))

  private def steps(winner: PlayerId): Vector[Operation] = Vector(
    ChronicleWindows.step(PowerWindow.ChronicleRevealRelics, Vector(
      BuildOps((state, _) => Right(revealHeldRelics(state)), required = true))),
    // Sun steps 2-5 (CR p. 36): flip or discard dormant legacies, Exiles'
    // unmet legacies go dormant, discard legacies over three, shuffle the
    // legacy deck. They wait for the Legacies slice.
    ChronicleWindows.step(PowerWindow.ChronicleFlipLegacies, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleExileDormancy, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleLegacyCap, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleShuffleLegacies, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleCrown, Vector(
      BuildOps((state, _) => ChronicleCrown.operations(state, winner),
        required = true),
      Note(noteId, states => Option.when(brokeTheEmpire(states, winner))(
        empireBroke.sourceless())),
      Note(noteId, _ => Some(crowned.sourceless(NoteArg.Player(winner)))))),
    ChronicleWindows.step(PowerWindow.ChronicleAssignTasks,
      ChronicleTaskAssignment.steps(winner, tookNote)))

  /** Reveal all facedown relics held by players; relics at sites stay. */
  private def revealHeldRelics(ready: ReadyGame): Vector[CoreOperation] =
    ready.game.current.players.flatMap(player => player.relics.collect {
      case RelicState(id, Orientation.FaceDown, _) =>
        Reveal(id, Location.PlayArea(player.player))
    })

  /** Whether the step before made an Exile winner Chancellor. */
  private def brokeTheEmpire(states: NoteStates, winner: PlayerId): Boolean =
    states.previous.exists((before, _) =>
      ChronicleTasks.lineageOf(before, winner)
        .flatMap(before.game.campaign.lineages.get)
        .exists(_.role == Role.Exile))

  private def tookNote(task: ChronicleTask): Operation =
    Note(noteId, states => states.now.game.current.chronicleTasks.get(task)
      .map(holder => tookTask(task).sourceless(NoteArg.Player(holder))))
