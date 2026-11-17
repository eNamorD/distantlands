package distantlands.application.chronicle

import java.nio.charset.StandardCharsets.UTF_8
import java.security.MessageDigest

import distantlands.application.FoundationPresentation
import distantlands.gameplay.cards.NewFoundations
import distantlands.model.{Chronicle, Role}
import distantlands.protocol.{ChronicleEntry, ChronicleLegacy, ChronicleScore,
  ChronicleScorepadEntry, ChronicleSummary}

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
        ChronicleScore(lineage.id.value, era.score(lineage.id),
          roleName(lineage.role),
          // By printed name, every face shown: the Chronicle is its owner's
          // record (Legacies implementation ruling R39). A legacy the
          // catalog does not know keeps its id.
          lineage.legacies.map(legacy => ChronicleLegacy(
            NewFoundations.catalog.legacy(legacy.id).fold(legacy.id.value)(
              _.name), legacy.active)))),
      FoundationPresentation.ofChronicle(chronicle),
      era.games.map(_.entries.map(entry => ChronicleScorepadEntry(
        entry.lineage.value, entry.totalScore, roleName(entry.endingRole)))))

  /** An Imperial role as the summary names it; an Exile has none. */
  private def roleName(role: Role): Option[String] = role match
    case Role.Chancellor => Some("chancellor")
    case Role.Citizen => Some("citizen")
    case Role.Exile => None
