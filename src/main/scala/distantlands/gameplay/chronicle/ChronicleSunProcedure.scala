package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.legacies.{IronHand,
  LegacyChroniclePowers}
import distantlands.gameplay.walker.ProcedureStart
import distantlands.model._

/** The Sun task (CR p. 36, `docs/rules/chronicle.md` task 1). Each step is
  * its own power window. Steps 2-4 resolve legacies (`ResolveLegacies`);
  * step 5, Shuffle, is an empty window, since the legacy deck is derived
  * (Legacies design, ruling 3). Two empty windows hold the legacies' added
  * steps: one before step 2, where Gatekeeper acts, and one after step 5,
  * where Chronicler does (Legacy Chronicle powers design, rulings 6 and
  * 7); the first window, before Reveal Relics, held Royal Line's step
  * (ruling 22) until Legacy in-play powers R36 moved it, with
  * Conspirator's comparison, before Start Chronicle
  * (`ChronicleWinnerProcedure`); it stays, empty.
  *
  * Every step reads the winner from the game's result when it is reached,
  * so Royal Line's choice and a Conspirator win reach them all (ruling 23): Crown Chancellor and
  * Assign Tasks here (`forWinner`); and, read the same way elsewhere,
  * Reformer's "if you won", Score Lineage Points and Iron Hand's scoring
  * after it, the Grand Scepter goal's "if none, win" (`LegacyGoals`), Swear
  * an Oath, and the Stars' and the extraction's winner-ruled sites, with
  * Iron Hand's holder in the winner's place. The Throne holder is the
  * journaled Throne task. */
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
  /** "p1 became an Exile": a former Imperial when an Exile's win breaks the
    * Empire. */
  val exiled: NoteKey = notes.key("exiled", Vector(
    NotePart.Arg(0), NotePart.Text(" became an Exile")))
  private val gaveKeys: Map[ChronicleTask, NoteKey] =
    ChronicleTask.all.map(task => task -> notes.key(s"gave-${task.key}",
      Vector(NotePart.Arg(0), NotePart.Text(" gave "), NotePart.Arg(1),
        NotePart.Text(s" the ${name(task)} task")))).toMap
  /** The winner, or a player holding two tasks, gave `task` to another. */
  def gaveTask(task: ChronicleTask): NoteKey = gaveKeys(task)
  def noteKeys: Vector[NoteKey] = notes.declared ++ ResolveLegacies.noteKeys ++
    ChronicleSteps.noteKeys(TriggeredProcedureRef.ChronicleSun) ++
    LegacyChroniclePowers.sunNoteKeys ++
    IronHand.noteKeys(TriggeredProcedureRef.ChronicleSun)

  private def name(task: ChronicleTask): String = task match
    case ChronicleTask.Throne => "Throne"
    case ChronicleTask.World => "World"
    case ChronicleTask.Beacon => "Beacon"

  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- ProcedureStart.noSelection("the Sun", args)
      _ <- ChronicleTasks.winner(ready)
    yield Sequence(steps(catalog))

  /** The nodes `steps` builds for the winner the result names when the
    * step is reached. Royal Line's choice and a Conspirator win are settled
    * before the Chronicle (`ChronicleWinnerProcedure`; Legacy in-play powers
    * R36), so the result is the final winner. A shape that reads only the
    * result, which nothing in the step changes, holds on resume. */
  private def forWinner(steps: PlayerId => Vector[Operation]): Operation =
    Branch((state, _) => ChronicleTasks.winner(state).fold(
      violation => Vector(ChronicleTasks.failing(violation)), steps))

  private def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    // Iron Hand's first checkpoint opens the Sun (Legacy Chronicle powers
    // design, ruling 18; R30).
    ChronicleWindows.step(ChronicleSteps.resolveLegacies,
      PowerWindow.ChronicleRevealRelics, Vector(
      IronHand.discard(TriggeredProcedureRef.ChronicleSun),
      BuildOps((state, _) => Right(revealHeldRelics(state)), required = true))),
    ChronicleWindows.step(PowerWindow.ChronicleBeforeResolveLegacies,
      Vector.empty),
    // Sun steps 2-5 (CR p. 36): flip or discard dormant legacies, Exiles'
    // unmet legacies go dormant, discard legacies over three, shuffle the
    // legacy deck, which is derived and has nothing to shuffle.
    ChronicleWindows.step(PowerWindow.ChronicleFlipLegacies,
      ResolveLegacies.flip(catalog)),
    ChronicleWindows.step(PowerWindow.ChronicleExileDormancy,
      ResolveLegacies.dormancy(catalog)),
    // Iron Hand's checkpoint before the cap catches one an Imperial gained
    // active after the Sun's first, by Flip Legacies or Gatekeeper (R46).
    ChronicleWindows.step(PowerWindow.ChronicleLegacyCap,
      IronHand.discard(TriggeredProcedureRef.ChronicleSun) +:
        ResolveLegacies.capAll),
    ChronicleWindows.step(PowerWindow.ChronicleShuffleLegacies, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleAfterResolveLegacies,
      Vector.empty),
    ChronicleWindows.step(ChronicleSteps.crownChancellor,
      PowerWindow.ChronicleCrown, Vector(forWinner(
      winner => Vector(
        BuildOps((state, _) => ChronicleCrown.operations(state, winner),
          required = true),
        Note(noteId, states => Option.when(brokeTheEmpire(states, winner))(
          empireBroke.sourceless())),
        // Each seated Imperial the break made an Exile; a boxed lineage has
        // no player to name.
        Branch((state, _) => ChronicleTasks.seated(state)
          .filter(_ != winner).map(player => Note(noteId, states =>
            Option.when(brokeTheEmpire(states, winner) &&
              wasImperial(states, player))(
              exiled.sourceless(NoteArg.Player(player)))))),
        Note(noteId, _ => Some(crowned.sourceless(NoteArg.Player(winner)))),
        // An Exile's win: their newly dormant legacies become active (CR
        // "If an Exile won, the Empire breaks"), then the cap if need be.
        // An Imperial winner holds no newly dormant legacy after step 3
        // (ruling 2), so this asks nothing of them; a legacy Stone Baths
        // kept is not newly dormant (catalog batch 13, ruling 14). Iron
        // Hand's second checkpoint sits between the reactivation and the
        // cap (R30).
        ResolveLegacies.reactivation(winner, noteId,
          ResolveLegacies.reactivated),
        IronHand.discard(TriggeredProcedureRef.ChronicleSun),
        ResolveLegacies.cap(winner,
          ResolveLegacies.crownCapDecisionId(winner), noteId,
          ResolveLegacies.capped))))),
    ChronicleWindows.step(ChronicleSteps.assignTasks,
      PowerWindow.ChronicleAssignTasks, Vector(forWinner(
      ChronicleTaskAssignment.steps(_, ChronicleTaskAssignment.Notes(
        tookNote, gaveNote))))))

  /** Reveal all facedown relics held by players; relics at sites stay. */
  private def revealHeldRelics(ready: ReadyGame): Vector[CoreOperation] =
    ready.game.current.players.flatMap(player => player.relics.collect {
      case RelicState(id, Orientation.FaceDown, _) =>
        Reveal(id, Location.PlayArea(player.player))
    })

  /** Whether the step before made an Exile winner Chancellor of an Empire
    * that stood: a first game has no Imperial to lose it. */
  private def brokeTheEmpire(states: NoteStates, winner: PlayerId): Boolean =
    states.previous.exists((before, _) =>
      ChronicleTasks.lineageOf(before, winner)
        .flatMap(before.game.campaign.lineages.get)
        .exists(_.role == Role.Exile) &&
        ChronicleCrown.imperials(before).nonEmpty)

  /** Whether `player`'s lineage held an Imperial role before the step. */
  private def wasImperial(states: NoteStates, player: PlayerId): Boolean =
    states.previous.exists((before, _) =>
      ChronicleTasks.lineageOf(before, player)
        .exists(ChronicleCrown.imperials(before).contains))

  private def tookNote(task: ChronicleTask): Operation =
    Note(noteId, states => states.now.game.current.chronicleTasks.get(task)
      .map(holder => tookTask(task).sourceless(NoteArg.Player(holder))))

  /** `giver` gave `task` to the player now holding it, or took it when they
    * gave it to themself. */
  private def gaveNote(task: ChronicleTask, giver: PlayerId): Operation =
    Note(noteId, states => states.now.game.current.chronicleTasks.get(task)
      .map(holder =>
        if holder == giver then tookTask(task).sourceless(NoteArg.Player(holder))
        else gaveTask(task).sourceless(NoteArg.Player(giver),
          NoteArg.Player(holder))))
