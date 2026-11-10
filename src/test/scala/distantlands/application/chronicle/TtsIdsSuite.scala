package distantlands.application.chronicle

import distantlands.gameplay.setup.FirstGameSetupFixture
import distantlands.model._

/** The TTS id tables against the TTS codec's own name tables (resource
  * copied from chronicle-codec.js at f6051bb0) and the catalog's names. A
  * renumbering on either side fails here. */
class TtsIdsSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val ids = TtsIds(catalog)
  private val mapping = ujson.read(scala.io.Source.fromResource(
    "tts/chronicle-codec-mapping.json").mkString)

  private def catalogName(id: CardId): Option[String] = id match
    case d: DenizenId => catalog.denizen(d).map(_.name)
    case r: RelicId => catalog.relic(r).map(_.name)
    case e: EdificeId => catalog.edifice(e).map(_.intact.name)
    case _ => None

  test("every TTS card id names the catalog card with the same name"):
    val wrong = mapping("ids").obj.toVector.flatMap((number, name) =>
      val found = ids.card(number.toInt).toOption.flatMap(catalogName)
      Option.when(!found.contains(name.str))(s"$number ${name.str}: $found"))
    assertEquals(wrong, Vector.empty)

  test("every TTS legacy id names the catalog legacy with the same name"):
    val wrong = mapping("legacies").obj.toVector.flatMap((number, name) =>
      val found = ids.legacy(number.toInt).toOption
        .flatMap(catalog.legacy).map(_.name)
      Option.when(!found.contains(name.str))(s"$number ${name.str}: $found"))
    assertEquals(wrong, Vector.empty)

  test("each TTS site index is the catalog site with its canonical name"):
    val canonical = mapping("sites").arr.toVector
      .map(pair => pair(1).num.toInt -> pair(0).str)
      .distinctBy(_._1)
    assertEquals(canonical.map(_._1), Vector.range(0, 24))
    canonical.foreach((index, name) =>
      assertEquals(ids.site(index).toOption.flatMap(catalog.site).map(_.name),
        Some(name), index))

  test("ids map back to the same TTS numbers"):
    val cards = mapping("ids").obj.keys.map(_.toInt).toVector
    cards.foreach(n =>
      assertEquals(ids.card(n).flatMap(ids.ttsCard), Right(n), n))
    Vector.range(0, 24).foreach(i =>
      assertEquals(ids.site(i).flatMap(ids.ttsSite), Right(i), i))
    Vector.range(1, 37).foreach(n =>
      assertEquals(ids.legacy(n).flatMap(ids.ttsLegacy), Right(n), n))

  test("a number the catalog lacks is refused by number"):
    assertEquals(ids.card(94), Left("TTS card 94 is not in the catalog"))
    assertEquals(ids.card(600), Left("TTS card 600 is not in the catalog"))
    assertEquals(ids.site(24), Left("TTS site 24 is not in the catalog"))
    assertEquals(ids.legacy(37), Left("TTS legacy 37 is not in the catalog"))
