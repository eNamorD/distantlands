package distantlands.gameplay.setup

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._
import distantlands.model.OathViolation.InvalidEventOrder

/** Foundation III's Quiet Ambitions keep in Setup's walk: each player who
  * drew two legacies keeps one facedown and returns the other (Legacies
  * design, slice 2). Split from `SetupProcedure`, which calls `step` in
  * each player's turn. */
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

  /** Quiet Ambitions: keep one of the two legacies game start drew,
    * facedown, and return the other to the deck (CR "Set Up Players";
    * Legacies design, slice 2). The drawn legacies are the dormant ones the
    * lineage holds that its baseline record does not, read when the
    * question is reached (implementation ruling R13). A short deck's single
    * legacy is kept without a question, and a player who drew none is asked
    * nothing (R9). */
  def step(participant: FirstGameParticipant): Operation =
    val decisionId = LegacyKeep.decisionId(participant.playerId)
    Branch((state, _) =>
      drawn(state, participant.lineageId) match
        case Vector() => Vector.empty
        case Vector(_) => Vector(keptLegacyNote(participant.playerId))
        case drawn => Vector(
          Decide(decisionId, participant.playerId, DecisionQuery.ChooseOne(
            drawn.map(id =>
              DecisionOption.Legacy(DecisionOptionRef.Legacy(id))),
            heading = Some("Keep a legacy"), confirmVerb = Some("Keep"))),
          BuildOps(returnLegacies(participant.lineageId, drawn, decisionId)),
          keptLegacyNote(participant.playerId)))

  private def keptLegacyNote(player: PlayerId): Operation =
    Note(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      _ => Some(keptLegacy.sourceless(NoteArg.Player(player))))

  /** The legacies game start drew for `lineage` that are still on it:
    * the dormant ones its baseline record does not hold. */
  def drawn(ready: ReadyGame, lineage: LineageId)
      : Vector[LegacyId] =
    val stored = ready.game.campaign.baseline.lineages.find(_.id == lineage)
      .toVector.flatMap(_.legacies).map(_.id).toSet
    ready.game.campaign.lineages.get(lineage).toVector
      .flatMap(_.legacies).collect {
        case LegacyState(id, false) if !stored(id) => id }

  private def returnLegacies(lineage: LineageId, drawn: Vector[LegacyId],
      decisionId: String)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (_, pending) =>
      pending.answered.find(_.decisionId == decisionId).map(_.answer) match
        case Some(DecisionAnswer.ChooseOneAnswer(
            DecisionOptionRef.Legacy(kept))) if drawn.contains(kept) =>
          Right(drawn.filterNot(_ == kept).map(id => Move(Piece.Card(id),
            PositionedLocation(Location.Legacies(lineage)),
            PositionedLocation(Location.Deck(CardDeck.Legacy)))))
        case _ => Left(InvalidEventOrder(
          s"no legacy answer is recorded for $decisionId"))

