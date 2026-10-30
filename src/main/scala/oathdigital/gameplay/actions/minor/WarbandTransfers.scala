package oathdigital.gameplay.actions.minor

import oathdigital.gameplay.NoteSupport
import oathdigital.gameplay.actions.{ImperialLaws, ProcedureNotes}
import oathdigital.model._
import oathdigital.model.OathViolation._

/** Warband Transfers (CR p. 35): as a minor action, an Imperial player
  * moves warbands to or from another Imperial player's board, when that
  * player's pawn is at their site and that player consents. The actor picks the
  * partner, the way and the amount, then confirms; confirming asks the
  * partner's consent to that exact transfer, so the ask names it. */
object WarbandTransfers:
  val partnerDecisionId: String = "minor.transfer-partner"
  val directionDecisionId: String = "minor.transfer-direction"
  val amountDecisionId: String = "minor.transfer-amount"
  val confirmDecisionId: String = "minor.transfer-confirm"
  val give: DecisionOptionRef.Button = DecisionOptionRef.Button("give")
  val take: DecisionOptionRef.Button = DecisionOptionRef.Button("take")

  private val notes = NoteSet(ProcedureNotes.id(ActionRef.TransferWarbands))
  /** "Gave 3 warbands to {Blue}". */
  val gave: NoteKey = notes.key("gave", Vector(NotePart.Text("Gave "),
    NotePart.Arg(0), NotePart.Text(" "),
    NotePart.Plural(0, "warband", "warbands"), NotePart.Text(" to "),
    NotePart.Arg(1)))
  /** "Took 3 warbands from {Blue}". */
  val took: NoteKey = notes.key("took", Vector(NotePart.Text("Took "),
    NotePart.Arg(0), NotePart.Text(" "),
    NotePart.Plural(0, "warband", "warbands"), NotePart.Text(" from "),
    NotePart.Arg(1)))
  def keys: Vector[NoteKey] = notes.declared

  /** The confirm button for moving `amount` warbands `way`: "give.3". */
  def confirmation(way: DecisionOptionRef.Button, amount: Int)
      : DecisionOptionRef.Button =
    DecisionOptionRef.Button(s"${way.key}.$amount")

  /** The way and amount a confirm button names, for the consent ask's log
    * line. */
  def confirmed(ref: DecisionOptionRef)
      : Option[(DecisionOptionRef.Button, Int)] = ref match
    case DecisionOptionRef.Button(key) => key.split('.') match
      case Array(way, amount) if Set(give.key, take.key).contains(way) =>
        amount.toIntOption.filter(_ > 0)
          .map(DecisionOptionRef.Button(way) -> _)
      case _ => None
    case _ => None

  def confirmKey(actor: PlayerId, way: DecisionOptionRef.Button,
      amount: Int): ConsentKey = ConsentKey(ImperialLaws.warbandTransfers,
    actor, confirmDecisionId, confirmation(way, amount))

  val build: MinorActionProcedures.Build = (_, ready, actor, args) =>
    for
      _ <- MinorActionProcedures.started(ready, actor, args,
        ActionRef.TransferWarbands)
      _ <- Either.cond(Imperials.isImperial(ready, actor), (),
        MinorActionUnavailable("Only an Imperial player transfers warbands"))
      _ <- Either.cond(partners(ready, actor).nonEmpty, (),
        MinorActionUnavailable("No other Imperial player is at your site"))
    yield tree(ready, actor)

  val rebuild: MinorActionProcedures.Build = (_, ready, actor, _) =>
    Right(tree(ready, actor))

  /** The other Imperial players whose pawns share the actor's site. */
  private def partners(ready: ReadyGame, actor: PlayerId): Vector[PlayerId] =
    val current = ready.game.current
    current.pawnSite(actor).toVector.flatMap(site =>
      Imperials.players(ready).filter(other => other != actor &&
        current.pawnSite(other).contains(site)))

  private def tree(ready: ReadyGame, actor: PlayerId): Operation =
    Sequence(Vector(
      Decide(partnerDecisionId, actor, DecisionQuery.ChooseOne(
        partners(ready, actor).map(other =>
          DecisionOption.Player(DecisionOptionRef.Player(other))),
        heading = Some("Transfer warbands with")),
        presentation = DecisionPresentation.Panel),
      Branch((live, pending) => partner(pending.answered).toVector.map(
        other => Decide(directionDecisionId, actor, DecisionQuery.ChooseOne(
          Vector(way(live, actor, other, give, "Give warbands"),
            way(live, actor, other, take, "Take warbands")),
          heading = Some("Transfer warbands")),
          presentation = DecisionPresentation.Panel))),
      Branch((live, pending) => (for
        other <- partner(pending.answered)
        picked <- direction(pending.answered)
        max = available(live, actor, other, picked)
        if max > 0
      yield Decide(amountDecisionId, actor, DecisionQuery.ChooseAmount(1,
        max, Some("Transfer how many warbands?"), "Transfer"))).toVector),
      Branch((live, pending) => (for
        other <- partner(pending.answered)
        picked <- direction(pending.answered)
        count <- amount(pending.answered)
      yield Decide(confirmDecisionId, actor, DecisionQuery.ChooseOne(
        Vector(confirmOption(live, actor, other, picked, count)),
        heading = Some("Transfer warbands")),
        presentation = DecisionPresentation.Panel)).toVector),
      BuildOps((_, pending) => Right((for
        other <- partner(pending.answered)
        picked <- direction(pending.answered)
        count <- amount(pending.answered)
        if pending.answered.exists(_.decisionId == confirmDecisionId)
      yield transfer(actor, other, picked, count)).toVector),
        required = true),
      Note(ProcedureNotes.id(ActionRef.TransferWarbands),
        transferredNote(actor))))

  /** The confirm button, askable until the partner consents to this exact
    * transfer (CR p. 35: Warband Transfers needs the other player's
    * consent). */
  private def confirmOption(ready: ReadyGame, actor: PlayerId,
      other: PlayerId, way: DecisionOptionRef.Button, count: Int)
      : DecisionOption =
    val noun = if count == 1 then "warband" else "warbands"
    val shown = DecisionOption.Button(confirmation(way, count),
      if way == give then s"Give $count $noun" else s"Take $count $noun")
    val key = confirmKey(actor, way, count)
    ready.consent.answer(key, Some(other), ConsentTurn.of(ready)) match
      case Some(ConsentAnswer.Granted) => shown
      case Some(ConsentAnswer.Refused) => DecisionOption.Unavailable(shown,
        OptionReason.Noted(ImperialLaws.warbandTransfers,
          ImperialLaws.refused.sourceless(NoteArg.Player(other))))
      case None => DecisionOption.Unavailable(shown, OptionReason.Askable(
        ImperialLaws.warbandTransfers,
        ImperialLaws.needsPartner.sourceless(NoteArg.Player(other)),
        Vector(ConsentAsk(other, Vector(ImperialLaws.warbandTransfers))),
        Vector(key)))

  private def way(ready: ReadyGame, actor: PlayerId, other: PlayerId,
      ref: DecisionOptionRef.Button, label: String): DecisionOption =
    val option = DecisionOption.Button(ref, label)
    if available(ready, actor, other, ref) > 0 then option
    else DecisionOption.Unavailable(option, OptionReason.Written(
      if ref == give then "No warbands on your board"
      else "No warbands on their board"))

  /** The warbands on the giving board. */
  private def available(ready: ReadyGame, actor: PlayerId, other: PlayerId,
      ref: DecisionOptionRef.Button): Int =
    val from = if ref == give then actor else other
    ready.game.current.players.find(_.player == from)
      .fold(0)(_.board.warbands)

  private def transfer(actor: PlayerId, other: PlayerId,
      ref: DecisionOptionRef.Button, amount: Int): CoreOperation =
    val mine = PositionedLocation(Location.PlayArea(actor))
    val theirs = PositionedLocation(Location.PlayArea(other))
    Move(Piece.Warbands(ForceKind.Imperial, amount),
      if ref == give then mine else theirs,
      if ref == give then theirs else mine)

  private def partner(answered: Vector[Answered]): Option[PlayerId] =
    answered.collectFirst {
      case Answered(`partnerDecisionId`, DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Player(other)), _) => other
    }

  private def amount(answered: Vector[Answered]): Option[Int] =
    answered.collectFirst {
      case Answered(`amountDecisionId`,
          DecisionAnswer.ChooseAmountAnswer(value), _) => value
    }

  private def direction(answered: Vector[Answered])
      : Option[DecisionOptionRef.Button] = answered.collectFirst {
    case Answered(`directionDecisionId`, DecisionAnswer.ChooseOneAnswer(
        picked: DecisionOptionRef.Button), _) if picked == give ||
        picked == take => picked
  }

  /** How many warbands moved, read off the actor's board, and whom with. */
  private def transferredNote(actor: PlayerId)(states: NoteStates)
      : Option[LogNote] = for
    step <- states.previous
    other <- partner(states.answered)
    picked <- direction(states.answered)
    change = NoteSupport.warbands(step, actor)
    moved = if picked == give then -change else change
    if moved > 0
  yield (if picked == give then gave else took)
    .sourceless(NoteArg.Number(moved), NoteArg.Player(other))
