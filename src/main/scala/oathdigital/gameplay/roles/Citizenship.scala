package oathdigital.gameplay.roles

import oathdigital.gameplay.powers.{PlayerFacts, PowerAnswers}
import oathdigital.model._
import oathdigital.model.OathViolation.InvalidEventOrder

/** Role changes as walker trees, shared by every source (Empire phase
  * design, "Slice 3"). Becoming Imperial asks the player which warbands
  * turn purple only when the purple bank is short (ruling R18). */
object Citizenship:
  val replaceDecisionId: String = "role.replace-warbands"
  val ignoredReason: String = "Citizenship is ignored due to setup option"

  /** Whether the first-game option ignores "become a Citizen" powers. */
  def ignored(ready: ReadyGame): Boolean =
    ready.setup.ignoreCitizenshipPowers

  /** `actor` becomes a Citizen, or the Chancellor in a game with no
    * Empire. */
  def become(actor: PlayerId): Operation = Sequence(Vector(
    Branch((live, _) => query(live, actor).toVector.map(asked =>
      Decide(replaceDecisionId, actor, asked))),
    BuildOps((live, pending) => for
      kept <- answered(live, actor, pending)
      ops <- RoleChanges.becomeImperial(live, actor, kept)
    yield ops, required = true)))

  /** `actor`, a Citizen, becomes an Exile. */
  def exile(actor: PlayerId): Operation =
    BuildOps((live, _) => RoleChanges.exile(live, actor), required = true)

  /** The split a short purple bank asks for: every slot opens empty, and
    * the slots together take the whole bank. */
  private[roles] def query(ready: ReadyGame, actor: PlayerId)
      : Option[DecisionQuery] = for
    state <- PlayerFacts.player(ready, actor).toOption
    held = RoleChanges.holdings(ready, state)
    available = RoleChanges.purpleAvailable(ready)
    if held.total > available
  yield DecisionQuery.Distribute.exactly(
    DistributeSlot(DecisionOptionRef.Player(actor), 0, held.board, None) +:
      held.sites.map((site, count) =>
        DistributeSlot(DecisionOptionRef.Site(site), 0, count, None)),
    available,
    Some("Too few purple warbands: choose where yours turn purple"),
    "Replace warbands")

  private[roles] def kept(actor: PlayerId, rows: Vector[DistributeAmount])
      : Either[OathViolation, RoleChanges.Kept] =
    val board = rows.collect {
      case DistributeAmount(DecisionOptionRef.Player(`actor`), n) => n }
    val sites = rows.collect {
      case DistributeAmount(DecisionOptionRef.Site(site), n) => site -> n }
    Either.cond(board.size == 1 && board.size + sites.size == rows.size,
      RoleChanges.Kept(board.head, sites.toMap),
      InvalidEventOrder(s"$replaceDecisionId names an unknown place"))

  /** Every warband kept when nothing was asked; otherwise the split. */
  private def answered(ready: ReadyGame, actor: PlayerId,
      pending: PendingTree): Either[OathViolation, RoleChanges.Kept] =
    if query(ready, actor).isEmpty then RoleChanges.keepAll(ready, actor)
    else PowerAnswers.distribution(pending, replaceDecisionId)
      .toRight(PowerAnswers.missing(replaceDecisionId))
      .flatMap(kept(actor, _))
