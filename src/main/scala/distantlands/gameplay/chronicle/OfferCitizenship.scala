package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.legacies.IronHand
import distantlands.gameplay.powers.{PlayerFacts, PowerAnswers}
import distantlands.model._

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
  * An Exile who accepts also turns their newly dormant legacies active,
  * and, once every answer is in, discards any excess over three active
  * legacies (Legacies implementation rulings R19 and R20).
  *
  * Lines, as the Throne's other steps name their players: "p1 offered
  * Citizenship to p2, p3", "p2 accepted Citizenship", "p2 reactivated
  * Iron Hand" and "p3 declined Citizenship".
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
  /** "p2 reactivated Iron Hand": a newly dormant legacy active again as its
    * Exile becomes a Citizen (Legacies implementation ruling R20). */
  val reactivated: NoteKey = notes.key("reactivated-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" reactivated "), NotePart.Arg(1)))
  /** "p2 discarded Seer": a discard over the cap after a reactivation
    * (R19). */
  val capped: NoteKey = notes.key("capped-legacy", Vector(
    NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1)))
  def capDecisionId(player: PlayerId): String =
    s"chronicle.throne.legacy-cap.${player.value}"
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The offer, read when reached. Once the offer is answered the shape
    * holds whatever roles have changed since: the candidates are then the
    * Exiles left and every player offered, so a walk resumed after the last
    * Exile accepted still finds the offer it answered. */
  val steps: Vector[Operation] = Vector(Branch((state, pending) =>
    ChronicleTasks.forHolder(state, ChronicleTask.Throne)(holder =>
      val answered = pending.answered.exists(_.decisionId == offerDecisionId)
      val offeredTo = if answered then chosen(pending) else Vector.empty
      ChronicleTasks.seated(state).filter(player =>
          exiles(state).contains(player) || offeredTo.contains(player))
      match
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
      Simultaneous(ordered.map(exile => new Sequence(answer(exile)))),
      // Iron Hand's third checkpoint: a new Citizen discards it before the
      // cap (Legacy Chronicle powers design, ruling 18; R30).
      IronHand.discard(TriggeredProcedureRef.ChronicleThrone),
      // Once every answer is in, each new Citizen left above three active
      // legacies discards the excess (R19), in seat order: a simultaneous
      // answer cannot ask a second question.
      Branch((_, pending) => ordered.filter(exile =>
          PowerAnswers.one(pending, answerDecisionId(exile)).contains(accept))
        .map(exile => ResolveLegacies.cap(exile, capDecisionId(exile), noteId,
          capped))))

  private def answer(exile: PlayerId): Vector[Operation] = Vector(
    Decide(answerDecisionId(exile), exile, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(decline, "Decline"),
      DecisionOption.Button(accept, "Become a Citizen")),
      heading = Some("The new Chancellor offers you Citizenship"))),
    Branch((_, pending) =>
      if PowerAnswers.one(pending, answerDecisionId(exile)).contains(accept)
      then Vector(BuildOps((live, _) => joins(live, exile), required = true),
        Note(noteId, _ => Some(accepted.sourceless(NoteArg.Player(exile)))),
        ResolveLegacies.reactivation(exile, noteId, reactivated))
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
      val board = Option.when[CoreOperation](state.board.warbands > 0)(Move(
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
