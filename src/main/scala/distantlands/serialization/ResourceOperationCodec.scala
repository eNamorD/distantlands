package distantlands.serialization

import distantlands.model._

/** The wire spellings of the [[ResourceOperation]] family: its encode
  * match and its decode partial function, chained by
  * [[WalkerOperationCodec]]. */
private[serialization] trait ResourceOperationCodec extends CampaignResultCodec
    with FollowUpWire with OathkeeperWire with PositionWire
    with WalkerValueCodec with OperationWireSupport:
  this: GameEventJsonSupport =>
  import WireError._

  protected final def encodeResourceOperation(operation: ResourceOperation)
      : ujson.Value =
    operation match
      case burn: Burn => ujson.Obj("kind" -> "burn",
        "resource" -> encodePiece(burn.resource),
        "from" -> encodePositionedLocation(burn.from),
        "by" -> burn.by.fold[ujson.Value](ujson.Null)(
          by => ujson.Str(by.value)))
      case DrawNomadFavor(player, amount) => ujson.Obj(
        "kind" -> "draw-nomad-favor", "playerId" -> player.value,
        "amount" -> amount)
      case PayCost(player, placedAt, cost, intoOccupied, matchingBank, _) =>
        val optional: Vector[(String, ujson.Value)] =
          (if intoOccupied then Vector("intoOccupied" -> (ujson.Bool(true): ujson.Value))
          else Vector.empty) ++
            matchingBank.toVector.map(suit =>
              "matchingBank" -> (ujson.Str(suit.key): ujson.Value))
        ujson.Obj.from(Vector[(String, ujson.Value)](
          "kind" -> "pay-cost", "playerId" -> player.value,
          "placedAt" -> encodeLocation(placedAt),
          "cost" -> encodeCost(cost)) ++ optional)
      case SpendSupply(player, amount, _) => ujson.Obj(
        "kind" -> "spend-supply", "playerId" -> player.value,
        "amount" -> amount)
      case GainSupply(player, amount) => ujson.Obj(
        "kind" -> "gain-supply", "playerId" -> player.value,
        "amount" -> amount)
      case FlipSecrets(player, amount, from, to) => ujson.Obj(
        "kind" -> "flip-secrets", "playerId" -> player.value,
        "amount" -> amount, "from" -> encodeSecretSide(from),
        "to" -> encodeSecretSide(to))
      case Gain.Favor(player, suit, amount) => ujson.Obj(
        "kind" -> "gain-favor", "playerId" -> player.value,
        "suit" -> suit.key, "amount" -> amount)
      case Gain.Secrets(player, amount) => ujson.Obj(
        "kind" -> "gain-secrets", "playerId" -> player.value,
        "amount" -> amount)
      case Gain.Warbands(player, kind, amount) => ujson.Obj(
        "kind" -> "gain-warbands", "playerId" -> player.value,
        "force" -> encodeForceKind(kind), "amount" -> amount)

  protected final def decodeResourceOperation(value: ujson.Value,
      path: String): DecodedOperation =
    case "draw-nomad-favor" =>
      decodePositiveInt(value("amount"), s"$path.amount")
        .map(amount => DrawNomadFavor(PlayerId(value("playerId").str), amount))
    case "burn" => for
      resource <- decodePiece(value("resource"), s"$path.resource")
      from <- decodePositionedLocation(value("from"), s"$path.from")
      by = value("by").strOpt.map(PlayerId(_))
      burn <- resource match
        case Piece.Favor(amount) => Right(Burn.favor(amount, from, by))
        case Piece.Secrets(amount) => Right(Burn.secrets(amount, from, by))
        case other => Left(InvalidValue(s"$path.resource",
          s"unsupported burn resource $other"))
    yield burn
    case "pay-cost" => for
      placedAt <- decodeLocation(value("placedAt"), s"$path.placedAt")
      cost <- decodeCost(value("cost"), s"$path.cost")
      bank <- value.obj.get("matchingBank") match
        case None | Some(ujson.Null) => Right(None)
        case Some(raw) => decodeSuit(raw.str, s"$path.matchingBank").map(Some(_))
    yield PayCost(PlayerId(value("playerId").str), placedAt, cost,
      intoOccupied = value.obj.get("intoOccupied").exists(_.bool),
      matchingBank = bank)
    case "spend-supply" => decodePositiveInt(value("amount"), s"$path.amount")
      .map(amount => SpendSupply(PlayerId(value("playerId").str), amount))
    case "gain-supply" => decodePositiveInt(value("amount"), s"$path.amount")
      .map(amount => GainSupply(PlayerId(value("playerId").str), amount))
    case "flip-secrets" => for
      amount <- safeIntField(value.obj, "amount", path)
      from <- decodeSecretSide(value("from").str, s"$path.from")
      to <- decodeSecretSide(value("to").str, s"$path.to")
    yield FlipSecrets(PlayerId(value("playerId").str), amount, from, to)
    case "gain-favor" => for
      amount <- safeIntField(value.obj, "amount", path)
      suit <- decodeSuit(value("suit").str, s"$path.suit")
    yield Gain.Favor(PlayerId(value("playerId").str), suit, amount)
    case "gain-secrets" => safeIntField(value.obj, "amount", path).map(
      amount => Gain.Secrets(PlayerId(value("playerId").str), amount))
    case "gain-warbands" => for
      force <- decodeForceKind(value("force"), s"$path.force")
      amount <- safeIntField(value.obj, "amount", path)
    yield Gain.Warbands(PlayerId(value("playerId").str), force, amount)
