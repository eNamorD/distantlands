package oathdigital.gameplay.powers.whenplayed

import oathdigital.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import oathdigital.gameplay.powers.banner.CardPowerBanners
import oathdigital.model._

object AncientPactCard extends Denizen(DenizenId("166"), "Ancient Pact", Suit.Nomad) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.ancient-pact"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** if you're an Exile and hold the Darkest " +
      "Secret, you may take a random relic from the Reliquary and " +
      "become a Citizen. _Follow the steps in the rules and end " +
      "your Act Phase._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Ancient Pact (card 166, adviser-only, locked), WHEN PLAYED: "if you're an
  * Exile and hold the Darkest Secret, you may take a random relic from the
  * Reliquary and become a Citizen."
  *
  * The shared reading is [[CitizenshipWhenPlayed]]'s. */
case object AncientPact extends CitizenshipWhenPlayed(AncientPactCard.id,
  AncientPactCard.power.id, "Ancient Pact", Some(CitizenshipWhenPlayed.Condition(
    (ready, player) => CardPowerBanners.holdsDarkestSecret(ready, player),
    "Did not hold the Darkest Secret.")), takesRelic = true)
