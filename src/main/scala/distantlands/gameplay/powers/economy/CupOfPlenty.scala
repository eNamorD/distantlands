package oathdigital.gameplay.powers.economy

import oathdigital.catalog.{ModifierMark, PrintedPower, Relic}
import oathdigital.gameplay.actions.economy.{MusterSource, TradeProcedure}
import oathdigital.gameplay.powerresolver.Contribution
import oathdigital.gameplay.powers.SelectedModifier
import oathdigital.model._

object CupOfPlentyCard extends Relic(RelicId("R07"), "Cup of Plenty", value = 96, defense = 1):
  val power = PrintedPower(PowerId("relic.cup-of-plenty"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Trade)),
    text = "Spend no Supply if you're trading with a card that does not " +
      "match any of your advisers.")
  val powers: Vector[PrintedPower] = Vector(power)

/** The Cup of Plenty (relic R07), a selected Trade modifier: trading with a
  * card whose suit differs from every faceup adviser the player holds costs no
  * Supply. A facedown adviser does not count, and a player with no faceup
  * adviser trades free. A picked Acting Troupe counts as its picked suit.
  * The waiver is [[SuitSupplyWaiver]].
  */
case object CupOfPlenty extends SelectedModifier:
  val id: PowerId = CupOfPlentyCard.power.id

  val card: Relic = CupOfPlentyCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Trade)

  def effects: Map[PowerWindow, Vector[Contribution]] = SuitSupplyWaiver.effects(
    PowerWindow.TradeCost, TradeProcedure.decisionId)(
    (catalog, ready, actor, answered, suit) =>
      MusterSource.matching(catalog, ready, actor, suit, answered) == 0)
