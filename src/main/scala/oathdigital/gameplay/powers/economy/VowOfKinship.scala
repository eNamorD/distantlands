package oathdigital.gameplay.powers.economy

import oathdigital.gameplay.{KinshipDraws, KinshipRule, VowOfKinshipCard}
import oathdigital.gameplay.powerresolver.{AsksBeforeBatch, BatchAsk,
  BatchContext, Contribution, Replacement}
import oathdigital.model._

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
  * built, which draws only what the board lacks.
  *
  * The line reads "Spent {2 favor} from the Nomad bank.", naming the
  * holder when they are not the walk's actor. An answer of none writes no
  * line.
  */
case object VowOfKinship extends AsksBeforeBatch:
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

  def watching(ready: ReadyGame): Boolean =
    KinshipRule.bank(ready) > 0 && ready.game.current.players.exists(
      player => player.board.favor > 0 &&
        KinshipRule.holds(ready, player.player))

  def askBefore(batch: BatchContext, operation: CoreOperation)
      : Option[BatchAsk] = for
    holder <- spender(batch.ready, operation)
    if batch.operations.find(KinshipDraws.spent(_, holder) > 0)
      .contains(operation)
    total = batch.operations.map(KinshipDraws.spent(_, holder)).sum
    board = batch.ready.game.current.player(holder).fold(0)(_.board.favor)
    bank = KinshipRule.bank(batch.ready)
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
