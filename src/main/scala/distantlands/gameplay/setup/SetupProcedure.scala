package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.chronicle.legacies.OathsOfLoyalty
import distantlands.gameplay.setup.legacies.{LegacySetupPowers,
  QuestToDistantLands, SecretSociety, SetupLegacyRules, StartLines}
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
 *   Note(Oaths' Teeming World burials)                   // always 3
 *   Note(Public Ambitions dealt ...)                     // when the row holds any
 *   Branch(                                              // a later game's
 *     Decide(reliquary-pick, chancellor),                // Chancellor, when the
 *     BuildOps(keep the relic facedown),                 // Reliquary holds one
 *     Note(took a relic))
 *   ImperialMaps.step(chancellor)                        // with a Chancellor and
 *                                                        // Foundation I normal
 *   Sequence(Vector.empty)                               // window = SetupBanks
 *   Sequence(                                            // a later game's
 *     Branch(Draw(2 into the keeper's hand)),            // LegacyKeep.step, per
 *     Branch(Decide(legacy-choice, keeper), ...))        // seat in turn order
 *   ...                                                  // (Foundation III)
 *   Sequence(Vector.empty)                               // window = SetupWorldDeck
 *   Branch(Draw(2 into the keeper's hand))               // a first game's
 *   ...                                                  // LegacyKeep.draw, per
 *                                                        // seat in turn order
 *   Sequence(                                            // per participant
 *     Branch(Decide(pawn-placement, participant)),       // legal sites, and
 *                                                        // Quest's stay-off
 *     Sequence(BuildOps(place the pawn), Note(placed))
 *     OrderedWindow(participant)                         // window =
 *                                                        // SetupPawnPlaced; the
 *                                                        // placer orders its powers
 *     Branch(Decide(secret-society discard, participant)) // over the limit,
 *                                                        // with a pawn
 *     Branch(                                            // a first game's
 *       Decide(legacy-choice, keeper),                   // LegacyKeep.keep; one
 *       BuildOps(keep one, the other to the bottom),     // in the hand is kept
 *       Note(kept a legacy))                             // unasked
 *     QuestToDistantLands.swapStep(participant)          // no pawn, a card to
 *                                                        // give (R31, R37)
 *     Branch(                                            // with a pawn; a player
 *       Branch(                                          // with none chooses and
 *         Decide(adviser-choice, participant),           // reveals at their
 *         BuildOps(keep one adviser, discard the rest)), // first Wake
 *       Branch(                                          // CR p. 13, Reveal Cards
 *         Decide(reveal, participant),                   // only when one can be
 *         BuildOps(reveal the chosen cards))))
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
 * faceup, except site-only advisers; an Exile's facedown Vision goes to
 * their Vision space (R38). It triggers no When Played power. The step asks
 * nothing when the player has nothing to reveal.
 */
object SetupProcedure:
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

  def build(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    Either.cond(args.isEmpty, (),
      InvalidEventOrder("Setup selects nothing")).map(_ => tree(catalog, ready))

  private def tree(catalog: ExecutableCatalog, ready: ReadyGame): Operation =
    val participants = turnOrder(ready)
    // A first game draws every seat's legacies before the first pawn and
    // keeps in each player's turn, after their pawn (CR p. 7);
    // a later game keeps while Foundation III resolves, every seat before
    // any pawn (CR "Later games", step 5; catalog batch 13, ruling 3). An
    // exile-only game resolves its stored foundations, so it keeps in
    // Foundation III, unless its wiped baseline carries no history: then
    // it keeps after each pawn, as it places its pawns, as a first game
    // does (R41). The baseline never changes in Setup, so a resume
    // rebuilds the same tree.
    val firstGame = EmpireSetupRules.isFirstGame(catalog,
      ready.game.campaign.baseline)
    val keeps =
      if firstGame then Vector.empty else participants.map(LegacyKeep.step)
    // A first game draws every seat's two before the first pawn, so each
    // player places holding them (CR p. 7), and keeps after their pawn.
    val firstGameDraws =
      if firstGame then participants.map(LegacyKeep.draw) else Vector.empty
    val steps = firstGameDraws ++
      participants.map(playerStep(catalog, ready, _, firstGame))
    val setupEnd = Sequence(Vector.empty, Some(PowerWindow.SetupEnd))
    val chancellor = Imperials.chancellor(ready)
    val imperialMaps = chancellor.filter(_ => ImperialMaps.applies(ready))
      .map(ImperialMaps.step(catalog, ready, _))
    // The banks are set up after Imperial Maps, or where it would run:
    // Populist hooks here (Legacy Setup powers ruling 12).
    val banks = Sequence(Vector.empty, Some(PowerWindow.SetupBanks))
    // The world deck game start built is changed here, after a later
    // game's keeps: Secret Society and then Seer hook it (Legacy Setup
    // powers rulings 14 and 15).
    val worldDeck = Sequence(Vector.empty, Some(PowerWindow.SetupWorldDeck))
    Sequence(StartLines.operations ++ teemingBurials ++
      publicRow(ready) ++
      chancellor.map(reliquaryPick).toVector ++
      imperialMaps ++ (banks +: keeps :+ worldDeck) ++ steps :+ setupEnd :+
      BeginTurn(participants.head.playerId, Phase.Wake))

  /** One line per card an Oaths of Loyalty holder drew and buried under
    * Teeming World at game start (Legacy Chronicle powers R24, re-ruled):
    * those are the world deck's only denizens when Setup begins, since
    * Teeming World leaves the deck nothing but Visions. Three Notes, the
    * most a holder draws, each writing nothing past the cards buried. The
    * log names a card only to viewers who know it: the holder. */
  private def teemingBurials: Vector[Operation] =
    (0 until 3).toVector.map(slot => Note(ProcedureNotes.id(
      TriggeredProcedureRef.Setup), states =>
      val state = states.now
      val teeming = state.game.campaign.foundations.get(FoundationNumber.IV)
        .exists(_.face == FoundationFace.Altered)
      for
        holder <- ChronicleLegacyRules.keepsAdvisers(state)
        if teeming
        card <- state.game.current.commonCards.worldDeck.collect {
          case id: DenizenId => id }.lift(slot)
        line <- OathsOfLoyalty.buriedLine(ProcedureNotes.id(
          TriggeredProcedureRef.Setup), holder, card)
      yield line))

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
      PowerAnswers.one(pending, reliquaryDecisionId).collect {
        case DecisionOptionRef.Relic(id) => Vector(Move(Piece.Card(id),
          PositionedLocation(Location.Reliquary),
          PositionedLocation(Location.PlayArea(chancellor)),
          resultingOrientation = Some(Orientation.FaceDown)))
      }.toRight(PowerAnswers.missing(reliquaryDecisionId))

  private def turnOrder(ready: ReadyGame): Vector[FirstGameParticipant] =
    val order = ready.game.current.players.map(_.player)
    val start = order.indexOf(ready.setup.firstPlayer)
    (order.drop(start) ++ order.take(start)).map { playerId =>
      val player = ready.game.current.player(playerId).get
      FirstGameParticipant(playerId, player.lineage,
        ready.playerColors(playerId))
    }

  private def playerStep(catalog: ExecutableCatalog, ready: ReadyGame,
      participant: FirstGameParticipant, firstGame: Boolean): Operation =
    val pawnId = pawnDecisionId(participant.playerId)
    val adviserId = adviserDecisionId(participant.playerId)
    val hand = ready.game.current.temporaryHands
      .getOrElse(participant.playerId, Vector.empty)
      .collect { case id: DenizenId => id }
    Sequence(Vector(
      // The sites are read when the question is reached: Imperial Maps
      // fills the map earlier in the same walk.
      // Quest to Distant Lands' holder may stay off the map (R30).
      Branch((state, _) => Vector(Decide(pawnId, participant.playerId,
        DecisionQuery.ChooseOne(SetupPawnSites.legal(catalog, state,
            participant.playerId).map(site =>
            DecisionOption.Site(DecisionOptionRef.Site(site))) ++
          Option.when(SetupLegacyRules.mayStayOff(state,
              participant.playerId))(DecisionOption.Button(
            QuestToDistantLands.stayOff, QuestToDistantLands.stayOffLabel)),
          heading = Some("Choose your starting site"),
          confirmVerb = Some("Start at"))))),
      // The pawn's line is written before the window, so it reads before
      // every power's. The arrival is read off the live state: this
      // player's pawn site is settled, since the next participant has not
      // placed yet.
      Sequence(Vector(
        BuildOps(placePawn(participant.playerId, pawnId)),
        Note(ProcedureNotes.id(TriggeredProcedureRef.Setup),
          placedPawnNote(participant.playerId)))),
      // The placer orders the powers that apply to them (catalog batch 13,
      // ruling 4).
      OrderedWindow(participant.playerId, Vector.empty),
      // Secret Society's holder discards excess advisers right after their
      // pawn (Legacy Setup powers ruling 14; R21).
      SecretSociety.excessStep(catalog, participant.playerId,
        SecretSociety.discardDecisionId(participant.playerId),
        TriggeredProcedureRef.Setup)) ++
      // A first game's keep, of the two drawn before the first pawn; a
      // later game's ran in Foundation III.
      Option.when(firstGame)(LegacyKeep.keep(participant)) ++ Vector(
      QuestToDistantLands.swapStep(participant.playerId),
      // A player with no pawn chooses and reveals at their first Wake,
      // keyed on the missing pawn (Legacy Setup powers rulings 18, 20).
      Branch((state, _) =>
        if state.game.current.pawnSite(participant.playerId).isEmpty then
          Vector.empty
        else Vector(
          adviserSteps.chooseAdviser(participant.playerId, adviserId, hand),
          adviserSteps.reveal(catalog, participant.playerId,
            revealDecisionId(participant.playerId))))))

  /** Where `player`'s pawn stands once it has been placed. */
  private def placedPawnNote(player: PlayerId)(states: NoteStates)
      : Option[LogNote] = states.now.game.current.pawnSite(player).map(site =>
    placedPawn.sourceless(NoteArg.Player(player), NoteArg.Site(site)))
    .orElse(Option.when(states.answered.exists(answer =>
      answer.decisionId == pawnDecisionId(player) &&
        answer.answer == DecisionAnswer.ChooseOneAnswer(
          QuestToDistantLands.stayOff)))(
      QuestToDistantLands.placedNoPawnLine(player)))


  private def placePawn(player: PlayerId, decisionId: String)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (_, pending) =>
      // Staying off places nothing (R30).
      if pending.answered.exists(answer => answer.decisionId == decisionId &&
          answer.answer == DecisionAnswer.ChooseOneAnswer(
            QuestToDistantLands.stayOff)) then Right(Vector.empty)
      else siteAnswer(pending, decisionId).map(site => Vector(
        Move(Piece.Pawn(player), PositionedLocation(Location.PlayArea(player)),
          PositionedLocation(Location.Site(site)))))

  private def siteAnswer(pending: PendingTree, decisionId: String)
      : Either[OathViolation, SiteId] =
    PowerAnswers.one(pending, decisionId).collect {
      case DecisionOptionRef.Site(site) => site
    }.toRight(PowerAnswers.missing(decisionId))
