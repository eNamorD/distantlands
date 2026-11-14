package distantlands.serialization

import distantlands.model._

/** The wire spellings of the [[TurnOperation]] family: its encode
  * match and its decode partial function, chained by
  * [[WalkerOperationCodec]]. */
private[serialization] trait TurnOperationCodec extends CampaignResultCodec
    with FollowUpWire with OathkeeperWire with PositionWire
    with WalkerValueCodec with OperationWireSupport:
  this: GameEventJsonSupport =>
  import WireError._

  protected final def encodeTurnOperation(operation: TurnOperation)
      : ujson.Value =
    operation match
      case AdvanceVisionsDrawn => ujson.Obj("kind" -> "advance-visions-drawn")
      case AdvanceRound => ujson.Obj("kind" -> "advance-round")
      case UsurperLimiterRemoved =>
        ujson.Obj("kind" -> "usurper-limiter-removed")
      case BeginTurn(player, phase) => ujson.Obj("kind" -> "begin-turn",
        "playerId" -> player.value, "phase" -> phase.key)
      case EnterPhase(phase) => ujson.Obj("kind" -> "enter-phase",
        "phase" -> phase.key)
      case operation: SetOathkeeper => encodeSetOathkeeper(operation)
      case operation: SetOathkeeperGoal => encodeSetOathkeeperGoal(operation)
      // A use limit is journalled as the ref it records, not as the power
      // that asked for it: replay adds the same ref to the same turn without
      // gathering anything.
      case RecordPowerUse(PowerUseRef(timing, source, id)) =>
        ujson.Obj.from(Vector[(String, ujson.Value)](
          "kind" -> "record-power-use",
          "timing" -> encodePowerTiming(timing)) ++ (source match {
          case PowerSourceRef.Site(site) => Vector("siteId" -> ujson.Str(site.value))
          case PowerSourceRef.Card(card) => Vector(
            "cardKind" -> ujson.Str(card.kind), "cardId" -> ujson.Str(card.value))
          case PowerSourceRef.Banner(banner) =>
            Vector("bannerKey" -> ujson.Str(banner.key))
        }) :+ ("powerId" -> ujson.Str(id.value)))
      case RecordCampaignResult(result) => ujson.Obj(
        "kind" -> "record-campaign-result",
        "result" -> encodeCampaignResult(result))
      case QueueProcedure(entry, atFront) => ujson.Obj(
        "kind" -> "queue-procedure",
        "entry" -> encodeQueued(entry),
        "atFront" -> ujson.Bool(atFront))
      // A held op is spelled as a recorded one: it is held canonically.
      case Defer(key, ops) => ujson.Obj("kind" -> "defer",
        "key" -> key.value, "ops" -> ujson.Arr.from(ops.map(encodeOperation)))
      case TakeDeferred(key) => ujson.Obj("kind" -> "take-deferred",
        "key" -> key.value)
      case ActAsIf(site) => ujson.Obj("kind" -> "act-as-if",
        "siteId" -> site.value)
      case ActAsIfAdvisers(player) => ujson.Obj(
        "kind" -> "act-as-if-advisers", "playerId" -> player.value)
      case TrackPawn(holder, target) => ujson.Obj("kind" -> "track-pawn",
        "holder" -> holder.value, "target" -> target.value)
      case ClearTrackedPawn(holder) => ujson.Obj(
        "kind" -> "clear-tracked-pawn", "holder" -> holder.value)
      case SetFestivalMarker(site) => ujson.Obj(
        "kind" -> "set-festival-marker",
        "siteId" -> site.fold[ujson.Value](ujson.Null)(at => ujson.Str(at.value)))
      case FlipBanner(banner) => ujson.Obj("kind" -> "flip-banner",
        "bannerKey" -> banner.key)
      case SetEndDieModifier(amount) => ujson.Obj(
        "kind" -> "set-end-die-modifier", "amount" -> amount)
      case OpenPick(decisionId, option, label) => ujson.Obj(
        "kind" -> "open-pick", "decisionId" -> decisionId,
        "option" -> DecisionAnswerCodec.encodeRef(option),
        "label" -> label.fold[ujson.Value](ujson.Null)(ujson.Str(_)))
      case ClosePick() => ujson.Obj("kind" -> "close-pick")

  protected final def decodeTurnOperation(value: ujson.Value,
      path: String): DecodedOperation =
    case "advance-visions-drawn" => Right(AdvanceVisionsDrawn)
    case "advance-round" => Right(AdvanceRound)
    case "usurper-limiter-removed" => Right(UsurperLimiterRemoved)
    case "begin-turn" =>
      val key = value("phase").str
      Phase.fromKey(key).toRight(
        InvalidValue(s"$path.phase", s"unknown phase '$key'"))
        .map(BeginTurn(PlayerId(value("playerId").str), _))
    case "enter-phase" =>
      val key = value("phase").str
      Phase.fromKey(key).toRight(
        InvalidValue(s"$path.phase", s"unknown phase '$key'"))
        .map(EnterPhase.apply)
    case "set-oathkeeper" => Right(decodeSetOathkeeper(value))
    case "set-oathkeeper-goal" => decodeSetOathkeeperGoal(value, path)
    case "record-power-use" => for
      timing <- decodePowerTiming(value("timing").str, s"$path.timing")
      source <- (if value.obj.contains("siteId") then
          Right(PowerSourceRef.Site(SiteId(value("siteId").str)))
        else if value.obj.contains("bannerKey") then
          Banner.fromKey(value("bannerKey").str).map(PowerSourceRef.Banner(_))
            .toRight(InvalidValue(s"$path.bannerKey", "unknown banner"))
        else GameEventJsonSupport.cardIdOfKind(value("cardKind").str,
          value("cardId").str, s"$path.cardKind").map(PowerSourceRef.Card.apply)
        ): Either[WireError, PowerSourceRef]
    yield RecordPowerUse(PowerUseRef(timing, source,
      PowerId(value("powerId").str)))
    case "record-campaign-result" =>
      decodeCampaignResult(value("result"), s"$path.result")
        .map(RecordCampaignResult(_))
    case "queue-procedure" =>
      decodeQueued(value("entry"), s"$path.entry")
        .map(QueueProcedure(_, value("atFront").bool))
    case "defer" =>
      traverse(value("ops").arr.zipWithIndex.toVector)({
        case (operation, index) =>
          decodeOperation(operation, s"$path.ops[$index]") })
        .map(Defer(DeferralKey(value("key").str), _))
    case "take-deferred" =>
      Right(TakeDeferred(DeferralKey(value("key").str)))
    case "act-as-if" => Right(ActAsIf(SiteId(value("siteId").str)))
    case "act-as-if-advisers" =>
      Right(ActAsIfAdvisers(PlayerId(value("playerId").str)))
    case "track-pawn" => Right(TrackPawn(PlayerId(value("holder").str),
      PlayerId(value("target").str)))
    case "clear-tracked-pawn" =>
      Right(ClearTrackedPawn(PlayerId(value("holder").str)))
    case "set-festival-marker" =>
      Right(SetFestivalMarker(value("siteId").strOpt.map(SiteId(_))))
    case "flip-banner" => Banner.fromKey(value("bannerKey").str)
      .map(FlipBanner(_))
      .toRight(InvalidValue(s"$path.bannerKey", "unknown banner"))
    case "set-end-die-modifier" =>
      decodeSignedInt(value("amount"), s"$path.amount").flatMap(amount =>
        if amount >= -1 && amount <= 3 then Right(SetEndDieModifier(amount))
        else Left(InvalidValue(s"$path.amount",
          "an end die modifier runs from -1 to +3")))
    case "open-pick" => DecisionAnswerCodec.decodeRef(value("option"),
      s"$path.option").map(OpenPick(value("decisionId").str, _,
        value("label").strOpt))
    case "close-pick" => Right(ClosePick())

  private def encodePowerTiming(timing: PowerTiming): String = timing match
    case PowerTiming.Wake => "wake"
    case PowerTiming.Act => "act"
    case PowerTiming.Rest => "rest"

  private def decodePowerTiming(value: String,
      path: String): Either[WireError, PowerTiming] = value match
    case "wake" => Right(PowerTiming.Wake)
    case "act" => Right(PowerTiming.Act)
    case "rest" => Right(PowerTiming.Rest)
    case other => Left(InvalidValue(path, s"unknown power timing '$other'"))
