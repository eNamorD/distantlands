package oathdigital.gameplay.powers.action

import oathdigital.catalog.{Edifice, EdificeFace, ExecutableCatalog, Locked,
  PrintedPower}
import oathdigital.gameplay.actions.PlacementLimit
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Transform}
import oathdigital.model._

object TempleOfAncestorsCard extends Edifice(EdificeId("E10"), Suit.Nomad):
  object intact extends EdificeFace("Temple of Ancestors") with Locked:
    val power = PrintedPower(PowerId("edifice.e10.intact"),
      persistent = false, cost = Cost(favor = 1),
      text = "**ACTION:** Draw a card from the bottom of the world deck " +
        "and play it to your advisers or discard it, as if you " +
        "searched.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Ruined Temple"):
    val power = PrintedPower(PowerId("edifice.e10.ruined"),
      persistent = false, cost = Cost.free,
      text = "**SETUP / WHEN EXPLORED:** If you place your pawn here, you " +
        "may burn [secret-burnt]. If you do and then win the game, " +
        "score double the legacy points and take rule of all bandit " +
        "sites.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Temple of Ancestors (E10, intact), ACTION: place 1 favor on this card,
  * draw the bottom card of the world deck and play it to your advisers or
  * discard it, as if you searched (catalog batch 9, slice 9).
  *
  * The card is taken from the deck's bottom into the user's temporary hand,
  * the Visions Drawn track advancing when it is a Vision, as a Search's
  * draw does. It is then placed through the card-play subtree a Search's
  * kept card goes through, as Oracle's Vision is, so the card-play windows
  * fold: a WHEN PLAYED fires, and the restrictions on a faceup Vision
  * apply. The power's own `Transform` at `SearchPlayAdviser`, applicable
  * only inside this power's walk, limits the placements to the advisers
  * (faceup, facedown, or the revealed Vision slot for a Vision) and the
  * discard: a site slot shows "Temple of Ancestors: the card goes to your
  * advisers" (owner question 6). An empty deck draws nothing and asks
  * nothing. The ruined face is a Chronicle step and is not implemented.
  */
final case class TempleOfAncestors private (catalog: ExecutableCatalog)
    extends PaidAction(TempleOfAncestorsCard.intact.power)
    with ContributingPower:
  import TempleOfAncestors._
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  override def noteKeys: Vector[NoteKey] = TempleOfAncestors.noteKeys

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    BuildOps((live, _) => Right(bottom(live).toVector.flatMap(card =>
      Take(Piece.Card(card), player, Location.Deck(CardDeck.World),
        Location.Hand(player), sourcePosition = StackPosition.Bottom) +:
        Option.when(card.isInstanceOf[VisionId])(AdvanceVisionsDrawn)
          .toVector))),
    Note(this.id, states => PowerSourceRef.of(source).map(card =>
      held(states.now, player).fold(emptyDeck(card))(drawn =>
        drew(card, NoteArg.Player(player), NoteArg.Card(drawn))))),
    Branch((live, pending) => held(live, player)
      .orElse(CardPlayProcedure.placedCard(pending)).toVector.map(card =>
        CardPlayProcedure.unchecked(catalog, live, player, card,
          CardPlayProcedure.Origin.TemporaryHand))))))

  /** Inside this power's walk only, the play goes to the advisers or the
    * discard. */
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SearchPlayAdviser -> Vector(Transform((ctx, children) =>
      ctx.operation match
        case tree: CardPlayProcedure.PlacementTree =>
          tree.adjust(children)(_.limitPlacements(adviserRule))
        case _ => children)))

  override def applicable(ctx: PowerCtx): Boolean =
    ctx.procedure.contains(ActionRef.UsePower(id))

object TempleOfAncestors:
  val id: PowerId = TempleOfAncestorsCard.intact.power.id
  val toAdvisers: String = "Temple of Ancestors: the card goes to your " +
    "advisers"
  /** Only an adviser play, or the discard, stays open. */
  val adviserRule: PlacementLimit = PlacementLimit(
    OptionReason.Written(toAdvisers), {
      case SearchPlacement.Site(_, _) => false
      case _ => true
    })
  private val notes = NoteSet(id)
  def noteKeys: Vector[NoteKey] = notes.declared
  /** "{Blue} drew {card} from the bottom of the world deck." */
  val drew: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" drew "), NotePart.Arg(1),
    NotePart.Text(" from the bottom of the world deck.")))
  /** "The world deck was empty." */
  val emptyDeck: NoteKey = notes.key("used.none",
    Vector(NotePart.Text("The world deck was empty.")))

  def forCatalog(catalog: ExecutableCatalog): TempleOfAncestors =
    new TempleOfAncestors(catalog)

  /** The world deck's bottom card, stored last. */
  private def bottom(ready: ReadyGame): Option[WorldCardId] =
    ready.game.current.commonCards.worldDeck.lastOption

  /** The card in the player's temporary hand, until it is played. */
  private def held(ready: ReadyGame, player: PlayerId): Option[WorldCardId] =
    ready.game.current.temporaryHands.getOrElse(player, Vector.empty)
      .headOption
