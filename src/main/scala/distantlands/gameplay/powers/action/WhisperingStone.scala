package distantlands.gameplay.powers.action

import distantlands.catalog.{ExecutableCatalog, PrintedPower, Relic}
import distantlands.gameplay.actions.negotiation.NegotiationProcedure
import distantlands.model._

object WhisperingStoneCard extends Relic(RelicId("R34"), "Whispering Stone",
    value = 13, defense = 2):
  val power = PrintedPower(PowerId("relic.whispering-stone"),
    persistent = false, cost = Cost(secret = 1),
    text = "**ACTION:** Negotiate with any one player in private, even " +
      "if their pawn is not at your site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Whispering Stone (relic R34), ACTION, 1 secret (catalog batch 12,
  * rulings 21 to 23).
  *
  * It pays its cost onto the relic and queues a Negotiation `Run` at the
  * front of the queue, sourced on the relic, carrying the private marker
  * ([[NegotiationProcedure.privateArgs]]). The queued Negotiation asks for
  * the one player, reads its When Negotiating powers as the Negotiate
  * action does, and keeps its terms from players outside the deal. Its
  * own line, "Negotiate in private.", is public; the Negotiation's opening
  * line names the partner.
  */
case object WhisperingStone extends PaidAction(WhisperingStoneCard.power):
  private val notes = NoteSet(id)
  /** "Negotiate in private.": the use's own line. */
  val negotiateInPrivate: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Negotiate in private.")))
  override val noteKeys: Vector[NoteKey] = notes.declared

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    source match
      case DecisionOptionRef.Relic(stone) =>
        Right(Sequence(Vector(
          QueueProcedure(entry(player, stone), atFront = true),
          Note(id, _ => Some(negotiateInPrivate(PowerSourceRef.Card(stone)))))))
      case other => Left(OathViolation.InvalidEventOrder(
        s"${other.kind} is not a relic source"))

  /** The private Negotiation the Stone queues for `player`. */
  def entry(player: PlayerId, stone: RelicId): QueuedProcedure =
    QueuedProcedure(player, ActionRef.Negotiation,
      NegotiationProcedure.privateArgs, Some(PowerSourceRef.Card(stone)),
      QueueKind.Run)
