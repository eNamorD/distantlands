package oathdigital.application.chronicle

import oathdigital.application.FirstGameBootstrapMapper.lineageOf
import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

import TtsWire.sequence

/** A TTS export string and the Chronicle fields it could not hold. */
final case class TtsExport(text: String, omitted: Vector[String])

/** A Chronicle to and from the TTS export string (Chronicle TTS codec
  * design). Decoding fills what TTS does not carry: Oathkeeper goal
  * Supremacy unless a version 3 string sets it, era target 20 with no
  * scores, no previous players, no Foundation alteration sources. Encoding
  * writes version 3 and lists what it left out. */
final class ChronicleTtsCodec(catalog: ExecutableCatalog):
  private val ids = TtsIds(catalog)
  private val colors: Vector[PlayerColor] =
    TtsWire.colors.map(key => PlayerColor.fromKey(key).get)
  private val defaultEra = EraState(20, Vector.empty)
  // The version 3 goal digits; 0 is unset, 5-20 reserved, 21+ fan content.
  private val goalDigits: Vector[(Int, OathkeeperGoal)] = Vector(
    1 -> OathkeeperGoal.Supremacy, 2 -> OathkeeperGoal.Devotion,
    3 -> OathkeeperGoal.ThePeople, 4 -> OathkeeperGoal.Protection)
  private val roles = Vector(Role.Exile, Role.Citizen, Role.Chancellor)

  def decode(text: String): Either[String, Chronicle] =
    TtsWire.decodeExport(text).flatMap((_, tts) => fromTts(tts))

  def encode(chronicle: Chronicle): Either[String, TtsExport] =
    toTts(chronicle).flatMap(TtsWire.encodeExport(_, 3))
      .map(TtsExport(_, omitted(chronicle)))

  /** Versions differ only in the goal digit, which `tts` already holds. */
  private[chronicle] def fromTts(tts: TtsChronicle): Either[String, Chronicle] =
    val empty = TtsFoundations(0, None)
    for
      atlasBox <- sites(tts.atlasBox.getOrElse(Vector.empty))
      world <- sites(tts.world.getOrElse(Vector.empty))
      worldDeck <- denizens(tts.worldDeck.getOrElse(Vector.empty))
      relicDeck <- relics(tts.relicDeck.getOrElse(Vector.empty))
      dispossessed <- denizens(tts.dispossessed.getOrElse(Vector.empty))
      reliquary <- relics(tts.reliquary.getOrElse(Vector.empty))
      foundations = tts.foundations.getOrElse(empty)
      goal <- oathkeeperGoal(foundations.goal)
      lineages <- sequence(colors.zip(tts.players).collect {
        case (color, Some(player)) if !emptyExile(player) =>
          lineage(color, player)
      })
    yield Chronicle(atlasBox, world, worldDeck, relicDeck, defaultEra, goal,
      dispossessed, reliquary, FoundationNumber.all.zipWithIndex.collect {
        case (number, bit) if (foundations.mask & (1 << bit)) != 0 =>
          number -> FoundationState(FoundationFace.Altered, Set.empty)
      }.toMap, lineages)

  private def toTts(chronicle: Chronicle): Either[String, TtsChronicle] =
    val byColor = colors.map(color => lineageOf(color) -> color).toMap
    for
      _ <- chronicle.lineages.find(l => !byColor.contains(l.id)).toLeft(())
        .left.map(l => s"lineage ${l.id.value} has no TTS color")
      atlasBox <- sequence(chronicle.atlasBox.map(ttsSite))
      world <- sequence(chronicle.world.map(ttsSite))
      worldDeck <- sequence(chronicle.worldDeck.map(ids.ttsCard))
      relicDeck <- sequence(chronicle.relicDeck.map(ids.ttsCard))
      dispossessed <- sequence(chronicle.dispossessed.map(ids.ttsCard))
      reliquary <- sequence(chronicle.reliquary.map(ids.ttsCard))
      players <- sequence(colors.map(color =>
        chronicle.lineages.find(_.id == lineageOf(color))
          .fold(Right(TtsPlayer(0, Vector.empty, Vector.empty, Vector.empty)))(
            ttsPlayer)))
    yield TtsChronicle(Some(atlasBox), Some(world), Some(worldDeck),
      Some(relicDeck), Some(dispossessed), Some(reliquary),
      Some(TtsFoundations(FoundationNumber.all.zipWithIndex.collect {
        case (number, bit) if chronicle.foundations.get(number)
            .exists(_.face == FoundationFace.Altered) => 1 << bit
      }.sum, goalDigits.find(_._2 == chronicle.oathkeeperGoal).map(_._1))),
      players.map(Some(_)))

  private def omitted(chronicle: Chronicle): Vector[String] = Vector(
    Option.when(chronicle.era.targetScore != defaultEra.targetScore)(
      "the era target"),
    Option.when(chronicle.era.games.nonEmpty)("the era scorepad"),
    Option.when(chronicle.lineages.exists(_.previousPlayer.nonEmpty))(
      "previous players"),
    Option.when(chronicle.foundations.values
      .exists(_.alterationSources.nonEmpty))(
      "Foundation alteration sources"),
    // An empty Exile section reads back as no lineage.
    Option.when(chronicle.lineages.exists(l => l.role == Role.Exile &&
      l.startingAdvisers.isEmpty && l.legacies.isEmpty &&
      l.startingRelics.isEmpty))("Exile lineages with nothing kept")).flatten

  private def sites(tts: Vector[TtsSite]): Either[String, Vector[StoredSite]] =
    sequence(tts.map(site =>
      for
        id <- ids.site(site.index)
        _ <- Either.cond(site.items.size <= StoredSite.capacity, (),
          s"TTS site ${site.index} stores more than ${StoredSite.capacity} items")
        items <- sequence(site.items.map(ids.card))
      yield StoredSite(id, items)))

  private def ttsSite(site: StoredSite): Either[String, TtsSite] =
    for
      index <- ids.ttsSite(site.site)
      items <- sequence(site.items.map(ids.ttsCard))
    yield TtsSite(index, items)

  private def denizens(tts: Vector[Int]): Either[String, Vector[DenizenId]] =
    sequence(tts.map(n => ids.card(n).flatMap {
      case id: DenizenId => Right(id)
      case _ => Left(s"TTS card $n is not a denizen")
    }))

  private def relics(tts: Vector[Int]): Either[String, Vector[RelicId]] =
    sequence(tts.map(n => ids.card(n).flatMap {
      case id: RelicId => Right(id)
      case _ => Left(s"TTS card $n is not a relic")
    }))

  private def oathkeeperGoal(digit: Option[Int])
      : Either[String, OathkeeperGoal] = digit.getOrElse(0) match
    case 0 => Right(OathkeeperGoal.Supremacy)
    case n if n > 20 => Left(s"Oathkeeper goal $n is fan content")
    case n => goalDigits.find(_._1 == n).map(_._2)
      .toRight(s"Oathkeeper goal $n is reserved")

  private def emptyExile(player: TtsPlayer): Boolean =
    player.status == 0 && player.cards.isEmpty && player.legacies.isEmpty &&
      player.relics.isEmpty

  private def lineage(color: PlayerColor, player: TtsPlayer)
      : Either[String, LineageState] =
    for
      role <- roles.lift(player.status)
        .toRight(s"TTS player status ${player.status} is unknown")
      legacies <- sequence(player.legacies.map(l =>
        ids.legacy(l.id).map(LegacyState(_, active = l.flipped))))
      advisers <- denizens(player.cards)
      kept <- relics(player.relics)
    yield LineageState(lineageOf(color), None, role, legacies,
      advisers.map(DenizenState(_, Orientation.FaceDown, Tokens.empty)),
      startingRelics = kept)

  private def ttsPlayer(lineage: LineageState): Either[String, TtsPlayer] =
    for
      cards <- sequence(lineage.startingAdvisers.map {
        case denizen: DenizenState => ids.ttsCard(denizen.id)
        case vision: VisionState => Left(
          s"lineage ${lineage.id.value} keeps the Vision ${vision.id.value}, " +
            "which TTS cannot store")
      })
      legacies <- sequence(lineage.legacies.map(l =>
        ids.ttsLegacy(l.id).map(TtsLegacy(_, flipped = l.active))))
      kept <- sequence(lineage.startingRelics.map(ids.ttsCard))
    yield TtsPlayer(roles.indexOf(lineage.role), cards, legacies, kept)
