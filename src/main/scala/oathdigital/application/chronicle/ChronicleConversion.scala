package oathdigital.application.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.setup.ChronicleAccounting
import oathdigital.model.Chronicle
import oathdigital.protocol.ChronicleEntry

/** A converted Chronicle: the entry a browser keeps, and the TTS string
  * when TTS can hold the Chronicle. */
final case class ConvertedChronicle(entry: ChronicleEntry, tts: Option[String])

/** The convert route's work (Chronicle browsing design, "POST
  * /chronicles/convert"): read a Chronicle from JSON or TTS, check that
  * every card is known and stored once, and write it back both ways. */
final class ChronicleConversion(catalog: ExecutableCatalog,
    json: ChronicleJson = ChronicleJson.default):
  private val entries = ChronicleEntries(json)
  private val tts = ChronicleTtsCodec(catalog)

  def fromJson(text: String): Either[String, ConvertedChronicle] =
    decode(text).map(converted)

  /** TTS carries no name, so the caller gives one; it is trimmed. */
  def fromTts(text: String, name: String): Either[String, ConvertedChronicle] =
    tts.decode(text, name.trim).flatMap(checked).map(converted)

  /** A saved Chronicle's JSON, checked as the convert route checks it. */
  def decode(text: String): Either[String, Chronicle] =
    json.decode(text).flatMap(checked)

  def entry(chronicle: Chronicle): ChronicleEntry = entries.entry(chronicle)

  private def checked(chronicle: Chronicle): Either[String, Chronicle] =
    ChronicleAccounting.knownAndUnique(catalog, chronicle).map(_ => chronicle)

  private def converted(chronicle: Chronicle): ConvertedChronicle =
    ConvertedChronicle(entries.entry(chronicle),
      tts.encode(chronicle).toOption.map(_.text))
