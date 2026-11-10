package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture.{Board, board, on, rules}
import distantlands.gameplay.actions.campaign.{CampaignIds, PlanAid}
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.testkit.Look
import distantlands.testkit.Table.{p1, p2, p3}

/** Imperial allies pay first (Empire phase design, "1b-ii"; rulings R11
  * and R12): each ally in turn order after the user pays what their offer
  * has left, and the user pays the rest. p1, an Exile, raids p3, a Citizen,
  * at the bandit-held Ancient City; p2, the Chancellor, has 3 favor and p3
  * has 2. */
class ImperialTacticsPaymentSuite extends munit.FunSuite:
  private val chancellorFavor = 3
  private val citizenFavor = 2
  private val shieldWall: DecisionOptionRef =
    DecisionOptionRef.Denizen(ShieldWall.cardId)
  private val extraProvisions: DecisionOptionRef =
    DecisionOptionRef.Denizen(ExtraProvisionsCard.id)
  private val fireTalkers: DecisionOptionRef =
    DecisionOptionRef.Denizen(FireTalkersCard.id)
  private val arcaneArmor: DecisionOptionRef =
    DecisionOptionRef.Denizen(ArcaneArmorCard.id)
  private val heartsAndMinds: DecisionOptionRef =
    DecisionOptionRef.Denizen(HeartsAndMindsCard.id)
  private val defending = AidWindow(p1, CampaignPlanSide.Defender, p3)

  private def raidOnP3: Board =
    val b = on(board(warbands = 5, supply = 7))(
      _.imperial(p2, p3).oathkeeper(None))
    on(b)(_.pawn(p3, at = b.origin).boardWarbands(p3, 2)
      .favor(p2, chancellorFavor).favor(p3, citizenFavor))

  private def raided(b: Board): Run =
    commit(rules(winningDice), b, 4, raid = true)

  private def favorOf(run: Run, player: PlayerId): Int =
    Look(run.state).favor(player)

  private def secretsOf(run: Run, player: PlayerId): Int =
    Look(run.state).faceUpSecrets(player)

  private def citizenDefendsWithChancellorShieldWallOffering(offer: AidOffer)
      : Run =
    raided(on(raidOnP3)(_.adviser(p2, ShieldWall.cardId)
      .aid(p2, defending, offer)))

  test("an ally's offered favor pays a lent plan's cost"):
    val run = citizenDefendsWithChancellorShieldWallOffering(AidOffer(
      plans = Vector(shieldWall), amounts = AidAmounts(favor = 1)))
    assert(run.options(p1).exists(option => option.ref == shieldWall &&
      option.isInstanceOf[DecisionOption.Badged]),
      "the user pays nothing, so the option carries no price")
    val picked = run.pick(p3, CampaignIds.defenderPlan, shieldWall)
    assertEquals(favorOf(picked, p2), chancellorFavor - 1)
    assertEquals(favorOf(picked, p3), citizenFavor)

  test("the user pays what the offer does not cover"):
    val run = citizenDefendsWithChancellorShieldWallOffering(AidOffer(
      plans = Vector(shieldWall))).pick(p3, CampaignIds.defenderPlan,
      shieldWall)
    assertEquals(favorOf(run, p2), chancellorFavor)
    assertEquals(favorOf(run, p3), citizenFavor - 1)

  test("spent aid is not spent twice"):
    val run = raided(on(raidOnP3)(_.adviser(p2, ShieldWall.cardId)
      .adviser(p3, ExtraProvisionsCard.id)
      .aid(p2, defending, AidOffer(plans = Vector(shieldWall),
        amounts = AidAmounts(favor = 1)))))
      .pick(p3, CampaignIds.defenderPlan, shieldWall)
      .pick(p3, CampaignIds.defenderPlan, extraProvisions)
    assertEquals(favorOf(run, p2), chancellorFavor - 1)
    assertEquals(favorOf(run, p3), citizenFavor - 1)
    assertEquals(PlanAid.remaining(ready(run.state), defending, p2),
      AidAmounts.none)

  test("a used lent plan is recorded against its lender"):
    val run = citizenDefendsWithChancellorShieldWallOffering(AidOffer(
      plans = Vector(shieldWall), amounts = AidAmounts(favor = 1)))
      .pick(p3, CampaignIds.defenderPlan, shieldWall)
    assertEquals(PlanAid.usedRefs(ready(run.state), defending, p2),
      Vector(shieldWall))

  test("an offered requirement a plan relied on is recorded as used"):
    val run = raided(on(raidOnP3)(_.adviser(p3, FireTalkersCard.id)
      .darkestSecret(Some(p2), 1)
      .aid(p2, defending, AidOffer(requirements =
        Vector(PlanAid.darkestSecret)))))
      .pick(p3, CampaignIds.defenderPlan, fireTalkers)
    assertEquals(PlanAid.usedRefs(ready(run.state), defending, p2),
      Vector(PlanAid.darkestSecret))

  test("Arcane Armor's amount reaches the allies' offered secrets"):
    val run = raided(on(raidOnP3)(_.adviser(p3, ArcaneArmorCard.id)
      .secrets(p3, faceUp = 0).secrets(p2, faceUp = 2)
      .aid(p2, defending, AidOffer(amounts = AidAmounts(secrets = 2)))))
    assert(run.offered(p1).contains(arcaneArmor))

  test("Arcane Armor waits on the allies' secrets when none are offered"):
    val run = raided(on(raidOnP3)(_.adviser(p3, ArcaneArmorCard.id)
      .secrets(p3, faceUp = 0).secrets(p2, faceUp = 2)))
    assertEquals(run.withheld(p1, arcaneArmor),
      Some(ImperialTactics.awaitsNeed))

  test("a surcharge on a lent plan is paid from the offered secret first"):
    val run = raided(on(raidOnP3)(_.adviser(p1, GleamingArmorCard.id)
      .adviser(p2, ShieldWall.cardId).secrets(p2, faceUp = 2)
      .aid(p2, defending, AidOffer(plans = Vector(shieldWall),
        amounts = AidAmounts(favor = 1, secrets = 1)))))
      .pick(p3, CampaignIds.defenderPlan, shieldWall)
    assertEquals(secretsOf(run, p2), 1)
    assertEquals(secretsOf(run, p3), 1)
    assertEquals(favorOf(run, p3), citizenFavor)

  test("Hearts and Minds stays when the lender of the People's Favor " +
      "holds it"):
    // Hearts and Minds is a site-only card at an Imperial site, so it is
    // the Chancellor's (ruling R10). p2 offers it, 3 favor and the
    // People's Favor.
    val b = raidOnP3
    val at = b.player(p2).pawnSite.get
    val run = raided(on(b)(_.warbandsAt(at, p2, 2)
      .denizen(HeartsAndMindsCard.id, at).peoplesFavor(Some(p2), 1)
      .aid(p2, defending, AidOffer(plans = Vector(heartsAndMinds),
        requirements = Vector(PlanAid.peoplesFavor),
        amounts = AidAmounts(favor = 3)))))
      .pick(p3, CampaignIds.defenderPlan, heartsAndMinds).finishPlanWindows
    assertEquals(run.winner, Some(false))
    assert(Look(run.state).cardsAt(at).contains(HeartsAndMindsCard.id),
      run.ops.toString)

  test("Hearts and Minds is discarded when the People's Favor was not lent"):
    val b = raidOnP3
    val at = b.player(p2).pawnSite.get
    val run = raided(on(b)(_.warbandsAt(at, p2, 2)
      .denizen(HeartsAndMindsCard.id, at).peoplesFavor(Some(p2), 1)
      .aid(p2, defending, AidOffer(plans = Vector(heartsAndMinds),
        amounts = AidAmounts(favor = 3)))))
      .pick(p3, CampaignIds.defenderPlan, heartsAndMinds).finishPlanWindows
    assert(!Look(run.state).cardsAt(at).contains(HeartsAndMindsCard.id))

  test("Peace Envoy's gift is given by the ally's offered favor first"):
    // p3 holds Peace Envoy; p2 offers all 3 favor towards its cost and gift.
    val run = raided(on(raidOnP3)(_.adviser(p3, PeaceEnvoyCard.id)
      .aid(p2, defending, AidOffer(amounts =
        AidAmounts(favor = chancellorFavor)))))
    val dice = Look(run.state).diceInPool(CampaignIds.defensePool)
    val owed = PeaceEnvoy.Favor + dice
    val picked = run.pick(p3, CampaignIds.defenderPlan,
      DecisionOptionRef.Denizen(PeaceEnvoyCard.id))
    assertEquals(favorOf(picked, p2), 0)
    assertEquals(favorOf(picked, p3),
      citizenFavor - math.max(0, owed - chancellorFavor))
