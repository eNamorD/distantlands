package distantlands.application.chronicle

/** The TTS string layer against the test strings of chronicle-codec.js at
  * 856373ec, made by the Lua exporter in TTS. */
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
  private val v3Export = "03H3uk5&m#lvuYL;KIO:Mg9FjOs)5.yU!bk{6k9tUwlHwZ}z%uTY3.}IvkwdiP9.r)s-g,XAxa!)JRjGCc5ze$;93~2=J;chc)06a}66Pn:tI#vWs~QDbnK5PpZ#&Xq36lcTLE-g359{rq/DBYLh@RBb~R;?$:iMT},~=/N@)J+fh9Ip8T1#dMl?Gf:M?tM%r/8%cL,lF{RGZOorax@0pONlP@9O.&SmJ>G6bq2t^7FJFmI>W$a,3r4:&ohBXUX)$&d%DuAJl%i~Y,c)@b!/W30&z!C-uQw(dF6Ni2k&rOzPKszQh!I4AT}rDwTx3cqDpjWuF%!o=v:aydNT-"
  private val fullExport = "02H3uk5&m#lvuYL;KIO:Mg9FjOs)5.yU!bk{6k9tUwlHwZ}z%uTY3.}IvkwdiP9.r)s-g,XAxa!)JRjGCc5ze$;93~2=J;chc)06a}66Pn:tI#vWs~QDbnK5PpZ#&Xq36lcTLE-g359{rq/DBYLh@RBb~R;?$:iMT},~=/N@)J+fh9Ip8T1#dMl?Gf:M?tM%r/8%cL,lF{RGZOorax@0pONlP@9O.&SmJ>G6bq2t^7FJFmI>W$a,3r4:&ohBXUX)$&d%DuAJl%i~Y,c)@b!/eoUQ9Qs!w223^(:%Lf.{eA5@(1u^DqWE$Kv#=iyHXC/5T"

  private def decoded(text: String, version: Int = TtsWire.version)
      (using munit.Location): TtsChronicle =
    TtsWire.decodePlain(text, version).fold(fail(_), identity)

  private def exported(text: String)(using munit.Location)
      : (Int, TtsChronicle) =
    TtsWire.decodeExport(text).fold(fail(_), identity)

  private def plainOf(chronicle: TtsChronicle)(using munit.Location): String =
    TtsWire.encodePlain(chronicle).fold(fail(_), identity)

  private def onlyPlayers(players: Option[TtsPlayer]*): TtsChronicle =
    TtsChronicle(players = players.toVector.padTo(7, None))

  test("the exporter's plain string decodes and re-encodes exactly"):
    // No Foundations or players, so it reads the same in every version.
    val chronicle = decoded(plain, 2)
    assertEquals(chronicle.worldDeck.map(_.size), Some(55))
    assertEquals(decoded(plain), chronicle)
    assertEquals(TtsWire.encodePlain(chronicle), Right(plain))

  test("the Lua pair: the plain string scrambles as the exporter's does"):
    val chronicle = decoded(luaPlain, 2)
    assertEquals(chronicle.reliquary, Some(Vector.empty))
    assertEquals(TtsWire.encodePlain(chronicle), Right(luaPlain))
    assertEquals(TtsWire.decodeExport(luaExport), Right((2, chronicle)))
    // Only the version header differs when re-encoded.
    assertEquals(TtsWire.encodeExport(chronicle), Right("04" + luaExport.drop(2)))

  test("the full version 3 Lua export decodes to the values its own test " +
      "asserts"):
    val (version, chronicle) = exported(v3Export)
    assertEquals(version, 3)
    // Foundation IV (Teeming World) flipped, Oath 4 (Protection).
    assertEquals(chronicle.foundations, Some(TtsFoundations(8, 4)))
    assertEquals(chronicle.players.map(_.map(_.lineageScore)),
      (1 to 7).toVector.map(Some(_)))
    assertEquals(chronicle.players(2).map(_.status), Some(2))
    assertEquals(chronicle.players(3), Some(TtsPlayer(0, 4, Vector.empty,
      Vector.empty, Vector.empty)))

  test("the full version 3 export re-encodes as version 4, differing only " +
      "in its header"):
    val (_, chronicle) = exported(v3Export)
    assertEquals(TtsWire.encodeExport(chronicle), Right("04" + v3Export.drop(2)))

  test("the version 2 export of the same table reads as no Oath and lineage " +
      "scores of 0, and survives version 4"):
    val (version, chronicle) = exported(fullExport)
    assertEquals(version, 2)
    assertEquals(chronicle.foundations, Some(TtsFoundations(8, 0)))
    assertEquals(chronicle.players.map(_.map(_.lineageScore)),
      Vector.fill(7)(Some(0)))
    val (_, v3) = exported(v3Export)
    assertEquals(chronicle.players.map(_.map(_.status)),
      v3.players.map(_.map(_.status)))
    val text = TtsWire.encodeExport(chronicle).fold(fail(_), identity)
    assert(text.startsWith("04"), text)
    assertEquals(exported(text), (4, chronicle))

  test("whitespace around and inside the string is ignored"):
    assertEquals(TtsWire.decodeExport(s"\n $luaExport \n").map(_._2),
      Right(decoded(luaPlain, 2)))

  test("a changed character fails the checksum"):
    val bad = luaExport.take(10) +
      (if luaExport(10) == 'a' then 'b' else 'a') + luaExport.drop(11)
    assertEquals(TtsWire.decodeExport(bad),
      Left("Checksum failure, the export string is likely corrupted"))

  test("an unknown version and a short string are refused"):
    assertEquals(TtsWire.decodeExport("01" + luaExport.drop(2)),
      Left("This export string uses encoding version 1, which is not supported"))
    assertEquals(TtsWire.decodeExport("05" + luaExport.drop(2)),
      Left("This export string uses encoding version 5, which is not supported"))
    assertEquals(TtsWire.decodeExport("02ab"), Left("Export string is too short"))

  test("a skipped and an empty Reliquary both read as no relics"):
    assertEquals(decoded(plain + ">").reliquary, Some(Vector.empty))
    assertEquals(decoded(plain + ">0").reliquary, Some(Vector.empty))

  test("legacies: under five are one number, five or more are chunked"):
    val legacies = Vector(1, 2, 3, 4, 5, 36).zipWithIndex
      .map((id, i) => TtsLegacy(id, flipped = i % 2 == 1))
    for count <- Vector(0, 1, 4, 5, 6) do
      val player = TtsPlayer(1, 3, Vector(120), legacies.take(count),
        Vector(447))
      val text = plainOf(TtsChronicle(players = Vector.fill(7)(Some(player))))
      val field = text.split(">", -1).last.split("#", -1)(1)
      assert(if count < 5 then field.length <= 5 else field.length % 7 == 0,
        s"$count: $field")
      assertEquals(decoded(text).players.last, Some(player), count)

  test("seven starting cards cannot be encoded exactly"):
    val player = TtsPlayer(0, 0, Vector.fill(7)(120), Vector.empty,
      Vector.empty)
    assert(TtsWire.encodePlain(onlyPlayers(Some(player))).isLeft)

  test("a lineage score is two digits after the status"):
    assertEquals(plainOf(onlyPlayers(Some(TtsPlayer.emptyExile))),
      ">>>>>>000>000##")
    val scored = TtsPlayer(1, 82, Vector(120), Vector.empty, Vector.empty)
    val section = plainOf(onlyPlayers(Some(scored))).split(">", -1).last
    assertEquals(section.take(3), "110")
    assertEquals(decoded(plainOf(onlyPlayers(Some(scored)))).players.head,
      Some(scored))

  test("a lineage score above 6723 is written as 6723; a negative one is " +
      "refused"):
    val high = TtsPlayer.emptyExile.copy(lineageScore = 7000)
    assertEquals(decoded(plainOf(onlyPlayers(Some(high)))).players.head,
      Some(high.copy(lineageScore = 6723)))
    assertEquals(TtsWire.encodePlain(onlyPlayers(Some(
      TtsPlayer.emptyExile.copy(lineageScore = -1)))),
      Left("Lineage score -1 must be 0 or more"))

  test("a version 3 player needs both lineage score digits"):
    assertEquals(TtsWire.decodePlain(">>>>>>000>10##", 3),
      Left("Player section is missing its lineage score: 10##"))
    assertEquals(decoded(">>>>>>8>10##", 2).players.head,
      Some(TtsPlayer(1, 0, Vector.empty, Vector.empty, Vector.empty)))

  test("every Foundations mask and Oath round trips in three characters"):
    for mask <- 0 until 64; oath <- Vector(0, 1, 4, 5, 20, 21, 81) do
      val chronicle = TtsChronicle(foundations = Some(TtsFoundations(mask, oath)))
      val text = plainOf(chronicle)
      assertEquals(text.split(">", -1).last.length, 3, (mask, oath))
      assertEquals(decoded(text).foundations,
        Some(TtsFoundations(mask, oath)), (mask, oath))

  test("an Oath of 82 does not fit in one digit"):
    assertEquals(TtsWire.encodePlain(TtsChronicle(foundations =
      Some(TtsFoundations(0, 82)))), Left("Oath 82 does not fit in one digit"))

  test("from version 3 the Foundations section is always three characters"):
    for section <- Vector("", "01", "0010") do
      assertEquals(TtsWire.decodePlain(s">>>>>>$section", 3),
        Left(s"Foundations section should be 3 characters, found " +
          s"${section.length}: $section"), section)
    assertEquals(decoded(">>>>>>", 2).foundations, Some(TtsFoundations(0, 0)))
    assertEquals(decoded(">>>>>>8", 2).foundations, Some(TtsFoundations(8, 0)))

  test("a missing Foundations section before the players is written as " +
      "none flipped and no Oath"):
    assertEquals(plainOf(onlyPlayers(Some(TtsPlayer.emptyExile))).split(">", -1)(6),
      "000")

  test("missing named players before a later one are written as empty " +
      "Exiles"):
    val scored = TtsPlayer(1, 5, Vector.empty, Vector.empty, Vector.empty)
    val text = plainOf(onlyPlayers(None, None, Some(scored)))
    assertEquals(text, ">>>>>>000>000##>000##>105##")
    assertEquals(decoded(text).players.take(3), Vector(
      Some(TtsPlayer.emptyExile), Some(TtsPlayer.emptyExile), Some(scored)))

  test("version 4 extra players follow pink, with gaps written as empty " +
      "Exiles"):
    val extra = TtsPlayer(1, 9, Vector(120), Vector.empty, Vector(447))
    val (_, v3) = exported(v3Export)
    val withExtras = v3.copy(players = v3.players ++
      Vector(Some(extra), None, Some(extra)))
    val text = plainOf(withExtras)
    assertEquals(text.split(">", -1).size, 14 + 3)
    val back = decoded(text)
    assertEquals(back.players.drop(7), Vector(Some(extra),
      Some(TtsPlayer.emptyExile), Some(extra)))
    assertEquals(plainOf(back), text)
    assertEquals(TtsWire.encodeExport(withExtras).map(TtsWire.decodeExport),
      Right(Right((4, back))))

  test("before version 4 more than fourteen sections are refused"):
    val (_, v3) = exported(v3Export)
    val text = plainOf(v3.copy(players = v3.players :+ Some(TtsPlayer.emptyExile)))
    assertEquals(TtsWire.decodePlain(text, 3),
      Left("Expected at most 14 sections, found 15"))
