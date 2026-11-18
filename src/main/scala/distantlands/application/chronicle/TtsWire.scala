package distantlands.application.chronicle

/** A stored site: its TTS index and up to three TTS item ids. */
final case class TtsSite(index: Int, items: Vector[Int])

/** A legacy by TTS id, flipped or not. */
final case class TtsLegacy(id: Int, flipped: Boolean)

/** A player section. Status 0 is Exile, 1 Citizen, 2 Chancellor;
  * `lineageScore` is the lineage's era score (0 in version 2, which has
  * none); relics are full TTS ids (401-499). */
final case class TtsPlayer(status: Int, lineageScore: Int, cards: Vector[Int],
    legacies: Vector[TtsLegacy], relics: Vector[Int])

object TtsPlayer:
  /** What a missing player is written as, and what an empty section from
    * the exporter holds: an Exile with nothing. */
  val emptyExile: TtsPlayer =
    TtsPlayer(0, 0, Vector.empty, Vector.empty, Vector.empty)

/** The flipped Foundations as a bitmask, Foundation I as bit 0, and the
  * Oath digit: 0 unset (always, in version 2), 1 Supremacy, 2 Devotion,
  * 3 the People, 4 Protection, 5-20 reserved, 21+ fan content. */
final case class TtsFoundations(mask: Int, oath: Int)

/** Every section that the string held, in order; absent trailing
  * sections are None. Card ids are full TTS ids (relics 401-499).
  * `players` holds the seven named colors' sections in `TtsWire.colors`
  * order, then any extra player sections (players 8, 9, ...) that
  * version 4 allows. */
final case class TtsChronicle(
    atlasBox: Option[Vector[TtsSite]] = None,
    world: Option[Vector[TtsSite]] = None,
    worldDeck: Option[Vector[Int]] = None,
    relicDeck: Option[Vector[Int]] = None,
    dispossessed: Option[Vector[Int]] = None,
    reliquary: Option[Vector[Int]] = None,
    foundations: Option[TtsFoundations] = None,
    players: Vector[Option[TtsPlayer]] = Vector.fill(7)(None))

/** The Oath NF TTS Chronicle export string (harsch1/oath-nf-tts-scripts
  * chronicle-codec.js at 856373ec), ported exactly: base-82 numbers, packed
  * sites, decks and lists, a scrambled body between a version and a
  * checksum. This layer knows TTS numbers only; `ChronicleTtsCodec` maps them
  * to catalog ids. Decoding reads versions 2 to 4; encoding always writes
  * `version`. Version 3 widened the Foundations section to a 2-digit mask
  * and an Oath digit, and gave each player a 2-digit lineage score; version
  * 4 allows extra player sections after the named seven. */
