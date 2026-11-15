package distantlands.model

/** Operations on cards: looking at, flipping and revealing them,
  * burying, discarding and removing them, drawing, playing and
  * swapping them, and reordering a shuffle pile.
  *
  * One operation family: a sealed area of Oath's operation
  * vocabulary, joined to [[CoreOperation]] by the union alias
  * in `CoreOperations.scala`. A leaf also extends
  * [[LeafOperation]]; a composite exposes its `children`.
  */
sealed trait CardOperation extends OperationBase

/** Looks at a card's front without changing its orientation or location. */
final case class Peek(viewer: PlayerId, card: CardId,
    at: Location) extends CardOperation, LeafOperation

/** Sets a card's physical orientation. */
final case class Flip(card: CardId, at: Location,
    orientation: Orientation) extends CardOperation, LeafOperation

/** Adds a denizen, relic, or edifice to the bottom of its matching deck.
  * Bury explicitly ignores the locked restriction.
  */
final case class Bury(card: BuryableCard, from: PositionedLocation,
    override val required: Boolean = false)
    extends CardOperation, LeafOperation:
  val to: PositionedLocation = PositionedLocation(
    Location.Deck(card.deck), StackPosition.Bottom)

object Bury:
  /** A bury with the returns a discard makes: favor to the suit's bank and
    * secrets to the acting player, facedown. `Bury` alone returns nothing.
    * The returns come first because a card must carry no resources when it
    * enters a deck.
    * `suit` is a fact about the card the caller supplies (the pipeline has no
    * catalog). It may be `None` only when `favor` is zero, as for a relic.
    */
  def standard(card: BuryableCard, from: PositionedLocation,
      suit: Option[Suit], favor: Int, secrets: Int,
      actingPlayer: PlayerId): Vector[CoreOperation] =
    require(favor == 0 || suit.isDefined, "buried favor needs its suit bank")
    OperationChildren.returns(card.id, suit, favor, secrets,
      actingPlayer) :+
      Bury(card, from)

/** Why a power discards a card (catalog batch 11, slice 4): `power`, whose
  * use discards it, and whether the discard `makesRoom` that same power
  * then fills (a mover's full target site, an adviser area at its limit).
  * A discard without a cause is a rule's: a Search's own, a plain card
  * play's, the Chronicle's. Wild Pastures reads it, since the walk itself
  * cannot tell which power built a batch.
  */
final case class DiscardCause(power: PowerId, makesRoom: Boolean = false)

object DiscardCause:
  /** `operations` with every denizen and ruined-edifice discard among them
    * caused by `cause`. A power marks its own batch, which nests no
    * discard in a `Take`'s leaving. */
  def mark(operations: Vector[CoreOperation], cause: DiscardCause)
      : Vector[CoreOperation] = operations.map {
    case discard: Discard.Denizen => discard.copy(cause = Some(cause))
    case discard: Discard.RuinedEdifice => discard.copy(cause = Some(cause))
    case other => other
  }

