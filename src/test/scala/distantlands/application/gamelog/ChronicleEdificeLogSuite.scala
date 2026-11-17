package distantlands.application.gamelog

import distantlands.gameplay.powers.economy.{CupOfPlentyCard,
  HallowedSpringCard}
import distantlands.model.PlayerId
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
