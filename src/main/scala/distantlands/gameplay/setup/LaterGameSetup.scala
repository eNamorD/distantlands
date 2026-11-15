package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.StateBasedEvaluation
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** What a Chronicle's lineages bring to the table `FirstGameSetupMaterializer`
  * built (CR pp. 8-11): every seated lineage's starting advisers and
  * starting relics, and, in a later game, the Empire's roles, purple
  * warbands, Grand Scepter, title and banners, or, with no Empire, the box's
  * Exiles and the Reliquary set aside. The state is built directly, as game
  * start builds the whole game; nothing here is journaled. */
object LaterGameSetup:
  /** Each seated lineage's starting advisers move to its player; with an
    * Empire, its table facts follow, and with none, the box's. */
  def apply(catalog: ExecutableCatalog, ready: ReadyGame,
      plan: FirstGameSetupPlan, mode: SetupMode): ReadyGame =
    val advised = startingRelics(startingAdvisers(ready, plan), plan)
    mode match
      case SetupMode.FirstGame => advised
      case SetupMode.WithEmpire(empire) =>
        shortSitesTakeBandits(catalog, imperial(advised, plan, empire))
      case SetupMode.NoEmpire(_, exiled) => withoutEmpire(advised, exiled)
      // No Empire existed, so no lineage is exiled; the Reliquary is set
      // aside as with no Empire.
      case SetupMode.FirstGameExileOnly(_) => withoutEmpire(advised, Vector.empty)

  /** Setup With No Empire, off the map: every Imperial lineage in the box
    * becomes an Exile, and the Reliquary is set aside until the Chronicle
    * Phase (Foundations ruling 3). The title, the banners, the Usurper
    * Limiter and the first player stay as the materializer set them, as in
    * a first game; the Grand Scepter stays in the box. */
  private def withoutEmpire(ready: ReadyGame, exiled: Vector[LineageId])
      : ReadyGame =
    ready.updateCampaign(campaign => campaign.copy(
      boxRoles = campaign.boxRoles ++ exiled.map(_ -> Role.Exile),
      reliquarySetAside = true))

  /** The advisers go facedown to their players and leave the lineage's
    * record, so the card index holds each card once (CR p. 11). They come
    * before any adviser Teeming World dealt the player: a player takes
    * their stored advisers before Foundation IV, and the order of facedown
    * advisers cannot change. */
  private def startingAdvisers(ready: ReadyGame, plan: FirstGameSetupPlan)
      : ReadyGame =
    val seated = plan.participants.map(_.lineageId).toSet
    ready.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.map((id, lineage) => id -> (if seated(id) then
        lineage.copy(startingAdvisers = Vector.empty) else lineage))))
      .updateCurrent(current => current.copy(players = current.players.map(
        state => state.copy(advisers = plan.startingAdvisers
          .getOrElse(state.player, Vector.empty).map(faceDown) ++
          state.advisers))))

  /** Each seated lineage's starting relics go facedown to its player and
    * leave the lineage's record, as its starting advisers do (Legacies
    * design, slice 1). Heirloom keeps one; an import may carry them. */
  private def startingRelics(ready: ReadyGame, plan: FirstGameSetupPlan)
      : ReadyGame =
    val seats = plan.participants.map(p => p.lineageId -> p.playerId).toMap
    val kept = ready.game.campaign.lineages.collect {
      case (lineage, state) if seats.contains(lineage) =>
        seats(lineage) -> state.startingRelics }
    ready.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.map((id, lineage) => id -> (if seats.contains(id)
        then lineage.copy(startingRelics = Vector.empty) else lineage))))
      .updateCurrent(current => current.copy(players = current.players.map(
        state => state.copy(relics = state.relics ++ kept.getOrElse(
          state.player, Vector.empty).map(
          RelicState(_, Orientation.FaceDown, Tokens.empty))))))

  private def imperial(ready: ReadyGame, plan: FirstGameSetupPlan,
      empire: EmpireSetup): ReadyGame =
    val chancellor = empire.chancellor
    val chancellorLineage = plan.participants
      .find(_.playerId == chancellor).map(_.lineageId)
    val goal = plan.oathkeeperGoal
    val scepter = RelicState(TheGrandScepterCard.id, Orientation.FaceUp,
      Tokens.empty)
    // Scepter Bearer: an Imperial holder starts with the Grand Scepter in
    // the Chancellor's place (Legacy Setup powers ruling 6).
    val bearer = plan.legacies.scepterBearer.getOrElse(chancellor)
    val crowned = ready.updateCampaign(campaign => campaign.copy(
      lineages = campaign.lineages.map((id, lineage) => id ->
        (if chancellorLineage.contains(id) then
          lineage.copy(role = Role.Chancellor) else lineage)),
      boxRoles = campaign.boxRoles ++
        empire.absentChancellor.map(_ -> Role.Citizen)))
    crowned.copy(banks = crowned.banks.copy(warbandSupply =
      crowned.banks.warbandSupply.updated(ForceKind.Imperial,
        MaterialBankState.ImperialWarbands))).updateCurrent(current =>
      current.copy(
        players = current.players.map(state =>
          if state.player == bearer then
            state.copy(relics = state.relics :+ scepter)
          else state),
        title = OathkeeperState(Some(chancellor), TitleSide.Oathkeeper),
        banners = current.banners.copy(
          peoplesFavor = current.banners.peoplesFavor.copy(holder =
            Option.when(goal == OathkeeperGoal.ThePeople)(chancellor)
              .orElse(current.banners.peoplesFavor.holder)),
          darkestSecret = current.banners.darkestSecret.copy(holder =
            Option.when(goal == OathkeeperGoal.Devotion)(chancellor)
              .orElse(current.banners.darkestSecret.holder))),
        // The Chancellor goes first, so they already begin the round.
        tracks = current.tracks.copy(usurperLimited = false)))

  /** An Empire site the purple supply could not reach is empty; the check
    * the walker runs after each action fills it, and every other empty
    * site, with bandits up to its capacity (Empire ruling R30). */
  private def shortSitesTakeBandits(catalog: ExecutableCatalog,
      ready: ReadyGame): ReadyGame =
    val refills = StateBasedEvaluation.emptySites(catalog, ready).toMap
    ready.updateCurrent(current => current.copy(map = current.map.copy(
      sites = current.map.sites.map { (site, state) =>
        site -> refills.get(site).fold(state)(count =>
          state.copy(forces = SiteForces.Occupied(ForceKind.Bandit, count)))
      })))

  private def faceDown(adviser: AdviserState): AdviserState = adviser match
    case state: DenizenState => state.copy(orientation = Orientation.FaceDown)
    case state: VisionState => state.copy(orientation = Orientation.FaceDown)
