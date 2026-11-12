package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

final class FirstGameSetupMaterializer(catalog: ExecutableCatalog):

  /** The table `plan` sets up, with Foundation IV's cards dealt as `deal`
    * says. No pawn is placed and no adviser chosen: Setup's walk does
    * both. */
  def materialize(plan: FirstGameSetupPlan, deal: SetupDeal)
      : FirstGameSetupMaterial =
    val relicsBySite = assignRelics(plan)
    val edifices = plan.homelandEdifices.toMap
    val purple = plan.empire.fold(Set.empty[SiteId])(empire =>
      empire.sites.take(MaterialBankState.ImperialWarbands -
        3 * empire.imperial.size).map(_.site).toSet)
    val empireSites = plan.empire.fold(Map.empty[SiteId, StoredSite])(
      _.sites.map(stored => stored.site -> stored).toMap)
    val atlasSites = plan.atlasSites.map(stored => stored.site -> stored).toMap
    val map = MapState.inSlots(plan.orderedSites.map { siteId =>
      val drawn = relicsBySite.getOrElse(siteId, Vector.empty)
      siteId -> empireSites.get(siteId).map(empireSite(_, purple(siteId)))
        .orElse(atlasSites.get(siteId).map(atlasSite(_, drawn)))
        .getOrElse(firstGameSite(siteId, edifices.get(siteId), drawn))
    })
    val usedRelics = relicsBySite.valuesIterator.flatten.toSet
    val players = plan.participants.map { participant =>
      PlayerState(participant.playerId, participant.lineageId, None,
        PlayerBoardState(startingFavor(plan), 1, 0, 3, SupplyTrack.full),
        deal.kept(plan.denizenOrder, participant.playerId).map(id =>
          DenizenState(id, Orientation.FaceDown, Tokens.empty)),
        Vector.empty, Vector.empty)
    }
    FirstGameSetupMaterial(players, map,
      CardZones(plan.worldDeckOrder, plan.relicOrder.filterNot(usedRelics),
        catalog.edifices.map(e => EdificeId(e.id.value))
          .filterNot(edificesOn(map)), Vector.empty,
        deal.discards(plan.denizenOrder)),
      BannersState(
        PeoplesFavorState(peoplesFavorFace(plan), None, 1),
        DarkestSecretState(DarkestSecretFace.WanderingFlame, None, 1)),
      // The first player begins round 1: in a later game, the Chancellor.
      GameTracks(1, 0, usurperLimited = true,
        roundStarter = plan.firstPlayer), favorBanks(plan, map),
      temporaryHands = plan.participants.map(p => p.playerId ->
        deal.hand(plan.denizenOrder, p.playerId)).toMap)

  /** A first game deals every relic slot. With no Empire, the former
    * Empire's sites take the slots their stored relics leave open, and an
    * Empire's sites take none (Empire ruling R27); Imperial Maps' sites
    * are dealt theirs as Setup's walk places them (`PlaceAtlasSite`). Slots
    * are dealt Cradlemost first, and a relic deck that runs out leaves the
    * last ones empty. */
  private def assignRelics(plan: FirstGameSetupPlan): Map[SiteId, Vector[RelicId]] =
    val stored = plan.atlasSites.map(site => site.site -> site).toMap
    val empire = plan.empire.fold(Set.empty[SiteId])(_.sites.map(_.site).toSet)
    var offset = 0
    plan.orderedSites.map { site =>
      val slots = catalog.site(site).get.relicSlots
      val count = if empire(site) then 0
        else stored.get(site).fold(slots)(atlas => SiteState.openRelicSlots(
          slots, atlas.items.count(_.isInstanceOf[RelicId])))
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

  /** A former Empire's site with no Empire keeps its stored cards, its
    * edifice ruined, and fills its open relic slots, as an atlas site
    * Imperial Maps places does (Empire ruling R27). */
  private def atlasSite(stored: StoredSite, drawn: Vector[RelicId]): SiteState =
    val printed = catalog.site(stored.site).get
    SiteState.banditSite(printed.capacity, printed.startingResources,
      storedDenizens(stored, EdificeSide.Ruined), storedRelics(stored) ++ drawn)

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

  /** Foundation V's face, on the People's Favor: Mob's Favor sets the
    * Mob, and Grand Council flips it to the Grand Council. Both faces put
    * 1 favor on it from the shared bank, and `LaterGameSetup` gives it to
    * the Chancellor when the Oathkeeper goal is the People's Favor. */
  private def peoplesFavorFace(plan: FirstGameSetupPlan): PeoplesFavorFace =
    if plan.altered(FoundationNumber.V) then PeoplesFavorFace.GrandCouncil
    else PeoplesFavorFace.Mob

  /** The favor on each board: 1, and under Movers and Shakers (altered
    * Foundation II) 5 more from the shared bank, which has no count. */
  private def startingFavor(plan: FirstGameSetupPlan): Int =
    if plan.altered(FoundationNumber.II) then 6 else 1

  /** Foundation II. Powerful Tribes: 3 favor per suit, 1 more with 5 or
    * more players, and 1 more per denizen of that suit on the map,
    * edifices included. Movers and Shakers prints neither the 3 nor the
    * fifth-player favor: each bank holds 1 per denizen of its suit on the
    * map. A site Imperial Maps places in Setup's walk adds its own
    * (`AtlasPlacement.suitFavor`). */
  private def favorBanks(plan: FirstGameSetupPlan, map: MapState)
      : Map[Suit, Int] =
    val printed =
      if plan.altered(FoundationNumber.II) then 0
      else if plan.participants.size >= 5 then 4
      else 3
    val suits = map.sites.values.flatMap(_.denizens)
      .flatMap(AtlasPlacement.suitOf(catalog, _)).toVector
    Suit.all.map(suit => suit -> (printed + suits.count(_ == suit))).toMap
