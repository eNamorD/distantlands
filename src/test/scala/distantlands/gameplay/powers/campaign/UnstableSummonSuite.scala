package distantlands.gameplay.powers.campaign

import distantlands.gameplay.CampaignFixture._
import distantlands.gameplay.actions.campaign.CampaignIds
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.banner.YewStaffCard
import distantlands.gameplay.powers.campaign.PlanDriver._
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseAmountAnswer
import distantlands.testkit.Look

/** Unstable Summon: a free defender's plan that burns X secrets from the
  * Darkest Secret its user holds for X defense dice. It is the first plan
  * that asks an amount (catalog batch 7, slice 3). */
class UnstableSummonSuite extends munit.FunSuite:
  private val card = UnstableSummonCard.id
  private val ref: DecisionOptionRef = DecisionOptionRef.Denizen(card)
  private val onBanner =
    PositionedLocation(Location.OnBanner(Banner.DarkestSecret))

  private def lines(run: Run): Vector[NoteText.Said] =
    said(UnstableSummon, run.events)

  private def burned(text: String): NoteText.Said = NoteText.Said("burned",
    s"Burned $text.", covers = false)

  /** p1 attacks the origin, which the rival rules holding the Oathkeeper
    * title and Unstable Summon faceup; the Darkest Secret, with
    * `bannerSecrets` on it, is held by the rival, or by p1 when
    * `defenderHoldsBanner` is false. */
  private def defenderWithSummon(bannerSecrets: Int,
      defenderHoldsBanner: Boolean = true): Board =
    val ruled = rivalRulesOriginAndHoldsOathkeeper(
      board(warbands = 5, supply = 7))
    val base = withAdviserFor(ruled, ruled.rival, card, Orientation.FaceUp)
    on(base)(_.darkestSecret(Some(
      if defenderHoldsBanner then base.rival else base.actor), bannerSecrets))

  test("the defender burns every secret on the Darkest Secret for as many " +
      "defense dice, for nothing"):
    val b = defenderWithSummon(bannerSecrets = 3)
    val run = commit(rules(winningDice), b, 2)
    assert(run.offered(b.rival).contains(ref))
    assert(!run.options(b.rival).exists {
      case priced: DecisionOption.Priced => priced.ref == ref
      case _ => false
    })
    val asked = run.pick(b.rival, CampaignIds.defenderPlan, ref)
    assert(awaits(asked, b.rival, UnstableSummon.decisionId))
    assertEquals(asked.query(b.rival), DecisionQuery.ChooseAmount(1, 3,
      Some(UnstableSummon.heading), UnstableSummon.confirm))
    val done = asked.answer(b.rival, UnstableSummon.decisionId,
      ChooseAmountAnswer(3))
    assertEquals(Look(done.state).secretsOnDarkestSecret, 0)
    // A burn is recorded as the move it is made of.
    assert(done.since(asked).contains(Burn.secrets(3, onBanner, None).move))
    assert(done.since(asked).contains(
      ModifyDicePool(CampaignIds.defensePool, 3)))
    assert(!done.since(run).exists(_.isInstanceOf[PayCost]))
    assertEquals(lines(done), Vector(burned(
      "3 secrets from the Darkest Secret for 3 defense dice")))

  test("X may be as low as one, leaving the rest on the banner"):
    val b = defenderWithSummon(bannerSecrets = 3)
    val done = commit(rules(winningDice), b, 2)
      .pick(b.rival, CampaignIds.defenderPlan, ref)
      .answer(b.rival, UnstableSummon.decisionId, ChooseAmountAnswer(1))
    assertEquals(Look(done.state).secretsOnDarkestSecret, 2)
    assert(done.ops.contains(ModifyDicePool(CampaignIds.defensePool, 1)))
    assertEquals(lines(done), Vector(burned(
      "1 secret from the Darkest Secret for 1 defense die")))

  test("without the Darkest Secret, or with no secret on it, it is shown " +
      "unavailable"):
    // The title's plan keeps the defender's plan decision asked.
    for b <- Vector(defenderWithSummon(3, defenderHoldsBanner = false),
        defenderWithSummon(0)) do
      assertEquals(commit(rules(winningDice), b, 2).withheld(b.rival,
        ref), Some(OptionReason.Written(UnstableSummon.noBanner)))

  test("with Yew Staff it burns from the Darkest Secret another player " +
      "holds, or the bank"):
    val base = defenderWithSummon(bannerSecrets = 3,
      defenderHoldsBanner = false)
    for holder <- Vector(Some(base.actor), None) do
      val b = on(base)(_.darkestSecret(holder, 3)
        .relic(base.rival, YewStaffCard))
      val done = commit(rules(winningDice), b, 2)
        .pick(b.rival, CampaignIds.defenderPlan, ref)
        .answer(b.rival, UnstableSummon.decisionId, ChooseAmountAnswer(2))
      assertEquals(Look(done.state).secretsOnDarkestSecret, 1,
        holder.toString)
      assert(done.ops.contains(ModifyDicePool(CampaignIds.defensePool, 2)))
      assertEquals(lines(done), Vector(burned(
        "2 secrets from the Darkest Secret for 2 defense dice")))

  test("an attacker is not offered it"):
    val base = withAdviser(board(warbands = 5, supply = 7), card,
      Orientation.FaceUp)
    val b = withAdviser(on(base)(_.darkestSecret(Some(base.actor), 3)),
      LongbowsCard, Orientation.FaceUp)
    // Longbows keeps the attacker's plan decision asked.
    val run = commit(rules(winningDice), b, 2)
    assert(awaits(run, b.actor, CampaignIds.attackerPlan))
    assert(!run.offered(b.actor).contains(ref))
