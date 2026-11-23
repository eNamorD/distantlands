package distantlands.gameplay.powers.setup

import distantlands.catalog.{Edifice, EdificeFace, ExecutableCatalog,
  Locked, PrintedPower}
import distantlands.gameplay.chronicle.ChronicleTasks
import distantlands.gameplay.chronicle.legacies.{IronHand, IronHandCard}
import distantlands.gameplay.powers.{LegacyDraws, PowerAnswers}
import distantlands.gameplay.powers.legacies.LegacyActivation
import distantlands.model._

object AmberDoorsCard extends Edifice(EdificeId("E05"), Suit.Hearth):
  object intact extends EdificeFace("Amber Doors") with Locked:
    val power = PrintedPower(PowerId("edifice.e05.intact"),
      persistent = false, cost = Cost.free,
      text = "SETUP / **WHEN EXPLORED:** If you place your pawn here, you " +
        "may discard all of your legacies (even dormant) to draw 2 " +
        "dormant legacies and activate one.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Lost Doors"):
    val power = PrintedPower(PowerId("edifice.e05.ruined"),
      persistent = false, cost = Cost.free,
      text = "SETUP / **WHEN EXPLORED:** If you place your pawn here, " +
        "draw a dormant legacy, then discard one you hold.")
    val powers: Vector[PrintedPower] = Vector(power)

/** E05, both faces (catalog batch 13, rulings 11 to 13), a
  * `PlacedPawnEdificeRule`. Each question sits in its own `Branch` after
  * the draw's, so a walk resumed at it selects the same shape: it lists a
  * hand or a lineage that nothing changed since the park.
  */
sealed abstract class DoorsRule extends PlacedPawnEdificeRule:
  final val edifice: EdificeId = AmberDoorsCard.id

  /** `actor`'s lineage in `ready`. */
  protected final def lineageOf(ready: ReadyGame, actor: PlayerId)
      : Either[OathViolation, LineageId] =
    ChronicleTasks.lineageOf(ready, actor).toRight(
      OathViolation.InvalidEventOrder(s"${actor.value} has no lineage"))

  /** The legacies `actor`'s lineage holds in `ready`, in lineage order. */
  protected final def held(ready: ReadyGame, actor: PlayerId)
      : Vector[LegacyState] =
    ChronicleTasks.lineageOf(ready, actor).toVector.flatMap(lineage =>
      ready.game.campaign.lineages.get(lineage).toVector.flatMap(_.legacies))

/** Lost Doors (E05 ruined): the player draws 1 legacy, dormant, then
  * discards one dormant legacy they hold, the new one included, to the
  * bottom of the legacy deck (ruling 13). Active legacies are never
  * offered. With one dormant legacy it is discarded unasked; with none
  * nothing is discarded. An empty deck draws nothing and says so, and the
  * discard still follows. Matchmaker plays no part. Lines: "p2 drew a
  * legacy." and "p2 discarded a dormant legacy.", never naming either.
  */
