package oathdigital.gameplay.powers.action

import oathdigital.catalog.{Edifice, EdificeFace, ExecutableCatalog, Locked,
  PrintedPower}
import oathdigital.gameplay.{NoteSupport, PowerAccess}
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.gameplay.powers.setup.EdificeSetupSupport
import oathdigital.model._

object GiantsPathCard extends Edifice(EdificeId("E09"), Suit.Nomad):
  object intact extends EdificeFace("Giant's Path") with Locked:
    val power = PrintedPower(PowerId("edifice.e09.intact"),
      persistent = false, cost = Cost(secret = 1),
      text = "**ACTION:** Move this card to another site, then place your " +
        "pawn there.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Giant's Rampage"):
    val power = PrintedPower(PowerId("edifice.e09.ruined"),
      persistent = false, cost = Cost(favor = 1, secret = 2),
      text = "**ACTION:** Bury a denizen at any site _(even here)_. If " +
        "you do, move Giant's Rampage to that site and place your " +
        "pawn there.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Giant's Rampage (E09, ruined), ACTION: place 1 favor and 2 secrets on
  * this card, bury a denizen at any site, then move the Rampage into the
  * freed place and place the user's pawn there (catalog batch 9, slice 8).
  *
  * The question lists every faceup denizen at every site, the Rampage's
  * own included; an edifice is not a denizen it may bury. A bury ignores
  * locks and returns the card's favor and secrets. The Rampage, a ruined
  * edifice, then moves as a card moves, unless the denizen stood at its
  * own site, and the pawn is placed with a plain `Move`, not a Travel. The
  * batch is required, so a move a restriction refuses (Ancient Bloodline)
  * hides that denizen instead of burying it for nothing. With no denizen
  * anywhere the question shows Continue and the cost stays paid.
  *
  * Its line covers the bury and the moves: "Buried {card} at {site};
  * Giant's Rampage and {Blue} now stand there." The intact face, Giant's
  * Path, is locked and cannot move itself (catalog batch 9, "Out of
  * scope").
  */
final case class GiantsRampage private (catalog: ExecutableCatalog)
    extends PaidAction(GiantsPathCard.ruined.power):
  import GiantsRampage._
  override def noteKeys: Vector[NoteKey] = GiantsRampage.noteKeys
  override def narratedDecisions: Set[String] = Set(decisionId)

  def build(ready: ReadyGame, player: PlayerId, source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseOne(denizens(live).map((_, card) =>
        DecisionOption.Denizen(DecisionOptionRef.Denizen(card.id))),
        heading = Some(heading)), passWhenEmpty = true))),
    BuildOps((live, pending) => rampage(live, player, pending),
      required = true),
    Note(id, line(_, player), covers = true))))

  /** The bury, the Rampage's move when the site is another, and the pawn's
    * placement. No answer means Continue was pressed. */
  private def rampage(ready: ReadyGame, player: PlayerId,
      pending: PendingTree): Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.one(pending, decisionId)
      .fold[Either[OathViolation, Vector[CoreOperation]]](Right(Vector.empty))(
        bury(ready, player, _))

  private def bury(ready: ReadyGame, player: PlayerId,
      ref: DecisionOptionRef): Either[OathViolation, Vector[CoreOperation]] =
    for
      found <- denizens(ready).find((_, card) =>
        DecisionOptionRef.Denizen(card.id) == ref).toRight(
        OathViolation.InvalidEventOrder(
          s"${ref.wireId} is not a denizen at a site"))
      (site, card) = found
      suit <- catalog.suitOf(card.id).toRight(
        OathViolation.UnknownWorldCard(card.id))
      here <- rampageSite(ready).toRight(OathViolation.InvalidEventOrder(
        "Giant's Rampage is not on the map"))
      placed <- PawnMoves.relocate(ready, player, site)
    yield
      val from = PositionedLocation(Location.Site(site))
      val buried = Bury.standard(BuryableCard.Denizen(card.id), from,
        Some(suit), card.tokens.favor, card.tokens.secrets, player)
      val moved = Option.when(here != site)(Move(Piece.Card(edifice),
        PositionedLocation(Location.Site(here)), from, by = Some(player)))
      buried ++ moved ++ placed

object GiantsRampage:
  val edifice: EdificeId = GiantsPathCard.id
  val id: PowerId = GiantsPathCard.ruined.power.id
  val decisionId: String = "power.giants-rampage.denizen"
  val heading: String = "Giant's Rampage: bury which denizen?"
  private val notes = NoteSet(id)
  def noteKeys: Vector[NoteKey] = notes.declared
  /** "Buried {card} at {site}; Giant's Rampage and {Blue} now stand
    * there." */
  val rampaged: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Buried "), NotePart.Arg(0), NotePart.Text(" at "),
    NotePart.Arg(1), NotePart.Text("; Giant's Rampage and "), NotePart.Arg(2),
    NotePart.Text(" now stand there.")))
  /** "No denizen could be buried." */
  val nothing: NoteKey = notes.key("used.none", Vector(
    NotePart.Text("No denizen could be buried.")))

  def forCatalog(catalog: ExecutableCatalog): GiantsRampage =
    new GiantsRampage(catalog)

  def rampageSite(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, edifice, EdificeSide.Ruined)

  /** Every faceup denizen at a site in play, with its site, in map order. */
  def denizens(ready: ReadyGame): Vector[(SiteId, DenizenState)] =
    val map = ready.game.current.map
    map.inPlay.flatMap(site => map.sites.get(site).toVector
      .flatMap(_.denizens).collect {
        case card: DenizenState if card.orientation == Orientation.FaceUp =>
          site -> card })

  /** The line, read from the answer and where the pawn now stands. */
  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    val card = PowerSourceRef.Card(edifice)
    NoteSupport.answer(states, decisionId) match
      case Some(DecisionOptionRef.Denizen(buried)) =>
        PowerAccess.pawnSite(states.now, player).map(site =>
          rampaged(card, NoteArg.Card(buried), NoteArg.Site(site),
            NoteArg.Player(player)))
      case _ => Some(nothing(card))
