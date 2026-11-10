package oathdigital.gameplay

import oathdigital.gameplay.actions.VisionRules
import oathdigital.model._

/** CR pp. 16-17, 19: the one authoritative true-Vision qualification rule. */
object VisionVictoryEligibility:
  /** `playerId`'s revealed Visions whose goal they meet, in reveal order.
    * Several can qualify at once only through Desecrated Ground. */
  def qualifying(playerId: PlayerId, ready: ReadyGame): Vector[VisionId] =
    val current = ready.game.current
    if current.tracks.visionsDrawn < 3 then Vector.empty
    else RevealedVisions.of(ready, playerId).filter(vision =>
      VisionRules.trueGoal(vision).exists:
        case OathkeeperGoal.Supremacy => uniquePositiveLeader(playerId,
          SupremacySites.counts(ready))
        case OathkeeperGoal.Protection => uniquePositiveLeader(playerId,
          current.players.map(player => player.player -> player.relics.size))
        case OathkeeperGoal.ThePeople =>
          current.banners.peoplesFavor.holder.contains(playerId)
        case OathkeeperGoal.Devotion =>
          current.banners.darkestSecret.holder.contains(playerId))

  private def uniquePositiveLeader(playerId: PlayerId,
      counts: Vector[(PlayerId, Int)]): Boolean =
    val maximum = counts.map(_._2).maxOption.getOrElse(0)
    maximum > 0 && counts.count(_._2 == maximum) == 1 &&
      counts.contains(playerId -> maximum)