case object LostDoors extends DoorsRule:
  val id: PowerId = AmberDoorsCard.ruined.power.id
  protected def side: EdificeSide = EdificeSide.Ruined
  protected def slug: String = "lost-doors"

  def discardDecisionId(placer: Option[PlayerId]): String =
    decision("discard", placer)

  private val notes = NoteSet(id)
  /** "p2 drew a legacy." */
  val drew: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" drew a legacy.")))
  /** "p2 discarded a dormant legacy." */
  val discarded: NoteKey = notes.key("discarded", Vector(NotePart.Arg(0),
    NotePart.Text(" discarded a dormant legacy.")))
  notes.borrow(LegacyDraws.empty)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** Its discard line tells the answer, and names no legacy. */
  override def narratedDecisions: Set[String] = Set(discardDecisionId(None))

  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation] =
    Vector(draw(actor), discard(actor, discardDecisionId(placer)))

  /** The top legacy, through the hand, onto the lineage, dormant. */
  private def draw(actor: PlayerId): Operation = Branch((live, _) =>
    LegacyDraws.top(live, 1) match
      case Vector() => Vector(Note(id, _ => Some(LegacyDraws.empty(card))))
      case taken => Vector(LegacyDraws.draw(actor, taken),
        BuildOps((ready, _) => lineageOf(ready, actor).map(lineage =>
          taken.map(legacy => Move(Piece.Card(legacy),
            PositionedLocation(Location.Hand(actor)),
            PositionedLocation(Location.Legacies(lineage))))),
          required = true),
        Note(id, _ => Some(drew(card, NoteArg.Player(actor))))))

  private def discard(actor: PlayerId, decisionId: String): Operation =
    Branch((live, _) =>
      held(live, actor).collect { case LegacyState(legacy, false) =>
        legacy } match
        case Vector() => Vector.empty
        case Vector(only) => discardOf(actor, _ => Some(only))
        case dormant => LegacyDraws.pick(decisionId, actor, dormant,
          LegacyDraws.Pick.discard,
          heading = "Lost Doors: discard a dormant legacy",
          confirmLabel = "Discard Legacy") +:
          discardOf(actor, pending => LegacyDraws.picked(pending.answered,
            decisionId, LegacyDraws.Pick.discard)))

  /** The legacy `chosen` names, from the lineage to the deck's bottom, and
    * the line. */
  private def discardOf(actor: PlayerId,
      chosen: PendingTree => Option[LegacyId]): Vector[Operation] = Vector(
    BuildOps((ready, pending) => for
      legacy <- chosen(pending).toRight(PowerAnswers.missing(
        "a Lost Doors discard"))
      lineage <- lineageOf(ready, actor)
    yield Vector(Move(Piece.Card(legacy),
      PositionedLocation(Location.Legacies(lineage)),
      PositionedLocation.legacyDeckBottom)), required = true),
    Note(id, _ => Some(discarded(card, NoteArg.Player(actor)))))

/** Amber Doors (E05 intact): "you may" (ruling 12). On yes, every legacy
  * the player holds, active and dormant, goes to the bottom of the legacy
  * deck; then they draw 2 into their hand and choose one to activate, and
  * the other joins their lineage dormant. A deck of one is activated
  * unasked; an empty deck draws and activates nothing, and says so. The
  * activation is The Mouth's (`LegacyActivation`): the legacy's foundation
  * is altered, and an Imperial's Iron Hand is discarded at once. It needs
  * no goal, and no cap: the player holds only what they drew. Matchmaker
  * plays no part (ruling 10; its printed text). A declined Doors writes no
  * line. Lines: "p2 discarded their legacies.", "p2 activated Seer.", or
  * "p2 activated Iron Hand." in words when it is discarded at once (R32 of
  * the Legacy in-play powers), then Iron Hand's own line.
  */
