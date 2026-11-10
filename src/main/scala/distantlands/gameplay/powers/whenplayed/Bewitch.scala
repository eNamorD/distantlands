package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{Denizen, PrintedPower}
import oathdigital.gameplay.PlayerSecretSummary
import oathdigital.model._

object BewitchCard extends Denizen(DenizenId("67"), "Bewitch", Suit.Arcane):
  val power = PrintedPower(PowerId("denizen.bewitch"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** if you're an Exile and have the most " +
      "[secret] _(even on cards)_, you may take a random relic " +
      "from the Reliquary and become a Citizen. _Follow the steps " +
      "in the rules and end your Act Phase._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Bewitch (card 67), WHEN PLAYED: "if you're an Exile and have the most
  * secrets (even on cards), you may take a random relic from the Reliquary
  * and become a Citizen."
  *
  * Secrets are counted as the player's area shows them
  * (`PlayerSecretSummary.totalSecrets`), which credits every secret on
  * cards in play to the active player; strictly more than every other
  * player is the most (ruling R21).
  *
  * The shared reading is [[CitizenshipWhenPlayed]]'s. */
case object Bewitch extends CitizenshipWhenPlayed(BewitchCard.id,
  BewitchCard.power.id, "Bewitch", Some(CitizenshipWhenPlayed.Condition(
    (ready, player) => CitizenshipWhenPlayed.most(ready, player)(other =>
      PlayerSecretSummary.derive(ready, other).fold(_ => 0, _.totalSecrets)),
    "Did not have the most secrets.")), takesRelic = true)
