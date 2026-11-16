package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, ChronicleTasks,
  ShapeEmpire}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.{PowerAnswers, SiteRoom}
import distantlands.model._

object ReformerCard extends Legacy(LegacyId("L35"), "Reformer",
    LegacyGoal.MostOfSuit, Suit.Order, Some(FoundationNumber.V)):
  val power = PrintedPower(PowerId("legacy.reformer"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation V to Grand Council. **CHRONICLE (START OF " +
      "THRONE):** If you won, you may move and swap denizens at " +
      "sites you rule, ignoring the locked restriction.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Reformer (legacy L35): "If you won, you may move and swap denizens at
  * sites you rule, ignoring the locked restriction." Its step is the window
  * at the start of the Throne (Legacy Chronicle powers design, ruling 12),
  * only when the holder is the game's winner. Each pass is one move or
  * swap (R11): the holder picks a card at a site they rule, edifices
  * included (R10), or Done; then its destination, another site they rule
  * with room (a move) or a card at another site they rule (a swap). Passes
  * repeat until Done or until no card has a destination. Each is read when
  * reached, under the same two ids, from the latest answers, as Call for
  * Help's passes are (R13). Locks do not bind in the Chronicle (Chronicle
  * ruling 16). A swap is two moves, so each card takes the last slot of
  * the other's site. Lines (R12): "p1 moved Village Idiot to Broken Peaks
  * with Reformer", "p1 swapped Village Idiot and Map Library with
  * Reformer". */
case object Reformer extends ContributingPower:
  val id: PowerId = ReformerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleStartOfThrone -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  def pickId(player: PlayerId): String =
    s"chronicle.throne.legacy.reformer.${player.value}"
  def destinationId(player: PlayerId): String = pickId(player) + ".to"
  val done: DecisionOptionRef.Button = DecisionOptionRef.Button("done")

  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val moved: NoteKey = notes.key("reformer-moved", Vector(NotePart.Arg(0),
    NotePart.Text(" moved "), NotePart.Arg(1), NotePart.Text(" to "),
    NotePart.Arg(2), NotePart.Text(" with "), NotePart.Arg(3)))
  val swapped: NoteKey = notes.key("reformer-swapped", Vector(
    NotePart.Arg(0), NotePart.Text(" swapped "), NotePart.Arg(1),
    NotePart.Text(" and "), NotePart.Arg(2), NotePart.Text(" with "),
    NotePart.Arg(3)))
  /** The Throne lines this power writes, on the Throne's note id. */
  def throneNoteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, ReformerCard.id)
      .filter(holder => ChronicleTasks.winner(state).contains(holder))
      .toVector.map(holder => Repeat((live, pending) =>
          !finished(pending, holder) && movable(catalog, live, holder).nonEmpty,
        Sequence(Vector(
          Branch((live, _) => Vector(Decide(pickId(holder), holder,
            DecisionQuery.ChooseOne(DecisionOption.Button(done, "Done") +:
              movable(catalog, live, holder),
              heading = Some("Reformer: you may move or swap a card at " +
                "sites you rule"))))),
          Branch((live, pending) => picked(pending, holder)
            .filter(_ => owed(pending, holder)).toVector.map(card =>
              Decide(destinationId(holder), holder, DecisionQuery.ChooseOne(
                destinations(catalog, live, holder, card),
                heading = Some("Reformer: move it to a site, or swap it " +
                  "with a card"))))),
          Branch((live, pending) => applied(live, holder, pending)))))))

  private def row(state: ReadyGame, site: SiteId): Vector[SiteDenizenState] =
    state.game.current.map.sites.get(site).toVector.flatMap(_.denizens)

  private def siteOf(state: ReadyGame, card: CardId): Option[SiteId] =
    state.game.current.map.inPlay.find(row(state, _).exists(_.id == card))

  /** Each other ruled site with room, then each card at another ruled site,
    * in map order. */
  private def destinations(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId, card: CardId): Vector[DecisionOption] =
    val from = siteOf(state, card)
    ChronicleTasks.ruled(state, holder).filterNot(from.contains).flatMap(site =>
      Option.when(SiteRoom.hasRoom(catalog, state, site))(
        DecisionOption.Site(DecisionOptionRef.Site(site))).toVector ++
        row(state, site).map(ShapeEmpire.option))

  /** The cards at ruled sites that have a destination. */
  private def movable(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId): Vector[DecisionOption] =
    ChronicleTasks.ruled(state, holder).flatMap(row(state, _)).filter(card =>
      destinations(catalog, state, holder, card.id).nonEmpty)
      .map(ShapeEmpire.option)

  private def cardOf(ref: DecisionOptionRef): Option[CardId] = ref match
    case DecisionOptionRef.Denizen(id) => Some(id)
    case DecisionOptionRef.Edifice(id) => Some(id)
    case _ => None

  private def picks(pending: PendingTree, holder: PlayerId)
      : Vector[DecisionOptionRef] = pending.answered.collect {
    case Answered(id, DecisionAnswer.ChooseOneAnswer(ref), _)
        if id == pickId(holder) => ref }

  private def destinationCount(pending: PendingTree, holder: PlayerId): Int =
    pending.answered.count(_.decisionId == destinationId(holder))

  private def finished(pending: PendingTree, holder: PlayerId): Boolean =
    PowerAnswers.last(pending.answered, pickId(holder)).contains(done)

  /** The latest pick, when it is a card. */
  private def picked(pending: PendingTree, holder: PlayerId): Option[CardId] =
    PowerAnswers.last(pending.answered, pickId(holder)).flatMap(cardOf)

  /** Whether a card pick still waits for its destination: once per pass. */
  private def owed(pending: PendingTree, holder: PlayerId): Boolean =
    picks(pending, holder).count(cardOf(_).nonEmpty) >
      destinationCount(pending, holder)

  /** The pass's move or swap, once both its answers are in; nothing for
    * Done, and nothing for an earlier pass, whose answers are no longer the
    * latest. */
  private def applied(state: ReadyGame, holder: PlayerId,
      pending: PendingTree): Vector[Operation] =
    val complete = picked(pending, holder).nonEmpty && !owed(pending, holder)
    (for
      card <- picked(pending, holder) if complete
      from <- siteOf(state, card)
      target <- PowerAnswers.last(pending.answered, destinationId(holder))
    yield target match
      case DecisionOptionRef.Site(to) => Vector(
        BuildOps((_, _) => Right(Vector(move(card, from, to))), required = true),
        line(moved, holder, NoteArg.Card(card), NoteArg.Site(to)))
      case other => cardOf(other).flatMap(swap => siteOf(state, swap)
        .map(at => Vector(
          BuildOps((_, _) => Right(Vector(move(card, from, at),
            move(swap, at, from))), required = true),
          line(swapped, holder, NoteArg.Card(card), NoteArg.Card(swap)))))
        .getOrElse(Vector.empty)
    ).getOrElse(Vector.empty)

  private def move(card: CardId, from: SiteId, to: SiteId): CoreOperation =
    Move(Piece.Card(card), PositionedLocation(Location.Site(from)),
      PositionedLocation(Location.Site(to)))

  private def line(key: NoteKey, holder: PlayerId, first: NoteArg,
      second: NoteArg): Operation =
    Note(noteId, _ => Some(key.sourceless(NoteArg.Player(holder), first,
      second, NoteArg.Card(ReformerCard.id))))
