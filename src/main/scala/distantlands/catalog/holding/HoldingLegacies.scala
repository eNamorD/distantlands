package distantlands.catalog.holding

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.model.{Cost, LegacyId, PowerId,
  Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object ConspiratorCard extends Legacy(LegacyId("L26"), "Conspirator",
    LegacyGoal.HighestRelic, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.conspirator"),
    persistent = false, cost = Cost.free,
    text = "**WAKE:** On your first turn, secretly write down which " +
      "round the game will end on and who will win. If you are " +
      "correct, you win instead. _This even beats a Successor win._")
  val powers: Vector[PrintedPower] = Vector(power)
