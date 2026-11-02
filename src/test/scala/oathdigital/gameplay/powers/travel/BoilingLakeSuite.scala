package oathdigital.gameplay.powers.travel

import oathdigital.gameplay.operations.BoilingLakeCard
import oathdigital.gameplay.powers.{NoteText, PowerFixture}
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, Look, Outcomes, PhasePowerHarness,
  Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Unwrap.accepted

/** Boiling Lake (catalog batch 9, slice 4): a traveller who does not rule
  * the Lake's site kills 2 of their board's warbands on arrival, if able. */
class BoilingLakeSuite extends munit.FunSuite:
  import PowerFixture._
  import TravelFixture._

  /** The actor at the Cradle's plains with `warbands` on their board; the
    * Lake on `side` at the coast, which `ruled` settles (bandits hold it
    * by default). */
  private def lakeAtCoast(warbands: Int, side: EdificeSide = EdificeSide.Intact,
      ruled: Table => Table = identity): ReadyGame =
    on(board(supply = 7))(table => ruled(table
      .edifice(BoilingLakeCard.id, side, at = coast)
      .boardWarbands(actor, warbands)))

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(BoilingLakeTravel.id, BoilingLakeTravel.noteKeys, events)

  private def kills(events: Vector[OathEvent]): Vector[Kill] =
    PhasePowerHarness.ops(events).collect { case kill: Kill => kill }

  test("arriving without ruling the Lake kills 2 of the traveller's board " +
      "warbands, with its line, and the journal replays"):
    val ready = lakeAtCoast(warbands = 3)
    val done = accepted(travel(ready, coast))
    val result = Outcomes.ready(done.state)
    assertEquals(Look(result).pawn(actor), coast)
    assertEquals(Look(result).boardWarbands(actor), 1)
    assertEquals(said(done.events), Vector(NoteText.Said("killed",
      s"Killed 2 ${actor.value} warbands.", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events), result)
    assertWireRoundTrips(done.events)

  test("if able: one warband dies when one is held, none when none"):
    val one = accepted(travel(lakeAtCoast(warbands = 1), coast))
    assertEquals(Look(Outcomes.ready(one.state)).boardWarbands(actor), 0)
    assertEquals(said(one.events), Vector(NoteText.Said("killed",
      s"Killed 1 ${actor.value} warband.", covers = false)))
    val none = accepted(travel(lakeAtCoast(warbands = 0), coast))
    assertEquals(kills(none.events), Vector.empty)
    assertEquals(said(none.events), Vector.empty)

  test("the Lake's ruler arrives unharmed"):
    val ready = lakeAtCoast(warbands = 3, ruled = _.warbandsAt(coast, actor, 1))
    val done = accepted(travel(ready, coast))
    assertEquals(Look(Outcomes.ready(done.state)).boardWarbands(actor), 3)
    assertEquals(kills(done.events), Vector.empty)

  test("a Travel elsewhere, and the ruined face, kill nothing"):
    val elsewhere = accepted(travel(lakeAtCoast(warbands = 3), hinterlandPlains))
    assertEquals(kills(elsewhere.events), Vector.empty)
    val ruined = accepted(travel(lakeAtCoast(warbands = 3,
      side = EdificeSide.Ruined), coast))
    assertEquals(kills(ruined.events), Vector.empty)
    assertEquals(said(ruined.events), Vector.empty)
