package distantlands.application

import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.BuriedGiantSite
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.LaterGameFixture
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Dice, MenuPlay, Situation, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.{accepted, present}

/** Explore as a table sees it (Foundations design, slice 3): the Act
  * menu's option, the roll beside its questions, and the map after it, in a
  * game started from a Chronicle and journaled by the service. */
class ExploreGameSuite extends munit.FunSuite:
  private val projector = new GameProjector(catalog)

  /** p1's Act menu, parked, under Wide Horizons on a map of three sites
    * with p1's pawn at Buried Giant, the Hintermost. */
  private def actMenuAtHintermostOfThree: Table = Table.start
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
    .phaseMenuParked

  private def exploreOption(table: Table, stream: String) =
    val projected = projector.project(stream, LoadedGame(table.state, 1L), p1)
    present(present(projected.walkerDecision.flatMap(_.query),
      "the Act menu").offeredOptions.find(_.id == "explore"),
      "the menu's Explore option")

  test("the projected Act menu lists Explore among the major actions with " +
      "its cost"):
    val option = exploreOption(actMenuAtHintermostOfThree, "explore-menu")
    assertEquals(option.label, "Explore")
    assertEquals(option.group, Some("major"))
    assertEquals(option.badge, Some("1 Supply"))
    assertEquals(option.state, "available")

  test("under Imperial Maps the projected Act menu lists no Explore"):
    val projected = projector.project("explore-hidden",
      LoadedGame(Table.start.phaseMenuParked.state, 1L), p1)
    val ids = present(projected.walkerDecision.flatMap(_.query),
      "the Act menu").offeredOptions.map(_.id)
    assert(!ids.contains("explore"), ids)
    assert(ids.contains("recover"), ids)

  /** A later game's Chronicle under Wide Horizons: l1 is the Chancellor of
    * an Empire of three sites, and five sites wait in the Atlas, each with
    * what the Chronicle stores on it. */
  private val wideHorizonsChronicle: Chronicle =
    LaterGameFixture.chronicle(LineageId("l1"), 3).copy(foundations = Map(
      FoundationNumber.I -> FoundationState(FoundationFace.Altered,
        Set.empty)))
  private val hintermost = wideHorizonsChronicle.world.last.site
  private val recent = wideHorizonsChronicle.atlasBox.head

  /** The game set up from [[wideHorizonsChronicle]] on stream `name`, every
    * pawn placed at the Hintermost site, as the Chancellor's Act begins.
    * The defense dice roll blanks, then two shields each. */
  private def chancellorActs(name: String)
      : (GameApplicationService, Situation) =
    val repository = new InMemoryEventStreamRepository
    val service = new GameApplicationService(catalog, repository,
      campaignDicePort = new Dice.ScriptedDefense(Vector(
        Vector(DefenseDieFace.Blank, DefenseDieFace.Blank),
        Vector(DefenseDieFace.TwoShields, DefenseDieFace.TwoShields))))
    val woken = Situation.wake(Situation.journaled(service, catalog,
        repository, name).withAnswers(Situation.pawnsAt(
          Vector.fill(3)(hintermost))),
      wideHorizonsChronicle, LaterGameFixture.orders(wideHorizonsChronicle,
        p1))
    (service, woken.endingWake(p1))

  test("in a game started under Wide Horizons, the Chancellor's short " +
      "roll shows the Explore dice against 4 beside the question"):
    val (service, act) = chancellorActs("explore-short-roll")
    val short = act.parkedAfter(MenuPlay.start(p1, ActionRef.Explore))
    val decision = present(projector.project("explore-short-roll",
      present(accepted(service.load("explore-short-roll")), "the game"), p1)
      .walkerDecision, "the continue-or-stop question")
    assertEquals(decision.action, "explore")
    assertEquals(decision.decisionId, ExploreProcedure.choiceDecisionId)
    assertEquals(decision.rollOutcome.map(roll =>
      (roll.pool, roll.score, roll.target)), Some(("explore", 0, Some(4))))
    assertEquals(present(decision.query, "its query").offeredOptions
      .map(option => option.id -> option.label), Vector(
        "continue" -> "Roll 2 more dice (1 Supply)",
        "stop" -> "Stop exploring"))
    assertEquals(short.ready.game.current.map.inPlay.size, 3)

  test("the explored site fills the empty frame: the projected region " +
      "gains the site and loses a slot, and the journal replays to the " +
      "same game"):
    val (service, act) = chancellorActs("explore-game")
    val before = projector.project("explore-game",
      present(accepted(service.load("explore-game")), "the game"), p1).world
    assertEquals(before.map(_.sites.size), Vector(2, 1, 0))
    assertEquals(before.map(_.emptySlots), Vector(0, 2, 3))
    val explored = act.withAnswers {
      case park if park.decisionId == ExploreProcedure.choiceDecisionId =>
        ChooseOneAnswer(ExploreProcedure.continueOption)
      case park if park.decisionId == ExploreProcedure.endDecisionId =>
        ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))
    }.after(MenuPlay.start(p1, ActionRef.Explore))
    val loaded = present(accepted(service.load("explore-game")), "the game")
    // The service folds its journal to the state the commands reached.
    assertEquals(loaded.state, explored.state)
    val game = explored.ready.game
    assertEquals(game.current.map.provinces, Vector(hintermost, recent.site))
    assertEquals(game.current.pawnSite(p1), Some(recent.site))
    // The Chronicle stores an edifice on the site: it is placed ruined.
    val stored = recent.items.collect { case id: EdificeId => id }
    assert(stored.nonEmpty)
    assertEquals(game.current.map.sites(recent.site).denizens.collect {
      case EdificeState(id, side, _) => id -> side
    }, stored.map(_ -> EdificeSide.Ruined))
    assert(game.cardIndex.isRight, game.cardIndex)
    val world = projector.project("explore-game", loaded, p1).world
    assertEquals(world.map(_.sites.size), Vector(2, 2, 0))
    assertEquals(world.map(_.emptySlots), Vector(0, 1, 3))
    assertEquals(world(1).sites.last.siteId, recent.site.value)
