package distantlands.gameplay.chronicle

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** Who meets a legacy's goal in Resolve Legacies (CR "Task 1: The Sun";
  * Legacies design, slice 3). Only seated players compete, and bandits
  * never do: only sites and cards that players rule count. Every Imperial
  * rules each Empire site and its cards. For a counted goal only the
  * highest positive score meets it (ruling 6); a tie between Imperials
  * alone is friendly, and every tied Imperial meets it; a tie that
  * includes any Exile is unfriendly, and nobody meets it (Chronicle ruling
  * 4). */
object LegacyGoals:
  def meeting(ready: ReadyGame, catalog: ExecutableCatalog, legacy: Legacy)
      : Set[PlayerId] = legacy.goal match
    case LegacyGoal.PeoplesFavor =>
      ready.game.current.banners.peoplesFavor.holder.toSet
    case LegacyGoal.DarkestSecret =>
      ready.game.current.banners.darkestSecret.holder.toSet
    case LegacyGoal.GrandScepter => scepter(ready)
    case goal => leaders(ready, ChronicleTasks.seated(ready).map(player =>
      player -> score(ready, catalog, goal, legacy.suit, player)))

  /** A seated player's score toward a counted goal: the sites their side
    * rules, the cards of `suit` there and among their own advisers (R23),
    * or their best held relic's printed value. A goal held by a single
    * holder scores 0 here. */
  def score(ready: ReadyGame, catalog: ExecutableCatalog, goal: LegacyGoal,
      suit: Suit, player: PlayerId): Int = goal match
    case LegacyGoal.MostSites => ruled(ready, player).size
    case LegacyGoal.MostOfSuit =>
      val current = ready.game.current
      val atSites = ruled(ready, player).flatMap(site =>
        current.map.sites.get(site).toVector.flatMap(_.denizens)).count {
          case DenizenState(id, _, _) => catalog.denizen(id).exists(_.suit == suit)
          case EdificeState(id, _, _) => catalog.edifice(id).exists(_.suit == suit)
        }
      val advised = current.player(player).toVector.flatMap(_.advisers).count {
        case DenizenState(id, _, _) => catalog.denizen(id).exists(_.suit == suit)
        case _: VisionState => false
      }
      atSites + advised
    case LegacyGoal.HighestRelic =>
      ready.game.current.player(player).toVector.flatMap(_.relics)
        .map(relic => if relic.id == TheGrandScepterCard.id then
          TheGrandScepterCard.value
          else catalog.relic(relic.id).fold(0)(_.value)).maxOption.getOrElse(0)
    case _ => 0

  /** The seated players in the game's turn order: seat order from the
    * player who started the last round (implementation ruling R21). */
  def turnOrder(ready: ReadyGame): Vector[PlayerId] =
    val seats = ChronicleTasks.seated(ready)
    val start = seats.indexOf(ready.game.current.tracks.roundStarter)
    seats.drop(start) ++ seats.take(start)

  /** The sites in play `player`'s side rules (Chronicle ruling 11). */
  private def ruled(ready: ReadyGame, player: PlayerId): Vector[SiteId] =
    ready.game.current.map.inPlay.filter(ChronicleTasks.rules(ready, player, _))

  /** The highest positive score's players, unless an Exile is among
    * several. */
  private def leaders(ready: ReadyGame, scores: Vector[(PlayerId, Int)])
      : Set[PlayerId] =
    val best = scores.map(_._2).maxOption.getOrElse(0)
    val tied = scores.collect {
      case (player, score) if best > 0 && score == best => player }
    if tied.size > 1 && tied.exists(!Imperials.isImperial(ready, _)) then
      Set.empty
    else tied.toSet

  /** Its holder; the winner when no Grand Scepter is in play (ruling 7);
    * nobody when it lies at a site (R22). */
  private def scepter(ready: ReadyGame): Set[PlayerId] =
    val current = ready.game.current
    val id = TheGrandScepterCard.id
    current.players.find(_.relics.exists(_.id == id)) match
      case Some(holder) => Set(holder.player)
      case None if current.map.sites.values.exists(_.relics.exists(_.id == id)) =>
        Set.empty
      case None => current.result.map(_.winner).toSet
