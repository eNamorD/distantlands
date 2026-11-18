package distantlands.gameplay.actions.negotiation

import distantlands.catalog.{AdviserOnly, Denizen, Edifice, EdificeFace,
  Locked, PrintedPower}
import distantlands.model._

/** The cards whose powers let advisers into a Negotiation deal (catalog
  * batch 12, slice 3). They live beside the Negotiation, which reads them
  * ([[NegotiationAdvisers]]). Their powers are registered in
  * `gameplay.powers.action`; Squalid District, the ruined face, is an
  * action there. */
object CouncilArbiterCard extends Denizen(DenizenId("200"), "Council Arbiter", Suit.Arcane) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.council-arbiter"),
    persistent = false, cost = Cost.free,
    text = "**WHEN NEGOTIATING:** You may give, receive, and swap " +
      "advisers. _You cannot exchange locked advisers. You still " +
      "follow your adviser limit._")
  val powers: Vector[PrintedPower] = Vector(power)

object FestivalDistrictCard extends Edifice(EdificeId("E21"), Suit.Discord):
  object intact extends EdificeFace("Festival District") with Locked:
    val power = PrintedPower(PowerId("edifice.e21.intact"),
      persistent = false, cost = Cost.free,
      text = "**WHEN NEGOTIATING:** Players may give, receive, and swap " +
        "advisers. _Players must follow their adviser limits._")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Squalid District"):
    val power = PrintedPower(PowerId("edifice.e21.ruined"),
      persistent = false, cost = Cost(favor = 1, favorBurnt = 1),
      text = "**ACTION:** Discard any number of facedown advisers you " +
        "have. Draw that many denizens from the bottom of the world " +
        "deck as facedown advisers.")
    val powers: Vector[PrintedPower] = Vector(power)
