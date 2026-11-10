package distantlands.gameplay.powers.travel

import distantlands.gameplay.{OathRules, TravelPlay}
import distantlands.gameplay.actions.travel.TravelProcedure
import distantlands.gameplay.cards.{BuriedGiantSite, DunesSite, SaltFlatsSite,
  ShroudedWoodsSite}
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.powers.{PowerFixture, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{WalkerPowers, WalkerSimulation}
import distantlands.gameplay.walker.WalkerSimulation.PreviewedOption
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ParkedNode, Refusals, Table}
import distantlands.testkit.PowerRules
import distantlands.testkit.Unwrap.accepted

/** A Travel board for the modifiers and rules of Travel: the map of
  * `TravelProcedureSuite`, with named sites and the active player at the
  * Cradle's plains.
  *
  * Regions: the Cradle holds `cradlePlains` (Buried Giant) and `coast`
  * (Desolate Shore); the Provinces hold `provincePlains` (Dunes),
  * `mountain` (Broken Peaks) and `pass` (Narrow Pass); the Hinterland holds
  * `hinterlandPlains` (Salt Flats), `island` (Fair Isle) and
  * `secondHinterlandPlains` (Shrouded Woods). The printed base is 1 within
  * the Cradle, 2 from the Cradle to the Provinces and within them, 4 from the
  * Cradle to the Hinterland. Every site with a printed capacity holds a
  * bandit stack of it, and every site its printed starting wealth: Fair
  * Isle 3 favor, Broken Peaks 2 secrets, Shrouded Woods 1 favor and 1
  * secret.
  */
object TravelFixture:
  import PowerFixture._

  val rules: OathRules = PowerRules.withWalkerPowers

  private def site(power: String): SiteId = catalog.sites.find(
    _.handlers.exists(_.value.endsWith(s".$power"))).get.id
  val cradlePlains: SiteId = BuriedGiantSite.id
  val provincePlains: SiteId = DunesSite.id
  val hinterlandPlains: SiteId = SaltFlatsSite.id
  val secondHinterlandPlains: SiteId = ShroudedWoodsSite.id
  val coast: SiteId = site("coast")
  val island: SiteId = site("island")
  val mountain: SiteId = site("mountain")
  val pass: SiteId = site("pass")

  /** The Act phase on the board. `source` is the active player's site; the
    * other players stand on the sites after it.
    */
  def board(source: SiteId = cradlePlains, supply: Int = 7): ReadyGame =
    val chosen = Vector(source, coast, provincePlains, mountain, pass,
      hinterlandPlains, island, secondHinterlandPlains)
    val ids = (chosen.distinct ++ catalog.sites.map(_.id)
      .filterNot(chosen.contains)).take(8)
    val states = ids.map { id =>
      val definition = catalog.sites.find(_.id == id).get
      id -> SiteState(
        if definition.capacity == 0 then SiteForces.Empty
        else SiteForces.Occupied(ForceKind.Bandit, definition.capacity),
        Vector.empty, Vector.empty, definition.startingResources)
    }.toMap
    val players = quietTable.game.current.players.zipWithIndex.map:
      case (player, index) => player.copy(
        pawnSite = Some(if player.player == actor then source else ids(index + 1)),
        board = if player.player == actor then
          player.board.copy(supply = SupplyTrack(supply)) else player.board)
    // No Table step lays out regions: the board replaces the whole map.
    inPhase(quietTable.updateCurrent(_.copy(players = players,
      map = MapState(ids.take(2), ids.slice(2, 5), ids.slice(5, 8), states))),
      Phase.Act)

  /** `ready` continued by Table steps, read without the card check: the board
    * replaces the whole map, so the cards it lays out are not the ones the
    * check expects the first game to have dealt. */
  def on(ready: ReadyGame)(steps: Table => Table)(
      using munit.Location): ReadyGame =
    steps(Table.from(ready)).ready

  def powers(modifiers: Vector[PowerId]): WalkerPowers =
    WalkerPowers.selected(WalkerPowerCatalog.default(catalog), modifiers)

  /** The destinations Travel offers with `modifiers` selected, and their Supply. */
  def candidates(ready: ReadyGame, modifiers: Vector[PowerId] = Vector.empty)
      : Map[SiteId, Int] =
    TravelProcedure.afterModifiers(catalog, ready, actor)
      .flatMap(WalkerSimulation.preview(_, ready, powers(modifiers)))
      .getOrElse(Vector.empty).collect {
        case PreviewedOption(DecisionOption.Site(DecisionOptionRef.Site(site)),
            Right(outcome)) => site -> outcome.operations.collect {
          case SpendSupply(player, amount, _) if player == actor => amount }.sum
      }.toMap

  /** What picking a destination the decision left out is refused with: the
    * look-ahead hides a destination no path completes, so the answer names an
    * option the decision does not offer. */
  val destinationNotOffered: OathViolation =
    Refusals.notOffered(TravelProcedure.destinationDecisionId)

  /** `actor` starts a Travel with `modifiers` and picks `destination`. */
  def travel(ready: ReadyGame, destination: SiteId,
      modifiers: Vector[PowerId] = Vector.empty)
      : Either[OathViolation, OathTransition] =
    TravelPlay.travelled(rules, Ready(ready), actor, destination, modifiers)

  /** `from`, parked on a pay-or-ask step, continued by `actor` answering it
    * with `choice`. The events are both transitions'. */
  def answeringStep(from: OathTransition, decisionId: String,
      choice: DecisionOptionRef): Either[OathViolation, OathTransition] =
    rules.resolveWalker(from.state, actor, decisionId,
      DecisionAnswer.ChooseOneAnswer(choice))
      .map(done => done.copy(events = from.events ++ done.events))

  /** Travel's modifier decision, `travel.modifiers`. */
  val modifierDecisionId: String =
    ModifierPicks.decisionId(PowerWindow.TravelModifierSelection)

  /** The decision a walk at `state` is parked on, rebuilt as the projector
    * rebuilds it. */
  def parkedDecision(state: OathState)(using munit.Location): Decide =
    ParkedNode.of(state, catalog, WalkerPowerCatalog.default(catalog)) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide
      case other =>
        munit.Assertions.fail(s"expected a parked decision, got $other")

  /** The id of the decision a Travel `actor` starts at `ready` parks on
    * first. */
  def startParksOn(ready: ReadyGame)(using munit.Location): String =
    parkedDecision(accepted(rules.startWalker(Ready(ready), ActionRef.Travel,
      actor)).state).decisionId