sealed trait Discard extends CardOperation
object Discard:
  import OperationChildren.*
  /** Discards a denizen facedown to the specified regional pile and returns
    * all resources on it. `actingPlayer` takes its secrets and discards it,
    * unless the discard is `actorless`: an effect no player performs, as
    * Empty Grounds'. Its secrets still go to `actingPlayer`, the active
    * player, since secrets reach a card only on a player's turn.
    * `position` is the end of the pile it goes to: the top, unless a power
    * says otherwise (Bracken, catalog batch 10, slice 9). `cause` is the
    * power whose use discards it ([[DiscardCause]]).
    */
  final case class Denizen(card: DenizenId, from: PositionedLocation,
      to: Region, suit: Suit, favor: Int,
      secrets: Int, actingPlayer: PlayerId,
      override val required: Boolean = false, actorless: Boolean = false,
      position: StackPosition = StackPosition.Top,
      cause: Option[DiscardCause] = None)
      extends Discard:
    require(favor >= 0, "discarded favor must be non-negative")
    require(secrets >= 0, "discarded secrets must be non-negative")
    require(position != StackPosition.Unspecified,
      "a discard goes to the top or the bottom of its pile")

    /** The player who discards it, if any. */
    def by: Option[PlayerId] = Option.unless(actorless)(actingPlayer)

    override val children: Vector[Operation] =
      discardWorld(card, from, to, by, position) ++
        returnedResources(card, suit, favor, secrets, actingPlayer)

  /** Visions carry no resources but otherwise use world-card discard rules,
    * `position` included. */
  final case class Vision(card: VisionId, from: PositionedLocation,
      to: Region, override val required: Boolean = false,
      position: StackPosition = StackPosition.Top) extends Discard:
    require(position != StackPosition.Unspecified,
      "a discard goes to the top or the bottom of its pile")
    override val children: Vector[Operation] =
      discardWorld(card, from, to, None, position)

  /** A world card discarded to the bottom of the world deck instead of a
    * pile (Cracked Horn, catalog batch 10, slice 9). It is still a discard,
    * so the rules that judge discards judge it: the locks, Hall of
    * Ministers and the active-modifier rule (owner ruling, 2026-11-06). The
    * card itself is buried, with the returns a discard makes; a buried
    * Vision leaves the Visions Drawn track alone. `actingPlayer` discards
    * it. */
  final case class Buried(card: BuryableCard, from: PositionedLocation,
      suit: Option[Suit], favor: Int, secrets: Int, actingPlayer: PlayerId,
      override val required: Boolean = false) extends Discard:
    require(card.deck == CardDeck.World,
      "only a denizen or a Vision is discarded under the world deck")
    require(favor >= 0, "discarded favor must be non-negative")
    require(secrets >= 0, "discarded secrets must be non-negative")
    require(favor == 0 || suit.isDefined, "buried favor needs its suit bank")

    /** The player who discards it. */
    def by: Option[PlayerId] = Some(actingPlayer)

    override val children: Vector[Operation] =
      returns(card.id, suit, favor, secrets, actingPlayer) :+ Bury(card, from)

  /** Only a ruined edifice can be discarded; intact edifices are locked.
    * `actingPlayer` and `actorless` are as for `Denizen`. A discarded
    * edifice is set aside in discard order, not buried, so Build Edifice
    * offers it only when its suit has no other (Chronicle ruling 9).
    * `cause` is the power whose use discards it ([[DiscardCause]]). */
  final case class RuinedEdifice(card: EdificeId, from: PositionedLocation,
      suit: Suit, favor: Int, secrets: Int, actingPlayer: PlayerId,
      override val required: Boolean = false, actorless: Boolean = false,
      cause: Option[DiscardCause] = None)
      extends Discard:
    require(favor >= 0, "discarded favor must be non-negative")
    require(secrets >= 0, "discarded secrets must be non-negative")

    /** The player who discards it, if any. */
    def by: Option[PlayerId] = Option.unless(actorless)(actingPlayer)

    override val children: Vector[Operation] = Vector(Move(
      Piece.Card(card), from,
      PositionedLocation(Location.SetAsideEdifices), by = by)) ++
      returnedResources(card, suit, favor, secrets, actingPlayer)

  /** Sets a relic aside until Chronicle and takes its secrets facedown. */
  final case class Relic(card: RelicId, from: PositionedLocation, secrets: Int,
      actingPlayer: PlayerId) extends Discard:
    require(secrets >= 0, "discarded secrets must be non-negative")

    override val children: Vector[Operation] = Vector(Move(
      Piece.Card(card), from,
      PositionedLocation(Location.SetAsideRelics),
      by = Some(actingPlayer))) ++
      returnedSecrets(card, secrets, actingPlayer)

