package distantlands.protocol

/** The Chronicle entry a browser keeps (Chronicle browsing design,
  * "Summary"). */
class ChronicleProtocolSuite extends munit.FunSuite:
  private val entry = ChronicleEntry("{}", "ab12", ChronicleSummary(
    "Ashen Crown", Some(2), 20, Vector(ChronicleScore("red-lineage", 3))))

  private def roundTripped(value: ChronicleEntry) =
    ChronicleEntryCodec.decodeValue(
      ujson.read(ujson.write(ChronicleEntryCodec.encodeValue(value))), "$")

  test("an entry round trips, with and without a game number"):
    assertEquals(roundTripped(entry), Right(entry))
    val imported = entry.copy(summary = entry.summary.copy(game = None))
    assertEquals(roundTripped(imported), Right(imported))
    assert(!ChronicleEntryCodec.encodeValue(imported)("summary").obj
      .contains("game"))

  test("a summary row carries an Imperial role and omits an Exile's"):
    val imperial = entry.copy(summary = entry.summary.copy(scores = Vector(
      ChronicleScore("red-lineage", 4, Some("chancellor")),
      ChronicleScore("blue-lineage", 2))))
    val rows = ChronicleEntryCodec.encodeValue(imperial)("summary")("scores")
    assertEquals(rows(0)("role").str, "chancellor")
    assert(!rows(1).obj.contains("role"))
    assertEquals(roundTripped(imperial), Right(imperial))

  test("a summary row refuses an unknown role"):
    val raw = ChronicleEntryCodec.encodeValue(entry)
    raw("summary")("scores")(0)("role") = "usurper"
    assert(ChronicleEntryCodec.decodeValue(raw, "$").isLeft)

  test("an entry refuses an unknown field"):
    val raw = ChronicleEntryCodec.encodeValue(entry)
    raw("extra") = 1
    assert(ChronicleEntryCodec.decodeValue(raw, "$").isLeft)

  test("a convert request is JSON alone, or a TTS string with a name"):
    Vector(ChronicleConvertRequest.FromJson("{}"),
      ChronicleConvertRequest.FromTts("03abc", "Imported")).foreach(request =>
      assertEquals(ChronicleConvertRequestCodec.decode(
        ChronicleConvertRequestCodec.encode(request)), Right(request)))
    assert(ChronicleConvertRequestCodec.decode("""{"tts":"03abc"}""").isLeft)
    assert(ChronicleConvertRequestCodec.decode(
      """{"json":"{}","name":"x"}""").isLeft)

  test("a convert response is the entry's fields and an optional TTS string"):
    Vector(Some("03abc"), None).foreach(tts =>
      val response = ChronicleConvertResponse(entry, tts)
      assertEquals(ChronicleConvertResponseCodec.decode(
        ChronicleConvertResponseCodec.encode(response)), Right(response)))
    assertEquals(ujson.read(ChronicleConvertResponseCodec.encode(
      ChronicleConvertResponse(entry, None))).obj.keySet.toSet,
      Set("json", "digest", "summary"))
