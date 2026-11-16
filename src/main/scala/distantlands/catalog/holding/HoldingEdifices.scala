package distantlands.catalog.holding

import distantlands.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import distantlands.model.{Cost, EdificeId, PowerId, Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object StoneBathsCard extends Edifice(EdificeId("E04"), Suit.Discord):
  object intact extends EdificeFace("Stone Baths") with Locked:
    val power = PrintedPower(PowerId("edifice.e04.intact"),
      persistent = true, cost = Cost.free,
      text = "CHRONICLE (SUN): Players with a pawn at this site do not " +
        "discard their dormant legacies.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Ruined Baths"):
    val power = PrintedPower(PowerId("edifice.e04.ruined"),
      persistent = true, cost = Cost.free,
      text = "CHRONICLE (SUN): Exiles at this site don't need to meet the " +
        "goals of their active legacies to keep them active.")
    val powers: Vector[PrintedPower] = Vector(power)
