package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{BrokenPeaksSite, BuriedGiantSite}
import distantlands.gameplay.chronicle.legacies.{RoyalLine, RoyalLineCard}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Ruined Temple at the win (catalog batch 13, ruling 18): the pledged
  * final winner takes every bandit site, once Royal Line and Conspirator
  * have settled who won and before the Sun. Every game here ended with
  * bandits at Broken Peaks and Buried Giant, among any others. */
class RuinedTempleWinSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** `table` at the game's end, before the Chronicle, with bandits at two
    * known sites. */
  private def ended(table: Table): Table = table
    .bandits(BrokenPeaksSite, 2).bandits(BuriedGiantSite, 1)
    .turn(p1, Phase.RoundEnd)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, ended(table), procedure, answers,
      after = PhaseTransitionRef.FinishRest)

  /** The sites holding bandits in `table` once it has ended. */
  private def banditSites(table: Table): Vector[SiteId] =
    val current = ended(table).ready.game.current
    current.map.inPlay.filter(site =>
      current.map.sites(site).forces.count(ForceKind.Bandit) > 0)

  private def forcesAt(ready: ReadyGame, sites: Vector[SiteId])
      : Vector[SiteForces] =
    sites.map(ready.game.current.map.sites(_).forces)

  private def kindOf(ready: ReadyGame, player: PlayerId): ForceKind =
    PlayerForceKind.of(ready, ready.game.current.player(player).get).get

  /** The Temple's own lines: the final winner step's note id carries Royal
    * Line's and Conspirator's too. */
  private def lines(run: ChronicleWalk.Run): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleWinner),
      RuinedTempleWin.noteKeys, run.events)
      .filter(said => RuinedTempleWin.noteKeys.exists(_.name == said.key))
      .map(_.text)

  private def won: Table = ChronicleWalk.chronicleWonByP1

  test("a pledged Exile winner takes every bandit site with one warband"):
    val table = won.pledgedAtRuinedTemple(p1)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner)
    assertEquals(forcesAt(run.ready, sites), sites.map(_ =>
      SiteForces.Occupied(kindOf(run.ready, p1), 1)))
    assertEquals(lines(run),
      Vector(s"p1 took rule of ${sites.size} bandit sites with Ruined Temple"))

  test("Crown Chancellor then turns those sites Imperial for an Exile " +
      "winner"):
    val table = won.pledgedAtRuinedTemple(p1)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleSun)
    assertEquals(forcesAt(run.ready, sites), sites.map(_ =>
      SiteForces.Occupied(ForceKind.Imperial, 1)))

  test("a pledged Imperial winner takes them with Imperial warbands"):
    val table = won.imperial(p1).pledgedAtRuinedTemple(p1)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner)
    assertEquals(forcesAt(run.ready, sites), sites.map(_ =>
      SiteForces.Occupied(ForceKind.Imperial, 1)))

  test("a winner who did not pledge takes nothing, even when another " +
      "player pledged"):
    val table = won.pledgedAtRuinedTemple(p2)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner)
    assert(forcesAt(run.ready, sites).forall(_.count(ForceKind.Bandit) > 0))
    assertEquals(lines(run), Vector.empty)
    assert(!RuinedTempleWin.doubles(run.ready, p2))

  test("a winner whose bank runs short has it topped up"):
    val table = won.pledgedAtRuinedTemple(p1).warbandsInBank(p1, 1)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner)
    assertEquals(RecordedOps.all(run.events).collect {
      case WarbandsAdded(kind, count) => kind -> count },
      Vector(kindOf(run.ready, p1) -> (sites.size - 1)))
    assertEquals(forcesAt(run.ready, sites), sites.map(_ =>
      SiteForces.Occupied(kindOf(run.ready, p1), 1)))

  test("with no bandit site a pledged winner takes nothing and writes " +
      "nothing"):
    val cleared = won.pledgedAtRuinedTemple(p1).update(_.updateCurrent(
      current => current.copy(map = current.map.copy(sites =
        current.map.sites.view.mapValues(site =>
          if site.forces.count(ForceKind.Bandit) > 0 then
            site.copy(forces = SiteForces.Empty)
          else site).toMap))))
    val run = ChronicleWalk.through(rules, cleared.turn(p1, Phase.RoundEnd),
      TriggeredProcedureRef.ChronicleWinner, PartialFunction.empty,
      after = PhaseTransitionRef.FinishRest)
    assertEquals(lines(run), Vector.empty)
    assert(!RecordedOps.all(run.events).exists(_.isInstanceOf[WarbandsAdded]))

  test("the final winner Royal Line names is the one who takes the sites"):
    val table = won.imperial(p1, p2, p3)
      .update(_.updateCurrent(_.copy(result =
        Some(GameResult(p1, VictoryKind.StableRegime)))))
      .legacy(p3, RoyalLineCard).pledgedAtRuinedTemple(p2)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner, {
      case decide if decide.decisionId == RoyalLine.decisionId(p3) =>
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(p2)) })
    assertEquals(forcesAt(run.ready, sites), sites.map(_ =>
      SiteForces.Occupied(ForceKind.Imperial, 1)))
    assertEquals(lines(run),
      Vector(s"p2 took rule of ${sites.size} bandit sites with Ruined Temple"))
