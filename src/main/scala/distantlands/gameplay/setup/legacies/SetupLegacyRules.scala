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

  /** Who chooses the end of `explorer`'s successful Explore: World
    * Crafter's holder, who may answer on another player's turn, else the
    * explorer (ruling 11). */
  def exploreEndChooser(state: ReadyGame, explorer: PlayerId): PlayerId =
    ChronicleLegacyRules.holder(state, WorldCrafterCard.id)
      .getOrElse(explorer)

  /** Who may divide the banks' favor evenly: Populist's holder (ruling
    * 12). */
  def populist(state: ReadyGame): Option[PlayerId] =
    ChronicleLegacyRules.holder(state, PopulistCard.id)
