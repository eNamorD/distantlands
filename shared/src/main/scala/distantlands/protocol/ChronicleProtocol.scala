package distantlands.protocol

import distantlands.protocol.projection.ProjectionCodecSupport._

/** A lineage's era score, and its Imperial role: `"chancellor"` or
  * `"citizen"`, absent for an Exile. An entry saved before roles were kept
  * has none. */
final case class ChronicleScore(lineage: String, score: Int,
    role: Option[String] = None)

object ChronicleScore:
  val roles: Set[String] = Set("chancellor", "citizen")

/** What a browser lists for a Chronicle without decoding it (Chronicle
  * browsing design, "Summary"). `game` is the next game's number; it is
  * absent when the Chronicle has lineages but no scorepad, as a TTS import
  * does. */
final case class ChronicleSummary(name: String, game: Option[Int],
    eraTarget: Int, scores: Vector[ChronicleScore])

/** A Chronicle as a browser keeps it: canonical JSON, the JSON's SHA-256
  * digest, and its summary. */
final case class ChronicleEntry(json: String, digest: String,
    summary: ChronicleSummary)

object ChronicleEntryCodec:
  val fields: Set[String] = Set("json", "digest", "summary")

  def encodeValue(entry: ChronicleEntry): ujson.Obj =
    val summary = ujson.Obj("name" -> entry.summary.name,
      "eraTarget" -> entry.summary.eraTarget,
      "scores" -> encoded(entry.summary.scores) { score =>
        val row = ujson.Obj("lineage" -> score.lineage, "score" -> score.score)
        score.role.foreach(role => row("role") = role)
        row
      })
    entry.summary.game.foreach(game => summary("game") = game)
    ujson.Obj("json" -> entry.json, "digest" -> entry.digest,
      "summary" -> summary)

  def decodeValue(raw: ujson.Value, path: String): Result[ChronicleEntry] =
    for
      value <- obj(raw, path); _ <- exact(value, fields, path)
      entry <- decodeFields(value, path)
    yield entry

  /** The entry's fields, leaving any other keys to the caller. */
  def decodeFields(value: ujson.Obj, path: String): Result[ChronicleEntry] =
    val summaryPath = s"$path.summary"
    for
      json <- string(value, "json", path)
      digest <- string(value, "digest", path)
      summaryRaw <- field(value, "summary", path)
      summary <- obj(summaryRaw, summaryPath)
      _ <- exact(summary, Set("name", "game", "eraTarget", "scores"),
        summaryPath)
      name <- string(summary, "name", summaryPath)
      game <- optionalAbsent(summary, "game", summaryPath)(int)
      target <- int(summary, "eraTarget", summaryPath)
      scoreRaws <- array(summary, "scores", summaryPath)
      scores <- traverse(scoreRaws, s"$summaryPath.scores") { (raw, child) =>
        for
          row <- obj(raw, child)
          _ <- exact(row, Set("lineage", "score", "role"), child)
          lineage <- string(row, "lineage", child)
          score <- int(row, "score", child)
          role <- optionalAbsent(row, "role", child)((raw, at) =>
            string(raw, at).filterOrElse(ChronicleScore.roles,
              ProtocolDecodeFailure.InvalidValue(at, "unknown role")))
        yield ChronicleScore(lineage, score, role)
      }
    yield ChronicleEntry(json, digest, ChronicleSummary(name, game, target,
      scores))

/** `POST /chronicles/convert`'s body: a saved Chronicle's JSON, or a TTS
  * string with the name it is imported under. */
enum ChronicleConvertRequest:
  case FromJson(json: String)
  case FromTts(tts: String, name: String)

/** The convert route's answer: `{json, digest, summary, tts?}`. */
final case class ChronicleConvertResponse(entry: ChronicleEntry,
    tts: Option[String])

object ChronicleConvertRequestCodec:
  import TrustedGameCodecFields.{exact => exactKeys, parse, text}

  def encode(request: ChronicleConvertRequest): String =
    ujson.write(request match
      case ChronicleConvertRequest.FromJson(json) => ujson.Obj("json" -> json)
      case ChronicleConvertRequest.FromTts(tts, name) =>
        ujson.Obj("tts" -> tts, "name" -> name))

  def decode(json: String)
      : Either[ProtocolDecodeFailure, ChronicleConvertRequest] =
    parse(json) {
      case raw: ujson.Obj if raw.value.contains("json") =>
        exactKeys(raw, Vector("json"), "$").flatMap(root =>
          text(root, "json", "$")).map(ChronicleConvertRequest.FromJson(_))
      case raw => for
        root <- exactKeys(raw, Vector("tts", "name"), "$")
        tts <- text(root, "tts", "$")
        name <- text(root, "name", "$")
      yield ChronicleConvertRequest.FromTts(tts, name)
    }

object ChronicleConvertResponseCodec:
  def encode(response: ChronicleConvertResponse): String =
    val value = ChronicleEntryCodec.encodeValue(response.entry)
    response.tts.foreach(tts => value("tts") = tts)
    ujson.write(value)

  def decode(json: String)
      : Either[ProtocolDecodeFailure, ChronicleConvertResponse] =
    TrustedGameCodecFields.parse(json) { raw => for
      value <- obj(raw, "$")
      _ <- exact(value, ChronicleEntryCodec.fields + "tts", "$")
      entry <- ChronicleEntryCodec.decodeFields(value, "$")
      tts <- optionalAbsent(value, "tts", "$")(string)
    yield ChronicleConvertResponse(entry, tts) }
