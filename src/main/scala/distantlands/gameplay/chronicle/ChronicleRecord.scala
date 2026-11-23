package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.setup.ChronicleAccounting
import distantlands.model._

/** The Chronicle the Chronicle Phase journals (Chronicle steps design,
  * "Exit and extraction"). Roles, era scores, the Oathkeeper goal, the
  * foundation faces, legacies and starting advisers come from the live
  * campaign; sites, decks, the Dispossessed and the Reliquary from the
  * table (`ChronicleExtraction`). */
object ChronicleRecord:
  /** The evolved Chronicle, when it passes game start's card accounting
    * and keeps every baseline site and relic. */
  def evolved(catalog: ExecutableCatalog, ready: ReadyGame)
      : Either[OathViolation, Chronicle] =
    for
      record <- ChronicleExtraction.extract(ready, campaignFields(ready))
      _ <- ChronicleAccounting.knownAndUnique(catalog, record)
        .flatMap(_ => ChronicleAccounting.conserves(
          ready.game.campaign.baseline, record))
        .left.map(OathViolation.UnsupportedChronicle(_))
    yield record

  def campaignFields(ready: ReadyGame): Chronicle =
    val campaign = ready.game.campaign
    val seatedBy = ready.game.current.players
      .map(player => player.lineage -> player.player).toMap
    // Every lineage as it stands (`CampaignState.standingLineages`): a
    // seated one is its live state, since game start seated it from its
    // record (`GameStartRules`), and a boxed one takes the role this game
    // gave it in the box. The baseline's lineages keep the baseline's
    // order; the ones it lacks follow by id. A lineage that sat this game
    // out was in no previous game of the next one, so it is recorded with
    // no previous player (Legacy Setup powers R39).
    val standing = campaign.standingLineages
    val listed = campaign.baseline.lineages.map(_.id)
    val byId = standing.map(lineage => lineage.id -> lineage).toMap
    val lineages = (listed.map(byId) ++
      standing.filterNot(lineage => listed.contains(lineage.id)))
      .map(lineage => lineage.copy(previousPlayer = seatedBy.get(lineage.id)))
    campaign.baseline.copy(era = campaign.era,
      oathkeeperGoal = campaign.oathkeeperGoal,
      foundations = campaign.foundations, lineages = lineages)
