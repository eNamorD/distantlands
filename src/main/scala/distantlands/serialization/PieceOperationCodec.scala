package distantlands.serialization

import distantlands.model._

/** The wire spellings of the [[PieceOperation]] family: its encode
  * match and its decode partial function, chained by
  * [[WalkerOperationCodec]]. */
private[serialization] trait PieceOperationCodec extends CampaignResultCodec
    with FollowUpWire with OathkeeperWire with PositionWire
    with WalkerValueCodec with OperationWireSupport:
  this: GameEventJsonSupport =>
  import WireError._

  protected final def encodePieceOperation(operation: PieceOperation)
      : ujson.Value =
    operation match
      case Move(piece, from, to, orientation, by) => withMover(ujson.Obj(
        "kind" -> "move",
        "piece" -> encodePiece(piece),
        "from" -> encodePositionedLocation(from),
        "to" -> encodePositionedLocation(to),
        "resultingOrientation" -> orientation.fold[ujson.Value](ujson.Null)(
          value => ujson.Str(orientationKey(value)))), by)
      case Take(piece, player, from, to, sourcePosition, _, leaving,
          sourceSide) =>
        // Written only when present, so a Take without it encodes as before.
        val optional: Vector[(String, ujson.Value)] =
          if leaving.isEmpty then Vector.empty
          else Vector("leaving" -> ujson.Arr.from(leaving.map(encodeOperation)))
        ujson.Obj.from(Vector[(String, ujson.Value)](
          "kind" -> "take", "piece" -> encodePiece(piece),
          "playerId" -> player.value, "from" -> encodeLocation(from),
          "to" -> encodeLocation(to),
          "sourcePosition" -> encodeStackPosition(sourcePosition)) ++
          optional ++ sideField("sourceSide", sourceSide))
      case Kill(warbands, from) => ujson.Obj("kind" -> "kill",
        "warbands" -> encodePiece(warbands),
        "from" -> encodePositionedLocation(from))
      case Replace(removed, replacements, at, _, by) => withMover(ujson.Obj(
        "kind" -> "replace", "removed" -> encodePiece(removed),
        "replacements" -> encodePiece(replacements),
        "at" -> encodePositionedLocation(at)), by)
      case Sacrifice(player, warbands, from) => ujson.Obj(
        "kind" -> "sacrifice", "playerId" -> player.value,
        "warbands" -> encodePiece(warbands),
        "from" -> encodePositionedLocation(from))
      case give: Give => encodeGive(give, "give")
      case Exchange(give, receive) => ujson.Obj("kind" -> "exchange",
        "give" -> encodeGive(give, "exchange-give"),
        "receive" -> encodeGive(receive, "exchange-receive"))

  protected final def decodePieceOperation(value: ujson.Value,
      path: String): DecodedOperation =
    case "move" => for
      piece <- decodePiece(value("piece"), s"$path.piece")
      from <- decodePositionedLocation(value("from"), s"$path.from")
      to <- decodePositionedLocation(value("to"), s"$path.to")
      orientation <- value("resultingOrientation") match
        case ujson.Null => Right(None)
        case other => orientationFromKey(other.str,
          s"$path.resultingOrientation").map(Some(_))
    yield Move(piece, from, to, orientation, decodeMover(value))
    case "take" => for
      piece <- decodePiece(value("piece"), s"$path.piece")
      from <- decodeLocation(value("from"), s"$path.from")
      to <- decodeLocation(value("to"), s"$path.to")
      sourcePosition <- decodeStackPosition(value("sourcePosition").str,
        s"$path.sourcePosition")
      leaving <- value.obj.get("leaving").fold[Either[WireError,
          Vector[CoreOperation]]](Right(Vector.empty))(raw =>
        traverse(raw.arr.zipWithIndex.toVector)({ case (operation, index) =>
          decodeOperation(operation, s"$path.leaving[$index]") }))
      sourceSide <- decodeSide(value, "sourceSide", path)
    yield Take(piece, PlayerId(value("playerId").str), from, to,
      sourcePosition, leaving = leaving, sourceSide = sourceSide)
    case "kill" => for
      piece <- decodePiece(value("warbands"), s"$path.warbands")
      warbands <- asWarbands(piece, s"$path.warbands")
      from <- decodePositionedLocation(value("from"), s"$path.from")
    yield Kill(warbands, from)
    case "replace" => for
      removedPiece <- decodePiece(value("removed"), s"$path.removed")
      removed <- asWarbands(removedPiece, s"$path.removed")
      replacementsPiece <- decodePiece(value("replacements"),
        s"$path.replacements")
      replacements <- asWarbands(replacementsPiece, s"$path.replacements")
      at <- decodePositionedLocation(value("at"), s"$path.at")
      _ <- Either.cond(removed.amount == replacements.amount, (),
        InvalidValue(path, "replace must exchange equal numbers of warbands"))
      _ <- Either.cond(removed.kind != replacements.kind, (),
        InvalidValue(path, "replacement warbands must have a new color"))
    yield Replace(removed, replacements, at, by = decodeMover(value))
    case "sacrifice" => for
      piece <- decodePiece(value("warbands"), s"$path.warbands")
      warbands <- asWarbands(piece, s"$path.warbands")
      from <- decodePositionedLocation(value("from"), s"$path.from")
    yield Sacrifice(PlayerId(value("playerId").str), warbands, from)
    case "give" => decodeGive(value, path)
    case "exchange" => for
      give <- decodeGive(value("give"), s"$path.give")
      receive <- decodeGive(value("receive"), s"$path.receive")
    yield Exchange(give, receive)
