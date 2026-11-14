package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.legacies.OathsOfLoyalty
import distantlands.gameplay.setup.legacies.{LegacySetupPowers,
  SecretSociety}
import distantlands.model._
import distantlands.model.OathViolation._

/**
 * Declared Setup procedure tree for the walker (2026-09-21 Chronicle
 * design, slice 2, "Setup on the walker"). Triggered once, right after
 * `GameStarted` evolves a fresh `Ready` game in `Phase.Setup`; no client
 * command starts it (`TriggeredProcedureRef.Setup`).
 *
 * {{{
 * Sequence(                                              // no window
 *   Note(the legacies' game-start lines)                 // StartLines, always 18
 *   Note(Public Ambitions dealt ...)                     // when the row holds any
 *   Branch(                                              // a later game's
 *     Decide(reliquary-pick, chancellor),                // Chancellor, when the
 *     BuildOps(keep the relic facedown),                 // Reliquary holds one
 *     Note(took a relic))
 *   ImperialMaps.step(chancellor)                        // with a Chancellor and
 *                                                        // Foundation I normal
 *   Sequence(Vector.empty)                               // window = SetupBanks
 *   Sequence(Vector.empty)                               // window = SetupWorldDeck
 *   Sequence(                                            // per participant
 *     Branch(Decide(pawn-placement, participant)),       // legal sites only
 *     BuildOps(place the pawn)                           // window = SetupPawnPlaced
 *     Branch(Decide(secret-society discard, participant)) // over the limit
 *     Branch(                                            // Quiet Ambitions, with
 *       Decide(legacy-choice, participant),              // two drawn legacies
 *       BuildOps(return the other to the deck),
 *       Note(kept a legacy))
 *     Branch(                                            // only with a hand
 *       Decide(adviser-choice, participant),
 *       BuildOps(keep one adviser, discard the other two)),
 *     Branch(                                            // CR p. 13, Reveal Cards
 *       Decide(reveal, participant),                     // only when one can be
 *       BuildOps(reveal the chosen cards)))
 *   ...
 *   Sequence(Vector.empty)                                // window = SetupEnd
 *   BeginTurn(firstPlayer, Wake))
 * }}}
 *
 * Turn order and every hand are already fixed the moment `GameStarted`
 * evolves -- read off `ready.setup.firstPlayer`/`ready.game.current
 * .players`/`.temporaryHands`, never off a start selection -- so `build`
 * doubles as `rebuild`, like Oathkeeper and End Wake: nothing here is a
 * start-only gate, and a resume rebuilds the identical tree against live
 * state.
 *
 * The `SetupEnd` window is declared with no static children (unlike End
 * Wake's undeclared window, on purpose): slice 3's E02/E06/E22 SETUP
 * powers hook it with a `Transform` that adds their own operations: nothing
 * runs there yet.
 *
 * Reveal Cards lets a player turn any of their facedown advisers and relics
 * faceup, except site-only advisers. A `Reveal` does not trigger When Played
 * powers. The step asks nothing when the player has nothing to reveal.
 */
