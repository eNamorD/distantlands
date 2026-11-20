package distantlands.application

import distantlands.gameplay.actions.ImperialLaws
import distantlands.gameplay.actions.travel.TravelProcedure
import distantlands.gameplay.cards.{AncientCitySite, NarrowPassSite}
import distantlands.gameplay.powers.cardplay.SecretPoliceCard
import distantlands.gameplay.powers.poweruse.{CityWall, CityWallCard}
import distantlands.gameplay.powers.recover.RelicPatrolsCard
import distantlands.gameplay.powers.PayOrAsk
import distantlands.gameplay.powers.travel.{TollRoads, TravelFixture,
  TyrantCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.{accepted, present}
import distantlands.testkit.Commands

/** What each viewer is shown of consent (consent design, "Projection" and
  * "Testing", hidden information). */
class ConsentProjectionSuite extends munit.FunSuite:
  import ConsentFixture._
  private val projector = new GameProjector(catalog)
  private val askReason = "Narrow Pass: Cannot travel to other sites in " +
    "the region without ruler's consent."

  private def view(at: GameAccepted, viewer: PlayerId): GameProjection =
    projector.project(gameId, at.loaded, viewer)

  /** p1 holds Tyrant faceup at the Cradle's plains; City Wall stands at
    * the coast, which p2 rules with 2 warbands. */
  private val tyrantBesideAWalledCoast: ReadyGame =
    TravelFixture.on(TravelFixture.board(supply = 7))(_
      .adviser(p1, TyrantCard)
      .denizen(CityWallCard.id, at = TravelFixture.coast)
      .warbandsAt(TravelFixture.coast, p2, 2))

  /** p1 travels to the walled coast at `sequence`; the pick's batch. */
  private def travelledToTheWalledCoast(service: GameApplicationService,
      sequence: Long)(using munit.Location): GameAccepted =
    ParkedServiceFixture.travelTo(service, gameId, sequence, p1,
      TravelFixture.coast).picked

  private def dunesRow(at: GameAccepted)(using munit.Location)
      : DecisionOptionProjection =
    present(view(at, p1).walkerDecision.flatMap(_.query).toVector
      .flatMap(_.offeredOptions).find(_.id == dunes.wireId), "the Dunes row")

  private def asking(service: GameApplicationService)(using munit.Location)
      : GameAccepted =
    val started = startTravel(service)
    accepted(service.handle(gameId, started.nextSequence, requestDunes))

  private def answering(service: GameApplicationService, at: GameAccepted,
      choice: ConsentChoice)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence,
      Commands.answerConsent(p2, choice)))

  test("a destination its ruler may be asked about is askable, with the " +
      "consent reason and whom to ask"):
    val (service, _) = ConsentFixture.service()
    val row = dunesRow(startTravel(service))
    assertEquals(row.state, "askable")
    assertEquals(row.reason, Some(askReason))
    assertEquals(row.consent, Some(ConsentAskProjection(Vector("p2"),
      Vector("Narrow Pass"))))
    assert(!row.choosable)

  test("after a refusal it is unavailable, with the ordinary reason"):
    val (service, _) = ConsentFixture.service()
    val row = dunesRow(answering(service, asking(service), ConsentChoice.No))
    assertEquals(row.state, "unavailable")
    assertEquals(row.reason, Some(
      "Narrow Pass: Cannot travel to other sites in the region."))
    assertEquals(row.consent, None)

  test("while a request waits, the requester sees the wait and an Undo " +
      "that withdraws, and no decision"):
    val (service, _) = ConsentFixture.service()
    val shown = view(asking(service), p1)
    assertEquals(shown.walkerDecision, None)
    assertEquals(shown.consentWaiting,
      Some(ConsentWaitingProjection(Vector("p2"))))
    assertEquals(shown.consentPrompt, None)
    assertEquals(shown.undo, Some(StepOfferProjection(
      "Withdraw your consent request")))
    assertEquals(shown.redo, None)

  test("the asked ruler sees the prompt; a third player and the public see " +
      "neither"):
    val (service, _) = ConsentFixture.service()
    val asked = asking(service)
    assertEquals(view(asked, p2).consentPrompt, Some(ConsentPromptProjection(
      "p1", Vector("Narrow Pass"), "Dunes", adjust = "answer")))
    assertEquals(view(asked, p2).consentWaiting, None)
    Vector(view(asked, p3), projector.projectPublic(gameId, asked.loaded))
      .foreach { shown =>
        assertEquals(shown.consentPrompt, None)
        assertEquals(shown.consentWaiting, None)
      }

  test("the Pass's ruler alone sees their preferences, one row per other " +
      "player"):
    val (service, _) = ConsentFixture.service()
    val set = accepted(service.handle(gameId, 0L,
      setPreference(ConsentSetting.Always)))
    assertEquals(view(set, p2).consentPreferences, Vector(
      ConsentPreferencesProjection("site.narrow-pass.pass", "Narrow Pass",
        Vector(ConsentPreferenceRowProjection("p1", "always"),
          ConsentPreferenceRowProjection("p3", "case-by-case")))))
    assertEquals(view(set, p1).consentPreferences, Vector.empty)
    assertEquals(projector.projectPublic(gameId, set.loaded)
      .consentPreferences, Vector.empty)

  private val destination = TravelProcedure.destinationDecisionId

  private def requestsShownTo(viewer: PlayerId, at: GameAccepted)
      : Vector[ConsentRequestRowProjection] =
    view(at, viewer).consentPreferences.flatMap(_.requests)

  private def changing(service: GameApplicationService, at: GameAccepted,
      allow: Boolean)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence,
      Commands.reconsiderConsent(p2, p1, destination, dunes, allow)))

  /** `base` projected for p2 with `ledger` as its consent ledger: a
    * position the service cannot be played to. */
  private def shownToP2With(base: ReadyGame, ledger: ConsentState)
      : GameProjection =
    projector.project(gameId, LoadedGame(OathState.Ready(
      base.copy(consent = ledger)), 0L), p2)

  /** `on` after p2 answered p1's request about `target`. */
  private def answeredByP2(asks: Vector[ConsentAsk],
      target: DecisionOptionRef, granted: Boolean, on: ConsentState,
      base: ReadyGame, decisionId: String = destination): ConsentState =
    accepted(accepted(on.requested(ConsentRequest(p1, decisionId, target,
      asks))).answered(p2, granted, ConsentTurn.of(base)))

  private val passAsk = Vector(ConsentAsk(p2, Vector(NarrowPassSite.pass)))

  test("a power with no requests this turn carries an empty list"):
    val (service, _) = ConsentFixture.service()
    assertEquals(view(startTravel(service), p2).consentPreferences
      .map(_.requests), Vector(Vector.empty))

  test("the ruler's answered request is listed as answered and open to " +
      "change, and nobody else sees it"):
    val (service, _) = ConsentFixture.service()
    val no = answering(service, asking(service), ConsentChoice.No)
    assertEquals(requestsShownTo(p2, no), Vector(ConsentRequestRowProjection(
      "p1", destination, "site", dunes.wireId, "Dunes", "refused",
      Vector.empty, None, "open")))
    assertEquals(requestsShownTo(p1, no), Vector.empty)
    assertEquals(requestsShownTo(p3, no), Vector.empty)
    assertEquals(projector.projectPublic(gameId, no.loaded)
      .consentPreferences, Vector.empty)
    val granted = changing(service, no, allow = true)
    assertEquals(requestsShownTo(p2, granted).map(row =>
      (row.answer, row.change)), Vector(("allowed", "open")))

  test("an entry the requester acted on is used, and open again after " +
      "their undo"):
    val (service, _) = ConsentFixture.service()
    val yes = answering(service, asking(service), ConsentChoice.Yes)
    assertEquals(requestsShownTo(p2, yes).map(row =>
      (row.answer, row.change)), Vector(("allowed", "used")))
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    assertEquals(requestsShownTo(p2, undone).map(row =>
      (row.answer, row.change)), Vector(("allowed", "open")))

  test("an automatic power's answer is listed as automatic, with its " +
      "card"):
    val (service, _) = ConsentFixture.service(tyrantBesideAWalledCoast)
    val waiting = travelledToTheWalledCoast(service, 0L)
    val no = accepted(service.handle(gameId, waiting.nextSequence,
      Commands.answerConsent(p2, ConsentChoice.No)))
    assertEquals(requestsShownTo(p2, no).map(row =>
      (row.use, row.answer, row.change)),
      Vector((Some("Tyrant"), "refused", "automatic")))

  test("an entry waits while a request about the same option does"):
    val base = passRuledByP2
    val answered = answeredByP2(passAsk, dunes, granted = true,
      base.consent, base)
    val waiting = answered.copy(pending = Some(ConsentRequest(p1,
      destination, dunes, Vector(ConsentAsk(p3,
        Vector(NarrowPassSite.pass))))))
    assertEquals(shownToP2With(base, waiting).consentPreferences
      .flatMap(_.requests).map(_.change), Vector("waiting"))

  test("the list is newest first, names the other powers an ask covered, " +
      "and marks an answer that covers a power the viewer lost"):
    val police = SecretPoliceCard.power.id
    val both = TravelFixture.on(passRuledByP2)(_.denizen(
      SecretPoliceCard.id, at = TravelFixture.pass))
    val coast = DecisionOptionRef.Site(TravelFixture.coast)
    val first = answeredByP2(passAsk, dunes, granted = true, both.consent,
      both)
    val second = answeredByP2(Vector(ConsentAsk(p2,
      Vector(NarrowPassSite.pass, police))), coast, granted = false, first,
      both)
    val shown = shownToP2With(both, second).consentPreferences
    assertEquals(shown.find(_.powerId == NarrowPassSite.pass.value)
      .toVector.flatMap(_.requests).map(row =>
        (row.optionId, row.alsoPowers, row.change)), Vector(
      (coast.wireId, Vector("Secret Police"), "open"),
      (dunes.wireId, Vector.empty, "open")))
    assertEquals(shown.find(_.powerId == police.value).toVector
      .flatMap(_.requests).map(row => (row.optionId, row.alsoPowers)),
      Vector((coast.wireId, Vector("Narrow Pass"))))
    // Without the Police on the map, p2 rules the Pass alone.
    assertEquals(shownToP2With(passRuledByP2, second).consentPreferences
      .flatMap(_.requests).map(row => (row.optionId, row.change)), Vector(
      (coast.wireId, "lapsed"), (dunes.wireId, "open")))

  test("a button whose decision is no longer parked reads the log's " +
      "fallback label"):
    val base = passRuledByP2
    val answered = answeredByP2(passAsk,
      DecisionOptionRef.Button("give-way"), granted = true, base.consent,
      base, decisionId = "not-parked")
    assertEquals(shownToP2With(base, answered).consentPreferences
      .flatMap(_.requests).map(_.label), Vector("Give Way"))

  test("a toll's free option keeps its own words in the list after the " +
      "traveller has paid and the step is gone"):
    val (service, _) = ConsentFixture.service(TollConsentFixture.tolled)
    val asked = accepted(service.handle(gameId,
      TollConsentFixture.atTheStep(service).nextSequence,
      TollConsentFixture.requestFree))
    val no = answering(service, asked, ConsentChoice.No)
    assertEquals(requestsShownTo(p2, no).map(_.label),
      Vector("Travel without paying"))
    val paid = accepted(service.handle(gameId, no.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(TollRoads.decisionId,
        DecisionAnswer.ChooseOneAnswer(PayOrAsk.pay)))))
    assertEquals(requestsShownTo(p2, paid).map(_.label),
      Vector("Travel without paying"))

  test("once the game is over an entry stays listed and cannot be " +
      "changed (R24)"):
    val base = passRuledByP2
    val answered = answeredByP2(passAsk, dunes, granted = true,
      base.consent, base)
    val over = base.updateCurrent(_.copy(result =
      Some(GameResult(p1, VictoryKind.Usurper))))
    assertEquals(shownToP2With(over, answered).consentPreferences
      .flatMap(_.requests).map(_.change), Vector("over"))

  test("an answer given to a request that still waits is listed to its " +
      "giver first, as waiting (R26)"):
    val base = passRuledByP2
    val earlier = answeredByP2(passAsk, dunes, granted = true,
      base.consent, base)
    val coast = DecisionOptionRef.Site(TravelFixture.coast)
    val waiting = accepted(accepted(earlier.requested(ConsentRequest(p1,
      destination, coast, Vector(
        ConsentAsk(p2, Vector(NarrowPassSite.pass), alternative = true),
        ConsentAsk(p3, Vector(NarrowPassSite.pass), alternative = true)))))
      .answered(p2, granted = false, ConsentTurn.of(base)))
    assert(waiting.pending.nonEmpty)
    assertEquals(shownToP2With(base, waiting).consentPreferences
      .flatMap(_.requests).map(row =>
        (row.optionId, row.answer, row.change)), Vector(
      (coast.wireId, "refused", "waiting"), (dunes.wireId, "allowed", "open")))
    assertEquals(projector.project(gameId, LoadedGame(OathState.Ready(
      base.copy(consent = waiting)), 0L), p3).consentPreferences
      .flatMap(_.requests), Vector.empty)

  test("the prompt names a card its granter may not identify as a hidden " +
      "card, and its id stays on the server (R25)"):
    val base = TravelFixture.on(passRuledByP2)(_.adviser(p1, TyrantCard,
      facedown = true))
    val asked = base.copy(consent = accepted(base.consent.requested(
      ConsentRequest(p1, "not-parked",
        DecisionOptionRef.Denizen(TyrantCard.id), passAsk))))
    val shown = projector.project(gameId, LoadedGame(OathState.Ready(asked),
      0L), p2)
    assertEquals(shown.consentPrompt.map(_.target), Some("a hidden card"))
    assert(!GameProjectionCodec.encode(shown).contains(
      "\"" + TyrantCard.id.value + "\""))

  test("an entry whose option the viewer may not identify is not listed, " +
      "so its id never leaves the server (R19)"):
    // p1 holds Tyrant facedown: p2 may not name it.
    val base = TravelFixture.on(passRuledByP2)(_.adviser(p1, TyrantCard,
      facedown = true))
    val answered = answeredByP2(passAsk,
      DecisionOptionRef.Denizen(TyrantCard.id), granted = true,
      base.consent, base, decisionId = "not-parked")
    val shown = shownToP2With(base, answered)
    assertEquals(shown.consentPreferences.flatMap(_.requests), Vector.empty)
    assert(!GameProjectionCodec.encode(shown).contains(
      "\"" + TyrantCard.id.value + "\""))

  test("the prompt says what its granter may adjust later: the answer, " +
      "the preferences for an automatic power, nothing for an ask nobody " +
      "rules"):
    val (service, _) = ConsentFixture.service()
    assertEquals(view(asking(service), p2).consentPrompt.map(_.adjust),
      Some("answer"))
    val (walled, _) = ConsentFixture.service(tyrantBesideAWalledCoast)
    assertEquals(view(travelledToTheWalledCoast(walled, 0L), p2)
      .consentPrompt.map(_.adjust), Some("preferences"))
    val law = passRuledByP2.copy(consent = ConsentState(pending = Some(
      ConsentRequest(p1, destination, dunes, Vector(ConsentAsk(p2,
        Vector(ImperialLaws.chancellorsEye)))))))
    assertEquals(projector.project(gameId, LoadedGame(OathState.Ready(law),
      0L), p2).consentPrompt.map(_.adjust), Some("none"))

  test("a site power carries its power id"):
    val (service, _) = ConsentFixture.service()
    val shown = projector.projectPublic(gameId, startTravel(service).loaded)
    assertEquals(shown.world.flatMap(_.sites)
      .find(_.siteId == NarrowPassSite.id.value).map(_.powers.map(_.id)),
      Some(Vector("site.narrow-pass.pass")))

  test("the requester's view cannot tell an automatic grant from a manual " +
      "one"):
    val (manualService, _) = ConsentFixture.service()
    val manual = answering(manualService, asking(manualService),
      ConsentChoice.Yes)
    val (autoService, _) = ConsentFixture.service()
    val set = accepted(autoService.handle(gameId, 0L,
      setPreference(ConsentSetting.Always)))
    val started = startTravel(autoService, set.nextSequence)
    val automatic = accepted(autoService.handle(gameId, started.nextSequence,
      requestDunes))
    assertEquals(view(automatic, p1).copy(nextSequence = 0L),
      view(manual, p1).copy(nextSequence = 0L))

  test("a card's waivable power carries the card's id; a site's carries " +
      "none"):
    val police = SecretPoliceCard.id
    val home = AncientCitySite.id
    val ready = Table.start.denizen(police, at = home)
      .warbandsAt(home, p2, 2).ready
    assertEquals(projector.project(gameId, LoadedGame(OathState.Ready(ready),
      0L), p2).consentPreferences.map(entry => (entry.powerId, entry.cardId)),
      Vector(("denizen.secret-police", Some(police.value))))

  test("a ruined edifice's waivable power is named for its ruined face"):
    val home = AncientCitySite.id
    def named(side: EdificeSide): Vector[String] =
      val ready = Table.start.edifice(RelicPatrolsCard, side, at = home)
        .warbandsAt(home, p2, 2).ready
      projector.project(gameId, LoadedGame(OathState.Ready(ready), 0L), p2)
        .consentPreferences.map(_.power)
    assertEquals(named(EdificeSide.Ruined), Vector("Bandit Patrols"))
    assertEquals(named(EdificeSide.Intact), Vector("Relic Patrols"))

  test("the Festival's holder sees its preferences keyed by the banner, " +
      "and no card"):
    val ready = Table.start
      .darkestSecret(Some(p2), 1, face = DarkestSecretFace.Festival).ready
    assertEquals(projector.project(gameId, LoadedGame(OathState.Ready(ready),
      0L), p2).consentPreferences.map(entry =>
        (entry.power, entry.cardId, entry.bannerKey)),
      Vector(("Darkest Secret", None, Some("darkest-secret"))))
    assertEquals(projector.project(gameId, LoadedGame(OathState.Ready(ready),
      0L), p1).consentPreferences, Vector.empty)

  test("City Wall's ruler is asked whether to deactivate the traveller's " +
      "Tyrant, and the traveller waits on that decision"):
    val (service, _) = ConsentFixture.service(tyrantBesideAWalledCoast)
    val waiting = travelledToTheWalledCoast(service, 0L)
    val prompt = present(view(waiting, p2).consentPrompt, "p2's prompt")
    assertEquals((prompt.requesterPlayerId, prompt.powers, prompt.use),
      ("p1", Vector("City Wall"), Some("Tyrant")))
    assertEquals(view(waiting, p1).consentWaiting, Some(
      ConsentWaitingProjection(Vector("p2"), use = Some("Tyrant"))))

  test("the traveller's view cannot tell an automatic deactivation, or " +
      "an automatic grant, from a manual one"):
    for (choice, setting) <- Vector(
        (ConsentChoice.No, ConsentSetting.Never),
        (ConsentChoice.Yes, ConsentSetting.Always)) do
      val (manualService, _) = ConsentFixture.service(
        tyrantBesideAWalledCoast)
      val manual = accepted(manualService.handle(gameId,
        travelledToTheWalledCoast(manualService, 0L).nextSequence,
        Commands.answerConsent(p2, choice)))
      val (autoService, _) = ConsentFixture.service(tyrantBesideAWalledCoast)
      val set = accepted(autoService.handle(gameId, 0L,
        Commands.setConsentPreference(p2, CityWall.id, p1, setting)))
      val automatic = travelledToTheWalledCoast(autoService,
        set.nextSequence)
      assertEquals(view(automatic, p1).copy(nextSequence = 0L),
        view(manual, p1).copy(nextSequence = 0L))
