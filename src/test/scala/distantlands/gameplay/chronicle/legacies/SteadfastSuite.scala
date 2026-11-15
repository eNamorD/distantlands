package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.gameplay.setup.legacies.MatchmakerCard
import distantlands.gameplay.OathRules
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, ChronicleWalk,
  ResolveLegacies}
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p2, p3}

/** Steadfast (L03): "Your other legacies cannot become dormant—they always
  * stay active." Read at the start of Sun step 3 (ruling 5). Every game here
  * is p1's win; p2 and p3 are Exiles who rule no site. */
class SteadfastSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
  private def won: Table = ChronicleWalk.chronicleWonByP1

  private def sun(table: Table)(using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleSun)

  private def legaciesOf(ready: ReadyGame, player: PlayerId)
      : Vector[LegacyState] =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies

  test("Steadfast is implemented"):
    assert(PowerImplementationStatus.implemented(SteadfastCard.power.id))

  test("an Exile holding Steadfast keeps their other unmet legacies " +
      "active, while Steadfast itself goes dormant (ruling 5)"):
    // p2 rules no site and holds no banner: every goal here is unmet.
    val run = sun(won.legacy(p2, SteadfastCard).legacy(p2, IronHandCard)
      .legacy(p2, SeerCard))
    assertEquals(legaciesOf(run.ready, p2), Vector(
      LegacyState(SteadfastCard.id, active = false),
      LegacyState(IronHandCard.id, active = true),
      LegacyState(SeerCard.id, active = true)))

  test("Steadfast protects only its holder's legacies"):
    val run = sun(won.legacy(p2, SteadfastCard).legacy(p3, IronHandCard))
    assertEquals(legaciesOf(run.ready, p3),
      Vector(LegacyState(IronHandCard.id, active = false)))

  test("Steadfast does not lift the cap: discarding is not going dormant " +
      "(ruling 5)"):
    val asked = sun(won.legacy(p2, SteadfastCard).legacy(p2, IronHandCard)
      .legacy(p2, SeerCard).legacy(p2, MatchmakerCard)
      .legacy(p2, RivalCard)).asked
      .find(_.decisionId == ResolveLegacies.capDecisionId(p2)).get
    // Steadfast went dormant: four active, one over the cap.
    val many = asked.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((many.min, many.max), (1, 1))

  test("a dormant Steadfast protects nothing"):
    val run = sun(won.legacy(p2, SteadfastCard, active = false)
      .legacy(p2, IronHandCard))
    assert(!legaciesOf(run.ready, p2).exists(legacy =>
      legacy.id == IronHandCard.id && legacy.active))

  test("a boxed lineage's active Steadfast does nothing (ruling 1)"):
    val boxed = LineageState(LineageId("lineage:boxed"), None, Role.Exile,
      Vector(LegacyState(SteadfastCard.id, active = true)), Vector.empty)
    val table = won.legacy(p2, IronHandCard).update(_.updateCampaign(
      campaign => campaign.copy(baseline = campaign.baseline.copy(
        lineages = Vector(boxed)))))
    assert(table.ready.game.current.players.forall(player =>
      !ChronicleLegacyRules.keepsOthersActive(table.ready, player.player)))
    assertEquals(legaciesOf(sun(table).ready, p2),
      Vector(LegacyState(IronHandCard.id, active = false)))
