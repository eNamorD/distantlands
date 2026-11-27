package distantlands.gameplay.powers.action

import distantlands.gameplay.powers.{NoteText, PowerFixture}
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, Table}
import distantlands.testkit.Outcomes.{assertWireRoundTrips, ready}
import distantlands.testkit.Unwrap.accepted

class DemonTailSuite extends munit.FunSuite:
  import ActPowerHarness._
  import PowerFixture._

  private val tail = DemonTailCard.id
  private val source = DecisionOptionRef.Relic(tail)

  /** p1's Act holding Demon Tail, with `supply` Supply and `favor`
    * favor. */
  private def staged(supply: Int = 2, favor: Int = 3): ReadyGame =
    Table.start.turn(actor, Phase.Act).relic(actor, tail)
      .supply(actor, supply).favor(actor, favor).ready

  test("it burns three favor and gains 2 Supply"):
    // 2 Supply + 2 = 4; 3 favor - 3 burnt = 0.
    val ready0 = staged(supply = 2, favor = 3)
    val done = accepted(use(rules(), ready0, DemonTail.id, source))
    val end = ready(done.state)
    assertEquals(Look(end).favor(actor), 0)
    // Burnt favor does not go onto the relic.
    assertEquals(Look(end).tokensOn(tail), Tokens.empty)
    assertEquals(Look(end).player(actor).board.supply, SupplyTrack(4))
    assertEquals(replayed(rules(), ready0, done.events), end)
    assertWireRoundTrips(done.events)

  test("the gain is clamped at the track maximum, and the line says so"):
    val done = accepted(use(rules(), staged(supply = SupplyTrack.Maximum - 1),
      DemonTail.id, source))
    assertEquals(Look(ready(done.state)).player(actor).board.supply,
      SupplyTrack.full)
    assertEquals(NoteText.said(DemonTail, done.events), Vector(NoteText.Said(
      NoteKey.Used, s"${actor.value} gained 1 Supply.", covers = false)))

  test("two favor are not enough"):
    val broke = staged(favor = 2)
    assert(!usableIds(broke).contains(DemonTail.id))
    assertEquals(use(rules(), broke, DemonTail.id, source).left.toOption,
      Some(OathViolation.InsufficientFavor(3, 2)))

  test("its line reads the Supply gained"):
    val done = accepted(use(rules(), staged(), DemonTail.id, source))
    assertEquals(NoteText.said(DemonTail, done.events), Vector(NoteText.Said(
      NoteKey.Used, s"${actor.value} gained 2 Supply.", covers = false)))
