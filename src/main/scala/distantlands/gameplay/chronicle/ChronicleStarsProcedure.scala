package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.walker.ProcedureStart
import distantlands.model._

/** The Stars task (CR pp. 40-41, `docs/rules/chronicle.md` task 5). No step
  * has an actor, and none changes the table (Chronicle ruling 18): the
  * Chronicle stores no pieces, and `ChronicleFinish` reads the sites as
  * these steps name them. Each step is its own power window. */
object ChronicleStarsProcedure:
  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleStars)
  private val notes = NoteSet(noteId)
  val archived: NoteKey = notes.key("archived", Vector(NotePart.Arg(0),
    NotePart.Text(" was archived")))
  val keptInEmpire: NoteKey = notes.key("kept-in-empire", Vector(
    NotePart.Arg(0), NotePart.Text(" stays in the Empire")))

  /** The notes the Stars' steps write. */
  def noteKeys: Vector[NoteKey] = notes.declared

  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    ProcedureStart.noSelection("the Stars task", args)
      .map(_ => Sequence(steps))

  private val steps: Vector[Operation] = Vector(
    // The constellation and narrative are not implemented (ruling 7).
    ChronicleWindows.step(PowerWindow.ChronicleWriteInTheStars, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleArchiveRivalSites,
      Vector(perSite(_.rivals, archived))),
    // Pieces return to their banks, which the Chronicle does not store;
    // the Empire's sites are already kept Cradlemost first.
    ChronicleWindows.step(PowerWindow.ChronicleCleanUp, Vector.empty),
    ChronicleWindows.step(PowerWindow.ChronicleStore,
      Vector(perSite(_.empire, keptInEmpire))))

  private def perSite(sites: ChronicleExtraction.SiteLayout => Vector[SiteId],
      key: NoteKey): Operation =
    // The Chronicle Phase follows a result, so the layout is always there;
    // the refusal keeps a walk without one from noting nothing.
    Branch((state, _) => ChronicleExtraction.layout(state) match
      case Left(violation) => Vector(ChronicleTasks.failing(violation))
      case Right(layout) => sites(layout).map(site =>
        Note(noteId, _ => Some(key.sourceless(NoteArg.Site(site))))))
