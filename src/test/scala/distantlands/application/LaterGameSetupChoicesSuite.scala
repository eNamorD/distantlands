package distantlands.application

import distantlands.gameplay.setup.{AtlasPlacement, FirstGameSetupFixture,
  GameStartRules, ImperialMaps, LaterGameFixture, SetupPawnSites,
  SetupProcedure}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Answers, ParkedNode, Situation}
import distantlands.testkit.Unwrap.{accepted, present}

/** Setup's choices in a later game: l1 is the seated Chancellor, l2 a
  * Citizen and l3 an Exile; p1 goes first. */
class LaterGameSetupChoicesSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val (p1, p2, p3) = (PlayerId("p1"), PlayerId("p2"), PlayerId("p3"))
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val record = LaterGameFixture.chronicle(l1, l2)
  private val Vector(kept, other) = record.relicDeck.take(2)
  /** `record` with two relics, `kept` and `other`, in the Reliquary. */
  private val withReliquary = record.copy(relicDeck = record.relicDeck.drop(2),
    reliquary = Vector(kept, other))

  private def started(chronicle: Chronicle): ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1)))

  private def begin(chronicle: Chronicle, answers: Answers =
      PartialFunction.empty): Situation =
    Situation.start(Situation.rules(catalog).withAnswers(answers))
      .parkedAfter(GameCommand.Begin(chronicle,
        LaterGameFixture.orders(chronicle, p1)))

  /** `chronicle`'s Setup once Imperial Maps has filled the map from the
    * Recent end: parked at the Chancellor's pawn question. */
  private def filledFromRecent(chronicle: Chronicle): ReadyGame =
    Situation.start(Situation.rules(catalog))
      .parkedAt(SetupProcedure.pawnDecisionId(p1))(GameCommand.Begin(
        chronicle, LaterGameFixture.orders(chronicle, p1))).ready

  private def parkedOn(situation: Situation): ParkedNode =
    present(accepted(ParkedNode.of(situation.state, catalog)), "a park")

  private def player(ready: ReadyGame, id: PlayerId): PlayerState =
    ready.game.current.player(id).get

  test("the Chancellor first keeps one Reliquary relic facedown (R29)"):
    val parked = begin(withReliquary)
    assertEquals(parkedOn(parked).decisionId,
      SetupProcedure.reliquaryDecisionId)
    assertEquals(parkedOn(parked).awaiting, p1)
    val woken = parked.withAnswers {
      case park if park.decisionId == SetupProcedure.reliquaryDecisionId =>
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Relic(kept))
    }.after()
    assert(player(woken.ready, p1).relics.contains(RelicState(kept,
      Orientation.FaceDown, Tokens.empty)))
    assertEquals(woken.ready.game.campaign.reliquary, Vector(other))

  test("an empty Reliquary skips the pick: Imperial Maps asks first"):
    assertEquals(parkedOn(begin(record)).decisionId,
      ImperialMaps.decisionId)

  test("Imperial players may start only at Empire sites"):
    val ready = filledFromRecent(record)
    Vector(p1, p2).foreach(player => assertEquals(
      SetupPawnSites.legal(catalog, ready, player), record.world.map(_.site)))

  test("an Exile may start at the Hintermost Empire site or a site the " +
      "bandits rule, never at an empty site (R25)"):
    // The first site Imperial Maps placed is emptied, as Sunken Isles'
    // capacity 0 leaves it.
    val ready = filledFromRecent(record)
    val emptied = ready.game.current.map.hinterward(2)
    val bare = ready.updateCurrent(current => current.copy(map =
      current.map.copy(sites = current.map.sites.updatedWith(emptied)(
        _.map(_.copy(forces = SiteForces.Empty))))))
    assertEquals(SetupPawnSites.legal(catalog, bare, p3),
      record.world.last.site +: ready.game.current.map.hinterward.drop(3))

  test("a first game offers every site"):
    val firstGame = FirstGameSetupFixture.freshReady
    assertEquals(SetupPawnSites.legal(catalog, firstGame, p1),
      firstGame.game.current.map.inPlay)

  test("a player with a starting adviser is not asked to choose one"):
    val adviser = DenizenState(record.worldDeck.last, Orientation.FaceDown,
      Tokens.empty)
    val advised = record.copy(worldDeck = record.worldDeck.init,
      lineages = record.lineages.map(lineage =>
        if lineage.id == l2 then
          lineage.copy(startingAdvisers = Vector(adviser))
        else lineage))
    var asked = Vector.empty[String]
    val recording: Answers =
      case park if { asked :+= park.decisionId; false } => ???
    begin(advised, recording).after()
    assert(asked.contains(SetupProcedure.adviserDecisionId(p1)), asked)
    assert(asked.contains(SetupProcedure.pawnDecisionId(p2)), asked)
    assert(!asked.contains(SetupProcedure.adviserDecisionId(p2)), asked)

  test("only the Chancellor identifies the Reliquary's relics, and only in " +
      "Setup"):
    val presentation = new GamePresentationProjector(catalog)
    val setup = started(withReliquary)
    val wake = setup.updateCurrent(current => current.copy(turn =
      current.turn.copy(phase = Phase.Wake)))
    def identifies(ready: ReadyGame, viewer: PlayerId) =
      presentation.identifiesCard(ready, Some(viewer), kept, None,
        CardContainer.Reliquary)
    assert(identifies(setup, p1))
    assert(!identifies(setup, p2))
    assert(!identifies(wake, p1))

  test("the parked Reliquary pick is the Chancellor's to see"):
    val parked = Situation.start(Situation.rules(catalog))
      .parkedAfter(GameCommand.Begin(withReliquary,
        LaterGameFixture.orders(withReliquary, p1))).state
    val projector = new WalkerDecisionProjector(catalog,
      new GamePresentationProjector(catalog))
    val ready = parked match
      case Ready(ready) => ready
      case other => fail(other.toString)
    val own = present(projector.project(ScopedProjectionContext(ready,
      Some(p1))), "p1's decision")
    assertEquals(own.query.toVector.flatMap(_.offeredOptions).size, 2)
    assertEquals(projector.project(ScopedProjectionContext(ready, Some(p2))),
      None)

  test("the parked Imperial Maps question is the Chancellor's, and names " +
      "the two ends and only the first Recent site"):
    val parked = begin(record).ready
    val projector = new WalkerDecisionProjector(catalog,
      new GamePresentationProjector(catalog))
    val own = present(projector.project(ScopedProjectionContext(parked,
      Some(p1))), "p1's decision")
    assertEquals(own.decisionId, ImperialMaps.decisionId)
    assertEquals(own.query.toVector.flatMap(_.offeredOptions)
      .map(option => option.id -> option.label), Vector(
        "recent" -> "Recent Sites (Buried Giant first)",
        "forgotten" -> "Forgotten Sites"))
    // The Forgotten end's site, Golden Valley, is private.
    assert(!own.toString.contains("Golden Valley"), own)
    assert(!own.toString.contains("golden-valley"), own)
    assertEquals(projector.project(ScopedProjectionContext(parked, Some(p2))),
      None)

  test("a later game saved mid-Foundation III, between two keeps, " +
      "replays its journal to the same game (catalog batch 13, ruling 3)"):
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val keeping = Situation.start(Situation.journaled(service, catalog,
        repository, "keeping")).parkedAt(SetupProcedure.legacyDecisionId(p2))(
      GameCommand.Begin(record, LaterGameFixture.orders(record, p1)))
    assert(keeping.ready.game.campaign.lineages(l1).legacies.nonEmpty,
      "p1 kept before p2's keep")
    assertEquals(keeping.ready.game.current.pawnSite(p1), None)
    val loaded = present(accepted(service.load("keeping")), "the game")
    assertEquals(loaded.state, keeping.state)

  test("a Setup filled from Forgotten Sites replays its journal to the " +
      "same game, and the projected map has no empty slot"):
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository)
    val filled = Situation.start(Situation.journaled(service, catalog,
        repository, "forgotten").withAnswers {
        case park if park.decisionId == ImperialMaps.decisionId =>
          DecisionAnswer.ChooseOneAnswer(
            AtlasPlacement.endOption(AtlasEnd.Forgotten))
      }).parkedAt(SetupProcedure.pawnDecisionId(p1))(GameCommand.Begin(
        record, LaterGameFixture.orders(record, p1)))
    val loaded = present(accepted(service.load("forgotten")), "the game")
    assertEquals(loaded.state, filled.state)
    assertEquals(filled.ready.game.current.map.hinterward,
      (record.world ++ record.atlasBox.reverse).map(_.site))
    val world = new GameProjector(catalog).project("forgotten", loaded, p1)
      .world
    assertEquals(world.map(_.sites.size), Vector(2, 3, 3))
    assertEquals(world.map(_.emptySlots), Vector(0, 0, 0))
