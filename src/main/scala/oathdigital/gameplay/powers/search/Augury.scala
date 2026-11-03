package oathdigital.gameplay.powers.search

import oathdigital.catalog.{Denizen, ModifierMark, PrintedPower}
import oathdigital.gameplay.powerresolver.{Contribution, Transform}
import oathdigital.gameplay.powers.SelectedModifier
import oathdigital.model._

object AuguryCard extends Denizen(DenizenId("56"), "Augury", Suit.Arcane):
  val power = PrintedPower(PowerId("denizen.augury"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "Draw one more card. _(Stop after a Vision as normal.)_")
  val powers: Vector[PrintedPower] = Vector(power)

/** Augury (card 56), a selected Search modifier: a Search from the world deck
  * or a regional discard draws one more card. The draw still stops after a
  * Vision.
  */
case object Augury extends SelectedModifier:
  val id: PowerId = AuguryCard.power.id
  val More: Int = 1

  val card: Denizen = AuguryCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Search)

  def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SearchBeforeDraw -> Vector(Transform((_, operations) =>
      DrawExtension.extend(operations, Augury.More))))
