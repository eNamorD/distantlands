package distantlands.gameplay.powers.action

import distantlands.catalog.{ExecutableCatalog, PrintedPower, Relic}
import distantlands.gameplay.powers.{LegacyDraws, PlayerFacts, PowerAnswers}
import distantlands.model._

object AncientWritCard extends Relic(RelicId("R22"), "Ancient Writ", value = 81, defense = 1):
  val power = PrintedPower(PowerId("relic.ancient-writ"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** Bury this to draw 3 legacy cards, keep 1 " +
      "facedown, and discard the other 2.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Ancient Writ (relic R22), ACTION: bury this relic, draw 3 legacies, keep
  * 1 dormant and discard the others (catalog batch 13, rulings 8 to 10).
  *
  * The bury comes first and returns the relic's secrets to its holder, as
  * Magic Waterskin's does. The tree reads the relic and the owner's lineage
  * where each step is reached, not when it is built: a walk resumed at the
  * question rebuilds the tree from a state where the relic is already
  * buried. The draw takes the top 3 of the legacy deck into
  * the keeper's hand (`LegacyDraws`); a short deck draws what remains. The
  * keep is its own `Branch`, after the draw's, so a walk resumed at the
  * question reads the same hand. With two or three drawn the keeper chooses
  * one; with one it is kept unasked; with none nothing is asked. The kept
  * legacy joins the owner's lineage, dormant, and the others go to the
  * bottom of the deck. The lines name no legacy: the kept one is dormant.
  *
  * The power id was `relic.ancient-grit` while the card was in holding
  * (catalog batch 10 design); it is fixed here.
  */
case object AncientWrit extends PaidAction(AncientWritCard.power):
  /** How many legacies the Writ draws. */
  val Drawn: Int = 3
  val decisionId: String = "power.ancient-writ.keep"
  private val notes = NoteSet(id)
  /** "p1 drew 3 legacies." */
  val drew: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" drew "), NotePart.Arg(1),
    NotePart.Plural(1, " legacy.", " legacies.")))
  /** "p1 kept a legacy." */
  val kept: NoteKey = notes.key("kept", Vector(NotePart.Arg(0),
    NotePart.Text(" kept a legacy.")))
  notes.borrow(LegacyDraws.empty)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** The kept line is the keep's narration: no "Chose" line names the
    * dormant legacy. */
  override def narratedDecisions: Set[String] = Set(decisionId)

  private val card: PowerSourceRef = PowerSourceRef.Card(AncientWritCard.id)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    source match
      case DecisionOptionRef.Relic(relicId) => Right(Sequence(Vector(
        BuildOps((live, _) => bury(live, player, relicId), required = true),
        draw(player), keep(player))))
      case other => Left(OathViolation.InvalidEventOrder(
        s"${other.kind} is not a relic source"))

  /** The relic buried from `player`'s play area, its secrets back to them. */
  private def bury(state: ReadyGame, player: PlayerId, id: RelicId)
      : Either[OathViolation, Vector[CoreOperation]] = for
    held <- PlayerFacts.player(state, player)
    relic <- held.relics.find(_.id == id).toRight(
      OathViolation.InvalidEventOrder(
        s"${id.value} is not held by ${player.value}"))
  yield Bury.standard(BuryableCard.Relic(id),
    PositionedLocation(Location.PlayArea(player)), None, 0,
    relic.tokens.secrets, player)

  /** Who draws and chooses for `owner`. */
  private def keeper(state: ReadyGame, owner: PlayerId): PlayerId = owner

  private def draw(owner: PlayerId): Operation = Branch((live, _) =>
    val drawer = keeper(live, owner)
    LegacyDraws.steps(id, drawer, Drawn, card, count =>
      drew(card, NoteArg.Player(drawer), NoteArg.Number(count))))

  private def keep(owner: PlayerId): Operation =
    Branch((live, _) =>
      val chooser = keeper(live, owner)
      val line = Note(id, _ => Some(kept(card, NoteArg.Player(chooser))))
      live.game.current.player(owner).map(_.lineage) match
        case None => Vector.empty
        case Some(lineage) => keepOf(LegacyDraws.hand(live, chooser), chooser,
          lineage, line))

  /** The keep of `drawn`, waiting in `chooser`'s hand, for `lineage`. */
  private def keepOf(drawn: Vector[LegacyId], chooser: PlayerId,
      lineage: LineageId, line: Operation): Vector[Operation] =
    drawn match
      case Vector() => Vector.empty
      case Vector(only) => Vector(BuildOps((_, _) =>
        Right(Vector(keptMove(only, chooser, lineage))), required = true),
        line)
      case drawn => Vector(
        Decide(decisionId, chooser, DecisionQuery.ChooseOne(
          drawn.map(legacy =>
            DecisionOption.Legacy(DecisionOptionRef.Legacy(legacy))),
          heading = Some("Ancient Writ: keep a legacy"),
          confirmVerb = Some("Keep"))),
        BuildOps((_, pending) =>
          settled(pending, drawn, chooser, lineage), required = true),
        line)

  /** The kept legacy, from the chooser's hand to the owner's lineage, with
    * no face: dormant. */
  private def keptMove(legacy: LegacyId, chooser: PlayerId,
      lineage: LineageId): Move =
    Move(Piece.Card(legacy), PositionedLocation(Location.Hand(chooser)),
      PositionedLocation(Location.Legacies(lineage)))

  private def settled(pending: PendingTree, drawn: Vector[LegacyId],
      chooser: PlayerId, lineage: LineageId)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.one(pending, decisionId) match
      case Some(DecisionOptionRef.Legacy(chosen)) if drawn.contains(chosen) =>
        Right(keptMove(chosen, chooser, lineage) +: drawn.filterNot(_ == chosen)
          .map(legacy => Move(Piece.Card(legacy),
            PositionedLocation(Location.Hand(chooser)),
            PositionedLocation.legacyDeckBottom)))
      case _ => Left(PowerAnswers.missing(decisionId))
