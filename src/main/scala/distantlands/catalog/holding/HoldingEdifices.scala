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
