package distantlands.gameplay

import distantlands.gameplay.BannerTakeParkFixture.{ParkAfterBannerTake,
  decisionId, resumeAnswer}
import distantlands.gameplay.actions.{ProcedureNotes, VisionRules}
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powerresolver.ContributingPower
import distantlands.gameplay.powers.{NoteText, PhasePowerCatalog,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.campaign.GlamorCard
import distantlands.gameplay.powers.search.DisciplesCard
import distantlands.gameplay.powers.whenplayed.{ConspiracyWhenPlayed,
  FavoredSon, FavoredSonCard, WhenPlayedHarness, WizardsConclave,
  WizardsConclaveCard}
import distantlands.gameplay.setup.{GameStartRules, LaterGameFixture,
  SetupLegacyFixture, SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{TheStandardBearer,
  TheStandardBearerCard}
import distantlands.gameplay.walker.{ProcedureWalker, WalkerOutcome,
  WalkerPowers}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Answers, Journal, Outcomes, PlacementPicks,
  Situation, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Every banner taker keeps its tree's shape when a walk parks right after
  * its take and resumes on the state the take left (Legacy in-play powers
  * R20): The Needle parks there, as `ParkAfterBannerTake` does. */
class BannerTakeResumeSuite extends munit.FunSuite:
  private val powers = WalkerPowers(
    WalkerPowerCatalog.default(catalog).powers :+ ParkAfterBannerTake)
  private val rules = new OathRules(catalog, walkerPowerCatalog = powers)

  private def answer(state: OathState, by: PlayerId, id: String,
      reply: DecisionAnswer): OathTransition =
    accepted(rules.resolveWalker(state, by, id, reply))

  /** `events` replayed from `start`, then the park after the take answered
    * by `taker`: the state the resumed walk reaches. */
  private def resumed(start: ReadyGame, events: Vector[OathEvent],
      taker: PlayerId): ReadyGame =
    Outcomes.ready(resumedWith(start, events, taker).state)

  /** As `resumed`, the resuming command's transition. */
  private def resumedWith(start: ReadyGame, events: Vector[OathEvent],
      taker: PlayerId): OathTransition =
    answer(accepted(Journal.evolved(rules, start, events)), taker,
      decisionId, resumeAnswer)

  test("Challenge: a walk parked after the take resumes and keeps the " +
      "banner with the challenger"):
    // p1, in Act at Ancient City with 6 favor, challenges for the
    // unclaimed People's Favor, with 2 favor on it, bidding 3.
    val board = Table.start.favor(p1, 6).secrets(p1, faceUp = 6,
      faceDown = 4).peoplesFavor(None, favor = 2).ready
    val started = accepted(rules.startWalker(Ready(board), ActionRef.Challenge,
      p1))
    val chosen = answer(started.state, p1, "challenge.banner",
      DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Banner(Banner.PeoplesFavor)))
    val bid = answer(chosen.state, p1, "challenge.amount",
      DecisionAnswer.ChooseAmountAnswer(3))
    val end = resumed(board, started.events ++ chosen.events ++ bid.events,
      p1)
    assertEquals(end.game.current.banners.peoplesFavor.holder, Some(p1))
    assertEquals(end.game.current.banners.peoplesFavor.favor, 3)

  /** `power` played from `ready` beside the park trigger, parked after its
    * take, then resumed from the replayed state with the park answered by
    * `taker`: the state the walk ends in. */
  private def whenPlayedResumed(ready: ReadyGame, power: ContributingPower,
      card: DenizenId, taker: PlayerId): WalkerOutcome.Finished =
    val beside = WhenPlayedHarness.powersBeside(power, ParkAfterBannerTake)
    val hook = WhenPlayedHarness.hook(card)
    val parked = Outcomes.parked(ProcedureWalker.advance(ready, hook, None,
      beside))
    val at = WhenPlayedHarness.replayed(ready, parked.events)
    Outcomes.finished(ProcedureWalker.resolve(at, hook, parked.tree,
      Answered(decisionId, resumeAnswer, taker), beside))

  test("Favored Son: a walk parked after the take resumes and keeps the " +
      "People's Favor with the player"):
    // p1 plays Favored Son while p2 holds the People's Favor with 3 favor.
    val ready = Table.start.adviser(p1, FavoredSonCard)
      .peoplesFavor(Some(p2), favor = 3).ready
    val done = whenPlayedResumed(ready, FavoredSon, FavoredSonCard.id, p1)
    assertEquals(done.treeless.game.current.banners.peoplesFavor.holder,
      Some(p1))
    // The take's line, after the park, is still written: the tree kept the
    // shape it parked in.
    assertEquals(WhenPlayedHarness.said(FavoredSon, done.events).map(_.key),
      Vector("took"))

  test("Wizard's Conclave: a walk parked after the take resumes and writes " +
      "its line"):
    // p1 plays Wizard's Conclave; p2 rules Disciples and Glamor, the most
    // Arcane cards, and takes the Darkest Secret, with 2 secrets, from p3.
    val ready = Table.start.adviser(p1, WizardsConclaveCard)
      .adviser(p2, DisciplesCard).adviser(p2, GlamorCard)
      .darkestSecret(Some(p3), 2).ready
    val done = whenPlayedResumed(ready, WizardsConclave,
      WizardsConclaveCard.id, p2)
    assertEquals(done.treeless.game.current.banners.darkestSecret.holder,
      Some(p2))
    assertEquals(WhenPlayedHarness.said(WizardsConclave, done.events)
      .map(_.key), Vector("took"))

  test("Conspiracy: a walk parked after the take resumes, writes its line " +
      "and removes the card"):
    // p1 at Ancient City searches the world deck, with Conspiracy on top,
    // and keeps it faceup; p2, also at Ancient City, holds the People's
    // Favor with 1 favor, its only target.
    val board = Table.start.pawn(p1, at = AncientCitySite)
      .pawn(p2, at = AncientCitySite).peoplesFavor(Some(p2), favor = 1)
      .darkestSecret(None, secrets = 1)
      .worldDeckTop(VisionRules.Conspiracy).ready
    val started = accepted(SearchPlay.searched(rules, Ready(board), p1))
    val placed = answer(started.state, p1,
      PlacementFixture.cardPlayDecisionId(VisionRules.Conspiracy, "place"),
      PlacementPicks.parkedAnswer(started.state, PlayedAs.Faceup, powers))
    val target = answer(placed.state, p1, ConspiracyWhenPlayed.decisionId,
      DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Banner(Banner.PeoplesFavor)))
    val resume = resumedWith(board,
      started.events ++ placed.events ++ target.events, p1)
    val end = Outcomes.ready(resume.state)
    assertEquals(end.game.current.banners.peoplesFavor.holder, Some(p1))
    assertEquals(end.game.current.temporaryHands(p1), Vector.empty)
    assert(!accepted(CardIndex.from(end.game)).ids
      .contains(VisionRules.Conspiracy))
    assertEquals(NoteText.said(ConspiracyWhenPlayed.id,
      ConspiracyWhenPlayed.noteKeys, resume.events).map(_.key),
      Vector("seized"))

  test("The Standard Bearer: a setup walk parked after the take resumes, " +
      "puts two secrets on the banner and writes its line"):
    // l2's player p2, a Citizen, holds The Standard Bearer; the People goal
    // starts the People's Favor with the Chancellor, so only the Darkest
    // Secret is in the shared bank, and p2 takes it after their pawn.
    val (l1, l2) = (LineageId("l1"), LineageId("l2"))
    val record = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2)
        .copy(oathkeeperGoal = OathkeeperGoal.ThePeople),
      l2, Role.Citizen, TheStandardBearerCard.id -> true)
    val orders = LaterGameFixture.orders(record, p1)
    val bank = accepted(GameStartRules.evolve(catalog, record, orders))
      .game.current.banners.darkestSecret
    val taking: Answers =
      case park if park.decisionId == TheStandardBearer.decisionId(p2) =>
        DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Banner(Banner.DarkestSecret))
    val woken = Situation.wake(Situation.rules(catalog, powers,
      PhasePowerCatalog.default).withAnswers(taking), record, orders)
    val held = woken.ready.game.current.banners.darkestSecret
    assertEquals(held.holder, Some(p2))
    assertEquals(held.secrets, bank.secrets + 2)
    assert(NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, woken.events).map(_.text).contains(
      s"p2 took the Darkest Secret with ${TheStandardBearerCard.id.value}"))
