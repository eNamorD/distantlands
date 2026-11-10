package distantlands.gameplay.powers.campaign

import distantlands.gameplay.GrandMaskCard
import distantlands.gameplay.powers.search.CrackedHornCard
import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseAmountAnswer, ChooseManyAnswer}
import distantlands.testkit.{ActPowerHarness, Look}
import distantlands.testkit.Outcomes.assertWireRoundTrips

/** Relic Hunter: an attacker's plan, in a Conquest only, that targets
  * facedown relics at the targeted sites for a defense die each; a victorious
  * attacker takes them and may put any on the bottom of the relic deck. */
class RelicHunterSuite extends munit.FunSuite:
  private val ref: DecisionOptionRef =
    DecisionOptionRef.Denizen(RelicHunterCard.id)
  private val mask: RelicId = GrandMaskCard.id
  private val horn: RelicId = CrackedHornCard.id

  private def slot(site: SiteId, index: Int): DecisionOptionRef.SiteRelicSlot =
    DecisionOptionRef.SiteRelicSlot(site, index)

  private def targeted(text: String): NoteText.Said =
    NoteText.Said("targeted", s"Targeted $text.", covers = false)

  /** p1 holds Relic Hunter faceup and attacks the bandit-ruled origin, where
    * the Grand Mask and the Cracked Horn lie facedown, in that order. */
  private def huntingTwoRelicsAtOrigin: Board =
    val base = withAdviser(board(warbands = 5, supply = 7), RelicHunterCard,
      Orientation.FaceUp)
    on(base)(_.relicAt(mask, at = base.origin).relicAt(horn, at = base.origin))

  test("the attacker targets both relics at the origin for two defense " +
      "dice, takes them on victory, and puts the one chosen on the bottom " +
      "of the relic deck"):
    val b = huntingTwoRelicsAtOrigin
    val run = commit(rules(winningDice), b, 4)
    val asked = run.pick(b.actor, CampaignIds.attackerPlan, ref)
    assert(awaits(asked, b.actor, RelicHunter.decisionId))
    val both = Vector(slot(b.origin, 0), slot(b.origin, 1))
    assertEquals(asked.query(b.actor), DecisionQuery.ChooseMany(0, 2,
      both.map(DecisionOption.SiteRelicSlot(_)),
      heading = Some(RelicHunter.heading)))
    val chosen = asked.answer(b.actor, RelicHunter.decisionId,
      ChooseManyAnswer(both))
    assert(chosen.since(asked).contains(
      ModifyDicePool(CampaignIds.defensePool, 2)))
    // Four swords beat two bandits and blank defense dice.
    val bottom = chosen.finishPlanWindows
    assertEquals(bottom.winner, Some(true))
    assert(awaits(bottom, b.actor, RelicHunter.bottomDecisionId))
    assertEquals(bottom.query(b.actor), DecisionQuery.ChooseMany(0, 2,
      both.map(DecisionOption.SiteRelicSlot(_)),
      heading = Some(RelicHunter.bottomHeading)))
    // The attacker peeked at both before the question.
    assertEquals(bottom.since(chosen).collect { case peek: Peek => peek },
      Vector(Peek(b.actor, mask, Location.Site(b.origin)),
        Peek(b.actor, horn, Location.Site(b.origin))))
    val done = bottom.answer(b.actor, RelicHunter.bottomDecisionId,
      ChooseManyAnswer(Vector(slot(b.origin, 1))))
    assertEquals(Look(done.state).relicsAt(b.origin), Vector.empty)
    assertEquals(Look(done.state).player(b.actor).relics,
      Vector(RelicState(mask, Orientation.FaceDown, Tokens.empty)))
    assertEquals(ready(done.state).game.current.commonCards.relicDeck.last,
      horn)
    assertEquals(said(RelicHunter, done.events), Vector(
      targeted("2 facedown relics for 2 defense dice"),
      NoteText.Said("took", s"Took ${mask.value}.", covers = true),
      NoteText.Said("bottomed",
        s"Put ${horn.value} on the bottom of the relic deck.", covers = true)))
    parked.assertResumed(done.state, Phase.Act, b.actor)
    assertEquals(ActPowerHarness.replayed(done.game, b.ready, done.events),
      ready(done.state))
    assertWireRoundTrips(done.events)

  test("the attacker may keep every relic taken"):
    val b = huntingTwoRelicsAtOrigin
    val done = commit(rules(winningDice), b, 4)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
      .answer(b.actor, RelicHunter.decisionId,
        ChooseManyAnswer(Vector(slot(b.origin, 0))))
      .finishPlanWindows
      .answer(b.actor, RelicHunter.bottomDecisionId,
        ChooseManyAnswer(Vector.empty))
    assertEquals(Look(done.state).relicsAt(b.origin), Vector(horn))
    assertEquals(Look(done.state).relics(b.actor), Vector(mask))
    assertEquals(said(RelicHunter, done.events), Vector(
      targeted("1 facedown relic for 1 defense die"),
      NoteText.Said("took", s"Took ${mask.value}.", covers = true)))

  test("a defeated attacker takes nothing, and the targeted relics stay"):
    val b = huntingTwoRelicsAtOrigin
    val done = commit(rules(losingDice), b, 4)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
      .answer(b.actor, RelicHunter.decisionId,
        ChooseManyAnswer(Vector(slot(b.origin, 0), slot(b.origin, 1))))
      .finishPlanWindows
    assertEquals(done.winner, Some(false))
    assertEquals(Look(done.state).relicsAt(b.origin), Vector(mask, horn))
    assertEquals(Look(done.state).relics(b.actor), Vector.empty)
    assertEquals(said(RelicHunter, done.events), Vector(
      targeted("2 facedown relics for 2 defense dice")))

  test("a victory with no survivor to place still takes the targeted relic"):
    val b = huntingTwoRelicsAtOrigin
    val done = commit(rules(winningDice), b, 3)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
      .answer(b.actor, RelicHunter.decisionId,
        ChooseManyAnswer(Vector(slot(b.origin, 1))))
      .finishAttackerPlans(b.actor)
      .answer(b.actor, CampaignIds.sacrifice, ChooseAmountAnswer(3))
    // Three swords and three sacrificed beat two bandits and three blank
    // dice, and no warband is left to place.
    assertEquals(done.winner, Some(true))
    assert(awaits(done, b.actor, RelicHunter.bottomDecisionId))
    val kept = done.answer(b.actor, RelicHunter.bottomDecisionId,
      ChooseManyAnswer(Vector.empty))
    assertEquals(Look(kept.state).relics(b.actor), Vector(horn))

  test("only relics at targeted sites are offered: a relic at a site the " +
      "defender rules but the Conquest does not target is not"):
    val base = withAdviser(board(extras = 1, warbands = 5, supply = 7),
      RelicHunterCard, Orientation.FaceUp)
    val extra = base.extras.head
    val b = on(base)(_.relicAt(mask, at = base.origin)
      .relicAt(horn, at = extra))
    val originOnly = commit(rules(winningDice), b, 4)
      .pick(b.actor, CampaignIds.attackerPlan, ref)
    assertEquals(originOnly.query(b.actor), DecisionQuery.ChooseMany(0, 1,
      Vector(DecisionOption.SiteRelicSlot(slot(b.origin, 0))),
      heading = Some(RelicHunter.heading)))
    val bothSites = commit(rules(winningDice), b, 4,
        targets = Vector(DecisionOptionRef.Site(extra)))
      .pick(b.actor, CampaignIds.attackerPlan, ref)
    assertEquals(bothSites.query(b.actor), DecisionQuery.ChooseMany(0, 2,
      Vector(slot(b.origin, 0), slot(extra, 0))
        .map(DecisionOption.SiteRelicSlot(_)),
      heading = Some(RelicHunter.heading)))

  test("with no facedown relic at the targeted sites it is shown " +
      "unavailable"):
    // Longbows keeps the attacker's plan decision asked.
    val b = withAdviser(withAdviser(board(warbands = 5, supply = 7),
      RelicHunterCard, Orientation.FaceUp), LongbowsCard, Orientation.FaceUp)
    assertEquals(commit(rules(winningDice), b, 4).withheld(b.actor, ref),
      Some(OptionReason.Written(RelicHunter.noRelic)))

  test("it is never offered in a Raid"):
    val base = withEnemyAtOrigin(board(warbands = 5, supply = 7))
    // Longbows keeps the attacker's plan decision asked.
    val b = on(withAdviser(withAdviser(base, RelicHunterCard,
      Orientation.FaceUp), LongbowsCard, Orientation.FaceUp))(
      _.relicAt(mask, at = base.origin))
    val raid = commit(rules(winningDice), b, 4, raid = true)
    assert(awaits(raid, b.actor, CampaignIds.attackerPlan))
    assert(!raid.options(b.actor).exists(_.ref == ref))
