package distantlands.catalog.holding

import distantlands.catalog.{PrintedPower, Relic}
import distantlands.model.{Cost, PowerId, RelicId}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object AncientWritCard extends Relic(RelicId("R22"), "Ancient Writ", value = 81, defense = 1):
  val power = PrintedPower(PowerId("relic.ancient-grit"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** Bury this to draw 3 legacy cards, keep 1 " +
      "facedown, and discard the other 2.")
  val powers: Vector[PrintedPower] = Vector(power)

object WhisperingStoneCard extends Relic(RelicId("R34"), "Whispering Stone", value = 13, defense = 2):
  val power = PrintedPower(PowerId("relic.whispering-stone"),
    persistent = false, cost = Cost(secret = 1),
    text = "**ACTION:** Negotiate with any one player in private, even " +
      "if their pawn is not at your site.")
  val powers: Vector[PrintedPower] = Vector(power)
