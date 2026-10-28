package oathdigital.gameplay.chronicle

import oathdigital.model._
import oathdigital.testkit.{CatalogNames, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

/** What the journaled Chronicle carries after Sun and Throne: the live
  * roles, era scores and Oathkeeper goal over the baseline. */
class ChronicleRecordSuite extends munit.FunSuite:
  private val boxed = LineageId("lineage:boxed")
  private val legacy = LegacyState(LegacyId("legacy:any"), active = true)

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.players.find(_.player == player).get.lineage

  test("the record keeps the live era scores and Oathkeeper goal"):
    val ready = Table.start.oathkeeperGoal(OathkeeperGoal.Devotion)
      .update(state => state.updateCampaign(campaign => campaign.copy(
        era = campaign.era.copy(lineageScores =
          Map(lineageOf(state, p1) -> 4))))).ready
    val record = ChronicleRecord.campaignFields(ready)
    assertEquals(record.oathkeeperGoal, OathkeeperGoal.Devotion)
    assertEquals(record.era, ready.game.campaign.era)

  test("seated lineages take their live role and join the record when the " +
      "baseline lacks them"):
    val ready = Table.start.update(state => state.updateCampaign(campaign =>
      campaign.copy(lineages = campaign.lineages.updatedWith(
        lineageOf(state, p1))(_.map(_.copy(role = Role.Chancellor)))))).ready
    val record = ChronicleRecord.campaignFields(ready)
    val expected = Vector(p1, p2, p3).map(player =>
      LineageState(lineageOf(ready, player), Some(player),
        if player == p1 then Role.Chancellor else Role.Exile,
        Vector.empty, Vector.empty)).sortBy(_.id.value)
    assertEquals(record.lineages, ready.game.campaign.baseline.lineages ++
      expected)

  test("a lineage in the box takes its role from this Chronicle and keeps " +
      "its legacies"):
    val ready = Table.start.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(LineageState(boxed,
        None, Role.Citizen, Vector(legacy), Vector.empty))),
      boxRoles = Map(boxed -> Role.Exile)))).ready
    assertEquals(ChronicleRecord.campaignFields(ready).lineages.head,
      LineageState(boxed, None, Role.Exile, Vector(legacy), Vector.empty))

  test("a seated lineage the baseline lists keeps its legacies"):
    val start = Table.start.ready
    val lineage = lineageOf(start, p2)
    val ready = Table.start.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(LineageState(
        lineage, None, Role.Citizen, Vector(legacy), Vector.empty)))))).ready
    assertEquals(ChronicleRecord.campaignFields(ready).lineages.head,
      LineageState(lineage, None, Role.Exile, Vector(legacy), Vector.empty))

  test("every other field is the baseline's"):
    val ready = Table.start.denizen("Fae Merchant", at = "Ancient City")
      .relic(p1, CatalogNames.relic("Brass Horse")).ready
    val baseline = ready.game.campaign.baseline
    val record = ChronicleRecord.campaignFields(ready)
    assertEquals(record.copy(era = baseline.era,
      oathkeeperGoal = baseline.oathkeeperGoal,
      lineages = baseline.lineages), baseline)
