package distantlands.serialization

import distantlands.application.chronicle.ChronicleJson
import distantlands.catalog.holding.{ArbiterCard, LightFingersCard}
import distantlands.gameplay.chronicle.EvolvedChronicleFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.chronicle
import distantlands.model._

/** Canonical Chronicle JSON (Chronicle browsing design, "ChronicleJsonCodec"). */
class ChronicleJsonCodecSuite extends munit.FunSuite:
  private val codec = ChronicleJsonCodec()

  test("the first-game and the evolved Chronicle round trip"):
    assertEquals(codec.decode(codec.encode(chronicle)), Right(chronicle))
    val evolved = EvolvedChronicleFixture.record
    assertEquals(codec.decode(codec.encode(evolved)), Right(evolved))

  test("encoding is compact, versioned and starts with the name"):
    val text = codec.encode(chronicle)
    assert(text.startsWith("""{"format":1,"name":"Fixture Chronicle","""),
      text)
    assert(!text.contains(" \"") && !text.contains("\n"), text)

  test("encoding does not depend on map or set insertion order"):
    val sources = Vector(ArbiterCard.id, LightFingersCard.id)
    val altered = FoundationState(FoundationFace.Altered, sources.toSet)
    val reordered = FoundationState(FoundationFace.Altered,
      sources.reverse.toSet)
    val normal = FoundationState(FoundationFace.Normal, Set.empty)
    val one = chronicle.copy(foundations = Map(
      FoundationNumber.IV -> altered, FoundationNumber.I -> normal))
    val other = chronicle.copy(foundations = Map(
      FoundationNumber.I -> normal, FoundationNumber.IV -> reordered))
    assertEquals(codec.encode(one), codec.encode(other))

  test("decoding refuses another format, malformed text and a blank name"):
    val text = codec.encode(chronicle)
    assertEquals(codec.decode(text.replace("\"format\":1", "\"format\":2")),
      Left("Chronicle format 2 is not supported"))
    assert(codec.decode("{").left.exists(_.startsWith("not a Chronicle file")))
    assertEquals(codec.decode("[]"), Left("a Chronicle file names its format"))
    assertEquals(codec.decode(text.replace("Fixture Chronicle", "")),
      Left("$.name: a Chronicle needs a name"))

  test("the application finds this codec through ServiceLoader"):
    assert(ChronicleJson.default.isInstanceOf[ChronicleJsonCodec])
