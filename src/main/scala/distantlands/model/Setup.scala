package distantlands.model

final case class FirstGameParticipant(
    playerId: PlayerId,
    lineageId: LineageId,
    color: PlayerColor
)

/** The concrete per-game deal derived from a Chronicle: who is seated and in
  * what order, and the dealt orders of the two decks a Chronicle's own
  * order is not meaningful for (2026-09-21 Chronicle design, slice 2,
  * "Setup from a Chronicle"). Recorded on `GameStarted` alongside the
  * Chronicle itself, so replay never re-derives either from live state.
  *
  * Setup deals from these orders, never from the Chronicle's: a Chronicle
  * is shown to players, so its deck order must not be the deal.
  * `denizenOrder` is the world deck shuffled, which deals hands and the
  * regional discards; `worldDeckOrder` is the rest of it with the Visions
  * spliced in; `relicOrder` is the relic deck shuffled.
  */
final case class SetupOrders(
    participants: Vector[FirstGameParticipant],
    firstPlayer: PlayerId,
    denizenOrder: Vector[DenizenId],
    worldDeckOrder: Vector[WorldCardId],
    relicOrder: Vector[RelicId]
)

/** `ignoreCitizenshipPowers` is the first-game option that ignores
  * "become a Citizen" powers (CR p. 34, ambiguity F8). The rulebook
  * recommends it for the very first game only, so game start turns it on
  * for a first game and off for every later game; the Settings Feature
  * phase will let players change it. */
final case class FirstGameSupportState(
    firstPlayer: PlayerId,
    ignoreCitizenshipPowers: Boolean
)

/** The validated shape `FirstGameSetupMaterializer` builds a table from.
  * Built fresh from a Chronicle by `GameStartRules.evolve` (slice 2); no
  * longer a command payload, so it carries no `catalog` field -- Chronicle
  * validation now checks ids directly against the live catalog.
  *
  * `denizenOrder` is the starting denizen stack Foundation IV deals from
  * (`SetupDeal`; CR pp. 6-7 and 11); it never contains a Vision.
  * `worldDeckOrder` is what that deal leaves of it, with the five fixed
  * Vision identities spliced into the 10+2/15+3 packets or mixed through a
  * short deck, and becomes the draw deck once dealing is done.
  */
final case class FirstGameSetupPlan(
    participants: Vector[FirstGameParticipant],
    firstPlayer: PlayerId,
    /** The sites game start places, in slot order going Hinterward: eight
      * on a full map, fewer when slots stay empty. */
    orderedSites: Vector[SiteId],
    denizenOrder: Vector[DenizenId],
    worldDeckOrder: Vector[WorldCardId],
    relicOrder: Vector[RelicId],
    homelandEdifices: Vector[(SiteId, EdificeId)],
    oathkeeperGoal: OathkeeperGoal,
    /** The Empire a later game starts with; a first game has none, nor has
      * a game set up with no Empire. */
    empire: Option[EmpireSetup] = None,
    /** The sites game start places as bandit sites with their stored
      * cards, their edifices ruined: with no Empire, the former Empire's
      * sites. Empty for a first game, whose sites are all in
      * `orderedSites`, and with an Empire, whose slots Imperial Maps fills
      * in Setup's walk and Wide Horizons leaves empty. */
    atlasSites: Vector[StoredSite] = Vector.empty,
    /** Each seated player's stored starting advisers (CR p. 11). */
    startingAdvisers: Map[PlayerId, Vector[AdviserState]] = Map.empty,
    /** The foundation faces the game starts with. A number the map does
      * not list lies normal. */
    foundations: Map[FoundationNumber, FoundationState] = Map.empty
):
  /** Whether foundation `number` starts on its altered face. */
  def altered(number: FoundationNumber): Boolean =
    foundations.get(number).exists(_.face == FoundationFace.Altered)

/** The Empire a later game starts with (CR pp. 8-9): its Chancellor, every
  * seated Imperial player, and the Empire's stored sites Cradlemost first.
  * `absentChancellor` is the Chancellor's lineage when it sits out; it
  * becomes a Citizen in the box (Empire ruling R6). */
final case class EmpireSetup(chancellor: PlayerId,
    imperial: Set[PlayerId], sites: Vector[StoredSite],
    absentChancellor: Option[LineageId])

/** Pure CR pp. 6-7 table setup, shared by `GameStartRules`. */
final case class FirstGameSetupMaterial(
    players: Vector[PlayerState],
    map: MapState,
    commonCards: CardZones,
    banners: BannersState,
    tracks: GameTracks,
    favorBanks: Map[Suit, Int],
    temporaryHands: Map[PlayerId, Vector[WorldCardId]]
)
