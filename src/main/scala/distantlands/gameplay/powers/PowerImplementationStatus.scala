package oathdigital.gameplay.powers

import oathdigital.gameplay.cards.HomelandsAndPlains
import oathdigital.gameplay.operations.OperationRestrictions
import oathdigital.model.PowerId

/** Whether a power genuinely runs: it is wired into
  * `WalkerPowerCatalog` or `PhasePowerCatalog`, an
  * operation restriction in `OperationRestrictions.printedRules` enforces
  * it, or it is a site handler a game rule runs (the Homelands) or that
  * prints no power (the Plains), listed in `HomelandsAndPlains`. No
  * catalog carries an "implemented" flag; being wired in is what
  * implemented means. The presentation layer's unimplemented-card marker,
  * `ImplementedCardCatalog` and the first-game generator all read this one
  * predicate.
  */
object PowerImplementationStatus:
  val implemented: PowerId => Boolean =
    val covered = WalkerPowerCatalog.contributing.map(_.id).toSet ++
      PhasePowerCatalog.default.powers.map(_.id).toSet ++
      OperationRestrictions.printedRules.map(_.id).toSet ++
      HomelandsAndPlains.handlers
    covered.contains
