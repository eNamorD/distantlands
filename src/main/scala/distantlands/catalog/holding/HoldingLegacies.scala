package distantlands.catalog.holding

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.model.{Cost, FoundationNumber, LegacyId, PowerId,
  Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object TheNeedleCard extends Legacy(LegacyId("L05"), "The Needle",
    LegacyGoal.MostSites, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.the-needle"),
    persistent = false, cost = Cost.free,
    text = "After you take a banner in any way, you may flip it to its " +
      "other side.")
  val powers: Vector[PrintedPower] = Vector(power)

object KeeperOfOrderCard extends Legacy(LegacyId("L11"), "Keeper of Order",
    LegacyGoal.GrandScepter, Suit.Order):
  val power = PrintedPower(PowerId("legacy.keeper-of-order"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** **BATTLE PLAN:** If your enemy " +
      "is an Imperial, ±2 [attack-die] and you ignore all [skull] " +
      "you roll when attacking.")
  val powers: Vector[PrintedPower] = Vector(power)

object ArbiterCard extends Legacy(LegacyId("L23"), "Arbiter",
    LegacyGoal.DarkestSecret, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.arbiter"),
    persistent = false, cost = Cost.free,
    text = "**END OF ROUND:** Before the end die is rolled, you may " +
      "choose to modify the roll by −1 to +3.")
  val powers: Vector[PrintedPower] = Vector(power)

object ConspiratorCard extends Legacy(LegacyId("L26"), "Conspirator",
    LegacyGoal.HighestRelic, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.conspirator"),
    persistent = false, cost = Cost.free,
    text = "**WAKE:** On your first turn, secretly write down which " +
      "round the game will end on and who will win. If you are " +
      "correct, you win instead. _This even beats a Successor win._")
  val powers: Vector[PrintedPower] = Vector(power)

object LightFingersCard extends Legacy(LegacyId("L27"), "Light Fingers",
    LegacyGoal.HighestRelic, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.light-fingers"),
    persistent = false, cost = Cost.free,
    text = "**WAKE:** When you Take Wealth, you may take [favor] or " +
      "[secret] from your site even if enemy pawns are there.")
  val powers: Vector[PrintedPower] = Vector(power)

object BelovedCard extends Legacy(LegacyId("L33"), "Beloved",
    LegacyGoal.MostOfSuit, Suit.Beast, Some(FoundationNumber.IV)):
  val power = PrintedPower(PowerId("legacy.beloved"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation IV to Teeming World. **WHEN TRADING:** You " +
      "may trade with advisers held by other players whose pawns " +
      "are at your site.")
  val powers: Vector[PrintedPower] = Vector(power)

object TheMouthCard extends Legacy(LegacyId("L34"), "The Mouth",
    LegacyGoal.MostOfSuit, Suit.Arcane, Some(FoundationNumber.VI)):
  val power = PrintedPower(PowerId("legacy.the-mouth"),
    persistent = false, cost = Cost(favorBurnt = 2),
    text = "Set Foundation VI to Festival. **ACTION:** Burn " +
      "[favor-burnt] [favor-burnt] to activate a dormant legacy if " +
      "you meet its goal. _You can even activate legacies in the " +
      "Public Ambitions foundation._")
  val powers: Vector[PrintedPower] = Vector(power)
