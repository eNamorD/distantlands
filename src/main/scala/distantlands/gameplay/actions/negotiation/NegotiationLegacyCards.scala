package distantlands.gameplay.actions.negotiation

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.model.{Cost, LegacyId, PowerId, Suit}

// The legacy cards the Negotiation reads at its start, beside the action
// as Council Arbiter's card is: High Priest's WHEN NEGOTIATING half is
// `NegotiationAdvisers.highPriestUsers`, and its ACTION is
// `gameplay/powers/legacies/HighPriest`.

object HighPriestCard extends Legacy(LegacyId("L21"), "High Priest",
    LegacyGoal.DarkestSecret, Suit.Order):
  val power = PrintedPower(PowerId("legacy.high-priest"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** If you have the Darkest Secret, you may move " +
      "any number of [secret] from the Darkest Secret to your " +
      "player board. **WHEN NEGOTIATING:** You may give [secret].")
  val powers: Vector[PrintedPower] = Vector(power)
