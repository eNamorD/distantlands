package distantlands.gameplay.powers

import distantlands.catalog.PrintedPower
import distantlands.model.PowerResolution

/** How a modifier or a persistent rule activates, read from its printed
  * power: a persistent power is automatic, a non-persistent one is selected
  * by the player at the start of the major action.
  *
  * Only modifiers and persistent rules use this. The catalog also marks When
  * Played powers (Dazzle) `persistent: false`, but those fire on the play and
  * must stay automatic. Phase powers and battle plans do not activate this
  * way either.
  */
object CatalogResolution:
  /** The resolution of the power its card prints as `printed`. */
  def of(printed: PrintedPower): PowerResolution =
    if printed.persistent then PowerResolution.Automatic
    else PowerResolution.PlayerSelected
