package oathdigital.gameplay.setup

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.StateBasedEvaluation
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.model._

/** What a Chronicle's lineages bring to the table `FirstGameSetupMaterializer`
  * built (CR pp. 8-11): every seated lineage's starting advisers, and, in a
  * later game, the Empire's roles, purple warbands, Grand Scepter, title and
  * banners. The state is built directly, as game start builds the whole
  * game; nothing here is journaled. */
object LaterGameSetup:
  /** Each seated lineage's starting advisers move to its player; with an
    * Empire, its table facts follow. */
  def apply(catalog: ExecutableCatalog, ready: ReadyGame,
      plan: FirstGameSetupPlan): ReadyGame =
    val advised = startingAdvisers(ready, plan)
    plan.empire.fold(advised)(empire =>
      shortSitesTakeBandits(catalog, imperial(advised, plan, empire)))

  /** The advisers go facedown to their players and leave the lineage's
    * record, so the card index holds each card once (CR p. 11). */
  private def startingAdvisers(ready: ReadyGame, plan: FirstGameSetupPlan)
      : ReadyGame =
    val seated = plan.participants.map(_.lineageId).toSet
    ready.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.map((id, lineage) => id -> (if seated(id) then
        lineage.copy(startingAdvisers = Vector.empty) else lineage))))
      .updateCurrent(current => current.copy(players = current.players.map(
        state => state.copy(advisers = state.advisers ++ plan.startingAdvisers
          .getOrElse(state.player, Vector.empty).map(faceDown)))))

  private def imperial(ready: ReadyGame, plan: FirstGameSetupPlan,
      empire: EmpireSetup): ReadyGame =
    val chancellor = empire.chancellor
    val chancellorLineage = plan.participants
      .find(_.playerId == chancellor).map(_.lineageId)
    val goal = plan.oathkeeperGoal
    val scepter = RelicState(TheGrandScepterCard.id, Orientation.FaceUp,
      Tokens.empty)
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
          if state.player == chancellor then
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
