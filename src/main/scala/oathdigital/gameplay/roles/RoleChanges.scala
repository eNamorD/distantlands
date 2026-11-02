package oathdigital.gameplay.roles

import oathdigital.gameplay.actions.CardPlay
import oathdigital.gameplay.operations.GrandScepterSource
import oathdigital.gameplay.powers.PlayerFacts
import oathdigital.model._
import oathdigital.model.OathViolation.InvalidEventOrder

/** A role change in play as operations (CR p. 34; Empire phase design,
  * "Slice 3"): becoming a Citizen, becoming the Chancellor in a game with
  * no Empire, and exile. Each change is one batch, ordered so that every
  * step leaves a valid state: a board's warbands follow its owner's role,
  * so the board is emptied before the role changes and refilled after. */
object RoleChanges:
  /** A player's own warbands: on their board, and at each site. */
  final case class Holdings(board: Int, sites: Vector[(SiteId, Int)]):
    def total: Int = board + sites.map(_._2).sum

  /** How many of them turn purple at each place (ruling R18). The rest
    * return to the player's own bank. */
  final case class Kept(board: Int, sites: Map[SiteId, Int])

  def holdings(ready: ReadyGame, player: PlayerState): Holdings =
    val own = ForceKind.Exile(player.lineage)
    val current = ready.game.current
    Holdings(player.board.warbands, current.map.inPlay.flatMap(site =>
      current.map.sites.get(site).map(_.forces).collect {
        case SiteForces.Occupied(`own`, count) => site -> count }))

  /** The purple warbands a player can take: the Imperial bank's, or all
    * of them when this change creates the bank. */
  def purpleAvailable(ready: ReadyGame): Int =
    if ready.banks.warbandSupply.contains(ForceKind.Imperial) then
      PlayerFacts.banked(ready, ForceKind.Imperial)
    else MaterialBankState.ImperialWarbands

  /** A "become a Citizen" power makes the player the Chancellor in a game
    * with no Empire (CR p. 34). */
  def newRole(ready: ReadyGame): Role =
    if Imperials.chancellor(ready).isEmpty then Role.Chancellor
    else Role.Citizen

  /** Every warband kept, for a bank with room for them all. */
  def keepAll(ready: ReadyGame, player: PlayerId)
      : Either[OathViolation, Kept] =
    PlayerFacts.player(ready, player).map { state =>
      val held = holdings(ready, state)
      Kept(held.board, held.sites.toMap)
    }

  def becomeImperial(ready: ReadyGame, player: PlayerId, kept: Kept)
      : Either[OathViolation, Vector[CoreOperation]] = for
    state <- PlayerFacts.player(ready, player)
    _ <- Either.cond(Imperials.role(ready, player).contains(Role.Exile), (),
      InvalidEventOrder(s"${player.value} is not an Exile"))
    held = holdings(ready, state)
    _ <- Either.cond(fits(held, kept, purpleAvailable(ready)), (),
      InvalidEventOrder(s"${player.value} cannot keep those warbands"))
  yield
    val role = newRole(ready)
    val own = ForceKind.Exile(state.lineage)
    val chancellor = role == Role.Chancellor
    val bank = Option.when(
      !ready.banks.warbandSupply.contains(ForceKind.Imperial))(
      ImperialWarbandsTaken(MaterialBankState.ImperialWarbands))
    val sites = held.sites.flatMap((site, count) =>
      val keep = kept.sites.getOrElse(site, 0)
      val at = PositionedLocation(Location.Site(site))
      Option.when(count > keep)(Move(Piece.Warbands(own, count - keep), at,
        PositionedLocation(Location.WarbandBank(own)))).toVector ++
        Option.when(keep > 0)(Replace(Piece.Warbands(own, keep),
          Piece.Warbands(ForceKind.Imperial, keep), at)).toVector)
    val board = Option.when(held.board > 0)(Move(
      Piece.Warbands(own, held.board),
      PositionedLocation(Location.PlayArea(player)),
      PositionedLocation(Location.WarbandBank(own))))
    val gained = Option.when(kept.board > 0)(
      Gain.Warbands(player, ForceKind.Imperial, kept.board))
    val scepter =
      if chancellor then GrandScepterSource.take(ready, player)
      else Vector.empty
    val limiter = Option.when(chancellor &&
      ready.game.current.tracks.usurperLimited)(UsurperLimiterRemoved)
    bank.toVector ++ title(ready, player, role) ++ vision(ready, state) ++
      sites ++ board.toVector ++
      Vector(RoleChanged(state.lineage, role)) ++ gained.toVector ++
      scepter ++ limiter.toVector ++ finish(ready, state)

  def exile(ready: ReadyGame, player: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] = for
    state <- PlayerFacts.player(ready, player)
    _ <- Either.cond(Imperials.role(ready, player).contains(Role.Citizen),
      (), InvalidEventOrder(s"${player.value} is not a Citizen"))
  yield
    val own = ForceKind.Exile(state.lineage)
    val purple = state.board.warbands
    // Read before the change: the board still counts as purple, so the
    // own bank's count leaves it out.
    val gained = math.min(purple, PlayerFacts.banked(ready, own))
    Option.when(purple > 0)(Move(Piece.Warbands(ForceKind.Imperial, purple),
      PositionedLocation(Location.PlayArea(player)),
      PositionedLocation(Location.WarbandBank(ForceKind.Imperial))))
      .toVector ++ Vector(RoleChanged(state.lineage, Role.Exile)) ++
      Option.when(gained > 0)(Gain.Warbands(player, own, gained)).toVector ++
      finish(ready, state)

  private def fits(held: Holdings, kept: Kept, available: Int): Boolean =
    val places = kept.sites.keySet.subsetOf(held.sites.map(_._1).toSet)
    val within = kept.board >= 0 && kept.board <= held.board &&
      held.sites.forall((site, count) =>
        kept.sites.get(site).forall(n => n >= 0 && n <= count))
    val total = kept.board + kept.sites.values.sum
    places && within && total == math.min(held.total, available)

  /** CR p. 34: a Usurper title turns to its Oathkeeper side. Under
    * Supremacy it passes to the Chancellor, on either side (R19). */
  private def title(ready: ReadyGame, player: PlayerId, role: Role)
      : Vector[CoreOperation] =
    val current = ready.game.current.title
    if !current.holder.contains(player) then Vector.empty
    else
      val supremacy =
        ready.game.campaign.oathkeeperGoal == OathkeeperGoal.Supremacy
      val heir =
        if role == Role.Chancellor then Some(player)
        else Imperials.chancellor(ready)
      if supremacy && heir != Some(player) then Vector(SetOathkeeper(heir))
      else if current.side == TitleSide.Usurper then
        Vector(SetOathkeeper(Some(player)))
      else Vector.empty

  /** The revealed Vision goes to the discard pile after the player's
    * pawn's region, as a played card's discard does. */
  private def vision(ready: ReadyGame, state: PlayerState)
      : Vector[CoreOperation] = (for
    held <- state.revealedVision
    region <- CardPlay.discardRegion(ready, state.player)
  yield Discard.Vision(held.id,
    PositionedLocation(Location.PlayArea(state.player)), region)).toVector

  /** End their Act phase if it is their turn, then refresh their Supply to
    * its leftmost space. */
  private def finish(ready: ReadyGame, state: PlayerState)
      : Vector[CoreOperation] =
    val turn = ready.game.current.turn
    val ending = Option.when(turn.activePlayer == state.player &&
      turn.phase == Phase.Act)(EnterPhase(Phase.Rest))
    val missing = SupplyTrack.Maximum - state.board.supply.supply
    ending.toVector ++
      Option.when(missing > 0)(GainSupply(state.player, missing)).toVector
