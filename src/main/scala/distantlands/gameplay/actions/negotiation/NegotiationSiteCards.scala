package distantlands.gameplay.actions.negotiation

import distantlands.catalog.{Denizen, PrintedPower}
import distantlands.model.{Cost, DenizenId, PowerId, Suit}

/** The card whose power lets sites into a Negotiation deal (catalog batch
  * 12, slice 4). It lives beside the Negotiation, which reads it
  * ([[NegotiationSites]]). Its power is registered in
  * `gameplay.powers.action`. */
object DeedWriterCard extends Denizen(DenizenId("146"), "Deed Writer", Suit.Hearth):
  val power = PrintedPower(PowerId("denizen.deed-writer"),
    persistent = false, cost = Cost.free,
    text = "**WHEN NEGOTIATING:** You may give, receive, and swap " +
      "sites. Old ruler moves warbands from site to their board, " +
      "and new ruler moves warbands to site from their board.")
  val powers: Vector[PrintedPower] = Vector(power)
