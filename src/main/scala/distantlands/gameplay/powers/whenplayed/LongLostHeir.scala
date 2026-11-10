package distantlands.gameplay.powers.whenplayed

import distantlands.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import distantlands.model._

object LongLostHeirCard extends Denizen(DenizenId("44"), "Long-Lost Heir", Suit.Beast) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.long-lost-heir"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** if you're an Exile, you may become a " +
      "Citizen. _Follow the steps in the rules and end your Act " +
      "Phase._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Long-Lost Heir (card 44, adviser-only, locked), WHEN PLAYED: "if you're
  * an Exile, you may become a Citizen."
  *
  * The shared reading is [[CitizenshipWhenPlayed]]'s. */
case object LongLostHeir extends CitizenshipWhenPlayed(LongLostHeirCard.id,
  LongLostHeirCard.power.id, "Long-Lost Heir", None,
  takesRelic = false)
