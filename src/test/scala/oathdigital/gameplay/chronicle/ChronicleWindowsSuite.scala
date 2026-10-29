package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerPowers
import oathdigital.model._
import oathdigital.testkit.Outcomes
import oathdigital.testkit.Table.p2
import oathdigital.testkit.Unwrap.accepted

/** Every Chronicle task step runs inside its own power window, so a later
  * legacy power can add to it (Chronicle steps design, "Windows, not
  * hardcoded actors"). The fake power gives p2 one lineage point per
  * window it is folded into. */
class ChronicleWindowsSuite extends munit.FunSuite:
  private final case class PointPerWindow(lineage: LineageId,
      windows: Vector[PowerWindow]) extends ContributingPower:
    val id: PowerId = PowerId("test.chronicle.point-per-window")
    val source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    val contributions: Map[PowerWindow, Vector[Contribution]] =
      windows.map(window => window -> Vector[Contribution](Transform(
        (_, ops) => ops :+ BuildOps((_, _) =>
          Right(Vector(LineagePointsScored(lineage, 1))))))).toMap

  private def lineageOf(state: OathState, player: PlayerId): LineageId =
    Outcomes.ready(state).game.current.players.find(_.player == player)
      .get.lineage

  test("a power folded into every Chronicle window runs once per window"):
    val state = ChronicleWalk.chronicleWonByP1.state
    val lineage = lineageOf(state, p2)
    val windows = ChronicleWindows.sun ++ ChronicleWindows.throne ++
      ChronicleWindows.world ++ ChronicleWindows.beacon ++
      ChronicleWindows.stars
    val rules = new OathRules(catalog,
      walkerPowerCatalog =
        WalkerPowers(Vector(PointPerWindow(lineage, windows))),
      walkerDice = WalkerDiceFixture.reversingShuffle)
    val done = ChronicleWalk.answeringEverything(rules, accepted(rules.next(
      OathTransition(state, Vector.empty),
      TriggeredProcedureRef.ChronicleBegin)))
    assertEquals(Outcomes.ready(done.state).game.campaign.era
      .score(lineage), windows.size)

  test("Chronicle window keys stay as power data spells them"):
    assertEquals(
      (ChronicleWindows.sun.map(_.key), ChronicleWindows.throne.map(_.key)), (
      Vector("chronicle.sun.reveal-relics", "chronicle.sun.flip-legacies",
        "chronicle.sun.exile-dormancy", "chronicle.sun.legacy-cap",
        "chronicle.sun.shuffle-legacies", "chronicle.sun.crown",
        "chronicle.sun.assign-tasks"),
      Vector("chronicle.throne.offer-citizenship",
        "chronicle.throne.shape-empire", "chronicle.throne.build-edifice",
        "chronicle.throne.stock-reliquary", "chronicle.throne.scatter-relics",
        "chronicle.throne.score-lineage", "chronicle.throne.swear-oath")))

  test("World and Beacon window keys stay as power data spells them"):
    assertEquals(
      (ChronicleWindows.world.map(_.key), ChronicleWindows.beacon.map(_.key)), (
      Vector("chronicle.world.separate-visions",
        "chronicle.world.gather-denizens",
        "chronicle.world.protect-and-dispossess"),
      Vector("chronicle.beacon.introduce-newcomers",
        "chronicle.beacon.welcome-stranger",
        "chronicle.beacon.shape-foundation")))

  test("Stars window keys stay as power data spells them"):
    assertEquals(ChronicleWindows.stars.map(_.key),
      Vector("chronicle.stars.write-in-the-stars",
        "chronicle.stars.archive-rival-sites", "chronicle.stars.clean-up",
        "chronicle.stars.store"))

  test("the Throne is followed by the World, the Beacon, then the Stars"):
    assertEquals(Vector(TriggeredProcedureRef.ChronicleThrone,
      TriggeredProcedureRef.ChronicleWorld,
      TriggeredProcedureRef.ChronicleBeacon).map(ChronicleSequence.after),
      Vector(Some(TriggeredProcedureRef.ChronicleWorld),
        Some(TriggeredProcedureRef.ChronicleBeacon),
        Some(TriggeredProcedureRef.ChronicleStars)))