/** Takes a card off the map into a pile that holds no tokens (Pilgrimage,
  * catalog batch 10, slice 6): a denizen to the Dispossessed, or a ruined
  * edifice to the bottom of the edifice deck. Its favor goes to `suit`'s
  * bank and its secrets to `actingPlayer`, facedown, as a discard's do.
  * The returns are children of this operation with the card's move, so a
  * restriction that refuses the move, such as a lock, refuses them too,
  * and the card keeps its tokens. */
final case class RemoveFromMap(card: CardId, from: PositionedLocation,
    to: PositionedLocation, suit: Suit, favor: Int, secrets: Int,
    actingPlayer: PlayerId) extends CardOperation:
  require(favor >= 0, "removed favor must be non-negative")
  require(secrets >= 0, "removed secrets must be non-negative")

  override val children: Vector[Operation] =
    OperationChildren.returns(card, Some(suit), favor, secrets,
      actingPlayer) :+
      Move(Piece.Card(card), from, to, by = Some(actingPlayer))

/** Takes known cards from a prompted draw source, in the order drawn: from
  * the top, or from the bottom when `position` says so (a Search drawn
  * bottom up, catalog batch 6). The destination is intentionally supplied
  * by the prompting rule.
  */
final case class Draw(player: PlayerId, cards: Vector[CardId],
    source: Location, destination: Location,
    position: StackPosition = StackPosition.Top)
    extends CardOperation:
  override val required: Boolean = true
  require(cards.nonEmpty, "draw must contain at least one card")
  require(position != StackPosition.Unspecified,
    "a draw takes from the top or the bottom")

  val takes: Vector[Take] = cards.map(card => Take(Piece.Card(card),
    player, source, destination, sourcePosition = position))
  override val children: Vector[Operation] =
    takes.flatMap(_.children)

/** Places a prompted card at a site or in a player's play area, including
  * their Advisers.
  */
final case class Play(card: CardId, from: PositionedLocation,
    destination: Location, orientation: Orientation,
    override val required: Boolean = false)
    extends CardOperation:
  require(destination match {
    case _: Location.Site | _: Location.PlayArea => true
    case _ => false
  }, "play destination must be a site or a play area")

  override val children: Vector[Operation] = Vector(Move(
    Piece.Card(card), from, PositionedLocation(destination),
    resultingOrientation = Some(orientation)))

/** Flips a card faceup without triggering When Played powers. */
final case class Reveal(card: CardId, at: Location)
    extends CardOperation:
  override val children: Vector[Operation] =
    Vector(Flip(card, at, Orientation.FaceUp))

/** Swaps two cards by moving each one to the other's location. `by` is the
  * player who swaps them. */
final case class Swap(firstCard: CardId, firstLocation: PositionedLocation,
    secondCard: CardId, secondLocation: PositionedLocation,
    by: Option[PlayerId] = None)
    extends CardOperation:
  require(firstCard != secondCard, "swap requires two different cards")
  require(firstLocation != secondLocation,
    "swap requires two different locations")

  val first: Move =
    Move(Piece.Card(firstCard), firstLocation, secondLocation, by = by)
  val second: Move =
    Move(Piece.Card(secondCard), secondLocation, firstLocation, by = by)
  override val children: Vector[Operation] = Vector(first, second)
  override val simultaneous: Boolean = true

/** Reorders a shuffle pile: a pile Search draws from (the world deck, or
  * one region's discard pile), the edifice deck, or the set-aside relics.
  * `order` is the pile's new contents in stored order (the world deck top
  * first, a discard pile top last).
  *
  * A tree declares a shuffle with no order, as a leaf of its own (a
  * `Branch` may return one). The walker fills the order from its random
  * source before running it, so the journal records the order and replay
  * applies it as it ran, as it applies an automatic roll's faces. A
  * `BuildOps` batch is run as it is built, so an unordered shuffle inside one
  * is never filled. Applying a shuffle with no order, or with an order that
  * is not exactly the pile's cards, is an error.
  */
final case class Shuffle(pile: ShufflePile,
    order: Option[Vector[CardId]] = None) extends CardOperation, LeafOperation:
  override val required: Boolean = true
