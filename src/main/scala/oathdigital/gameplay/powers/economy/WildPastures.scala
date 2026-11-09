package oathdigital.gameplay.powers.economy

import oathdigital.catalog.{Edifice, EdificeFace, ExecutableCatalog, Locked,
  PrintedPower}
import oathdigital.gameplay.CardRule
import oathdigital.gameplay.powerresolver.{AsksBeforeBatch, BatchAsk,
  BatchContext, Contribution, OnEdificeFace, Replacement}
import oathdigital.model._

object WildPasturesCard extends Edifice(EdificeId("E07"), Suit.Nomad):
  object intact extends EdificeFace("Wild Pastures") with Locked:
    val power = PrintedPower(PowerId("edifice.e07.intact"),
      persistent = true, cost = Cost.free,
      text = "Whenever this card's ruler would discard a [suit-nomad] " +
        "card from a power, they may instead pay [favor] to the " +
        "[suit-nomad] bank.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Blasted Pastures"):
    val power = PrintedPower(PowerId("edifice.e07.ruined"),
      persistent = true, cost = Cost.free,
      text = "**CHRONICLE (START OF THRONE):** Bury all [suit-nomad] " +
        "cards on the map unless the winner rules this card.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Wild Pastures (E07, intact), a persistent rule: "Whenever this card's
  * ruler would discard a Nomad card from a power, they may instead pay
  * favor to the Nomad bank." (catalog batch 11, slice 4)
  *
  * Its ruler is the card-level ruler (`CardRule.rulerAt`), so a Grand Mask
  * wearer rules it during their turn. Before a walker batch runs, it asks
  * that ruler about each discard in the batch that meets all of these:
  *
  *  - it is a denizen or ruined edifice discard;
  *  - its card is Nomad by its effective suit (rulings 21 and 23);
  *  - the ruler is its acting player;
  *  - its cause names a power and makes no room (rulings 20 and 22);
  *  - its card is faceup in play, or in a discard pile (ruling 25).
  *
  * The ruler is asked only while they have favor on their board, less the
  * favor their earlier answers on the same batch pay. A "pay"
  * puts a `Give` of 1 favor to the Nomad bank in the discard's place, and
  * the card stays where it is. The rest of the batch runs as built (ruling
  * 24).
  *
  * The line reads "Paid 1 favor to the Nomad bank instead of discarding
  * {card}." For a ruler who is not the walk's actor (a defender's plan), it
  * reads "{Blue} paid ...".
  *
  * Vow of Kinship is not implemented. When it lands, a Kinship holder may
  * pay out of the Nomad bank (ruling 12), so the favor check and the
  * `Give`'s source here change with it.
  *
  * The ruined face, Blasted Pastures, is a Chronicle step and is not
  * implemented.
  */
case object WildPastures extends AsksBeforeBatch, OnEdificeFace:
  val edifice: EdificeId = WildPasturesCard.id
  val side: EdificeSide = EdificeSide.Intact
  val id: PowerId = WildPasturesCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  val pay: DecisionOptionRef.Button = DecisionOptionRef.Button("pay")
  val discard: DecisionOptionRef.Button = DecisionOptionRef.Button("discard")
  /** One question per card, so two in one batch differ. */
  def decisionId(card: CardId): String =
    s"wild-pastures.${card.kind}.${card.value}"

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Paid 1 favor to the Nomad bank instead of discarding {card}." */
  val paid: NoteKey = notes.key("paid", Vector(
    NotePart.Text("Paid 1 favor to the Nomad bank instead of discarding "),
    NotePart.Arg(0), NotePart.Text(".")))
  /** "{Blue} paid 1 favor to the Nomad bank instead of discarding
    * {card}.", for a ruler who is not the walk's actor. */
  val paidOffTurn: NoteKey = notes.key("paid-off-turn", Vector(
    NotePart.Arg(0),
    NotePart.Text(" paid 1 favor to the Nomad bank instead of discarding "),
    NotePart.Arg(1), NotePart.Text(".")))

  /** The player who rules Wild Pastures, while it stands intact. */
  def ruler(ready: ReadyGame): Option[PlayerId] = for
    site <- ready.game.current.edificeSite(edifice, side)
    ruling <- CardRule.rulerAt(ready, site, edifice)
    player <- ruling match
      case SiteRuler.Player(player) => Some(player)
      case _ => None
  yield player

  def watching(ready: ReadyGame): Boolean = ruler(ready).nonEmpty

  def askBefore(batch: BatchContext, operation: CoreOperation)
      : Option[BatchAsk] = for
    ruler <- ruler(batch.ready)
    (card, from) <- discarded(operation, ruler)
    if standsWithAFace(batch.ready, card, from)
    if batch.ready.game.current.player(ruler).exists(_.board.favor -
      paid(batch.taken, ruler) >= 1)
  yield BatchAsk(question(batch.catalog, ruler, card), answer =>
    Option.when(answer == DecisionAnswer.ChooseOneAnswer(pay))(Replacement(
      Vector(Give(Piece.Favor(1), ruler, Location.PlayArea(ruler),
        Location.FavorBank(Suit.Nomad), required = true)),
      line(ruler, batch.actor, card))))

  /** The card a discard of a Nomad card by `ruler`, from a power's use
    * that makes no room, takes, and where from. */
  private def discarded(operation: CoreOperation, ruler: PlayerId)
      : Option[(CardId, Location)] = operation match
    case discard: Discard.Denizen if discard.suit == Suit.Nomad &&
        discard.by.contains(ruler) && fromAPower(discard.cause) =>
      Some(discard.card -> discard.from.location)
    case discard: Discard.RuinedEdifice if discard.suit == Suit.Nomad &&
        discard.by.contains(ruler) && fromAPower(discard.cause) =>
      Some(discard.card -> discard.from.location)
    case _ => None

  /** The favor `ruler` already gives in place of the batch's earlier
    * discards. */
  private def paid(taken: Vector[Replacement], ruler: PlayerId): Int =
    taken.flatMap(_.operations).collect {
      case Give(Piece.Favor(amount), giver, _, _, _) if giver == ruler =>
        amount
    }.sum

  private def fromAPower(cause: Option[DiscardCause]): Boolean =
    cause.exists(!_.makesRoom)

  /** Whether `card` stands faceup in play where `from` says, or lies in a
    * discard pile (ruling 25). */
  private def standsWithAFace(ready: ReadyGame, card: CardId,
      from: Location): Boolean = from match
    case Location.PlayArea(holder) => ready.game.current.player(holder)
      .exists(_.advisers.exists {
        case DenizenState(id, Orientation.FaceUp, _) => id == card
        case _ => false
      })
    case Location.Site(site) => ready.game.current.map.sites.get(site)
      .exists(_.denizens.exists {
        case DenizenState(id, Orientation.FaceUp, _) => id == card
        case EdificeState(id, EdificeSide.Ruined, _) => id == card
        case _ => false
      })
    case _: Location.RegionalDiscard => true
    case _ => false

  private def question(catalog: ExecutableCatalog, ruler: PlayerId,
      card: CardId): Decide =
    Decide(decisionId(card), ruler, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(pay, "Pay 1 favor"),
      DecisionOption.Button(discard, "Discard it")),
      heading = Some("Wild Pastures: pay 1 favor to the Nomad bank instead " +
        s"of discarding ${named(catalog, card)}?")))

  private def named(catalog: ExecutableCatalog, card: CardId): String =
    card match
      case id: DenizenId => catalog.denizen(id).fold(id.value)(_.name)
      case id: EdificeId => catalog.edifice(id).fold(id.value)(_.ruined.name)
      case other => other.value

  private def line(ruler: PlayerId, actor: PlayerId, card: CardId): LogNote =
    val source = PowerSourceRef.Card(edifice)
    if ruler == actor then paid(source, NoteArg.Card(card))
    else paidOffTurn(source, NoteArg.Player(ruler), NoteArg.Card(card))
