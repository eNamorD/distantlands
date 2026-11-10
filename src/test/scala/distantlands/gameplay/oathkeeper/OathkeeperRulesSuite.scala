package distantlands.gameplay.oathkeeper

import distantlands.gameplay.cards.{DeepWoodsSite, DunesSite, FairIsleSite}
import distantlands.gameplay.oathkeeper.OathkeeperOutcome._
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

class OathkeeperRulesSuite extends munit.FunSuite:
  /** Each of `rulers` rules one site with a warband: the first Dunes, the
    * second Fair Isle. `holder` holds the title on `side`. */
  private def outcome(rulers: Vector[PlayerId],
      holder: Option[PlayerId] = None, side: TitleSide = TitleSide.Oathkeeper) =
    val table = rulers.zip(Vector(DunesSite, FairIsleSite)).foldLeft(
      Table.start.oathkeeper(holder, side)) { case (t, (ruler, site)) =>
        t.warbandsAt(site, ruler, 1) }
    OathkeeperRules.outcome(table.ready)

  test("a holder who still leads keeps the title, even when tied"):
    assertEquals(outcome(Vector(p1), holder = Some(p1)), NoChange)
    // Pins row order: a tied holder who leads must not be offered a choice.
    assertEquals(outcome(Vector(p1, p2), holder = Some(p1)), NoChange)

  test("a holder tied out of the lead chooses among the leaders, in seat order"):
    assertEquals(outcome(Vector(p2, p1), holder = Some(p3)),
      Choose(p3, Vector(p1, p2)))

  test("a single leader takes the title from a holder or from the bank"):
    assertEquals(outcome(Vector(p2), holder = Some(p1)), Transfer(Some(p2)))
    assertEquals(outcome(Vector(p2)), Transfer(Some(p2)))

  test("a holder with nobody leading returns the title to the bank"):
    assertEquals(outcome(Vector.empty, holder = Some(p1)), Transfer(None))

  test("with nobody holding it, no leader or a tie changes nothing"):
    assertEquals(outcome(Vector.empty), NoChange)
    assertEquals(outcome(Vector(p1, p2)), NoChange)

  test("the Usurper side is not consulted"):
    assertEquals(outcome(Vector(p2), holder = Some(p1),
      side = TitleSide.Usurper), Transfer(Some(p2)))

  /** p1 is the Chancellor and p2 a Citizen, with the Empire holding
    * `empire` sites, and p3, an Exile, holding `exile` sites. Supremacy is
    * the goal. */
  private def empireAgainstExile(empire: Int, exile: Int,
      holder: Option[PlayerId] = None): OathkeeperOutcome =
    val sites = Vector(DunesSite, FairIsleSite, DeepWoodsSite)
    val withEmpire = sites.take(empire).foldLeft(
      Table.start.imperial(p1, p2).oathkeeper(holder))((t, site) =>
        t.warbandsAt(site, p1, 1))
    val table = sites.drop(empire).take(exile).foldLeft(withEmpire)(
      (t, site) => t.warbandsAt(site, p3, 1))
    OathkeeperRules.outcome(table.ready)

  test("the Chancellor holds Supremacy when the Empire has the most sites"):
    assertEquals(empireAgainstExile(2, 1), Transfer(Some(p1)))

  test("a Citizen holding Supremacy loses it to the Chancellor"):
    assertEquals(empireAgainstExile(2, 1, holder = Some(p2)),
      Transfer(Some(p1)))

  test("an Exile with more sites than the Empire takes Supremacy"):
    assertEquals(empireAgainstExile(1, 2), Transfer(Some(p3)))
