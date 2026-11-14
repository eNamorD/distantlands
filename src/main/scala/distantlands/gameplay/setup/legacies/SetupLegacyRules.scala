package distantlands.gameplay.setup.legacies

import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.model._

/** The seam the legacies' SETUP powers share in Setup's walk (Legacy Setup
  * powers design, "Approach"). Each query reads the holder live, through
  * `ChronicleLegacyRules.holder`: nothing in Setup changes who holds a
  * legacy active, so a step built from it keeps its shape on resume. */
object SetupLegacyRules:
  /** Who fills Imperial Maps' slots in the Chancellor's place: World
    * Crafter's holder (ruling 10). */
  def mapFiller(state: ReadyGame): Option[PlayerId] =
    ChronicleLegacyRules.holder(state, WorldCrafterCard.id)
