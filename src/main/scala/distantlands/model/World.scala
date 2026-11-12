package distantlands.model

enum Role(val isImperial: Boolean):
  case Exile extends Role(false)
  case Citizen extends Role(true)
  case Chancellor extends Role(true)

  /** Only an Exile's board has a Vision slot, so only an Exile can play a
    * true Vision faceup (Empire phase design, "Boards"). */
  def hasVisionSlot: Boolean = !isImperial

sealed trait ForceKind extends Product with Serializable
object ForceKind:
  final case class Exile(lineage: LineageId) extends ForceKind
  case object Imperial extends ForceKind
  case object Bandit extends ForceKind

sealed trait SiteForces extends Product with Serializable:
  /** The warbands of `kind` standing here: 0 when the site is empty or
    * another kind holds it. */
  def count(kind: ForceKind): Int = this match
    case SiteForces.Occupied(`kind`, held) => held
    case _ => 0

  /** The warbands standing here of whatever kind: 0 when the site is
    * empty. */
  def total: Int = this match
    case SiteForces.Occupied(_, held) => held
    case SiteForces.Empty => 0

object SiteForces:
  case object Empty extends SiteForces

  final case class Occupied(kind: ForceKind, count: Int) extends SiteForces:
    require(count > 0, "occupied site forces must be positive")

sealed trait SiteRuler extends Product with Serializable
object SiteRuler:
  case object Unruled extends SiteRuler
  case object Bandits extends SiteRuler
  case object Empire extends SiteRuler
  final case class Player(playerId: PlayerId) extends SiteRuler

sealed trait SiteRuleError extends Product with Serializable
object SiteRuleError:
  final case class UnknownLineage(lineage: LineageId) extends SiteRuleError
  final case class DuplicateCurrentLineage(
      lineage: LineageId,
      players: Vector[PlayerId]
  ) extends SiteRuleError

/** Direct interpretation of the physical warbands stored at a site. */
object SiteRule:
  import SiteRuleError._
  import SiteRuler._

  def ruler(forces: SiteForces, players: Vector[PlayerState])
      : Either[SiteRuleError, SiteRuler] = forces match
    case SiteForces.Empty => Right(Unruled)
    case SiteForces.Occupied(ForceKind.Bandit, _) => Right(Bandits)
    case SiteForces.Occupied(ForceKind.Imperial, _) => Right(Empire)
    case SiteForces.Occupied(ForceKind.Exile(lineage), _) =>
      players.filter(_.lineage == lineage).map(_.player) match
        case Vector(player) => Right(Player(player))
        case Vector() => Left(UnknownLineage(lineage))
        case duplicates => Left(DuplicateCurrentLineage(lineage, duplicates))

  def ruledBy(forces: SiteForces, players: Vector[PlayerState], player: PlayerId)
      : Either[SiteRuleError, Boolean] =
    ruler(forces, players).map(_ == Player(player))

  /** `ruler` as the player who rules for it now (Empire phase design,
    * "Ruler"): the Empire becomes its governor; any other ruler is
    * unchanged. The Empire stays the Empire only with no Chancellor. */
  def governed(ready: ReadyGame, ruler: SiteRuler): SiteRuler = ruler match
    case Empire => Imperials.governor(ready).fold[SiteRuler](Empire)(Player(_))
    case other => other

  /** Who rules `site` as a player, for every rule that asks which player
    * rules a site. `ruler` stays the reading for "does the Empire hold this
    * site". */
  def rulingPlayer(ready: ReadyGame, site: SiteId): Option[SiteRuler] =
    ready.game.current.map.sites.get(site)
      .flatMap(state => ruler(state.forces, ready.game.current.players)
        .toOption)
      .map(governed(ready, _))

  def sameRuler(left: SiteForces, right: SiteForces,
      players: Vector[PlayerState]): Either[SiteRuleError, Boolean] =
    for
      leftRuler <- ruler(left, players)
      rightRuler <- ruler(right, players)
    yield leftRuler != Unruled && leftRuler == rightRuler

  def enemies(left: SiteRuler, right: SiteRuler): Boolean =
    left != Unruled && right != Unruled && left != right

