package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.holding.VowOfKinshipCard
import oathdigital.gameplay.powers.whenplayed.SearchPartyCard
import oathdigital.gameplay.CampaignFixture.{Board, board, on, rules}
import oathdigital.gameplay.actions.campaign.{CampaignIds, CampaignPlanChoice,
  PlanAid}
import oathdigital.gameplay.powers.banner.YewStaffCard
import oathdigital.gameplay.powers.campaign.PlanDriver._
import oathdigital.model._
import oathdigital.testkit.Table.{p1, p2, p3}

/** Imperial Tactics in the plan window (Empire phase design, "1b-ii"): an
  * Imperial user may use a plan an Imperial ally lends, and meet a plan's
  * requirement with an ally's. p1 raids p3 at the bandit-held Ancient City,
  * where p3's pawn stands with 2 board warbands; p2, the Chancellor, stands
  * at the first site nobody rules. Nobody holds the Oathkeeper title unless
  * a test says so. */
class ImperialTacticsSuite extends munit.FunSuite:
  private val shieldWall: DecisionOptionRef =
    DecisionOptionRef.Denizen(ShieldWall.cardId)
  private val fireTalkers: DecisionOptionRef =
    DecisionOptionRef.Denizen(FireTalkersCard.id)
  private val greatCrusade: DecisionOptionRef =
    DecisionOptionRef.Denizen(GreatCrusadeCard.id)
  private val rampart: DecisionOptionRef =
    DecisionOptionRef.Edifice(ToweringRampart.edificeId)
  private val encirclement: DecisionOptionRef =
    DecisionOptionRef.Denizen(EncirclementCard.id)
  private val title: DecisionOptionRef = DecisionOptionRef.Button("title")
  private val defending = AidWindow(p1, CampaignPlanSide.Defender, p3)

  /** p1 raids p3 at the origin; p2 is the Chancellor and `citizens` are
    * Citizens. */
  private def raidOnP3(citizens: PlayerId*): Board =
    val b = on(board(warbands = 5, supply = 7))(
      _.imperial(p2, citizens*).oathkeeper(None))
    on(b)(_.pawn(p3, at = b.origin).boardWarbands(p3, 2))

  private def raided(b: Board): Run =
    commit(rules(winningDice), b, 4, raid = true)

  private def citizenDefendsWithChancellorHoldingShieldWall(
      offer: Option[AidOffer] = None): Run =
    val b = raidOnP3(p3)
    raided(on(b)(table => offer.foldLeft(
      table.adviser(p2, ShieldWall.cardId))(_.aid(p2, defending, _))))

  test("an ally's faceup plan is shown awaiting their offer"):
    val run = citizenDefendsWithChancellorHoldingShieldWall()
    assert(awaits(run, p3, CampaignIds.defenderPlan))
    assertEquals(run.withheld(p1, shieldWall),
      Some(ImperialTactics.awaitsPlan(p2)))

  test("an offered plan is choosable, lent by its owner"):
    val run = citizenDefendsWithChancellorHoldingShieldWall(Some(
      AidOffer(plans = Vector(shieldWall))))
    assert(run.offered(p1).contains(shieldWall))
    assert(run.options(p1).exists(option => option.ref == shieldWall &&
      DecisionOption.lender(option).contains(p2)))

  test("an ally's facedown adviser is never lent"):
    val b = raidOnP3(p3)
    val run = raided(on(b)(_.adviser(p2, ShieldWall.cardId, facedown = true)
      .aid(p2, defending, AidOffer(plans = Vector(shieldWall)))))
    assert(!awaits(run, p3, CampaignIds.defenderPlan))

  test("an Exile defender sees no ally's plan"):
    val b = raidOnP3()
    val run = raided(on(b)(_.adviser(p2, ShieldWall.cardId)))
    assert(!awaits(run, p3, CampaignIds.defenderPlan))

  private def citizenWithFireTalkersAndChancellorOffering(offer: AidOffer,
      yewStaff: Boolean = false): Run =
    val b = raidOnP3(p3)
    raided(on(b) { table =>
      val held = table.adviser(p3, FireTalkersCard.id)
      val lender =
        if yewStaff then held.relic(p2, YewStaffCard.id)
        else held.darkestSecret(Some(p2), 1)
      lender.aid(p2, defending, offer)
    })

  test("a Darkest Secret plan of the user's waits on an ally's banner"):
    val run = citizenWithFireTalkersAndChancellorOffering(AidOffer())
    assertEquals(run.withheld(p1, fireTalkers),
      Some(ImperialTactics.awaitsNeed))

  test("a Darkest Secret plan of the user's is usable with an ally's banner"):
    val run = citizenWithFireTalkersAndChancellorOffering(
      AidOffer(requirements = Vector(PlanAid.darkestSecret)))
    assert(run.offered(p1).contains(fireTalkers))
    assert(run.options(p1).forall(DecisionOption.lender(_).isEmpty))

  test("the Yew Staff meets the Darkest Secret too"):
    val run = citizenWithFireTalkersAndChancellorOffering(
      AidOffer(requirements = Vector(PlanAid.yewStaff)), yewStaff = true)
    assert(run.offered(p1).contains(fireTalkers))

  /** Towering Rampart stands at the site p2 stands at, which p2's Imperial
    * warbands hold for the Empire; a Raid targets no site. */
  private def citizenDefendsRaidWithRampartAndChancellorPawn(offer: AidOffer)
      : Run =
    val b = raidOnP3(p3)
    val at = b.player(p2).pawnSite.get
    raided(on(b)(_.warbandsAt(at, p2, 2)
      .edifice(ToweringRampart.edificeId, EdificeSide.Intact, at)
      .aid(p2, defending, offer)))

  test("an ally's pawn meets Towering Rampart's pawn (R16)"):
    val run = citizenDefendsRaidWithRampartAndChancellorPawn(AidOffer(
      plans = Vector(rampart),
      requirements = Vector(DecisionOptionRef.Player(p2))))
    assert(run.offered(p1).contains(rampart))

  test("Towering Rampart waits on the pawn when only the plan is lent"):
    val run = citizenDefendsRaidWithRampartAndChancellorPawn(AidOffer(
      plans = Vector(rampart)))
    assertEquals(run.withheld(p1, rampart),
      Some(ImperialTactics.awaitsPlan(p2)))

  test("Great Crusade counts only the user's own nomads (R16)"):
    // p2 rules two nomad advisers and offers them and Great Crusade's
    // requirements; the plan still removes one die, for itself.
    val nomads = planless(SearchPartyCard, VowOfKinshipCard)
    val b = raidOnP3(p3)
    val run = raided(on(b)(_.adviser(p3, GreatCrusadeCard.id)
      .adviser(p2, nomads(0)).adviser(p2, nomads(1))
      .aid(p2, defending, AidOffer(plans =
        nomads.map(DecisionOptionRef.Denizen(_))))))
      .pick(p3, CampaignIds.defenderPlan, greatCrusade)
    assert(run.ops.contains(ModifyDicePool(CampaignIds.attackPool, -1)),
      run.ops.toString)

  test("the Oathkeeper holder may lend the title's defense"):
    val b = raidOnP3(p3)
    val run = raided(on(b)(_.oathkeeper(Some(p2))
      .aid(p2, defending, AidOffer(plans = Vector(title)))))
    assert(run.offered(p1).contains(title))

  test("the title's defense is not lent without an offer"):
    val b = raidOnP3(p3)
    val run = raided(on(b)(_.oathkeeper(Some(p2))))
    assertEquals(run.withheld(p1, title),
      Some(ImperialTactics.awaitsPlan(p2)))

  test("a plan the attacker used is unavailable to the defender"):
    // p2 lends Encirclement to p1, who uses it; then p2 offers it to p3,
    // who holds the title, so the defender's window opens.
    val attacking = AidWindow(p1, CampaignPlanSide.Attacker, p1)
    val b = raidOnP3(p1, p3)
    val run = raided(on(b)(_.oathkeeper(Some(p3))
      .adviser(p2, EncirclementCard.id)
      .aid(p2, attacking, AidOffer(plans = Vector(encirclement)))))
      .pick(p1, CampaignIds.attackerPlan, encirclement)
      .finishAttackerPlans(p1)
    val offered = run.copy(transition = run.transition.copy(state =
      run.game.evolve(run.state, OathEvent.PlanAidSaved(p2, defending,
        AidOffer(plans = Vector(encirclement)))).toOption.get))
    assert(awaits(offered, p3, CampaignIds.defenderPlan))
    assertEquals(offered.withheld(p1, encirclement),
      Some(OptionReason.Written(CampaignPlanChoice.attackerUsed)))
