package oathdigital.gameplay.chronicle

import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.{PlayerFacts, PowerAnswers}
import oathdigital.model._

/** Offer Citizenship, the Throne's first step (CR p. 37, Chronicle rulings
  * 5 and 20-22). The Throne holder offers Citizenship to any seated Exiles,
  * none included; the offered Exiles answer at once, in any order, Decline
  * first, and each answer applies when it is given.
  * An Exile who accepts becomes a Citizen: their board warbands return to
  * their bank, and at each site they rule their warbands become one
  * Imperial warband. No other Citizenship step runs in the Chronicle. The
  * step assumes unlimited purple warbands and adds any the bank lacks.
  *
  * A former Chancellor needs no rule of their own: Crown Chancellor left
  * them the Empire's sites (ruling 20), which turn Imperial like any
  * Exile's.
  *
  * Lines, as the Throne's other steps name their players: "p1 offered
  * Citizenship to p2, p3", "p2 accepted Citizenship" and "p3 declined
  * Citizenship".
  */
object OfferCitizenship:
  val offerDecisionId: String = "chronicle.throne.offer"
  def answerDecisionId(player: PlayerId): String =
    s"chronicle.throne.citizenship.${player.value}"
  val accept: DecisionOptionRef.Button = DecisionOptionRef.Button("accept")
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val offered: NoteKey = notes.key("offered", Vector(NotePart.Arg(0),
    NotePart.Text(" offered Citizenship to "), NotePart.Arg(1)))
  val accepted: NoteKey = notes.key("accepted", Vector(NotePart.Arg(0),
    NotePart.Text(" accepted Citizenship")))
  val declined: NoteKey = notes.key("declined", Vector(NotePart.Arg(0),
    NotePart.Text(" declined Citizenship")))
  def noteKeys: Vector[NoteKey] = notes.declared

  val steps: Vector[Operation] = Vector(Branch((state, _) =>
    ChronicleTasks.forHolder(state, ChronicleTask.Throne)(holder =>
      exiles(state) match
        case Vector() => Vector.empty
        case candidates => Vector(
          Decide(offerDecisionId, holder, DecisionQuery.ChooseMany(0,
            candidates.size, ChronicleTasks.playerOptions(candidates),
            heading = Some("Offer Citizenship to any Exiles"),
            confirm = Some(CountedConfirm("Offer Citizenship to", "Exile",
              "Exiles")))),
          Branch((live, pending) => answers(live, holder, chosen(pending)))))))

  /** The seated Exiles, in seat order (ruling 21). */
  private def exiles(state: ReadyGame): Vector[PlayerId] =
    ChronicleTasks.seated(state).filter(player =>
      Imperials.role(state, player).contains(Role.Exile))

  private def chosen(pending: PendingTree): Vector[PlayerId] =
    PowerAnswers.many(pending, offerDecisionId).getOrElse(Vector.empty)
      .collect { case DecisionOptionRef.Player(player) => player }

  private def answers(state: ReadyGame, holder: PlayerId,
      offeredTo: Vector[PlayerId]): Vector[Operation] =
    // Seat order of the players offered, read from the seats alone: an Exile
    // who accepts is no longer one, and the tree must keep its shape. The
    // answers are open at once; seat order is only the search's order.
    val ordered = ChronicleTasks.seated(state).filter(offeredTo.contains)
    if ordered.isEmpty then Vector.empty
    else Vector(Note(noteId, _ => Some(offered.sourceless(
        NoteArg.Player(holder), NoteArg.Players(ordered)))),
      Simultaneous(ordered.map(exile => new Sequence(answer(exile)))))

  private def answer(exile: PlayerId): Vector[Operation] = Vector(
    Decide(answerDecisionId(exile), exile, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(decline, "Decline"),
      DecisionOption.Button(accept, "Become a Citizen")),
      heading = Some("The new Chancellor offers you Citizenship"))),
    Branch((_, pending) =>
      if PowerAnswers.one(pending, answerDecisionId(exile)).contains(accept)
      then Vector(BuildOps((live, _) => joins(live, exile), required = true),
        Note(noteId, _ => Some(accepted.sourceless(NoteArg.Player(exile)))))
      else Vector(Note(noteId, _ => Some(declined.sourceless(
        NoteArg.Player(exile)))))))

  /** `exile` becomes a Citizen. The board and the sites change before the
    * role does, as in `RoleChanges`: a board's warbands follow its owner's
    * role, and the player's own colour must still be theirs while their
    * warbands leave. The replacements name no one who performs them
    * (ruling R20). */
  private def joins(ready: ReadyGame, exile: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] =
    PlayerFacts.player(ready, exile).map { state =>
      val own = ForceKind.Exile(state.lineage)
      val current = ready.game.current
      val ruled = current.map.inPlay.flatMap(site =>
        current.map.sites.get(site).map(_.forces).collect {
          case SiteForces.Occupied(`own`, n) => site -> n })
      val board = Option.when(state.board.warbands > 0)(Move(
        Piece.Warbands(own, state.board.warbands),
        PositionedLocation(Location.PlayArea(exile)),
        PositionedLocation(Location.WarbandBank(own)))).toVector
      val short = ruled.size - PlayerFacts.banked(ready, ForceKind.Imperial)
      val added = Option.when(short > 0)(WarbandsAdded(ForceKind.Imperial, short)).toVector
      val swaps = ruled.flatMap((site, n) =>
        val at = PositionedLocation(Location.Site(site))
        Option.when(n > 1)(Kill(Piece.Warbands(own, n - 1), at)).toVector :+
          Replace(Piece.Warbands(own, 1), Piece.Warbands(ForceKind.Imperial, 1),
            at))
      board ++ added ++ swaps :+ RoleChanged(state.lineage, Role.Citizen)
    }
