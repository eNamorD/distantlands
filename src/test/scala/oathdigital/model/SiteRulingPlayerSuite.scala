package oathdigital.model

import oathdigital.gameplay.cards.DunesSite
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

class SiteRulingPlayerSuite extends munit.FunSuite:
  private val dunes = DunesSite

  /** p1 is the Chancellor and p2 a Citizen; the Empire holds the Dunes. */
  private def empireAtDunes: Table =
    Table.start.imperial(p1, p2).warbandsAt(dunes, p1, 2)

  private def rulerOfDunes(table: Table): Option[SiteRuler] =
    val ready = table.ready
    SiteRule.rulingPlayer(ready,
      dunes.id)

  test("the Empire's sites keep the Empire as their raw ruler"):
    val ready = empireAtDunes.ready
    val site = ready.game.current.map.sites(
      dunes.id)
    assertEquals(SiteRule.ruler(site.forces, ready.game.current.players),
      Right(SiteRuler.Empire))

  test("on a Citizen's turn the Citizen rules the Empire's sites (R1)"):
    assertEquals(rulerOfDunes(empireAtDunes.turn(p2, Phase.Act)),
      Some(SiteRuler.Player(p2)))

  test("on an Exile's turn the Chancellor rules the Empire's sites"):
    assertEquals(rulerOfDunes(empireAtDunes.turn(p3, Phase.Act)),
      Some(SiteRuler.Player(p1)))

  test("outside any turn the Chancellor rules the Empire's sites (R2)"):
    assertEquals(rulerOfDunes(empireAtDunes.turn(p2, Phase.RoundEnd)),
      Some(SiteRuler.Player(p1)))

  test("an Exile's site and a bandit site resolve unchanged"):
    val table = Table.start.warbandsAt(dunes, p3, 1)
    assertEquals(rulerOfDunes(table), Some(SiteRuler.Player(p3)))
    assertEquals(rulerOfDunes(Table.start.bandits(dunes, 1)),
      Some(SiteRuler.Bandits))

  test("only Exile boards have a Vision slot"):
    assertEquals(Role.values.filter(_.hasVisionSlot).toVector,
      Vector(Role.Exile))

  test("the Table's Imperial step seats the roles and the purple bank"):
    val ready = empireAtDunes.ready
    assertEquals(Imperials.chancellor(ready), Some(p1))
    assertEquals(Imperials.players(ready), Vector(p1, p2))
    assertEquals(ready.banks.warbandSupply.get(ForceKind.Imperial),
      Some(MaterialBankState.ImperialWarbands))
