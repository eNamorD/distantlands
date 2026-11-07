package oathdigital.catalog.holding

import oathdigital.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import oathdigital.model.{Cost, DenizenId, PowerId, Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object ReliquaryRaidCard extends Denizen(DenizenId("220"), "Reliquary Raid", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.reliquary-raid"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** take a random relic from the Reliquary.")
  val powers: Vector[PrintedPower] = Vector(power)

object FalseProphetCard extends Denizen(DenizenId("85"), "False Prophet", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.false-prophet"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** if you're an Exile, gain one warband and " +
      "put it on any revealed Vision. You now also have it " +
      "revealed. If it is ever discarded, kill the warband and " +
      "play or discard the Vision as if you had searched.")
  val powers: Vector[PrintedPower] = Vector(power)
