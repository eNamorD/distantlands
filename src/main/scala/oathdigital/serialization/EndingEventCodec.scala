package oathdigital.serialization

import oathdigital.model._
import oathdigital.model.OathEvent._

private[serialization] trait EndingEventCodec { this: GameEventJsonSupport =>
  import GameEventWire._
  import WireError._

  protected final val endingDiscriminator: PartialFunction[OathEvent, String] =
      case _: RoundEnded => RoundEndedType
      case _: WarExhaustionResolved => WarExhaustionResolvedType
      case _: EndDieRolled => EndDieRolledType
      case _: BanditsRefilled => BanditsRefilledType
      case _: UsurperFlipped => UsurperFlippedType
      case _: UsurperVictory => UsurperVictoryType
      case _: VisionVictory => VisionVictoryType

  protected final val endingEncoder: PartialFunction[OathEvent, ujson.Value] =
      case RoundEnded(completed, next) => ujson.Obj(
        "completedRound" -> completed,
        "nextRound" -> next.fold[ujson.Value](ujson.Null)(ujson.Num(_)))
      case WarExhaustionResolved(winner, kind, vision, candidates) => ujson.Obj(
        "winnerPlayerId" -> winner.value,
        "victoryKind" -> kind.key,
        "visionId" -> vision.fold[ujson.Value](ujson.Null)(v => ujson.Str(v.value)),
        "randomCandidatePlayerIds" -> stringArray(candidates.map(_.value)))
      case EndDieRolled(round, face, result) => ujson.Obj(
        "round" -> round,
        "face" -> face,
        "winnerPlayerId" -> result.fold[ujson.Value](ujson.Null)(r =>
          ujson.Str(r.winner.value)),
        "victoryKind" -> result.fold[ujson.Value](ujson.Null)(r =>
          ujson.Str(r.kind.key)))
      case BanditsRefilled(sites) => ujson.Obj("sites" -> ujson.Arr.from(
        sites.map { case (site, count) =>
          ujson.Obj("siteId" -> site.value, "count" -> count)
        }))
      case UsurperFlipped(player) => ujson.Obj("playerId" -> player.value)
      case UsurperVictory(player) => ujson.Obj("playerId" -> player.value)
      case VisionVictory(player, vision) => ujson.Obj(
        "playerId" -> player.value, "visionId" -> vision.value)

  protected final def victoryKind(value: ujson.Value, path: String)
      : Either[WireError, VictoryKind] =
    value match
      case ujson.Str(key) => VictoryKind.values.find(_.key == key).toRight(
        InvalidValue(path, s"unknown victory kind $key"))
      case other => Left(InvalidValue(path, s"victory kind is not a string: $other"))

  protected final def endingDecode(eventType: String, payload: ujson.Value,
      path: String): Option[Either[WireError, OathEvent]] =
    val decoder: PartialFunction[String, Either[WireError, OathEvent]] =
        case RoundEndedType => for
          completed <- safeIntField(payload.obj, "completedRound", path)
          next <- payload("nextRound") match
            case ujson.Null => Right(None)
            case value => safeInt(value, s"$path.nextRound").map(Some(_))
        yield RoundEnded(completed, next)
        case WarExhaustionResolvedType => for
          kind <- victoryKind(payload("victoryKind"), s"$path.victoryKind")
        yield WarExhaustionResolved(PlayerId(payload("winnerPlayerId").str),
          kind, payload("visionId") match {
            case ujson.Null => None
            case value => Some(VisionId(value.str))
          }, payload("randomCandidatePlayerIds").arr.toVector.map(v => PlayerId(v.str)))
        case EndDieRolledType => for
          round <- safeIntField(payload.obj, "round", path)
          face <- safeIntField(payload.obj, "face", path)
          result <- (payload("winnerPlayerId"), payload("victoryKind")) match
            case (ujson.Null, ujson.Null) => Right(None)
            case (ujson.Null, _) | (_, ujson.Null) => Left(InvalidValue(path,
              "an end die names a winner and a victory kind together"))
            case (winner, kind) =>
              victoryKind(kind, s"$path.victoryKind").map(k =>
                Some(GameResult(PlayerId(winner.str), k)))
        yield EndDieRolled(round, face, result)
        case BanditsRefilledType => for
          sites <- traverse(payload("sites").arr.toVector) { value => for
            count <- safeIntField(value.obj, "count", s"$path.sites")
          yield SiteId(value("siteId").str) -> count }
        yield BanditsRefilled(sites)
        case UsurperFlippedType =>
          Right(UsurperFlipped(PlayerId(payload("playerId").str)))
        case UsurperVictoryType =>
          Right(UsurperVictory(PlayerId(payload("playerId").str)))
        case VisionVictoryType => Right(VisionVictory(
          PlayerId(payload("playerId").str), VisionId(payload("visionId").str)))
    decoder.lift(eventType)
}
