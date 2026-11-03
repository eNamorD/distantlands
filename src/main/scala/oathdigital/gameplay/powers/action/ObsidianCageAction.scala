package oathdigital.gameplay.powers.action

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.gameplay.powers.campaign.ObsidianCageCard
import oathdigital.model._

/** Obsidian Cage (relic R18), ACTION: "Move any number of warbands from
  * Obsidian Cage to any board of the same color."
  *
  * Free. The Cage's holder moves caged warbands onto the boards whose
  * warbands are of their force: an Exile's back to the player of that
  * lineage, Imperial ones to the Chancellor's or a Citizen's board. One
  * destination asks how many, from one to all of that force; several ask
  * a distribution, of at least one. It is usable only while a caged force
  * has a board to go to.
  *
  * Lines, one per board that gained warbands: "Returned {2} warbands from
  * Obsidian Cage to {Blue}." for another's board, and "Took back {2}
  * warbands from Obsidian Cage." for the user's own.
  */
case object ObsidianCageAction extends PaidAction(ObsidianCageCard.action):
  val relic: RelicId = ObsidianCageCard.id
  val decisionId: String = "power.obsidian-cage.return"
  val heading: String = "Obsidian Cage: move how many warbands to their board?"
  val confirm: String = "Move warbands"
  private val notes = NoteSet(id)
  /** "Returned {n} warbands from Obsidian Cage to {Blue}." */
  val returned: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Returned "), NotePart.Arg(0),
    NotePart.Plural(0, " warband from Obsidian Cage to ",
      " warbands from Obsidian Cage to "), NotePart.Arg(1),
    NotePart.Text(".")))
  /** "Took back {n} warbands from Obsidian Cage." */
  val tookBack: NoteKey = notes.key("used.own", Vector(
    NotePart.Text("Took back "), NotePart.Arg(0),
    NotePart.Plural(0, " warband from Obsidian Cage.",
      " warbands from Obsidian Cage.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** A board caged warbands may go to: its player, their force and how many
    * of that force the Cage holds. */
  final case class Destination(player: PlayerId, kind: ForceKind, caged: Int)

  /** Every board of a force the Cage holds, in seat order. */
  def destinations(ready: ReadyGame): Vector[Destination] =
    val current = ready.game.current
    current.players.flatMap(player => PlayerForceKind.of(ready, player)
      .map(kind => Destination(player.player, kind,
        current.warbandsOn(relic, kind))).filter(_.caged > 0))

  override def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef): Boolean =
    destinations(ready).nonEmpty

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    val boards = destinations(ready)
    Right(Sequence(Vector[Operation](
      Decide(decisionId, player, query(boards)),
      BuildOps((live, pending) => moves(live, boards, pending))) ++
      boards.map(board => Note(id, movedNote(board.player, player)))))

  private def query(boards: Vector[Destination]): DecisionQuery =
    boards match
      case Vector(only) => DecisionQuery.ChooseAmount(1, only.caged,
        Some(heading), confirm, suggested = Some(only.caged))
      case several => DecisionQuery.Distribute(several.map(board =>
        DistributeSlot(DecisionOptionRef.Player(board.player), 0,
          board.caged, None)), 1, several.map(_.caged).sum,
        Some(heading), confirm)

  /** How many go to each board, from the answer. */
  private def chosen(boards: Vector[Destination], pending: PendingTree)
      : Option[Vector[(Destination, Int)]] = boards match
    case Vector(only) =>
      PowerAnswers.amount(pending, decisionId).map(n => Vector(only -> n))
    case several => PowerAnswers.distribution(pending, decisionId).map(rows =>
      several.map(board => board -> rows.collectFirst {
        case DistributeAmount(DecisionOptionRef.Player(p), n)
            if p == board.player => n }.getOrElse(0)))

  /** The moves off the Cage, refused when a force's boards would take more
    * than the Cage holds of it. */
  private def moves(ready: ReadyGame, boards: Vector[Destination],
      pending: PendingTree): Either[OathViolation, Vector[CoreOperation]] =
    for
      amounts <- chosen(boards, pending).toRight(
        PowerAnswers.missing(decisionId))
      _ <- amounts.groupBy(_._1.kind).toVector.find((kind, rows) =>
          rows.map(_._2).sum > ready.game.current.warbandsOn(relic, kind))
        .fold(Right(()))((kind, _) => Left(OathViolation.InvalidEventOrder(
          s"more $kind warbands than Obsidian Cage holds")))
    yield amounts.filter(_._2 > 0).map((board, n) => Move(
      Piece.Warbands(board.kind, n),
      PositionedLocation(Location.OnCard(relic)),
      PositionedLocation(Location.PlayArea(board.player))))

  /** What `owner`'s board gained in the step before. */
  private def movedNote(owner: PlayerId, user: PlayerId)(states: NoteStates)
      : Option[LogNote] = for
    (before, after) <- states.previous
    gained = boardOf(after, owner) - boardOf(before, owner)
    if gained > 0
  yield
    val card = PowerSourceRef.Card(relic)
    if owner == user then tookBack(card, NoteArg.Number(gained))
    else returned(card, NoteArg.Number(gained), NoteArg.Player(owner))

  private def boardOf(ready: ReadyGame, player: PlayerId): Int =
    ready.game.current.players.find(_.player == player)
      .fold(0)(_.board.warbands)
