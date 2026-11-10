package oathdigital.gameplay

import oathdigital.gameplay.phases.rest.RoundEndRandomPort
import oathdigital.model.PlayerId
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.RoundEndChoices

/** What every `RoundEndRandomPort` promises: it picks one of the tied
  * candidates it is given, and its end die shows a face from 1 to 6. The
  * real random port and the shared stubs run it, so a stub cannot name a
  * winner who was not tied. */
class RoundEndRandomPortSuite extends munit.FunSuite:
  private val ports = Vector(
    "random" -> RoundEndRandomPort.random,
    "first candidate" -> RoundEndChoices.firstCandidate,
    "last candidate" -> RoundEndChoices.lastCandidate)

  ports.foreach { (name, port) =>
    test(s"$name: picks one of the tied candidates"):
      val tied = Vector(p1, p2, p3)
      // Random, so ask many times: every answer must be a tied player.
      val picks = Vector.fill(50)(port.choose(tied))
      assertEquals(picks.filterNot(tied.contains), Vector.empty[PlayerId])

    test(s"$name: a single candidate is the pick"):
      assertEquals(port.choose(Vector(p2)), p2)
  }

  test("the random end die rolls every face from 1 to 6 and nothing else"):
    val faces = Vector.fill(600)(RoundEndRandomPort.random.endDie())
    assertEquals(faces.toSet, (1 to 6).toSet)

  test("a fixed end die rolls its face"):
    assertEquals(RoundEndChoices.endDie(4).endDie(), 4)
