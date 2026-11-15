package distantlands.serialization

import distantlands.model._

/** Operation spellings for recorded walker steps, one codec trait per
  * operation family. `WalkerOperationWireSuite` and the family wire suites
  * pin every spelling.
  *
  * `encodeOperation` is total over `CoreOperation`, exactly like
  * [[encodeLocation]] in [[WalkerValueCodec]] (added for the same reason: a
  * bounded operation set is what let Catacombs' recorded `Move` out of
  * `Location.Deck` reach an append-time throw with no test to catch it).
  * Totality is exceptionless: the walker's control vocabulary lives under
  * the unsealed `Operation`, not under `CoreOperation`, so a control node
  * cannot reach this method. A new case in any operation family fails to
  * compile in its family's encoder until it is given a real arm, and a new
  * family fails to compile here.
  *
  * Each family codec's decoder is a partial function over the wire kind,
  * chained below; `WalkerOperationWireSuite` pins that no two families
  * claim one kind.
  */
private[serialization] trait WalkerOperationCodec extends PieceOperationCodec
    with ResourceOperationCodec with CardOperationCodec
    with DiceOperationCodec with TurnOperationCodec
    with ChronicleOperationCodec:
  this: GameEventJsonSupport =>
  import WireError._

  protected final def encodeOperation(operation: CoreOperation): ujson.Value =
    operation match
      case operation: PieceOperation => encodePieceOperation(operation)
      case operation: ResourceOperation => encodeResourceOperation(operation)
      case operation: CardOperation => encodeCardOperation(operation)
      case operation: DiceOperation => encodeDiceOperation(operation)
      case operation: TurnOperation => encodeTurnOperation(operation)
      case operation: ChronicleOperation => encodeChronicleOperation(operation)

  protected final def decodeOperation(value: ujson.Value,
      path: String): Either[WireError, CoreOperation] =
    decodePieceOperation(value, path)
      .orElse(decodeResourceOperation(value, path))
      .orElse(decodeCardOperation(value, path))
      .orElse(decodeDiceOperation(value, path))
      .orElse(decodeTurnOperation(value, path))
      .orElse(decodeChronicleOperation(value, path))
      .applyOrElse(value("kind").str, (other: String) => Left(InvalidValue(
        s"$path.kind", s"unknown recorded walker operation '$other'")))
