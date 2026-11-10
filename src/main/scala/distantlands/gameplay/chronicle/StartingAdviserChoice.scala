package oathdigital.gameplay.chronicle

import oathdigital.model._

/** Keeping one card of a temporary hand as a facedown starting adviser,
  * the rest going to the world deck bottom: the World's Protect and
  * Dispossess and the Beacon's Welcome a Stranger (CR pp. 38-39). The
  * choice is the holder's alone, as Setup's adviser choice is. */
object StartingAdviserChoice:
  val keepKey: String = "keep"
  val deckKey: String = "world-deck"

  def steps(holder: PlayerId, lineage: LineageId, hand: Vector[DenizenId],
      decisionId: String, heading: String, kept: NoteKey, noteId: PowerId)
      : Vector[Operation] = Vector(
    Decide(decisionId, holder, DecisionQuery.Partition(
      Vector(DecisionSection(keepKey, "Keep", 1, Some(1)),
        DecisionSection(deckKey, "World deck", 0)),
      hand.map(card => DecisionOption.Denizen(DecisionOptionRef.Denizen(card))),
      heading = Some(heading), confirmLabel = Some("Confirm Adviser"))),
    BuildOps((_, pending) => placements(pending, decisionId).map(_.map {
      case (card, section) if section == keepKey => Move(Piece.Card(card),
        PositionedLocation(Location.Hand(holder)),
        PositionedLocation(Location.StartingAdvisers(lineage)),
        resultingOrientation = Some(Orientation.FaceDown))
      case (card, _) => Move(Piece.Card(card),
        PositionedLocation(Location.Hand(holder)),
        PositionedLocation(Location.Deck(CardDeck.World), StackPosition.Bottom))
    }), required = true),
    Note(noteId, states => states.answered.collectFirst {
      case Answered(`decisionId`, DecisionAnswer.PartitionAnswer(placed), _) =>
        placed.collectFirst {
          case DecisionPlacement(DecisionOptionRef.Denizen(card), `keepKey`) =>
            kept.sourceless(NoteArg.Player(holder), NoteArg.Card(card))
        }
    }.flatten))

  private def placements(pending: PendingTree, decisionId: String)
      : Either[OathViolation, Vector[(DenizenId, String)]] =
    pending.answered.find(_.decisionId == decisionId).map(_.answer) match
      case Some(DecisionAnswer.PartitionAnswer(placed)) => Right(placed.collect {
        case DecisionPlacement(DecisionOptionRef.Denizen(card), section) =>
          (card, section)
      })
      case _ => Left(OathViolation.InvalidEventOrder(
        s"no starting adviser answer is recorded for $decisionId"))
