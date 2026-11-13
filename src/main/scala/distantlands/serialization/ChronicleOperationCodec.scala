package distantlands.serialization

import distantlands.model._

/** The wire spellings of the [[ChronicleOperation]] family: its encode
  * match and its decode partial function, chained by
  * [[WalkerOperationCodec]]. */
private[serialization] trait ChronicleOperationCodec extends CampaignResultCodec
    with FollowUpWire with OathkeeperWire with PositionWire
    with WalkerValueCodec with OperationWireSupport:
  this: GameEventJsonSupport =>
  import WireError._

  protected final def encodeChronicleOperation(operation: ChronicleOperation)
      : ujson.Value =
    operation match
      case RoleChanged(lineage, role) => ujson.Obj("kind" -> "role-changed",
        "lineage" -> lineage.value, "role" -> encodeRole(role))
      case ChronicleTaskAssigned(task, player) => ujson.Obj(
        "kind" -> "chronicle-task-assigned", "task" -> task.key,
        "player" -> player.value)
      case GrandScepterFromBox(player) => ujson.Obj(
        "kind" -> "grand-scepter-from-box", "player" -> player.value)
      case ImperialWarbandsTaken(count) => ujson.Obj(
        "kind" -> "imperial-warbands-taken", "count" -> count)
      case WarbandsAdded(force, count) => ujson.Obj(
        "kind" -> "warbands-added", "force" -> encodeForceKind(force),
        "count" -> count)
      case LineagePointsScored(lineage, points) => ujson.Obj(
        "kind" -> "lineage-points-scored", "lineage" -> lineage.value,
        "points" -> points)
      case EdificeBuilt(edifice, site) => ujson.Obj("kind" -> "edifice-built",
        "edifice" -> edifice.value, "site" -> site.value)
      case FoundationRestored(number) => ujson.Obj(
        "kind" -> "foundation-restored", "number" -> number.value)
      case SuitedReserveFromBox(suit, cards) => ujson.Obj(
        "kind" -> "suited-reserve-from-box", "suit" -> suit.key,
        "cards" -> cards.map(card => ujson.Str(card.value)))
      case PlaceAtlasSite(end, region, capacity, relicSlots, resources) =>
        ujson.Obj("kind" -> "place-atlas-site", "end" -> end.key,
          "region" -> region.key, "capacity" -> capacity,
          "relicSlots" -> relicSlots, "resources" -> encodeTokens(resources))
      case ChronicleUpdated(chronicle) => ujson.Obj(
        "kind" -> "chronicle-updated", "chronicle" -> encodeChronicle(chronicle))

  protected final def decodeChronicleOperation(value: ujson.Value,
      path: String): DecodedOperation =
    case "role-changed" =>
      decodeRole(value("role").str, s"$path.role")
        .map(RoleChanged(LineageId(value("lineage").str), _))
    case "chronicle-task-assigned" =>
      val key = value("task").str
      ChronicleTask.fromKey(key).toRight(
        InvalidValue(s"$path.task", s"unknown Chronicle task '$key'"))
        .map(ChronicleTaskAssigned(_, PlayerId(value("player").str)))
    case "grand-scepter-from-box" =>
      Right(GrandScepterFromBox(PlayerId(value("player").str)))
    case "imperial-warbands-taken" => for
      count <- positive(value, "count", path)
    yield ImperialWarbandsTaken(count)
    case "warbands-added" => for
      force <- decodeForceKind(value("force"), s"$path.force")
      count <- positive(value, "count", path)
    yield WarbandsAdded(force, count)
    case "lineage-points-scored" => for
      points <- positive(value, "points", path)
    yield LineagePointsScored(LineageId(value("lineage").str), points)
    case "edifice-built" => Right(EdificeBuilt(
      EdificeId(value("edifice").str), SiteId(value("site").str)))
    case "foundation-restored" => for
      number <- safeIntField(value.obj, "number", path)
      foundation <- FoundationNumber.all.find(_.value == number).toRight(
        InvalidValue(s"$path.number", s"unknown Foundation number $number"))
    yield FoundationRestored(foundation)
    case "suited-reserve-from-box" => for
      suit <- decodeSuit(value("suit").str, s"$path.suit")
      cards = value("cards").arr.toVector.map(card => DenizenId(card.str))
      _ <- Either.cond(cards.nonEmpty, (), InvalidValue(s"$path.cards",
        "a suited reserve takes at least one boxed card"))
    yield SuitedReserveFromBox(suit, cards)
    case "place-atlas-site" =>
      val end = value("end").str
      for
        from <- AtlasEnd.fromKey(end).toRight(
          InvalidValue(s"$path.end", s"unknown Atlas end '$end'"))
        region <- decodeRegion(value("region").str, s"$path.region")
        capacity <- nonNegative(value, "capacity", path)
        relicSlots <- nonNegative(value, "relicSlots", path)
        resources <- decodeTokens(value("resources"), s"$path.resources")
      yield PlaceAtlasSite(from, region, capacity, relicSlots, resources)
    case "chronicle-updated" =>
      decodeChronicle(value("chronicle"), s"$path.chronicle")
        .map(ChronicleUpdated.apply)

  private def positive(value: ujson.Value, name: String, path: String)
      : Either[WireError, Int] =
    safeIntField(value.obj, name, path).flatMap(number => Either.cond(
      number > 0, number, InvalidValue(s"$path.$name", "must be positive")))

  private def nonNegative(value: ujson.Value, name: String, path: String)
      : Either[WireError, Int] =
    safeIntField(value.obj, name, path).flatMap(number => Either.cond(
      number >= 0, number,
      InvalidValue(s"$path.$name", "must not be negative")))
