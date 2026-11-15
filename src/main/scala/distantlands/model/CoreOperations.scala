package distantlands.model

/** Core operations occurring in a game of Oath.
  *
  * These values describe semantic game instructions. They do not
  * validate restrictions, choices, and consent.
  *
  * Oath's own vocabulary is split into operation families, each a sealed
  * trait in its own file: [[PieceOperation]], [[ResourceOperation]],
  * [[CardOperation]], [[DiceOperation]], [[TurnOperation]] and
  * [[ChronicleOperation]]. [[CoreOperation]] is their union, so the
  * serializer, the applier and the restriction screener still match the
  * whole vocabulary exhaustively, and a new case in any family fails to
  * compile until every such match has an arm. The walker's generic control
  * vocabulary is not here: it lives under the unsealed [[Operation]] in
  * `Operation.scala`, and a control node is therefore not a
  * `Vector[CoreOperation]` member and never a recorded delta.
  *
  * Composites expose their constituent operations through `children`; read
  * the leaves with [[Operation.flatten]]. [[PrimitiveOperation]] is Oath's
  * half of [[LeafOperation]]: a family case that is also a leaf.
  *
  * Only the family files extend [[OperationBase]]
  * (`scripts/check-architecture.py`). A family joins the vocabulary by
  * joining the union below.
  */
trait OperationBase extends Operation:
  def simultaneous: Boolean = false

type CoreOperation = PieceOperation | ResourceOperation | CardOperation |
  DiceOperation | TurnOperation | ChronicleOperation

type PrimitiveOperation = CoreOperation & LeafOperation
