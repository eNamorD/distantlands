package distantlands.gameplay.powers.campaign

import distantlands.catalog.{Denizen, PrintedPower}
import distantlands.model.{DecisionOptionRef, DenizenId, PowerId}

/** A battle plan printed on a denizen. The plan is its card's printed
  * power, so it names the card once: its `id` is the power's, and the plan
  * decision names the card itself.
  *
  * `id` is a constructor value, so it is set before the plan's own body
  * runs: a plan's `NoteSet(id)` reads it as the object initialises.
  */
abstract class DenizenPlan(final val card: Denizen, power: PrintedPower)
    extends BattlePlan:
  final val id: PowerId = power.id
  final val cardId: DenizenId = card.id
  final def cardRef: DecisionOptionRef = DecisionOptionRef.Denizen(cardId)
