package distantlands.application.gamelog

import distantlands.catalog.holding.{IronHandCard, SeerCard}
import distantlands.protocol.projection.LogSpan
import distantlands.testkit.Table.{p1, p2, p3}
import LogScripts.{format, text}

/** The Sun's legacy lines as each viewer reads them: in the Chronicle Phase
  * every legacy is named to everyone (Legacies implementation ruling R18). */
class ChronicleLegacyLogSuite extends munit.FunSuite:
  test("the Sun names a discarded dormant legacy and a legacy gone dormant " +
      "to a seat that does not own them and to the public view"):
    val script = ChronicleLogScripts.chronicleWithLegacies
    Vector(Some(p1), None).foreach { viewer =>
      val entries = format(script, viewer)
      val discarded = entries.filter(entry =>
        text(entry) == s"${p2.value} discarded ${SeerCard.name}")
      val dormant = entries.filter(entry =>
        text(entry) == s"${p3.value}'s ${IronHandCard.name} went dormant")
      assertEquals((discarded.size, dormant.size), (1, 1), viewer)
      assert(discarded.head.spans.contains(
        LogSpan.Card(SeerCard.id.value, SeerCard.name)), viewer)
      assert(dormant.head.spans.contains(
        LogSpan.Card(IronHandCard.id.value, IronHandCard.name)), viewer)
    }
