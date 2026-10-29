package oathdigital.application.chronicle

import oathdigital.catalog.ExecutableCatalog
import oathdigital.model._

/** TTS Chronicle numbers for catalog ids (harsch1/oath-nf-tts-scripts
  * chronicle-codec.js at f6051bb0). Deck cards are their printed number,
  * relics 400 plus their number, edifices 500 plus theirs, and legacies their
  * number. Sites are an index; both faces of a site share it. A number is
  * accepted only for a card the catalog holds. */
final class TtsIds(catalog: ExecutableCatalog):
  def card(tts: Int): Either[String, CardId] =
    val id: Option[CardId] =
      if tts >= 1 && tts < 400 then Some(DenizenId(tts.toString))
        .filter(d => catalog.denizen(d).nonEmpty)
      else if tts > 400 && tts < 500 then Some(RelicId(f"R${tts - 400}%02d"))
        .filter(r => catalog.relic(r).nonEmpty)
      else if tts > 500 && tts < 600 then Some(EdificeId(f"E${tts - 500}%02d"))
        .filter(e => catalog.edifice(e).nonEmpty)
      else None
    id.toRight(s"TTS card $tts is not in the catalog")

  def ttsCard(id: CardId): Either[String, Int] = id match
    case DenizenId(n) => n.toIntOption.filter(v => v >= 1 && v < 400)
      .toRight(s"denizen $n has no TTS number")
    case RelicId(r) => numbered(r, "R").map(400 + _)
      .toRight(s"relic $r has no TTS number")
    case EdificeId(e) => numbered(e, "E").map(500 + _)
      .toRight(s"edifice $e has no TTS number")
    case other => Left(s"${other.value} has no TTS number")

  def site(index: Int): Either[String, SiteId] =
    TtsIds.sites.lift(index).toRight(s"TTS site $index is not in the catalog")

  def ttsSite(id: SiteId): Either[String, Int] =
    Some(TtsIds.sites.indexOf(id)).filter(_ >= 0)
      .toRight(s"site ${id.value} has no TTS index")

  def legacy(tts: Int): Either[String, LegacyId] =
    Some(LegacyId(f"L$tts%02d")).filter(l => tts >= 1 &&
      catalog.legacy(l).nonEmpty).toRight(s"TTS legacy $tts is not in the catalog")

  def ttsLegacy(id: LegacyId): Either[String, Int] =
    numbered(id.value, "L").toRight(s"legacy ${id.value} has no TTS number")

  private def numbered(value: String, prefix: String): Option[Int] =
    Option.when(value.startsWith(prefix))(value.drop(prefix.length))
      .flatMap(_.toIntOption).filter(_ > 0)

object TtsIds:
  /** The sites in TTS index order: alphabetical by name. */
  val sites: Vector[SiteId] = Vector(
    "ancient-city", "broken-peaks", "buried-giant", "deep-woods",
    "desolate-shore", "dunes", "fair-isle", "golden-valley", "great-slum",
    "green-shore", "headwaters", "hidden-place", "mines", "narrow-pass",
    "painted-towers", "riverbank", "rocky-coast", "salt-flats",
    "shrouded-woods", "solitary-pillar", "standing-stones", "steppe",
    "sunken-isles", "tidal-marshes").map(name => SiteId(s"site:$name"))
