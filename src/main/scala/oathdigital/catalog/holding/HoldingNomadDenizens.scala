package oathdigital.catalog.holding

import oathdigital.catalog.{AdviserOnly, Denizen, PrintedPower,
  SiteOnly}
import oathdigital.model.{Cost, DenizenId, PowerId, Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object TravelingNegotiatorCard extends Denizen(DenizenId("242"), "Traveling Negotiator", Suit.Nomad) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.traveling-negotiator"),
    persistent = false, cost = Cost.free,
    text = "**WHEN NEGOTIATING:** You may negotiate with any player.")
  val powers: Vector[PrintedPower] = Vector(power)

object TheGatheringCard extends Denizen(DenizenId("27"), "The Gathering", Suit.Nomad) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.the-gathering"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** any players in turn order may put their " +
      "pawn on this site. Then, players with pawns here may " +
      "negotiate binding exchanges of [favor], [secret], relics, " +
      "and advisers.")
  val powers: Vector[PrintedPower] = Vector(power)
