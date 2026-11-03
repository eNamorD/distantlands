package oathdigital.gameplay.setup

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

final class FirstGameSetupMaterializer(catalog: ExecutableCatalog):

  def materialize(plan: FirstGameSetupPlan,
      placements: Vector[PawnPlacement],
      adviserChoices: Vector[(PlayerId, DenizenId)]): FirstGameSetupMaterial =
    val placementMap = placements.map(p => p.playerId -> p.siteId).toMap
    val adviserMap = adviserChoices.toMap
    val relicsBySite = assignRelics(plan)
    val edifices = plan.homelandEdifices.toMap
    val purple = plan.empire.fold(Set.empty[SiteId])(empire =>
      empire.sites.take(MaterialBankState.ImperialWarbands -
        3 * empire.imperial.size).map(_.site).toSet)
    val empireSites = plan.empire.fold(Map.empty[SiteId, StoredSite])(
      _.sites.map(stored => stored.site -> stored).toMap)
    val atlasSites = plan.atlasSites.map(stored => stored.site -> stored).toMap
    val map = MapState(
      plan.orderedSites.take(2),
      plan.orderedSites.slice(2, 5),
      plan.orderedSites.slice(5, 8),
      plan.orderedSites.map { siteId =>
        val drawn = relicsBySite.getOrElse(siteId, Vector.empty)
        siteId -> empireSites.get(siteId).map(empireSite(_, purple(siteId)))
          .orElse(atlasSites.get(siteId).map(atlasSite(_, drawn)))
          .getOrElse(firstGameSite(siteId, edifices.get(siteId), drawn))
      }.toMap)
    val usedRelics = relicsBySite.valuesIterator.flatten.toSet
    val players = plan.participants.map { participant =>
      PlayerState(participant.playerId, participant.lineageId,
        placementMap.get(participant.playerId),
        PlayerBoardState(1, 1, 0, 3, SupplyTrack.full),
        adviserMap.get(participant.playerId).toVector.map(id =>
          DenizenState(id, Orientation.FaceDown, Tokens.empty)),
        Vector.empty, None)
    }
    FirstGameSetupMaterial(players, map,
      CardZones(plan.worldDeckOrder, plan.relicOrder.filterNot(usedRelics),
        catalog.edifices.map(e => EdificeId(e.id.value))
          .filterNot(edificesOn(map)), Vector.empty,
        initialDiscards(plan, placementMap, adviserMap)),
      BannersState(
        PeoplesFavorState(PeoplesFavorFace.Mob, None, 1),
        DarkestSecretState(DarkestSecretFace.WanderingFlame, None, 1)),
      // While game start refuses Imperial roles the first player begins
      // round 1; a seated Chancellor would (Empire slice 4).
      GameTracks(1, 0, usurperLimited = true,
        roundStarter = plan.firstPlayer), favorBanks(plan, map),
      temporaryHands = plan.participants.map(p =>
        p.playerId -> handFor(plan, p.playerId)).toMap)

  def handFor(plan: FirstGameSetupPlan, playerId: PlayerId): Vector[DenizenId] =
    val index = plan.participants.indexWhere(_.playerId == playerId)
    plan.denizenOrder.slice(6 + index * 3, 9 + index * 3)

  /** A first game deals every relic slot; a later game deals only the
    * slots an atlas site's stored relics leave open, and none at the
    * Empire's sites (Empire ruling R27). */
  private def assignRelics(plan: FirstGameSetupPlan): Map[SiteId, Vector[RelicId]] =
    val stored = plan.atlasSites.map(site => site.site -> site).toMap
    val empire = plan.empire.fold(Set.empty[SiteId])(_.sites.map(_.site).toSet)
    var offset = 0
    plan.orderedSites.map { site =>
      val slots = catalog.site(site).get.relicSlots
      val count = if empire(site) then 0
        else stored.get(site).fold(slots)(atlas => math.max(0,
          slots - atlas.items.count(_.isInstanceOf[RelicId])))
      val assigned = plan.relicOrder.slice(offset, offset + count)
      offset += count
      site -> assigned
    }.toMap

  private def bandits(site: SiteId): SiteForces =
    val capacity = catalog.site(site).get.capacity
    if capacity == 0 then SiteForces.Empty
    else SiteForces.Occupied(ForceKind.Bandit, capacity)

  private def faceDown(relics: Vector[RelicId]): Vector[RelicState] =
    relics.map(RelicState(_, Orientation.FaceDown, Tokens.empty))

  private def firstGameSite(site: SiteId, edifice: Option[EdificeId],
      drawn: Vector[RelicId]): SiteState =
    SiteState(bandits(site), edifice.toVector.map(id =>
        EdificeState(id, EdificeSide.Ruined, Tokens.empty)),
      faceDown(drawn), catalog.site(site).get.startingResources)

  /** An Empire site: its stored edifice intact, its denizens faceup, its
    * stored relics facedown, and an Imperial warband while the supply
    * lasts (Empire ruling R30). A short site stays empty for game start's
    * bandits. */
  private def empireSite(stored: StoredSite, purple: Boolean): SiteState =
    SiteState(if purple then SiteForces.Occupied(ForceKind.Imperial, 1)
        else SiteForces.Empty,
      storedDenizens(stored, EdificeSide.Intact), faceDown(storedRelics(stored)),
      catalog.site(stored.site).get.startingResources)

  /** An atlas site in a later game keeps its stored cards, its edifice
    * ruined, and fills its open relic slots (Empire ruling R27). */
  private def atlasSite(stored: StoredSite, drawn: Vector[RelicId]): SiteState =
    SiteState(bandits(stored.site), storedDenizens(stored, EdificeSide.Ruined),
      faceDown(storedRelics(stored) ++ drawn),
      catalog.site(stored.site).get.startingResources)

  private def storedDenizens(stored: StoredSite, side: EdificeSide)
      : Vector[SiteDenizenState] = stored.items.collect {
    case id: EdificeId => EdificeState(id, side, Tokens.empty)
    case id: DenizenId => DenizenState(id, Orientation.FaceUp, Tokens.empty)
  }

  private def storedRelics(stored: StoredSite): Vector[RelicId] =
    stored.items.collect { case id: RelicId => id }

  private def edificesOn(map: MapState): Set[EdificeId] =
    map.sites.values.flatMap(_.denizens).collect {
      case EdificeState(id, _, _) => id
    }.toSet

  private def initialDiscards(plan: FirstGameSetupPlan,
      placements: Map[PlayerId, SiteId],
      choices: Map[PlayerId, DenizenId]): Map[Region, Vector[WorldCardId]] =
    val seeded = Map[Region, Vector[WorldCardId]](
      Region.Cradle -> plan.denizenOrder.slice(0, 2),
      Region.Provinces -> plan.denizenOrder.slice(2, 4),
      Region.Hinterland -> plan.denizenOrder.slice(4, 6))
    plan.participants.foldLeft(seeded) { (discards, participant) =>
      (placements.get(participant.playerId), choices.get(participant.playerId)) match
        case (Some(site), Some(choice)) =>
          val rejected = handFor(plan, participant.playerId).filterNot(_ == choice)
          val destination = regionOf(plan, site) match
            case Region.Cradle => Region.Provinces
            case Region.Provinces => Region.Hinterland
            case Region.Hinterland => Region.Cradle
          discards.updated(destination, discards(destination) ++ rejected)
        case _ => discards
    }

  private def regionOf(plan: FirstGameSetupPlan, site: SiteId): Region =
    if plan.orderedSites.take(2).contains(site) then Region.Cradle
    else if plan.orderedSites.slice(2, 5).contains(site) then Region.Provinces
    else Region.Hinterland

  /** Foundation II: 3 favor per suit, 1 more with 5 or more players, and 1
    * more per denizen of that suit on the map, edifices included. */
  private def favorBanks(plan: FirstGameSetupPlan, map: MapState)
      : Map[Suit, Int] =
    val bonus = if plan.participants.size >= 5 then 1 else 0
    val suits = map.sites.values.flatMap(_.denizens).flatMap {
      case EdificeState(id, _, _) => catalog.edifice(id).map(_.suit)
      case DenizenState(id, _, _) => catalog.denizen(id).map(_.suit)
    }.toVector
    Suit.all.map(suit => suit -> (3 + bonus + suits.count(_ == suit))).toMap
