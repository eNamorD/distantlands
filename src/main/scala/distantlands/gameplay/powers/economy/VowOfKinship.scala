package distantlands.gameplay.powers.economy

import distantlands.gameplay.{KinshipDraws, KinshipRule, VowOfKinshipCard}
import distantlands.gameplay.powerresolver.{AdviserArrivals, ArrivalFollowUp,
  AsksBeforeBatch, BatchAsk, BatchContext, Contribution, FollowsArrivals,
  Replacement}
import distantlands.model._

/** Vow of Kinship (card 152, catalog batch 11, rulings 9, 10, 12 and 26 to
  * 29). The credit, the draw and the spend checks are rules
  * (`KinshipRule`, `KinshipDraws`, `SpendableFavor`). This power asks the
  * source question.
  *
  * When the holder's board and the Nomad bank both hold favor, a batch
  * whose spend could take from either asks the holder, at its first spend,
  * how much of the batch's spend comes from the bank (ruling 10). The
  * least is what the board cannot cover; the most is the spend, or the
  * bank if it holds less. The answer puts a `DrawNomadFavor` in front of
  * that spend, and the board pays the rest. A search walks the batch as
  * built, which draws only what the board lacks. A cost walked as its
  * moves is a batch per move, so a cost that both places and burns favor
  * asks once for each.
  *
  * The line reads "Spent {2 favor} from the Nomad bank.", naming the
  * holder when they are not the walk's actor. An answer of none writes no
  * line.
  *
  * When the Vow becomes a faceup adviser, its holder's board favor moves to
  * the Nomad bank (Readings). When it leaves, the favor stays in the bank.
  */
case object VowOfKinship extends AsksBeforeBatch, FollowsArrivals:
  val id: PowerId = VowOfKinshipCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  val decisionId: String = "vow-of-kinship.source"

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Spent {2 favor} from the Nomad bank." */
  val spentFromBank: NoteKey = notes.key("spent-from-bank", Vector(
    NotePart.Text("Spent "), NotePart.Arg(0),
    NotePart.Text(" from the Nomad bank.")))
  /** "{Blue} spent {2 favor} from the Nomad bank.", for a holder who is
    * not the walk's actor. */
  val spentFromBankOffTurn: NoteKey = notes.key("spent-from-bank-off-turn",
    Vector(NotePart.Arg(0), NotePart.Text(" spent "), NotePart.Arg(1),
      NotePart.Text(" from the Nomad bank.")))

  /** "Moved {3 favor} to the Nomad bank." */
  val moved: NoteKey = notes.key("moved", Vector(NotePart.Text("Moved "),
    NotePart.Arg(0), NotePart.Text(" to the Nomad bank.")))
  /** "{Blue} moved {3 favor} to the Nomad bank.", for a holder who is not
    * the walk's actor. */
  val movedOffTurn: NoteKey = notes.key("moved-off-turn", Vector(
    NotePart.Arg(0), NotePart.Text(" moved "), NotePart.Arg(1),
    NotePart.Text(" to the Nomad bank.")))

  /** The Vow turned faceup among a player's advisers: their board's favor
    * moves to the Nomad bank. Nobody moves it. */
  def onArrival(arrived: AdviserArrivals): Option[ArrivalFollowUp] = for
    arrival <- arrived.arrivals.find(_.card == VowOfKinshipCard.id)
    holder = arrival.player
    favor <- arrived.after.game.current.player(holder).map(_.board.favor)
    if favor > 0
  yield ArrivalFollowUp(Vector(Move(Piece.Favor(favor),
      PositionedLocation(Location.PlayArea(holder)),
      PositionedLocation(Location.FavorBank(Suit.Nomad)))),
    Note(id, _ => Some(movedLine(holder, arrived.actor, favor))))

  private def movedLine(holder: PlayerId, actor: PlayerId, favor: Int)
      : LogNote =
    val card = PowerSourceRef.Card(VowOfKinshipCard.id)
    val amount = NoteArg.Amount(favor, NoteUnit.Favor)
    if holder == actor then moved(card, amount)
    else movedOffTurn(card, NoteArg.Player(holder), amount)

  def watching(ready: ReadyGame): Boolean =
    KinshipRule.bank(ready) > 0 && ready.game.current.players.exists(
      player => player.board.favor > 0 &&
        KinshipRule.holds(ready, player.player))

  def askBefore(batch: BatchContext, operation: CoreOperation)
      : Option[BatchAsk] = for
    holder <- spender(batch.ready, operation)
    first = batch.operations.indexWhere(KinshipDraws.spent(_, holder) > 0)
    if batch.operations.lift(first).contains(operation)
    total = batch.operations.drop(first).map(KinshipDraws.spent(_, holder))
      .sum
    // The board and the bank as they stand at the first spend: after the
    // operations before it, and after the replacements the batch's earlier
    // answers took (a Wild Pastures payment).
    (board, bank) = KinshipDraws.standing(batch.ready,
      batch.taken.flatMap(_.operations) ++ batch.operations.take(first),
      holder)
    least = math.max(0, total - board)
    most = math.min(total, bank)
    if board > 0 && bank > 0 && least < most
  yield BatchAsk(
    Decide(decisionId, holder, DecisionQuery.ChooseAmount(least, most,
      Some(s"Vow of Kinship: how much of the $total favor comes from the " +
        "Nomad bank?"), "Take from the Nomad bank")),
    {
      case DecisionAnswer.ChooseAmountAnswer(drawn) if drawn > 0 =>
        Some(Replacement(Vector(DrawNomadFavor(holder, drawn), operation),
          line(holder, batch.actor, drawn)))
      case _ => None
    })

  /** The holder whose spend `operation` makes, if any. */
  private def spender(ready: ReadyGame, operation: CoreOperation)
      : Option[PlayerId] =
    ready.game.current.players.map(_.player).find(player =>
      KinshipRule.holds(ready, player) &&
        KinshipDraws.spent(operation, player) > 0)

  private def line(holder: PlayerId, actor: PlayerId, drawn: Int): LogNote =
    val card = PowerSourceRef.Card(VowOfKinshipCard.id)
    val amount = NoteArg.Amount(drawn, NoteUnit.Favor)
    if holder == actor then spentFromBank(card, amount)
    else spentFromBankOffTurn(card, NoteArg.Player(holder), amount)
