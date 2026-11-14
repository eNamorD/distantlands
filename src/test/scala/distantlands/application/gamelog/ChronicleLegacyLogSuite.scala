package distantlands.application.gamelog

import distantlands.catalog.holding.SeerCard
import distantlands.gameplay.chronicle.legacies.{ChroniclerCard,
  GatekeeperCard, HeirloomCard, IronHandCard, TheBrightBeaconCard,
  TheEarCard}
import distantlands.gameplay.powers.economy.{BirdsongCard, CupOfPlentyCard,
  GolemLegionsCard}
import distantlands.model.PlayerId
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

  test("Gatekeeper's and Chronicler's lines read the same to every viewer"):
    val script = ChronicleLogScripts.sunGatekeeperChronicler
    Vector(Some(p1), Some(p2), None).foreach { viewer =>
      val said = format(script, viewer).map(text)
      assert(said.contains(s"${p2.value} took ${SeerCard.name} from " +
        s"${p3.value} with ${GatekeeperCard.name}"), viewer)
      assert(said.contains(s"${p2.value} declared ${ChroniclerCard.name}"),
        viewer)
      assert(said.exists(_.startsWith(s"${p2.value} scored ")), viewer)
    }

  test("The Ear's and The Bright Beacon's peeks write no line; each power " +
      "writes its one line (R47)"):
    val script = ChronicleLogScripts.chronicleEarBeacon
    Vector(Some(p1), Some(p2), Some(p3), None).foreach { viewer =>
      val said = format(script, viewer).map(text)
      assertEquals(said.filter(_.toLowerCase.contains("peeked at")),
        Vector.empty, viewer)
      assertEquals(said.count(_ == s"${p2.value} chose where the relics " +
        s"went with ${TheEarCard.name}"), 1, viewer)
      assertEquals(said.count(_ == s"${p2.value} took six newcomers from " +
        s"the Dispossessed with ${TheBrightBeaconCard.name}"), 1, viewer)
    }

  test("Heirloom's relic is named to its holder alone (ruling 13; R19)"):
    val script = ChronicleLogScripts.throneHeirloom
    val public = s"${p2.value} kept a relic with ${HeirloomCard.name}"
    val named = s"${p2.value} kept ${CupOfPlentyCard.name} with " +
      HeirloomCard.name
    def heirloomLines(viewer: Option[PlayerId]) =
      format(script, viewer).map(text).filter(line =>
        line == public || line == named)
    assertEquals(heirloomLines(Some(p2)), Vector(public, named))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assertEquals(heirloomLines(viewer), Vector(public), viewer))

  test("the next game starts p2's lineage with what Oaths and Heirloom " +
      "kept, facedown (R27)"):
    val record = ChronicleLogScripts.oathsHeirloomPlayed._2
    val lineage = record.lineages.find(_.startingRelics.nonEmpty).get
    assertEquals(lineage.startingRelics, Vector(CupOfPlentyCard.id))
    assertEquals(lineage.startingAdvisers.map(_.id).toSet,
      Set[distantlands.model.CardId](BirdsongCard.id, GolemLegionsCard.id))
