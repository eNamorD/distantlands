package oathdigital.frontend

import oathdigital.protocol.{ChronicleEntry, ChronicleScore, ChronicleSummary}

/** Chronicle entries and a fixed clock for the browsing suites. An entry's
  * JSON is a stand-in, `{"format":1,"name":…}`: the browser never reads a
  * Chronicle's JSON except to rename it. */
private[frontend] object ChronicleFixtures:
  val savedAt = "2026-10-29T12:00:00.000Z"
  val clock: () => String = () => savedAt

  def entry(name: String, digest: String = "d-1", game: Option[Int] = Some(1),
      json: Option[String] = None): ChronicleEntry =
    ChronicleEntry(
      json.getOrElse(ujson.write(ujson.Obj("format" -> 1, "name" -> name))),
      digest,
      ChronicleSummary(name, game, 20, Vector(
        ChronicleScore("red-lineage", 9), ChronicleScore("blue-lineage", 6))))
