package distantlands.catalog.holding

import distantlands.catalog.{Denizen, PrintedPower, SiteOnly}
import distantlands.model.{Cost, DenizenId, PowerId, Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object TheGatheringCard extends Denizen(DenizenId("27"), "The Gathering", Suit.Nomad) with SiteOnly:
  val power = PrintedPower(PowerId("denizen.the-gathering"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED:** any players in turn order may put their " +
      "pawn on this site. Then, players with pawns here may " +
      "negotiate binding exchanges of [favor], [secret], relics, " +
      "and advisers.")
  val powers: Vector[PrintedPower] = Vector(power)
