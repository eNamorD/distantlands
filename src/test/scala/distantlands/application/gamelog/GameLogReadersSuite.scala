package distantlands.application.gamelog

import distantlands.engine.ReplayStep
import distantlands.gameplay.actions.negotiation.NegotiationProcedure
import distantlands.gameplay.walker.LogNoted
import distantlands.model._
import distantlands.protocol.projection.LogEntry
import distantlands.testkit.Table.{p1, p2, p3}
import LogScripts._

/** A note with readers posts only for them (catalog batch 12, ruling 23),
  * and every other line keeps its keys for every viewer. */
class GameLogReadersSuite extends munit.FunSuite:
  private val promise = s"${playerLabel(p1)} promised ${playerLabel(p2)}: " +
    "“I will not attack you this round.”"

  /** `negotiationPromised`'s journal, its promise line read only by the
    * two negotiators. p3 stands outside the deal. */
  private def promiseReadByTheNegotiators(using munit.Location)
      : Vector[ReplayStep[OathState, OathEvent]] =
    negotiationPromised.history.steps.map(step => step.event.event match
      case noted @ LogNoted(_, note, _)
          if note.key == NegotiationProcedure.promised.name =>
        step.copy(event = step.event.copy(
          event = noted.copy(note = note.readBy(Vector(p1, p2)))))
      case _ => step)

  private def keys(entries: Vector[LogEntry]): Vector[(Long, Int)] =
    entries.map(entry => (entry.sequence, entry.ordinal))

  test("a note's readers read its line, and nobody else does"):
    val steps = promiseReadByTheNegotiators
    Vector(Some(p1) -> 1, Some(p2) -> 1, Some(p3) -> 0, None -> 0).foreach {
      (viewer, count) =>
        val all = texts(formatter.format(steps, viewer))
        assertEquals(all.count(_ == promise), count, s"$viewer: $all")
    }

  test("the lines around a dropped line keep their keys"):
    val steps = promiseReadByTheNegotiators
    val reader = formatter.format(steps, Some(p1))
    assertEquals(keys(formatter.format(steps, Some(p3))),
      keys(reader.filterNot(entry => text(entry) == promise)))

  test("every line, read-restricted ones too, is what a reader receives"):
    val steps = promiseReadByTheNegotiators
    assertEquals(keys(formatter.formatEveryLine(steps, Some(p3))),
      keys(formatter.format(steps, Some(p1))))
