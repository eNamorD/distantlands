package oathdigital.gameplay.powers.cardplay

import oathdigital.catalog.{AdviserOnly, Denizen, PrintedPower}
import oathdigital.gameplay.actions.CardPlayRules
import oathdigital.model._

object FamilyWagonCard extends Denizen(DenizenId("168"), "Family Wagon", Suit.Nomad) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.family-wagon"),
    persistent = true, cost = Cost.free,
    text = "You have no adviser limit but may only have [suit-nomad] " +
      "advisers-—bury other advisers you have or would gain.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Family Wagon (card 168, adviser-only): "You have no adviser limit but
  * may only have [Nomad] advisers--bury other advisers you have or would
  * gain."
  *
  * It is a [[SuitOnlyAdvisers]] rule for Nomads, shared with Vow of Union.
  * While its holder has it faceup it also lifts their limit:
  *  - card play gives the holder no limit. Its transform at
  *    `SearchPlayAdviser` lifts the base limit while Family Wagon is a
  *    faceup adviser after the play (`CardPlayRules.liftWhileFaceUp`).
  *  - `AdviserLimit.of` gives the holder no limit, before the lowering
  *    powers apply (`lifts`).
  */
case object FamilyWagon extends SuitOnlyAdvisers:
  val id: PowerId = FamilyWagonCard.power.id
  val cardId: DenizenId = FamilyWagonCard.id
  /** The note on a card play Family Wagon will bury. */
  val buryWarning: String = "Family Wagon will bury it"
  /** The question that orders Pledge to Discord's bury. */
  val pledgeDecisionId: String = "power.family-wagon.pledge"

  /** Whether `player` has Family Wagon faceup, and so no adviser limit. */
  def lifts(ready: ReadyGame, player: PlayerId): Boolean =
    SuitOnlyAdvisers.hasFaceUp(ready, player, cardId)

  def suit: Suit = Suit.Nomad
  def ruleName: String = FamilyWagonCard.name
  override protected def playRules(rules: CardPlayRules): CardPlayRules =
    rules.liftWhileFaceUp(cardId)
