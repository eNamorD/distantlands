package distantlands.application

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powers.PowerImplementationStatus
import distantlands.model.{PowerId, Suit}

/** Production's implemented predicate with every Discord denizen's power
  * stood in as unimplemented. Every printed denizen power runs, so a test
  * of how a first game orders unimplemented denizens needs some that do
  * not. A whole suit is needed: the generator puts every implemented
  * denizen of a suit in the world deck before any unimplemented one, so a
  * single stand-in would never be dealt. */
final class StandInUnimplemented(catalog: ExecutableCatalog):
  val powers: Set[PowerId] = catalog.denizens.filter(_.suit == Suit.Discord)
    .flatMap(_.powers.map(_.id)).toSet
  val implemented: PowerId => Boolean = id =>
    !powers(id) && PowerImplementationStatus.implemented(id)
