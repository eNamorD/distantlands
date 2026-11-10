package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseAmountAnswer
import distantlands.testkit.Look

/** Cursed Cauldron: a victorious user gains a warband per enemy warband that
  * died in the Campaign, net of those that came back. */
class CursedCauldronSuite extends munit.FunSuite:
  private val relic = CursedCauldronCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Relic(relic)

  private def gained(who: PlayerId, amount: Int): NoteText.Said =
    val noun = if amount == 1 then "warband" else "warbands"
    NoteText.Said("gained", s"${who.value} gained $amount $noun.",
      covers = false)

  test("a victorious attacker gains a warband per bandit killed"):
    val base = board(warbands = 5, supply = 7)
    val b = withRelic(base, relic)
    val done = commit(rules(winningDice), b, 4)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows
    // Both bandits at the origin died, and no survivor was placed.
    assertEquals(done.winner, Some(true))
    assertEquals(Look(done.state).boardWarbands(b.actor), 7)
    assertEquals(said(CursedCauldron, done.events), Vector(gained(b.actor, 2)))

  test("a Conquest defender's returned warband did not die"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = withRelic(base, relic)
    val done = commit(rules(winningDice), b, 4)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows
    // Two died at the origin, and one of them returned to the rival's board.
    assertEquals(done.winner, Some(true))
    assertEquals(Look(done.state).boardWarbands(b.actor), 6)
    assertEquals(said(CursedCauldron, done.events), Vector(gained(b.actor, 1)))

  test("a victorious defender gains a warband per attacker warband killed"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = withRelicFor(base, base.rival, relic)
    val before = b.player(b.rival).board.warbands
    val done = commit(rules(losingDice), b, 4)
      .pick(b.rival, CampaignIds.defenderPlan, ref).finishPlanWindows
    // The defeat killed half of the attacker's four survivors.
    assertEquals(done.winner, Some(false))
    assertEquals(Look(done.state).boardWarbands(b.rival), before + 2)
    assertEquals(said(CursedCauldron, done.events), Vector(gained(b.rival, 2)))

  test("a victorious defender gains for the attacker's skull and sacrifice " +
      "too"):
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = withRelicFor(base, base.rival, relic)
    val before = b.player(b.rival).board.warbands
    // A paid skull and three hollow swords make 3, and a sacrifice 4,
    // against two double shields and the rival's two warbands at the
    // origin.
    val game = rules(dice(AttackDieFace.TwoSwordsSkull +:
      Vector.fill(3)(AttackDieFace.HollowSword),
      Vector.fill(2)(DefenseDieFace.TwoShields)))
    val done = commit(game, b, 4)
      .pick(b.rival, CampaignIds.defenderPlan, ref)
      .finishDefenderPlans(b.rival)
      .answer(b.actor, CampaignIds.sacrifice, ChooseAmountAnswer(1))
      .finishPlanWindows
    // The skull, the sacrifice and half the two survivors died.
    assertEquals(done.winner, Some(false))
    assertEquals(Look(done.state).boardWarbands(b.rival), before + 3)
    assertEquals(said(CursedCauldron, done.events),
      Vector(gained(b.rival, 3)))

  test("a defeated user gains nothing"):
    val base = board(warbands = 5, supply = 7)
    val b = withRelic(base, relic)
    val done = commit(rules(losingDice), b, 4)
      .pick(b.actor, CampaignIds.attackerPlan, ref).finishPlanWindows
    assertEquals(done.winner, Some(false))
    assertEquals(Look(done.state).boardWarbands(b.actor), 3)
    assertEquals(said(CursedCauldron, done.events), Vector.empty)

  test("a warband Obsidian Cage took from the enemy's Hospital did not die"):
    val hospital = HospitalCard.id
    val site = BrokenPeaksSite.id
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = withRelic(withRelic(withSiteCard(
      on(base)(_.warbandsAt(site, base.rival, 2)), site, hospital), relic),
      ObsidianCageCard)
    val done = commit(rules(winningDice), b, 4)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
      .pick(b.actor, CampaignIds.attackerPlan,
        DecisionOptionRef.Relic(ObsidianCageCard.id))
      .pick(b.rival, CampaignIds.defenderPlan,
        DecisionOptionRef.Denizen(hospital))
      .finishPlanWindows
    // Two died at the origin: one returned to the rival's board and the
    // other Hospital saved; the Cage takes both, so none stayed dead.
    val rival: ForceKind = ForceKind.Exile(b.player(b.rival).lineage)
    assertEquals(done.winner, Some(true))
    assertEquals(ready(done.state).game.current.warbandsOnCards
      .get(ObsidianCageCard.id), Some(Map(rival -> 2)))
    assertEquals(ready(done.state).game.current.map.sites(site).forces,
      SiteForces.Occupied(rival, 2))
    assertEquals(said(CursedCauldron, done.events), Vector.empty)

  test("a warband the enemy's Hospital places on its site did not die"):
    val hospital = HospitalCard.id
    // Broken Peaks, the first site after the origin, which nobody else rules.
    val site = BrokenPeaksSite.id
    val base = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val b = withRelic(withSiteCard(
      on(base)(_.warbandsAt(site, base.rival, 2)), site, hospital), relic)
    val done = commit(rules(winningDice), b, 4)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
      .pick(b.rival, CampaignIds.defenderPlan,
        DecisionOptionRef.Denizen(hospital))
      .finishPlanWindows
    // Two died at the origin: one returned to the rival's board, and Hospital
    // places the other on its site.
    assertEquals(done.winner, Some(true))
    assertEquals(ready(done.state).game.current.map.sites(site).forces,
      SiteForces.Occupied(ForceKind.Exile(b.player(b.rival).lineage), 3))
    assertEquals(Look(done.state).boardWarbands(b.actor), 5)
    assertEquals(said(CursedCauldron, done.events), Vector.empty)
