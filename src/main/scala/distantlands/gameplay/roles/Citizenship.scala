package distantlands.gameplay.roles

import distantlands.gameplay.powers.{PlayerFacts, PowerAnswers}
import distantlands.model._
import distantlands.model.OathViolation.InvalidEventOrder

/** Role changes as walker trees, shared by every source (Empire phase
  * design, "Slice 3"). Becoming Imperial asks the player which warbands
  * turn purple only when the purple bank is short (ruling R18).
  *
  * The split is read from the tree's first answer to the player's own
  * `replaceDecisionId`, so a tree may make several players Imperial, each
  * once. */
object Citizenship:
  /** The split a short purple bank asks `player`. */
  def replaceDecisionId(player: PlayerId): String =
    s"role.replace-warbands.${player.value}"
  val ignoredReason: String = "Citizenship is ignored due to setup option"

  /** Whether the first-game option ignores "become a Citizen" powers. */
  def ignored(ready: ReadyGame): Boolean =
    ready.setup.ignoreCitizenshipPowers

  /** `actor` becomes a Citizen, or the Chancellor in a game with no
    * Empire. */
  def become(actor: PlayerId): Operation = Sequence(Vector(
    Branch((live, _) => query(live, actor).toVector.map(asked =>
      Decide(replaceDecisionId(actor), actor, asked))),
    BuildOps((live, pending) => for
      kept <- answered(live, actor, pending)
      ops <- RoleChanges.becomeImperial(live, actor, kept)
    yield ops, required = true)))

  /** `actor`, a Citizen, becomes an Exile. Nothing happens while Council
    * Seat guards them (catalog batch 10, slice 1). */
  def exile(actor: PlayerId): Operation =
    BuildOps((live, _) =>
      if CouncilSeat.guards(live, actor) then Right(Vector.empty)
      else RoleChanges.exile(live, actor), required = true)

  /** The split a short purple bank asks for: one slot per place that holds
    * the player's warbands, each opening empty, and together they take the
    * whole bank. A bank short for warbands in one place asks nothing: that
    * place keeps what the bank holds. */
  private[roles] def query(ready: ReadyGame, actor: PlayerId)
      : Option[DecisionQuery] = for
    state <- PlayerFacts.player(ready, actor).toOption
    held = RoleChanges.holdings(ready, state)
    available = RoleChanges.purpleAvailable(ready)
    if held.total > available
    slots = places(actor, held)
    if slots.size >= 2
  yield DecisionQuery.Distribute.exactly(slots.map((ref, count) =>
    DistributeSlot(ref, 0, count, None)),
    available,
    Some("Too few purple warbands: choose where yours turn purple"),
    "Replace warbands")

  /** The places holding `actor`'s warbands: their board, then each site. */
  private def places(actor: PlayerId, held: RoleChanges.Holdings)
      : Vector[(DecisionOptionRef, Int)] =
    Option.when(held.board > 0)(
      (DecisionOptionRef.Player(actor): DecisionOptionRef) -> held.board)
      .toVector ++ held.sites.map((site, count) =>
        (DecisionOptionRef.Site(site): DecisionOptionRef) -> count)

  private[roles] def kept(actor: PlayerId, rows: Vector[DistributeAmount])
      : Either[OathViolation, RoleChanges.Kept] =
    val board = rows.collect {
      case DistributeAmount(DecisionOptionRef.Player(`actor`), n) => n }
    val sites = rows.collect {
      case DistributeAmount(DecisionOptionRef.Site(site), n) => site -> n }
    Either.cond(board.size <= 1 && board.size + sites.size == rows.size,
      RoleChanges.Kept(board.headOption.getOrElse(0), sites.toMap),
      InvalidEventOrder(
        s"${replaceDecisionId(actor)} names an unknown place"))

  /** Every warband kept when the bank has room; the bank's warbands at the
    * one place when they are all in one place; otherwise the split. */
  private def answered(ready: ReadyGame, actor: PlayerId,
      pending: PendingTree): Either[OathViolation, RoleChanges.Kept] =
    PlayerFacts.player(ready, actor).flatMap { state =>
      val held = RoleChanges.holdings(ready, state)
      val available = RoleChanges.purpleAvailable(ready)
      places(actor, held) match
        case _ if held.total <= available => RoleChanges.keepAll(ready, actor)
        case Vector((DecisionOptionRef.Site(site), _)) =>
          Right(RoleChanges.Kept(0, Map(site -> available)))
        case Vector((_, _)) => Right(RoleChanges.Kept(available, Map.empty))
        case _ => PowerAnswers.distribution(pending, replaceDecisionId(actor))
          .toRight(PowerAnswers.missing(replaceDecisionId(actor)))
          .flatMap(kept(actor, _))
    }
