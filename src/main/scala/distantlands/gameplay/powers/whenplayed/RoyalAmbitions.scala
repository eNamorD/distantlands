package distantlands.gameplay.powers.whenplayed

import distantlands.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import distantlands.gameplay.PowerAccess
import distantlands.model._

object RoyalAmbitionsCard extends Denizen(DenizenId("88"), "Royal Ambitions", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.royal-ambitions"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** if you're an Exile and rule the most " +
      "sites, you may take a random relic from the Reliquary and " +
      "become a Citizen. _Follow the steps in the rules and end " +
      "your Act Phase._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Royal Ambitions (card 88, adviser-only, locked), WHEN PLAYED: "if you're
  * an Exile and rule the most sites, you may take a random relic from the
  * Reliquary and become a Citizen."
  *
  * Strictly more sites than every other player is the most (ruling R21).
  *
  * The shared reading is [[CitizenshipWhenPlayed]]'s. */
case object RoyalAmbitions extends CitizenshipWhenPlayed(
  RoyalAmbitionsCard.id, RoyalAmbitionsCard.power.id, "Royal Ambitions",
  Some(CitizenshipWhenPlayed.Condition(
    (ready, player) => CitizenshipWhenPlayed.most(ready, player)(other =>
      PowerAccess.ruledSites(ready, other).size),
    "Did not rule the most sites.")), takesRelic = true)
