package oathdigital.gameplay.powers.action

import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.campaign.ObsidianCageCard
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, DistributeAnswer}
import oathdigital.testkit.{Look, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Obsidian Cage's Action: its holder moves caged warbands onto the boards
  * of their force. */
class ObsidianCageActionSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val cage = ObsidianCageCard.id
  private val source = DecisionOptionRef.Relic(cage)

  private def caged(ready: ReadyGame): Map[ForceKind, Int] =
    ready.game.current.warbandsOnCards.getOrElse(cage, Map.empty)

  private def kindOf(ready: ReadyGame, player: PlayerId): ForceKind =
    PlayerForceKind.of(ready, Look(ready).player(player)).get

  /** p1's Act holding Obsidian Cage faceup, with p2's warbands on it and
    * none on p2's board. */
  private def holdingCaged(p2Caged: Int): Table = Table.start
    .turn(p1, Phase.Act).relic(p1, cage).boardWarbands(p2, 0)
    .warbandsOnCard(cage, p2, p2Caged)

  test("one board: the holder returns some of the caged warbands to it"):
    val ready = holdingCaged(3).ready
    val started = accepted(use(ready, ObsidianCageAction, source))
    assert(awaits(started, ObsidianCageAction.decisionId))
    val done = accepted(answer(started, p1, ObsidianCageAction.decisionId,
      ChooseAmountAnswer(2)))
    parked.assertResumed(done.state, Phase.Act, p1)
    val end = after(done)
    assertEquals(Look(end).player(p2).board.warbands, 2)
    assertEquals(caged(end), Map(kindOf(ready, p2) -> 1))
    val events = started.events ++ done.events
    assertEquals(NoteText.said(ObsidianCageAction, events), Vector(
      NoteText.Said(NoteKey.Used,
        s"Returned 2 warbands from Obsidian Cage to ${p2.value}.",
        covers = false)))
    assertEquals(replayed(ready, events), Right(done.state))
    assertWireRoundTrips(events)

  test("several boards: the holder distributes, taking back their own"):
    val ready = holdingCaged(2).boardWarbands(p1, 0)
      .warbandsOnCard(cage, p1, 1).ready
    val started = accepted(use(ready, ObsidianCageAction, source))
    val done = accepted(answer(started, p1, ObsidianCageAction.decisionId,
      DistributeAnswer(Vector(
        DistributeAmount(DecisionOptionRef.Player(p1), 1),
        DistributeAmount(DecisionOptionRef.Player(p2), 2)))))
    val end = after(done)
    assertEquals(Look(end).player(p1).board.warbands, 1)
    assertEquals(Look(end).player(p2).board.warbands, 2)
    assertEquals(caged(end), Map.empty[ForceKind, Int])
    assertEquals(NoteText.said(ObsidianCageAction,
      started.events ++ done.events), Vector(
      NoteText.Said("used.own", "Took back 1 warband from Obsidian Cage.",
        covers = false),
      NoteText.Said(NoteKey.Used,
        s"Returned 2 warbands from Obsidian Cage to ${p2.value}.",
        covers = false)))

  test("the Action is offered only while the Cage holds warbands"):
    val ready = Table.start.turn(p1, Phase.Act).relic(p1, cage).ready
    assert(!usableNow(ready).exists(_.power == ObsidianCageAction))
    assert(usableNow(holdingCaged(1).ready)
      .contains(MenuPower(ObsidianCageAction, source)))
