package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._

/** The headings the Chronicle Phase's Game Log groups its lines under: the
  * task's own ("The Sun") and, inside it, each step's ("Crown Chancellor").
  *
  * A step has no event of its own, so each step that writes lines opens with
  * a heading note, written just before its window (`ChronicleWindows.step`).
  * The log posts the heading only with the step's first line, so a step that
  * wrote nothing has no heading, and the lines until the next heading sit
  * under it. A step the rules name but that never writes a line, such as
  * Separate Visions, has no heading to write.
  *
  * A heading's key reads `step-<position>-<slug>`: the position is the
  * step's place in its task, and picks its colour. */
object ChronicleSteps:
  /** A note key beginning with this is a step heading, never a line. */
  private val prefix = "step-"

  def isHeading(key: String): Boolean = key.startsWith(prefix)

  /** The step's place in its task, read from its heading's key. */
  def position(key: String): Option[Int] =
    Option.when(isHeading(key))(key.stripPrefix(prefix).takeWhile(_ != '-'))
      .flatMap(_.toIntOption)

  /** The heading a task's walk opens with. */
  def taskTitle(procedure: ProcedureRef): Option[String] = procedure match
    case TriggeredProcedureRef.ChronicleSun => Some("The Sun")
    case TriggeredProcedureRef.ChronicleThrone => Some("The Throne")
    case TriggeredProcedureRef.ChronicleWorld => Some("The World")
    case TriggeredProcedureRef.ChronicleBeacon => Some("The Beacon")
    case TriggeredProcedureRef.ChronicleStars => Some("The Stars")
    case _ => None

  /** A step's heading note, for the procedure that runs the step. */
  final class Heading private[ChronicleSteps] (procedure: TriggeredProcedureRef,
      val key: NoteKey):
    def note: Operation =
      Note(ProcedureNotes.id(procedure), _ => Some(key.sourceless()))

  private final class Task(procedure: TriggeredProcedureRef):
    val notes: NoteSet = NoteSet(ProcedureNotes.id(procedure))
    def heading(position: Int, title: String): Heading =
      val slug = title.toLowerCase.replace(' ', '-')
      Heading(procedure,
        notes.key(s"$prefix$position-$slug", Vector(NotePart.Text(title))))

  private val sun = Task(TriggeredProcedureRef.ChronicleSun)
  val resolveLegacies: Heading = sun.heading(0, "Resolve Legacies")
  val crownChancellor: Heading = sun.heading(1, "Crown Chancellor")
  val assignTasks: Heading = sun.heading(2, "Assign Tasks")

  private val throne = Task(TriggeredProcedureRef.ChronicleThrone)
  val startOfThrone: Heading = throne.heading(0, "Start of Throne")
  val offerCitizenship: Heading = throne.heading(1, "Offer Citizenship")
  val shapeEmpire: Heading = throne.heading(2, "Shape Empire")
  val buildEdifice: Heading = throne.heading(3, "Build Edifice")
  val stockReliquary: Heading = throne.heading(4, "Stock Reliquary")
  val scatterRelics: Heading = throne.heading(5, "Scatter Relics")
  val scoreLineage: Heading = throne.heading(6, "Score Lineage Points")
  val swearOath: Heading = throne.heading(7, "Swear an Oath")

  private val world = Task(TriggeredProcedureRef.ChronicleWorld)
  val gatherDenizens: Heading = world.heading(0, "Gather Denizens")
  val protectAndDispossess: Heading =
    world.heading(1, "Protect and Dispossess")

  private val beacon = Task(TriggeredProcedureRef.ChronicleBeacon)
  val introduceNewcomers: Heading = beacon.heading(0, "Introduce Newcomers")
  val welcomeStranger: Heading = beacon.heading(1, "Welcome a Stranger")
  val shapeFoundation: Heading = beacon.heading(2, "Shape Foundation")

  private val stars = Task(TriggeredProcedureRef.ChronicleStars)
  val archiveRivalSites: Heading = stars.heading(0, "Archive Rival Sites")
  val store: Heading = stars.heading(1, "Store")

  /** The heading keys the steps of `procedure` write. */
  def noteKeys(procedure: ProcedureRef): Vector[NoteKey] = procedure match
    case TriggeredProcedureRef.ChronicleSun => sun.notes.declared
    case TriggeredProcedureRef.ChronicleThrone => throne.notes.declared
    case TriggeredProcedureRef.ChronicleWorld => world.notes.declared
    case TriggeredProcedureRef.ChronicleBeacon => beacon.notes.declared
    case TriggeredProcedureRef.ChronicleStars => stars.notes.declared
    case _ => Vector.empty
