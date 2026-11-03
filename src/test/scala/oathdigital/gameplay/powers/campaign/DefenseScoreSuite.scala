package oathdigital.gameplay.powers.campaign

import oathdigital.catalog.{Denizen, Relic}
import oathdigital.model.{CampaignPlanSide, DecisionOptionRef, DefenseDieFace}
import oathdigital.model.DefenseDieFace.{Blank, Doubler, OneShield, TwoShields}

/** The defense scorer: face filters, then doublers on what is left, then the
  * defender's force (catalog batch 4 rulings, "The defense score"). Each
  * plan's rules come from `DefenseScore.changes`, the list a Campaign reads,
  * so a card mapped to the wrong rules or the wrong side fails here. */
class DefenseScoreSuite extends munit.FunSuite:
  private final case class Plan(side: CampaignPlanSide, ref: DecisionOptionRef)
  private def attacker(card: Denizen): Plan =
    Plan(CampaignPlanSide.Attacker, DecisionOptionRef.Denizen(card.id))
  private def attackerRelic(relic: Relic): Plan =
    Plan(CampaignPlanSide.Attacker, DecisionOptionRef.Relic(relic.id))
  private def defender(card: Denizen): Plan =
    Plan(CampaignPlanSide.Defender, DecisionOptionRef.Denizen(card.id))

  /** The rules once `plans` are chosen, folded from `DefenseScore.changes` as
    * a Campaign folds them; fails if the scorer does not know a plan. */
  private def chosen(plans: Plan*)(using munit.Location): DefenseRules =
    val changes = DefenseScore.changes.filter(change =>
      plans.contains(Plan(change.side, change.ref)))
    assertEquals(changes.map(change => Plan(change.side, change.ref)).toSet,
      plans.toSet, "the defense scorer knows each plan")
    changes.foldLeft(DefenseRules())((rules, change) => change.change(rules))

  test("with no plan chosen it scores the dice and adds the force"):
    for faces <- Vector(Vector(OneShield, TwoShields, Doubler), Vector(Blank),
        Vector.empty[DefenseDieFace]) do
      assertEquals(DefenseScore.of(faces, 2, chosen()),
        DefenseDieFace.score(faces) + 2)

  test("with the attacker's Rain Boots or Bag of Siegeworks a single shield " +
      "scores nothing"):
    // The two shields are left, doubled.
    val faces = Vector(OneShield, TwoShields, Doubler)
    for plan <- Vector(attacker(RainBootsCard),
        attackerRelic(BagOfSiegeworksCard)) do
      assertEquals(DefenseScore.dice(faces, chosen(plan)), 4)

  test("the attacker's War Tortoise with Rain Boots and a doubler: the " +
      "filters run before the doubler"):
    val faces = Vector(OneShield, TwoShields, TwoShields, Doubler)
    assertEquals(DefenseScore.dice(faces, chosen()), 10)
    assertEquals(DefenseScore.dice(faces, chosen(attacker(WarTortoiseCard))), 2)
    assertEquals(DefenseScore.dice(faces, chosen(attacker(RainBootsCard))), 8)
    assertEquals(DefenseScore.dice(faces,
      chosen(attacker(WarTortoiseCard), attacker(RainBootsCard))), 0)

  test("the defender's Garrison Armory adds the force twice, undoubled"):
    assertEquals(DefenseScore.of(Vector(TwoShields, Doubler), 2,
      chosen(defender(GarrisonArmoryCard))), 4 + 2 + 2)
