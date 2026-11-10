package oathdigital.application.chronicle

import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.gameplay.setup.FirstGameSetupFixture.chronicle
import oathdigital.model._

/** The convert route's work (Chronicle browsing design, "POST
  * /chronicles/convert"). */
class ChronicleConversionSuite extends munit.FunSuite:
  private val conversion = ChronicleConversion(FirstGameSetupFixture.catalog)
  private val entries = ChronicleEntries()
  private val tts = ChronicleTtsCodec(FirstGameSetupFixture.catalog)

  test("JSON converts to its entry and its TTS string"):
    val json = entries.entry(chronicle).json
    val converted = conversion.fromJson(json).fold(fail(_), identity)
    assertEquals(converted.entry, entries.entry(chronicle))
    assertEquals(converted.tts, tts.encode(chronicle).toOption.map(_.text))
    assert(converted.tts.nonEmpty)

  test("a TTS string converts under the name it is given, trimmed"):
    val text = tts.encode(chronicle).fold(fail(_), _.text)
    val converted = conversion.fromTts(text, "  Imported ")
      .fold(fail(_), identity)
    assertEquals(converted.entry, entries.entry(chronicle.copy(
      name = "Imported")))

  test("a Chronicle TTS cannot hold converts without a TTS string"):
    val colorless = chronicle.copy(lineages = Vector(LineageState(
      LineageId("purple"), None, Role.Citizen, Vector.empty, Vector.empty)))
    val converted = conversion.fromJson(entries.entry(colorless).json)
      .fold(fail(_), identity)
    assertEquals(converted.tts, None)

  test("a Chronicle that stores a card twice is refused with the reason"):
    val doubled = chronicle.copy(relicDeck =
      chronicle.relicDeck :+ chronicle.relicDeck.head)
    val refused = conversion.fromJson(entries.entry(doubled).json)
    assert(refused.isLeft, refused)
    assertEquals(conversion.decode(entries.entry(doubled).json),
      refused.map(_ => doubled))

  test("text that is not a Chronicle is refused with the codec's reason"):
    assert(conversion.fromJson("{").left.exists(
      _.startsWith("not a Chronicle file")))
    assert(conversion.fromTts("nonsense", "Imported").isLeft)
