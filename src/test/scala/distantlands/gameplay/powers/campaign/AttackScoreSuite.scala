package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, Relic}
import distantlands.gameplay.actions.campaign.CampaignBattle
import distantlands.model.{AttackDieFace, CampaignPlanSide, DecisionOptionRef}
import distantlands.model.AttackDieFace.{HollowSword, OneSword, TwoSwordsSkull}

/** The attack scorer's steps (catalog batch 4 rulings, "The attack score"):
  * face values, Lancers' doubling, the skull cap, War Tortoise, the total.
  * Each plan's rules come from `AttackScore.changes`, the list a Campaign
  * reads, so a card mapped to the wrong rules or the wrong side fails here. */
class AttackScoreSuite extends munit.FunSuite:
  private final case class Plan(side: CampaignPlanSide, ref: DecisionOptionRef)
  private def attacker(card: Denizen): Plan =
    Plan(CampaignPlanSide.Attacker, DecisionOptionRef.Denizen(card.id))
  private def defender(card: Denizen): Plan =
    Plan(CampaignPlanSide.Defender, DecisionOptionRef.Denizen(card.id))
  private def relicDefender(relic: Relic): Plan =
    Plan(CampaignPlanSide.Defender, DecisionOptionRef.Relic(relic.id))

  /** The rules once `plans` are chosen, folded from `AttackScore.changes` as
    * a Campaign folds them; fails if the scorer does not know a plan. */
  private def chosen(plans: Plan*)(using munit.Location): AttackRules =
    val changes = AttackScore.changes.filter(change =>
      plans.contains(Plan(change.side, change.ref)))
    assertEquals(changes.map(change => Plan(change.side, change.ref)).toSet,
      plans.toSet, "the attack scorer knows each plan")
    changes.foldLeft(AttackRules())((rules, change) => change.change(rules))

  private def assertScores(faces: Vector[AttackDieFace], force: Int,
      rules: AttackRules)(score: Int, lost: Int)(using munit.Location): Unit =
    assertEquals(AttackScore.of(faces, force, rules), (score, lost))

  test("with no plan chosen it scores as the printed rules do"):
    val rolls = Vector(Vector(OneSword, HollowSword, HollowSword, HollowSword),
      Vector(TwoSwordsSkull, TwoSwordsSkull, OneSword), Vector(TwoSwordsSkull),
      Vector.empty)
    for faces <- rolls; force <- 0 to 3 do
      assertEquals(AttackScore.of(faces, force, chosen()),
        CampaignBattle.attackResult(faces, force, ignoreSkulls = false))

  test("a hollow sword is worth half, a whole sword with Fae Battalion, and " +
      "nothing with the defender's Rusting Ray"):
    val faces = Vector.fill(3)(HollowSword)
    assertScores(faces, 3, chosen())(score = 1, lost = 0)
    assertScores(faces, 3, chosen(attacker(FaeBattalionCard)))(score = 3,
      lost = 0)
    assertScores(faces, 3, chosen(defender(RustingRayCard)))(score = 0,
      lost = 0)

  test("Lancers doubles every face, skulls included, before the skull cap"):
    // Two hollow swords make one sword, two swords, and two payable skulls
    // of two swords each.
    assertScores(Vector(HollowSword, OneSword, TwoSwordsSkull), 5,
      chosen(attacker(LancersCard)))(score = 1 + 2 + 4, lost = 2)

  test("the rulings' example: Lancers and Fearsome General with a force of 3"):
    // Doubled: four skulls and two swords. Three skulls are payable and score
    // 12, the fourth scores 0, the swords add 2.
    assertScores(Vector(TwoSwordsSkull, TwoSwordsSkull, OneSword), 3,
      chosen(attacker(LancersCard), attacker(FearsomeGeneralCard)))(score = 14,
      lost = 3)

  test("an unpayable skull scores nothing, with Fearsome General too"):
    val faces = Vector(TwoSwordsSkull, TwoSwordsSkull)
    assertScores(faces, 1, chosen())(score = 2, lost = 1)
    assertScores(faces, 1, chosen(attacker(FearsomeGeneralCard)))(score = 4,
      lost = 1)

  test("with Outriders, The Great Levy, Kindred Warriors or Rangers a skull " +
      "scores its swords and kills nothing, four with Fearsome General"):
    val faces = Vector(TwoSwordsSkull, TwoSwordsSkull)
    for card <- Vector(OutridersCard, TheGreatLevyCard, KindredWarriorsCard,
        RangersCard) do
      assertScores(faces, 1, chosen(attacker(card)))(score = 4, lost = 0)
    assertScores(faces, 1,
      chosen(attacker(OutridersCard), attacker(FearsomeGeneralCard)))(score = 8,
      lost = 0)

  test("the defender's War Tortoise scores every skull face as nothing, " +
      "Fearsome General's included, and payable skulls still kill"):
    assertScores(Vector(TwoSwordsSkull, TwoSwordsSkull, OneSword), 1,
      chosen(defender(WarTortoiseCard), attacker(FearsomeGeneralCard)))(
      score = 1, lost = 1)
    assertScores(Vector(TwoSwordsSkull, OneSword), 2,
      chosen(defender(WarTortoiseCard), attacker(OutridersCard)))(score = 1,
      lost = 0)

  test("with the defender's Weeping Banner each skull kills two, capped by " +
      "the force, and scores only when both are paid"):
    val faces = Vector(TwoSwordsSkull, TwoSwordsSkull, OneSword)
    val banner = relicDefender(WeepingBannerCard)
    assertScores(faces, 4, chosen(banner))(score = 1 + 4, lost = 4)
    // An odd force: the last warband dies and its skull scores nothing.
    assertScores(faces, 3, chosen(banner))(score = 1 + 2, lost = 3)
    assertScores(faces, 3, chosen(banner, attacker(FearsomeGeneralCard)))(
      score = 1 + 4, lost = 3)
    assertScores(faces, 3, chosen(banner, attacker(OutridersCard)))(
      score = 1 + 4, lost = 0)

  test("the skull faces count each skull rolled, twice when doubled"):
    val faces = Vector(TwoSwordsSkull, OneSword, TwoSwordsSkull)
    assertEquals(AttackScore.skullFaces(faces, AttackRules()), 2)
    assertEquals(AttackScore.skullFaces(faces, AttackRules(doubled = true)),
      4)
    assertEquals(AttackScore.skullFaces(faces, AttackRules(skullKills = 2)),
      2)
