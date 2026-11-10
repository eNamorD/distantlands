package distantlands.gameplay

import distantlands.gameplay.actions.minor.SelfExile
import distantlands.gameplay.phases.ActOffers
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Look, MenuPlay, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Self-exile (CR p. 34): as a minor action a Citizen gives the Chancellor
  * 1 favor per Imperial warband on their board, at least 3, and becomes an
  * Exile (Empire slice 3a). */
class SelfExileSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val gameId = "self-exile"

  /** p1 is the Chancellor and p2 a Citizen in their Act, with `warbands`
    * on their board and `favor` favor. */
  private def citizen(warbands: Int, favor: Int): Table =
    Table.start.imperial(p1, p2).turn(p2, Phase.Act)
      .boardWarbands(p2, warbands).favor(p2, favor)

  test("the price is one favor per board warband, at least 3"):
    assertEquals(SelfExile.price(citizen(1, 5).ready, p2), 3)
    assertEquals(SelfExile.price(citizen(5, 5).ready, p2), 5)

  test("a Citizen pays the Chancellor and becomes an Exile"):
    val table = citizen(4, 6)
    val before = Look(table.ready).player(p1).board.favor
    val (service, _) = table.service()
    val done = accepted(service.handle(gameId, 0L,
      MenuPlay.starting(p2, ActionRef.SelfExile)))
    val after = Outcomes.ready(done.state)
    assertEquals(Imperials.role(after, p2), Some(Role.Exile))
    assertEquals(Look(after).player(p2).board.favor, 2)
    assertEquals(Look(after).player(p1).board.favor, before + 4)
    assertEquals(Look(after).player(p2).board.warbands, 4)
    // Their Act ended at once: Rest ran and p3's turn began.
    assertEquals(after.game.current.turn.activePlayer, p3)

  test("a Citizen short of favor sees why"):
    assertEquals(rules.startWalker(citizen(4, 3).state, ActionRef.SelfExile,
      p2).left.map(ActOffers.reason(ActionRef.SelfExile, _)),
      Left("You need 4 favor to give the Chancellor"))

  test("only a Citizen's menu lists it"):
    val ready = citizen(4, 6).ready
    def lists(player: PlayerId) = ActOffers.offeredTo(ready, player)
      .exists(_.procedure == ActionRef.SelfExile)
    assert(lists(p2))
    assert(!lists(p1))
    assert(!lists(p3))
