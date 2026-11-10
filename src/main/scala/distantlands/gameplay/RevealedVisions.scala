package distantlands.gameplay

import distantlands.model._

/** The Visions a player has revealed, as the victory rules read them: their
  * own, in reveal order, then every other player's revealed Vision that
  * carries a warband of their lineage (False Prophet, catalog batch 11,
  * slice 7), in seat order of the owners. The kind is the lineage's, not
  * the role's, so a claimant who becomes a Citizen keeps the claim; the
  * victory reads' Exile check stops them winning with it (FAQ). The slot's
  * own bookkeeping reads `PlayerState.revealedVisions`, because a claim
  * never puts a Vision in the claimant's slot. */
object RevealedVisions:
  def of(ready: ReadyGame, player: PlayerId): Vector[VisionId] =
    val current = ready.game.current
    current.player(player).toVector.flatMap { state =>
      val own = state.revealedVisions.map(_.id)
      val claimed = current.players.filterNot(_.player == player)
        .flatMap(_.revealedVisions.map(_.id))
        .filter(vision => holds(ready, state, vision))
      own ++ claimed
    }

  /** The players with a warband of their lineage on `vision`, in seat
    * order. The card need not be revealed: a discarded claim is read too. */
  def claimants(ready: ReadyGame, vision: VisionId): Vector[PlayerId] =
    ready.game.current.players.filter(holds(ready, _, vision)).map(_.player)

  private def holds(ready: ReadyGame, state: PlayerState, vision: VisionId)
      : Boolean =
    ready.game.current.warbandsOn(vision, ForceKind.Exile(state.lineage)) > 0
