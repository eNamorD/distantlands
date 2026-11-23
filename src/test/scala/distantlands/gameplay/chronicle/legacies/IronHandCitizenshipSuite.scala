package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.negotiation.NegotiationDeal
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powerresolver.{Contribution, ExecutedBatch,
  ExecutedTrigger}
import distantlands.gameplay.powers.{NoteText, PhasePowerCatalog}
import distantlands.gameplay.powers.action.GrandScepterAction
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, ChooseManyAnswer,
  ProposeTerms}
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, Journal, Outcomes, PowerRules,
  RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Iron Hand's discard when its holder becomes Imperial during a game:
  * "If you are Imperial, discard Iron Hand" (Legacy in-play powers ruling
  * 22; Legacy Chronicle powers ruling 18; R12). */
class IronHandCitizenshipSuite extends munit.FunSuite:
  private val scepterRules = ActPowerHarness.rules()
  private val dealRules = new OathRules(catalog)
  private val scepter = DecisionOptionRef.Relic(TheGrandScepterCard.id)
  private val discard = Move(Piece.Card(IronHandCard.id),
    PositionedLocation(Location.Legacies(LineageId("l1"))),
    PositionedLocation.legacyDeckBottom)

  /** GrandScepterActionSuite's table: p2 the Chancellor; p1, an Exile in
    * their Act at Broken Peaks, holds the Grand Scepter and 2 board
    * warbands, and Iron Hand, active unless `active` says otherwise. */
  private def scepterHolderWithIronHand(active: Boolean = true): Table =
    Table.start.imperial(p2).turn(p1, Phase.Act)
      .pawn(p1, at = BrokenPeaksSite).relic(p1, TheGrandScepterCard)
      .boardWarbands(p1, 2).legacy(p1, IronHandCard, active)
      .allowingCitizenshipPowers

  /** NegotiationCitizenshipSuite's table: p1 the Chancellor at Broken
    * Peaks with the Grand Scepter, beside p2 and p3, Exiles with 2 board
    * warbands each, p2 holding Iron Hand active; the purple bank holds 10. */
  private def dealWithIronHandHolder: Table = Table.start.imperial(p1)
    .turn(p1, Phase.Act).pawn(p1, at = BrokenPeaksSite)
    .pawn(p2, at = BrokenPeaksSite).pawn(p3, at = BrokenPeaksSite)
    .relic(p1, TheGrandScepterCard)
    .boardWarbands(p2, 2).boardWarbands(p3, 2)
    .boardWarbands(p1, MaterialBankState.ImperialWarbands - 10)
    .legacy(p2, IronHandCard).allowingCitizenshipPowers

  private def holds(ready: ReadyGame, player: PlayerId): Boolean =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies.exists(_.id == IronHandCard.id)

  private def lines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(IronHand.id, IronHand.noteKeys, events).map(_.text)

  private def scepterUsed(table: Table): (ReadyGame, Vector[OathEvent]) =
    val start = table.ready
    val done = accepted(ActPowerHarness.use(scepterRules, start,
      GrandScepterAction.id, scepter))
    (ActPowerHarness.replayed(scepterRules, start,
      ActPowerHarness.untilWalkEnds(done.events)), done.events)

  test("becoming a Citizen by the Grand Scepter's Action discards it, " +
      "right after the role changes"):
    val (after, events) = scepterUsed(scepterHolderWithIronHand())
    assertEquals(Imperials.role(after, p1), Some(Role.Citizen))
    assert(!holds(after, p1))
    val ops = RecordedOps.all(events)
    val changed = ops.indexOf(RoleChanged(LineageId("l1"), Role.Citizen))
    assert(changed >= 0, ops)
    assertEquals(ops.lift(changed + 1), Some(discard))
    assertEquals(lines(events),
      Vector(s"p1 discarded ${IronHandCard.id.value} on becoming a Citizen"))

  test("becoming a Citizen by a deal discards it"):
    val ready = dealWithIronHandHolder.ready
    val started = accepted(dealRules.startWalker(Ready(ready),
      ActionRef.Negotiation, p1))
    val gathered = accepted(dealRules.resolveWalker(started.state, p1,
      NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(Vector(
        DecisionOptionRef.Player(p2), DecisionOptionRef.Player(p3)))))
    val (end, events) = Vector[(PlayerId, DecisionAnswer)](
      p1 -> ProposeTerms(NegotiationTerms(citizenship = Vector(p2))),
      p1 -> AcceptDeal, p2 -> AcceptDeal, p3 -> AcceptDeal)
      .foldLeft((gathered.state, Vector.empty[OathEvent])) {
        case ((state, events), (by, answer)) =>
          val next = accepted(dealRules.resolveWalker(state, by,
            NegotiationDeal.dealDecisionId, answer))
          (next.state, events ++ next.events)
      }
    val after = Outcomes.ready(end)
    assertEquals(Imperials.role(after, p2), Some(Role.Citizen))
    assert(!holds(after, p2))
    assertEquals(lines(events),
      Vector(s"p2 discarded ${IronHandCard.id.value} on becoming a Citizen"))

  test("with no Empire, becoming the Chancellor discards it, and the line " +
      "says so"):
    val table = Table.start.turn(p1, Phase.Act)
      .pawn(p1, at = BrokenPeaksSite).relic(p1, TheGrandScepterCard)
      .legacy(p1, IronHandCard).allowingCitizenshipPowers
    val (after, events) = scepterUsed(table)
    assertEquals(Imperials.chancellor(after), Some(p1))
    assert(!holds(after, p1))
    assertEquals(lines(events), Vector(
      s"p1 discarded ${IronHandCard.id.value} on becoming the Chancellor"))

  test("a dormant Iron Hand stays (ruling 1)"):
    val (after, events) = scepterUsed(scepterHolderWithIronHand(
      active = false))
    assertEquals(Imperials.role(after, p1), Some(Role.Citizen))
    assert(holds(after, p1))
    assertEquals(lines(events), Vector.empty)

  /** Parks right after a batch that changes a role, as a follow-up (a
    * False Prophet claim) may, so the change's line is written on resume. */
  private case object ParkAfterRoleChange extends ExecutedTrigger:
    val id: PowerId = PowerId("test.park-after-role-change")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      Option.when(batch.executed.exists(_.isInstanceOf[RoleChanged]))(
        Decide(parkId, p1, DecisionQuery.ChooseOne(
          Vector(DecisionOption.Button(DecisionOptionRef.Button("ok"),
            "OK")))))
  private val parkId = "test.after-role-change"

  test("a walk parked right after the change writes Iron Hand's line once, " +
      "from the change's record, on resume"):
    val parking = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowers(Vector(ParkAfterRoleChange),
        declared = PowerRules.declared),
      phasePowerCatalog = PhasePowerCatalog.default)
    val start = scepterHolderWithIronHand().ready
    val parked = accepted(ActPowerHarness.use(parking, start,
      GrandScepterAction.id, scepter))
    val at = accepted(Journal.evolved(parking, start, parked.events))
    val resumed = accepted(parking.resolveWalker(at, p1, parkId,
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Button("ok"))))
    val events = parked.events ++ resumed.events
    assertEquals(lines(events),
      Vector(s"p1 discarded ${IronHandCard.id.value} on becoming a Citizen"))
    assert(!holds(Outcomes.ready(resumed.state), p1))
