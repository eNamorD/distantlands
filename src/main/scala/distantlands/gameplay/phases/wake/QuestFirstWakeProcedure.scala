package distantlands.gameplay.phases.wake

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.legacies.OathsOfLoyalty
import distantlands.gameplay.setup.SetupAdviserSteps
import distantlands.gameplay.setup.legacies.SecretSociety
import distantlands.model._

/** The first Wake of a player who placed no pawn in Setup, as Quest to
  * Distant Lands lets its holder do (Legacy Setup powers design, ruling 20;
  * R32). The driver starts it at the start of that player's Wake, before
  * the Wake checks and Forced Wake, keyed on the missing pawn.
  *
  * {{{
  * Sequence(
  *   Decide(wake.legacy.quest.<player>, any site in play),  // no Decline
  *   BuildOps(place the pawn), Note(placed their pawn),     // no window
  *   SetupAdviserSteps.chooseAdviser(.adviser),             // with a hand
  *   SecretSociety.excessStep(.discard),                    // over the limit
  *   SetupAdviserSteps.reveal(.reveal))                     // when one can
  * }}}
  *
  * The steps Setup deferred run as Setup runs them, Oaths of Loyalty's
  * burial included. No `SetupPawnPlaced` power fires: the Wake is not
  * Setup. `build` is also `rebuild`: the hand is read from the state the
  * walk resumes in, and nothing parks between a question and its effect.
  */
object QuestFirstWakeProcedure:
  def placeDecisionId(player: PlayerId): String =
    s"wake.legacy.quest.${player.value}"
  def adviserDecisionId(player: PlayerId): String =
    s"${placeDecisionId(player)}.adviser"
  def discardDecisionId(player: PlayerId): String =
    s"${placeDecisionId(player)}.discard"
  def revealDecisionId(player: PlayerId): String =
    s"${placeDecisionId(player)}.reveal"

  private val procedure = TriggeredProcedureRef.QuestFirstWake
  private val noteId = ProcedureNotes.id(procedure)
  private val notes = NoteSet(noteId)
  /** "p2 placed their pawn at Deep Woods". */
  val placedPawn: NoteKey = notes.key("placed-pawn", Vector(
    NotePart.Arg(0), NotePart.Text(" placed their pawn at "), NotePart.Arg(1)))
  private val adviserSteps = SetupAdviserSteps(procedure)
  val keptAdviser: NoteKey = adviserSteps.keptAdviser
  val revealedCard: NoteKey = adviserSteps.revealedCard
  def noteKeys: Vector[NoteKey] = notes.declared ++ adviserSteps.noteKeys ++
    OathsOfLoyalty.noteKeys(procedure) ++ SecretSociety.wakeNoteKeys

  /** Whether `player`'s Wake must open with this procedure: they are seated
    * and stand at no site. */
  def due(ready: ReadyGame, player: PlayerId): Boolean =
    ready.game.current.player(player).nonEmpty &&
      ready.game.current.pawnSite(player).isEmpty

  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- Either.cond(args.isEmpty, (), OathViolation.InvalidEventOrder(
        "the first Wake's placement selects nothing"))
    yield
      val hand = state.game.current.temporaryHands
        .getOrElse(actor, Vector.empty).collect { case id: DenizenId => id }
      val decisionId = placeDecisionId(actor)
      Sequence(Vector(
        Decide(decisionId, actor, DecisionQuery.ChooseOne(
          state.game.current.map.inPlay.map(site =>
            DecisionOption.Site(DecisionOptionRef.Site(site))),
          heading = Some("Place your pawn at any site"),
          confirmVerb = Some("Place your pawn at"))),
        BuildOps((_, pending) => site(pending, decisionId).map(at => Vector(
          Move(Piece.Pawn(actor), PositionedLocation(Location.PlayArea(actor)),
            PositionedLocation(Location.Site(at))))), required = true),
        Note(noteId, states => states.now.game.current.pawnSite(actor).map(at =>
          placedPawn.sourceless(NoteArg.Player(actor), NoteArg.Site(at)))),
        adviserSteps.chooseAdviser(actor, adviserDecisionId(actor), hand),
        SecretSociety.excessStep(catalog, actor, discardDecisionId(actor),
          procedure),
        adviserSteps.reveal(catalog, actor, revealDecisionId(actor))))

  private def site(pending: PendingTree, decisionId: String)
      : Either[OathViolation, SiteId] =
    pending.answered.find(_.decisionId == decisionId).map(_.answer) match
      case Some(DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Site(at))) =>
        Right(at)
      case _ => Left(OathViolation.InvalidEventOrder(
        s"no placement is recorded for $decisionId"))
