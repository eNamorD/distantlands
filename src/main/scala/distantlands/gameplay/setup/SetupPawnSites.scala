package distantlands.gameplay.setup

import distantlands.model._

object SetupPawnSites:
  /** Where a player may place their pawn in Setup (CR p. 13, Empire ruling
    * R25). With no Empire, any site; an Imperial player, any Empire site; an
    * Exile, the Hintermost Empire site or a site the bandits rule. A site
    * with no warbands is ruled by nobody. */
  def legal(ready: ReadyGame, player: PlayerId): Vector[SiteId] =
    val map = ready.game.current.map
    def held(kind: ForceKind)(site: SiteId) = map.sites.get(site)
      .exists(_.forces match
        case SiteForces.Occupied(`kind`, _) => true
        case _ => false)
    val empire = map.hinterward.filter(held(ForceKind.Imperial))
    if empire.isEmpty then map.inPlay
    else if Imperials.isImperial(ready, player) then empire
    else map.hinterward.filter(site =>
      empire.lastOption.contains(site) || held(ForceKind.Bandit)(site))
