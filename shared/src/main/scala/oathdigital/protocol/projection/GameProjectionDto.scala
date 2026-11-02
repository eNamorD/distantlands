package oathdigital.protocol.projection

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
)

/** Every seat's view of the game's Chronicle (Chronicle browsing design,
  * "Projection"): its name and the starting Chronicle's digest from the
  * start, and the evolved Chronicle once the Chronicle Phase journals it. */
final case class ChronicleProjection(name: String, startingDigest: String,
    evolved: Option[oathdigital.protocol.ChronicleEntry])

/** A step the viewer may take back or restore, with the hover note that
  * says what it is (undo design, "Controls"). Present only for the step's
  * issuer. */
final case class StepOfferProjection(note: String)
