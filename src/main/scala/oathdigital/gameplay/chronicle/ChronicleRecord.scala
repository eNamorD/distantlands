package oathdigital.gameplay.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.setup.ChronicleAccounting
import oathdigital.model._

/** The Chronicle the Chronicle Phase journals (Chronicle steps design,
  * "Exit and extraction"). Roles, era scores, the Oathkeeper goal and
  * starting advisers come from the live campaign; sites, decks, the
  * Dispossessed and the Reliquary from the table (`ChronicleExtraction`). */
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
    // A seated lineage the baseline lists keeps its baseline legacies: game
    // start seats every lineage fresh, so the live state has none of its
    // own yet. Its starting advisers are the ones this Chronicle kept.
    val kept = campaign.baseline.lineages.map(lineage =>
      campaign.lineages.get(lineage.id)
        .map(live => lineage.copy(role = live.role,
          previousPlayer = seatedBy.get(lineage.id),
          startingAdvisers = live.startingAdvisers))
        .getOrElse(lineage.copy(role =
          campaign.boxRoles.getOrElse(lineage.id, lineage.role))))
    val known = kept.map(_.id).toSet
    val added = campaign.lineages.values.toVector
      .filterNot(lineage => known(lineage.id)).sortBy(_.id.value)
      .map(live => LineageState(live.id, seatedBy.get(live.id), live.role,
        Vector.empty, live.startingAdvisers))
    campaign.baseline.copy(era = campaign.era,
      oathkeeperGoal = campaign.oathkeeperGoal, lineages = kept ++ added)
