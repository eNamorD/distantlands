package distantlands.application

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.chronicle.legacies.{RoyalLine, RoyalLineCard}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** The result display follows Royal Line's named winner (Legacy Chronicle
  * powers design, ruling 23). */
class RoyalLineProjectionSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)
  private val projector = GameProjector(catalog)

  test("every viewer's result shows the named winner as the Successor"):
    val table = ChronicleWalk.chronicleWonByP1.imperial(p1, p2, p3)
      .update(_.updateCurrent(_.copy(result =
        Some(GameResult(p1, VictoryKind.StableRegime)))))
      .legacy(p3, RoyalLineCard)
    val parked = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(rules,
      table, TriggeredProcedureRef.ChronicleBegin), RoyalLine.decisionId(p3))
    val named = accepted(rules.resolveWalker(parked.state, p3,
      RoyalLine.decisionId(p3), DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Player(p2))))
    val loaded = LoadedGame(named.state, 1L)
    (Vector(p1, p2, p3).map(projector.project("royal", loaded, _)) :+
        projector.projectPublic("royal", loaded)).foreach { view =>
      val oathkeeper = view.oathkeeper.get
      assertEquals((oathkeeper.winnerPlayerId, oathkeeper.winnerVictoryKind),
        (Some(p2.value), Some(VictoryKind.Successor.key)))
    }
