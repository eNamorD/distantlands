package oathdigital.protocol

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

  test("an entry refuses an unknown field"):
    val raw = ChronicleEntryCodec.encodeValue(entry)
    raw("extra") = 1
    assert(ChronicleEntryCodec.decodeValue(raw, "$").isLeft)
