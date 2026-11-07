package oathdigital.catalog.holding

import oathdigital.catalog.{AdviserOnly, Denizen, PrintedPower}
import oathdigital.model.{Cost, DenizenId, PowerId, Suit}

// Generated from the retired runtime JSON (card classes slice 1).
// Hand-edited from here on; a card moves out when it is implemented.

object CouncilArbiterCard extends Denizen(DenizenId("200"), "Council Arbiter", Suit.Arcane) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.council-arbiter"),
    persistent = false, cost = Cost.free,
    text = "**WHEN NEGOTIATING:** You may give, receive, and swap " +
      "advisers. _You cannot exchange locked advisers. You still " +
      "follow your adviser limit._")
  val powers: Vector[PrintedPower] = Vector(power)

object MasterOfDisguiseCard extends Denizen(DenizenId("78"), "Master of Disguise", Suit.Arcane) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.master-of-disguise"),
    persistent = false, cost = Cost(secret = 1),
    text = "Act as if you had another player's advisers instead. _(You " +
      "can't use your other advisers.)_")
  val powers: Vector[PrintedPower] = Vector(power)
