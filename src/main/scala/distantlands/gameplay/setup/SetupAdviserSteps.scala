package distantlands.gameplay.setup

import distantlands.catalog.{ExecutableCatalog, SiteOnly}
import distantlands.gameplay.actions.{CardPlay, ProcedureNotes}
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.chronicle.legacies.OathsOfLoyalty
import distantlands.model._
import distantlands.model.OathViolation._

/** Choose Adviser and Reveal Cards (CR p. 13), as Setup asks them of each
  * player and as the first Wake asks them of a player who placed no pawn
  * in Setup (Legacy Setup powers design, rulings 18 and 20). Each procedure
  * writes the lines under its own note id, so one instance serves each. */
final class SetupAdviserSteps(procedure: TriggeredProcedureRef):
  import SetupAdviserSteps._

  private val noteId = ProcedureNotes.id(procedure)
  private val notes = NoteSet(noteId)
  val keptAdviser: NoteKey = notes.key("kept-adviser", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1)))
  val revealedCard: NoteKey = notes.key("revealed-card", Vector(
    NotePart.Arg(0), NotePart.Text(" revealed "), NotePart.Arg(1)))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Keep one card of `hand` as a facedown adviser, the rest to the
    * discard pile of the region after the pawn's. Nothing is asked with no
    * hand: a player with starting advisers drew none (CR p. 11), and under
    * Teeming World nobody did. The hand is read by the caller when its
    * tree is built, so a walk resumed past the answer keeps the step. */
  def chooseAdviser(player: PlayerId, decisionId: String,
      hand: Vector[DenizenId]): Operation =
    Branch((_, _) =>
      if hand.isEmpty then Vector.empty
      else Vector(
        Decide(decisionId, player,
          DecisionQuery.Partition(
            Vector(DecisionSection(keepKey, "Keep", 1, Some(1)),
              DecisionSection(discardKey, "Discard", 0)),
            hand.map(id => DecisionOption.Denizen(DecisionOptionRef.Denizen(id))),
            heading = Some("Choose your starting adviser"),
            confirmLabel = Some("Confirm Adviser"))),
        BuildOps(chosen(player, decisionId)),
        Note(noteId, keptAdviserNote(player, decisionId))))

  /** The adviser the answer kept. `adviserSelection` takes a `PendingTree`
    * and a note has only `states.answered`, so this reads the placements
    * directly rather than rebuilding a tree to borrow it. */
  private def keptAdviserNote(player: PlayerId, decisionId: String)(
      states: NoteStates): Option[LogNote] =
    states.answered.collectFirst {
      case Answered(`decisionId`, DecisionAnswer.PartitionAnswer(placements), _) =>
        placements.collect {
          case DecisionPlacement(DecisionOptionRef.Denizen(id), `keepKey`) => id
        }
    }.flatMap(_.headOption).flatMap(kept =>
      if ChronicleLegacyRules.buriesStartingAdvisers(states.now, player) then
        OathsOfLoyalty.buriedLine(noteId, player, kept)
      else Some(keptAdviser.sourceless(NoteArg.Player(player),
        NoteArg.Card(kept))))

  /** Reveal Cards: any facedown advisers that are not site-only and
    * facedown relics turn faceup, with no When Played. Nothing is asked
    * when there is nothing to reveal. */
  def reveal(catalog: ExecutableCatalog, player: PlayerId,
      decisionId: String): Operation =
    Branch((state, _) =>
      val options = revealable(catalog, state, player)
      if options.isEmpty then Vector.empty
      else Vector(
        Decide(decisionId, player, DecisionQuery.ChooseMany(0, options.size,
          options, heading = Some("Reveal cards"),
          confirm = Some(CountedConfirm("Reveal", "card", "cards",
            Some("Reveal nothing"))))),
        BuildOps(revealing(catalog, player, decisionId)),
        // One line per card, as the rule posted one per recorded `Reveal`.
        // A second `Branch`, because the answer this reads does not exist
        // when the branch above is walked.
        Branch((live, pending) =>
          revealed(live, player, decisionId, pending.answered)
            .getOrElse(Vector.empty).map(card =>
            Note(noteId, _ => Some(revealedCard.sourceless(
              NoteArg.Player(player), NoteArg.Card(card))))))))

