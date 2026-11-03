package oathdigital.gameplay.powers.action

import oathdigital.gameplay.{OathRules, PowerAccess}
import oathdigital.gameplay.actions.campaign.{CampaignIds, CampaignSetup}
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite}
import oathdigital.gameplay.powers.{PhasePowerCatalog, WalkerPowerCatalog}
import oathdigital.gameplay.powers.campaign.{PlanDriver, WrestlersCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseAmountAnswer
import oathdigital.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  ParkedNode, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Captains (card 115): "ACTION: Campaign at any site you rule. Act as if
  * your pawn is there. Spend no Supply and add your warbands there to your
  * force." (catalog batch 8, slice 13). */
class CaptainsSuite extends munit.FunSuite:
  private val card = CaptainsCard.id
  private val source = DecisionOptionRef.Denizen(card)
  private val home = AncientCitySite.id
  /** In the Provinces, away from p1's Cradle, with no pawn. */
  private val wilds = DeepWoodsSite.id

  /** Captains at Ancient City, which p1 rules with 1 warband and where p1
    * pays a favor; p1 also rules the wilds with `atWilds` warbands, where
    * p2's pawn stands, and holds `onBoard` on the board. */
  private def captainsAtHomeWithTheRivalAtARuledSite(onBoard: Int = 3,
      atWilds: Int = 2): Table = Table.start
    .denizen(card, at = home).warbandsAt(home, p1, 1).favor(p1, 1)
    .warbandsAt(wilds, p1, atWilds).boardWarbands(p1, onBoard)
    .pawn(p2, at = wilds)

  private def menuOption(ready: ReadyGame): DecisionOption =
    MenuPlay.menuOptionFor(catalog, ready, ActionRef.UsePower(Captains.id),
      Vector(source))

  /** The decision `state` is parked on, rebuilt with every production
    * power. */
  private def parkedDecide(state: OathState): Decide =
    ParkedNode.of(state, catalog, WalkerPowerCatalog.default(catalog),
        PhasePowerCatalog.default) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide
      case other => fail(s"expected a parked decision, got $other")

  /** The use answered with the wilds, as a Campaign run. */
  private def campaignAtTheWilds(rules: OathRules,
      ready: ReadyGame): (OathTransition, OathTransition) =
    val asked = accepted(ActPowerHarness.use(rules, ready, Captains.id,
      source))
    (asked, accepted(ActPowerHarness.answer(rules, asked.state,
      Captains.siteDecisionId, DecisionOptionRef.Site(wilds))))

  /** `run` with every plan window finished and no sacrifice, stopping at
    * the placement question or wherever the Campaign asks something else. */
  private def finishedPlansAndSacrifice(run: PlanDriver.Run): PlanDriver.Run =
    if PlanDriver.awaits(run, p1, CampaignIds.attackerPlan) then
      finishedPlansAndSacrifice(run.pick(p1, CampaignIds.attackerPlan,
        CampaignIds.finish))
    else if PlanDriver.awaits(run, p2, CampaignIds.defenderPlan) then
      finishedPlansAndSacrifice(run.pick(p2, CampaignIds.defenderPlan,
        CampaignIds.finish))
    else if PlanDriver.awaits(run, p1, CampaignIds.sacrifice) then
      finishedPlansAndSacrifice(run.answer(p1, CampaignIds.sacrifice,
        ChooseAmountAnswer(0)))
    else run

  test("the site question lists the sites the user rules, their own pawn " +
      "site included"):
    val ready = captainsAtHomeWithTheRivalAtARuledSite().ready
    assertEquals(PowerAccess.ruledSites(ready, p1), Set(home, wilds))
    val asked = accepted(ActPowerHarness.use(
      ActPowerHarness.rulesWithPrintedRestrictions(), ready, Captains.id,
      source))
    val decide = parkedDecide(asked.state)
    assertEquals(decide.decisionId, Captains.siteDecisionId)
    decide.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), ready.game.current.map.inPlay
          .filter(site => site == home || site == wilds)
          .map(DecisionOptionRef.Site(_)))
        assertEquals(one.options.find(_.ref == DecisionOptionRef.Site(home)),
          Some(DecisionOption.Unavailable(DecisionOption.Site(
            DecisionOptionRef.Site(home)), OptionReason.Written(
            "Nothing to conquer and no enemy pawn to raid"))))
        assert(one.options.find(_.ref == DecisionOptionRef.Site(wilds))
          .exists(_.available))
      case other => fail(s"expected a choose-one, got $other")

  test("ruling no site where a Campaign could start, the ACTION is " +
      "unavailable with the reason"):
    val ready = captainsAtHomeWithTheRivalAtARuledSite()
      .pawn(p2, at = BrokenPeaksSite.id).ready
    assertEquals(menuOption(ready), DecisionOption.Unavailable(
      DecisionOption.Denizen(source), OptionReason.Written(
        "No site you rule where a Campaign could start")))

  test("the Campaign from a ruled site adds its warbands, spends no Supply " +
      "and pays the favor"):
    val rules = ActPowerHarness.rulesWithPrintedRestrictions()
    val (_, queued) = campaignAtTheWilds(rules,
      captainsAtHomeWithTheRivalAtARuledSite().ready)
    val live = Outcomes.ready(queued.state)
    assertEquals(CampaignSetup.originOf(live, p1), Some(wilds))
    assertEquals(CampaignSetup.atOrigin(live, p1), 2)
    assertEquals(Look(live).supply(p1), 7)
    assertEquals(Look(live).favor(p1), 0)
    assertEquals(Look(live).tokensOn(card), Tokens(1, 0))

  test("a defeat with a real choice splits the dead between board and " +
      "site"):
    // Four committed, two hollow swords against the pawn's two blank dice
    // and p2's three board warbands: a defeat, so two die.
    val rules = ActPowerHarness.rulesWithPrintedRestrictions(
      PlanDriver.losingDice)
    val (_, queued) = campaignAtTheWilds(rules,
      captainsAtHomeWithTheRivalAtARuledSite(onBoard = 1, atWilds = 3).ready)
    val fought = PlanDriver.Run(rules, queued, queued.events)
      .answer(p1, CampaignIds.force, ChooseAmountAnswer(4)).finishPlanWindows
    assert(PlanDriver.awaits(fought, p1, CampaignIds.originDeaths))
    val done = fought.answer(p1, CampaignIds.originDeaths,
      ChooseAmountAnswer(1))
    assertEquals(Look(done.state).boardWarbands(p1), 0)
    assertEquals(Look(done.state).forces(wilds), SiteForces.Occupied(
      Look(done.state).forceKind(p1), 2))

  test("a replayed use reaches the same state"):
    val rules = ActPowerHarness.rulesWithPrintedRestrictions()
    val ready = captainsAtHomeWithTheRivalAtARuledSite().ready
    val (asked, queued) = campaignAtTheWilds(rules, ready)
    assertEquals(ActPowerHarness.replayed(rules, MenuPlay.withoutMenu(ready),
      asked.events ++ queued.events), Outcomes.ready(queued.state))

  /** p1, a Citizen, with Captains at home and 1 board warband; the wilds
    * hold 3 purple warbands, so on p1's turn p1 rules them and p2, the
    * Chancellor, defends them. p2 has an empty board, and nobody holds the
    * Oathkeeper title. */
  private def citizenWithCaptainsAndTheEmpiresWilds: Table = Table.start
    .imperial(p2, p1).oathkeeper(None).denizen(card, at = home).favor(p1, 1)
    .warbandsAt(wilds, p2, 3).boardWarbands(p1, 1).boardWarbands(p2, 0)

  /** Captains used at the wilds and all 4 warbands committed. Only a
    * Conquest is legal and no other Empire site can be added, so neither
    * the kind nor the targets is asked. */
  private def conquestOfTheWildsWithAllFour(rules: OathRules,
      ready: ReadyGame): PlanDriver.Run =
    val (_, queued) = campaignAtTheWilds(rules, ready)
    val start = PlanDriver.Run(rules, queued, queued.events)
    assertEquals(CampaignSetup.conquestDefender(Outcomes.ready(queued.state),
      p1), Some(CampaignDefender.Player(p2)))
    assert(!PlanDriver.awaits(start, p1, CampaignIds.kind))
    assert(!PlanDriver.awaits(start, p1, CampaignIds.targets))
    start.answer(p1, CampaignIds.force, ChooseAmountAnswer(4))

  test("a Citizen campaigning from an Empire site adds its warbands to the " +
      "attack only"):
    // The wilds' 3 purple warbands join p1's force and not the Chancellor's
    // defense (ruling R13). Four swords against blank dice and no defending
    // warbands win; none of the 3 die and none go to p2's board.
    val rules = ActPowerHarness.rulesWithPrintedRestrictions(
      PlanDriver.winningDice)
    val fought = finishedPlansAndSacrifice(conquestOfTheWildsWithAllFour(
      rules, citizenWithCaptainsAndTheEmpiresWilds.ready))
    val result = fought.ops.collectFirst { case RecordCampaignResult(r) => r }
    assertEquals(result.map(_.defenseScore), Some(0), fought.ops.toString)
    assert(!fought.ops.exists {
      case Kill(_, PositionedLocation(Location.Site(`wilds`), _, _)) => true
      case _ => false }, fought.ops.toString)
    assertEquals(Look(fought.state).forces(wilds),
      SiteForces.Occupied(ForceKind.Imperial, 3))
    assertEquals(Look(fought.state).boardWarbands(p2), 0)
    // Only the board's survivor is placed: the 3 already stand there.
    assert(PlanDriver.awaits(fought, p1, CampaignIds.placement))
    parkedDecide(fought.state).query match
      case amount: DecisionQuery.ChooseAmount => assertEquals(amount.max, 1)
      case other => fail(s"expected an amount, got $other")
    val placed = fought.answer(p1, CampaignIds.placement,
      ChooseAmountAnswer(1))
    assertEquals(Look(placed.state).forces(wilds),
      SiteForces.Occupied(ForceKind.Imperial, 4))
    assertEquals(Look(placed.state).boardWarbands(p1), 0)

  test("a Chancellor's warband sacrifice cannot take the attacker's warbands " +
      "at the origin"):
    // Wrestlers sacrifices a defending warband from a target site. The only
    // target is the Captains origin, whose warbands are the attacker's
    // (ruling R13), so no plan is left and the Chancellor's window does not
    // open.
    val rules = ActPowerHarness.rulesWithPrintedRestrictions(
      PlanDriver.winningDice)
    val forced = conquestOfTheWildsWithAllFour(rules,
      citizenWithCaptainsAndTheEmpiresWilds
        .adviser(p2, WrestlersCard.id).ready)
    assert(!PlanDriver.awaits(forced, p2, CampaignIds.defenderPlan))
    assert(PlanDriver.awaits(forced, p1, CampaignIds.sacrifice))

  test("with a second Empire site added, only that site's warbands defend"):
    // Broken Peaks also holds 2 purple warbands. The targets question tallies
    // none defending at the origin; the victory kills only Broken Peaks' 2,
    // and half of them, rounded up, go back to the Chancellor's board.
    val peaks = BrokenPeaksSite.id
    val rules = ActPowerHarness.rulesWithPrintedRestrictions(
      PlanDriver.winningDice)
    val (_, queued) = campaignAtTheWilds(rules,
      citizenWithCaptainsAndTheEmpiresWilds.warbandsAt(peaks, p2, 2).ready)
    val start = PlanDriver.Run(rules, queued, queued.events)
    assert(PlanDriver.awaits(start, p1, CampaignIds.targets))
    parkedDecide(start.state).query match
      case many: DecisionQuery.ChooseMany =>
        val force = many.tallies.find(_.label == "Defending force")
          .map(_.perOption)
        assertEquals(force.flatMap(_.get(DecisionOptionRef.Site(wilds))),
          Some(0))
        assertEquals(force.flatMap(_.get(DecisionOptionRef.Site(peaks))),
          Some(2))
      case other => fail(s"expected a choose-many, got $other")
    val fought = finishedPlansAndSacrifice(start.answer(p1,
      CampaignIds.targets, DecisionAnswer.ChooseManyAnswer(
        Vector(DecisionOptionRef.Site(peaks))))
      .answer(p1, CampaignIds.force, ChooseAmountAnswer(4)))
    assertEquals(Look(fought.state).forces(wilds),
      SiteForces.Occupied(ForceKind.Imperial, 3))
    assertEquals(Look(fought.state).forces(peaks), SiteForces.Empty)
    assertEquals(Look(fought.state).boardWarbands(p2), 1)
