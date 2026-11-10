package distantlands.gameplay.powers.poweruse

import distantlands.gameplay.powerresolver.PowerUse
import distantlands.model._

/** Placing favor or secrets on a card (catalog batch 8, slice 10): a cost
  * paid onto it on the payer's own turn, a gift of favor or secrets onto
  * it, or a forced placement of a player's own favor or secrets, which
  * nobody moves (Whispering Leaves: "They must place"). Off its payer's
  * turn a payment settles to the bank and places nothing
  * (`PayCostSettlement`). */
private[powers] object Placements:
  /** The placer and the card, when `operation` places on a card. */
  def onto(ready: ReadyGame, operation: CoreOperation)
      : Option[(PlayerId, CardId)] = operation match
    case pay: PayCost if pay.cost.favor > 0 || pay.cost.secret > 0 =>
      (pay.placedAt, pay.player) match
        case (Location.OnCard(card), player)
            if player == ready.game.current.turn.activePlayer =>
          Some(player -> card)
        case _ => None
    case give: Give => (give.piece, give.to) match
      case (_: Piece.Favor | _: Piece.Secrets, Location.OnCard(card)) =>
        Some(give.giver -> card)
      case _ => None
    // A forced placement: the board's owner places it, though nobody
    // chose to (catalog batch 11, ruling 28).
    case Move(_: Piece.Favor | _: Piece.Secrets,
        PositionedLocation(Location.PlayArea(player), _, _),
        PositionedLocation(Location.OnCard(card), _, _), _, None) =>
      Some(player -> card)
    case _ => None

  /** The card `use`'s cost places on, when it places on its user's turn. */
  def byUse(ready: ReadyGame, use: PowerUse): Option[CardId] =
    use.card.filter(_ =>
      use.places && use.user == ready.game.current.turn.activePlayer)
