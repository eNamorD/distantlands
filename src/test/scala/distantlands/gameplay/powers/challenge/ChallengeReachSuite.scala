package oathdigital.gameplay.powers.challenge

import oathdigital.catalog.ModifierMark
import oathdigital.gameplay.{ModifierPlay, OathRules}
import oathdigital.gameplay.actions.challenge.{ChallengeProcedure,
  ChallengeReach, SchoolOfVinesCard, SigilOfTheEyeCard, SigilOfTheHeartCard}
import oathdigital.gameplay.powerresolver.ModifierPicks
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.ModifierChoice
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  ParkedNode, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** School of Vines (E27, intact), Sigil of the Eye (R43) and Sigil of the
  * Heart (R44): free Challenge modifiers that lift the pawn-site
  * requirement for some banners (catalog batch 10, slice 11). */
class ChallengeReachSuite extends munit.FunSuite:
  private val powers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog, walkerPowerCatalog = powers)
  private val window = PowerWindow.ChallengeModifierSelection
  private val peoplesFavor = DecisionOptionRef.Banner(Banner.PeoplesFavor)
  private val darkestSecret = DecisionOptionRef.Banner(Banner.DarkestSecret)

  /** p1, in Act with 6 favor and 6 faceup secrets. p2, at their quiet site
    * away from p1, holds the People's Favor with 1 favor and the Darkest
    * Secret with 1 secret, so p1 could outbid both but reaches neither. */
  private def p2HoldsBothBannersAway: Table = Table.start
    .favor(p1, 6).secrets(p1, faceUp = 6)
    .peoplesFavor(Some(p2), favor = 1).darkestSecret(Some(p2), secrets = 1)

  /** `p2HoldsBothBannersAway` with School of Vines intact at p1's site,
    * where p1 may use it. */
  private def schoolAtP1sSite: Table =
    p2HoldsBothBannersAway.edifice(SchoolOfVinesCard, EdificeSide.Intact,
      at = Table.quietPawnSite(p1))

  private def refOf(card: CardId): DecisionOptionRef =
    DecisionOptionRef.ofCard(card).getOrElse(fail(s"$card has no option"))

  /** The question parked in `state`, read with the production powers. */
  private def parkedQuestion(state: OathState): Decide =
    ParkedNode.of(state, catalog, powers) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide
      case other => fail(s"expected a parked question, got $other")

  private def chooseOneOptions(decide: Decide): Vector[DecisionOption] =
    decide.query match
      case one: DecisionQuery.ChooseOne => one.options
      case other => fail(s"expected a choose-one, got $other")

  /** The modifier decision's options when p1 starts a Challenge. */
  private def challengeModifierOptions(table: Table): Vector[DecisionOption] =
    val begun = accepted(MenuPlay.started(rules, Ready(table.ready), p1,
      ActionRef.Challenge))
    val asked = parkedQuestion(begun.state)
    assertEquals(asked.decisionId, ModifierPicks.decisionId(window))
    chooseOneOptions(asked)

  /** p1's Challenge started with `picks` picked, parked on its banner
    * question. */
  private def challengeAtBannerWith(table: Table, picks: PowerId*)
      : OathTransition =
    val started = accepted(ModifierPlay.started(rules, Ready(table.ready),
      ActionRef.Challenge, p1, window, picks.toVector))
    assertEquals(parkedQuestion(started.state).decisionId,
      ChallengeProcedure.bannerDecisionId)
    started

  private def bannersOffered(at: OathTransition): Vector[DecisionOptionRef] =
    chooseOneOptions(parkedQuestion(at.state)).map(_.ref)

  test("each card's printed power carries the Challenge icon"):
    val icon = Some(ModifierMark.Action(MajorActionType.Challenge))
    assertEquals(SchoolOfVinesCard.intact.power.modifier, icon)
    assertEquals(SigilOfTheEyeCard.power.modifier, icon)
    assertEquals(SigilOfTheHeartCard.power.modifier, icon)

  test("without a reach modifier, banners held away are out of reach and " +
      "the Challenge is refused"):
    val ready = p2HoldsBothBannersAway.ready
    assert(!MenuPlay.availableInMenu(catalog, ready, ActionRef.Challenge))
    assertEquals(rules.startWalker(Ready(ready), ActionRef.Challenge, p1)
      .left.toOption, Some(OathViolation.NoPlayableOption("challenge")))

  test("Sigil of the Eye opens the Darkest Secret from any site, and only " +
      "it"):
    val table = p2HoldsBothBannersAway.relic(p1, SigilOfTheEyeCard)
    assert(MenuPlay.availableInMenu(catalog, table.ready, ActionRef.Challenge))
    val options = challengeModifierOptions(table)
    // Nothing is in reach before the pick, so Start waits for it.
    assertEquals(options.find(_.ref == ModifierPicks.start),
      Some(DecisionOption.Unavailable(DecisionOption.Button(
        ModifierPicks.start, "Start Challenge with no modifiers"),
        OptionReason.Written(ModifierChoice.cannotStart))))
    assert(options.exists(option =>
      option.ref == refOf(SigilOfTheEyeCard.id) && option.available))
    assertEquals(bannersOffered(challengeAtBannerWith(table,
      SigilOfTheEye.id)), Vector(darkestSecret))

  test("Sigil of the Heart opens the People's Favor from any site, and " +
      "only it"):
    val table = p2HoldsBothBannersAway.relic(p1, SigilOfTheHeartCard)
    assert(MenuPlay.availableInMenu(catalog, table.ready, ActionRef.Challenge))
    assertEquals(bannersOffered(challengeAtBannerWith(table,
      SigilOfTheHeart.id)), Vector(peoplesFavor))

  test("a Sigil that brings no new banner into reach cannot be picked"):
    // The People's Favor is unheld, so it needs no Sigil; p2 holds the
    // Darkest Secret away, so Sigil of the Eye opens it.
    val table = Table.start.favor(p1, 6).secrets(p1, faceUp = 6)
      .peoplesFavor(None, favor = 1).darkestSecret(Some(p2), secrets = 1)
      .relic(p1, SigilOfTheEyeCard).relic(p1, SigilOfTheHeartCard)
    val heart = refOf(SigilOfTheHeartCard.id)
    assertEquals(challengeModifierOptions(table).find(_.ref == heart),
      Some(DecisionOption.Unavailable(
        DecisionOption.forRef(heart).getOrElse(fail("no Sigil option")),
        OptionReason.Written(ChallengeReach.noNewBanner))))

  test("School of Vines opens every banner an enemy holds, from any site"):
    val table = schoolAtP1sSite
    assert(MenuPlay.availableInMenu(catalog, table.ready, ActionRef.Challenge))
    assertEquals(bannersOffered(challengeAtBannerWith(table,
      SchoolOfVines.id)), Vector(peoplesFavor, darkestSecret))

  test("once School of Vines is picked, a Sigil it covers cannot be " +
      "picked, and Start is available"):
    val table = schoolAtP1sSite.relic(p1, SigilOfTheEyeCard)
    val sigil = refOf(SigilOfTheEyeCard.id)
    val options = challengeModifierOptions(table)
    assert(options.exists(option =>
      option.ref == refOf(SchoolOfVinesCard.id) && option.available))
    assert(options.exists(option => option.ref == sigil && option.available))
    // A later pass asks while a modifier is still listed, so the decision
    // is asked again, with the Sigil shown unavailable.
    val begun = accepted(MenuPlay.started(rules, Ready(table.ready), p1,
      ActionRef.Challenge))
    val picked = accepted(rules.resolveWalker(begun.state, p1,
      ModifierPicks.decisionId(window), ModifierPlay.pick(SchoolOfVines.id)))
    val again = chooseOneOptions(parkedQuestion(picked.state))
    assertEquals(again.find(_.ref == sigil), Some(DecisionOption.Unavailable(
      DecisionOption.forRef(sigil).getOrElse(fail("no Sigil option")),
      OptionReason.Written(ChallengeReach.noNewBanner))))
    assert(again.find(_.ref == ModifierPicks.start).exists(_.available))
    val started = accepted(rules.resolveWalker(picked.state, p1,
      ModifierPicks.decisionId(window), ModifierPlay.start))
    assertEquals(bannersOffered(started), Vector(peoplesFavor, darkestSecret))

  test("School of Vines does not reach a banner an Imperial ally holds"):
    // p2 is the Chancellor and p1 a Citizen, so they are not enemies.
    val ready = schoolAtP1sSite.imperial(p2, p1).turn(p1, Phase.Act).ready
    assert(!Imperials.enemies(ready, p1, p2))
    assert(!ChallengeReach.widens(ready, p1, Vector.empty,
      SchoolOfVinesCard.id, Vector.empty))
    assert(!MenuPlay.availableInMenu(catalog, ready, ActionRef.Challenge))

  test("a Challenge a Sigil opened takes the banner and replays"):
    val ready = p2HoldsBothBannersAway.relic(p1, SigilOfTheEyeCard).ready
    val started = challengeAtBannerWith(Table.from(ready), SigilOfTheEye.id)
    val chosen = accepted(rules.resolveWalker(started.state, p1,
      ChallengeProcedure.bannerDecisionId, ChooseOneAnswer(darkestSecret)))
    // The banner holds 1 secret, so the amount starts at 2.
    val done = accepted(rules.resolveWalker(chosen.state, p1,
      ChallengeProcedure.amountDecisionId, ChooseAmountAnswer(2)))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).darkestSecretHolder, Some(p1))
    assertEquals(Look(after).secretsOnDarkestSecret, 2)
    val events = started.events ++ chosen.events ++ done.events
    assertEquals(ActPowerHarness.replayed(rules, ready, events), after)
    assertWireRoundTrips(events)
