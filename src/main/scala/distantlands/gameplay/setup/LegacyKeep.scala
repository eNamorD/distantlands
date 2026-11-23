package distantlands.gameplay.setup

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.Deferred
import distantlands.gameplay.powers.{LegacyDraws, PowerAnswers}
import distantlands.gameplay.setup.legacies.{Matchmaker, MatchmakerCard,
  SetupLegacyRules}
import distantlands.model._

/** Foundation III's Quiet Ambitions keep in Setup's walk: each player draws
  * two legacies from the legacy deck, keeps one facedown and returns the
  * other to the bottom (Legacies design, slice 2; catalog batch 13, ruling
  * 6). Split from `SetupProcedure`, which calls [[deal]] once and then
  * [[keep]] for every seat. A later game deals and keeps while Foundation
  * III resolves; a first game deals before the first pawn and keeps in
  * each player's turn, after their pawn (catalog batch 13, rulings 3 and
  * 8; setup keep undo design, rulings 1 and 7). Matchmaker's holder
  * answers every other player's keep (Legacy Setup powers ruling 13;
  * R16). */
object LegacyKeep:
  def decisionId(player: PlayerId): String =
    s"setup.legacy-choice.${player.value}"

  /** The key the deal records `owner`'s dealt pair under, which `owner`'s
    * keep reads and takes (setup keep undo design, "The deal"). */
  def dealKey(owner: PlayerId): DeferralKey =
    DeferralKey(s"setup.legacy-deal.${owner.value}")

  private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
  /** "p2 kept a legacy". It names no card: the kept legacy is dormant, and
    * only its owner may know it (Legacies ruling 1). */
  val keptLegacy: NoteKey = notes.key("kept-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a legacy")))
  /** The lines the keep writes, on Setup's note id. */
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Quiet Ambitions' deal: every seat in `participants`' order draws the
    * next 2 legacies from the top of the deck into its keeper's hand, before
    * any keep (setup keep undo design, rulings 1 to 3). The keeper is the
    * seat itself or Matchmaker's holder (Legacy Setup powers ruling 13),
    * and a hand is private to its holder, so only the keeper sees the
    * draw. A short deck gives the later seats fewer, and a seat it cannot
    * reach draws none. Each draw is recorded under [[dealKey]] first, then
    * run, so [[keep]] reads the seat's own pair however the deck and the
    * keeper's hand change before it. Public Ambitions deals nothing. */
  def deal(participants: Vector[FirstGameParticipant]): Operation =
    Sequence(participants.map(dealTo))

  /** One seat's part of [[deal]]: its draw, read off the deck when the step
    * is reached, after every earlier seat's. */
  private def dealTo(participant: FirstGameParticipant): Operation =
    val owner = participant.playerId
    Branch((state, _) =>
      if !quiet(state) then Vector.empty
      else LegacyDraws.top(state, LegacyDeal.QuietDraw) match
        case Vector() => Vector.empty
        case pair =>
          val keeper = SetupLegacyRules.keeper(state, owner)
          val draw = Draw(keeper, pair, Location.Deck(CardDeck.Legacy),
            Location.Hand(keeper))
          Vector(BuildOps((_, _) => Right(Vector[CoreOperation](
            Defer(dealKey(owner), Vector(draw)), draw)), required = true)))

  /** The keep of the pair [[deal]] recorded for this player: the keeper
    * sorts the legacies into Keep, one, and Discard, the rest. A seat dealt
    * one legacy keeps it without a question, and a seat dealt none is asked
    * nothing (R9). A walk resumed at the question finds the same pair still
    * recorded. */
  def keep(participant: FirstGameParticipant): Operation =
    val owner = participant.playerId
    val lineage = participant.lineageId
    val decisionId = LegacyKeep.decisionId(owner)
    Deferred.resolve(dealKey(owner))((state, _, held) =>
      val keeper = SetupLegacyRules.keeper(state, owner)
      dealt(held) match
        case Vector() => Vector.empty
        case Vector(only) => Vector(
          BuildOps((_, _) => Right(Vector(keptMove(only, keeper, lineage))),
            required = true),
          keptNote(keeper, owner))
        case drawn => Vector(
          LegacyDraws.pick(decisionId, keeper, drawn, LegacyDraws.Pick.keep,
            heading =
              if keeper == owner then "Keep a legacy"
              else s"Matchmaker: keep a legacy for ${owner.value}",
            confirmLabel = "Keep Legacy"),
          BuildOps(settled(lineage, keeper, drawn, decisionId),
            required = true),
          keptNote(keeper, owner)))

  /** The legacies the recorded draws took, in draw order. */
  private def dealt(held: Vector[CoreOperation]): Vector[LegacyId] =
    held.flatMap {
      case draw: Draw => draw.cards.collect { case id: LegacyId => id }
      case _ => Vector.empty
    }

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
      LegacyDraws.picked(pending.answered, decisionId, LegacyDraws.Pick.keep)
        .filter(drawn.contains).map { kept =>
          keptMove(kept, keeper, lineage) +: drawn.filterNot(_ == kept)
            .map(id => Move(Piece.Card(id),
              PositionedLocation(Location.Hand(keeper)),
              PositionedLocation.legacyDeckBottom))
        }.toRight(PowerAnswers.missing(decisionId))
