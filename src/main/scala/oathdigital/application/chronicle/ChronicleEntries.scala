package oathdigital.application.chronicle

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

import oathdigital.model.Chronicle
import oathdigital.protocol.{ChronicleEntry, ChronicleScore, ChronicleSummary}

/** A Chronicle as a browser keeps it (Chronicle browsing design, "Digest"
  * and "Summary"). */
final class ChronicleEntries(val json: ChronicleJson = ChronicleJson.default):
  def entry(chronicle: Chronicle): ChronicleEntry =
    val text = json.encode(chronicle)
    ChronicleEntry(text, ChronicleEntries.digest(text),
      ChronicleEntries.summary(chronicle))

object ChronicleEntries:
  /** SHA-256 of `text` as UTF-8, in lowercase hex. */
  def digest(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.getBytes(UTF_8))
      .map(byte => f"${byte & 0xff}%02x").mkString

  def summary(chronicle: Chronicle): ChronicleSummary =
    val era = chronicle.era
    ChronicleSummary(chronicle.name,
      // A TTS import keeps lineages but no scorepad: its game is unknown.
      Option.when(era.games.nonEmpty || chronicle.lineages.isEmpty)(
        era.games.size + 1),
      era.targetScore,
      chronicle.lineages.map(lineage =>
        ChronicleScore(lineage.id.value, era.score(lineage.id))))
