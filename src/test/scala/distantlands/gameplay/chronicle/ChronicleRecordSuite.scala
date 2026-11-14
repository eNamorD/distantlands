package distantlands.gameplay.chronicle

import distantlands.catalog.holding.ArbiterCard
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.action.{BrassHorseCard, FaeMerchantCard}
import distantlands.gameplay.setup.{FirstGameSetupFixture, GameStartRules}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** What the journaled Chronicle carries from the campaign: the live roles,
  * era scores, Oathkeeper goal, foundation faces and starting advisers over
  * the baseline.
  * `ChronicleExtraction` supplies the table fields. */
class ChronicleRecordSuite extends munit.FunSuite:
  private val boxed = LineageId("lineage:boxed")
  private val legacy = LegacyState(LegacyId("legacy:any"), active = true)

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.player(player).get.lineage

  test("the record keeps the live era scores and Oathkeeper goal"):
    val ready = Table.start.oathkeeperGoal(OathkeeperGoal.Devotion)
      .update(state => state.updateCampaign(campaign => campaign.copy(
        era = campaign.era.copy(games = Vector(EraGame(Vector(
          EraEntry(lineageOf(state, p1), 4, Role.Exile)))))))).ready
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

  test("a seated lineage the baseline lists takes its live state"):
    val start = Table.start.ready
    val lineage = lineageOf(start, p2)
    val ready = Table.start.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(LineageState(
        lineage, None, Role.Citizen, Vector.empty, Vector.empty))),
      lineages = campaign.lineages.updatedWith(lineage)(
        _.map(_.copy(legacies = Vector(legacy))))))).ready
    assertEquals(ChronicleRecord.campaignFields(ready).lineages.head,
      LineageState(lineage, Some(p2), Role.Exile, Vector(legacy),
        Vector.empty))

  test("every other field is the baseline's"):
    val ready = Table.start.denizen(FaeMerchantCard, at = AncientCitySite)
      .relic(p1, BrassHorseCard.id).ready
    val baseline = ready.game.campaign.baseline
    val record = ChronicleRecord.campaignFields(ready)
    assertEquals(record.copy(era = baseline.era,
      oathkeeperGoal = baseline.oathkeeperGoal,
      foundations = baseline.foundations,
      lineages = baseline.lineages), baseline)

  test("the record keeps the live foundation faces, a changed one and its " +
      "sources included"):
    val sourced = FoundationState(FoundationFace.Altered, Set(legacy.id))
    val ready = Table.start
      .foundation(FoundationNumber.III, FoundationFace.Altered)
      .update(_.updateCampaign(campaign => campaign.copy(foundations =
        campaign.foundations.updated(FoundationNumber.VI, sourced)))).ready
    // The baseline lists no foundation: the record's faces are the game's.
    assertEquals(ready.game.campaign.baseline.foundations, Map.empty)
    val normal = FoundationState(FoundationFace.Normal, Set.empty)
    assertEquals(ChronicleRecord.campaignFields(ready).foundations,
      FoundationNumber.all.map(_ -> normal).toMap
        .updated(FoundationNumber.III,
          FoundationState(FoundationFace.Altered, Set.empty))
        .updated(FoundationNumber.VI, sourced))

  test("a seated lineage takes the starting advisers this Chronicle kept"):
    val start = Table.start.ready
    val lineage = lineageOf(start, p2)
    // Two denizens the table does not hold, so the live state stays whole.
    val Vector(baselineAdviser, keptAdviser) = catalog.denizens
      .map(card => DenizenId(card.id.value))
      .filterNot(card => start.game.cardIndex.toOption.get.ids(card)).take(2)
    val kept = DenizenState(keptAdviser, Orientation.FaceDown, Tokens.empty)
    val ready = Table.start.update(_.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(LineageState(
        lineage, None, Role.Exile, Vector.empty, Vector(DenizenState(
          baselineAdviser, Orientation.FaceDown, Tokens.empty))))),
      lineages = campaign.lineages.updatedWith(lineage)(
        _.map(_.copy(startingAdvisers = Vector(kept))))))).ready
    assertEquals(ChronicleRecord.campaignFields(ready).lineages.head
      .startingAdvisers, Vector(kept))

  test("a game started from a lineage's legacies and starting relic records " +
      "the same legacies, and the relic no longer as a starting relic"):
    val (chronicle, orders) =
      (FirstGameSetupFixture.chronicle, FirstGameSetupFixture.orders)
    val seated = orders.participants.head.lineageId
    val relic = chronicle.relicDeck.head
    val legacies = Vector(LegacyState(SeerCard.id, active = true),
      LegacyState(ArbiterCard.id, active = false))
    val keeping = chronicle.copy(relicDeck = chronicle.relicDeck.tail,
      lineages = Vector(LineageState(seated, None, Role.Exile, legacies,
        Vector.empty, startingRelics = Vector(relic))))
    val ready = GameStartRules.evolve(catalog, keeping, orders.copy(
      relicOrder = orders.relicOrder.filterNot(_ == relic),
      legacyOrder = orders.legacyOrder.filterNot(
        Set(SeerCard.id, ArbiterCard.id)))).toOption.get
    val recorded = ChronicleRecord.campaignFields(ready).lineages
      .find(_.id == seated)
    // Quiet Ambitions' draw follows them; Setup has not run.
    assertEquals(recorded.map(_.legacies.take(legacies.size)), Some(legacies))
    assertEquals(recorded.map(_.startingRelics), Some(Vector.empty))

  test("a lineage the baseline lacks is recorded with its legacies (R1)"):
    val ready = Table.start.legacy(p1, SeerCard, active = false).ready
    val record = ChronicleRecord.campaignFields(ready)
    assertEquals(record.lineages.find(_.id == lineageOf(ready, p1))
      .map(_.legacies), Some(Vector(LegacyState(SeerCard.id, active = false))))
