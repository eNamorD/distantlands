package oathdigital.catalog.holding

import oathdigital.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import oathdigital.model.{Cost, DenizenId, PowerId, Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object CouncilSeatCard extends Denizen(DenizenId("123"), "Council Seat", Suit.Order) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.council-seat"),
    persistent = true, cost = Cost.free,
    text = "If you're a Citizen, you **cannot** be exiled, even by " +
      "yourself.")
  val powers: Vector[PrintedPower] = Vector(power)

object RelicHunterCard extends Denizen(DenizenId("126"), "Relic Hunter", Suit.Order):
  val power = PrintedPower(PowerId("denizen.relic-hunter"),
    persistent = false, cost = Cost.free,
    text = "You may target facedown relics at targeted sites, adding 1 " +
      "[defense-die] per relic. You may put any relics you take on " +
      "the bottom of the relic deck.")
  val powers: Vector[PrintedPower] = Vector(power)
