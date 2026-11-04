package oathdigital.catalog.holding

import oathdigital.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower}
import oathdigital.model.{Cost, EdificeId, MajorActionType, PowerId, Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object GrandCanalCard extends Edifice(EdificeId("E03"), Suit.Discord):
  object intact extends EdificeFace("Grand Canal") with Locked:
    val power = PrintedPower(PowerId("edifice.e03.intact"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Travel)),
      text = "Act as if this site is a Coast along with its other powers.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Lost Lagoon"):
    val power = PrintedPower(PowerId("edifice.e03.ruined"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Travel)),
      text = "Act as if this site is an Island along with its other " +
        "powers.")
    val powers: Vector[PrintedPower] = Vector(power)

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

object AmberDoorsCard extends Edifice(EdificeId("E05"), Suit.Discord):
  object intact extends EdificeFace("Amber Doors") with Locked:
    val power = PrintedPower(PowerId("edifice.e05.intact"),
      persistent = false, cost = Cost.free,
      text = "SETUP / **WHEN EXPLORED:** If you place your pawn here, you " +
        "may discard all of your legacies (even dormant) to draw 2 " +
        "dormant legacies and activate one.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Lost Doors"):
    val power = PrintedPower(PowerId("edifice.e05.ruined"),
      persistent = false, cost = Cost.free,
      text = "SETUP / **WHEN EXPLORED:** If you place your pawn here, " +
        "draw a dormant legacy, then discard one you hold.")
    val powers: Vector[PrintedPower] = Vector(power)

object WildPasturesCard extends Edifice(EdificeId("E07"), Suit.Nomad):
  object intact extends EdificeFace("Wild Pastures") with Locked:
    val power = PrintedPower(PowerId("edifice.e07.intact"),
      persistent = true, cost = Cost.free,
      text = "Whenever this card's ruler would discard a [suit-nomad] " +
        "card from a power, they may instead pay [favor] to the " +
        "[suit-nomad] bank.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Blasted Pastures"):
    val power = PrintedPower(PowerId("edifice.e07.ruined"),
      persistent = true, cost = Cost.free,
      text = "**CHRONICLE (START OF THRONE):** Bury all [suit-nomad] " +
        "cards on the map unless the winner rules this card.")
    val powers: Vector[PrintedPower] = Vector(power)

object GreatSpireCard extends Edifice(EdificeId("E11"), Suit.Arcane):
  object intact extends EdificeFace("Great Spire") with Locked:
    val power = PrintedPower(PowerId("edifice.e11.intact"),
      persistent = false, cost = Cost(secret = 1),
      text = "**ACTION:** Add 3 random cards from the Dispossessed to " +
        "this region's discard pile, then add the bottom 3 denizens " +
        "from that pile to the Dispossessed.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Fallen Spire"):
    val power = PrintedPower(PowerId("edifice.e11.ruined"),
      persistent = false, cost = Cost.free,
      text = "SETUP (END) / **WHEN EXPLORED:** Swap denizens in this " +
        "region's discard pile with the same number of random cards " +
        "from the Dispossessed. Then, shuffle these cards with any " +
        "Visions in this pile.")
    val powers: Vector[PrintedPower] = Vector(power)

object TheTribunalCard extends Edifice(EdificeId("E19"), Suit.Order):
  object intact extends EdificeFace("The Tribunal") with Locked:
    val power = PrintedPower(PowerId("edifice.e19.intact"),
      persistent = false, cost = Cost.free,
      text = "**WHEN NEGOTIATING:** You may negotiate with players at any " +
        "sites. Players may make binding promises, even into later " +
        "games.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Bandit Tribunal"):
    val power = PrintedPower(PowerId("edifice.e19.ruined"),
      persistent = true, cost = Cost.free,
      text = "**WHEN NEGOTIATING:** Players at sites ruled by bandits may " +
        "negotiate with each other.")
    val powers: Vector[PrintedPower] = Vector(power)
