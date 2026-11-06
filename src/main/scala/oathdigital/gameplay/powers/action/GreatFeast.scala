package oathdigital.gameplay.powers.action

import oathdigital.catalog.{Denizen, ExecutableCatalog, PrintedPower}
import oathdigital.gameplay.NoteSupport
import oathdigital.gameplay.powers.PlayerFacts
import oathdigital.model._

object GreatFeastCard extends Denizen(DenizenId("257"), "Great Feast", Suit.Order):
  val power = PrintedPower(PowerId("denizen.great-feast"),
    persistent = false, cost = Cost(favor = 1),
    text = "**ACTION:** Discard a [suit-beast] card at your site to " +
      "gain 3 Supply.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Great Feast (card 257), ACTION: place 1 favor on this card, then discard
  * a Beast card at your site to gain 3 Supply.
  *
  * The player chooses a faceup Beast denizen or ruined Beast edifice at
  * their site ([[SiteCards]]), which gets the standard discard. The Supply
  * runs in the same required batch, capped to the room on the track: a
  * required batch refuses an operation it cannot make in full. The choice is
  * narrated: the line names the card, and covers the Discarded line. With
  * nothing to discard, the cost stays paid and the line names the site.
  */
case object GreatFeast extends PaidAction(GreatFeastCard.power):
  val decisionId: String = "power.great-feast.card"
  val Gained: Int = 3
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "{Red} discarded {card} and gained {n} Supply." */
  val feasted: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" discarded "), NotePart.Arg(1),
    NotePart.Text(" and gained "), NotePart.Arg(2), NotePart.Text(".")))
  /** "{site} held no Beast card." */
  val bare: NoteKey = notes.key("used.none", Vector(NotePart.Arg(0),
    NotePart.Text(" held no Beast card.")))
  /** "No Beast card at {site} could be discarded." */
  val kept: NoteKey = notes.key("used.kept", Vector(
    NotePart.Text("No Beast card at "), NotePart.Arg(0),
    NotePart.Text(" could be discarded.")))

  private val cards = new SiteCards(Set(Suit.Beast))
  override def narratedDecisions: Set[String] = Set(decisionId)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => cards.ask(catalog, live, player, decisionId,
      "Great Feast: discard a Beast card at your site")),
    BuildOps((live, pending) => feast(catalog, live, player, pending),
      required = true),
    Note(this.id, feastNote(catalog, _, player, source), covers = true))))

  private def feast(catalog: ExecutableCatalog,
      ready: ReadyGame, player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    cards.chosen(catalog, ready, player, pending, decisionId) match
      case None => Right(Vector.empty)
      case Some((site, card, suit)) => meal(ready, player, site, card, suit)

  /** The discard, then the Supply the track has room for. */
  private def meal(ready: ReadyGame, player: PlayerId, site: SiteId,
      card: SiteDenizenState, suit: Suit)
      : Either[OathViolation, Vector[CoreOperation]] = for
    discard <- cards.discard(ready, site, card, suit, player, required = true)
    held <- PlayerFacts.player(ready, player)
  yield
    val room = Gained.min(SupplyTrack.Maximum - held.board.supply.supply)
    Vector[CoreOperation](discard) ++
      Option.when(room > 0)(GainSupply(player, room))

  private def feastNote(catalog: ExecutableCatalog,
      states: NoteStates, player: PlayerId,
      source: DecisionOptionRef): Option[LogNote] =
    PowerSourceRef.of(source).flatMap(card =>
      NoteSupport.answer(states, decisionId)
        .flatMap(DecisionOptionRef.cardOf) match
        case Some(chosen) => Some(feasted(card, NoteArg.Player(player),
          NoteArg.Card(chosen), NoteArg.Amount(
            states.previous.fold(0)(NoteSupport.supply(_, player)),
            NoteUnit.Supply)))
        case None => states.now.game.current.pawnSite(player).map(site =>
          // A card was there, but a refusal kept it: it was not discarded.
          if cards.at(catalog, states.now, site).nonEmpty then
            kept(card, NoteArg.Site(site))
          else bare(card, NoteArg.Site(site))))
