package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.model._
import distantlands.model.OathViolation._

/**
 * Builds the initial `ReadyGame` a `GameStarted` event evolves into
 * (2026-09-21 Chronicle design, slice 2, "Setup from a Chronicle"). The
 * application layer still derives the per-game deal order
 * (`ChronicleFirstGamePlan.dealOrder`, called by
 * `GeneratedFirstGamePlanFactory`); this object turns the Chronicle and
 * the orders into the `FirstGameSetupPlan` and the game state.
 *
 * Validation here is only what Setup itself needs: known and unique ids
 * across every Chronicle field (`ChronicleAccounting`), how a later game
 * starts, with an Empire, with none or as a first or exile-only game
 * (`EmpireSetupRules`), the foundation faces it starts with
 * (`StartingFoundations`) and enough cards to deal. A first game's
 * Homelands each store one edifice of their suit (Empire ruling R26), or
 * the Chronicle carries history and starts an exile-only game.
 * The old plan's suit-count and Vision-packet audit is the generator's own
 * job now (`FirstGameChronicleGenerator.validate`), checked once at
 * generation time rather than again at every setup.
 */
object GameStartRules:
  def evolve(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders): Either[OathViolation, ReadyGame] =
    val deal = SetupDeal.of(chronicle, orders.participants)
    val legacyDeal = LegacyDeal.of(chronicle, orders.participants)
    for
      planned <- buildPlan(catalog, chronicle, orders, deal)
      (plan, mode) = planned
      material = new FirstGameSetupMaterializer(catalog)
        .materialize(plan, deal)
      // An edifice the Chronicle stores at a site stays there: it is not also
      // in the edifice deck. The materializer only knows the edifices at the
      // sites in play.
      storedEdifices = (chronicle.atlasBox ++ chronicle.world).flatMap(_.items)
        .collect { case id: EdificeId => id }.toSet
      commonCards = material.commonCards.copy(edificeDeck =
        material.commonCards.edificeDeck.filterNot(storedEdifices),
        publicLegacies = legacyDeal.publicRow(orders.legacyOrder))
      // A seated lineage the Chronicle lists starts from its record, its
      // legacies with their stored faces; `LaterGameSetup` hands its
      // starting relics to its player. Foundation III's draw joins it
      // dormant (`LegacyDeal`); the player keeps one in Setup.
      recorded = chronicle.lineages.map(lineage => lineage.id -> lineage).toMap
      lineages = plan.participants.map { participant =>
        val record = recorded.getOrElse(participant.lineageId,
          LineageState(participant.lineageId, None, Role.Exile, Vector.empty,
            Vector.empty))
        participant.lineageId -> record.copy(legacies = record.legacies ++
          legacyDeal.drawn(orders.legacyOrder, participant.playerId)
            .map(LegacyState(_, active = false)))
      }.toMap
      // The atlas sites not placed on the map stay in the atlas, in order,
      // with every stored card: the inverse of
      // `ChronicleExtraction.extract`'s `carried`.
      onMap = plan.orderedSites.toSet
      atlas = AtlasState(chronicle.atlasBox.filterNot(stored =>
          onMap(stored.site))
        .map(stored => AtlasEntry.StoredSite(stored.site,
          stored.items.collect { case id: DenizenId =>
            DenizenState(id, Orientation.FaceUp, Tokens.empty) },
          stored.items.collect { case id: RelicId =>
            RelicState(id, Orientation.FaceDown, Tokens.empty) },
          stored.items.collectFirst { case id: EdificeId => id })))
      game = OathGame(
        // The faces `buildPlan` read from the Chronicle and did not refuse.
        CampaignState(atlas, plan.foundations, lineages, chronicle.reliquary,
          chronicle.dispossessed, Map.empty, plan.oathkeeperGoal,
          chronicle.era,
          baseline = chronicle),
        CurrentGameState(material.players, material.map, commonCards,
          material.banners, OathkeeperState(None, TitleSide.Oathkeeper),
          TurnState(orders.firstPlayer, Phase.Setup, Set.empty),
          material.tracks, None, material.temporaryHands))
      // "If you are playing your very first game, we recommend ignoring
      // these powers": only a first game ignores Citizenship powers.
      ready = LaterGameSetup(catalog, ReadyGame.start(game,
        plan.participants.map(p => p.playerId -> p.color).toMap,
        orders.firstPlayer, material.favorBanks,
        ignoreCitizenshipPowers = mode == SetupMode.FirstGame), plan, mode)
      problems = DomainValidation.validate(ready.game)
      _ <- Either.cond(problems.isEmpty, (), InvalidAggregate(problems))
    yield ready

  private def buildPlan(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders, deal: SetupDeal)
      : Either[OathViolation, (FirstGameSetupPlan, SetupMode)] =
    for
      _ <- validateParticipants(orders)
      mode <- EmpireSetupRules.resolve(catalog, chronicle, orders)
      // Setup With No Empire erodes the faces before the foundations are
      // resolved; an exile-only game had no Empire to erode them. Every
      // face of every foundation starts a game.
      faces = mode match
        case _: SetupMode.NoEmpire =>
          StartingFoundations.eroded(StartingFoundations.of(chronicle))
        case _ => StartingFoundations.of(chronicle)
      slots = atlasSlots(mode, faces)
      _ <- Either.cond(chronicle.atlasBox.size >= slots, (),
        UnsupportedChronicle(
          s"at least $slots atlas sites are required, got ${chronicle.atlasBox.size}"))
      // Only a first game's sites are placed here; Imperial Maps fills its
      // slots in Setup's walk.
      atlasSites =
        if mode == SetupMode.FirstGame then chronicle.atlasBox.take(slots)
        else Vector.empty
      // A later game keeps every card stored at its sites (R27). A first
      // game's sites hold only their Homelands' edifices, one of each
      // Homeland's suit (R26): a stored denizen or relic, an edifice at a
      // site that is no Homeland, and a Homeland storing none or another
      // suit's are history (`EmpireSetupRules.hasHistory`).
      _ <- validateIds(catalog, chronicle)
      _ <- Either.cond(
        chronicle.worldDeck.size >= deal.needed, (),
        UnsupportedChronicle("not enough world-deck cards to deal"))
      homelands =
        if mode == SetupMode.FirstGame then homelandEdifices(catalog, atlasSites)
        else Vector.empty
      _ <- Either.cond(chronicle.relicDeck.size >=
          atlasSites.map(emptyRelicSlots(catalog, _)).sum,
        (), UnsupportedChronicle("not enough relics to fill site slots"))
      _ <- validateDeal(catalog, chronicle, orders, deal)
    yield FirstGameSetupPlan(orders.participants, orders.firstPlayer,
      (chronicle.world ++ atlasSites ++ (mode match
        case SetupMode.ExileOnly(sites) => sites
        case _ => Vector.empty)).map(_.site), orders.denizenOrder,
      orders.worldDeckOrder, orders.relicOrder, homelands,
      chronicle.oathkeeperGoal, mode match
        case SetupMode.WithEmpire(empire) => Some(empire)
        case _ => None,
      mode match
        case SetupMode.NoEmpire(sites, _) => sites
        case SetupMode.ExileOnly(sites) => sites
        case _ => Vector.empty,
      startingAdvisers(chronicle, orders), faces) -> mode

  /** The slots filled from the atlas. A first game's eight are placed at
    * game start from the Recent end. With an Empire, Imperial Maps fills
    * the slots past the Empire's sites in Setup's walk, from the end the
    * Chancellor chooses (Foundations design, slice 4), and Wide Horizons
    * fills none. With no Empire the former Empire's sites are all that is
    * placed. An exile-only game places eight as bandit sites at game
    * start, whatever Foundation I's face. */
  private def atlasSlots(mode: SetupMode,
      faces: Map[FoundationNumber, FoundationState]): Int = mode match
    case SetupMode.FirstGame | SetupMode.ExileOnly(_) => MapState.slots
    case SetupMode.WithEmpire(empire)
        if faces(FoundationNumber.I).face == FoundationFace.Normal =>
      MapState.slots - empire.sites.size
    case _ => 0

  /** A site's relic slots its stored relics leave open, never fewer than 0
    * (R27). */
  private def emptyRelicSlots(catalog: ExecutableCatalog, stored: StoredSite)
      : Int =
    math.max(0, relicSlots(catalog, stored.site) -
      stored.items.count(_.isInstanceOf[RelicId]))

  private def startingAdvisers(chronicle: Chronicle, orders: SetupOrders)
      : Map[PlayerId, Vector[AdviserState]] =
    val players = orders.participants.map(p => p.lineageId -> p.playerId).toMap
    chronicle.lineages.flatMap(lineage => players.get(lineage.id)
      .filter(_ => lineage.startingAdvisers.nonEmpty)
      .map(_ -> lineage.startingAdvisers)).toMap

  /** The deal holds the Chronicle's decks and the derived legacy deck, each
    * card once, in any order; the world deck holds the denizens Foundation
    * IV leaves of `denizenOrder`, in order unless a short deck was mixed
    * with the Visions (CR p. 12). Teeming World leaves none. */
  private def validateDeal(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders, deal: SetupDeal): Either[OathViolation, Unit] =
    val rest = deal.deck(orders.denizenOrder)
    val deck = orders.worldDeckOrder.collect { case id: DenizenId => id }
    for
      _ <- Either.cond(orders.denizenOrder.sortBy(_.value) ==
          chronicle.worldDeck.sortBy(_.value), (),
        UnsupportedChronicle("the deal does not hold the Chronicle's world deck"))
      _ <- Either.cond(orders.relicOrder.sortBy(_.value) ==
          chronicle.relicDeck.sortBy(_.value), (),
        UnsupportedChronicle("the deal does not hold the Chronicle's relic deck"))
      _ <- Either.cond(orders.legacyOrder.sortBy(_.value) ==
          LegacyDeal.deck(catalog, chronicle).sortBy(_.value), (),
        UnsupportedChronicle("the deal does not hold the legacy deck"))
      _ <- Either.cond(deck == rest || (rest.size < 25 &&
          deck.sortBy(_.value) == rest.sortBy(_.value)), (),
        UnsupportedChronicle("the world deck is not the rest of the deal"))
    yield ()

  private def duplicate[A](values: Vector[A]): Option[A] =
    val seen = scala.collection.mutable.HashSet.empty[A]
    values.find(value => !seen.add(value))

  private def validateIds(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Either[OathViolation, Unit] =
    ChronicleAccounting.knownAndUnique(catalog, chronicle)
      .left.map(UnsupportedChronicle(_))

  private def validateParticipants(orders: SetupOrders)
      : Either[OathViolation, Unit] =
    val playerIds = orders.participants.map(_.playerId)
    val lineageIds = orders.participants.map(_.lineageId)
    val colors = orders.participants.map(_.color)
    if orders.participants.isEmpty then Left(ParticipantsEmpty)
    else if duplicate(playerIds).nonEmpty then
      Left(DuplicatePlayer(duplicate(playerIds).get))
    else if duplicate(lineageIds).nonEmpty then
      Left(DuplicateLineage(duplicate(lineageIds).get))
    else if duplicate(colors).nonEmpty then
      Left(DuplicateColor(duplicate(colors).get))
    else if !playerIds.contains(orders.firstPlayer) then
      Left(UnknownFirstPlayer(orders.firstPlayer))
    else Right(())

  /** Each Homeland in play with its stored edifice. A first game's
    * Homelands each store one of their suit, or the Chronicle carries
    * history (`EmpireSetupRules.hasHistory`). */
  private def homelandEdifices(catalog: ExecutableCatalog,
      inPlay: Vector[StoredSite]): Vector[(SiteId, EdificeId)] =
    inPlay.filter(stored => catalog.site(stored.site).get.homeland.nonEmpty)
      .flatMap(stored => stored.items.collectFirst { case id: EdificeId =>
        stored.site -> id })

  private def relicSlots(catalog: ExecutableCatalog, site: SiteId): Int =
    catalog.site(site).get.relicSlots
