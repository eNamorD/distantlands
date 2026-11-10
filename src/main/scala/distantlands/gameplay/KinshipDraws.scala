package distantlands.gameplay

import distantlands.model._

/** Vow of Kinship's draw on the Nomad bank (catalog batch 11, slice 5,
  * ruling 9).
  *
  * A spend is a favor move out of a player's board that the player makes:
  * its mover is the board's owner. A cost's favor moves carry their payer,
  * a gift its giver, and a Challenge's or banner action's placement its
  * actor. A take, a forced burn and a forced placement carry another mover
  * or none, so they draw nothing and read the board only.
  *
  * Before a batch runs, the pass reads the batch in order against the
  * favor on each holder's board and in the Nomad bank. In front of an
  * operation whose spend a holder's board no longer covers, it puts a
  * `DrawNomadFavor` for what the board lacks, capped by the bank. Each
  * operation's favor moves then count: a take or burn from the board
  * lowers it, favor into or out of the Nomad bank changes the bank, and
  * favor bound for a holder's board lands in the bank. A draw already in
  * the batch, the source question's answer, adds to the board. Two holders
  * (Master of Disguise lending the Vow) draw on the one bank.
  *
  * It runs before `BurnTakes`, which turns a cost's taken burn into a
  * `Take` by the taker and so hides its payer.
  */
object KinshipDraws:
  def before(ready: ReadyGame, operations: Vector[CoreOperation])
      : Vector[CoreOperation] =
    val holders = ready.game.current.players.map(_.player)
      .filter(KinshipRule.holds(ready, _))
    if holders.isEmpty then operations
    else
      val boards = holders.map(holder => holder ->
        ready.game.current.player(holder).fold(0)(_.board.favor)).toMap
      operations.foldLeft(Pass(Vector.empty, boards,
          KinshipRule.bank(ready)))(_.next(_)).out

  /** `holder`'s board favor and the Nomad bank's after `operations` run,
    * with the draws the pass would put among them: what a spend after them
    * may take from each. */
  def standing(ready: ReadyGame, operations: Vector[CoreOperation],
      holder: PlayerId): (Int, Int) =
    val holders = (ready.game.current.players.map(_.player)
      .filter(KinshipRule.holds(ready, _)) :+ holder).distinct
    val boards = holders.map(player => player ->
      ready.game.current.player(player).fold(0)(_.board.favor)).toMap
    val pass = operations.foldLeft(Pass(Vector.empty, boards,
      KinshipRule.bank(ready)))(_.next(_))
    (pass.boards(holder), pass.bank)

  /** The favor `operation` spends from `player`'s board. */
  def spent(operation: CoreOperation, player: PlayerId): Int =
    Operation.flatten(operation).collect {
      case Move(Piece.Favor(amount), from, _, _, Some(by))
          if by == player && from.location == Location.PlayArea(player) =>
        amount
    }.sum

  /** The batch so far, each holder's board favor, and the bank's. */
  private final case class Pass(out: Vector[CoreOperation],
      boards: Map[PlayerId, Int], bank: Int):
    def next(operation: CoreOperation): Pass = operation match
      case draw @ DrawNomadFavor(holder, amount) if boards.contains(holder) =>
        Pass(out :+ draw, boards.updated(holder, boards(holder) + amount),
          math.max(0, bank - amount))
      case _ =>
        val drawn = boards.keys.toVector.sortBy(_.value)
          .foldLeft(this) { (pass, holder) =>
            val short = math.min(math.max(0,
              spent(operation, holder) - pass.boards(holder)), pass.bank)
            if short == 0 then pass
            else Pass(pass.out :+ DrawNomadFavor(holder, short),
              pass.boards.updated(holder, pass.boards(holder) + short),
              pass.bank - short)
          }
        drawn.moved(operation).copy(out = drawn.out :+ operation)

    /** The counts after `operation`'s favor moves. */
    private def moved(operation: CoreOperation): Pass =
      Operation.flatten(operation).foldLeft(this) {
        case (pass, Move(Piece.Favor(amount), from, to, _, _)) =>
          pass.leaving(from.location, amount).arriving(to.location, amount)
        case (pass, _) => pass
      }

    private def leaving(at: Location, amount: Int): Pass = at match
      case Location.PlayArea(player) if boards.contains(player) =>
        copy(boards = boards.updated(player,
          math.max(0, boards(player) - amount)))
      case Location.FavorBank(Suit.Nomad) =>
        copy(bank = math.max(0, bank - amount))
      case _ => this

    private def arriving(at: Location, amount: Int): Pass = at match
      case Location.PlayArea(player) if boards.contains(player) =>
        copy(bank = bank + amount)
      case Location.FavorBank(Suit.Nomad) => copy(bank = bank + amount)
      case _ => this
