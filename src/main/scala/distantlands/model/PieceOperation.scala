package distantlands.model

/** Operations that move pieces between custodies: a single `Move`, and
  * the composites that take, give, exchange, kill, replace or
  * sacrifice pieces through `Move` children.
  *
  * One operation family: a sealed area of Oath's operation
  * vocabulary, joined to [[CoreOperation]] by the union alias
  * in `CoreOperations.scala`. A leaf also extends
  * [[LeafOperation]]; a composite exposes its `children`.
  */
sealed trait PieceOperation extends OperationBase

/** Changes a piece's location or role within one play area. `by` is the
  * player who moves it, where a rule names one (catalog batch 8, slice 11):
  * a restriction that holds against some players only reads it off the
  * operation tree. `None` also means nobody moves it, as for Empty
  * Grounds' discards, so no such restriction holds.
  */
final case class Move(
    piece: Piece,
    from: PositionedLocation,
    to: PositionedLocation,
    resultingOrientation: Option[Orientation] = None,
    by: Option[PlayerId] = None)
    extends PieceOperation, LeafOperation:
  require(from != to || (resultingOrientation.isDefined &&
      piece.isInstanceOf[Piece.Card] &&
      from.location == to.location &&
      from.location.isInstanceOf[Location.PlayArea]),
    "a move must change location, stack position, or card role within a play area")
  require(resultingOrientation.isEmpty || piece.isInstanceOf[Piece.Card],
    "only cards have an orientation")
  require(from.side.isEmpty || piece.isInstanceOf[Piece.Secrets],
    "only secrets have a side")
  require(to.side.isEmpty, "a move names the side it takes, not lands on")

/** Gives pieces in both directions. */
final case class Exchange(give: Give, receive: Give) extends PieceOperation:
  override val required: Boolean = true
  require(give.giver != receive.giver,
    "exchange requires two different giving players")

  override val children: Vector[Operation] =
    give.children ++ receive.children

/** Moves a piece out of the giver's custody to the prompted destination.
  * Restrictions on Take do not prevent a Give operation.
  */
final case class Give(piece: Piece, giver: PlayerId,
    from: Location, to: Location,
    override val required: Boolean = false)
    extends PieceOperation:
  require(Location.ownedBy(from, giver),
    "give source must belong to the giving player")
  val move: Move = Move(piece, PositionedLocation(from),
    PositionedLocation(to), by = Some(giver))
  override val children: Vector[Operation] = Vector(move)

/** Returns warbands to their matching bank. Imperial warbands use the
  * Chancellor's bank; bandits use the shared bandit bank.
  */
final case class Kill(warbands: Piece.Warbands,
    from: PositionedLocation) extends PieceOperation:
  override val children: Vector[Operation] = Vector(Move(
    warbands, from,
    PositionedLocation(Location.WarbandBank(warbands.kind))))

/** Swaps warbands for a new color. This is distinct from kill and sacrifice.
  * `by` is the player who performs it, as for `Move`; `None` when nobody
  * does, as for a role change (ruling R20), so no restriction against a
  * player holds. */
final case class Replace(removed: Piece.Warbands,
    replacements: Piece.Warbands, at: PositionedLocation,
    override val required: Boolean = false,
    by: Option[PlayerId] = None)
    extends PieceOperation:
  require(removed.amount == replacements.amount,
    "replace must exchange equal numbers of warbands")
  require(removed.kind != replacements.kind,
    "replacement warbands must have a new color")

  override val children: Vector[Operation] = Vector(
    Move(removed, at,
      PositionedLocation(Location.WarbandBank(removed.kind)), by = by),
    Move(replacements,
      PositionedLocation(Location.WarbandBank(replacements.kind)), at,
      by = by))
  override val simultaneous: Boolean = true

/** Kills a warband that belongs to the player issuing the instruction. */
final case class Sacrifice(player: PlayerId,
    warbands: Piece.Warbands, from: PositionedLocation)
    extends PieceOperation:
  private val kill = Kill(warbands, from)
  override val children: Vector[Operation] = kill.children

/** Moves a piece into the prompted player's custody. A required `Take`
  * rejects its batch, or fails the search's path, when a restriction refuses
  * it.
  *
  * `leaving` clears what does not travel with the piece before it moves: a
  * banner's favor returned to the banks, its secrets burnt. Those operations
  * are the `Take`'s first children, so a restriction that refuses the `Take`
  * refuses them with it, and the banner stays whole.
  *
  * `sourceSide` names the side of the secrets it takes from a board.
  */
final case class Take(piece: Piece, player: PlayerId,
    from: Location, to: Location,
    sourcePosition: StackPosition = StackPosition.Unspecified,
    override val required: Boolean = false,
    leaving: Vector[CoreOperation] = Vector.empty,
    sourceSide: Option[SecretSide] = None)
    extends PieceOperation:
  require(Location.ownedBy(to, player),
    "take destination must belong to the taking player")
  val move: Move = Move(piece, PositionedLocation(from, sourcePosition,
    sourceSide), PositionedLocation(to), by = Some(player))
  override val children: Vector[Operation] = leaving :+ move
