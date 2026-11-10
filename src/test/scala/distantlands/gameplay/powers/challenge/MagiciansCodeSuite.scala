package distantlands.gameplay.powers.challenge

import distantlands.catalog.ModifierMark
import distantlands.gameplay.{ModifierPlay, OathRules}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.actions.challenge.{ChallengeProcedure,
  ChallengeReach, MagiciansCodeCard, SigilOfTheEyeCard, SigilOfTheHeartCard}
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseAmountAnswer, ChooseOneAnswer}
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  ParkedNode, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Magician's Code (card 32), a Challenge modifier for 2 favor: a
  * Challenge of the Darkest Secret gains 2 secrets and places them with
  * the rest (catalog batch 10, slice 11). */
class MagiciansCodeSuite extends munit.FunSuite:
  private val powers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog, walkerPowerCatalog = powers)
  private val window = PowerWindow.ChallengeModifierSelection
  private val peoplesFavor = DecisionOptionRef.Banner(Banner.PeoplesFavor)
  private val darkestSecret = DecisionOptionRef.Banner(Banner.DarkestSecret)

  /** p1, in Act with 4 favor and `faceUp` faceup secrets, holds Magician's
    * Code faceup. The People's Favor is unheld with 6 favor, out of p1's
    * reach; the Darkest Secret is unheld with `onBanner` secrets. */
  private def p1HoldsCodeBeforeDarkestSecret(faceUp: Int, onBanner: Int)
      : Table = Table.start.favor(p1, 4).secrets(p1, faceUp = faceUp)
    .adviser(p1, MagiciansCodeCard)
    .peoplesFavor(None, favor = 6).darkestSecret(None, secrets = onBanner)

  private def parkedQuestion(state: OathState): Decide =
    ParkedNode.of(state, catalog, powers) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide
      case other => fail(s"expected a parked question, got $other")

  private def chooseOneOptions(decide: Decide): Vector[DecisionOption] =
    decide.query match
      case one: DecisionQuery.ChooseOne => one.options
      case other => fail(s"expected a choose-one, got $other")

  /** p1's Challenge started with `picks` picked, parked on its banner
    * question. */
  private def challengeAtBannerWith(table: Table, picks: PowerId*)
      : OathTransition =
    val started = accepted(ModifierPlay.started(rules, Ready(table.ready),
      ActionRef.Challenge, p1, window, picks.toVector))
    assertEquals(parkedQuestion(started.state).decisionId,
      ChallengeProcedure.bannerDecisionId)
    started

  private def bannerChosen(from: OathTransition, banner: DecisionOptionRef)
      : OathTransition = accepted(rules.resolveWalker(from.state, p1,
    ChallengeProcedure.bannerDecisionId, ChooseOneAnswer(banner)))

  /** The amount question's range at `at`. */
  private def amountRange(at: OathTransition): (Int, Int) =
    parkedQuestion(at.state).query match
      case query: DecisionQuery.ChooseAmount => (query.min, query.max)
      case other => fail(s"expected an amount, got $other")

  test("its printed power carries the Challenge icon"):
    assertEquals(MagiciansCodeCard.power.modifier,
      Some(ModifierMark.Action(MajorActionType.Challenge)))

  test("its 2 secrets reach a Darkest Secret p1's own cannot, even with " +
      "none of p1's own"):
    val table = p1HoldsCodeBeforeDarkestSecret(faceUp = 0, onBanner = 1)
    val ready = table.ready
    val started = challengeAtBannerWith(table, MagiciansCode.id)
    assertEquals(chooseOneOptions(parkedQuestion(started.state)).map(_.ref),
      Vector(darkestSecret))
    val chosen = bannerChosen(started, darkestSecret)
    // 0 of p1's own and the 2 gained beat the banner's 1.
    assertEquals(amountRange(chosen), (0, 0))
    val done = accepted(rules.resolveWalker(chosen.state, p1,
      ChallengeProcedure.amountDecisionId, ChooseAmountAnswer(0)))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).darkestSecretHolder, Some(p1))
    assertEquals(Look(after).secretsOnDarkestSecret, 2)
    assertEquals(Look(after).faceUpSecrets(p1), 0)
    // The pick paid 2 favor onto the card.
    assertEquals(Look(after).favor(p1), 4 - 2)
    assertEquals(Look(after).adviserTokens(p1, MagiciansCodeCard),
      Tokens(2, 0))
    val events = started.events ++ chosen.events ++ done.events
    assertEquals(NoteText.said(ProcedureNotes.id(ActionRef.Challenge),
      ChallengeProcedure.noteKeys, events), Vector(NoteText.Said(
        "took-from-bank-secrets",
        "Took Darkest Secret from the bank with 2 secrets", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, events), after)
    assertWireRoundTrips(events)

  test("with p1's own secrets, the amount starts 2 lower and the payment " +
      "places the 2 gained with them"):
    val table = p1HoldsCodeBeforeDarkestSecret(faceUp = 4, onBanner = 1)
    // Unpicked, p1 must place more than the banner's 1. (The banner holds
    // 1 so its ribbon burns it and places none: a larger one would ask
    // which of the tied empty sites take the placed half.)
    assertEquals(amountRange(bannerChosen(challengeAtBannerWith(table),
      darkestSecret)), (2, 4))
    val chosen = bannerChosen(challengeAtBannerWith(table, MagiciansCode.id),
      darkestSecret)
    assertEquals(amountRange(chosen), (0, 4))
    val done = accepted(rules.resolveWalker(chosen.state, p1,
      ChallengeProcedure.amountDecisionId, ChooseAmountAnswer(2)))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).secretsOnDarkestSecret, 2 + 2)
    assertEquals(Look(after).faceUpSecrets(p1), 4 - 2)

  test("it cannot be picked when 2 more secrets do not reach the Darkest " +
      "Secret"):
    // p2 holds the People's Favor away, and Sigil of the Heart opens it,
    // so the modifier decision is asked; 2 + 2 does not beat 5.
    val table = p1HoldsCodeBeforeDarkestSecret(faceUp = 2, onBanner = 5)
      .peoplesFavor(Some(p2), favor = 1).relic(p1, SigilOfTheHeartCard)
    val begun = accepted(MenuPlay.started(rules, Ready(table.ready), p1,
      ActionRef.Challenge))
    val asked = parkedQuestion(begun.state)
    assertEquals(asked.decisionId, ModifierPicks.decisionId(window))
    val code = DecisionOptionRef.Denizen(MagiciansCodeCard.id)
    assertEquals(chooseOneOptions(asked).find(_.ref == code),
      Some(DecisionOption.Unavailable(
        DecisionOption.forRef(code).getOrElse(fail("no Code option")),
        OptionReason.Written(ChallengeReach.codeTooFew))))

  test("picked, it adds nothing to a Challenge of the People's Favor"):
    // p1's 6 favor can outbid the People's Favor's 2 after the pick's 2.
    val table = p1HoldsCodeBeforeDarkestSecret(faceUp = 0, onBanner = 1)
      .favor(p1, 6).peoplesFavor(None, favor = 2)
    val started = challengeAtBannerWith(table, MagiciansCode.id)
    assertEquals(chooseOneOptions(parkedQuestion(started.state)).map(_.ref),
      Vector(peoplesFavor, darkestSecret))
    // The pick left p1 4 favor: the People's Favor's range is as usual.
    val chosen = bannerChosen(started, peoplesFavor)
    assertEquals(amountRange(chosen), (3, 4))
    val done = accepted(rules.resolveWalker(chosen.state, p1,
      ChallengeProcedure.amountDecisionId, ChooseAmountAnswer(3)))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).peoplesFavorHolder, Some(p1))
    // No secret was gained: the Darkest Secret and p1's secrets are as
    // they were.
    assertEquals(Look(after).secretsOnDarkestSecret, 1)
    assertEquals(Look(after).faceUpSecrets(p1), 0)

  /** p1 with 1 faceup secret holds Magician's Code and Sigil of the Eye;
    * p2, away, holds the Darkest Secret with 2. Only both together reach
    * it: the Sigil lifts the site, the Code's 2 outbid the banner. */
  private def p1NeedsCodeAndSigil: Table =
    p1HoldsCodeBeforeDarkestSecret(faceUp = 1, onBanner = 2)
      .darkestSecret(Some(p2), secrets = 2).relic(p1, SigilOfTheEyeCard)

  test("when the Code and Sigil of the Eye are both needed, either may be " +
      "picked first"):
    val table = p1NeedsCodeAndSigil
    assert(MenuPlay.availableInMenu(catalog, table.ready, ActionRef.Challenge))
    val begun = accepted(MenuPlay.started(rules, Ready(table.ready), p1,
      ActionRef.Challenge))
    val options = chooseOneOptions(parkedQuestion(begun.state))
    Vector(MagiciansCodeCard.id, SigilOfTheEyeCard.id).foreach(card =>
      val ref = DecisionOptionRef.ofCard(card).getOrElse(fail("no option"))
      assert(options.exists(option => option.ref == ref && option.available),
        card))
    Vector(Vector(MagiciansCode.id, SigilOfTheEye.id),
        Vector(SigilOfTheEye.id, MagiciansCode.id)).foreach(order =>
      val started = challengeAtBannerWith(table, order*)
      assertEquals(chooseOneOptions(parkedQuestion(started.state)).map(_.ref),
        Vector(darkestSecret), order)
      // 1 of p1's own and the 2 gained beat the banner's 2.
      assertEquals(amountRange(bannerChosen(started, darkestSecret)), (1, 1),
        order))

  test("with the Darkest Secret's holder away and nothing to lift that, " +
      "the Code says so"):
    // p1's 4 secrets and the Code's 2 would outbid the banner's 1, but p2
    // holds it away. Sigil of the Heart opens the People's Favor, so the
    // modifier decision is asked.
    val table = p1HoldsCodeBeforeDarkestSecret(faceUp = 4, onBanner = 1)
      .darkestSecret(Some(p2), secrets = 1).peoplesFavor(Some(p2), favor = 1)
      .relic(p1, SigilOfTheHeartCard)
    val begun = accepted(MenuPlay.started(rules, Ready(table.ready), p1,
      ActionRef.Challenge))
    val code = DecisionOptionRef.Denizen(MagiciansCodeCard.id)
    assertEquals(chooseOneOptions(parkedQuestion(begun.state))
      .find(_.ref == code), Some(DecisionOption.Unavailable(
        DecisionOption.forRef(code).getOrElse(fail("no Code option")),
        OptionReason.Written(ChallengeReach.codeAway))))
