package distantlands.application.chronicle

/** The TTS string layer against the test strings of chronicle-codec.js at
  * f6051bb0, made by the Lua exporter in TTS. */
class TtsWireSuite extends munit.FunSuite:
  private val plain =
    "M#5#F#7f,#6#66)#7#8#K#623#:n@-#7sK#1#0#)oy~#6Bu#6f6#L#6cv#,&CK#3#6M/#>" +
      "D&1?8w#8U{9#>" +
      "7+-x;V08oZ8huHKQPKOQ4J)es4@aDMR,5JBHv,5)~EF/OybDN2D5rps-D;1311dFNUBqTQ6L~;A5a>" +
      "00yuz%g00g:LA100{N:i(01IG=nz00il1!!012G;R2>" +
      "00005QqFZ:dW^&Hg{t!5-4ONLOau9x/yb37AIM2pZqK@pO3p00/c17BaBG-@qHk"
  private val luaPlain =
    "M#5#F#7f,#6#66)#7#8#K#623#:n@-#7sK#1#0#)oy~#6Bu#6f6#L#6cv#,&CK#3#6M/#>" +
      "D&1?8w#8U{9#>" +
      "7+-x;V08oZ8huHKQPKOQ4J)es4@aDMR,5JBHv,5)~EF/OybDN2D5rps-D;1311dFNUBqTQ6L~;A5a>" +
      "00wjXpz00,,9Hd00cGcDm00aVgF=00F?%0}00i%s(Z>" +
      "00005QqFZ:dW^&Hg{t!5-4ONLOau9x/yb37AIM2pZqK@pO3p00/c17BaBG-@qHk>"
  private val luaExport = "02H3uk5&m#lvuYL;KIO:Mg9FjOs)5.yU!bk{6k9tUwlHwZ}z%uTY3.}IvkwdiP9.r)s-g,XAxa!)JRjGCc5ze$;93~2=J;chc)06a}66Pn:tI#vWs~QDbnK5PpZ#&Xq36lcTLE-g359{rq/DBYLh@RBb~R;?$:iMT},~=/N@)J+fh9Ip8T1#dMl?Gf:M?tM%r/8%cL,lF{RGZOorax@0pONlP@9O.&SmJ>G6bq2t^7FJFmI>W$a,3r4:&ohBXUX)$&d%DuAJl%i~Y,:P"
  private val fullExport = "02H3uk5&m#lvuYL;KIO:Mg9FjOs)5.yU!bk{6k9tUwlHwZ}z%uTY3.}IvkwdiP9.r)s-g,XAxa!)JRjGCc5ze$;93~2=J;chc)06a}66Pn:tI#vWs~QDbnK5PpZ#&Xq36lcTLE-g359{rq/DBYLh@RBb~R;?$:iMT},~=/N@)J+fh9Ip8T1#dMl?Gf:M?tM%r/8%cL,lF{RGZOorax@0pONlP@9O.&SmJ>G6bq2t^7FJFmI>W$a,3r4:&ohBXUX)$&d%DuAJl%i~Y,c)@b!/eoUQ9Qs!w223^(:%Lf.{eA5@(1u^DqWE$Kv#=iyHXC/5T"

  private def decoded(text: String, version: Int = 2)(using munit.Location)
      : TtsChronicle =
    TtsWire.decodePlain(text, version).fold(fail(_), identity)

  test("the exporter's plain string decodes and re-encodes exactly"):
    val chronicle = decoded(plain)
    assertEquals(chronicle.worldDeck.map(_.size), Some(55))
    assertEquals(TtsWire.encodePlain(chronicle, 2), Right(plain))

  test("the Lua pair: the plain string scrambles to the exporter's string"):
    val chronicle = decoded(luaPlain)
    assertEquals(chronicle.reliquary, Some(Vector.empty))
    assertEquals(TtsWire.encodePlain(chronicle, 2), Right(luaPlain))
    assertEquals(TtsWire.encodeExport(chronicle, 2), Right(luaExport))
    assertEquals(TtsWire.decodeExport(luaExport), Right((2, chronicle)))

  test("the full export re-encodes to the same string"):
    val (version, chronicle) = TtsWire.decodeExport(fullExport)
      .fold(fail(_), identity)
    assertEquals(version, 2)
    assertEquals(chronicle.foundations, Some(TtsFoundations(8, None)))
    assertEquals(chronicle.players(2).map(_.status), Some(2))
    assertEquals(TtsWire.encodeExport(chronicle, 2), Right(fullExport))

  test("whitespace around and inside the string is ignored"):
    assertEquals(TtsWire.decodeExport(s"\n $luaExport \n").map(_._2),
      Right(decoded(luaPlain)))

  test("a changed character fails the checksum"):
    val bad = luaExport.take(10) +
      (if luaExport(10) == 'a' then 'b' else 'a') + luaExport.drop(11)
    assertEquals(TtsWire.decodeExport(bad),
      Left("Checksum failure, the export string is likely corrupted"))

  test("an unknown version and a short string are refused"):
    assertEquals(TtsWire.decodeExport("01" + luaExport.drop(2)),
      Left("This export string uses encoding version 1, which is not supported"))
    assertEquals(TtsWire.decodeExport("02ab"), Left("Export string is too short"))

  test("a skipped and an empty Reliquary both read as no relics"):
    assertEquals(decoded(plain + ">").reliquary, Some(Vector.empty))
    assertEquals(decoded(plain + ">0").reliquary, Some(Vector.empty))

  test("legacies: under five are one number, five or more are chunked"):
    val legacies = Vector(1, 2, 3, 4, 5, 36).zipWithIndex
      .map((id, i) => TtsLegacy(id, flipped = i % 2 == 1))
    for count <- Vector(0, 1, 4, 5, 6) do
      val player = TtsPlayer(1, Vector(120), legacies.take(count), Vector(447))
      val chronicle = TtsChronicle(players = Vector.fill(7)(Some(player)))
      val text = TtsWire.encodePlain(chronicle, 2).fold(fail(_), identity)
      val field = text.split(">", -1).last.split("#", -1)(1)
      assert(if count < 5 then field.length <= 5 else field.length % 7 == 0,
        s"$count: $field")
      assertEquals(decoded(text).players.last, Some(player), count)

  test("seven starting cards cannot be encoded exactly"):
    val player = TtsPlayer(0, Vector.fill(7)(120), Vector.empty, Vector.empty)
    assert(TtsWire.encodePlain(TtsChronicle(players = Vector(Some(player)) ++
      Vector.fill(6)(None)), 2).isLeft)

  test("every Foundations mask round trips, with a version 3 goal digit"):
    for mask <- 0 until 64; goal <- Vector(None, Some(0), Some(4), Some(21)) do
      val chronicle = TtsChronicle(foundations = Some(TtsFoundations(mask, goal)))
      val text = TtsWire.encodePlain(chronicle, 3).fold(fail(_), identity)
      assertEquals(decoded(text, 3).foundations,
        Some(TtsFoundations(mask, Some(goal.getOrElse(0)))), (mask, goal))

  test("version 3 reads a Foundations section of 0, 1 or 2 characters"):
    assertEquals(decoded(">>>>>>", 3).foundations,
      Some(TtsFoundations(0, None)))
    assertEquals(decoded(">>>>>>8", 3).foundations,
      Some(TtsFoundations(8, None)))
    assert(TtsWire.decodePlain(">>>>>>812", 3).isLeft)
