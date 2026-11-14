package distantlands.catalog.holding

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.model.{Cost, FoundationNumber, LegacyId, PowerId,
  Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object IronHandCard extends Legacy(LegacyId("L01"), "Iron Hand",
    LegacyGoal.MostSites, Suit.Order):
  val power = PrintedPower(PowerId("legacy.iron-hand"),
    persistent = false, cost = Cost.free,
    text = "**If you are Imperial, discard Iron Hand.**\n\n**SETUP:** If " +
      "you were in the previous game, you place one warband on all " +
      "sites coming from the Atlas until the Empire divider, and " +
      "players may place their pawn at any site.\n\n**CHRONICLE:** " +
      "Score your legacies in addition to the winner, and perform " +
      "Shape Empire and Build Edifice (Throne). Your sites are " +
      "stored in place of the Empire (Stars).")
  val powers: Vector[PrintedPower] = Vector(power)

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

object CircleOfSwordsCard extends Legacy(LegacyId("L06"), "Circle of Swords",
    LegacyGoal.MostSites, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.circle-of-swords"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** You gain eight starting warbands _(instead of " +
      "three)_. If you're Imperial and other Imperials would gain " +
      "fewer than three, you gain less.")
  val powers: Vector[PrintedPower] = Vector(power)

object AncestralLandsCard extends Legacy(LegacyId("L08"), "Ancestral Lands",
    LegacyGoal.GrandScepter, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.ancestral-lands"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** **TRAVEL:** Traveling to the " +
      "Cradle costs you only 1 Supply.")
  val powers: Vector[PrintedPower] = Vector(power)

object ScepterBearerCard extends Legacy(LegacyId("L09"), "Scepter Bearer",
    LegacyGoal.GrandScepter, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.scepter-bearer"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** **SETUP:** You start with the " +
      "Grand Scepter _(instead of the Chancellor)_.")
  val powers: Vector[PrintedPower] = Vector(power)

object RoyalLineCard extends Legacy(LegacyId("L10"), "Royal Line",
    LegacyGoal.GrandScepter, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.royal-line"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE A CITIZEN:** **CHRONICLE (SUN):** You may " +
      "choose which Imperial meets the Successor goal. _Ignore the " +
      "printed goal. You may choose no one._")
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
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE AN IMPERIAL:** You may always peek at the " +
      "Reliquary. **ACTION:** Burn [secret-burnt] [favor-burnt] to " +
      "take a relic from the Reliquary.")
  val powers: Vector[PrintedPower] = Vector(power)

object WorldCrafterCard extends Legacy(LegacyId("L13"), "World Crafter",
    LegacyGoal.PeoplesFavor, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.world-crafter"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** For the Imperial Maps foundation, you fill site " +
      "slots, using any mix of Recent and Forgotten Sites _(after " +
      "resolving the Rival legacy)_. For the Wide Horizons " +
      "foundation, when anyone explores, you fill the slot with a " +
      "Recent or Forgotten Site.")
  val powers: Vector[PrintedPower] = Vector(power)

object MatchmakerCard extends Legacy(LegacyId("L14"), "Matchmaker",
    LegacyGoal.PeoplesFavor, Suit.Hearth):
  val power = PrintedPower(PowerId("legacy.matchmaker"),
    persistent = false, cost = Cost.free,
    text = "Whenever players draw legacies from the deck and choose one " +
      "to keep, you draw and choose for them instead. _The Amber " +
      "Doors edifice does not trigger this._")
  val powers: Vector[PrintedPower] = Vector(power)

object RevolutionaryCard extends Legacy(LegacyId("L15"), "Revolutionary",
    LegacyGoal.PeoplesFavor, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.revolutionary"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE (THRONE):** After Build Edifice, you may " +
      "discard any number of denizens at Imperial sites.")
  val powers: Vector[PrintedPower] = Vector(power)

object PopulistCard extends Legacy(LegacyId("L16"), "Populist",
    LegacyGoal.PeoplesFavor, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.populist"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** When setting up the favor banks, you may choose " +
      "to divide the favor among the banks evenly. If you do, take " +
      "any remainder. _For example, dividing 19 favor among the " +
      "banks leaves you one favor._")
  val powers: Vector[PrintedPower] = Vector(power)

object TheStandardBearerCard extends Legacy(LegacyId("L18"), "The Standard Bearer",
    LegacyGoal.PeoplesFavor, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.the-standard-bearer"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** After placing your pawn, you may take a banner " +
      "from the shared bank and place two more [favor] or [secret] " +
      "on it from the shared bank.")
  val powers: Vector[PrintedPower] = Vector(power)

