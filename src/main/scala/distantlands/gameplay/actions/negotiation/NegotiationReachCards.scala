package distantlands.gameplay.actions.negotiation

import distantlands.catalog.{AdviserOnly, Denizen, Edifice, EdificeFace,
  Locked, PrintedPower}
import distantlands.model._

/** The cards whose powers widen who a Negotiation reaches (catalog batch
  * 12, slice 2). They live beside the Negotiation, which reads them
  * ([[NegotiationReach]]), as the Challenge's reach cards live beside the
  * Challenge. Their powers are registered in `gameplay.powers.action`. The
  * Tribunal's "even into later games" is printed text the engine does not
  * track. */
object TravelingNegotiatorCard extends Denizen(DenizenId("242"), "Traveling Negotiator", Suit.Nomad) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.traveling-negotiator"),
    persistent = false, cost = Cost.free,
    text = "**WHEN NEGOTIATING:** You may negotiate with any player.")
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
