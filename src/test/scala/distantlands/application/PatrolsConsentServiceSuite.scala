package distantlands.application

import distantlands.application.gamelog.LogScripts.{formatter, texts}
import distantlands.gameplay.actions.travel.TravelProcedure
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  NarrowPassSite}
import distantlands.gameplay.phases.PhaseMenu
import distantlands.gameplay.powers.action.DowsingSticksCard
import distantlands.gameplay.powers.recover.{RelicPatrols, RelicPatrolsCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.ProcedureDequeued
import distantlands.model._
import distantlands.model.OathEvent.{ConsentAnswered, ConsentRequested}
import distantlands.testkit.{Commands, MenuPlay, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.{accepted, present}

/** Asking the Patrols' ruler from the Act menu through the service
  * (catalog batch 10, slice 13): a grant starts the Recover, a refusal
  * leaves it unavailable, and bandits are never asked. */
class PatrolsConsentServiceSuite extends munit.FunSuite:
  private val gameId = "patrols-consent"
  private val peaks = BrokenPeaksSite.id
  private val city = AncientCitySite.id
  private val recoverPick: DecisionOptionRef =
    PhaseMenu.optionFor(ActionRef.Recover, Vector.empty).getOrElse(
      fail("the Act menu lists no Recover"))

  /** p1 in Act at Broken Peaks beside a facedown relic, with Relic Patrols
    * at Ancient City; `ruling` settles who rules both sites. */
  private def patrolled(ruling: Table => Table): Table =
    ruling(Table.start.pawn(p1, at = peaks)
      .relicAt(DowsingSticksCard.id, peaks)
      .edifice(RelicPatrolsCard.id, EdificeSide.Intact, at = city))

  private val ruledByP2: Table = patrolled(_.warbandsAt(city, p2, 1)
    .warbandsAt(peaks, p2, 1))

  private val request = ConsentRequest(p1, PhaseMenu.decisionId, recoverPick,
    Vector(ConsentAsk(p2, Vector(RelicPatrols.id))),
    Vector(ConsentKey(RelicPatrols.id, p1, PhaseMenu.decisionId,
      recoverPick)))

  private val askRecover: GameCommand =
    Commands.requestConsent(p1, PhaseMenu.decisionId, recoverPick)

  private def asking(service: GameApplicationService)(using munit.Location)
      : GameAccepted = accepted(service.handle(gameId, 0L, askRecover))

  private def answering(service: GameApplicationService, at: GameAccepted,
      choice: ConsentChoice)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence,
      Commands.answerConsent(p2, choice)))

  test("p1 asks p2 from the Act menu, naming Relic Patrols, and the log " +
      "says so"):
    val (service, _) = ruledByP2.service()
    val asked = asking(service)
    assertEquals(asked.events, Vector(ConsentRequested(request)))
    val lines = texts(formatter.format(present(accepted(
      service.history(gameId)), "a stored game").steps, Some(p1)))
    assert(lines.contains("Asked p2 for consent to ignore Relic Patrols " +
      "and select Recover."), lines)

  test("Yes starts the Recover, and the journal reads back the same"):
    val (service, repository) = ruledByP2.service()
    val yes = answering(service, asking(service), ConsentChoice.Yes)
    assertEquals(yes.events.head, ConsentAnswered(p2, true))
    assert(yes.events.exists {
      case ProcedureDequeued(entry) => entry.procedure == ActionRef.Recover
      case _ => false }, yes.events)
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(game =>
      (game.state, game.undo))), Right(Some((yes.state, yes.undo))))

  test("No leaves the menu parked and the Recover unavailable, with the " +
      "Patrols' reason"):
    val (service, _) = ruledByP2.service()
    val no = answering(service, asking(service), ConsentChoice.No)
    assertEquals(no.events, Vector(ConsentAnswered(p2, false)))
    val after = Outcomes.ready(no.state)
    assertEquals(MenuPlay.procedureBeyondMenu(after), None)
    MenuPlay.menuOptionFor(catalog, after, ActionRef.Recover) match
      case DecisionOption.Unavailable(_, reason) =>
        assertEquals(reason, OptionReason.Written(RelicPatrols.reason))
      case other => fail(s"the Recover is offered: $other")

  test("under bandit-ruled Patrols nobody may be asked"):
    val banditRuled = patrolled(_.bandits(city, 1).bandits(peaks, 1))
    MenuPlay.menuOptionFor(catalog, banditRuled.ready, ActionRef.Recover)
      match
      case DecisionOption.Unavailable(_, reason) =>
        assertEquals(reason, OptionReason.Written(RelicPatrols.reason))
      case other => fail(s"the Recover is offered: $other")
    val (service, _) = banditRuled.service()
    assertEquals(service.handle(gameId, 0L, askRecover),
      Left(GameApplicationError.CommandRejected(
        OathViolation.ConsentUnavailable(
          s"${recoverPick.wireId} is not an option to ask consent for"))))

  test("a Travel from the menu still asks the Narrow Pass's ruler at its " +
      "destination"):
    val (service, _) = ConsentFixture.service()
    val started = ConsentFixture.startTravel(service)
    val asked = accepted(service.handle(ConsentFixture.gameId,
      started.nextSequence, ConsentFixture.requestDunes))
    assertEquals(asked.events.collect { case ConsentRequested(sent) =>
      sent.keys }, Vector(Vector(ConsentKey(NarrowPassSite.pass, p1,
        TravelProcedure.destinationDecisionId, ConsentFixture.dunes))))
