package oathdigital.gameplay

import oathdigital.gameplay.cards.{DunesSite, FairIsleSite}
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

class EmpireRulerSuite extends munit.FunSuite:
  private val dunes = DunesSite.id
  private val fairIsle = FairIsleSite.id

  /** p1 is the Chancellor and p2 a Citizen; the Empire holds the Dunes and
    * p3, an Exile, holds Fair Isle. */
  private def empireAndExileSites: Table =
    Table.start.imperial(p1, p2).warbandsAt(dunes, p1, 2)
      .warbandsAt(fairIsle, p3, 1)

  test("a Citizen rules the Empire's sites on their own turn"):
    val ready = empireAndExileSites.turn(p2, Phase.Act).ready
    assertEquals(PowerAccess.ruledSites(ready, p2), Set(dunes))
    assertEquals(PowerAccess.ruledSites(ready, p1), Set.empty[SiteId])

  test("the Chancellor rules the Empire's sites on an Exile's turn"):
    val ready = empireAndExileSites.turn(p3, Phase.Act).ready
    assertEquals(PowerAccess.ruledSites(ready, p1), Set(dunes))
    assertEquals(PowerAccess.ruledSites(ready, p3), Set(fairIsle))

  test("a card at an Empire site has the governor as its ruler"):
    val ready = empireAndExileSites.turn(p2, Phase.Act).ready
    assertEquals(SiteRulers.rulerOf(ready, dunes),
      Some(SiteRuler.Player(p2)))

  test("an Empire site protects every Imperial player"):
    val ready = empireAndExileSites.turn(p3, Phase.Act).ready
    assertEquals(SiteRulers.protectedAt(ready, dunes), Set(p1, p2))
    assertEquals(SiteRulers.protectedAt(ready, fairIsle), Set(p3))
