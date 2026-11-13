package distantlands.model

/** Operations the Chronicle Phase and the Atlas journal: roles,
  * tasks, the Grand Scepter, suited reserves, Imperial warbands,
  * lineage points, built edifices, restored foundations, placed
  * Atlas sites, and the evolved Chronicle.
  *
  * One operation family: a sealed area of Oath's operation
  * vocabulary, joined to [[CoreOperation]] by the union alias
  * in `CoreOperations.scala`. A leaf also extends
  * [[LeafOperation]]; a composite exposes its `children`.
  */
sealed trait ChronicleOperation extends OperationBase

/** Records the evolved Chronicle at the end of the Chronicle Phase
  * (Chronicle steps design, "Exit and extraction"). It is the phase's one
  * campaign record: the next game starts from it. Applying it only stores the
  * value; the Chronicle procedures decide what it holds.
  */
final case class ChronicleUpdated(chronicle: Chronicle) extends ChronicleOperation, LeafOperation

/** Sets a lineage's role (Crown Chancellor, CR p. 36). A seated lineage
  * changes in `CampaignState.lineages`; a lineage only the baseline lists
  * ("even in the box") changes in `CampaignState.boxRoles`. */
final case class RoleChanged(lineage: LineageId, role: Role)
    extends ChronicleOperation, LeafOperation

/** Gives a seated player a Chronicle task (Assign Tasks, CR p. 36). */
final case class ChronicleTaskAssigned(task: ChronicleTask, player: PlayerId)
    extends ChronicleOperation, LeafOperation

/** The winner takes the Grand Scepter from the box, faceup, when no one
  * holds it (Chronicle ruling 10). */
final case class GrandScepterFromBox(player: PlayerId)
    extends ChronicleOperation, LeafOperation

/** Brings a suit's boxed denizens into its live suited reserve, so the
  * Beacon can draw from them (CR p. 39). Live reserves start empty; the box
  * is every denizen the catalog has and the game does not. The walk builds
  * `cards` from the catalog; the write checks only that none is in play. */
final case class SuitedReserveFromBox(suit: Suit, cards: Vector[DenizenId])
    extends ChronicleOperation, LeafOperation:
  require(cards.nonEmpty, "a suited reserve takes at least one boxed card")

/** The Chancellor's purple warbands join the table: `count` Imperial
  * warbands, unless the game already has an Imperial supply. */
final case class ImperialWarbandsTaken(count: Int) extends ChronicleOperation, LeafOperation:
  require(count > 0, "the Imperial supply must be positive")

/** Adds warbands of `kind` to a supply that exists. A Chronicle step that
  * places one warband per site assumes an unlimited supply and adds what
  * the bank lacks (Chronicle rulings 20 and 22). */
final case class WarbandsAdded(kind: ForceKind, count: Int)
    extends ChronicleOperation, LeafOperation:
  require(count > 0, "added warbands must be positive")

/** Adds lineage points to a lineage's era score (Score Lineage Points,
  * CR p. 37). */
final case class LineagePointsScored(lineage: LineageId, points: Int)
    extends ChronicleOperation, LeafOperation:
  require(points > 0, "scored lineage points must be positive")

/** Places an edifice from the edifice deck or the set-aside list at a site,
  * intact (Build Edifice, CR p. 37, Chronicle ruling 12). A `Move` cannot do
  * this: a card with no state takes no edifice side. */
final case class EdificeBuilt(edifice: EdificeId, site: SiteId)
    extends ChronicleOperation, LeafOperation

/** Turns Foundation `number` from its altered face to its normal face and
  * clears its alteration sources (Shape Foundation, CR p. 39). */
final case class FoundationRestored(number: FoundationNumber)
    extends ChronicleOperation, LeafOperation

/** Turns Foundation `number` to its altered face, if it is not already, and
  * adds `source`, the active legacy that sets it, to its alteration
  * sources (Sun step 2, CR p. 36). The inverse of `FoundationRestored`. */
final case class FoundationAltered(number: FoundationNumber,
    source: LegacyId) extends ChronicleOperation, LeafOperation

/** Takes the site at `end` of the Atlas, with the cards stored on it, and
  * places it in the next slot, which is in `region`, as a bandit site
  * (Foundation I, "populate"): its edifice ruined, its denizens faceup, its
  * stored relics facedown, bandits up to `capacity`, its printed
  * `resources`, and the relic slots its stored relics leave open, of
  * `relicSlots`, dealt facedown from the top of the relic deck while it
  * lasts. Applying an operation reads no catalog, so the walk that builds
  * this copies the site's three printed facts onto it. */
final case class PlaceAtlasSite(end: AtlasEnd, region: Region, capacity: Int,
    relicSlots: Int, resources: Tokens) extends ChronicleOperation, LeafOperation:
  require(capacity >= 0 && relicSlots >= 0,
    "a placed site's capacity and relic slots must be non-negative")
