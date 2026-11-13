package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

/** Score Lineage Points, the Throne's sixth step (CR p. 37, Chronicle ruling
  * 13). The winner scores 3 lineage points plus their legacy bonus
  * (`bonus`). Any number of times, they may spend 2 of the points they are
  * scoring to let another seated player score 1. */
object LineageScoring:
  val giftsDecisionId: String = "chronicle.score-lineage.gifts"
  val basePoints: Int = 3

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val scored: NoteKey = notes.key("scored", Vector(NotePart.Arg(0),
    NotePart.Text(" scored "), NotePart.Arg(1)))
  val gave: NoteKey = notes.key("gave", Vector(NotePart.Arg(0),
    NotePart.Text(" gave "), NotePart.Arg(1), NotePart.Text(" to "),
    NotePart.Arg(2)))
  /** "p1 scored 7 lineage points (3 base, 3 from Beast, 1 from Nomad)". */
  val scoredWithBonus: NoteKey = notes.key("scored-with-bonus", Vector(
    NotePart.Arg(0), NotePart.Text(" scored "), NotePart.Arg(1),
    NotePart.Text(" ("), NotePart.Arg(2), NotePart.Text(" base, "),
    NotePart.Arg(3), NotePart.Text(")")))
  /** "p1 scored 3 lineage points (3 base, 3 from Beast, 1 from Nomad, less
    * 4 given)": the kept points after gifts (R28). */
  val scoredWithBonusLessGifts: NoteKey = notes.key(
    "scored-with-bonus-less-gifts", Vector(NotePart.Arg(0),
      NotePart.Text(" scored "), NotePart.Arg(1), NotePart.Text(" ("),
      NotePart.Arg(2), NotePart.Text(" base, "), NotePart.Arg(3),
      NotePart.Text(", less "), NotePart.Arg(4), NotePart.Text(" given)")))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The winner's legacy bonus by suit (CR, Score Lineage Points; Legacies
    * ruling 10): for each legacy the winner holds (R33), 1 point per card
    * of its printed suit, denizens and edifices, intact or ruined (R32), at
    * the sites the winner rules; advisers do not count. Legacies of one
    * suit add into one share, a suit that scores nothing is left out, and
    * shares come in suit order (R31). After Crown Chancellor the winner is
    * the Chancellor, so these are the Empire's sites (Chronicle ruling
    * 11). */
  def bonus(ready: ReadyGame, catalog: ExecutableCatalog, winner: PlayerId)
      : Vector[(Suit, Int)] =
    val current = ready.game.current
    val cards = current.map.inPlay
      .filter(ChronicleTasks.rules(ready, winner, _))
      .flatMap(site => current.map.sites.get(site).toVector.flatMap(_.denizens))
      .flatMap(card => catalog.suitOf(card.id))
    val legacies = ChronicleTasks.lineageOf(ready, winner)
      .flatMap(ready.game.campaign.lineages.get).toVector
      .flatMap(_.legacies).flatMap(legacy => catalog.legacy(legacy.id))
    Suit.all.map(suit => suit ->
      legacies.count(_.suit == suit) * cards.count(_ == suit))
      .filter(_._2 > 0)

  def steps(catalog: ExecutableCatalog): Vector[Operation] =
    Vector(Branch((state, _) =>
      ChronicleTasks.winner(state).fold(violation =>
        Vector(ChronicleTasks.failing(violation)),
        winner => score(state, winner, bonus(state, catalog, winner)))))

  private def points(amount: Int): NoteArg =
    NoteArg.Amount(amount, NoteUnit.LineagePoint)

  /** The gifts, bounded by half the winner's total, rounded down; then the
    * award. */
  private def score(state: ReadyGame, winner: PlayerId,
      shares: Vector[(Suit, Int)]): Vector[Operation] =
    val others = ChronicleTasks.seated(state).filterNot(_ == winner)
    val total = basePoints + shares.map(_._2).sum
    val giftable = total / 2
    if giftable == 0 || others.isEmpty then
      Vector(award(state, winner, shares, Vector.empty))
    else Vector(
      Decide(giftsDecisionId, winner, DecisionQuery.Distribute(
        others.map(player => DistributeSlot(DecisionOptionRef.Player(player),
          0, giftable, None)), 0, giftable,
        heading = Some("Give lineage points: 2 of yours for 1 of theirs"),
        confirmLabel = "Score")),
      Branch((live, pending) => PowerAnswers.distribution(pending,
        giftsDecisionId) match
        case Some(rows) => Vector(award(live, winner, shares, rows.collect {
          case DistributeAmount(DecisionOptionRef.Player(player), amount)
              if amount > 0 => player -> amount
        }))
        case None => Vector(ChronicleTasks.failing(
          PowerAnswers.missing(giftsDecisionId)))))

  /** The winner's points less 2 per gift, then each gift, and their lines.
    * With a bonus the winner's line is always written and names its shares,
    * even at 0 kept, which records nothing (R28, R29). */
  private def award(state: ReadyGame, winner: PlayerId,
      shares: Vector[(Suit, Int)], gifts: Vector[(PlayerId, Int)]): Operation =
    val spent = 2 * gifts.map(_._2).sum
    val kept = basePoints + shares.map(_._2).sum - spent
    val scoring = Option.when(kept > 0)(winner -> kept).toVector ++ gifts
    val ops = scoring.flatMap((player, amount) =>
      ChronicleTasks.lineageOf(state, player).map(
        LineagePointsScored(_, amount)))
    def line(player: PlayerId, amount: Int): Operation =
      Note(noteId, _ => Some(
        if player != winner || shares.isEmpty then
          scored.sourceless(NoteArg.Player(player), points(amount))
        else
          val breakdown = Vector(NoteArg.Player(winner), points(amount),
            NoteArg.Number(basePoints), NoteArg.SuitShares(
              shares.map((suit, share) => share -> suit)))
          if spent == 0 then scoredWithBonus.sourceless(breakdown*)
          else scoredWithBonusLessGifts.sourceless(
            (breakdown :+ NoteArg.Number(spent))*)))
    val lines = Option.when(kept > 0 || shares.nonEmpty)(winner -> kept)
      .toVector ++ gifts
    Sequence(BuildOps((_, _) => Right(ops), required = true) +:
      (lines.map(line) ++
        gifts.map((player, amount) => Note(noteId, _ => Some(
          gave.sourceless(NoteArg.Player(winner), points(amount),
            NoteArg.Player(player)))))))
