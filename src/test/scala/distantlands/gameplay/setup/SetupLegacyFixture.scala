package distantlands.gameplay.setup

import distantlands.model._

/** A later game's Chronicle in which a lineage holds legacies, for the
  * legacies' SETUP powers. */
object SetupLegacyFixture:
  /** Who played a holding lineage in the previous game: a player the
    * fixture does not seat, since only whether it played matters (R39). */
  val previousPlayer: PlayerId = PlayerId("previous")

  /** `chronicle` with `lineage` stored as `role`, holding `legacies`
    * (id, active), added when the Chronicle does not list it. The lineage
    * was in the previous game, as a holder of legacies has been. */
  def holding(chronicle: Chronicle, lineage: LineageId, role: Role,
      legacies: (LegacyId, Boolean)*): Chronicle =
    val held = legacies.toVector.map((id, active) => LegacyState(id, active))
    val record = chronicle.lineages.find(_.id == lineage).fold(
      LineageState(lineage, Some(previousPlayer), role, held, Vector.empty))(
      stored => stored.copy(role = role, legacies = held, previousPlayer =
        stored.previousPlayer.orElse(Some(previousPlayer))))
    chronicle.copy(lineages =
      chronicle.lineages.filterNot(_.id == lineage) :+ record)

  /** `chronicle` with `lineage` recorded as having sat out the previous
    * game (Legacy Setup powers R39). */
  def satOut(chronicle: Chronicle, lineage: LineageId): Chronicle =
    chronicle.copy(lineages = chronicle.lineages.map(stored =>
      if stored.id == lineage then stored.copy(previousPlayer = None)
      else stored))
