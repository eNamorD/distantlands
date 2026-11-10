package distantlands.gameplay.actions

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

/** A player's adviser limit, read from state, for a rule outside card play
  * that moves advisers between players: the Negotiation's adviser terms
  * (catalog batch 12, ruling 17). Which held cards narrow the limit, lift
  * it or leave an adviser uncounted is the powers' knowledge
  * (`gameplay.powers.AdviserLimit`), and actions may not import powers, so
  * the walker's procedure registry hands that one implementation in.
  */
trait AdviserLimits:
  /** How many advisers `player` may hold. */
  def of(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId): Int
  /** How many of `player`'s advisers count toward the limit. */
  def towardLimit(ready: ReadyGame, player: PlayerId): Int
  /** Whether `player` holds more counted advisers than their limit. */
  final def exceeded(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId): Boolean =
    towardLimit(ready, player) > of(catalog, ready, player)
