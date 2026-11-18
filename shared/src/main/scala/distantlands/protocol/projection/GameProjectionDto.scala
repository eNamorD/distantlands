package distantlands.protocol.projection

final case class GameProjection(
    gameId: String,
    nextSequence: Long,
    phase: String,
    activeParticipantId: Option[String],
    players: Vector[SetupPlayerProjection],
    world: Vector[SetupRegionProjection],
    pawnLocations: Vector[PawnLocationProjection],
    ready: Boolean,
    completed: Boolean,
    activePlayerResources: Option[ActivePlayerResourcesProjection] = None,
    currentSiteResources: Option[CurrentSiteResourcesProjection] = None,
    worldDeckCount: Int = 0,
    worldDeckTopCardKind: Option[String] = None,
    playerBoards: Vector[PlayerBoardProjection] = Vector.empty,
    oathkeeper: Option[OathkeeperProjection] = None
    ,banners: Vector[BannerProjection] = Vector.empty
    ,favorBanks: Vector[FavorBankProjection] = Vector.empty
    ,tracks: Option[GameTracksProjection] = None
    ,relicDeckCount: Int = 0
    ,temporaryHandPreview: Vector[CardDetailsProjection] = Vector.empty
    ,walkerDecision: Option[WalkerDecisionProjection] = None
    ,walkerWaiting: Option[WalkerWaitingProjection] = None
    ,viewerPlayerId: Option[String] = None
    ,supplyMaximum: Int = 0
    ,supplyBands: Vector[SupplyBandProjection] = Vector.empty
    ,undo: Option[StepOfferProjection] = None
    ,redo: Option[StepOfferProjection] = None
    ,logRevision: Option[Long] = None
    ,consentWaiting: Option[ConsentWaitingProjection] = None
    ,consentPrompt: Option[ConsentPromptProjection] = None
    ,consentPreferences: Vector[ConsentPreferencesProjection] = Vector.empty
    ,chronicle: Option[ChronicleProjection] = None
    ,planAidPrompt: Option[PlanAidPromptProjection] = None
    /** The six foundation setup cards, I to VI; empty before a game. */
    ,foundations: Vector[FoundationProjection] = Vector.empty
    /** The Public Ambitions row, faceup; empty unless Foundation III is
      * altered and the Chronicle has not resolved it. */
    ,publicLegacies: Vector[CardDetailsProjection] = Vector.empty
    /** The Reliquary in play, facedown, one entry per relic: a face only
      * where its viewer may peek (Legacy in-play powers rulings 9 and 10);
      * empty while it holds none or is set aside. */
    ,reliquary: Vector[CardDetailsProjection] = Vector.empty
    /** Cards every viewer is shown beside the parked decision, spectators
      * too, under a title: public information the decision is about, such
      * as Build Edifice's available edifices. Unlike
      * `temporaryHandPreview` they are no one's hand. */
    ,publicPreview: Option[PublicPreviewProjection] = None
)

/** A titled run of cards shown to the whole table ([[GameProjection]]'s
  * `publicPreview`). */
final case class PublicPreviewProjection(title: String,
    cards: Vector[CardDetailsProjection])

/** Every seat's view of the game's Chronicle (Chronicle browsing design,
  * "Projection"): its name and the starting Chronicle's digest from the
  * start, and the evolved Chronicle once the Chronicle Phase journals it. */
final case class ChronicleProjection(name: String, startingDigest: String,
    evolved: Option[distantlands.protocol.ChronicleEntry])

/** A step the viewer may take back or restore, with the hover note that
  * says what it is (undo design, "Controls"). Present only for the step's
  * issuer. */
final case class StepOfferProjection(note: String)
