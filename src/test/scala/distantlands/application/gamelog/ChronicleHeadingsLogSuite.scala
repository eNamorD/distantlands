package distantlands.application.gamelog

import distantlands.protocol.projection.{LogEntry, LogKind}
import LogScripts.{format, text}

/** The Chronicle Phase's headings: the phase at the round's level, each
  * task under it, each step that writes a line in its own band. */
class ChronicleHeadingsLogSuite extends munit.FunSuite:
  private def chronicle(script: Script): Vector[LogEntry] =
    format(script, None).dropWhile(_.kind != LogKind.Victory).drop(1)

  private def headlines(entries: Vector[LogEntry]): Vector[(LogKind, String)] =
    entries.filter(_.depth == 0).map(entry => entry.kind -> text(entry))

  private def steps(entries: Vector[LogEntry]): Vector[String] =
    entries.filter(_.kind == LogKind.Section).map(text)

  test("the Chronicle Phase stands at the round's level, each task under it " +
      "as a phase"):
    assertEquals(headlines(chronicle(ChronicleLogScripts.chronicleStarted)),
      Vector(LogKind.Round -> "Chronicle Phase", LogKind.Phase -> "The Sun",
        LogKind.Phase -> "The Throne", LogKind.Phase -> "The World",
        LogKind.Phase -> "The Beacon", LogKind.Phase -> "The Stars"))

  test("a step that writes lines opens a heading; a step that writes none " +
      "has no heading"):
    assertEquals(steps(chronicle(ChronicleLogScripts.chronicleStarted)),
      Vector("Crown Chancellor", "Assign Tasks", "Shape Empire",
        "Score Lineage Points", "Swear an Oath", "Protect and Dispossess",
        "Introduce Newcomers", "Welcome a Stranger", "Archive Rival Sites",
        "Store"))

  test("a step's heading comes first in its band, and every band has a line"):
    val entries = chronicle(ChronicleLogScripts.chronicleStarted)
    entries.flatMap(_.section).distinct.foreach { section =>
      val own = entries.filter(_.section.contains(section))
      assertEquals(own.head.kind, LogKind.Section, section.toString)
      assert(own.size > 1, s"$section holds only its heading")
      assert(section.action.startsWith("chronicle-step-"), section.toString)
    }

  test("the steps of one task take different colours"):
    val entries = chronicle(ChronicleLogScripts.chronicleStarted)
    val throne = entries.dropWhile(text(_) != "The Throne")
      .takeWhile(text(_) != "The World").filter(_.kind == LogKind.Section)
      .flatMap(_.section).map(_.action)
    assertEquals(throne.distinct, throne)

  test("the Start of Throne heading appears only when a power writes a line " +
      "there"):
    assert(!steps(chronicle(ChronicleLogScripts.chronicleStarted))
      .contains("Start of Throne"))
    assert(steps(chronicle(ChronicleLogScripts.throneHidingPlace))
      .contains("Start of Throne"))

  test("Protect and Dispossess names both faceup stacks before the choice"):
    val lines = chronicle(ChronicleLogScripts.chronicleStarted).map(text)
    val stacks = lines.filter(_.startsWith("Stack "))
    assertEquals(stacks.map(_.take(8)), Vector("Stack 1:", "Stack 2:"))
    assert(lines.indexOf(stacks.last) <
      lines.indexWhere(_.contains("protected the stack of")), lines)

  test("a heading never posts without its first line: no prefix of the " +
      "journal ends on one"):
    val script = ChronicleLogScripts.chronicleStarted
    val all = script.history.steps
    (1 to all.size).foreach { size =>
      val entries = LogScripts.formatter.format(all.take(size), None)
      entries.lastOption.foreach(last => assertNotEquals(last.kind,
        LogKind.Section, s"journal prefix of $size ends on a heading"))
    }
