package distantlands.catalog.holding

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.model.{Cost, FoundationNumber, LegacyId, PowerId,
  Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object PeacemakerCard extends Legacy(LegacyId("L04"), "Peacemaker",
    LegacyGoal.MostSites, Suit.Hearth):
  val power = PrintedPower(PowerId("legacy.peacemaker"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** Return a banner you hold to the shared bank. " +
      "_Do not adjust its [favor] or [secret]._ If you do, remove " +
      "all warbands except the last _(including bandits)_ from " +
      "each site and player board, even yours.")
  val powers: Vector[PrintedPower] = Vector(power)

object TheNeedleCard extends Legacy(LegacyId("L05"), "The Needle",
    LegacyGoal.MostSites, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.the-needle"),
    persistent = false, cost = Cost.free,
    text = "After you take a banner in any way, you may flip it to its " +
      "other side.")
  val powers: Vector[PrintedPower] = Vector(power)

object AncestralLandsCard extends Legacy(LegacyId("L08"), "Ancestral Lands",
    LegacyGoal.GrandScepter, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.ancestral-lands"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** **TRAVEL:** Traveling to the " +
      "Cradle costs you only 1 Supply.")
  val powers: Vector[PrintedPower] = Vector(power)

object KeeperOfOrderCard extends Legacy(LegacyId("L11"), "Keeper of Order",
    LegacyGoal.GrandScepter, Suit.Order):
  val power = PrintedPower(PowerId("legacy.keeper-of-order"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** **BATTLE PLAN:** If your enemy " +
      "is an Imperial, ±2 [attack-die] and you ignore all [skull] " +
      "you roll when attacking.")
  val powers: Vector[PrintedPower] = Vector(power)

object GoldenKeyCard extends Legacy(LegacyId("L12"), "Golden Key",
    LegacyGoal.GrandScepter, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.golden-key"),
    persistent = false, cost = Cost(favorBurnt = 1, secretBurnt = 1),
    text = "**IF YOU ARE AN IMPERIAL:** You may always peek at the " +
      "Reliquary. **ACTION:** Burn [secret-burnt] [favor-burnt] to " +
      "take a relic from the Reliquary.")
  val powers: Vector[PrintedPower] = Vector(power)

object HighPriestCard extends Legacy(LegacyId("L21"), "High Priest",
    LegacyGoal.DarkestSecret, Suit.Order):
  val power = PrintedPower(PowerId("legacy.high-priest"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** If you have the Darkest Secret, you may move " +
      "any number of [secret] from the Darkest Secret to your " +
      "player board. **WHEN NEGOTIATING:** You may give [secret].")
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

object RuthlessCard extends Legacy(LegacyId("L29"), "Ruthless",
    LegacyGoal.HighestRelic, Suit.Order):
  val power = PrintedPower(PowerId("legacy.ruthless"),
    persistent = false, cost = Cost(favorBurnt = 2),
    text = "**ACTION:** Burn [favor-burnt] [favor-burnt] to bury a " +
      "denizen at your site.")
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

object PathfinderCard extends Legacy(LegacyId("L36"), "Pathfinder",
    LegacyGoal.MostOfSuit, Suit.Nomad, Some(FoundationNumber.I)):
  val power = PrintedPower(PowerId("legacy.pathfinder"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation I to Wide Horizons. **TRAVEL:** You may " +
      "ignore the Mountain, Island, and Pass powers.")
  val powers: Vector[PrintedPower] = Vector(power)
