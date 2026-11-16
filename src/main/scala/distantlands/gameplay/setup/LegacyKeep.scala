package distantlands.gameplay.setup

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{LegacyDraws, PowerAnswers}
import distantlands.gameplay.setup.legacies.{Matchmaker, MatchmakerCard,
  SetupLegacyRules}
import distantlands.model._

/** Foundation III's Quiet Ambitions keep in Setup's walk: each player draws
  * two legacies from the legacy deck, keeps one facedown and returns the
  * other to the bottom (Legacies design, slice 2; catalog batch 13, ruling
  * 6). Split from `SetupProcedure`, which calls `step` for every seat
  * while Foundation III resolves in a later game, and in each player's
  * turn, after their pawn, in a first game (catalog batch 13, ruling 3).
  * Matchmaker's holder answers every other player's
  * keep (Legacy Setup powers ruling 13; R16). */
object LegacyKeep:
  def decisionId(player: PlayerId): String =
    s"setup.legacy-choice.${player.value}"

  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  /** "p2 kept a legacy". It names no card: the kept legacy is dormant, and
    * only its owner may know it (Legacies ruling 1). */
  val keptLegacy: NoteKey = notes.key("kept-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a legacy")))
  /** The lines the keep writes, on Setup's note id. */
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Quiet Ambitions: draw 2 legacies from the top of the legacy deck,
    * keep one facedown and return the other to the bottom (CR "Set Up
    * Players"; Legacies design, slice 2; catalog batch 13, rulings 2 and
    * 6). The draw goes to the keeper's hand: Matchmaker's holder draws and
    * keeps for every other player (Legacy Setup powers ruling 13), and a
    * hand is private to its holder, so only the keeper sees the draw. The
    * keep is its own `Branch`, after the draw's, so a walk resumed at the
    * question reads the same hand. A short deck's single legacy is kept
    * without a question, and a player who draws none is asked nothing
    * (R9). Public Ambitions draws nothing. */
  def step(participant: FirstGameParticipant): Operation =
    val owner = participant.playerId
    val lineage = participant.lineageId
    val decisionId = LegacyKeep.decisionId(owner)
    Sequence(Vector(
      Branch((state, _) =>
        if !quiet(state) then Vector.empty
        else LegacyDraws.top(state, LegacyDeal.QuietDraw) match
          case Vector() => Vector.empty
          case top => Vector(LegacyDraws.draw(
            SetupLegacyRules.keeper(state, owner), top))),
      Branch((state, _) =>
        val keeper = SetupLegacyRules.keeper(state, owner)
        LegacyDraws.hand(state, keeper) match
          case Vector() => Vector.empty
          case Vector(only) => Vector(
            BuildOps((_, _) => Right(Vector(keptMove(only, keeper, lineage))),
              required = true),
            keptNote(keeper, owner))
          case drawn => Vector(
            Decide(decisionId, keeper, DecisionQuery.ChooseOne(
              drawn.map(id =>
                DecisionOption.Legacy(DecisionOptionRef.Legacy(id))),
              heading = Some(
                if keeper == owner then "Keep a legacy"
                else s"Matchmaker: keep a legacy for ${owner.value}"),
              confirmVerb = Some("Keep"))),
            BuildOps(settled(lineage, keeper, drawn, decisionId),
              required = true),
            keptNote(keeper, owner)))))

  /** Whether Foundation III shows Quiet Ambitions, whose keep draws. */
  private def quiet(state: ReadyGame): Boolean =
    state.game.campaign.foundations.get(FoundationNumber.III)
      .forall(_.face == FoundationFace.Normal)

  private def keptNote(keeper: PlayerId, owner: PlayerId): Operation =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup), _ => Some(
      if keeper == owner then keptLegacy.sourceless(NoteArg.Player(owner))
      else Matchmaker.keptFor.sourceless(NoteArg.Player(keeper),
        NoteArg.Player(owner), NoteArg.Card(MatchmakerCard.id))))

  /** The kept legacy, from the keeper's hand to the owner's lineage, with
    * no face: dormant. */
  private def keptMove(legacy: LegacyId, keeper: PlayerId,
      lineage: LineageId): Move =
    Move(Piece.Card(legacy), PositionedLocation(Location.Hand(keeper)),
      PositionedLocation(Location.Legacies(lineage)))

  private def settled(lineage: LineageId, keeper: PlayerId,
      drawn: Vector[LegacyId], decisionId: String)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (_, pending) =>
      PowerAnswers.one(pending, decisionId).collect {
        case DecisionOptionRef.Legacy(kept) if drawn.contains(kept) =>
          keptMove(kept, keeper, lineage) +: drawn.filterNot(_ == kept)
            .map(id => Move(Piece.Card(id),
              PositionedLocation(Location.Hand(keeper)),
              PositionedLocation.legacyDeckBottom))
      }.toRight(PowerAnswers.missing(decisionId))
