package oathdigital.gameplay.powers.cardplay

import oathdigital.gameplay.powers.campaign.VowOfUnionCard
import oathdigital.model.{DenizenId, PowerId, Suit}

/** Vow of Union's restriction (card 185, adviser-only, locked): "You may
  * only have [Beast] advisers--bury other advisers you have or would gain."
  *
  * It is a [[SuitOnlyAdvisers]] rule for Beasts, as Family Wagon is for
  * Nomads. It lifts no limit. Its Campaign half is
  * [[oathdigital.gameplay.powers.campaign.VowOfUnion]].
  */
case object VowOfUnionAdvisers extends SuitOnlyAdvisers:
  val id: PowerId = VowOfUnionCard.restriction.id
  /** The note on a card play Vow of Union will bury. */
  val buryWarning: String = "Vow of Union will bury it"
  /** The question that orders Pledge to Discord's bury. */
  val pledgeDecisionId: String = "power.vow-of-union.pledge"

  def cardId: DenizenId = VowOfUnionCard.id
  def suit: Suit = Suit.Beast
  def ruleName: String = VowOfUnionCard.name