object SetupAdviserSteps:
  val keepKey: String = "keep"
  val discardKey: String = "discard"

  def apply(procedure: TriggeredProcedureRef): SetupAdviserSteps =
    new SetupAdviserSteps(procedure)

  /** The cards `player` may reveal in Reveal Cards: facedown advisers that
    * are not site-only, a facedown Vision for a player with a Vision space
    * (R38), then facedown relics. They are offered by slot, as Hunger
    * offers advisers, so the parked decision can be described to every
    * other viewer without naming a facedown card. */
  def revealable(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId)
      : Vector[DecisionOption] =
    val visions = CardPlay.hasVisionSpace(ready, player)
    ready.game.current.player(player).toVector.flatMap { state =>
      state.advisers.zipWithIndex.collect {
        case (DenizenState(id, Orientation.FaceDown, _), slot)
            if !catalog.denizen(id).exists(_.isInstanceOf[SiteOnly]) =>
          DecisionOption.AdviserSlot(DecisionOptionRef.AdviserSlot(player, slot))
        case (VisionState(_, Orientation.FaceDown), slot) if visions =>
          DecisionOption.AdviserSlot(DecisionOptionRef.AdviserSlot(player, slot))
      } ++ state.relics.zipWithIndex.collect {
        case (RelicState(_, Orientation.FaceDown, _), slot) =>
          DecisionOption.RelicSlot(DecisionOptionRef.RelicSlot(player, slot))
      }
    }

  /** The cards `player`'s reveal answer names, by slot, or nothing when the
    * first answer recorded under `decisionId` is not a choose-many. The one
    * definition the operations and the lines both read: `revealing` turns
    * the `None` into the rejection it has always made, and the note branch
    * writes no line for what was not revealed. */
  private def revealed(ready: ReadyGame, player: PlayerId, decisionId: String,
      answered: Vector[Answered]): Option[Vector[CardId]] =
    val held = ready.game.current.player(player)
    answered.find(_.decisionId == decisionId).map(_.answer).collect {
      case DecisionAnswer.ChooseManyAnswer(selected) =>
        selected.flatMap {
          case DecisionOptionRef.AdviserSlot(_, slot) =>
            held.flatMap(_.advisers.lift(slot)).map(_.id: CardId)
          case DecisionOptionRef.RelicSlot(_, slot) =>
            held.flatMap(_.relics.lift(slot)).map(_.id: CardId)
          case _ => None
        }
    }

  /** Each named card turned faceup: a Vision onto the Vision space as a
    * faceup Vision play puts it, discarding any Vision there, with no When
    * Played (R38); any other card in place. */
  private def revealing(catalog: ExecutableCatalog, player: PlayerId,
      decisionId: String)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (ready, pending) =>
      revealed(ready, player, decisionId, pending.answered)
        .toRight(InvalidEventOrder(
          s"no reveal answer is recorded for $decisionId"))
        .flatMap(_.foldLeft[Either[OathViolation, Vector[CoreOperation]]](
          Right(Vector.empty)) { (done, card) =>
          done.flatMap(operations => (card match
            case vision: VisionId => CardPlay.operations(catalog, ready,
              player, vision, SearchPlacement.Adviser(Orientation.FaceUp, None),
              CardPlay.Origin.FacedownAdviser)
            case other => Right(Vector(Reveal(other,
              Location.PlayArea(player))))).map(operations ++ _))
        })

  /** The kept card to the advisers facedown, or buried under Oaths of
    * Loyalty (Legacy Chronicle powers design, ruling 16; R24), and the rest
    * to the discard pile of the region after the pawn's. */
  private def chosen(player: PlayerId, decisionId: String)
      : (ReadyGame, PendingTree) => Either[OathViolation, Vector[CoreOperation]] =
    (ready, pending) => for
      selection <- adviserSelection(pending, decisionId)
      (kept, rejected) = selection
      pawnSite <- ready.game.current.pawnSite(player)
        .toRight(PawnSiteMissing(player))
      region <- ready.game.current.map.regionOf(pawnSite)
        .toRight(InvalidEventOrder(s"${pawnSite.value} is not in play"))
    yield (if ChronicleLegacyRules.buriesStartingAdvisers(ready, player) then
        Move(Piece.Card(kept), PositionedLocation(Location.Hand(player)),
          PositionedLocation(Location.Deck(CardDeck.World),
            StackPosition.Bottom))
      else Move(Piece.Card(kept), PositionedLocation(Location.Hand(player)),
        PositionedLocation(Location.PlayArea(player)),
        resultingOrientation = Some(Orientation.FaceDown))) +:
      rejected.map(id => Move(Piece.Card(id),
        PositionedLocation(Location.Hand(player)),
        PositionedLocation(Location.RegionalDiscard(CardPlay.nextRegion(region)),
          StackPosition.Top)))

  private def adviserSelection(pending: PendingTree, decisionId: String)
      : Either[OathViolation, (DenizenId, Vector[DenizenId])] =
    pending.answered.find(_.decisionId == decisionId).map(_.answer) match
      case Some(DecisionAnswer.PartitionAnswer(placements)) =>
        val kept = placements.filter(_.sectionKey == keepKey)
          .flatMap(value => denizen(value.option))
        val rejected = placements.filter(_.sectionKey == discardKey)
          .flatMap(value => denizen(value.option))
        kept match
          case Vector(id) => Right(id -> rejected)
          case _ => Left(InvalidEventOrder(
            s"adviser-choice must keep exactly one card for $decisionId"))
      case _ => Left(InvalidEventOrder(
        s"no adviser-choice answer is recorded for $decisionId"))

  private def denizen(ref: DecisionOptionRef): Option[DenizenId] = ref match
    case DecisionOptionRef.Denizen(id) => Some(id)
    case _ => None
