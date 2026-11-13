package distantlands.model

enum Orientation { case FaceUp, FaceDown }

enum EdificeSide { case Intact, Ruined }

sealed trait CardState extends Product with Serializable:
  def id: CardId

sealed trait AdviserState extends CardState
sealed trait SiteDenizenState extends CardState:
  def tokens: Tokens

final case class DenizenState(
    id: DenizenId,
    orientation: Orientation,
    tokens: Tokens
) extends AdviserState
    with SiteDenizenState

final case class VisionState(
    id: VisionId,
    orientation: Orientation
) extends AdviserState

/**
 * Edifices are special denizens. They can occupy a site's denizen slots, but
 * keep their intact/ruined side instead of an ordinary faceup/facedown flag.
 */
final case class EdificeState(
    id: EdificeId,
    side: EdificeSide,
    tokens: Tokens
) extends SiteDenizenState

final case class RelicState(
    id: RelicId,
    orientation: Orientation,
    tokens: Tokens
) extends CardState

final case class LegacyState(id: LegacyId, active: Boolean) extends CardState

/** A region of the map. `slots` is the number of sites it has room for
  * (rulebook, "The Map at Large"). */
enum Region(val key: String, val slots: Int):
  case Cradle extends Region("cradle", 2)
  case Provinces extends Region("provinces", 3)
  case Hinterland extends Region("hinterland", 3)
object Region:
  val all: Vector[Region] = Vector(Cradle, Provinces, Hinterland)

/** The shared card piles of a game. The legacy deck is not stored: it is
  * every legacy no lineage holds and the public row does not show
  * (Legacies design, ruling 3), and every legacy draw happens at game
  * start. `publicLegacies` is the Public Ambitions row, faceup. */
final case class CardZones(
    worldDeck: Vector[WorldCardId],
    relicDeck: Vector[RelicId],
    edificeDeck: Vector[EdificeId],
    regionalDiscards: Map[Region, Vector[WorldCardId]],
    publicLegacies: Vector[LegacyId] = Vector.empty
):
  def discard(region: Region): Vector[WorldCardId] =
    regionalDiscards.getOrElse(region, Vector.empty)

  /** The cards of a pile Search draws from, in stored order: the world deck
    * top first, a discard pile top last. */
  def pile(source: SearchSource): Vector[WorldCardId] = source match
    case SearchSource.WorldDeck => worldDeck
    case SearchSource.RegionalDiscard(region) => discard(region)

  /** These zones with `source` holding `cards`, in stored order. */
  def withPile(source: SearchSource, cards: Vector[WorldCardId]): CardZones =
    source match
      case SearchSource.WorldDeck => copy(worldDeck = cards)
      case SearchSource.RegionalDiscard(region) =>
        copy(regionalDiscards = regionalDiscards.updated(region, cards))

/** Stable, container-qualified target for a denizen printed at a site. */
final case class SiteDenizenTarget(siteId: SiteId, denizenId: DenizenId):
  def stableKey: String = s"site:${siteId.value}:denizen:${denizenId.value}"
