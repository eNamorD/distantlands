package distantlands.serialization

import distantlands.model._

/** What more than one operation family's codec uses: the decode-side
  * partial function each family contributes, the whole-vocabulary
  * encoder and decoder a held operation recurses into, and the
  * optional `by` of a move or a swap. */
private[serialization] trait OperationWireSupport:
  protected type DecodedOperation =
    PartialFunction[String, Either[WireError, CoreOperation]]

  /** Implemented by [[WalkerOperationCodec]]; a `Take`'s leaving
    * operations and a `Defer`'s held ones are whole operations. */
  protected def encodeOperation(operation: CoreOperation): ujson.Value

  protected def decodeOperation(value: ujson.Value,
      path: String): Either[WireError, CoreOperation]

  /** `encoded`, with `"by"` when the move names its mover. Written only
    * when present, so a move without one encodes as before. */
  protected final def withMover(encoded: ujson.Obj, by: Option[PlayerId]): ujson.Obj =
    by.foreach(player => encoded("by") = player.value)
    encoded

  /** The mover a move or swap names, if any. */
  protected final def decodeMover(value: ujson.Value): Option[PlayerId] =
    value.obj.get("by").map(by => PlayerId(by.str))
