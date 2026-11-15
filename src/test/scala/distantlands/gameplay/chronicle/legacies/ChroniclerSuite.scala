package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.powers.banner.TheNeedleCard
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite}
import distantlands.gameplay.chronicle.{ChronicleWalk, LineageScoring}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.economy.TheOldOakCard
import distantlands.gameplay.powers.poweruse.ForestCouncilCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Chronicler (L02): "After Resolve Legacies, you may declare that all
  * players score their legacies now. Each legacy may only be scored once
  * per game." A step after Sun step 5 (rulings 7 and 8). p1, the
  * Chancellor, won; p2 is a Citizen; the Empire rules Ancient City and its
  * two Beast denizens. */
class ChroniclerSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def empire: Table = ChronicleWalk.chronicleWonByP1.imperial(p1, p2)
    .warbandsAt(AncientCitySite, p1, 1)
    .denizen(ForestCouncilCard, AncientCitySite)
    .denizen(TheOldOakCard, AncientCitySite)

  private val declaring: PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Chronicler.decisionId(p2) =>
      DecisionAnswer.ChooseOneAnswer(Chronicler.declare)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = declaring)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, procedure, answers)

  private def sunLines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.sunNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleSun), keys,
      run.events).filter(said => keys.exists(_.name == said.key)).map(_.text)

  private def scoreOf(ready: ReadyGame, player: PlayerId): Int =
    ready.game.campaign.era.score(ready.game.current.player(player).get.lineage)

  /** p2 holds Chronicler; p1 holds The Needle (Beast). */
  private def chronicled: Table =
    empire.legacy(p2, ChroniclerCard).legacy(p1, TheNeedleCard)

  test("Chronicler is implemented"):
    assert(PowerImplementationStatus.implemented(ChroniclerCard.power.id))

  test("the holder is asked, Decline first, and declining scores nothing"):
    val run = through(chronicled, TriggeredProcedureRef.ChronicleSun,
      PartialFunction.empty)
    val decide = run.asked.find(_.decisionId == Chronicler.decisionId(p2)).get
    assertEquals(decide.owner, p2)
    assertEquals(decide.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref), Vector(Chronicler.decline, Chronicler.declare))
    assertEquals(run.ready.game.current.scoredLegacies, Set.empty)
    assertEquals(sunLines(run), Vector.empty)

  test("declaring scores each seated player's legacy bonus now, with no " +
      "base points, and records every legacy held (rulings 7 and 8, R2)"):
    val run = through(chronicled, TriggeredProcedureRef.ChronicleSun)
    // p1's Needle scores the Empire's two Beast denizens; p2's Chronicler
    // (Arcane) scores nothing and writes no line.
    assertEquals(scoreOf(run.ready, p1), 2)
    assertEquals(scoreOf(run.ready, p2), 0)
    assertEquals(run.ready.game.current.scoredLegacies,
      Set(TheNeedleCard.id, ChroniclerCard.id))
    assertEquals(sunLines(run), Vector(
      s"p2 declared ${ChroniclerCard.id.value}",
      "p1 scored 2 lineage points (2 from Beast)"))

  test("Score Lineage Points then scores only the base for the winner's " +
      "legacies Chronicler scored"):
    val run = through(chronicled, TriggeredProcedureRef.ChronicleThrone)
    assertEquals(scoreOf(run.ready, p1), 2 + LineageScoring.basePoints)

  test("a Chronicler gone dormant in step 3 is not asked (ruling 1)"):
    // p3, an Exile, rules no site, so misses Chronicler's goal.
    val run = through(empire.legacy(p3, ChroniclerCard)
      .legacy(p1, TheNeedleCard), TriggeredProcedureRef.ChronicleSun)
    assert(!run.asked.exists(_.decisionId.startsWith(
      "chronicle.sun.legacy.chronicler.")))

  test("a dormant legacy is neither scored nor recorded (R1, R2)"):
    // p3, an Exile, misses Seer's goal, as nobody holds the Darkest Secret,
    // so Seer goes dormant in step 3, before Chronicler.
    val run = through(chronicled.legacy(p3, SeerCard),
      TriggeredProcedureRef.ChronicleSun)
    val lineage = run.ready.game.current.player(p3).get.lineage
    assertEquals(run.ready.game.campaign.lineages(lineage).legacies,
      Vector(LegacyState(SeerCard.id, active = false)))
    assert(!run.ready.game.current.scoredLegacies.contains(SeerCard.id))

  test("an Exile winner's legacy dormant when Chronicler resolves scores " +
      "later, reactivated at the Crown (R1, R2)"):
    // p1, the Exile winner, rules Ancient City and its two Beast denizens;
    // p2 rules two sites, so holds Chronicler active and p1 misses The
    // Needle's goal: it goes dormant in step 3 and returns at the Crown.
    val table = ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 1)
      .warbandsAt(BrokenPeaksSite, p2, 1).warbandsAt(DeepWoodsSite, p2, 1)
      .denizen(ForestCouncilCard, AncientCitySite)
      .denizen(TheOldOakCard, AncientCitySite)
      .legacy(p2, ChroniclerCard).legacy(p1, TheNeedleCard)
    val sun = through(table, TriggeredProcedureRef.ChronicleSun)
    assert(sun.asked.exists(_.decisionId == Chronicler.decisionId(p2)))
    assert(!sun.ready.game.current.scoredLegacies.contains(TheNeedleCard.id))
    assertEquals(scoreOf(sun.ready, p1), 0)
    val throne = through(table, TriggeredProcedureRef.ChronicleThrone)
    assertEquals(scoreOf(throne.ready, p1), LineageScoring.basePoints + 2)

  test("a walk resumed at Chronicler's question applies the answer once"):
    // The declaration journals one LegaciesScored, replayed into `ready`.
    val run = through(chronicled.legacy(p2, SeerCard),
      TriggeredProcedureRef.ChronicleSun)
    assertEquals(RecordedOps.all(run.events).collect {
      case scored: LegaciesScored => scored.legacies.toSet },
      Vector(Set(TheNeedleCard.id, ChroniclerCard.id, SeerCard.id)))

  test("an Exile winner's scorepad entry is the Chancellor's though " +
      "Chronicler opened the row in the Sun (Chronicle ruling 19, R3)"):
    val table = ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p2, 1)
      .denizen(ForestCouncilCard, AncientCitySite)
      .legacy(p2, ChroniclerCard).legacy(p2, TheNeedleCard)
    val run = through(table, TriggeredProcedureRef.ChronicleThrone)
    val lineage = run.ready.game.current.player(p1).get.lineage
    assertEquals(run.ready.game.campaign.era.games.last.entries
      .find(_.lineage == lineage).map(_.endingRole), Some(Role.Chancellor))