final case class SiteState(
    forces: SiteForces,
    denizens: Vector[SiteDenizenState],
    relics: Vector[RelicState],
    tokens: Tokens
)

object SiteState:
  /** A site placed from the Atlas as a bandit site, populated as Foundation
    * I and Setup With No Empire both print: `denizens` as given, `relics`
    * facedown, `bandits` warbands, none for a site of capacity 0, and its
    * printed `resources`. */
  def banditSite(bandits: Int, resources: Tokens,
      denizens: Vector[SiteDenizenState], relics: Vector[RelicId]): SiteState =
    SiteState(
      if bandits == 0 then SiteForces.Empty
      else SiteForces.Occupied(ForceKind.Bandit, bandits),
      denizens, relics.map(RelicState(_, Orientation.FaceDown, Tokens.empty)),
      resources)

  /** The relic slots of a site with `slots` that `stored` relics leave
    * open, never fewer than 0. */
  def openRelicSlots(slots: Int, stored: Int): Int = math.max(0, slots - stored)

/** The sites on the map, by region. A region may hold fewer sites than its
  * slots: under Wide Horizons and after Setup With No Empire the slots past
  * the last site placed are empty (Foundations design, slice 3). Slots fill
  * going Hinterward, so the empty ones follow every site. */
final case class MapState(
    cradle: Vector[SiteId],
    provinces: Vector[SiteId],
    hinterland: Vector[SiteId],
    sites: Map[SiteId, SiteState]
):
  def inPlay: Vector[SiteId] = cradle ++ provinces ++ hinterland

  /** The sites in `region`, top slot first. */
  def sitesIn(region: Region): Vector[SiteId] = region match
    case Region.Cradle => cradle
    case Region.Provinces => provinces
    case Region.Hinterland => hinterland

  /** The slots of `region` that hold no site. */
  def emptySlots(region: Region): Int =
    math.max(0, region.slots - sitesIn(region).size)

  /** The region of the next empty slot going Hinterward: the first region
    * not yet full. None on a full map. */
  def nextSlot: Option[Region] = Region.all.find(emptySlots(_) > 0)

  /** The Hintermost site: the last site in Hinterward order. None on a map
    * with no site. */
  def hintermost: Option[SiteId] = hinterward.lastOption

  /** The map with `site` in the next slot of `region`, after its sites. */
  def withSite(region: Region, site: SiteId, state: SiteState): MapState =
    val placed = copy(sites = sites.updated(site, state))
    region match
      case Region.Cradle => placed.copy(cradle = cradle :+ site)
      case Region.Provinces => placed.copy(provinces = provinces :+ site)
      case Region.Hinterland => placed.copy(hinterland = hinterland :+ site)

  def regionOf(site: SiteId): Option[Region] =
    if cradle.contains(site) then Some(Region.Cradle)
    else if provinces.contains(site) then Some(Region.Provinces)
    else if hinterland.contains(site) then Some(Region.Hinterland)
    else None

  /** The map from the Cradle out (CR pp. 9, 45): the order the Chronicle's
    * Stars task archives rival sites in and keeps the Empire's sites in. */
  def hinterward: Vector[SiteId] = inPlay

object MapState:
  /** The site slots on the map. */
  val slots: Int = Region.all.map(_.slots).sum

  /** The map with `placed` in its slots going Hinterward: top to bottom in
    * the Cradle, then the Provinces, then the Hinterland. Slots past the
    * last site stay empty. More sites than the map has slots overfill the
    * Hinterland, which `DomainValidation` reports. */
  def inSlots(placed: Vector[(SiteId, SiteState)]): MapState =
    val (cradle, outer) = placed.map(_._1).splitAt(Region.Cradle.slots)
    val (provinces, hinterland) = outer.splitAt(Region.Provinces.slots)
    MapState(cradle, provinces, hinterland, placed.toMap)

