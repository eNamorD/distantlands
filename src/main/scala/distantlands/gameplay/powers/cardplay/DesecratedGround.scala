package oathdigital.gameplay.powers.cardplay

import oathdigital.gameplay.DesecratedGroundRule
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution}
import oathdigital.model._

/** Desecrated Ground (E08's ruined face): "If an Exile reveals a Vision
  * while their pawn is at this site, they do not discard their current
  * revealed Vision. They can have any number!"
  *
  * The rule is `DesecratedGroundRule`, which `CardSlots` and `CardPlay`
  * read. This power hooks no window. It is registered so the card counts
  * as implemented, and it declares the kept line the card play writes.
  */
case object DesecratedGround extends ContributingPower:
  val id: PowerId = DesecratedGroundRule.power
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  override def noteKeys: Vector[NoteKey] = DesecratedGroundRule.noteKeys
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
