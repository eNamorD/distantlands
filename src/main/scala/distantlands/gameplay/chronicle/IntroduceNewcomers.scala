package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.model._

/** Introduce Newcomers, the Beacon's first step (CR p. 39). The Beacon task
  * holder chooses three different suits, for three draws, two and one.
  * Each chosen suit's boxed denizens join its live reserve. If any chosen
  * suit then has too few, every older Dispossessed card returns to its
  * suit's reserve first; the newly Dispossessed stay. Each reserve is
  * shuffled and the holder draws from its top into their temporary hand. A
  * suit still short draws what remains (Chronicle ruling 15). */
object IntroduceNewcomers:
  val draws: Vector[Int] = Vector(3, 2, 1)
  def suitDecisionId(count: Int): String = s"chronicle.beacon.suit.$count"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon)
  private val notes = NoteSet(noteId)
  val choseSuits: NoteKey = notes.key("chose-suits", Vector(
    NotePart.Arg(0), NotePart.Text(" chose "), NotePart.Arg(1),
    NotePart.Text(", "), NotePart.Arg(2), NotePart.Text(" and "),
    NotePart.Arg(3)))
  val returned: NoteKey = notes.key("returned-dispossessed", Vector(
    NotePart.Arg(0), NotePart.Text(" returned to the box")))
  def noteKeys: Vector[NoteKey] = notes.declared

  def steps(catalog: ExecutableCatalog): Vector[Operation] =
    draws.indices.toVector.map(at => Branch((state, pending) =>
      ChronicleTasks.forHolder(state, ChronicleTask.Beacon)(holder => Vector(
        Decide(suitDecisionId(draws(at)), holder, DecisionQuery.ChooseOne(
          // Only the suits chosen before this one are gone: on a resume
          // every answer is already recorded, this one's included.
          Suit.all.filterNot(chosen(pending).take(at).contains).map(suit =>
            DecisionOption.FavorBank(DecisionOptionRef.FavorBank(suit))),
          heading = Some(s"Choose the suit to draw ${draws(at)} from"))))))) ++
    Vector(
      Branch((state, pending) =>
        ChronicleTasks.forHolder(state, ChronicleTask.Beacon)(holder =>
          Vector(Note(noteId, _ => chosen(pending) match
            case Vector(first, second, third) => Some(choseSuits.sourceless(
              NoteArg.Player(holder), NoteArg.SuitName(first),
              NoteArg.SuitName(second), NoteArg.SuitName(third)))
            case _ => None)))),
      BuildOps((state, pending) => fromBox(catalog, state, chosen(pending)),
        required = true),
      Note(noteId, states => Option.when(
        short(states.now, chosen(states.answered)) &&
          returning(states.now).nonEmpty)(
        returned.sourceless(NoteArg.Cards(returning(states.now))))),
      BuildOps((state, pending) => Right(returnIfShort(catalog, state,
        chosen(pending))), required = true),
      // One shuffle leaf per suit; the walker fills each order.
      Branch((_, pending) => chosen(pending).map(suit =>
        Shuffle(ShufflePile.SuitedReserve(suit)))),
      BuildOps((state, pending) =>
        ChronicleTasks.holder(state, ChronicleTask.Beacon).map(holder =>
          draw(state, holder, chosen(pending))), required = true))

  /** The suits answered so far, in draw order. */
  private def chosen(pending: PendingTree): Vector[Suit] =
    chosen(pending.answered)

  private def chosen(answered: Vector[Answered]): Vector[Suit] =
    draws.flatMap(count => answered.collectFirst {
      case Answered(id, DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.FavorBank(suit)), _)
          if id == suitDecisionId(count) => suit
    })

  /** A broken card index refuses the walk rather than reading as an empty
    * table, which would box every card again. */
  private def fromBox(catalog: ExecutableCatalog, state: ReadyGame,
      suits: Vector[Suit]): Either[OathViolation, Vector[CoreOperation]] =
    state.game.cardIndex.left.map(problems => OathViolation.InvalidEventOrder(
      s"the card index is broken: $problems")).map(index =>
      suits.flatMap(suit =>
        val boxed = catalog.denizens.filter(_.suit == suit)
          .map(denizen => DenizenId(denizen.id.value))
          .filterNot(index.ids.contains)
        Option.when(boxed.nonEmpty)(SuitedReserveFromBox(suit, boxed))))

  /** The older Dispossessed a short suit sends back: its denizens only. */
  private def returning(state: ReadyGame): Vector[DenizenId] =
    state.game.campaign.dispossessed.collect { case id: DenizenId => id }

  /** Whether a chosen suit's reserve holds fewer cards than it draws. */
  private def short(state: ReadyGame, suits: Vector[Suit]): Boolean =
    suits.zip(draws).exists((suit, count) =>
      state.game.campaign.suitedReserves.getOrElse(suit, Vector.empty).size <
        count)

  private def returnIfShort(catalog: ExecutableCatalog, state: ReadyGame,
      suits: Vector[Suit]): Vector[CoreOperation] =
    if !short(state, suits) then Vector.empty
    else returning(state).flatMap(card => catalog.denizen(card).map(denizen =>
      Move(Piece.Card(card), PositionedLocation(Location.Dispossessed),
        PositionedLocation(Location.SuitedReserve(denizen.suit)))))

  private def draw(state: ReadyGame, holder: PlayerId, suits: Vector[Suit])
      : Vector[CoreOperation] =
    suits.zip(draws).flatMap((suit, count) =>
      state.game.campaign.suitedReserves.getOrElse(suit, Vector.empty)
        .take(count).map(card => Move(Piece.Card(card),
          PositionedLocation(Location.SuitedReserve(suit)),
          PositionedLocation(Location.Hand(holder)))))
