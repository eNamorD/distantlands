package distantlands.protocol

import distantlands.protocol.projection.ProjectionCodecSupport._

/** A lineage's era score, and its Imperial role: `"chancellor"` or
  * `"citizen"`, absent for an Exile. An entry saved before roles were kept
  * has none. `legacies` are the lineage's legacies in held order, every face
  * shown (the Chronicle is its owner's own record); an entry saved before
  * they were kept has none. */
final case class ChronicleScore(lineage: String, score: Int,
    role: Option[String] = None,
    legacies: Vector[ChronicleLegacy] = Vector.empty)

/** A legacy a lineage holds, by its printed name, and whether it is
  * active. */
final case class ChronicleLegacy(name: String, active: Boolean)

object ChronicleScore:
  val roles: Set[String] = Set("chancellor", "citizen")

/** One foundation setup card as a Chronicle lists it (Foundations design,
  * slice 1): its number, 1 to 6; the face the next game starts on, `normal`
  * or `altered`; and that face's printed name. */
final case class ChronicleFoundation(number: Int, face: String, name: String)

object ChronicleFoundation:
  val faces: Set[String] = Set("normal", "altered")

/** What a browser lists for a Chronicle without decoding it (Chronicle
  * browsing design, "Summary"). `game` is the next game's number; it is
  * absent when the Chronicle has lineages but no scorepad, as a TTS import
  * does. `foundations` is the six setup cards, I to VI; an entry saved
  * before they were kept has none. */
final case class ChronicleSummary(name: String, game: Option[Int],
    eraTarget: Int, scores: Vector[ChronicleScore],
    foundations: Vector[ChronicleFoundation] = Vector.empty)

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
        if score.legacies.nonEmpty then row("legacies") = encoded(
          score.legacies)(legacy => ujson.Obj("name" -> legacy.name,
            "active" -> legacy.active))
        row
      },
      "foundations" -> encoded(entry.summary.foundations)(foundation =>
        ujson.Obj("number" -> foundation.number, "face" -> foundation.face,
          "name" -> foundation.name)))
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
      _ <- exact(summary, Set("name", "game", "eraTarget", "scores",
        "foundations"), summaryPath)
      name <- string(summary, "name", summaryPath)
      game <- optionalAbsent(summary, "game", summaryPath)(int)
      target <- int(summary, "eraTarget", summaryPath)
      scoreRaws <- array(summary, "scores", summaryPath)
      scores <- traverse(scoreRaws, s"$summaryPath.scores") { (raw, child) =>
        for
          row <- obj(raw, child)
          _ <- exact(row, Set("lineage", "score", "role", "legacies"), child)
          lineage <- string(row, "lineage", child)
          score <- int(row, "score", child)
          role <- optionalAbsent(row, "role", child)((raw, at) =>
            string(raw, at).filterOrElse(ChronicleScore.roles,
              ProtocolDecodeFailure.InvalidValue(at, "unknown role")))
          legacyRaws <- default(row, "legacies", child,
            Vector.empty[ujson.Value])(array)
          legacies <- traverse(legacyRaws, s"$child.legacies") { (raw, at) =>
            for
              legacy <- obj(raw, at)
              _ <- exact(legacy, Set("name", "active"), at)
              name <- string(legacy, "name", at)
              active <- bool(legacy, "active", at)
            yield ChronicleLegacy(name, active)
          }
        yield ChronicleScore(lineage, score, role, legacies)
      }
      // An entry a browser saved before foundations were kept has none.
      foundationRaws <- default(summary, "foundations", summaryPath,
        Vector.empty[ujson.Value])(array)
      foundations <- traverse(foundationRaws, s"$summaryPath.foundations") {
        (raw, child) =>
          for
            row <- obj(raw, child)
            _ <- exact(row, Set("number", "face", "name"), child)
            number <- int(row, "number", child)
            face <- string(row, "face", child).filterOrElse(
              ChronicleFoundation.faces, ProtocolDecodeFailure.InvalidValue(
                s"$child.face", "unknown face"))
            name <- string(row, "name", child)
          yield ChronicleFoundation(number, face, name)
      }
    yield ChronicleEntry(json, digest, ChronicleSummary(name, game, target,
      scores, foundations))

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
