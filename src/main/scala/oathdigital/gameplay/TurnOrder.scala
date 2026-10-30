package oathdigital.gameplay

import oathdigital.model._

/** The order of a round (CR p. 17): from the Chancellor, or from the first
  * player when there is none. */
object TurnOrder:
  def roundStart(ready: ReadyGame): PlayerId =
    Imperials.chancellor(ready).getOrElse(ready.setup.firstPlayer)

  def order(ready: ReadyGame): Vector[PlayerId] =
    val players = ready.game.current.players.map(_.player)
    val index = players.indexOf(roundStart(ready))
    players.drop(index) ++ players.take(index)
