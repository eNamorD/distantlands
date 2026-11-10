package distantlands.serialization

import scala.util.control.NonFatal

import distantlands.application.chronicle.ChronicleJson
import distantlands.model.Chronicle

/** A Chronicle as a file a player keeps (Chronicle browsing design,
  * "ChronicleJsonCodec"): the event wire's Chronicle object behind a
  * `"format"` key, written compact. */
final class ChronicleJsonCodec extends ChronicleJson with GameEventJsonSupport:
  def encode(chronicle: Chronicle): String =
    ujson.write(ujson.Obj.from(
      ("format" -> ujson.Num(ChronicleJsonCodec.format)) +:
        encodeChronicle(chronicle).obj.toSeq))

  def decode(json: String): Either[String, Chronicle] =
    try
      val value = ujson.read(json)
      for
        format <- value.objOpt.flatMap(_.get("format")).flatMap(_.numOpt)
          .toRight("a Chronicle file names its format")
        _ <- Either.cond(format == ChronicleJsonCodec.format, (),
          s"Chronicle format ${format.toInt} is not supported")
        chronicle <- decodeChronicle(value, "$").left.map(error =>
          s"${error.path}: ${error.message}")
      yield chronicle
    catch
      case NonFatal(error) => Left("not a Chronicle file: " +
        Option(error.getMessage).getOrElse("malformed JSON"))

object ChronicleJsonCodec:
  val format: Int = 1
