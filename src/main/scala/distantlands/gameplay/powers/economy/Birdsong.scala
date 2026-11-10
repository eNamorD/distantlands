package oathdigital.gameplay.powers.economy

import oathdigital.catalog.{AdviserOnly, Denizen, ModifierMark, PrintedPower}
import oathdigital.gameplay.actions.economy.TradeProcedure
import oathdigital.gameplay.powerresolver.Contribution
import oathdigital.gameplay.powers.SelectedModifier
import oathdigital.model._

object BirdsongCard extends Denizen(DenizenId("176"), "Birdsong", Suit.Beast) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.birdsong"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Trade)),
    text = "Spend no Supply if you're trading with a [suit-beast] or " +
      "[suit-nomad] card.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Birdsong (card 176, adviser-only), a selected Trade modifier with no cost:
  * trading with a beast or nomad card spends no Supply. It may be selected
  * whatever the card, as the Cup of Plenty may. The waiver is
  * [[SuitSupplyWaiver]].
  */
case object Birdsong extends SelectedModifier:
  val id: PowerId = BirdsongCard.power.id
  val Suits: Set[Suit] = Set(Suit.Beast, Suit.Nomad)

  val card: Denizen = BirdsongCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Trade)

  def effects: Map[PowerWindow, Vector[Contribution]] = SuitSupplyWaiver.effects(
    PowerWindow.TradeCost, TradeProcedure.decisionId)(
    (_, _, _, _, suit) => Birdsong.Suits(suit))