case object AmberDoors extends DoorsRule:
  val id: PowerId = AmberDoorsCard.intact.power.id
  protected def side: EdificeSide = EdificeSide.Intact
  protected def slug: String = "amber-doors"
  /** How many legacies the Doors draw. */
  val Drawn: Int = 2

  def useDecisionId(placer: Option[PlayerId]): String =
    decision("discard", placer)
  def activateDecisionId(placer: Option[PlayerId]): String =
    decision("activate", placer)
  val keep: DecisionOptionRef.Button = DecisionOptionRef.Button("keep")
  val discard: DecisionOptionRef.Button = DecisionOptionRef.Button("discard")

  private val notes = NoteSet(id)
  /** "p2 discarded their legacies." */
  val discardedAll: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Arg(0), NotePart.Text(" discarded their legacies.")))
  /** "p2 activated Seer." */
  val activated: NoteKey = notes.key("activated", Vector(NotePart.Arg(0),
    NotePart.Text(" activated "), NotePart.Arg(1), NotePart.Text(".")))
  /** "p2 activated Iron Hand.", for an Iron Hand the same batch discards:
    * it never shows faceup, so a card reference would read "a Legacy". */
  val activatedIronHand: NoteKey = notes.key("activated-iron-hand", Vector(
    NotePart.Arg(0), NotePart.Text(s" activated ${IronHandCard.name}.")))
  notes.include(LegacyActivation.notes)
  notes.borrow(LegacyDraws.empty)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** The discard and activation lines tell both answers. */
  override def narratedDecisions: Set[String] =
    Set(useDecisionId(None), activateDecisionId(None))

  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation] =
    val use = useDecisionId(placer)
    val activate = activateDecisionId(placer)
    def used(pending: PendingTree): Boolean =
      PowerAnswers.one(pending, use).contains(discard)
    Vector(
      Decide(use, actor, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(keep, "Keep your legacies"),
        DecisionOption.Button(discard, "Discard them and draw 2")),
        heading = Some("Amber Doors: you may discard all your legacies " +
          "to draw 2 and activate one"))),
      Branch((live, pending) =>
        if !used(pending) then Vector.empty
        else discardAll(actor, held(live, actor).map(_.id))),
      Branch((live, pending) =>
        if !used(pending) then Vector.empty
        else LegacyDraws.top(live, Drawn) match
          case Vector() => Vector(Note(id, _ => Some(LegacyDraws.empty(card))))
          case taken => Vector(LegacyDraws.draw(actor, taken))),
      Branch((live, pending) =>
        if !used(pending) then Vector.empty
        else LegacyDraws.hand(live, actor) match
          case Vector() => Vector.empty
          case Vector(only) => activation(catalog, actor, Vector(only),
            _ => Some(only))
          case drawn => LegacyDraws.pick(activate, actor, drawn,
            LegacyDraws.Pick.activate,
            heading = "Amber Doors: activate a legacy",
            confirmLabel = "Activate Legacy") +:
            activation(catalog, actor, drawn, answered =>
              LegacyDraws.picked(answered, activate,
                LegacyDraws.Pick.activate))))

  /** Every legacy `actor` holds, from the lineage to the deck's bottom, and
    * the line. */
  private def discardAll(actor: PlayerId, legacies: Vector[LegacyId])
      : Vector[Operation] =
    if legacies.isEmpty then Vector.empty
    else Vector(
      BuildOps((ready, _) => lineageOf(ready, actor).map(lineage =>
        legacies.map(legacy => Move(Piece.Card(legacy),
          PositionedLocation(Location.Legacies(lineage)),
          PositionedLocation.legacyDeckBottom))), required = true),
      Note(id, _ => Some(discardedAll(card, NoteArg.Player(actor)))))

  /** The legacy `chosen` names from `drawn` activated from `actor`'s hand,
    * the others onto the lineage dormant, then the lines. */
  private def activation(catalog: ExecutableCatalog, actor: PlayerId,
      drawn: Vector[LegacyId], chosen: Vector[Answered] => Option[LegacyId])
      : Vector[Operation] = Vector(
    BuildOps((ready, pending) => for
      legacy <- chosen(pending.answered).filter(drawn.contains)
        .toRight(PowerAnswers.missing("an Amber Doors activation"))
      lineage <- lineageOf(ready, actor)
    yield LegacyActivation.operations(catalog, ready, actor, lineage, legacy,
      Location.Hand(actor)) ++ drawn.filterNot(_ == legacy).map(other =>
        Move(Piece.Card(other), PositionedLocation(Location.Hand(actor)),
          PositionedLocation(Location.Legacies(lineage)))),
      required = true),
    Note(id, states => for
      (before, _) <- states.previous
      legacy <- chosen(states.answered)
    yield
      if LegacyActivation.discardsIronHand(before, actor, legacy) then
        activatedIronHand(card, NoteArg.Player(actor))
      else activated(card, NoteArg.Player(actor), NoteArg.Card(legacy))),
    Note(id, states =>
      LegacyActivation.alterationNote(states, chosen(states.answered))),
    Note(IronHand.id, states => LegacyActivation.ironHandNote(states, actor,
      chosen(states.answered))))
