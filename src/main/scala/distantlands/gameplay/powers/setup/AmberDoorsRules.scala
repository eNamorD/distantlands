package distantlands.gameplay.powers.setup

import distantlands.catalog.{Edifice, EdificeFace, ExecutableCatalog,
  Locked, PrintedPower}
import distantlands.gameplay.chronicle.ChronicleTasks
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferResolution, PowerCtx, Transform}
import distantlands.gameplay.powers.{LegacyDraws, PowerAnswers}
import distantlands.model._

object AmberDoorsCard extends Edifice(EdificeId("E05"), Suit.Discord):
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

/** E05, both faces (catalog batch 13, rulings 11 to 13), shaped as
  * `ForgeRule` is: "If you place your pawn here" is the placer at Setup
  * and the explorer at an Explore (`EdificeSetupSupport.pawnPlacement`).
  *
  * Setup is one walk in which several players may place at this site, so
  * there a face's decision ids name the placer, under `setup.`, which
  * Setup's log narrates. An Explore has one explorer, so there the ids are
  * fixed and each face narrates them itself. Each question sits in its own
  * `Branch` after the draw's, so a walk resumed at it selects the same
  * shape: it lists a hand or a lineage that nothing changed since the park.
  */
sealed abstract class DoorsRule extends ContributingPower:
  final val edifice: EdificeId = AmberDoorsCard.id
  protected def side: EdificeSide
  /** The face's name in its decision ids. */
  protected def slug: String

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  protected final val card: PowerSourceRef = PowerSourceRef.Card(edifice)

  /** The id of this face's `step` question: at Setup, for `placer`; at an
    * Explore, `None`. */
  protected final def decision(step: String,
      placer: Option[PlayerId]): String = placer match
    case Some(player) => s"setup.$slug.$step.${player.value}"
    case None => s"power.$slug.$step"

  private def at(ctx: PowerCtx): Option[(PlayerId, SiteId)] =
    for
      edificeSite <- ctx.state.game.current.edificeSite(edifice, side)
      placement <- EdificeSetupSupport.pawnPlacement(ctx)
      if placement._2 == edificeSite
    yield placement

  override def applicable(ctx: PowerCtx): Boolean = at(ctx).isDefined

  /** The face's steps for `actor`, asking under `placer`'s ids. */
  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation]

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

  /** Setup's pawn window orders its powers, so there the steps are offered
    * to the placer (catalog batch 13, ruling 4). An Explore folds them in.
    */
  final def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      at(ctx).map((actor, _) => ResolutionOffer(
        EdificeSetupSupport.option(edifice),
        steps(ctx.catalog, actor, Some(actor)))))),
    PowerWindow.WhenExplored -> Vector(Transform((ctx, ops) =>
      at(ctx).fold(ops)((actor, _) =>
        ops ++ steps(ctx.catalog, actor, None)))))

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
        case dormant => Decide(decisionId, actor, DecisionQuery.ChooseOne(
          dormant.map(legacy =>
            DecisionOption.Legacy(DecisionOptionRef.Legacy(legacy))),
          heading = Some("Lost Doors: discard a dormant legacy"),
          confirmVerb = Some("Discard"))) +:
          discardOf(actor, pending => PowerAnswers.one(pending, decisionId)
            .collect { case DecisionOptionRef.Legacy(legacy) => legacy }))

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
