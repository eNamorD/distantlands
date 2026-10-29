package oathdigital.protocol

import oathdigital.protocol.projection.ProjectionCodecSupport._

final case class ChronicleScore(lineage: String, score: Int)

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
      "scores" -> encoded(entry.summary.scores)(score => ujson.Obj(
        "lineage" -> score.lineage, "score" -> score.score)))
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
          _ <- exact(row, Set("lineage", "score"), child)
          lineage <- string(row, "lineage", child)
          score <- int(row, "score", child)
        yield ChronicleScore(lineage, score)
      }
    yield ChronicleEntry(json, digest, ChronicleSummary(name, game, target,
      scores))
