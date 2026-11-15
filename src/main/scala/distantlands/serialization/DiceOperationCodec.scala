package distantlands.serialization

import distantlands.model._

/** The wire spellings of the [[DiceOperation]] family: its encode
  * match and its decode partial function, chained by
  * [[WalkerOperationCodec]]. */
private[serialization] trait DiceOperationCodec extends CampaignResultCodec
    with FollowUpWire with OathkeeperWire with PositionWire
    with WalkerValueCodec with OperationWireSupport:
  this: GameEventJsonSupport =>

  protected final def encodeDiceOperation(operation: DiceOperation)
      : ujson.Value =
    operation match
      case ModifyDicePool(pool, delta, _) => ujson.Obj(
        "kind" -> "modify-dice-pool", "pool" -> pool.value,
        "delta" -> delta)
      case Roll(pool, dice, _, _) => ujson.Obj("kind" -> "roll",
        "pool" -> pool.value, "die" -> encodeDiceKind(dice.die))
      case ModifyRollOutcome(pool, skulls, score) => ujson.Obj(
        "kind" -> "modify-roll-outcome", "pool" -> pool.value,
        "skulls" -> skulls.fold[ujson.Value](ujson.Null)(ujson.Num(_)),
        "score" -> score.fold[ujson.Value](ujson.Null)(ujson.Num(_)))
      case Reroll(pool, indices, faces) =>
        val rerolled = ujson.Obj("kind" -> "reroll", "pool" -> pool.value,
          "indices" -> ujson.Arr.from(indices.map(ujson.Num(_))))
        faces.foreach(rolled =>
          rerolled("faces") = ujson.Arr.from(rolled.map(encodeDieFace)))
        rerolled
      case SetDieFaces(pool, faces) => ujson.Obj(
        "kind" -> "set-die-faces", "pool" -> pool.value,
        "faces" -> ujson.Arr.from(faces.map(encodeDieFace)))

  protected final def decodeDiceOperation(value: ujson.Value,
      path: String): DecodedOperation =
    case "modify-dice-pool" =>
      decodeSignedInt(value("delta"), s"$path.delta")
        .map(delta => ModifyDicePool(PoolKey(value("pool").str), delta))
    case "roll" => decodeDiceKind(value("die").str, s"$path.die").map(die =>
      Roll(PoolKey(value("pool").str), DiceSpec(die)))
    case "modify-roll-outcome" => for
      skulls <- decodeOptionalSignedInt(value("skulls"), s"$path.skulls")
      score <- decodeOptionalSignedInt(value("score"), s"$path.score")
    yield ModifyRollOutcome(PoolKey(value("pool").str), skulls, score)
    case "reroll" => for
      indices <- traverse(value("indices").arr.zipWithIndex.toVector)({
        case (index, at) => decodeSignedInt(index, s"$path.indices[$at]") })
      faces <- decodeRerolledFaces(value, path)
    yield Reroll(PoolKey(value("pool").str), indices, faces)
    case "set-die-faces" => traverse(value("faces").arr.toVector)(face =>
      decodeDieFace(face.str, s"$path.faces")).map(faces =>
      SetDieFaces(PoolKey(value("pool").str), faces))

  /** A recorded reroll carries its faces; a declared one has none. */
  private def decodeRerolledFaces(value: ujson.Value, path: String)
      : Either[WireError, Option[Vector[DieFace]]] =
    value.obj.get("faces").fold[Either[WireError, Option[Vector[DieFace]]]](
      Right(None))(faces => traverse(faces.arr.toVector)(face =>
        decodeDieFace(face.str, s"$path.faces")).map(Some(_)))
