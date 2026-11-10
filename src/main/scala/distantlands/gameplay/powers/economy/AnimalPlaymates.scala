package distantlands.gameplay.powers.economy

import distantlands.catalog.{AdviserOnly, Denizen, ModifierMark, PrintedPower}
import distantlands.gameplay.actions.economy.MusterProcedure
import distantlands.gameplay.powerresolver.Contribution
import distantlands.gameplay.powers.SelectedModifier
import distantlands.model._

object AnimalPlaymatesCard extends Denizen(DenizenId("40"), "Animal Playmates", Suit.Beast) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.animal-playmates"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Muster)),
    text = "Spend no Supply if you're mustering on a [suit-beast] card.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Animal Playmates (card 40, adviser-only), a selected Muster modifier with
  * no cost: mustering on a beast denizen or edifice spends no Supply. On any
  * other card the Muster pays as usual. It may be selected whatever the card,
  * as the Cup of Plenty may. The waiver is [[SuitSupplyWaiver]].
  */
case object AnimalPlaymates extends SelectedModifier:
  val id: PowerId = AnimalPlaymatesCard.power.id

  val card: Denizen = AnimalPlaymatesCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Muster)

  def effects: Map[PowerWindow, Vector[Contribution]] = SuitSupplyWaiver.effects(
    PowerWindow.MusterCost, MusterProcedure.decisionId)(
    (_, _, _, _, suit) => suit == Suit.Beast)
