package distantlands.gameplay

import distantlands.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import distantlands.model._

object VowOfKinshipCard extends Denizen(DenizenId("152"), "Vow of Kinship", Suit.Nomad) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.vow-of-kinship"),
    persistent = false, cost = Cost.free,
    text = "Move all of your [favor] to the [suit-nomad] bank. Any " +
      "[favor] you gain or take is put in the [suit-nomad] bank. " +
      "You can use [favor] in the [suit-nomad] bank as if it is on " +
      "your board.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Vow of Kinship (card 152, catalog batch 11, rulings 9, 10, 12 and 26 to
  * 29). Its holder is the player who acts with it faceup among their
  * advisers (`PowerAccess.holdsFaceupAdviser`), so Master of Disguise lifts
  * it, or lends it, for one Trade.
  *
  * Favor bound for the holder's board lands in the Nomad bank
  * (`creditAt`, applied by the executor). Favor the holder spends may come
  * from the bank: `KinshipDraws` draws it onto the board before the spend.
  * Anything done to the holder reads their board only.
  */
object KinshipRule:
  def holds(ready: ReadyGame, player: PlayerId): Boolean =
    PowerAccess.holdsFaceupAdviser(ready, player, VowOfKinshipCard.id)

  /** Where favor moved to `to` lands: the Nomad bank instead of a holder's
    * board. */
  def creditAt(ready: ReadyGame, to: Location): Location = to match
    case Location.PlayArea(player) if holds(ready, player) =>
      Location.FavorBank(Suit.Nomad)
    case other => other

  /** The favor in the Nomad bank. */
  def bank(ready: ReadyGame): Int =
    ready.banks.favor.getOrElse(Suit.Nomad, 0)

/** The favor `player` may spend: their board's, and for Vow of Kinship's
  * holder the Nomad bank's too (ruling 9). A check of a spend reads this;
  * a check of something done to the player reads the board. */
object SpendableFavor:
  def of(ready: ReadyGame, player: PlayerId): Int =
    val board = ready.game.current.player(player).fold(0)(_.board.favor)
    if KinshipRule.holds(ready, player) then board + KinshipRule.bank(ready)
    else board
