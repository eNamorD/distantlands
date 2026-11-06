package oathdigital.model

/** The pieces an operation moves, and where in a location it moves them
  * from or to. Split out of `CoreOperations.scala`, which holds the sealed
  * `CoreOperation` family and is close to the project's per-file bound. */
sealed trait Piece extends Product with Serializable
object Piece:
  final case class Card(id: CardId) extends Piece
  final case class Banner(banner: oathdigital.model.Banner) extends Piece
  final case class Pawn(player: PlayerId) extends Piece

  sealed trait Counted extends Piece { def amount: Int }
  final case class Favor(amount: Int) extends Counted:
    require(amount > 0, "favor amount must be positive")
  final case class Secrets(amount: Int) extends Counted:
    require(amount > 0, "secret amount must be positive")
  final case class Warbands(kind: ForceKind, amount: Int) extends Counted:
    require(amount > 0, "warband amount must be positive")

enum SecretSide { case FaceUp, FaceDown }

enum StackPosition { case Unspecified, Top, Bottom }

/** A location, and where in it a piece is moved from or to. `side`, for
  * secrets on a player's board, is the side a move takes them from; with
  * none, the move takes either side as its destination allows. Moved
  * secrets keep their side (catalog batch 8, slice 5). */
final case class PositionedLocation(
    location: Location,
    position: StackPosition = StackPosition.Unspecified,
    side: Option[SecretSide] = None):
  require(side.isEmpty || location.isInstanceOf[Location.PlayArea],
    "only secrets on a board have a side to move from")
