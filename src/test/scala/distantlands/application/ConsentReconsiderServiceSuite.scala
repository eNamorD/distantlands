package distantlands.application

import distantlands.gameplay.actions.ImperialLaws
import distantlands.gameplay.actions.travel.TravelProcedure
import distantlands.gameplay.cards.NarrowPassSite
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.action.GreatClock
import distantlands.gameplay.powers.setup.GreatClockCard
import distantlands.gameplay.powers.travel.{ForbiddenForest, TravelFixture,
  TyrantCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.model.OathEvent.{CommandAccepted, ConsentReconsidered,
  ConsentRequested}
import distantlands.testkit.{Commands, Look, MenuPlay, Outcomes,
  PlacementPicks}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}

/** Changing a consent answer through the service (reconsider design,
  * user story, rulings 20 to 26, "Commands"). p2 rules the Narrow Pass;
  * p1 travels and asks about Dunes. */
class ConsentReconsiderServiceSuite extends munit.FunSuite:
  import ConsentFixture._
  private val pass = NarrowPassSite.pass
  private val destination = TravelProcedure.destinationDecisionId
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)

  private def unavailable(reason: String) = Left(
    GameApplicationError.CommandRejected(
      OathViolation.ConsentUnavailable(reason)))

  private def change(allow: Boolean, granter: PlayerId = p2): GameCommand =
    Commands.reconsiderConsent(granter, p1, destination, dunes, allow)

  /** p1 starts a Travel and asks about Dunes, and p2 answers `choice`. */
  private def answeredBy(service: GameApplicationService,
      choice: ConsentChoice)(using munit.Location): GameAccepted =
    val started = startTravel(service)
    val asked = accepted(service.handle(gameId, started.nextSequence,
      requestDunes))
    accepted(service.handle(gameId, asked.nextSequence,
      Commands.answerConsent(p2, choice)))

  private def changing(service: GameApplicationService, at: GameAccepted,
      allow: Boolean)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence, change(allow)))

  private def pawnOfP1(at: GameAccepted)(using munit.Location)
      : Option[SiteId] =
    Look(Outcomes.ready(at.state)).player(p1).pawnSite

  private def parked(at: GameAccepted)(using munit.Location): Boolean =
    MenuPlay.pendingBeyondMenu(Outcomes.ready(at.state)).nonEmpty

  private def dunesOffered(at: GameAccepted)(using munit.Location)
      : DecisionOption =
    present(PlacementPicks.parkedOptions(at.state, powers)
      .find(_.ref == dunes), "Dunes among the destinations")

  private def refusedPlainly(option: DecisionOption): Boolean = option match
    case DecisionOption.Unavailable(_, OptionReason.Noted(_, note)) =>
      note.key == "no-travel"
    case _ => false

  private def marks(at: GameAccepted)(using munit.Location): Set[UseMark] =
    Outcomes.ready(at.state).game.current.useMarks

  /** `base` with p2's answer to p1's request about Dunes already in the
    * ledger: a position the service cannot be played to. */
  private def withP2sAnswerInTheLedger(base: ReadyGame, granted: Boolean,
      use: Option[ConsentUse] = None, power: PowerId = pass): ReadyGame =
    val request = ConsentRequest(p1, destination, dunes,
      Vector(ConsentAsk(p2, Vector(power))), use = use)
    base.copy(consent = accepted(accepted(base.consent.requested(request))
      .answered(p2, granted, ConsentTurn.of(base))))

  test("a refusal changed to a grant reopens Dunes, answers nothing and " +
      "opens no batch"):
    val (service, _) = ConsentFixture.service()
    val no = answeredBy(service, ConsentChoice.No)
    assert(refusedPlainly(dunesOffered(no)))
    val changed = changing(service, no, allow = true)
    assertEquals(changed.events, Vector(ConsentReconsidered(p2, p1,
      destination, dunes, granted = true)))
    assert(parked(changed))
    assert(dunesOffered(changed).available)
    assertEquals(pawnOfP1(changed), pawnOfP1(no))
    assertEquals(changed.undo, no.undo)
    assertEquals(marks(changed), Set.empty)

  test("a late grant can be changed back until the requester selects " +
      "Dunes, and is locked after"):
    val (service, _) = ConsentFixture.service()
    val granted = changing(service, answeredBy(service, ConsentChoice.No),
      allow = true)
    val back = changing(service, granted, allow = false)
    assert(refusedPlainly(dunesOffered(back)))
    val again = changing(service, back, allow = true)
    val picked = accepted(service.handle(gameId, again.nextSequence,
      ParkedServiceFixture.pickTravelDestination(p1,
        TravelFixture.provincePlains)))
    assertEquals(pawnOfP1(picked), Some(TravelFixture.provincePlains))
    assertEquals(marks(picked).map(mark => (mark.requester, mark.target)),
      Set((p1, dunes: DecisionOptionRef)))
    assertEquals(service.handle(gameId, picked.nextSequence,
      change(allow = false)), unavailable("p1 has acted on this consent"))

  test("Yes at the prompt makes the move and locks the answer; the " +
      "requester's undo unlocks it; a refusal then greys Dunes"):
    val (service, repository) = ConsentFixture.service()
    val yes = answeredBy(service, ConsentChoice.Yes)
    assertEquals(pawnOfP1(yes), Some(TravelFixture.provincePlains))
    assertEquals(service.handle(gameId, yes.nextSequence,
      change(allow = false)), unavailable("p1 has acted on this consent"))
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    assertEquals(marks(undone), Set.empty)
    val refused = changing(service, undone, allow = false)
    assertEquals(refused.events, Vector(ConsentReconsidered(p2, p1,
      destination, dunes, granted = false)))
    assert(!refused.events.exists(_.isInstanceOf[CommandAccepted]))
    assert(parked(refused))
    assert(refusedPlainly(dunesOffered(refused)))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(game =>
      (game.state, game.undo, game.redo))),
      Right(Some((refused.state, refused.undo, refused.redo))))

  test("a change to a refusal ends redo, as a new command does"):
    val (service, _) = ConsentFixture.service()
    val yes = answeredBy(service, ConsentChoice.Yes)
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    assert(undone.redo.nonEmpty)
    val refused = changing(service, undone, allow = false)
    assertEquals(refused.redo, None)
    assertEquals(service.handle(gameId, refused.nextSequence,
      Commands.redo(p1)), Left(GameApplicationError.CommandRejected(
        OathViolation.NothingToRedo)))

  test("a change to a grant ends redo too: a taken-back step may hold " +
      "what the grant now changes (R29)"):
    val (service, repository) = ConsentFixture.service()
    val no = answeredBy(service, ConsentChoice.No)
    // p1 takes back the Travel's start; the refusal stays in the ledger.
    val undone = accepted(service.handle(gameId, no.nextSequence,
      Commands.undo(p1)))
    assert(undone.redo.nonEmpty)
    val granted = changing(service, undone, allow = true)
    assertEquals(granted.redo, None)
    assertEquals(service.handle(gameId, granted.nextSequence,
      Commands.redo(p1)), Left(GameApplicationError.CommandRejected(
        OathViolation.NothingToRedo)))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(game =>
      (game.state, game.undo, game.redo))),
      Right(Some((granted.state, granted.undo, granted.redo))))

  test("a replay of a journal with a taken-back choice sets the same " +
      "marks as live play"):
    val (service, repository) = ConsentFixture.service()
    val yes = answeredBy(service, ConsentChoice.Yes)
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    val refused = changing(service, undone, allow = false)
    val granted = changing(service, refused, allow = true)
    val picked = accepted(service.handle(gameId, granted.nextSequence,
      ParkedServiceFixture.pickTravelDestination(p1,
        TravelFixture.provincePlains)))
    assertEquals(marks(picked).size, 1)
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(game =>
      (game.state, game.undo))), Right(Some((picked.state, picked.undo))))

  test("the Great Clock moves the round inside the turn: the lock holds " +
      "across it, and across its undo (R11, R13)"):
    // The Clock stands at Dunes, and p1 can pay for it.
    val clockAtDunes = TravelFixture.on(passRuledByP2)(_
      .edifice(GreatClockCard, EdificeSide.Intact,
        at = TravelFixture.provincePlains)
      .favor(p1, 1).secrets(p1, 3))
    val (service, repository) = ConsentFixture.service(clockAtDunes)
    val yes = answeredBy(service, ConsentChoice.Yes)
    val round = Outcomes.ready(yes.state).game.current.tracks.round
    val advanced = accepted(service.handle(gameId, yes.nextSequence,
      MenuPlay.starting(p1, ActionRef.UsePower(GreatClock.id),
        Vector(DecisionOptionRef.Edifice(GreatClockCard.id)))))
    assertEquals(Outcomes.ready(advanced.state).game.current.tracks.round,
      round + 1)
    assertEquals(service.handle(gameId, advanced.nextSequence,
      change(allow = false)), unavailable("p1 has acted on this consent"))
    val clockUndone = accepted(service.handle(gameId, advanced.nextSequence,
      Commands.undo(p1)))
    assertEquals(Outcomes.ready(clockUndone.state).game.current.tracks.round,
      round)
    assertEquals(service.handle(gameId, clockUndone.nextSequence,
      change(allow = false)), unavailable("p1 has acted on this consent"))
    val travelUndone = accepted(service.handle(gameId,
      clockUndone.nextSequence, Commands.undo(p1)))
    val refused = changing(service, travelUndone, allow = false)
    assert(refusedPlainly(dunesOffered(refused)))
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(_.state)),
      Right(Some(refused.state)))

  test("a redo restores the mark and the lock"):
    val (service, _) = ConsentFixture.service()
    val yes = answeredBy(service, ConsentChoice.Yes)
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    assertEquals(marks(undone), Set.empty)
    val redone = accepted(service.handle(gameId, undone.nextSequence,
      Commands.redo(p1)))
    assertEquals(marks(redone), marks(yes))
    assertEquals(service.handle(gameId, redone.nextSequence,
      change(allow = false)), unavailable("p1 has acted on this consent"))

  /** p1 starts a Travel on the board with two rulers and asks about
    * Dunes: p3 about the Forest, p2 about the Pass. */
  private def askingTwoRulers(service: GameApplicationService)(
      using munit.Location): GameAccepted =
    val started = startTravel(service)
    accepted(service.handle(gameId, started.nextSequence, requestDunes))

  private def answering(service: GameApplicationService, at: GameAccepted,
      granter: PlayerId, choice: ConsentChoice)(using munit.Location)
      : GameAccepted = accepted(service.handle(gameId, at.nextSequence,
    Commands.answerConsent(granter, choice)))

  test("with two rulers, a refusal changed to a grant leaves only the " +
      "other ruler to ask, and cannot be changed back while they are " +
      "asked (ruling 23, R8)"):
    val (service, _) = ConsentFixture.service(dunesBehindTwoRulers)
    val no = answering(service, askingTwoRulers(service), p2,
      ConsentChoice.No)
    val granted = changing(service, no, allow = true)
    val again = accepted(service.handle(gameId, granted.nextSequence,
      requestDunes))
    assertEquals(again.events, Vector(ConsentRequested(ConsentRequest(p1,
      destination, dunes, Vector(ConsentAsk(p3,
        Vector(ForbiddenForest.id)))))))
    assertEquals(service.handle(gameId, again.nextSequence,
      change(allow = false)),
      unavailable("a consent request about that option is waiting"))
    val yes = answering(service, again, p3, ConsentChoice.Yes)
    assertEquals(pawnOfP1(yes), Some(TravelFixture.provincePlains))

  test("with two rulers, a change that completes the grants needs no " +
      "second ask of the ruler who already allowed (R9)"):
    val (service, _) = ConsentFixture.service(dunesBehindTwoRulers)
    val allowed = answering(service, askingTwoRulers(service), p3,
      ConsentChoice.Yes)
    val no = answering(service, allowed, p2, ConsentChoice.No)
    assert(refusedPlainly(dunesOffered(no)))
    val granted = changing(service, no, allow = true)
    assert(dunesOffered(granted).available)
    val picked = accepted(service.handle(gameId, granted.nextSequence,
      ParkedServiceFixture.pickTravelDestination(p1,
        TravelFixture.provincePlains)))
    assertEquals(pawnOfP1(picked), Some(TravelFixture.provincePlains))
    assertEquals(service.handle(gameId, picked.nextSequence,
      Commands.reconsiderConsent(p3, p1, destination, dunes,
        allow = false)), unavailable("p1 has acted on this consent"))

  test("a change needs an answer the issuer gave this turn, and one that " +
      "differs"):
    val (service, _) = ConsentFixture.service()
    assertEquals(service.handle(gameId, 0L, change(allow = true)),
      unavailable(
        "p2 answered no consent request about that option this turn"))
    val no = answeredBy(service, ConsentChoice.No)
    assertEquals(service.handle(gameId, no.nextSequence,
      change(allow = true, granter = p3)), unavailable(
        "p3 answered no consent request about that option this turn"))
    assertEquals(service.handle(gameId, no.nextSequence,
      change(allow = false)), unavailable("p2's answer is already that"))

  test("an answer a preference gave can be changed, and the preference " +
      "stays"):
    val (service, _) = ConsentFixture.service()
    val set = accepted(service.handle(gameId, 0L,
      setPreference(ConsentSetting.Never)))
    val started = startTravel(service, set.nextSequence)
    val asked = accepted(service.handle(gameId, started.nextSequence,
      requestDunes))
    val changed = changing(service, asked, allow = true)
    assert(dunesOffered(changed).available)
    assertEquals(consent(changed).preference(p2, pass, p1),
      Some(ConsentPreference.Never))

  test("a change is refused once the game is over, for an automatic " +
      "power, and for a ruler who lost the Pass"):
    val over = withP2sAnswerInTheLedger(passRuledByP2, granted = false)
      .updateCurrent(_.copy(result =
        Some(GameResult(p1, VictoryKind.Usurper))))
    assertEquals(ConsentFixture.service(over)._1.handle(gameId, 0L,
      change(allow = true)), unavailable("the game is over"))
    val automatic = withP2sAnswerInTheLedger(passRuledByP2,
      granted = false, use = Some(ConsentUse(TyrantCard.id, p1)))
    assertEquals(ConsentFixture.service(automatic)._1.handle(gameId, 0L,
      change(allow = true)),
      unavailable("an automatic power's answer cannot be changed"))
    // On the plain board the bandits hold the Pass.
    val lost = withP2sAnswerInTheLedger(TravelFixture.board(),
      granted = false)
    assertEquals(ConsentFixture.service(lost)._1.handle(gameId, 0L,
      change(allow = true)),
      unavailable("p2 no longer rules site.narrow-pass.pass"))

  test("an ask nobody rules stays final: the Chancellor's Eye"):
    val law = withP2sAnswerInTheLedger(passRuledByP2, granted = false,
      power = ImperialLaws.chancellorsEye)
    assertEquals(ConsentFixture.service(law)._1.handle(gameId, 0L,
      change(allow = true)),
      unavailable("p2 no longer rules rule.chancellors-eye"))

  test("a change waits while a request about the same option does"):
    val answered = withP2sAnswerInTheLedger(passRuledByP2, granted = true)
    val waiting = answered.copy(consent = answered.consent.copy(pending =
      Some(ConsentRequest(p1, destination, dunes,
        Vector(ConsentAsk(p3, Vector(pass)))))))
    assertEquals(ConsentFixture.service(waiting)._1.handle(gameId, 0L,
      change(allow = false)),
      unavailable("a consent request about that option is waiting"))
