package oathdigital.catalog.holding

import oathdigital.catalog.{PrintedPower, Relic}
import oathdigital.model.{Cost, PowerId, RelicId}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object GrandMaskCard extends Relic(RelicId("R05"), "Grand Mask", value = 36, defense = 1):
  val power = PrintedPower(PowerId("relic.grand-mask"),
    persistent = true, cost = Cost.free,
    text = "If you're an Exile, during your turn you rule cards except " +
      "battle plans at Imperial sites, and Imperial players do not.")
  val powers: Vector[PrintedPower] = Vector(power)

object BanditCrownCard extends Relic(RelicId("R11"), "Bandit Crown", value = 56, defense = 3):
  val power = PrintedPower(PowerId("relic.bandit-crown"),
    persistent = true, cost = Cost.free,
    text = "You may use cards at sites ruled by bandits as if you ruled " +
      "them. _You may use these battle plans against bandits._")
  val powers: Vector[PrintedPower] = Vector(power)

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

object SigilOfTheEyeCard extends Relic(RelicId("R43"), "Sigil of the Eye", value = 66, defense = 1):
  val power = PrintedPower(PowerId("relic.sigil-of-the-eye"),
    persistent = false, cost = Cost.free,
    text = "You may challenge the Darkest Secret from any site.")
  val powers: Vector[PrintedPower] = Vector(power)

object SigilOfTheHeartCard extends Relic(RelicId("R44"), "Sigil of the Heart", value = 53, defense = 1):
  val power = PrintedPower(PowerId("relic.sigil-of-the-heart"),
    persistent = false, cost = Cost.free,
    text = "You may challenge the People's Favor from any site.")
  val powers: Vector[PrintedPower] = Vector(power)
