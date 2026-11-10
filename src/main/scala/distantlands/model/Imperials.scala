package distantlands.model

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

  /** Whether `player` and `other` are enemies (rules glossary, "Enemy"):
    * two different players are, unless both are Imperial. */
  def enemies(ready: ReadyGame, player: PlayerId, other: PlayerId)
      : Boolean =
    player != other && !(isImperial(ready, player) &&
      isImperial(ready, other))

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
    val active = ready.game.current.turn.activePlayer
    if inOwnTurn(ready) && isImperial(ready, active) then Some(active)
    else chancellor(ready)

  /** Whether the active player is in their own Wake, Act or Rest, the
    * part of a turn that is theirs (Seal of the Empire, CR p. 35). */
  def inOwnTurn(ready: ReadyGame): Boolean =
    val phase = ready.game.current.turn.phase
    phase == Phase.Wake || phase == Phase.Act || phase == Phase.Rest
