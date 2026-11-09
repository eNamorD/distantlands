package oathdigital.gameplay

import oathdigital.model._

/** The Visions a player has revealed, as the victory rules read them
  * (catalog batch 11, slice 6): their own, in reveal order. Slice 7 adds
  * the Visions they claim with False Prophet here, so every victory read
  * sees a claim at once. The slot's own bookkeeping reads
  * `PlayerState.revealedVisions`, because a claim never puts a Vision in
  * the claimant's slot. */
object RevealedVisions:
  def of(ready: ReadyGame, player: PlayerId): Vector[VisionId] =
    ready.game.current.player(player).toVector
      .flatMap(_.revealedVisions.map(_.id))