object TtsWire:
  /** The version every encoding writes. */
  val version: Int = 4

  /** The named player sections' colors, in section order. */
  val colors: Vector[String] =
    Vector("red", "blue", "yellow", "black", "white", "brown", "pink")

  private val Base82 =
    "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz+-=/~!@$%^&(){};:,.?"
  private val Base82Plus = Base82 + "#>"
  private val ScrambleSeed = 468529063L
  private val SeedModulus = 2147483647L
  private val ChecksumModulus = 82 * 82
  private val DeckRadix = 400
  private val RelicRadix = 100
  private val RelicOffset = 400
  private val LegacyRadix = 200
  private val LegacyFlipped = 100
  private val ChunkSize = 5
  private val ChunkWidth = 7
  private val FoundationCount = 6
  private val FoundationsWidth = 2
  private val LineageWidth = 2
  // 82^2 - 1, the most LineageWidth digits hold; a higher score is written
  // as this.
  private val MaxLineageScore = 82 * 82 - 1
  private val PlayerOffset = 7
  private val SectionCount = PlayerOffset + colors.size
  // JS numbers are exact up to Number.MAX_SAFE_INTEGER.
  private val MaxSafe = (1L << 53) - 1

  def decodeExport(text: String): Either[String, (Int, TtsChronicle)] =
    val compact = text.filterNot(_.isWhitespace)
    for
      _ <- Either.cond(compact.length >= 5, (), "Export string is too short")
      version <- fromBase82(compact.take(2)).map(_.toInt)
      _ <- supported(version)
      plain <- scramble(compact.drop(2).dropRight(2), decoding = true)
      check <- padEncode(checksum(plain), 2)
      _ <- Either.cond(compact.takeRight(2) == check, (),
        "Checksum failure, the export string is likely corrupted")
      chronicle <- decodePlain(plain, version)
    yield (version, chronicle)

  def encodeExport(chronicle: TtsChronicle): Either[String, String] =
    for
      plain <- encodePlain(chronicle)
      head <- padEncode(version, 2)
      body <- scramble(plain, decoding = false)
      tail <- padEncode(checksum(plain), 2)
    yield head + body + tail

  /** The unscrambled string: every section, separated by `>`. Before
    * version 4 there are no extra players, so more than the named
    * sections is refused. */
  def decodePlain(plain: String, version: Int): Either[String, TtsChronicle] =
    val sections = plain.split(">", -1).toVector
    def at[A](index: Int)(decode: String => Either[String, A])
        : Either[String, Option[A]] =
      sections.lift(index) match
        case None => Right(None)
        case Some(section) => decode(section).map(Some(_))
    val playerCount = colors.size max (sections.size - PlayerOffset)
    for
      _ <- supported(version)
      _ <- Either.cond(version >= 4 || sections.size <= SectionCount, (),
        s"Expected at most $SectionCount sections, found ${sections.size}")
      atlasBox <- at(0)(decodeSites)
      world <- at(1)(decodeSites)
      worldDeck <- at(2)(decodeCardDeck)
      relicDeck <- at(3)(section =>
        unpackIds(section, RelicRadix).map(_.map(_ + RelicOffset)))
      dispossessed <- at(4)(decodeCardDeck)
      reliquary <- at(5)(section =>
        unpackNumber(section, RelicRadix).map(_.map(_ + RelicOffset)))
      foundations <- at(6)(decodeFoundations(_, version))
      players <- sequence(Vector.tabulate(playerCount)(p =>
        at(PlayerOffset + p)(decodePlayer(_, version))))
    yield TtsChronicle(atlasBox, world, worldDeck, relicDeck, dispossessed,
      reliquary, foundations, players)

  /** Sections up to the last present one, as `version` writes them. An
    * absent one before it is written empty, except the Foundations and
    * players, which are written as none flipped with no Oath and as an
    * empty Exile, so every section still decodes. */
  def encodePlain(chronicle: TtsChronicle): Either[String, String] =
    val sections: Vector[Option[Either[String, String]]] = Vector(
      chronicle.atlasBox.map(encodeSites),
      chronicle.world.map(encodeSites),
      chronicle.worldDeck.map(packIds(_, DeckRadix)),
      chronicle.relicDeck.map(ids => packIds(ids.map(_ - RelicOffset), RelicRadix)),
      chronicle.dispossessed.map(packIds(_, DeckRadix)),
      chronicle.reliquary.map(ids =>
        packNumber(ids.map(_ - RelicOffset), RelicRadix)),
      chronicle.foundations.map(encodeFoundations)) ++
      chronicle.players.map(_.map(encodePlayer))
    val missing: Vector[Either[String, String]] =
      Vector.fill(6)(Right("")) ++
        (encodeFoundations(TtsFoundations(0, 0)) +:
          Vector.fill(chronicle.players.size)(
            encodePlayer(TtsPlayer.emptyExile)))
    val count = sections.lastIndexWhere(_.nonEmpty) + 1
    for
      _ <- Either.cond(chronicle.players.size >= colors.size, (),
        s"Expected at least ${colors.size} player sections, " +
          s"found ${chronicle.players.size}")
      written <- sequence(sections.zip(missing).take(count).map(
        (section, absent) => section.getOrElse(absent)))
    yield written.mkString(">")

  private def supported(version: Int): Either[String, Unit] =
    Either.cond(version >= 2 && version <= 4, (),
      s"This export string uses encoding version $version, which is not supported")

  // ---------- base 82 ----------

  private def toBase82(value: Long): Either[String, String] =
    if value < 0 || value > MaxSafe then Left(s"Cannot encode $value exactly")
    else if value == 0 then Right(Base82.take(1))
    else
      val digits = Iterator.iterate(value)(_ / 82).takeWhile(_ > 0)
        .map(n => Base82((n % 82).toInt)).toVector
      Right(digits.reverse.mkString)

  private def fromBase82(text: String): Either[String, Long] =
    text.foldLeft[Either[String, Long]](Right(0L))((total, c) =>
      total.flatMap(n =>
        val digit = Base82.indexOf(c)
        if digit < 0 then Left(s"Invalid base-82 character \"$c\"")
        else if n > (MaxSafe - digit) / 82 then
          Left(s"Value too large to decode exactly: $text")
        else Right(n * 82 + digit)))

  private def padEncode(value: Long, width: Int): Either[String, String] =
    toBase82(value).map(digits =>
      Base82.take(1) * (width - digits.length) + digits)

  // ---------- scramble and checksum ----------

  private def mod(a: Long, m: Int): Int = (((a % m) + m) % m).toInt

  /** Each character steps the seed, shifts by it, then feeds its true index
    * back into the seed. */
  private def scramble(input: String, decoding: Boolean): Either[String, String] =
    val n = Base82Plus.length
    input.foldLeft[Either[String, (Long, String)]](Right((ScrambleSeed, ""))) {
      case (done, c) => done.flatMap { case (previous, out) =>
        val seed = previous * 16807 % SeedModulus
        val idx = Base82Plus.indexOf(c)
        if idx < 0 then Left(s"scramble: invalid character \"$c\" in string")
        else
          val trueIdx = if decoding then mod(idx - seed, n) else idx
          val written = if decoding then trueIdx else mod(idx + seed, n)
          Right(((seed + trueIdx) % SeedModulus, out + Base82Plus(written)))
      }
    }.map(_._2)

  private def checksum(text: String): Int =
    text.zipWithIndex.foldLeft(0) { case (sum, (c, i)) =>
      (sum + (i + 1) * Base82Plus.indexOf(c)) % ChecksumModulus
    }

  // ---------- sites ----------
  // One token per site, ended by '#': the index in the lowest 2 digits,
  // then each item id in 3 digits above it.

  private def encodeSites(sites: Vector[TtsSite]): Either[String, String] =
    sequence(sites.map(site =>
      for
        _ <- Either.cond(site.index >= 0 && site.index < 100, (),
          s"Site index ${site.index} does not fit")
        _ <- site.items.find(id => id < 1 || id >= 1000)
          .toLeft(()).left.map(id => s"Item $id does not fit in a site")
        value = site.items.zipWithIndex.foldLeft(BigInt(site.index)) {
          case (n, (item, i)) => n + BigInt(item) * BigInt(1000).pow(i) * 100
        }
        token <- safe(value).flatMap(toBase82)
      yield token + "#")).map(_.mkString)

  private def decodeSites(section: String): Either[String, Vector[TtsSite]] =
    if section.isEmpty then Right(Vector.empty)
    else sequence(section.split("#", -1).toVector.dropRight(1).map(token =>
      fromBase82(token).map(n =>
        TtsSite((n % 100).toInt, Iterator.iterate(n / 100)(_ / 1000)
          .takeWhile(_ > 0).map(rest => (rest % 1000).toInt).toVector))))

  // ---------- decks ----------
  // Chunks of five ids, each padded to seven characters, the first chunk
  // rightmost.

  private def packIds(ids: Vector[Int], radix: Int): Either[String, String] =
    sequence(ids.grouped(ChunkSize).toVector.map(chunk =>
      packValue(chunk, radix).flatMap(padEncode(_, ChunkWidth))))
      .map(_.reverse.mkString)

  private def unpackIds(section: String, radix: Int): Either[String, Vector[Int]] =
    if section.length % ChunkWidth != 0 then
      Left(s"Deck section length ${section.length} is not a multiple of $ChunkWidth")
    else sequence(section.grouped(ChunkWidth).toVector.reverse.map(chunk =>
      fromBase82(chunk).map(digits(_, radix).take(ChunkSize))))
      .map(_.flatten)

  private def decodeCardDeck(section: String): Either[String, Vector[Int]] =
    unpackIds(section, DeckRadix).flatMap(deckCards)

  private def deckCards(ids: Vector[Int]): Either[String, Vector[Int]] =
    ids.find(_ < 1).toLeft(ids).left.map(id => s"Id $id is not a deck card")

  // ---------- small lists ----------
  // Every id packed into one unpadded number, written empty for no ids.

  private def packNumber(ids: Vector[Int], radix: Int): Either[String, String] =
    if ids.isEmpty then Right("")
    else packValue(ids, radix).flatMap(toBase82)

  private def unpackNumber(text: String, radix: Int): Either[String, Vector[Int]] =
    if text.isEmpty then Right(Vector.empty)
    else fromBase82(text).map(digits(_, radix).toVector)

  private def packValue(ids: Vector[Int], radix: Int): Either[String, Long] =
    ids.find(id => id < 1 || id >= radix).toLeft(()).left
      .map(id => s"Id $id does not fit in radix $radix")
      .flatMap(_ => safe(ids.zipWithIndex.foldLeft(BigInt(0)) {
        case (n, (id, i)) => n + BigInt(id) * BigInt(radix).pow(i)
      }))

  /** The value's digits in `radix`, lowest first, stopping at zero. */
  private def digits(value: Long, radix: Int): Iterator[Int] =
    Iterator.iterate(value)(_ / radix).takeWhile(_ > 0).map(n => (n % radix).toInt)

  private def safe(value: BigInt): Either[String, Long] =
    Either.cond(value <= MaxSafe, value.toLong, s"Cannot encode $value exactly")

  // ---------- Foundations and Oath ----------
  // Always 3 characters: the flipped Foundations' bitmask in 2 digits, then
  // the Oath digit. Version 2 had only the bitmask, unpadded, with an empty
  // section meaning none flipped; it reads as an unset Oath.

  private def decodeFoundations(section: String, version: Int)
      : Either[String, TtsFoundations] =
    def mask(text: String): Either[String, Int] =
      (if text.isEmpty then Right(0L) else fromBase82(text)).flatMap(m =>
        Either.cond(m < (1 << FoundationCount), m.toInt,
          s"Invalid Foundations value: $text"))
    if version == 2 then mask(section).map(TtsFoundations(_, 0))
    else if section.length != FoundationsWidth + 1 then
      Left(s"Foundations section should be ${FoundationsWidth + 1} " +
        s"characters, found ${section.length}: $section")
    else
      for
        m <- mask(section.take(FoundationsWidth))
        oath <- fromBase82(section.drop(FoundationsWidth))
      yield TtsFoundations(m, oath.toInt)

  private def encodeFoundations(foundations: TtsFoundations)
      : Either[String, String] =
    for
      _ <- Either.cond(foundations.mask >= 0 &&
        foundations.mask < (1 << FoundationCount), (),
        s"Invalid Foundations mask ${foundations.mask}")
      _ <- Either.cond(foundations.oath >= 0 && foundations.oath < 82, (),
        s"Oath ${foundations.oath} does not fit in one digit")
      mask <- padEncode(foundations.mask, FoundationsWidth)
    yield mask + Base82(foundations.oath)

  // ---------- players ----------
  // [status][lineage score: 2 digits][cards]#[legacies]#[relics]; version 2
  // has no lineage score. Legacies are id + 100 when flipped, radix 200:
  // under five are one number, five or more are packed like a deck, so the
  // length tells the two apart.

  private def encodePlayer(player: TtsPlayer): Either[String, String] =
    for
      _ <- Either.cond(player.status >= 0 && player.status <= 2, (),
        s"Unknown player status: ${player.status}")
      _ <- Either.cond(player.lineageScore >= 0, (),
        s"Lineage score ${player.lineageScore} must be 0 or more")
      score <- padEncode(player.lineageScore min MaxLineageScore, LineageWidth)
      _ <- player.legacies.find(l => l.id < 1 || l.id >= LegacyFlipped)
        .toLeft(()).left.map(l => s"Legacy ${l.id} does not fit")
      cards <- packNumber(player.cards, DeckRadix)
      values = player.legacies.map(l =>
        l.id + (if l.flipped then LegacyFlipped else 0))
      legacies <-
        if values.size < ChunkSize then packNumber(values, LegacyRadix)
        else packIds(values, LegacyRadix)
      relics <- packNumber(player.relics.map(_ - RelicOffset), RelicRadix)
    yield Base82(player.status).toString + score + cards + "#" + legacies +
      "#" + relics

  private def decodePlayer(section: String, version: Int)
      : Either[String, TtsPlayer] =
    section.split("#", -1).toVector match
      case Vector(head, legacies, relics) =>
        val status = head.headOption.map(Base82.indexOf(_)).getOrElse(-1)
        val scoreWidth = if version == 2 then 0 else LineageWidth
        for
          _ <- Either.cond(status >= 0 && status <= 2, (),
            s"Invalid player status in: $section")
          _ <- Either.cond(head.length >= 1 + scoreWidth, (),
            s"Player section is missing its lineage score: $section")
          score <-
            if scoreWidth == 0 then Right(0L)
            else fromBase82(head.slice(1, 1 + scoreWidth))
          cards <- unpackNumber(head.drop(1 + scoreWidth), DeckRadix)
            .flatMap(deckCards)
          values <-
            if legacies.length < ChunkWidth then unpackNumber(legacies, LegacyRadix)
            else unpackIds(legacies, LegacyRadix)
          relicIds <- unpackNumber(relics, RelicRadix)
        yield TtsPlayer(status, score.toInt, cards,
          values.map(v => TtsLegacy(v % LegacyFlipped, v > LegacyFlipped)),
          relicIds.map(_ + RelicOffset))
      case fields => Left(
        s"Player section should have 3 fields, found ${fields.size}: $section")

  /** The values, or the first refusal among them. */
  private[chronicle] def sequence[A](values: Vector[Either[String, A]])
      : Either[String, Vector[A]] =
    values.foldLeft[Either[String, Vector[A]]](Right(Vector.empty))(
      (done, next) => done.flatMap(found => next.map(found :+ _)))
