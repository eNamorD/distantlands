package oathdigital.gameplay.powers.action

import oathdigital.catalog.{Edifice, EdificeFace, ExecutableCatalog, Locked,
  PrintedPower}
import oathdigital.gameplay.actions.CardPlay
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

object FestivalDistrictCard extends Edifice(EdificeId("E21"), Suit.Hearth):
  object intact extends EdificeFace("Festival District") with Locked:
    val power = PrintedPower(PowerId("edifice.e21.intact"),
      persistent = false, cost = Cost.free,
      text = "**WHEN NEGOTIATING:** Players may give, receive, and swap " +
        "advisers. _Players must follow their adviser limits._")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Squalid District"):
    val power = PrintedPower(PowerId("edifice.e21.ruined"),
      persistent = false, cost = Cost(favor = 1, favorBurnt = 1),
      text = "**ACTION:** Discard any number of facedown advisers you " +
        "have. Draw that many denizens from the bottom of the world " +
        "deck as facedown advisers.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Squalid District (E21, ruined), ACTION: place 1 favor on this card and
  * burn 1, discard any number of your facedown advisers, then draw that
  * many denizens from the bottom of the world deck as facedown advisers
  * (catalog batch 9, slice 9).
  *
  * The question is a choose-many over the user's facedown adviser slots,
  * which names no facedown card. Each chosen adviser is discarded as card
  * play discards one, to the region after the pawn's; a Vision goes the
  * same way. Then as many cards are drawn from the deck's bottom into the
  * play area facedown, as Horned Mask takes a card, whatever they are: a
  * Vision drawn becomes a facedown adviser and advances the Visions Drawn
  * track, once per Vision, since the draw is no Search (owner's ruling,
  * catalog batch 9, "Rulings made while implementing"). The deck running
  * out draws fewer. With no facedown adviser nothing is asked and the cost
  * stays paid. Its line: "Discarded 2 facedown advisers and drew 2 cards
  * facedown." The intact face is a Negotiation power and is not
  * implemented.
  */
final case class SqualidDistrict private (catalog: ExecutableCatalog)
    extends PaidAction(FestivalDistrictCard.ruined.power):
  import SqualidDistrict._
  override def noteKeys: Vector[NoteKey] = SqualidDistrict.noteKeys
  override def narratedDecisions: Set[String] = Set(decisionId)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Option.when(facedown(live, player).nonEmpty)(
      Decide(decisionId, player, DecisionQuery.ChooseMany(0,
        facedown(live, player).size,
        facedown(live, player).map(held => DecisionOption.AdviserSlot(
          DecisionOptionRef.AdviserSlot(player, held.slot))),
        heading = Some(heading)))).toVector),
    BuildOps((live, pending) => swap(live, player, pending),
      required = true),
    Note(id, line(_, player)))))

  private def swap(ready: ReadyGame, player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    val chosen = PowerAnswers.many(pending, decisionId).getOrElse(Vector.empty)
    val held = facedown(ready, player).filter(adviser =>
      chosen.contains(DecisionOptionRef.AdviserSlot(player, adviser.slot)))
    for
      region <- CardPlay.discardRegion(ready, player).toRight(
        OathViolation.PawnSiteMissing(player))
      discards <- held.foldLeft[Either[OathViolation, Vector[CoreOperation]]](
        Right(Vector.empty))((acc, adviser) => acc.flatMap(done =>
          discard(adviser.card, player, region).map(done :+ _)))
    yield discards ++ drawn(ready, player, held.size)

  private def discard(card: WorldCardId, player: PlayerId, region: Region)
      : Either[OathViolation, CoreOperation] =
    val from = PositionedLocation(Location.PlayArea(player))
    card match
      case vision: VisionId =>
        Right(Discard.Vision(vision, from, region))
      case denizen: DenizenId => catalog.suitOf(denizen)
        .toRight(OathViolation.UnknownWorldCard(denizen))
        .map(suit => Discard.Denizen(denizen, from, region, suit, 0, 0,
          player, required = true))

  /** Up to `count` cards from the deck's bottom, bottom first, each into
    * the play area facedown; each Vision among them advances the Visions
    * Drawn track. Each card is at the bottom when its turn comes, which is
    * the end the move names. */
  private def drawn(ready: ReadyGame, player: PlayerId, count: Int)
      : Vector[CoreOperation] =
    ready.game.current.commonCards.worldDeck.reverse.take(count)
      .flatMap(card => Move(Piece.Card(card),
        PositionedLocation(Location.Deck(CardDeck.World),
          StackPosition.Bottom),
        PositionedLocation(Location.PlayArea(player)),
        resultingOrientation = Some(Orientation.FaceDown),
        by = Some(player)) +:
        Option.when(card.isInstanceOf[VisionId])(AdvanceVisionsDrawn)
          .toVector)

object SqualidDistrict:
  val id: PowerId = FestivalDistrictCard.ruined.power.id
  val decisionId: String = "power.squalid-district.advisers"
  val heading: String = "Squalid District: discard which facedown advisers?"
  private val notes = NoteSet(id)
  def noteKeys: Vector[NoteKey] = notes.declared
  /** "Discarded {2} facedown advisers and drew {2} cards facedown." */
  val swapped: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Discarded "), NotePart.Arg(0),
    NotePart.Plural(0, " facedown adviser and drew ",
      " facedown advisers and drew "), NotePart.Arg(1),
    NotePart.Plural(1, " card facedown.", " cards facedown.")))
  /** "Discarded no adviser." */
  val nothing: NoteKey = notes.key("used.none", Vector(
    NotePart.Text("Discarded no adviser.")))

  def forCatalog(catalog: ExecutableCatalog): SqualidDistrict =
    new SqualidDistrict(catalog)

  /** A facedown adviser of the user: its slot and its card. */
  final case class Facedown(slot: Int, card: WorldCardId)

  /** The user's facedown advisers, in adviser order. */
  def facedown(ready: ReadyGame, player: PlayerId): Vector[Facedown] =
    ready.game.current.players.find(_.player == player).toVector
      .flatMap(_.advisers.zipWithIndex).collect {
        case (DenizenState(card, Orientation.FaceDown, _), slot) =>
          Facedown(slot, card)
        case (VisionState(card, Orientation.FaceDown), slot) =>
          Facedown(slot, card)
      }

  /** The line, read from the step: what left the advisers and what the
    * deck gave back. */
  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    val card = PowerSourceRef.Card(FestivalDistrictCard.id)
    states.previous.map { (before, after) =>
      val chosen = PowerAnswers.many(PendingTree(Vector.empty,
        states.answered), decisionId).fold(0)(_.size)
      if chosen == 0 then nothing(card)
      else
        val deckBefore = before.game.current.commonCards.worldDeck.size
        val drawnCount = deckBefore - after.game.current.commonCards
          .worldDeck.size
        swapped(card, NoteArg.Number(chosen), NoteArg.Number(drawnCount))
    }
