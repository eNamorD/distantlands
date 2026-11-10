package distantlands.gameplay

import distantlands.gameplay.powers.search.CrackedHornCard
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.action.DowsingSticksCard
import distantlands.gameplay.powers.banner.YewStaffCard
import distantlands.gameplay.powers.economy.CupOfPlentyCard
import distantlands.model._
import distantlands.model.OathEvent.WarExhaustionResolved
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** The Empire's endings as rules (CR p. 19; Empire phase design, "Slice
  * 2"). p1 is the Chancellor and p2 a Citizen; p3 is an Exile unless a
  * test makes them a Citizen. */
class EmpireEndingsSuite extends munit.FunSuite:
  /** The Empire's table at `round`, with `holder` holding the title on
    * `side` under `goal`. */
  private def empire(round: Int = 5, holder: Option[PlayerId] = Some(p1),
      side: TitleSide = TitleSide.Oathkeeper,
      goal: OathkeeperGoal = OathkeeperGoal.ThePeople,
      citizens: Vector[PlayerId] = Vector(p2)): Table =
    Table.start.imperial(p1, citizens*).oathkeeper(holder, side)
      .oathkeeperGoal(goal)
      // No Table step sets the round.
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = round, usurperLimited = false))))

  private val chancellorWin = WarExhaustionResolved(p1,
    VictoryKind.ChancellorExhaustion, Vector.empty, Vector.empty)

  test("the end die needs a 6, a 5 and a 3 at rounds 5, 6 and 7"):
    assertEquals((1 to 8).map(EmpireEndings.threshold).toVector,
      Vector(None, None, None, None, Some(6), Some(5), Some(3), None))

  test("the die is rolled only with an Empire, an Imperial Oathkeeper and " +
      "a round from 5 to 7"):
    assert(EmpireEndings.rollsEndDie(empire().ready))
    assert(EmpireEndings.rollsEndDie(empire(holder = Some(p2)).ready))
    assert(!EmpireEndings.rollsEndDie(empire(holder = Some(p3)).ready))
    assert(!EmpireEndings.rollsEndDie(empire(holder = None).ready))
    assert(!EmpireEndings.rollsEndDie(empire(round = 4).ready))
    assert(!EmpireEndings.rollsEndDie(empire(round = 8).ready))
    assert(!EmpireEndings.rollsEndDie(Table.start.oathkeeper(Some(p1))
      .update(_.updateCurrent(c => c.copy(tracks =
        c.tracks.copy(round = 5)))).ready))

  test("each face wins from its round's threshold up"):
    for
      (round, least) <- Vector(5 -> 6, 6 -> 5, 7 -> 3)
      face <- 1 to 6
    do
      val ready = empire(round = round).ready
      assertEquals(EmpireEndings.stableRegime(ready, face),
        Option.when(face >= least)(GameResult(p1, VictoryKind.StableRegime)),
        clue((round, face)))

  test("a Successor takes the Stable Regime win"):
    val ready = empire().darkestSecret(Some(p2), secrets = 1).ready
    assertEquals(EmpireEndings.stableRegime(ready, 6),
      Some(GameResult(p2, VictoryKind.Successor)))

  test("the Successor goal follows the Oathkeeper goal"):
    val people = empire(goal = OathkeeperGoal.ThePeople)
    assertEquals(EmpireEndings.successor(
      people.darkestSecret(Some(p2), secrets = 1).ready), Some(p2))
    assertEquals(EmpireEndings.successor(
      people.peoplesFavor(Some(p2), favor = 1).ready), None)
    val protection = empire(goal = OathkeeperGoal.Protection)
    assertEquals(EmpireEndings.successor(
      protection.peoplesFavor(Some(p2), favor = 1).ready), Some(p2))
    val devotion = empire(goal = OathkeeperGoal.Devotion)
    assertEquals(EmpireEndings.successor(
      devotion.relic(p2, TheGrandScepterCard.id).ready), Some(p2))
    assertEquals(EmpireEndings.successor(
      devotion.darkestSecret(Some(p2), secrets = 1).ready), None)
    assertEquals(EmpireEndings.successor(
      devotion.relic(p1, TheGrandScepterCard.id).ready), None)

  test("only a Citizen can be the Successor"):
    assertEquals(EmpireEndings.successor(empire()
      .darkestSecret(Some(p3), secrets = 1).ready), None)
    assertEquals(EmpireEndings.successor(empire()
      .darkestSecret(Some(p1), secrets = 1).ready), None)

  test("Supremacy's Successor holds more relics than each Imperial, the " +
      "Grand Scepter counted"):
    // R17: the Chancellor's Grand Scepter is one of their relics.
    val supremacy = empire(goal = OathkeeperGoal.Supremacy,
      citizens = Vector(p2, p3)).relic(p1, TheGrandScepterCard.id)
    val tied = supremacy.relic(p2, YewStaffCard)
    assertEquals(EmpireEndings.successor(tied.ready), None)
    val ahead = tied.relic(p2, DowsingSticksCard)
    assertEquals(EmpireEndings.successor(ahead.ready), Some(p2))
    val matched = ahead.relic(p3, CrackedHornCard).relic(p3, CupOfPlentyCard)
    assertEquals(EmpireEndings.successor(matched.ready), None)

  test("War Exhaustion: an Imperial Oathkeeper gives the Chancellor the " +
      "win"):
    val ready = empire(round = 8, holder = Some(p2)).ready
    assertEquals(EmpireEndings.warExhaustion(ready), Some(
      WarExhaustionResolved(p1, VictoryKind.StableRegime, Vector.empty,
        Vector.empty)))

  test("War Exhaustion: an Exile Usurper wins next"):
    val ready = empire(round = 8, holder = Some(p3),
      side = TitleSide.Usurper).ready
    assertEquals(EmpireEndings.warExhaustion(ready), Some(
      WarExhaustionResolved(p3, VictoryKind.Usurper, Vector.empty, Vector.empty)))

  test("War Exhaustion: an Exile meeting their Vision wins next"):
    val ready = empire(round = 8, holder = None)
      .revealedVision(p3, VisionRules.Faith).visionsDrawn(3)
      .darkestSecret(Some(p3), secrets = 1).ready
    assertEquals(EmpireEndings.warExhaustion(ready), Some(
      WarExhaustionResolved(p3, VictoryKind.Visionary,
        Vector(VisionRules.Faith), Vector.empty)))

  test("War Exhaustion: tied Visions go to Conquest, then Rebellion, " +
      "Sanctuary and Faith"):
    // p1 is the Chancellor with no Citizens; p2 and p3 are Exiles who both
    // meet their Visions. Each seat holds the earlier Vision in turn, so
    // the printed order decides, not the seat.
    val order = Vector(VisionRules.Conquest, VisionRules.Rebellion,
      VisionRules.Sanctuary, VisionRules.Faith)
    for
      (earlier, i) <- order.zipWithIndex
      later <- order.drop(i + 1)
      (first, second) <- Vector(p2 -> p3, p3 -> p2)
    do
      val base = empire(round = 8, holder = None, citizens = Vector.empty)
      val table = base.revealedVisionMet(first, earlier)
        .revealedVisionMet(second, later).visionsDrawn(3)
      assertEquals(EmpireEndings.warExhaustion(table.ready), Some(
        WarExhaustionResolved(first, VictoryKind.Visionary, Vector(earlier),
          Vector.empty)), clue((earlier, later, first)))

  test("Supremacy has no Successor while the Chancellor holds the most " +
      "relics"):
    val ready = empire(goal = OathkeeperGoal.Supremacy)
      .relic(p1, TheGrandScepterCard.id).relic(p1, YewStaffCard)
      .relic(p2, DowsingSticksCard).ready
    assertEquals(EmpireEndings.successor(ready), None)

  test("War Exhaustion: a Citizen's Vision wins nothing"):
    // An Imperial board has no Vision slot, so this table cannot arise in
    // play; War Exhaustion still reads only Exiles' Visions. p2 meets Faith
    // with the Darkest Secret, which is no Successor goal under Supremacy.
    val ready = empire(round = 8, holder = None,
      goal = OathkeeperGoal.Supremacy)
      .revealedVisionMet(p2, VisionRules.Faith).visionsDrawn(3).ready
    assertEquals(VisionVictoryEligibility.qualifying(p2, ready),
      Vector[VisionId](VisionRules.Faith))
    assertEquals(EmpireEndings.warExhaustion(ready), Some(chancellorWin))

  test("War Exhaustion: otherwise the Chancellor wins, or the Successor"):
    assertEquals(EmpireEndings.warExhaustion(
      empire(round = 8, holder = Some(p3)).ready), Some(chancellorWin))
    assertEquals(EmpireEndings.warExhaustion(empire(round = 8, holder = None)
      .darkestSecret(Some(p2), secrets = 1).ready), Some(
      WarExhaustionResolved(p2, VictoryKind.Successor, Vector.empty, Vector.empty)))

  test("a game with no Chancellor has no Empire ending"):
    val ready = Table.start.oathkeeper(Some(p1)).ready
    assert(!EmpireEndings.hasEmpire(ready))
    assertEquals(EmpireEndings.chancellorWins(ready,
      VictoryKind.StableRegime), None)
