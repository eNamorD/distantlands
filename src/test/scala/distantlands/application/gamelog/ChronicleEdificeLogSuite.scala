package distantlands.application.gamelog

import distantlands.gameplay.powers.action.{EldersCard, GiantsPathCard}
import distantlands.gameplay.powers.campaign.RivalKhanCard
import distantlands.gameplay.powers.economy.{CupOfPlentyCard,
  HallowedSpringCard, WildPasturesCard}
import distantlands.gameplay.powers.setup.GreatForgeCard
import distantlands.gameplay.powers.whenplayed.GreatHerdCard
import distantlands.model.PlayerId
import distantlands.protocol.projection.LogSpan
import distantlands.testkit.Table.{p1, p2, p3}
import LogScripts.{format, text}

/** The Chronicle lines of ruined edifices, as each viewer reads them (Last
  * Chronicle powers design, "Lines"). */
class ChronicleEdificeLogSuite extends munit.FunSuite:
  test("Hiding Place's relic is named to its ruler alone (ruling 4)"):
    val script = ChronicleLogScripts.throneHidingPlace
    val name = HallowedSpringCard.ruined.name
    val public = s"${p2.value} kept a relic with $name"
    val named = s"${p2.value} kept ${CupOfPlentyCard.name} with $name"
    def keptLines(viewer: Option[PlayerId]) =
      format(script, viewer).map(text).filter(line =>
        line == public || line == named)
    assertEquals(keptLines(Some(p2)), Vector(public, named))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assertEquals(keptLines(viewer), Vector(public), viewer))

  test("Blasted Pastures names what it buried inline, each by the face it " +
      "lay on, to every viewer (ruling 5)"):
    val script = ChronicleLogScripts.throneBlastedPastures
    val name = WildPasturesCard.ruined.name
    Vector(Some(p1), Some(p2), Some(p3), None).foreach { viewer =>
      val lines = format(script, viewer).map(text)
      val buried = lines.filter(_.startsWith(s"$name buried "))
      assertEquals(buried.size, 1, lines)
      Vector(name, EldersCard.name, GiantsPathCard.ruined.name).foreach(card =>
        assert(buried.head.contains(card), buried.head))
      // The generic detail line names the edifice as it lay, too.
      assert(lines.contains(s"Buried $name"), lines)
      Vector(WildPasturesCard.intact.name, GiantsPathCard.intact.name)
        .foreach(intact => assert(!lines.exists(_.contains(intact)), lines))
    }

  test("Blasted Pastures lists six cards with their faces (ruling 5)"):
    val script = ChronicleLogScripts.throneBlastedPasturesListed
    val name = WildPasturesCard.ruined.name
    val entry = format(script, Some(p2))
      .find(entry => text(entry).startsWith(s"$name buried ")).get
    assertEquals(text(entry), s"$name buried 6 cards")
    val faces = entry.spans.collect { case LogSpan.Cards(cards) => cards }
      .flatten
    assertEquals(faces.map(_.name).toSet, Set(name, EldersCard.name,
      GreatHerdCard.name, GiantsPathCard.ruined.name, RivalKhanCard.name,
      GreatForgeCard.intact.name))
    assert(faces.forall(!_.hidden), faces)
