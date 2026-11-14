package distantlands.gameplay.setup

import distantlands.model._

/** The legacies' SETUP powers that change what game start builds (Legacy
  * Setup powers design, slice 1). Game start asks no question, so these
  * read the Chronicle's stored lineages and the seats, never a walk. A
  * later walker step that needs the same answer asks again from the
  * Chronicle, which keeps its shape fixed on resume. */
object StartingLegacies:
  val imperialRoles: Set[Role] = Role.values.filter(_.isImperial).toSet
  val exileRoles: Set[Role] = Set(Role.Exile)

  /** The seated lineage whose stored record holds `legacy` active, if its
    * stored role is one of `roles`. A lineage the Chronicle does not list
    * holds nothing (R1). */
  def holder(chronicle: Chronicle, seated: Vector[LineageId],
      legacy: LegacyId, roles: Set[Role] = Role.values.toSet)
      : Option[LineageId] =
    chronicle.lineages.find(lineage => seated.contains(lineage.id) &&
      roles(lineage.role) &&
      lineage.legacies.contains(LegacyState(legacy, active = true)))
      .map(_.id)
