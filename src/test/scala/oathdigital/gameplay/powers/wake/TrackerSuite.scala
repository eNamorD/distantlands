package oathdigital.gameplay.powers.wake

import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.phases.PhasePowerProcedure
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.action.PawnMoves
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  PhasePowerHarness, Refusals, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** Tracker (card 221, catalog batch 10, slice 14): ACTION, choose an enemy
  * pawn at your site and end your Act phase; WAKE, if you chose a pawn last
  * turn, you may place your pawn at their site (ruling 6). */
class TrackerSuite extends munit.FunSuite:
  private val tracker = TrackerCard.id
  private val source = DecisionOptionRef.Denizen(tracker)
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val rules = ActPowerHarness.rules()

  /** p1's Act at Ancient City holding Tracker faceup, with p2 there too. */
  private def actingWithP2AtTrackersSite: Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, home).pawn(p2, home)
      .adviser(p1, tracker)

  /** p1's Wake at Ancient City holding Tracker faceup, having tracked p2,
    * who now stands at `p2At`. */
  private def wakingAfterTrackingP2(p2At: SiteId): Table =
    Table.start.turn(p1, Phase.Wake).pawn(p1, home).pawn(p2, p2At)
      .adviser(p1, tracker).trackedPawn(p1, p2)

  private def startPower(ready: ReadyGame, power: PowerId) =
    rules.startWalker(Ready(MenuPlay.withoutMenu(ready)),
      ActionRef.UsePower(power), p1, Vector(source))

  private def chooseTarget(state: OathState, target: PlayerId) =
    rules.resolveWalker(state, p1, TrackerAction.decisionId,
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(target)))

  private def endWake(state: OathState) = rules.startWalker(
    MenuPlay.withoutMenu(state), PhaseTransitionRef.EndWake, p1)

  private def menuOption(ready: ReadyGame, power: PowerId)(
      using munit.Location): DecisionOption =
    MenuPlay.menuOptionFor(catalog, ready, ActionRef.UsePower(power),
      Vector(source))

  /** The written reason a menu option is unavailable for, if it is. */
  private def writtenReason(option: DecisionOption): Option[String] =
    option match
      case DecisionOption.Unavailable(_, OptionReason.Written(text)) =>
        Some(text)
      case _ => None

  // ---- ACTION ----

  test("the ACTION records the chosen enemy and ends the Act phase, and " +
      "the record outlives the turn"):
    val ready = actingWithP2AtTrackersSite.ready
    val parked = accepted(startPower(ready, TrackerAction.id))
    val done = accepted(chooseTarget(parked.state, p2))
    val events = parked.events ++ done.events
    val atRest = ActPowerHarness.replayed(rules, ready,
      ActPowerHarness.untilWalkEnds(events))
    assertEquals(Look(atRest).phase, Phase.Rest)
    assertEquals(atRest.game.current.trackedPawns, Map(p1 -> p2))
    // No REST power is usable, so the driver finishes Rest and passes the
    // turn. The record stays until p1's next Act.
    val end = Outcomes.ready(done.state)
    assertNotEquals(Look(end).active, p1)
    assertEquals(end.game.current.trackedPawns, Map(p1 -> p2))
    assertEquals(NoteText.said(TrackerAction, events), Vector(
      NoteText.Said(NoteKey.Used, s"Tracked ${p2.value}.", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, events), end)
    assertWireRoundTrips(events)

  test("the ACTION offers only enemies at the user's site"):
    val ready = actingWithP2AtTrackersSite.pawn(p3, peaks).ready
    val parked = accepted(startPower(ready, TrackerAction.id))
    assertEquals(chooseTarget(parked.state, p3).left.toOption,
      Some(Refusals.notOffered(TrackerAction.decisionId)))

  test("with no enemy pawn at the site, the ACTION is unavailable with its " +
      "reason"):
    val alone = Table.start.turn(p1, Phase.Act).pawn(p1, home)
      .pawn(p2, peaks).adviser(p1, tracker).ready
    assertEquals(writtenReason(menuOption(alone, TrackerAction.id)),
      Some(PawnMoves.noValidEnemyPawn))
    // A Citizen is no enemy of the Chancellor.
    val allied = actingWithP2AtTrackersSite.imperial(p1, p2).ready
    assertEquals(writtenReason(menuOption(allied, TrackerAction.id)),
      Some(PawnMoves.noValidEnemyPawn))

  // ---- WAKE ----

  test("the WAKE power is offered, not forced, and places the pawn at the " +
      "tracked pawn's site now, with its line"):
    val ready = wakingAfterTrackingP2(peaks).ready
    assert(!TrackerWake.forced)
    assert(menuOption(ready, TrackerWake.id).available)
    val done = accepted(startPower(ready, TrackerWake.id))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).pawn(p1), peaks)
    // Nothing else is offered, so the driver ends Wake, which clears the
    // spent record as the Act phase begins.
    assertEquals(Look(after).phase, Phase.Act)
    assertEquals(after.game.current.trackedPawns, Map.empty)
    assertEquals(NoteText.said(TrackerWake, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${p1.value} placed at ${peaks.value}.",
        covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events), after)
    assertWireRoundTrips(done.events)

  test("the WAKE power is unavailable with no record, or with the pawn " +
      "already there"):
    val unrecorded = Table.start.turn(p1, Phase.Wake).pawn(p1, home)
      .pawn(p2, peaks).adviser(p1, tracker).ready
    assertEquals(writtenReason(menuOption(unrecorded, TrackerWake.id)),
      Some(TrackerWake.noPawnChosen))
    assertEquals(writtenReason(menuOption(wakingAfterTrackingP2(home).ready,
      TrackerWake.id)), Some(TrackerWake.alreadyThere))

  test("with Tracker lost or facedown by Wake, the record has no source"):
    val lost = wakingAfterTrackingP2(peaks).withoutAdvisers(p1).ready
    val facedown = Table.start.turn(p1, Phase.Wake).pawn(p1, home)
      .pawn(p2, peaks).adviser(p1, tracker, facedown = true)
      .trackedPawn(p1, p2).ready
    Vector(lost, facedown).foreach { ready =>
      assert(PhasePowerProcedure.sources(catalog, ready, p1, TrackerWake)
        .isEmpty)
      assert(startPower(ready, TrackerWake.id).isLeft)
    }

  // ---- The clear at Act ----

  test("End Wake clears the holder's unused record, and no other"):
    val ready = wakingAfterTrackingP2(peaks).trackedPawn(p2, p1).ready
    val unused = accepted(endWake(Ready(ready)))
    val afterUnused = Outcomes.ready(unused.state)
    assertEquals(Look(afterUnused).phase, Phase.Act)
    assertEquals(Look(afterUnused).pawn(p1), home)
    assertEquals(afterUnused.game.current.trackedPawns, Map(p2 -> p1))
    assertEquals(PhasePowerHarness.ops(unused.events).collect {
      case op: ClearTrackedPawn => op }, Vector(ClearTrackedPawn(p1)))
    assertEquals(ActPowerHarness.replayed(rules, ready, unused.events),
      afterUnused)

  test("End Wake with no record clears nothing"):
    val ready = Table.start.turn(p1, Phase.Wake).pawn(p1, home).ready
    val ended = accepted(endWake(Ready(ready)))
    assert(!PhasePowerHarness.ops(ended.events)
      .exists(_.isInstanceOf[ClearTrackedPawn]))
