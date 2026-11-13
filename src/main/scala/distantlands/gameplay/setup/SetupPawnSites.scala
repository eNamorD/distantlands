package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

object SetupPawnSites:
  /** Where a player may place their pawn in Setup (CR p. 13, Empire ruling
    * R25). In a first game, any site. In a later game an Imperial player
    * places at any Empire site, and an Exile at the Hintermost Empire site
    * or a site the bandits rule, which after Setup With No Empire and in an
    * exile-only game is every site that holds bandits. A site with no
    * warbands is ruled by nobody. A player with no such site may place
    * anywhere, so Setup never stalls. */
  def legal(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId)
      : Vector[SiteId] =
    val map = ready.game.current.map
    def held(kind: ForceKind)(site: SiteId) = map.sites.get(site)
      .exists(_.forces match
        case SiteForces.Occupied(`kind`, _) => true
        case _ => false)
    val empire = map.hinterward.filter(held(ForceKind.Imperial))
    // An exile-only game places its pawns as a later game's Exiles do.
    val firstGame = EmpireSetupRules.isFirstGame(catalog,
      ready.game.campaign.baseline)
    val open =
      if firstGame then map.inPlay
      else if Imperials.isImperial(ready, player) then empire
      else map.hinterward.filter(site =>
        empire.lastOption.contains(site) || held(ForceKind.Bandit)(site))
    if open.isEmpty then map.inPlay else open
