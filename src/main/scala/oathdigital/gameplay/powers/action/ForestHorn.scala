package oathdigital.gameplay.powers.action

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.gameplay.powers.setup.ForestHornCard
import oathdigital.gameplay.powers.whenplayed.RegionDiscard
import oathdigital.model._

/** Forest Horn (E30, intact), ACTION: place 2 secrets on this card, choose
  * a region, then discard every non-Beast denizen at that region's sites
  * (catalog batch 9, slice 8).
  *
  * The region is asked as three buttons (owner question 8). The discard is
  * [[RegionDiscard]]'s with every suit but Beast kept, so faceup denizens
  * and ruined edifices go, as far as the discard rules permit, and intact
  * edifices stay locked. The Horn, a Beast card, never discards itself. Its
  * line covers the generic discard lines: "Discarded {cards}." or "Nothing
  * was discarded."
  */
case object ForestHorn extends PaidAction(ForestHornCard.intact.power):
  val decisionId: String = "power.forest-horn.region"
  val heading: String = "Forest Horn: discard the non-Beast denizens of " +
    "which region?"

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Discarded {cards}." */
  val discarded: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Discarded "), NotePart.Arg(0), NotePart.Text(".")))
  /** "Nothing was discarded." */
  val none: NoteKey = notes.key("used.none", Vector(
    NotePart.Text("Nothing was discarded.")))

  /** The line for the cards of `found` that left the sites. */
  private def said(found: Vector[CardId])(states: NoteStates)
      : Option[LogNote] =
    val card = PowerSourceRef.Card(ForestHornCard.id)
    val left = RegionDiscard.gone(found, states.now)
    Some(if left.nonEmpty then discarded(card, NoteArg.Cards(left))
      else none(card))

  /** The option that names `region`. */
  def button(region: Region): DecisionOptionRef.Button =
    DecisionOptionRef.Button(s"region.${region.toString.toLowerCase}")

  private def chosen(pending: PendingTree): Option[Region] =
    PowerAnswers.one(pending, decisionId).flatMap(ref =>
      Region.all.find(button(_) == ref))

  override def narratedDecisions: Set[String] = Set(decisionId)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Decide(decisionId, player, DecisionQuery.ChooseOne(
      Region.all.map(region => DecisionOption.Button(button(region),
        region.toString)), heading = Some(heading))),
    Branch((live, pending) => chosen(pending).toVector.flatMap { region =>
      val discard = new RegionDiscard(_ != Suit.Beast, (_, _) => Some(region))
      val found = discard.targets(catalog, live, player)
      Vector[Operation](
        BuildOps((ready, _) => discard.discards(catalog, ready, player)),
        Note(id, said(found), covers = true))
    }))))
