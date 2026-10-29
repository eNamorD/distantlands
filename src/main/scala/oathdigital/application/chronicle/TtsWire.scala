package oathdigital.application.chronicle

/** A stored site: its TTS index and up to three TTS item ids. */
final case class TtsSite(index: Int, items: Vector[Int])

/** A legacy by TTS id, flipped or not. */
final case class TtsLegacy(id: Int, flipped: Boolean)

/** A player section. Status 0 is Exile, 1 Citizen, 2 Chancellor; relics are
  * full TTS ids (401-499). */
final case class TtsPlayer(status: Int, cards: Vector[Int],
    legacies: Vector[TtsLegacy], relics: Vector[Int])

/** The flipped Foundations as a bitmask, Foundation I as bit 0, and the
  * version 3 Oathkeeper goal digit when the string carries one. */
final case class TtsFoundations(mask: Int, goal: Option[Int])

/** Every section that the string held, in order; absent trailing
  * sections are None. Card ids are full TTS ids (relics 401-499). */
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
  * chronicle-codec.js at f6051bb0), ported exactly: base-82 numbers, packed
  * sites, decks and lists, a scrambled body between a version and a
  * checksum. This layer knows TTS numbers only; `ChronicleTtsCodec` maps them
  * to catalog ids. Version 3 adds one goal digit after the Foundations
  * bitmask, as its maintainer announced. */
