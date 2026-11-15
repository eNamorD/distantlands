package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object PopulistCard extends Legacy(LegacyId("L16"), "Populist",
    LegacyGoal.PeoplesFavor, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.populist"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** When setting up the favor banks, you may choose " +
      "to divide the favor among the banks evenly. If you do, take " +
      "any remainder. _For example, dividing 19 favor among the " +
      "banks leaves you one favor._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Populist (legacy L16), its SETUP power (Legacy Setup powers design,
  * ruling 12). In the `SetupBanks` window its holder may divide all the
  * favor on the six suit banks evenly: each bank ends with the total
  * divided by six, rounded down, and the holder takes the remainder. It
  * applies under both faces of Foundation II; favor edifices add at
  * `SetupEnd` comes later. One `ChooseOne`, Decline first (R14). Lines:
  * "p2 divided the favor evenly among the banks with Populist and took 1
  * favor", or with no remainder the same without its last words (R15). */
case object Populist extends ContributingPower:
  val id: PowerId = PopulistCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupBanks -> Vector(Transform((_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.populist.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")
  val divide: DecisionOptionRef.Button = DecisionOptionRef.Button("divide")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val dividedAndTook: NoteKey = notes.key("populist-divided-took", Vector(
    NotePart.Arg(0),
    NotePart.Text(" divided the favor evenly among the banks with "),
    NotePart.Arg(1), NotePart.Text(" and took "), NotePart.Arg(2)))
  val divided: NoteKey = notes.key("populist-divided", Vector(
    NotePart.Arg(0),
    NotePart.Text(" divided the favor evenly among the banks with "),
    NotePart.Arg(1)))
  /** The Setup lines this power writes, on Setup's note id. */
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** Each bank's even share of `favor` and the remainder the holder takes. */
  def shares(favor: Map[Suit, Int]): (Int, Int) =
    val total = Suit.all.map(favor.getOrElse(_, 0)).sum
    (total / Suit.all.size, total % Suit.all.size)

  /** The moves that leave every bank its share: each bank's surplus goes to
    * the banks short of it in suit order, and what is left to the holder. */
  def moves(ready: ReadyGame, holder: PlayerId): Vector[CoreOperation] =
    val held = Suit.all.map(suit => suit -> ready.banks.favor.getOrElse(suit, 0))
    val (each, _) = shares(ready.banks.favor)
    val surplus = held.collect { case (suit, n) if n > each => suit -> (n - each) }
    val short = held.collect { case (suit, n) if n < each => suit -> (each - n) }
    def bank(suit: Suit) = PositionedLocation(Location.FavorBank(suit))
    @annotation.tailrec
    def pair(from: List[(Suit, Int)], to: List[(Suit, Int)],
        done: Vector[CoreOperation]): Vector[CoreOperation] = (from, to) match
      case ((giver, give) :: givers, (taker, take) :: takers) =>
        val moved = math.min(give, take)
        pair(if give > moved then (giver, give - moved) :: givers else givers,
          if take > moved then (taker, take - moved) :: takers else takers,
          done :+ Move(Piece.Favor(moved), bank(giver), bank(taker)))
      case (rest, _) => done ++ rest.map((giver, left) => Move(
        Piece.Favor(left), bank(giver),
        PositionedLocation(Location.PlayArea(holder))))
    pair(surplus.toList, short.toList, Vector.empty)

  private def step: Operation = Branch((state, _) =>
    SetupLegacyRules.populist(state).toVector.flatMap { holder =>
      val (each, rest) = shares(state.banks.favor)
      Vector(
        Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(Vector(
          DecisionOption.Button(decline, "Decline"),
          DecisionOption.Button(divide,
            s"Divide evenly: $each favor a bank, $rest for you")),
          heading = Some("Populist: you may divide the banks' favor evenly"))),
        Branch((live, pending) =>
          if !PowerAnswers.one(pending, decisionId(holder)).contains(divide)
          then Vector.empty
          else dividing(holder, shares(live.banks.favor)._2)))
    })

  /** The moves, then the line naming the remainder `taken` (R15). */
  private def dividing(holder: PlayerId, taken: Int): Vector[Operation] =
    val line =
      if taken == 0 then divided.sourceless(NoteArg.Player(holder),
        NoteArg.Card(PopulistCard.id))
      else dividedAndTook.sourceless(NoteArg.Player(holder),
        NoteArg.Card(PopulistCard.id), NoteArg.Amount(taken, NoteUnit.Favor))
    Vector(BuildOps((ready, _) => Right(moves(ready, holder)),
        required = true),
      Note(noteId, _ => Some(line)))