sealed trait AtlasEntry extends Product with Serializable
object AtlasEntry:
  final case class StoredSite(
      id: SiteId,
      denizens: Vector[SiteDenizenState],
      relics: Vector[RelicState],
      edifice: Option[EdificeId] = None
  ) extends AtlasEntry:
    /** The cards in the site's denizen slots once it is placed as a bandit
      * site: its edifice ruined, then its denizens faceup. */
    def placedDenizens: Vector[SiteDenizenState] =
      edifice.toVector.map(EdificeState(_, EdificeSide.Ruined, Tokens.empty)) ++
        denizens.map:
          case denizen: DenizenState =>
            denizen.copy(orientation = Orientation.FaceUp)
          case stored: EdificeState => stored.copy(side = EdificeSide.Ruined)

  case object EmpireDivider extends AtlasEntry

/** An end of the Atlas box: the Recent Sites side, its front, or the
  * Forgotten Sites side, its back. */
enum AtlasEnd(val key: String):
  case Recent extends AtlasEnd("recent")
  case Forgotten extends AtlasEnd("forgotten")
object AtlasEnd:
  val all: Vector[AtlasEnd] = Vector(Recent, Forgotten)
  def fromKey(key: String): Option[AtlasEnd] = all.find(_.key == key)

/**
 * One ordered Atlas sequence. `entries.head` is the Recent end and
 * `entries.last` is the Forgotten end.
 */
final case class AtlasState(entries: Vector[AtlasEntry]):
  /** The stored sites, Recent end first. */
  def sites: Vector[AtlasEntry.StoredSite] =
    entries.collect { case site: AtlasEntry.StoredSite => site }

  /** The site a player takes from `end`: the first stored site for Recent,
    * the last for Forgotten. None once the Atlas holds no site. */
  def site(end: AtlasEnd): Option[AtlasEntry.StoredSite] = end match
    case AtlasEnd.Recent => sites.headOption
    case AtlasEnd.Forgotten => sites.lastOption

  /** The Atlas with the stored site `id` taken out. */
  def without(id: SiteId): AtlasState = AtlasState(entries.filter:
    case site: AtlasEntry.StoredSite => site.id != id
    case AtlasEntry.EmpireDivider => true)

enum PeoplesFavorFace { case Mob, GrandCouncil }

enum DarkestSecretFace { case WanderingFlame, Festival }

final case class PeoplesFavorState(
    active: PeoplesFavorFace,
    holder: Option[PlayerId],
    favor: Int
):
  require(favor >= 0, "People's Favor resources must be non-negative")

final case class DarkestSecretState(
    active: DarkestSecretFace,
    holder: Option[PlayerId],
    secrets: Int
):
  require(secrets >= 0, "Darkest Secret resources must be non-negative")

final case class BannersState(
    peoplesFavor: PeoplesFavorState,
    darkestSecret: DarkestSecretState
)

enum Banner(val key: String):
  case PeoplesFavor extends Banner("peoples-favor")
  case DarkestSecret extends Banner("darkest-secret")
object Banner:
  val all: Vector[Banner] = Vector(PeoplesFavor, DarkestSecret)
  def fromKey(key: String): Option[Banner] = all.find(_.key == key)

enum FoundationNumber(val value: Int):
  case I extends FoundationNumber(1)
  case II extends FoundationNumber(2)
  case III extends FoundationNumber(3)
  case IV extends FoundationNumber(4)
  case V extends FoundationNumber(5)
  case VI extends FoundationNumber(6)
object FoundationNumber:
  val all: Vector[FoundationNumber] = Vector(I, II, III, IV, V, VI)

enum FoundationFace { case Normal, Altered }

final case class FoundationState(
    face: FoundationFace,
    alterationSources: Set[LegacyId]
)

enum OathkeeperGoal(val key: String):
  case Supremacy extends OathkeeperGoal("supremacy")
  case Protection extends OathkeeperGoal("protection")
  case ThePeople extends OathkeeperGoal("the-people")
  case Devotion extends OathkeeperGoal("devotion")
object OathkeeperGoal:
  val all: Vector[OathkeeperGoal] =
    Vector(Supremacy, Protection, ThePeople, Devotion)
  def fromKey(key: String): Option[OathkeeperGoal] = all.find(_.key == key)

enum TitleSide { case Oathkeeper, Usurper }

final case class OathkeeperState(
    holder: Option[PlayerId],
    side: TitleSide
)
