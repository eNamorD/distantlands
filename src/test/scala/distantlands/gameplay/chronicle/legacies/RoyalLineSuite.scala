package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.{ChronicleWalk, LineageScoring}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.VillageIdiotCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{Outcomes, RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Royal Line (L10): "If you are a Citizen: Chronicle (Sun): You may choose
  * which Imperial meets the Successor goal. Ignore the printed goal. You
  * may choose no one." (rulings 22 and 23; R37-R41). p1 is the Chancellor,
  * p2 and p3 Citizens. */
class RoyalLineSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** The game ended in `result`, p3 holding Royal Line active. */
  private def ended(result: GameResult): Table =
    ChronicleWalk.chronicleWonByP1.imperial(p1, p2, p3)
      .update(_.updateCurrent(_.copy(result = Some(result))))
      .legacy(p3, RoyalLineCard)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, procedure, answers)

  private def naming(player: PlayerId): PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == RoyalLine.decisionId(p3) =>
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(player))

  private def asked(run: ChronicleWalk.Run): Option[Decide] =
    run.asked.find(_.decisionId == RoyalLine.decisionId(p3))

  private def lines(run: ChronicleWalk.Run, procedure: TriggeredProcedureRef,
      keys: Vector[NoteKey]): Vector[String] =
    NoteText.said(ProcedureNotes.id(procedure), keys, run.events)
      .filter(said => keys.exists(_.name == said.key)).map(_.text)

  private def sunLines(run: ChronicleWalk.Run): Vector[String] =
    lines(run, TriggeredProcedureRef.ChronicleSun,
      LegacyChroniclePowers.sunNoteKeys)

  private val stable = GameResult(p1, VictoryKind.StableRegime)

  test("Royal Line is implemented"):
    assert(PowerImplementationStatus.implemented(RoyalLineCard.power.id))

  test("the holder chooses from every Imperial, the Chancellor first, with " +
      "no Decline (ruling 22; R38)"):
    val decide = asked(through(ended(stable),
      TriggeredProcedureRef.ChronicleSun)).get
    assertEquals(decide.owner, p3)
    assertEquals(decide.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref), Vector(p1, p2, p3).map(DecisionOptionRef.Player(_)))

  test("naming a Citizen makes them the Successor, whom the Crown and the " +
      "tasks follow (ruling 23)"):
    val run = through(ended(stable), TriggeredProcedureRef.ChronicleSun,
      naming(p2))
    assertEquals(run.ready.game.current.result,
      Some(GameResult(p2, VictoryKind.Successor)))
    assertEquals(Imperials.chancellor(run.ready), Some(p2))
    assertEquals(run.ready.game.current.chronicleTasks
      .get(ChronicleTask.Throne), Some(p2))
    assert(sunLines(run).contains(
      s"p3 named p2 the winner with ${RoyalLineCard.id.value}"),
      sunLines(run))

  test("naming the Chancellor in a Successor's place hands back the " +
      "Empire's win the game ended in (ruling 23; R37)"):
    // Round 1, no end die: War Exhaustion, whose Chancellor's win is Stable
    // Regime with an Imperial Oathkeeper and exhaustion otherwise.
    val successor = GameResult(p2, VictoryKind.Successor)
    Vector(None -> VictoryKind.ChancellorExhaustion,
        Some(p2) -> VictoryKind.StableRegime).foreach((oathkeeper, kind) =>
      val run = through(ended(successor).oathkeeper(oathkeeper),
        TriggeredProcedureRef.ChronicleSun, naming(p1))
      assertEquals(run.ready.game.current.result, Some(GameResult(p1, kind)),
        oathkeeper)
      assertEquals(Imperials.chancellor(run.ready), Some(p1)))

  test("naming the current winner keeps the result, journals nothing and " +
      "writes nothing (R38, R41)"):
    // The Chancellor in a Chancellor's win is also the plainest answer.
    val kept = through(ended(stable), TriggeredProcedureRef.ChronicleSun,
      naming(p1))
    assertEquals(kept.ready.game.current.result, Some(stable))
    assert(!RecordedOps.all(kept.events).exists(_.isInstanceOf[WinnerNamed]))
    assertEquals(sunLines(kept).filter(_.contains(" named ")), Vector.empty)
    val successor = GameResult(p2, VictoryKind.Successor)
    val same = through(ended(successor), TriggeredProcedureRef.ChronicleSun,
      naming(p2))
    assertEquals(same.ready.game.current.result, Some(successor))
    assert(!RecordedOps.all(same.events).exists(_.isInstanceOf[WinnerNamed]))

  test("not asked on an Exile's or a Vision's win"):
    Vector(GameResult(p1, VictoryKind.Usurper),
        GameResult(p1, VictoryKind.Visionary)).foreach(result =>
      assertEquals(asked(through(ended(result),
        TriggeredProcedureRef.ChronicleSun)), None, result))

  test("not asked when its holder was an Exile at game end (ruling 22)"):
    val table = ChronicleWalk.chronicleWonByP1.imperial(p1, p2)
      .update(_.updateCurrent(_.copy(result = Some(stable))))
      .legacy(p3, RoyalLineCard)
    assertEquals(asked(through(table, TriggeredProcedureRef.ChronicleSun)),
      None)

  test("a named winner reaches the Crown, Reformer and Score Lineage " +
      "Points (the audit)"):
    // p2 holds Reformer; the Empire rules Ancient City and Broken Peaks,
    // and Village Idiot lies at Ancient City, so Reformer has a move.
    val table = ended(stable).warbandsAt(AncientCitySite, p1, 1)
      .warbandsAt(BrokenPeaksSite, p1, 1)
      .denizen(VillageIdiotCard, at = AncientCitySite)
      .legacy(p2, ReformerCard)
    val run = through(table, TriggeredProcedureRef.ChronicleThrone,
      naming(p2))
    assertEquals(Imperials.chancellor(run.ready), Some(p2))
    assert(run.asked.exists(_.decisionId == Reformer.pickId(p2)))
    val scored = lines(run, TriggeredProcedureRef.ChronicleThrone,
      LineageScoring.noteKeys)
    assert(scored.exists(_.startsWith("p2 scored ")), scored)
    assert(!scored.exists(_.startsWith("p1 scored ")), scored)

  test("a walk resumed at Royal Line's question applies the answer"):
    val table = ended(stable)
    val parked = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(rules,
      table, TriggeredProcedureRef.ChronicleBegin), RoyalLine.decisionId(p3))
    val decide = ChronicleWalk.parked(parked.state).get
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
    val answered = accepted(rules.resolveWalker(replayed, p3,
      RoyalLine.decisionId(p3), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Player(p2))))
    assertEquals(Outcomes.ready(answered.state).game.current.result,
      Some(GameResult(p2, VictoryKind.Successor)))