object SetupProcedure:
  val adviserKeepKey: String = SetupAdviserSteps.keepKey
  val adviserDiscardKey: String = SetupAdviserSteps.discardKey
  /** Choose Adviser and Reveal Cards, with Setup's lines. */
  private val adviserSteps = SetupAdviserSteps(TriggeredProcedureRef.Setup)

  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  /** "p2 placed their pawn at Ancient City". Setup has no turn yet, so every line
    * names its player. */
  val placedPawn: NoteKey = notes.key("placed-pawn", Vector(
    NotePart.Arg(0), NotePart.Text(" placed their pawn at "), NotePart.Arg(1)))
  val keptAdviser: NoteKey = adviserSteps.keptAdviser
  val revealedCard: NoteKey = adviserSteps.revealedCard
  /** "p1 took a relic from the Reliquary" (Empire ruling R29). */
  val tookRelic: NoteKey = notes.key("took-relic", Vector(
    NotePart.Arg(0), NotePart.Text(" took a relic from the Reliquary")))
  /** "p2 kept a legacy" (`LegacyKeep`). */
  val keptLegacy: NoteKey = LegacyKeep.keptLegacy
  /** "Public Ambitions dealt Rival, Seer, Arbiter and Wealthy". */
  val dealtPublicLegacies: NoteKey = notes.key("dealt-public-legacies",
    Vector(NotePart.Text("Public Ambitions dealt "), NotePart.Arg(0)))
  def noteKeys: Vector[NoteKey] = notes.declared ++ adviserSteps.noteKeys ++
    ImperialMaps.noteKeys ++
    OathsOfLoyalty.noteKeys(TriggeredProcedureRef.Setup) ++
    LegacyKeep.noteKeys ++ LegacySetupPowers.noteKeys

  val reliquaryDecisionId: String = "setup.reliquary-pick"
  def pawnDecisionId(player: PlayerId): String =
    s"${SetupPawnPlacement.prefix}${player.value}"
  def adviserDecisionId(player: PlayerId): String =
    s"setup.adviser-choice.${player.value}"
  def revealDecisionId(player: PlayerId): String =
    s"setup.reveal.${player.value}"
  def legacyDecisionId(player: PlayerId): String =
    LegacyKeep.decisionId(player)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty, (),
      InvalidEventOrder("Setup selects nothing")).map(_ => tree(catalog, ready))

  /** The cards `player` may reveal in Reveal Cards (`SetupAdviserSteps`). */
  def revealable(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId)
      : Vector[DecisionOption] =
    SetupAdviserSteps.revealable(catalog, ready, player)

  private def tree(catalog: ExecutableCatalog, ready: ReadyGame): Operation =
    val participants = turnOrder(ready)
    val steps = participants.map(playerStep(catalog, ready, _))
    val setupEnd = Sequence(Vector.empty, Some(PowerWindow.SetupEnd))
    val chancellor = Imperials.chancellor(ready)
    val imperialMaps = chancellor.filter(_ => ImperialMaps.applies(ready))
      .map(ImperialMaps.step(catalog, ready, _))
    // The banks are set up after Imperial Maps, or where it would run:
    // Populist hooks here (Legacy Setup powers ruling 12).
    val banks = Sequence(Vector.empty, Some(PowerWindow.SetupBanks))
    // The world deck game start built is changed here: Secret Society and
    // then Seer hook it (Legacy Setup powers rulings 14 and 15).
    val worldDeck = Sequence(Vector.empty, Some(PowerWindow.SetupWorldDeck))
    Sequence(LegacySetupPowers.startNotes ++ publicRow(ready) ++
      chancellor.map(reliquaryPick).toVector ++
      imperialMaps ++ Vector(banks, worldDeck) ++ steps :+ setupEnd :+
      BeginTurn(participants.head.playerId, Phase.Wake))

  /** The Public Ambitions row game start dealt, named once and first
    * (implementation ruling R14). Nothing when the row is empty. */
  private def publicRow(ready: ReadyGame): Vector[Operation] =
    val row = ready.game.current.commonCards.publicLegacies
    Option.when(row.nonEmpty)(Note(ProcedureNotes.id(
      TriggeredProcedureRef.Setup), _ => Some(dealtPublicLegacies.sourceless(
        NoteArg.Cards(row))))).toVector

  /** A later game's Chancellor first keeps one Reliquary relic facedown
    * (CR p. 8, Empire ruling R29). An empty Reliquary asks nothing. The
    * pick is never asked while the Reliquary is set aside: Setup With No
    * Empire sets it aside only when there is no Chancellor, and only a
    * Chancellor is asked. It reads `reliquaryInPlay` all the same, so that
    * the Reliquary in play is read one way everywhere. */
  private def reliquaryPick(chancellor: PlayerId): Operation =
    Branch((state, _) =>
      val relics = state.game.campaign.reliquaryInPlay
      if relics.isEmpty then Vector.empty
      else Vector(
        Decide(reliquaryDecisionId, chancellor, DecisionQuery.ChooseOne(
          relics.map(id => DecisionOption.Relic(DecisionOptionRef.Relic(id))),
          heading = Some("Take a relic from the Reliquary"),
          confirmVerb = Some("Take"))),
        BuildOps(takeRelic(chancellor)),
        Note(ProcedureNotes.id(TriggeredProcedureRef.Setup), _ =>
          Some(tookRelic.sourceless(NoteArg.Player(chancellor))))))

  private def takeRelic(chancellor: PlayerId)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (_, pending) =>
      pending.answered.find(_.decisionId == reliquaryDecisionId)
        .map(_.answer) match
        case Some(DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Relic(id))) =>
          Right(Vector(Move(Piece.Card(id),
            PositionedLocation(Location.Reliquary),
            PositionedLocation(Location.PlayArea(chancellor)),
            resultingOrientation = Some(Orientation.FaceDown))))
        case _ => Left(InvalidEventOrder(
          s"no relic answer is recorded for $reliquaryDecisionId"))

  private def turnOrder(ready: ReadyGame): Vector[FirstGameParticipant] =
    val order = ready.game.current.players.map(_.player)
    val start = order.indexOf(ready.setup.firstPlayer)
    (order.drop(start) ++ order.take(start)).map { playerId =>
      val player = ready.game.current.player(playerId).get
      FirstGameParticipant(playerId, player.lineage,
        ready.playerColors(playerId))
    }

  private def playerStep(catalog: ExecutableCatalog, ready: ReadyGame,
      participant: FirstGameParticipant): Operation =
    val pawnId = pawnDecisionId(participant.playerId)
    val adviserId = adviserDecisionId(participant.playerId)
    val hand = ready.game.current.temporaryHands
      .getOrElse(participant.playerId, Vector.empty)
      .collect { case id: DenizenId => id }
    Sequence(Vector(
      // The sites are read when the question is reached: Imperial Maps
      // fills the map earlier in the same walk.
      Branch((state, _) => Vector(Decide(pawnId, participant.playerId,
        DecisionQuery.ChooseOne(SetupPawnSites.legal(catalog, state,
            participant.playerId).map(site =>
            DecisionOption.Site(DecisionOptionRef.Site(site))),
          heading = Some("Choose your starting site"),
          confirmVerb = Some("Start at"))))),
      // The window hangs on the pair, not on the placement alone: a power
      // hooked at `SetupPawnPlaced` appends its leaves to the node the
      // window names, so a note outside that node would read after them
      // and the pawn's line would follow the edifice's, where the rule it
      // replaced came first. The arrival is read off the live state rather
      // than off the step before the note, because those appended leaves
      // would reset `NoteStates.previous`; this player's pawn site is
      // settled either way, since the next participant has not placed yet.
      Sequence(Vector(
        BuildOps(placePawn(participant.playerId, pawnId)),
        Note(ProcedureNotes.id(TriggeredProcedureRef.Setup),
          placedPawnNote(participant.playerId))),
        Some(PowerWindow.SetupPawnPlaced)),
      // Secret Society's holder discards excess advisers right after their
      // pawn (Legacy Setup powers ruling 14; R21).
      SecretSociety.excessStep(catalog, participant.playerId),
      LegacyKeep.step(participant),
      adviserSteps.chooseAdviser(participant.playerId, adviserId, hand),
      adviserSteps.reveal(catalog, participant.playerId,
        revealDecisionId(participant.playerId))))

  /** Where `player`'s pawn stands once it has been placed. */
  private def placedPawnNote(player: PlayerId)(states: NoteStates)
      : Option[LogNote] = states.now.game.current.pawnSite(player).map(site =>
    placedPawn.sourceless(NoteArg.Player(player), NoteArg.Site(site)))


  private def placePawn(player: PlayerId, decisionId: String)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (_, pending) => siteAnswer(pending, decisionId).map(site => Vector(
      Move(Piece.Pawn(player), PositionedLocation(Location.PlayArea(player)),
        PositionedLocation(Location.Site(site)))))

  private def siteAnswer(pending: PendingTree, decisionId: String)
      : Either[OathViolation, SiteId] =
    pending.answered.find(_.decisionId == decisionId).map(_.answer) match
      case Some(DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Site(site))) =>
        Right(site)
      case _ => Left(InvalidEventOrder(
        s"no pawn-placement answer is recorded for $decisionId"))
