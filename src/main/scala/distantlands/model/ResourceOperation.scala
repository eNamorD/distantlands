package distantlands.model

/** Operations on counted resources and Supply: favor and secrets
  * (flipping, burning, gaining, the Nomad bank), Supply spent and
  * gained, and paying a typed cost.
  *
  * One operation family: a sealed area of Oath's operation
  * vocabulary, joined to [[CoreOperation]] by the union alias
  * in `CoreOperations.scala`. A leaf also extends
  * [[LeafOperation]]; a composite exposes its `children`.
  */
sealed trait ResourceOperation extends OperationBase

/** Flips secrets already in a player's play area. Moving secrets preserves
  * their current orientation unless followed by this operation.
  */
final case class FlipSecrets(player: PlayerId, amount: Int,
    from: SecretSide, to: SecretSide) extends ResourceOperation, LeafOperation:
  require(amount > 0, "secret amount must be positive")
  require(from != to, "secret flip must change side")

/** Gains Supply up to SupplyTrack.Maximum. */
final case class GainSupply(player: PlayerId, amount: Int)
    extends ResourceOperation, LeafOperation:
  require(amount > 0, "supply gain must be positive")

/** Spends Supply. Costs are required unless a power explicitly permits less. */
final case class SpendSupply(player: PlayerId, amount: Int,
    override val required: Boolean = true) extends ResourceOperation, LeafOperation:
  require(amount > 0, "supply spend must be positive")

/** Moves favor or secrets to the shared bank. `by` is the player who burns
  * (catalog batch 8, "A burn's actor"): the actor of the procedure that
  * burns, each owner when an effect makes each player burn, the defeated
  * defender for a Raid's half-favor burn, and `None` when no player burns,
  * as in a setup rule. A cost's burnt portions are its payer's, so
  * `PayCost` holds no `Burn`.
  */
sealed trait Burn extends ResourceOperation:
  def resource: Piece.Counted
  def from: PositionedLocation
  def by: Option[PlayerId]

  final lazy val move: Move = Move(resource, from,
    PositionedLocation(Location.SharedBank))
  final override lazy val children: Vector[Operation] = Vector(move)
object Burn:
  def favor(amount: Int, from: PositionedLocation,
      by: Option[PlayerId]): Burn =
    ResourceBurn(Piece.Favor(amount), from, by)

  def secrets(amount: Int, from: PositionedLocation,
      by: Option[PlayerId]): Burn =
    ResourceBurn(Piece.Secrets(amount), from, by)

  private final case class ResourceBurn(resource: Piece.Counted,
      from: PositionedLocation, by: Option[PlayerId]) extends Burn

sealed trait Gain extends ResourceOperation
object Gain:
  final case class Favor(player: PlayerId, suit: Suit, amount: Int)
      extends Gain:
    override val children: Vector[Operation] = Vector(Move(
      Piece.Favor(amount),
      PositionedLocation(Location.FavorBank(suit)),
      PositionedLocation(Location.PlayArea(player))))

  final case class Secrets(player: PlayerId, amount: Int) extends Gain:
    override val children: Vector[Operation] = Vector(Move(
      Piece.Secrets(amount),
      PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.PlayArea(player))))

  final case class Warbands(player: PlayerId, kind: ForceKind, amount: Int)
      extends Gain:
    override val children: Vector[Operation] = Vector(Move(
      Piece.Warbands(kind, amount),
      PositionedLocation(Location.WarbandBank(kind)),
      PositionedLocation(Location.PlayArea(player))))

/** Moves `amount` favor from the Nomad bank onto `player`'s board, for the
  * spend that follows it in the same batch (Vow of Kinship, catalog batch
  * 11, slice 5). The favor lands on the board even while `player` holds
  * the Vow, which sends every other favor bound for their board to the
  * Nomad bank. Nobody moves it, so no restriction against a player holds.
  */
final case class DrawNomadFavor(player: PlayerId, amount: Int)
    extends ResourceOperation:
  require(amount > 0, "a draw moves favor")
  override val required: Boolean = true
  val move: Move = Move(Piece.Favor(amount),
    PositionedLocation(Location.FavorBank(Suit.Nomad)),
    PositionedLocation(Location.PlayArea(player)))
  override val children: Vector[Operation] = Vector(move)

/** Pays a typed cost. The placed portions (`favor`/`secret`) move from the
  * player's play area to `placedAt`; the burnt portions leave play to the
  * shared bank. An all-zero cost is an inert no-op.
  *
  * A cost is placed only onto an empty card, unless `intoOccupied` (battle
  * plans set it). `matchingBank` is the suit bank of the card the cost is paid
  * onto, supplied by the caller because the pipeline has no catalog; it is
  * used only to settle an off-turn payment.
  *
  * Off turn (`offTurn`, set only by the pipeline) the placed portions settle
  * immediately instead of resting on the card: favor goes to `matchingBank`
  * and the payer's secrets flip facedown.
  */
final case class PayCost(player: PlayerId, placedAt: Location, cost: Cost,
    intoOccupied: Boolean = false, matchingBank: Option[Suit] = None,
    offTurn: Boolean = false) extends ResourceOperation:
  require(!offTurn || cost.favor == 0 || matchingBank.isDefined,
    "an off-turn placed favor needs its matching bank")
  override val required: Boolean = true
  override val children: Vector[Operation] =
    placedFavor ++ placedSecrets ++
      favorMove(cost.favorBurnt, Location.SharedBank) ++
      secretMove(cost.secretBurnt, Location.SharedBank)

  // The cost.favor > 0 guard matters: matchingBank.get is evaluated eagerly
  // as an argument, and matchingBank may be None for a zero favor.
  private def placedFavor: Vector[Operation] =
    if offTurn && cost.favor > 0 then
      favorMove(cost.favor, Location.FavorBank(matchingBank.get))
    else favorMove(cost.favor, placedAt)

  private def placedSecrets: Vector[Operation] =
    if cost.secret == 0 then Vector.empty
    else if offTurn then Vector(FlipSecrets(player, cost.secret,
      SecretSide.FaceUp, SecretSide.FaceDown))
    else secretMove(cost.secret, placedAt)

  // The payer moves the favor, so a Vow of Kinship holder's cost may draw
  // on the Nomad bank (`KinshipDraws`).
  private def favorMove(amount: Int, to: Location): Vector[Operation] =
    OperationChildren.fromPlayArea(player, amount, Piece.Favor.apply, to,
      by = Some(player))

  private def secretMove(amount: Int, to: Location): Vector[Operation] =
    OperationChildren.fromPlayArea(player, amount, Piece.Secrets.apply, to)
