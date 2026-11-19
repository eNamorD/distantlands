package distantlands.protocol.projection

import distantlands.protocol.ChronicleScorepadEntry

/** The Chronicle as the table sees it mid-game (Chronicle Details design,
  * slice 1). */
final case class ChronicleDetailsProjection(
    eraTarget: Int,
    /** A column per game played, in order, as ChronicleSummary keeps it. */
    scorepad: Vector[Vector[ChronicleScorepadEntry]],
    /** Every lineage in the Chronicle, seated or not, in PlayerColor order. */
    lineages: Vector[LineageDetailsProjection],
    /** The Atlas's Recent site; None once the Atlas holds no site. */
    atlasRecent: Option[SetupSiteProjection])

/** One lineage: its current era total, its Imperial role (`"chancellor"` or
  * `"citizen"`, absent for an Exile), and its legacies in held order as the
  * viewer may see them (Chronicle Details design, ruling D1). */
final case class LineageDetailsProjection(lineage: String, score: Int,
    role: Option[String], legacies: Vector[CardDetailsProjection])
