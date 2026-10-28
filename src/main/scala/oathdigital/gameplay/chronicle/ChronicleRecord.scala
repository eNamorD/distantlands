package oathdigital.gameplay.chronicle

import oathdigital.model._

/** The Chronicle as the tasks so far leave it (Chronicle steps design,
  * "Exit and extraction"). Roles, era scores and the Oathkeeper goal come
  * from the live campaign. Sites, decks, the Reliquary and the Dispossessed
  * stay the baseline's until the Stars slice extracts them from the table:
  * moving a card in play does not yet change the record. */
object ChronicleRecord:
  def campaignFields(ready: ReadyGame): Chronicle =
    val campaign = ready.game.campaign
    val seatedBy = ready.game.current.players
      .map(player => player.lineage -> player.player).toMap
    // A seated lineage the baseline lists keeps its baseline legacies and
    // advisers: game start seats every lineage fresh, so the live state has
    // none of its own yet.
    val kept = campaign.baseline.lineages.map(lineage =>
      campaign.lineages.get(lineage.id)
        .map(live => lineage.copy(role = live.role))
        .getOrElse(lineage.copy(role =
          campaign.boxRoles.getOrElse(lineage.id, lineage.role))))
    val known = kept.map(_.id).toSet
    val added = campaign.lineages.values.toVector
      .filterNot(lineage => known(lineage.id)).sortBy(_.id.value)
      .map(live => LineageState(live.id, seatedBy.get(live.id), live.role,
        Vector.empty, Vector.empty))
    campaign.baseline.copy(era = campaign.era,
      oathkeeperGoal = campaign.oathkeeperGoal, lineages = kept ++ added)