object TtsWire:
  /** The player sections' colors, in section order. */
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
  private val SectionCount = 7 + colors.size
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

  private[chronicle] def encodeExport(chronicle: TtsChronicle, version: Int)
      : Either[String, String] =
    for
      plain <- encodePlain(chronicle, version)
      head <- padEncode(version, 2)
      body <- scramble(plain, decoding = false)
      tail <- padEncode(checksum(plain), 2)
    yield head + body + tail

  /** The unscrambled string: every section, separated by `>`. */
  def decodePlain(plain: String, version: Int): Either[String, TtsChronicle] =
    val sections = plain.split(">", -1).toVector
    def at[A](index: Int)(decode: String => Either[String, A])
        : Either[String, Option[A]] =
      sections.lift(index) match
        case None => Right(None)
        case Some(section) => decode(section).map(Some(_))
    for
      _ <- supported(version)
      _ <- Either.cond(sections.size <= SectionCount, (),
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
      players <- sequence(colors.indices.toVector.map(c => at(7 + c)(decodePlayer)))
    yield TtsChronicle(atlasBox, world, worldDeck, relicDeck, dispossessed,
      reliquary, foundations, players)

  /** Sections up to the last present one; an absent one before it is
    * written empty, which keeps every later section in its place. */
  def encodePlain(chronicle: TtsChronicle, version: Int): Either[String, String] =
    val sections: Vector[Option[Either[String, String]]] = Vector(
      chronicle.atlasBox.map(encodeSites),
      chronicle.world.map(encodeSites),
      chronicle.worldDeck.map(packIds(_, DeckRadix)),
      chronicle.relicDeck.map(ids => packIds(ids.map(_ - RelicOffset), RelicRadix)),
      chronicle.dispossessed.map(packIds(_, DeckRadix)),
      chronicle.reliquary.map(ids =>
        packNumber(ids.map(_ - RelicOffset), RelicRadix)),
      chronicle.foundations.map(encodeFoundations(_, version))) ++
      chronicle.players.map(_.map(encodePlayer))
    val count = sections.lastIndexWhere(_.nonEmpty) + 1
    for
      _ <- supported(version)
      _ <- Either.cond(chronicle.players.size == colors.size, (),
        s"Expected ${colors.size} player sections, found ${chronicle.players.size}")
      written <- sequence(sections.take(count).map(_.getOrElse(Right(""))))
    yield written.mkString(">")

  private def supported(version: Int): Either[String, Unit] =
    Either.cond(version == 2 || version == 3, (),
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

  private def scramble(input: String, decoding: Boolean): Either[String, String] =
    val n = Base82Plus.length
    var seed = ScrambleSeed
    val out = new StringBuilder
    input.foldLeft[Either[String, Unit]](Right(()))((ok, c) =>
      ok.flatMap(_ =>
        seed = seed * 16807 % SeedModulus
        val idx = Base82Plus.indexOf(c)
        if idx < 0 then Left(s"scramble: invalid character \"$c\" in string")
        else
          val trueIdx = if decoding then mod(idx - seed, n) else idx
          out += Base82Plus(if decoding then trueIdx else mod(idx + seed, n))
          seed = (seed + trueIdx) % SeedModulus
          Right(()))).map(_ => out.toString)

  private def checksum(text: String): Int =
    text.zipWithIndex.foldLeft(0)((sum, entry) =>
      (sum + (entry._2 + 1) * Base82Plus.indexOf(entry._1)) % ChecksumModulus)

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
        value = site.items.zipWithIndex.foldLeft(BigInt(site.index))(
          (n, entry) => n + BigInt(entry._1) * BigInt(1000).pow(entry._2) * 100)
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
      .flatMap(_ => safe(ids.zipWithIndex.foldLeft(BigInt(0))((n, entry) =>
        n + BigInt(entry._1) * BigInt(radix).pow(entry._2))))

  /** The value's digits in `radix`, lowest first, stopping at zero. */
  private def digits(value: Long, radix: Int): Iterator[Int] =
    Iterator.iterate(value)(_ / radix).takeWhile(_ > 0).map(n => (n % radix).toInt)

  private def safe(value: BigInt): Either[String, Long] =
    Either.cond(value <= MaxSafe, value.toLong, s"Cannot encode $value exactly")

  // ---------- Foundations ----------

  private def decodeFoundations(section: String, version: Int)
      : Either[String, TtsFoundations] =
    def mask(text: String): Either[String, Int] =
      (if text.isEmpty then Right(0L) else fromBase82(text)).flatMap(m =>
        Either.cond(m < (1 << FoundationCount), m.toInt,
          s"Invalid Foundations value: $section"))
    if version == 2 then mask(section).map(TtsFoundations(_, None))
    else section.length match
      case 0 | 1 => mask(section).map(TtsFoundations(_, None))
      case 2 =>
        for
          m <- mask(section.take(1))
          goal <- fromBase82(section.drop(1))
        yield TtsFoundations(m, Some(goal.toInt))
      case _ => Left(s"Invalid Foundations value: $section")

  private def encodeFoundations(foundations: TtsFoundations, version: Int)
      : Either[String, String] =
    for
      _ <- Either.cond(foundations.mask >= 0 &&
        foundations.mask < (1 << FoundationCount), (),
        s"Invalid Foundations mask ${foundations.mask}")
      mask <- toBase82(foundations.mask)
      goal <-
        if version == 2 then Either.cond(foundations.goal.isEmpty, "",
          "Encoding version 2 has no Oathkeeper goal")
        else
          val digit = foundations.goal.getOrElse(0)
          Either.cond(digit >= 0 && digit < 82, Base82(digit).toString,
            s"Oathkeeper goal $digit does not fit in one character")
    yield mask + goal

  // ---------- players ----------
  // [status][cards]#[legacies]#[relics]. Legacies are id + 100 when
  // flipped, radix 200: under five are one number, five or more are packed
  // like a deck, so the length tells the two apart.

  private def encodePlayer(player: TtsPlayer): Either[String, String] =
    for
      _ <- Either.cond(player.status >= 0 && player.status <= 2, (),
        s"Unknown player status: ${player.status}")
      _ <- player.legacies.find(l => l.id < 1 || l.id >= LegacyFlipped)
        .toLeft(()).left.map(l => s"Legacy ${l.id} does not fit")
      cards <- packNumber(player.cards, DeckRadix)
      values = player.legacies.map(l =>
        l.id + (if l.flipped then LegacyFlipped else 0))
      legacies <-
        if values.size < ChunkSize then packNumber(values, LegacyRadix)
        else packIds(values, LegacyRadix)
      relics <- packNumber(player.relics.map(_ - RelicOffset), RelicRadix)
    yield Base82(player.status).toString + cards + "#" + legacies + "#" + relics

  private def decodePlayer(section: String): Either[String, TtsPlayer] =
    section.split("#", -1).toVector match
      case Vector(head, legacies, relics) =>
        val status = head.headOption.map(Base82.indexOf(_)).getOrElse(-1)
        for
          _ <- Either.cond(status >= 0 && status <= 2, (),
            s"Invalid player status in: $section")
          cards <- unpackNumber(head.drop(1), DeckRadix).flatMap(deckCards)
          values <-
            if legacies.length < ChunkWidth then unpackNumber(legacies, LegacyRadix)
            else unpackIds(legacies, LegacyRadix)
          relicIds <- unpackNumber(relics, RelicRadix)
        yield TtsPlayer(status, cards,
          values.map(v => TtsLegacy(v % LegacyFlipped, v > LegacyFlipped)),
          relicIds.map(_ + RelicOffset))
      case fields => Left(
        s"Player section should have 3 fields, found ${fields.size}: $section")

  private def sequence[A](values: Vector[Either[String, A]])
      : Either[String, Vector[A]] =
    values.foldLeft[Either[String, Vector[A]]](Right(Vector.empty))(
      (done, next) => done.flatMap(found => next.map(found :+ _)))
