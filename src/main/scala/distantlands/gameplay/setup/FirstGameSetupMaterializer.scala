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
    // Each board's starting warbands: 3, or what Circle of Swords gives
    // (`StartingLegacies`). The Empire's sites take purple warbands from
    // what the Imperial boards leave of the 24 (R3).
    val boards = plan.participants.map(p => p.playerId ->
      plan.legacies.boardWarbands.getOrElse(p.playerId, 3)).toMap
    val purpleBoards = plan.empire.fold(0)(_.imperial.toVector
      .map(boards).sum)
    // Under Iron Hand the Empire's sites take its holder's warbands.
    val purple = plan.empire.filter(_ => plan.legacies.ironHand.isEmpty)
      .fold(Set.empty[SiteId])(empire =>
        empire.sites.take(math.max(0,
          MaterialBankState.ImperialWarbands - purpleBoards))
          .map(_.site).toSet)
    val held = heldForces(plan, boards)
    val empireSites = plan.empire.fold(Map.empty[SiteId, StoredSite])(
      _.sites.map(stored => stored.site -> stored).toMap)
    val atlasSites = plan.atlasSites.map(stored => stored.site -> stored).toMap
    val rivalSites = plan.legacies.rivalSites
      .map(stored => stored.site -> stored).toMap
    val map = MapState.inSlots(plan.orderedSites.map { siteId =>
      val drawn = relicsBySite.getOrElse(siteId, Vector.empty)
      siteId -> empireSites.get(siteId).map(stored => empireSite(stored,
          held.getOrElse(siteId, empireForces(purple(siteId)))))
        .orElse(rivalSites.get(siteId).map(stored =>
          empireSite(stored, held(siteId))))
        .orElse(atlasSites.get(siteId).map(stored =>
          atlasSite(stored, drawn, held.get(siteId))))
        .getOrElse(firstGameSite(siteId, edifices.get(siteId), drawn))
    })
    val usedRelics = relicsBySite.valuesIterator.flatten.toSet
    val players = plan.participants.map { participant =>
      PlayerState(participant.playerId, participant.lineageId, None,
        PlayerBoardState(startingFavor(plan), 1, 0,
          boards(participant.playerId), SupplyTrack.full),
        deal.kept(plan.denizenOrder, participant.playerId).map(id =>
          DenizenState(id, Orientation.FaceDown, Tokens.empty)),
        Vector.empty, Vector.empty)
    }
    FirstGameSetupMaterial(players, map,
      // An Oaths of Loyalty holder's Teeming World draws go to the bottom
      // of the world deck, below the Visions (Legacy Chronicle powers R24,
      // re-ruled), so every card of the deal is somewhere once.
      CardZones(plan.worldDeckOrder ++ plan.participants.flatMap(p =>
          deal.buried(plan.denizenOrder, p.playerId)),
        plan.relicOrder.filterNot(usedRelics),
        catalog.edifices.map(e => EdificeId(e.id.value))
          .filterNot(edificesOn(map)),
        deal.discards(plan.denizenOrder)),
      BannersState(
        PeoplesFavorState(peoplesFavorFace(plan), None, 1),
        DarkestSecretState(darkestSecretFace(plan), None, 1, festival = None)),
      // The first player begins round 1: in a later game, the Chancellor.
      GameTracks(1, 0, usurperLimited = true,
        roundStarter = plan.firstPlayer), favorBanks(plan, map),
      temporaryHands = plan.participants.map(p => p.playerId ->
        deal.hand(plan.denizenOrder, p.playerId)).toMap)

  /** A first game deals every relic slot. With no Empire, the former
    * Empire's sites take the slots their stored relics leave open, and an
    * Empire's sites and Rival's take none (Empire ruling R27; R5); Imperial Maps' sites
    * are dealt theirs as Setup's walk places them (`PlaceAtlasSite`). Slots
    * are dealt Cradlemost first, and a relic deck that runs out leaves the
    * last ones empty. */
  private def assignRelics(plan: FirstGameSetupPlan): Map[SiteId, Vector[RelicId]] =
    val stored = plan.atlasSites.map(site => site.site -> site).toMap
    // Rival's sites are placed by the Empire rules, which fill no empty
    // relic slot (R5).
    val noDraw = plan.empire.fold(Set.empty[SiteId])(_.sites.map(_.site)
      .toSet) ++ plan.legacies.rivalSites.map(_.site)
    var offset = 0
    plan.orderedSites.map { site =>
      val slots = catalog.site(site).get.relicSlots
      val count = if noDraw(site) then 0
        else stored.get(site).fold(slots)(atlas => SiteState.openRelicSlots(
          slots, atlas.items.count(_.isInstanceOf[RelicId])))
      val assigned = plan.relicOrder.slice(offset, offset + count)
      offset += count
      site -> assigned
    }.toMap

  /** The forces at each site a legacy gives an Exile holder's warbands:
    * under Iron Hand the Empire's sites, or the former Empire's with no
    * Empire, then Rival's sites, which are built by the Empire rules
    * (ruling 8). A lineage's 14 warbands go to its board first, then to its
    * sites Cradlemost first; a site its supply misses takes bandits (R2). */
  private def heldForces(plan: FirstGameSetupPlan,
      boards: Map[PlayerId, Int]): Map[SiteId, SiteForces] =
    val lineageBoards = plan.participants.map(p => p.lineageId ->
      boards(p.playerId)).toMap
    val formerEmpire = plan.empire.fold(plan.atlasSites)(_.sites)
    val sites = plan.legacies.ironHand.toVector.flatMap(lineage =>
      formerEmpire.map(lineage -> _.site)) ++
      plan.legacies.rival.toVector.flatMap(lineage =>
        plan.legacies.rivalSites.map(lineage -> _.site))
    sites.groupBy(_._1).flatMap { (lineage, held) =>
      val reach = MaterialBankState.ExileWarbandsPerLineage -
        lineageBoards.getOrElse(lineage, 3)
      held.zipWithIndex.map { case ((_, site), index) =>
        site -> (if index < reach then
          SiteForces.Occupied(ForceKind.Exile(lineage), 1)
        else bandits(site))
      }
    }

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

  /** An Empire site's forces: an Imperial warband while the supply the
    * Imperial boards leave lasts (Empire ruling R30). A short site stays
    * empty for game start's bandits. */
  private def empireForces(purple: Boolean): SiteForces =
    if purple then SiteForces.Occupied(ForceKind.Imperial, 1)
    else SiteForces.Empty

  /** An Empire site: its stored edifice intact, its denizens faceup, its
    * stored relics facedown, with `forces`. */
  private def empireSite(stored: StoredSite, forces: SiteForces): SiteState =
    SiteState(forces,
      storedDenizens(stored, EdificeSide.Intact), faceDown(storedRelics(stored)),
      catalog.site(stored.site).get.startingResources)

  /** A former Empire's site with no Empire keeps its stored cards, its
    * edifice ruined, and fills its open relic slots, as an atlas site
    * Imperial Maps places does (Empire ruling R27). Under Iron Hand it
    * takes `held` in place of its bandits. */
  private def atlasSite(stored: StoredSite, drawn: Vector[RelicId],
      held: Option[SiteForces]): SiteState =
    val printed = catalog.site(stored.site).get
    val site = SiteState.banditSite(printed.capacity,
      printed.startingResources, storedDenizens(stored, EdificeSide.Ruined),
      storedRelics(stored) ++ drawn)
    held.fold(site)(forces => site.copy(forces = forces))

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

  /** Foundation VI's face, on the Darkest Secret: Wandering Flame sets the
    * Wandering Flame, and Festival flips it to the Festival, its marker
    * beside the map. Both faces put 1 secret on it from the shared bank,
    * and `LaterGameSetup` gives it to the Chancellor when the Oathkeeper
    * goal is the Darkest Secret. */
  private def darkestSecretFace(plan: FirstGameSetupPlan): DarkestSecretFace =
    if plan.altered(FoundationNumber.VI) then DarkestSecretFace.Festival
    else DarkestSecretFace.WanderingFlame

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
      .flatMap(card => catalog.suitOf(card.id)).toVector
    Suit.all.map(suit => suit -> (printed + suits.count(_ == suit))).toMap
