package distantlands.gameplay.setup

import distantlands.model._

/** A later game's Chronicle in which a lineage holds legacies, for the
  * legacies' SETUP powers. */
object SetupLegacyFixture:
  /** `chronicle` with `lineage` stored as `role`, holding `legacies`
    * (id, active), added when the Chronicle does not list it. */
  def holding(chronicle: Chronicle, lineage: LineageId, role: Role,
      legacies: (LegacyId, Boolean)*): Chronicle =
    val held = legacies.toVector.map((id, active) => LegacyState(id, active))
    val record = chronicle.lineages.find(_.id == lineage).fold(
      LineageState(lineage, None, role, held, Vector.empty))(
      _.copy(role = role, legacies = held))
    chronicle.copy(lineages =
      chronicle.lineages.filterNot(_.id == lineage) :+ record)
