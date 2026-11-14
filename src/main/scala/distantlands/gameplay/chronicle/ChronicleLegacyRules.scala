package distantlands.gameplay.chronicle

import distantlands.model._

/** The seam the legacies' Chronicle powers share (Legacy Chronicle powers
  * design, "In effect is read live"). Every power, and every step a power
  * changes, asks it when its window is reached, never when the walk is
  * built: a `Branch` whose children depended on state the walk itself
  * changes would lose its shape on resume (Legacies slice 3). */
object ChronicleLegacyRules:
  /** The seated player whose lineage holds `legacy` active, if their role
    * is one of `roles`. A boxed lineage's legacies never act and a dormant
    * legacy has no power (ruling 1); a role condition ("if you are an
    * Imperial") is read now, so the Crown and Offer Citizenship change it
    * for every later step (ruling 2). */
  def holder(state: ReadyGame, legacy: LegacyId,
      roles: Set[Role] = Role.values.toSet): Option[PlayerId] =
    state.game.current.players.find(player =>
      state.game.campaign.lineages.get(player.lineage).exists(lineage =>
        roles(lineage.role) &&
          lineage.legacies.contains(LegacyState(legacy, active = true))))
      .map(_.player)
