package oathdigital.model

/** The roles of the players at the table (Empire phase design, "Role").
  * A role lives on the lineage; every read of who is Imperial, or who is
  * the Chancellor, goes through here. */
object Imperials:
  def role(ready: ReadyGame, player: PlayerId): Option[Role] =
    ready.game.current.player(player)
      .flatMap(state => ready.game.campaign.lineages.get(state.lineage))
      .map(_.role)

  def isImperial(ready: ReadyGame, player: PlayerId): Boolean =
    role(ready, player).exists(_.isImperial)

  def chancellor(ready: ReadyGame): Option[PlayerId] =
    ready.game.current.players.map(_.player)
      .find(role(ready, _).contains(Role.Chancellor))

  /** The Chancellor and the Citizens, in seat order. */
  def players(ready: ReadyGame): Vector[PlayerId] =
    ready.game.current.players.map(_.player).filter(isImperial(ready, _))

  /** The player who rules the Empire's sites now: the turn player during an
    * Imperial player's Wake, Act or Rest (Seal of the Empire, CR p. 35),
    * otherwise the Chancellor (Steward of Imperial Domains; rulings R1 and
    * R2). */
  def governor(ready: ReadyGame): Option[PlayerId] =
    val turn = ready.game.current.turn
    val inTurn = turn.phase == Phase.Wake || turn.phase == Phase.Act ||
      turn.phase == Phase.Rest
    if inTurn && isImperial(ready, turn.activePlayer) then
      Some(turn.activePlayer)
    else chancellor(ready)
