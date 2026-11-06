package oathdigital.gameplay

import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.model._
import oathdigital.model.OathEvent.WarExhaustionResolved

/** The Empire's round-end endings (CR p. 19; Empire phase design, "Slice
  * 2"): Stable Regime on the end die at the end of rounds 5 to 7, War
  * Exhaustion's four steps at the end of round 8, and the Successor, who
  * takes any win of the Chancellor's. A game has an Empire while a
  * Chancellor is seated. */
object EmpireEndings:
  def hasEmpire(ready: ReadyGame): Boolean =
    Imperials.chancellor(ready).nonEmpty

  /** The lowest end-die face that gives the Chancellor the win at the end
    * of `round`. */
  def threshold(round: Int): Option[Int] = round match
    case 5 => Some(6)
    case 6 => Some(5)
    case 7 => Some(3)
    case _ => None

  /** Whether this round's end rolls the end die: an Empire, an Imperial
    * Oathkeeper, and a round from 5 to 7. */
  def rollsEndDie(ready: ReadyGame): Boolean =
    hasEmpire(ready) && imperialOathkeeper(ready) &&
      threshold(ready.game.current.tracks.round).nonEmpty

  /** The win `face` gives at the end of the current round, or none. */
  def stableRegime(ready: ReadyGame, face: Int): Option[GameResult] =
    threshold(ready.game.current.tracks.round).filter(face >= _).flatMap(_ =>
      chancellorWins(ready, VictoryKind.StableRegime))

  /** War Exhaustion with an Empire, in printed order: an Imperial
    * Oathkeeper gives the Chancellor the win; then an Exile Usurper wins;
    * then an Exile meeting their Vision; otherwise the Chancellor. */
  def warExhaustion(ready: ReadyGame): Option[WarExhaustionResolved] =
    def resolved(result: GameResult) =
      WarExhaustionResolved(result.winner, result.kind, None, Vector.empty)
    if imperialOathkeeper(ready) then
      chancellorWins(ready, VictoryKind.StableRegime).map(resolved)
    else ready.game.current.title match
      case OathkeeperState(Some(player), TitleSide.Usurper) =>
        Some(WarExhaustionResolved(player, VictoryKind.Usurper, None,
          Vector.empty))
      case _ => StateBasedEvaluation.visionaryWin(ready).orElse(
        chancellorWins(ready, VictoryKind.ChancellorExhaustion).map(resolved))

  /** The Chancellor's win of `kind`, or the Successor's in its place. */
  def chancellorWins(ready: ReadyGame, kind: VictoryKind)
      : Option[GameResult] =
    Imperials.chancellor(ready).map(chancellor => successor(ready).fold(
      GameResult(chancellor, kind))(GameResult(_, VictoryKind.Successor)))

  /** The Citizen who meets the Successor goal under the current Oathkeeper
    * goal; at most one can. Supremacy's goal counts every relic held, the
    * Grand Scepter included (ruling R17). */
  def successor(ready: ReadyGame): Option[PlayerId] =
    val current = ready.game.current
    val imperials = Imperials.players(ready)
    val citizens =
      imperials.filter(Imperials.role(ready, _).contains(Role.Citizen))
    def relics(player: PlayerId): Int =
      current.player(player).fold(0)(_.relics.size)
    def holdsScepter(player: PlayerId): Boolean =
      current.players.exists(state => state.player == player &&
        state.relics.exists(_.id == TheGrandScepterCard.id))
    ready.game.campaign.oathkeeperGoal match
      case OathkeeperGoal.Supremacy => citizens.find(citizen =>
        imperials.filterNot(_ == citizen).forall(relics(citizen) > relics(_)))
      case OathkeeperGoal.ThePeople =>
        current.banners.darkestSecret.holder.filter(citizens.contains)
      case OathkeeperGoal.Protection =>
        current.banners.peoplesFavor.holder.filter(citizens.contains)
      case OathkeeperGoal.Devotion => citizens.find(holdsScepter)

  private def imperialOathkeeper(ready: ReadyGame): Boolean =
    ready.game.current.title.holder.exists(Imperials.isImperial(ready, _))
