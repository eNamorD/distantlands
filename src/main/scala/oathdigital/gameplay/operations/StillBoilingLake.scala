package oathdigital.gameplay.operations

import oathdigital.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import oathdigital.model._

object BoilingLakeCard extends Edifice(EdificeId("E24"), Suit.Discord):
  object intact extends EdificeFace("Boiling Lake") with Locked:
    val power = PrintedPower(PowerId("edifice.e24.intact"),
      persistent = false, cost = Cost.free,
      text = "If you travel to this site and do not rule this card, you " +
        "**must** kill 2 warbands on your board if able.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Still Boiling Lake"):
    val power = PrintedPower(PowerId("edifice.e24.ruined"),
      persistent = true, cost = Cost.free,
      text = "Cards **cannot** be played to this site. _This card can " +
        "still be discarded by People's Favor, etc._")
    val powers: Vector[PrintedPower] = Vector(power)
