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
 * across every Chronicle field (`ChronicleAccounting`), a later game's
 * Empire (`EmpireSetupRules`), enough cards to deal, and, in a first game,
 * one edifice per Homeland in play (Empire ruling R26). The old
 * plan's suit-count and Vision-packet audit is the generator's own job now
 * (`FirstGameChronicleGenerator.validate`), checked once at generation
 * time rather than again at every setup.
 */
object GameStartRules:
  def evolve(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders): Either[OathViolation, ReadyGame] =
    for
      plan <- buildPlan(catalog, chronicle, orders)
      material = new FirstGameSetupMaterializer(catalog)
        .materialize(plan, Vector.empty, Vector.empty)
      // An edifice the Chronicle stores at a site stays there: it is not also
      // in the edifice deck. The materializer only knows the edifices at the
      // 8 sites in play.
      storedEdifices = (chronicle.atlasBox ++ chronicle.world).flatMap(_.items)
        .collect { case id: EdificeId => id }.toSet
      commonCards = material.commonCards.copy(edificeDeck =
        material.commonCards.edificeDeck.filterNot(storedEdifices))
      // A seated lineage the Chronicle lists starts from its record;
      // `buildPlan` refuses what that record needs Legacies for.
      recorded = chronicle.lineages.map(lineage => lineage.id -> lineage).toMap
      lineages = plan.participants.map(participant =>
        participant.lineageId -> recorded.getOrElse(participant.lineageId,
          LineageState(participant.lineageId, None, Role.Exile, Vector.empty,
            Vector.empty))
      ).toMap
      foundations = FoundationNumber.all.map(number =>
        number -> FoundationState(FoundationFace.Normal, Set.empty)).toMap
      // The atlas past the sites in play keeps every stored card, the
      // inverse of `ChronicleExtraction.extract`'s `carried`.
      atlas = AtlasState(chronicle.atlasBox.drop(8 - chronicle.world.size)
        .map(stored => AtlasEntry.StoredSite(stored.site,
          stored.items.collect { case id: DenizenId =>
            DenizenState(id, Orientation.FaceUp, Tokens.empty) },
          stored.items.collect { case id: RelicId =>
            RelicState(id, Orientation.FaceDown, Tokens.empty) },
          stored.items.collectFirst { case id: EdificeId => id })))
      game = OathGame(
        CampaignState(atlas, foundations, lineages, chronicle.reliquary,
          chronicle.dispossessed, Map.empty, plan.oathkeeperGoal,
          chronicle.era,
          baseline = chronicle),
        CurrentGameState(material.players, material.map, commonCards,
          material.banners, OathkeeperState(None, TitleSide.Oathkeeper),
          TurnState(orders.firstPlayer, Phase.Setup, Set.empty),
          material.tracks, None, material.temporaryHands))
      ready = LaterGameSetup(catalog, ReadyGame.start(game,
        plan.participants.map(p => p.playerId -> p.color).toMap,
        orders.firstPlayer, material.favorBanks), plan)
      problems = DomainValidation.validate(ready.game)
      _ <- Either.cond(problems.isEmpty, (), InvalidAggregate(problems))
    yield ready

  private def buildPlan(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders): Either[OathViolation, FirstGameSetupPlan] =
    val slots = 8 - chronicle.world.size
    val drawers = SetupDeal.drawers(chronicle, orders.participants)
    for
      _ <- Either.cond(chronicle.lineages.forall(_.startingRelics.isEmpty), (),
        UnsupportedChronicle("starting relics require Legacies"))
      seated = orders.participants.map(_.lineageId).toSet
      records = chronicle.lineages.filter(lineage => seated(lineage.id))
      _ <- Either.cond(records.forall(_.legacies.isEmpty), (),
        UnsupportedChronicle("legacies require Legacies"))
      _ <- validateParticipants(orders)
      empire <- EmpireSetupRules.resolve(chronicle, orders)
      _ <- Either.cond(chronicle.atlasBox.size >= slots, (),
        UnsupportedChronicle(
          s"at least $slots atlas sites are required, got ${chronicle.atlasBox.size}"))
      atlasSites = chronicle.atlasBox.take(slots)
      // A later game keeps every card stored at its sites (R27); a first
      // game's sites hold only their Homeland edifices (R26).
      _ <- Either.cond(empire.nonEmpty ||
        chronicle.atlasBox.forall(_.items.forall(_.isInstanceOf[EdificeId])), (),
        UnsupportedChronicle(
          "stored denizens or relics on an atlas site are not supported yet"))
      _ <- validateIds(catalog, chronicle)
      _ <- Either.cond(
        chronicle.worldDeck.size >= SetupDeal.dealt(drawers), (),
        UnsupportedChronicle("not enough world-deck cards to deal"))
      homelands <- if empire.isEmpty then homelandEdifices(catalog, atlasSites)
        else Right(Vector.empty)
      _ <- Either.cond(chronicle.relicDeck.size >=
          atlasSites.map(emptyRelicSlots(catalog, _)).sum,
        (), UnsupportedChronicle("not enough relics to fill site slots"))
      _ <- validateDeal(chronicle, orders, drawers)
    yield FirstGameSetupPlan(orders.participants, orders.firstPlayer,
      (chronicle.world ++ atlasSites).map(_.site), orders.denizenOrder,
      orders.worldDeckOrder, orders.relicOrder, homelands,
      chronicle.oathkeeperGoal, empire, if empire.isEmpty then Vector.empty
        else atlasSites, startingAdvisers(chronicle, orders))

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

  /** The deal holds the Chronicle's decks, each card once, in any order; the
    * world deck below the dealt cards holds the rest of `denizenOrder`, in
    * order unless a short deck was mixed with the Visions (CR p. 12). */
  private def validateDeal(chronicle: Chronicle, orders: SetupOrders,
      drawers: Int): Either[OathViolation, Unit] =
    val rest = orders.denizenOrder.drop(SetupDeal.dealt(drawers))
    val deck = orders.worldDeckOrder.collect { case id: DenizenId => id }
    for
      _ <- Either.cond(orders.denizenOrder.sortBy(_.value) ==
          chronicle.worldDeck.sortBy(_.value), (),
        UnsupportedChronicle("the deal does not hold the Chronicle's world deck"))
      _ <- Either.cond(orders.relicOrder.sortBy(_.value) ==
          chronicle.relicDeck.sortBy(_.value), (),
        UnsupportedChronicle("the deal does not hold the Chronicle's relic deck"))
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

  private def homelandEdifices(catalog: ExecutableCatalog,
      inPlay: Vector[StoredSite])
      : Either[OathViolation, Vector[(SiteId, EdificeId)]] =
    inPlay.foldLeft[Either[OathViolation, Vector[(SiteId, EdificeId)]]](
        Right(Vector.empty)) { (acc, stored) =>
      acc.flatMap { built =>
        catalog.site(stored.site).get.homeland match
          case None => Right(built)
          case Some(suit) =>
            stored.items.collectFirst { case id: EdificeId => id } match
              case Some(edificeId) if catalog.edifice(edificeId).exists(_.suit == suit) =>
                Right(built :+ (stored.site -> edificeId))
              case Some(edificeId) => Left(UnsupportedChronicle(
                s"edifice ${edificeId.value} at ${stored.site.value} does " +
                  "not match its Homeland suit"))
              case None => Left(UnsupportedChronicle(
                s"Homeland ${stored.site.value} has no stored edifice"))
      }
    }

  private def relicSlots(catalog: ExecutableCatalog, site: SiteId): Int =
    catalog.site(site).get.relicSlots
