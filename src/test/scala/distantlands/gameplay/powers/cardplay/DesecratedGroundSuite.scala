package distantlands.gameplay.powers.cardplay

import distantlands.gameplay.{DesecratedGroundRule, SacredGroundCard}
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.{NoteText, PowerFixture,
  PowerImplementationStatus, SearchFixture, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Look, PlacementPicks, Table}
import distantlands.testkit.Unwrap.accepted

/** Desecrated Ground (E08 ruined) keeps an Exile's revealed Visions when
  * they reveal another at its site (catalog batch 11, rulings 13 and 31).
  * E08 never appears in a generated first game (the Nomad Homeland takes
  * E06), so each test places it by hand. `SearchFixture.staged` stands p1
  * at Ancient City; Broken Peaks is elsewhere.
  */
class DesecratedGroundSuite extends munit.FunSuite:
  import PowerFixture._
  import VisionPlayFixture._

  private val home = AncientCitySite.id
  private val elsewhere = BrokenPeaksSite.id
  private val held = VisionRules.Conquest
  private val drawn = VisionRules.Faith

  /** p1 at Ancient City holds Conquest revealed and has drawn Faith in a
    * Search, with E08 ruined at `site`. `more` adds to the table first. */
  private def searchHoldingConquest(site: SiteId,
      more: Table => Table = identity)(using munit.Location): OathTransition =
    val drawing = SearchFixture.staged(Vector(drawn), supply = 5)
    val table = more(Table.from(drawing)
      .edifice(SacredGroundCard.id, EdificeSide.Ruined, at = site)
      .revealedVision(actor, held))
    searched(table.ready, drawn)

  private def revealedFaith(from: OathTransition)(using munit.Location)
      : OathTransition =
    accepted(SearchFixture.place(from, drawn, PlayedAs.Faceup))

  private def visionsOf(after: OathTransition): Vector[VisionId] =
    Look(after.state).player(actor).revealedVisions.map(_.id)

  private def keptLines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(DesecratedGround.id, DesecratedGround.noteKeys, events)
      .map(_.text)

  test("a reveal at Desecrated Ground keeps the revealed Vision"):
    assertEquals(visionsOf(revealedFaith(searchHoldingConquest(home))),
      Vector[VisionId](held, drawn))

  test("at Desecrated Ground the revealed Vision spot replaces nothing"):
    val options = PlacementPicks.parkedOptions(searchHoldingConquest(home)
      .state, WalkerPowerCatalog.default(catalog))
    val spot = options.find(_.ref == DecisionOptionRef.RevealedVision(actor))
    assert(spot.nonEmpty, options.map(_.ref))
    assertEquals(spot.flatMap(PlacementPicks.replaces), None)

  test("a reveal elsewhere discards the revealed Vision"):
    assertEquals(visionsOf(revealedFaith(searchHoldingConquest(elsewhere))),
      Vector[VisionId](drawn))

  test("a third Vision at the site keeps both earlier ones"):
    val twoHeld = searchHoldingConquest(home,
      _.revealedVision(actor, VisionRules.Sanctuary))
    assertEquals(visionsOf(revealedFaith(twoHeld)),
      Vector[VisionId](held, VisionRules.Sanctuary, drawn))

  test("Desecrated Ground at a site out of play keeps nothing"):
    val ready = Table.from(SearchFixture.staged(Vector(drawn), supply = 5))
      .edifice(SacredGroundCard.id, EdificeSide.Ruined, at = home).ready
    val outOfPlay = ready.updateCurrent(current => current.copy(map =
      current.map.copy(cradle = current.map.cradle.filterNot(_ == home))))
    assert(DesecratedGroundRule.keeps(ready, actor))
    assert(!DesecratedGroundRule.keeps(outOfPlay, actor))

  test("an intact Sacred Ground keeps nothing"):
    val ready = Table.from(SearchFixture.staged(Vector(drawn), supply = 5))
      .edifice(SacredGroundCard.id, EdificeSide.Intact, at = home).ready
    assert(!DesecratedGroundRule.keeps(ready, actor))

  test("a facedown Vision turned faceup at the site keeps the revealed one"):
    val ready = Table.from(inPhase(quietTable, Phase.Act))
      .edifice(SacredGroundCard.id, EdificeSide.Ruined, at = home)
      .revealedVision(actor, held).ready
    val played = accepted(SearchFixture.place(fromAdvisers(ready, drawn),
      drawn, PlayedAs.Faceup))
    assertEquals(visionsOf(played), Vector[VisionId](held, drawn))

  test("a kept reveal writes the kept line"):
    assertEquals(keptLines(revealedFaith(searchHoldingConquest(home)).events),
      Vector(s"Kept ${held.value}."))

  test("a reveal elsewhere writes no kept line"):
    assertEquals(
      keptLines(revealedFaith(searchHoldingConquest(elsewhere)).events),
      Vector.empty)

  test("a kept line names every kept Vision, in reveal order"):
    val twoHeld = searchHoldingConquest(home,
      _.revealedVision(actor, VisionRules.Sanctuary))
    assertEquals(keptLines(revealedFaith(twoHeld).events),
      Vector(s"Kept ${held.value}, ${VisionRules.Sanctuary.value}."))

  test("an Imperial at the site keeps nothing"):
    // An Imperial board has no Vision slot, so no Imperial reveals; the rule
    // still reads the card's "If an Exile".
    val ready = Table.from(SearchFixture.staged(Vector(drawn), supply = 5))
      .edifice(SacredGroundCard.id, EdificeSide.Ruined, at = home)
      .imperial(actor).ready
    assert(!DesecratedGroundRule.keeps(ready, actor))

  test("the ruined face counts as implemented"):
    assert(PowerImplementationStatus.implemented(
      SacredGroundCard.ruined.power.id))
