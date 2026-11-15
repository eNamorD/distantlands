package distantlands.serialization

import distantlands.model._

/** The wire spellings of the [[CardOperation]] family: its encode
  * match and its decode partial function, chained by
  * [[WalkerOperationCodec]]. */
private[serialization] trait CardOperationCodec extends CampaignResultCodec
    with FollowUpWire with OathkeeperWire with PositionWire
    with WalkerValueCodec with OperationWireSupport:
  this: GameEventJsonSupport =>
  import WireError._

  protected final def encodeCardOperation(operation: CardOperation)
      : ujson.Value =
    operation match
      case Peek(viewer, card, at) => ujson.Obj("kind" -> "peek",
        "viewerPlayerId" -> viewer.value, "card" -> encodeCardId(card),
        "at" -> encodeLocation(at))
      case Flip(card, at, orientation) => ujson.Obj("kind" -> "flip",
        "card" -> encodeCardId(card), "at" -> encodeLocation(at),
        "orientation" -> orientationKey(orientation))
      case bury: Bury => ujson.Obj("kind" -> "bury",
        "card" -> encodeBuryableCard(bury.card),
        "from" -> encodePositionedLocation(bury.from))
      case Discard.Denizen(card, from, to, suit, favor, secrets, actingPlayer,
          _, actorless, position, cause) =>
        withCause(withActorless(ujson.Obj("kind" -> "discard-denizen",
          "card" -> card.value,
          "from" -> encodePositionedLocation(from), "to" -> to.key,
          "position" -> encodeStackPosition(position),
          "suit" -> suit.key, "favor" -> favor, "secrets" -> secrets,
          "actingPlayerId" -> actingPlayer.value), actorless), cause)
      case Discard.Vision(card, from, to, _, position) => ujson.Obj(
        "kind" -> "discard-vision", "card" -> card.value,
        "from" -> encodePositionedLocation(from), "to" -> to.key,
        "position" -> encodeStackPosition(position))
      case Discard.RuinedEdifice(card, from, suit, favor, secrets,
          actingPlayer, _, actorless, cause) =>
        withCause(withActorless(ujson.Obj("kind" -> "discard-ruined-edifice",
          "card" -> card.value,
          "from" -> encodePositionedLocation(from), "suit" -> suit.key,
          "favor" -> favor, "secrets" -> secrets,
          "actingPlayerId" -> actingPlayer.value), actorless), cause)
      case Discard.Buried(card, from, suit, favor, secrets, actingPlayer, _) =>
        val encoded = ujson.Obj("kind" -> "discard-buried",
          "card" -> encodeBuryableCard(card),
          "from" -> encodePositionedLocation(from),
          "favor" -> favor, "secrets" -> secrets,
          "actingPlayerId" -> actingPlayer.value)
        suit.foreach(bank => encoded("suit") = bank.key)
        encoded
      case Discard.Relic(card, from, secrets, actingPlayer) => ujson.Obj(
        "kind" -> "discard-relic", "card" -> card.value,
        "from" -> encodePositionedLocation(from), "secrets" -> secrets,
        "actingPlayerId" -> actingPlayer.value)
      case RemoveFromMap(card, from, to, suit, favor, secrets, actingPlayer) =>
        ujson.Obj("kind" -> "remove-from-map", "card" -> encodeCardId(card),
          "from" -> encodePositionedLocation(from),
          "to" -> encodePositionedLocation(to), "suit" -> suit.key,
          "favor" -> favor, "secrets" -> secrets,
          "actingPlayerId" -> actingPlayer.value)
      case Draw(player, cards, source, destination, position) => ujson.Obj(
        "kind" -> "draw", "playerId" -> player.value,
        "cards" -> ujson.Arr.from(cards.map(encodeCardId)),
        "source" -> encodeLocation(source),
        "destination" -> encodeLocation(destination),
        "position" -> encodeStackPosition(position))
      case Play(card, from, destination, orientation, _) => ujson.Obj(
        "kind" -> "play", "card" -> encodeCardId(card),
        "from" -> encodePositionedLocation(from),
        "destination" -> encodeLocation(destination),
        "orientation" -> orientationKey(orientation))
      case Reveal(card, at) => ujson.Obj("kind" -> "reveal",
        "card" -> encodeCardId(card), "at" -> encodeLocation(at))
      case Swap(firstCard, firstLocation, secondCard, secondLocation, by) =>
        withMover(ujson.Obj("kind" -> "swap",
          "firstCard" -> encodeCardId(firstCard),
          "firstLocation" -> encodePositionedLocation(firstLocation),
          "secondCard" -> encodeCardId(secondCard),
          "secondLocation" -> encodePositionedLocation(secondLocation)), by)
      case Shuffle(pile, order) =>
        val shuffled = ujson.Obj("kind" -> "shuffle",
          "pile" -> encodeShufflePile(pile))
        order.foreach(cards =>
          shuffled("order") = ujson.Arr.from(cards.map(encodeCardId)))
        shuffled

  protected final def decodeCardOperation(value: ujson.Value,
      path: String): DecodedOperation =
    case "peek" => for
      card <- decodeCardRef(value("card"), s"$path.card")
      at <- decodeLocation(value("at"), s"$path.at")
    yield Peek(PlayerId(value("viewerPlayerId").str), card, at)
    case "flip" => for
      card <- decodeCardRef(value("card"), s"$path.card")
      at <- decodeLocation(value("at"), s"$path.at")
      orientation <- orientationFromKey(value("orientation").str,
        s"$path.orientation")
    yield Flip(card, at, orientation)
    case "bury" => for
      card <- decodeBuryableCard(value("card"), s"$path.card")
      from <- decodePositionedLocation(value("from"), s"$path.from")
    yield Bury(card, from)
    case "discard-denizen" => for
      from <- decodePositionedLocation(value("from"), s"$path.from")
      to <- decodeRegion(value("to").str, s"$path.to")
      position <- discardPosition(value, path)
      suit <- decodeSuit(value("suit").str, s"$path.suit")
      favor <- safeIntField(value.obj, "favor", path)
      secrets <- safeIntField(value.obj, "secrets", path)
      cause <- decodeCause(value, path)
    yield Discard.Denizen(DenizenId(value("card").str), from, to, suit,
      favor, secrets, PlayerId(value("actingPlayerId").str),
      actorless = decodeActorless(value), position = position, cause = cause)
    case "discard-vision" => for
      from <- decodePositionedLocation(value("from"), s"$path.from")
      to <- decodeRegion(value("to").str, s"$path.to")
      position <- discardPosition(value, path)
    yield Discard.Vision(VisionId(value("card").str), from, to,
      position = position)
    case "discard-ruined-edifice" => for
      from <- decodePositionedLocation(value("from"), s"$path.from")
      suit <- decodeSuit(value("suit").str, s"$path.suit")
      favor <- safeIntField(value.obj, "favor", path)
      secrets <- safeIntField(value.obj, "secrets", path)
      cause <- decodeCause(value, path)
    yield Discard.RuinedEdifice(EdificeId(value("card").str), from, suit,
      favor, secrets, PlayerId(value("actingPlayerId").str),
      actorless = decodeActorless(value), cause = cause)
    case "discard-buried" => for
      card <- decodeBuryableCard(value("card"), s"$path.card")
      from <- decodePositionedLocation(value("from"), s"$path.from")
      suit <- value.obj.get("suit").fold[Either[WireError, Option[Suit]]](
        Right(None))(found => decodeSuit(found.str, s"$path.suit").map(Some(_)))
      favor <- safeIntField(value.obj, "favor", path)
      secrets <- safeIntField(value.obj, "secrets", path)
    yield Discard.Buried(card, from, suit, favor, secrets,
      PlayerId(value("actingPlayerId").str))
    case "discard-relic" => for
      from <- decodePositionedLocation(value("from"), s"$path.from")
      secrets <- safeIntField(value.obj, "secrets", path)
    yield Discard.Relic(RelicId(value("card").str), from, secrets,
      PlayerId(value("actingPlayerId").str))
    case "remove-from-map" => for
      card <- decodeCardRef(value("card"), s"$path.card")
      from <- decodePositionedLocation(value("from"), s"$path.from")
      to <- decodePositionedLocation(value("to"), s"$path.to")
      suit <- decodeSuit(value("suit").str, s"$path.suit")
      favor <- safeIntField(value.obj, "favor", path)
      secrets <- safeIntField(value.obj, "secrets", path)
    yield RemoveFromMap(card, from, to, suit, favor, secrets,
      PlayerId(value("actingPlayerId").str))
    case "draw" => for
      cards <- traverse(value("cards").arr.zipWithIndex.toVector):
        case (id, index) => decodeCardRef(id, s"$path.cards[$index]")
      _ <- Either.cond(cards.nonEmpty, (), InvalidValue(s"$path.cards",
        "draw must contain at least one card"))
      source <- decodeLocation(value("source"), s"$path.source")
      destination <- decodeLocation(value("destination"), s"$path.destination")
      position <- value.obj.get("position").fold[Either[WireError,
          StackPosition]](Right(StackPosition.Top))(found =>
        decodeStackPosition(found.str, s"$path.position"))
      _ <- Either.cond(position != StackPosition.Unspecified, (),
        InvalidValue(s"$path.position",
          "a draw takes from the top or the bottom"))
    yield Draw(PlayerId(value("playerId").str), cards, source, destination,
      position)
    case "play" => for
      card <- decodeCardRef(value("card"), s"$path.card")
      from <- decodePositionedLocation(value("from"), s"$path.from")
      destination <- decodeLocation(value("destination"), s"$path.destination")
      _ <- destination match
        case _: Location.Site | _: Location.PlayArea => Right(())
        case other => Left(InvalidValue(s"$path.destination",
          s"play destination must be a site or a play area, got $other"))
      orientation <- orientationFromKey(value("orientation").str,
        s"$path.orientation")
    yield Play(card, from, destination, orientation)
    case "reveal" => for
      card <- decodeCardRef(value("card"), s"$path.card")
      at <- decodeLocation(value("at"), s"$path.at")
    yield Reveal(card, at)
    case "swap" => for
      firstCard <- decodeCardRef(value("firstCard"), s"$path.firstCard")
      firstLocation <- decodePositionedLocation(value("firstLocation"),
        s"$path.firstLocation")
      secondCard <- decodeCardRef(value("secondCard"), s"$path.secondCard")
      secondLocation <- decodePositionedLocation(value("secondLocation"),
        s"$path.secondLocation")
      _ <- Either.cond(firstCard != secondCard, (),
        InvalidValue(path, "swap requires two different cards"))
      _ <- Either.cond(firstLocation != secondLocation, (),
        InvalidValue(path, "swap requires two different locations"))
    yield Swap(firstCard, firstLocation, secondCard, secondLocation,
      decodeMover(value))
    case "shuffle" => for
      pile <- decodeShufflePile(value("pile"), s"$path.pile")
      order <- decodeShuffleOrder(value, path)
    yield Shuffle(pile, order)

  /** `encoded`, with `"actorless": true` when nobody performs the
    * discard. Written only then, so other discards encode as before. */
  private def withActorless(encoded: ujson.Obj, actorless: Boolean)
      : ujson.Obj =
    if actorless then encoded("actorless") = true
    encoded

  /** Whether a discard says nobody performs it. */
  private def decodeActorless(value: ujson.Value): Boolean =
    value.obj.get("actorless").exists(_.bool)

  /** A discard's cause, written only when it has one. */
  private def withCause(encoded: ujson.Obj, cause: Option[DiscardCause])
      : ujson.Obj =
    cause.foreach(found => encoded("cause") = ujson.Obj(
      "powerId" -> found.power.value, "makesRoom" -> found.makesRoom))
    encoded

  /** A discard's cause, when it names one. */
  private def decodeCause(value: ujson.Value, path: String)
      : Either[WireError, Option[DiscardCause]] =
    value.obj.get("cause").fold[Either[WireError, Option[DiscardCause]]](
        Right(None)) { found =>
      found.obj.get("powerId").map(_.str).flatMap(PowerId.fromValue)
        .toRight(InvalidValue(s"$path.cause.powerId",
          "a discard's cause names its power"))
        .map(power => Some(DiscardCause(power,
          found.obj.get("makesRoom").exists(_.bool))))
    }

  /** A discard's end of its pile. Required: a journal written before the
    * field existed need not decode. */
  private def discardPosition(value: ujson.Value, path: String)
      : Either[WireError, StackPosition] = for
    found <- value.obj.get("position").toRight(InvalidValue(
      s"$path.position", "a discard names the top or the bottom of its pile"))
    position <- decodeStackPosition(found.str, s"$path.position")
    _ <- Either.cond(position != StackPosition.Unspecified, (),
      InvalidValue(s"$path.position",
        "a discard goes to the top or the bottom of its pile"))
  yield position

  /** A Search pile keeps its Search source's spelling; the edifice deck and
    * the set-aside relics have kinds of their own. */
  private def encodeShufflePile(pile: ShufflePile): ujson.Value = pile match
    case ShufflePile.Search(source) => encodeSearchSource(source)
    case ShufflePile.EdificeDeck => ujson.Obj("kind" -> "edifice-deck")
    case ShufflePile.SetAsideRelics => ujson.Obj("kind" -> "set-aside-relics")
    case ShufflePile.CardGathering =>
      ujson.Obj("kind" -> "card-gathering")
    case ShufflePile.SuitedReserve(suit) =>
      ujson.Obj("kind" -> "suited-reserve", "suit" -> suit.key)
    case ShufflePile.Reliquary => ujson.Obj("kind" -> "reliquary")
    case ShufflePile.Dispossessed => ujson.Obj("kind" -> "dispossessed")
    case ShufflePile.VisionPackets => ujson.Obj("kind" -> "vision-packets")

  private def decodeShufflePile(value: ujson.Value, path: String)
      : Either[WireError, ShufflePile] =
    value.objOpt.flatMap(_.get("kind")).flatMap(_.strOpt) match
      case Some("edifice-deck") => Right(ShufflePile.EdificeDeck)
      case Some("set-aside-relics") => Right(ShufflePile.SetAsideRelics)
      case Some("card-gathering") => Right(ShufflePile.CardGathering)
      case Some("reliquary") => Right(ShufflePile.Reliquary)
      case Some("dispossessed") => Right(ShufflePile.Dispossessed)
      case Some("vision-packets") => Right(ShufflePile.VisionPackets)
      case Some("suited-reserve") => decodeSuit(value("suit").str,
        s"$path.suit").map(ShufflePile.SuitedReserve.apply)
      case _ => decodeSearchSource(value, path).map(ShufflePile.Search.apply)

  /** A recorded shuffle carries its order; a declared one has none. */
  private def decodeShuffleOrder(value: ujson.Value, path: String)
      : Either[WireError, Option[Vector[CardId]]] =
    value.obj.get("order").fold[Either[WireError, Option[Vector[CardId]]]](
      Right(None))(cards => traverse(cards.arr.zipWithIndex.toVector)({
        case (card, index) => decodeCardId(card, s"$path.order[$index]")
      }).map(Some(_)))
