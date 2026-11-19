package distantlands.protocol.projection

import distantlands.protocol.ChronicleEntryCodec
import ProjectionCodecSupport._

/** The wire of [[ChronicleDetailsProjection]] (Chronicle Details design,
  * slice 1). Every key is required; a lineage's `role` and `atlasRecent`
  * are null when absent. Scorepad marks keep the Chronicle summary's
  * wire. */
private[projection] object ChronicleDetailsCodec:
  private val Fields = Set("eraTarget", "scorepad", "lineages", "atlasRecent")
  private val LineageFields = Set("lineage", "score", "role", "legacies")

  def encode(value: ChronicleDetailsProjection): ujson.Value = ujson.Obj(
    "eraTarget" -> value.eraTarget,
    "scorepad" -> encoded(value.scorepad)(column =>
      encoded(column)(ChronicleEntryCodec.encodeScorepadEntry)),
    "lineages" -> encoded(value.lineages)(lineage => ujson.Obj(
      "lineage" -> lineage.lineage, "score" -> lineage.score,
      "role" -> stringOption(lineage.role),
      "legacies" -> encoded(lineage.legacies)(
        WorldProjectionCodec.encodeCard))),
    "atlasRecent" -> option(value.atlasRecent)(
      WorldProjectionCodec.encodeSite))

  def decode(raw: ujson.Value, path: String)
      : Result[ChronicleDetailsProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, Fields, path)
    target <- int(row, "eraTarget", path)
    columnRaws <- array(row, "scorepad", path)
    scorepad <- traverse(columnRaws, s"$path.scorepad") { (raw, column) =>
      array(raw, column).flatMap(marks =>
        traverse(marks, column)(ChronicleEntryCodec.decodeScorepadEntry)) }
    lineageRaws <- array(row, "lineages", path)
    lineages <- traverse(lineageRaws, s"$path.lineages")(decodeLineage)
    recent <- optional(row, "atlasRecent", path)(
      WorldProjectionCodec.decodeSite)
  yield ChronicleDetailsProjection(target, scorepad, lineages, recent)

  private def decodeLineage(raw: ujson.Value, path: String)
      : Result[LineageDetailsProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, LineageFields, path)
    lineage <- string(row, "lineage", path)
    score <- int(row, "score", path)
    role <- optional(row, "role", path)(ChronicleEntryCodec.knownRole)
    legacyRaws <- array(row, "legacies", path)
    legacies <- traverse(legacyRaws, s"$path.legacies")(
      WorldProjectionCodec.decodeCard)
  yield LineageDetailsProjection(lineage, score, role, legacies)
