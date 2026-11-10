package distantlands.gameplay

import distantlands.model._

/** The order of a round (CR p. 17). A round runs from the player recorded
  * when it began (ruling R4); the next round begins with the Chancellor,
  * or with the first player when there is none. */
object TurnOrder:
  def roundStart(ready: ReadyGame): PlayerId =
    ready.game.current.tracks.roundStarter

  def nextRoundStart(ready: ReadyGame): PlayerId =
    Imperials.chancellor(ready).getOrElse(ready.setup.firstPlayer)

  def order(ready: ReadyGame): Vector[PlayerId] =
    val players = ready.game.current.players.map(_.player)
    val index = players.indexOf(roundStart(ready))
    players.drop(index) ++ players.take(index)
