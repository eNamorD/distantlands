package distantlands.gameplay.chronicle

import distantlands.gameplay.OathRules
import distantlands.gameplay.WalkerDiceFixture
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.{ChronicleAccounting, GameStartRules,
  LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerCompleted
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{MenuPlay, Outcomes, Situation, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** A game in which a site was explored, through the Chronicle Phase and
  * into the next game (Foundations design, slice 3, "Tests"). The game is
  * set up with no Empire on a former Empire of three sites, which erodes
  * Foundation I to Wide Horizons. p1 explores the Forgotten end from the
  * Hintermost site, then wakes as the Usurper, wins, and every task is
  * answered plainly. */
class ExploredSiteChronicleSuite extends munit.FunSuite:
  import EvolvedChronicleFixture.rules

  /** A Chronicle whose Empire of three sites nobody at the table belongs
    * to. */
  private val absentEmpireChronicle: Chronicle =
    LaterGameFixture.chronicle(LineageId("l9"), Vector(LineageId("l8")), 3)

  private val forgotten = absentEmpireChronicle.atlasBox.last

  /** The no-Empire game after Setup, in p1's Act with p1's pawn at the
    * Hintermost of its three sites. */
  private lazy val exilesAtTheFrontier: ReadyGame =
    val setUp = Situation.wake(Situation.rules(catalog),
      absentEmpireChronicle,
      LaterGameFixture.orders(absentEmpireChronicle, p1)).ready
    Table.from(MenuPlay.withoutMenu(setUp)).turn(p1, Phase.Act)
      .pawn(p1, absentEmpireChronicle.world.last.site).ready

  /** p1's Explore of the Forgotten end, rolled as four shields. */
  private lazy val explored: ReadyGame =
    val rolling = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = WalkerDiceFixture.shields)
    val rolled = accepted(rolling.startWalker(Ready(exilesAtTheFrontier),
      ActionRef.Explore, p1))
    val placed = accepted(rolling.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Forgotten))))
    assert(placed.events.contains(WalkerCompleted(ActionRef.Explore)),
      placed.events)
    Outcomes.ready(placed.state)

  /** The game after the Explore, as p1's Wake begins with the Usurper side
    * of the title. */
  private lazy val usurperWakesAfterExploring: OathState =
    Table.from(MenuPlay.withoutMenu(explored)).turn(p1, Phase.Wake)
      .oathkeeper(Some(p1), TitleSide.Usurper).state

  private lazy val won = EvolvedChronicleFixture.gameWonFrom(
    usurperWakesAfterExploring)
  private lazy val done = EvolvedChronicleFixture.chronicleAnsweredAfter(won)
  private lazy val journaled = EvolvedChronicleFixture.chronicleJournaledBy(
    done)

  test("Setup With No Empire erodes Foundation I, so its Exiles may " +
      "Explore, and the explored site joins the map with its stored cards"):
    assertEquals(ExploreProcedure.refusal(exilesAtTheFrontier, p1), None)
    val map = explored.game.current.map
    assertEquals(map.hinterward,
      absentEmpireChronicle.world.map(_.site) :+ forgotten.site)
    assertEquals(map.sites(forgotten.site).denizens.map(_.id),
      forgotten.items.collect {
        case id: EdificeId => id: CardId
        case id: DenizenId => id: CardId
      })
    assertEquals(explored.game.campaign.atlas.sites.map(_.id),
      absentEmpireChronicle.atlasBox.init.map(_.site))

  test("the Chronicle Phase stores the explored site like any other: " +
      "every card is conserved, and the journal replays"):
    assert(done.events.contains(
      WalkerCompleted(TriggeredProcedureRef.ChronicleFinish)), done.events)
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, journaled),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(absentEmpireChronicle,
      journaled), Right(()))
    assertEquals(accepted((won.events ++ done.events)
      .foldLeft[Either[OathViolation, OathState]](
        Right(usurperWakesAfterExploring))((state, event) =>
        state.flatMap(rules.evolve(_, event)))), done.state)

  test("the Stars keep or archive the explored site with the map's other " +
      "sites, ahead of the Atlas sites nobody explored, which keep their " +
      "order"):
    val layout = accepted(ChronicleExtraction.layout(
      Outcomes.ready(done.state)))
    val onMap = absentEmpireChronicle.world.map(_.site) :+ forgotten.site
    assertEquals((layout.empire ++ layout.rivals).toSet, onMap.toSet)
    assert((layout.empire ++ layout.rivals).contains(forgotten.site))
    assertEquals(journaled.world.map(_.site), layout.empire)
    assertEquals(journaled.atlasBox.map(_.site),
      layout.rivals ++ absentEmpireChronicle.atlasBox.init.map(_.site))
    assertEquals((journaled.world ++ journaled.atlasBox).map(_.site).toSet,
      (absentEmpireChronicle.world ++ absentEmpireChronicle.atlasBox)
        .map(_.site).toSet)

  test("the next game starts from the journaled Chronicle under Wide " +
      "Horizons with the explored site in its Atlas or its Empire"):
    val orders = LaterGameFixture.orders(journaled, p1)
    val next = accepted(GameStartRules.evolve(catalog, journaled, orders))
    assertEquals(next.game.campaign.foundations(FoundationNumber.I).face,
      FoundationFace.Altered)
    assertEquals(next.game.current.map.inPlay, journaled.world.map(_.site))
    val everywhere = next.game.current.map.inPlay ++
      next.game.campaign.atlas.sites.map(_.id)
    assert(everywhere.contains(forgotten.site), everywhere)
    assertEquals(everywhere.size, 8)