object TheEarCard extends Legacy(LegacyId("L19"), "The Ear",
    LegacyGoal.DarkestSecret, Suit.Hearth):
  val power = PrintedPower(PowerId("legacy.the-ear"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** You may peek at the relics at any site. " +
      "**CHRONICLE (THRONE):** In Scatter Relics, you examine the " +
      "relics and choose which sites they go to.")
  val powers: Vector[PrintedPower] = Vector(power)

object HeirloomCard extends Legacy(LegacyId("L20"), "Heirloom",
    LegacyGoal.DarkestSecret, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.heirloom"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE (THRONE):** Before Scatter Relics, secretly " +
      "choose one of your relics to keep facedown. You will start " +
      "the next game with it.")
  val powers: Vector[PrintedPower] = Vector(power)

object HighPriestCard extends Legacy(LegacyId("L21"), "High Priest",
    LegacyGoal.DarkestSecret, Suit.Order):
  val power = PrintedPower(PowerId("legacy.high-priest"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** If you have the Darkest Secret, you may move " +
      "any number of [secret] from the Darkest Secret to your " +
      "player board. **WHEN NEGOTIATING:** You may give [secret].")
  val powers: Vector[PrintedPower] = Vector(power)

object SeerCard extends Legacy(LegacyId("L22"), "Seer",
    LegacyGoal.DarkestSecret, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.seer"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** When building the world deck, peek at the " +
      "Visions and secretly place them in any positions.")
  val powers: Vector[PrintedPower] = Vector(power)

object ArbiterCard extends Legacy(LegacyId("L23"), "Arbiter",
    LegacyGoal.DarkestSecret, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.arbiter"),
    persistent = false, cost = Cost.free,
    text = "**END OF ROUND:** Before the end die is rolled, you may " +
      "choose to modify the roll by −1 to +3.")
  val powers: Vector[PrintedPower] = Vector(power)

object TheBrightBeaconCard extends Legacy(LegacyId("L24"), "The Bright Beacon",
    LegacyGoal.DarkestSecret, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.the-bright-beacon"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE (BEACON):** If you're resolving the Beacon, you " +
      "may take any six cards from the Dispossessed _(instead of " +
      "choosing suits and taking from their matching dividers)_.")
  val powers: Vector[PrintedPower] = Vector(power)

object OathsOfLoyaltyCard extends Legacy(LegacyId("L25"), "Oaths of Loyalty",
    LegacyGoal.HighestRelic, Suit.Hearth):
  val power = PrintedPower(PowerId("legacy.oaths-of-loyalty"),
    persistent = false, cost = Cost.free,
    text = "When you would gain a starting adviser, bury it, except " +
      "for—**CHRONICLE (WORLD):** In Gather Denizens, keep your " +
      "faceup advisers. They will be your facedown starting " +
      "advisers in the next game.")
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

object SecretSocietyCard extends Legacy(LegacyId("L28"), "Secret Society",
    LegacyGoal.HighestRelic, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.secret-society"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** Before building the world deck, you may secretly " +
      "choose a Vision except the Conspiracy to take as a facedown " +
      "adviser _(discard excess advisers)_. If you do, advance the " +
      "Visions Drawn marker once.")
  val powers: Vector[PrintedPower] = Vector(power)

object RuthlessCard extends Legacy(LegacyId("L29"), "Ruthless",
    LegacyGoal.HighestRelic, Suit.Order):
  val power = PrintedPower(PowerId("legacy.ruthless"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** Burn [favor-burnt] [favor-burnt] to bury a " +
      "denizen at your site.")
  val powers: Vector[PrintedPower] = Vector(power)

object QuestToDistantLandsCard extends Legacy(LegacyId("L30"), "Quest to Distant Lands",
    LegacyGoal.HighestRelic, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.quest-to-distant-lands"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** You may choose to not place your pawn. If you " +
      "do, you may choose one card from the Dispossessed and swap " +
      "it with a starting adviser or card you drew in Foundation " +
      "IV. _It is not buried by the Oaths of Loyalty legacy._ " +
      "**WAKE:** On your first turn, you **must** place your pawn " +
      "at any site.")
  val powers: Vector[PrintedPower] = Vector(power)

object WealthyCard extends Legacy(LegacyId("L31"), "Wealthy",
    LegacyGoal.MostOfSuit, Suit.Hearth, Some(FoundationNumber.II)):
  val power = PrintedPower(PowerId("legacy.wealthy"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation II to Movers and Shakers. **SETUP:** After " +
      "placing your pawn, take [favor] from two players or take " +
      "[favor] [favor] [favor] from the shared bank.")
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
    persistent = false, cost = Cost.free,
    text = "Set Foundation VI to Festival. **ACTION:** Burn " +
      "[favor-burnt] [favor-burnt] to activate a dormant legacy if " +
      "you meet its goal. _You can even activate legacies in the " +
      "Public Ambitions foundation._")
  val powers: Vector[PrintedPower] = Vector(power)

object ReformerCard extends Legacy(LegacyId("L35"), "Reformer",
    LegacyGoal.MostOfSuit, Suit.Order, Some(FoundationNumber.V)):
  val power = PrintedPower(PowerId("legacy.reformer"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation V to Grand Council. **CHRONICLE (START OF " +
      "THRONE):** If you won, you may move and swap denizens at " +
      "sites you rule, ignoring the locked restriction.")
  val powers: Vector[PrintedPower] = Vector(power)

object PathfinderCard extends Legacy(LegacyId("L36"), "Pathfinder",
    LegacyGoal.MostOfSuit, Suit.Nomad, Some(FoundationNumber.I)):
  val power = PrintedPower(PowerId("legacy.pathfinder"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation I to Wide Horizons. **TRAVEL:** You may " +
      "ignore the Mountain, Island, and Pass powers.")
  val powers: Vector[PrintedPower] = Vector(power)
