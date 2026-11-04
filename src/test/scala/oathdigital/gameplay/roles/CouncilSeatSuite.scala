package oathdigital.gameplay.roles

import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.actions.minor.SelfExile
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Look, MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Council Seat (card 123): "If you're a Citizen, you cannot be exiled,
  * even by yourself." (catalog batch 10, slice 1, ruling 8). Every exile
  * path stays choosable with a warning, and the exile is skipped. */
class CouncilSeatSuite extends munit.FunSuite:
  private val gameId = "council-seat"

  /** p1 is the Chancellor and p2 a Citizen in their Act, holding Council
    * Seat as an adviser (faceup unless `facedown`), with 4 board warbands
    * and 6 favor. */
  private def citizenHoldingSeat(facedown: Boolean = false): Table =
    Table.start.imperial(p1, p2).turn(p2, Phase.Act)
      .adviser(p2, CouncilSeatCard, facedown = facedown)
      .boardWarbands(p2, 4).favor(p2, 6)

  /** The warnings an option carries, through the menu's wrappers. */
  private def warningsOf(option: DecisionOption): Vector[OptionReason] =
    option match
      case DecisionOption.Warned(inner, reason) => reason +: warningsOf(inner)
      case DecisionOption.Grouped(inner, _) => warningsOf(inner)
      case DecisionOption.Badged(inner, _) => warningsOf(inner)
      case DecisionOption.Unavailable(inner, _) => warningsOf(inner)
      case _ => Vector.empty

  private val seatWarning = OptionReason.Written(CouncilSeat.warning)

  test("the Act menu's self-exile carries the Seat's warning"):
    val option = MenuPlay.menuOptionFor(catalog, citizenHoldingSeat().ready,
      ActionRef.SelfExile)
    assert(option.available, option)
    assertEquals(warningsOf(option), Vector(seatWarning))

  test("a facedown Seat warns of nothing"):
    val option = MenuPlay.menuOptionFor(catalog,
      citizenHoldingSeat(facedown = true).ready, ActionRef.SelfExile)
    assertEquals(warningsOf(option), Vector.empty)

  test("self-exile still pays the Chancellor, and the Citizen stays one"):
    val table = citizenHoldingSeat()
    val chancellorFavor = Look(table.ready).player(p1).board.favor
    val (service, _) = table.service()
    val done = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p2, ActionRef.SelfExile)))
    val after = Outcomes.ready(done.state)
    assertEquals(Imperials.role(after, p2), Some(Role.Citizen))
    assertEquals(Look(after).player(p2).board.favor, 2)
    assertEquals(Look(after).player(p1).board.favor, chancellorFavor + 4)
    assertEquals(NoteText.said(ProcedureNotes.id(ActionRef.SelfExile),
      SelfExile.keys, done.events), Vector(NoteText.Said("stayed",
      s"Gave 4 favor to ${p1.value} and stayed a Citizen.", covers = false)))

  test("with the Seat facedown, self-exile exiles"):
    val (service, _) = citizenHoldingSeat(facedown = true).service()
    val done = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p2, ActionRef.SelfExile)))
    assertEquals(Imperials.role(Outcomes.ready(done.state), p2),
      Some(Role.Exile))

  test("the Seat guards only a Citizen who holds it faceup"):
    val ready = citizenHoldingSeat().ready
    assert(CouncilSeat.guards(ready, p2))
    assert(!CouncilSeat.guards(ready, p1))
    assert(!CouncilSeat.guards(citizenHoldingSeat(facedown = true).ready,
      p2))
