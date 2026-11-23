package distantlands.gameplay.powers.legacies

import distantlands.catalog.{ExecutableCatalog, FoundationCards}
import distantlands.gameplay.chronicle.ChronicleTasks
import distantlands.gameplay.chronicle.legacies.{IronHand, IronHandCard}
import distantlands.gameplay.powers.LegacyDraws
import distantlands.model._

/** Activating a legacy the way The Mouth does (Legacy in-play powers
  * rulings 23 and 24; R25), shared with Amber Doors (catalog batch 13,
  * ruling 12). The legacy goes faceup into the holder's lineage: a flip
  * when it is already there, dormant, else a move from where it waits (the
  * Public Ambitions row, or the holder's hand after a draw). The same batch
  * alters the foundation the legacy names, unless that foundation already
  * lists it, and discards an Iron Hand its Imperial holder activated to the
  * bottom of the legacy deck, whose standing rule says to.
  *
  * Callers check their own conditions first (The Mouth's goal) and handle
  * what the activation leaves (The Mouth's cap). The two lines are read
  * after the activation's own step, which nothing parks after.
  */
object LegacyActivation:
  /** The keys this object owns, which no one power owns. A power that
    * writes one includes this set. */
  val notes: NoteSet = NoteSet.unowned("LegacyActivation")
  /** "{Beloved} set Foundation {IV} to {Teeming World}", one key per
    * foundation, as the Sun's are. */
  val altered: Map[FoundationNumber, NoteKey] = FoundationNumber.all.map(
    number => number -> notes.key(
      s"altered-foundation-${number.toString.toLowerCase}", Vector(
      NotePart.Arg(0), NotePart.Text(s" set Foundation $number to " +
        FoundationCards.face(number, FoundationFace.Altered).name)))).toMap

  /** `legacy` faceup in `lineage`, from `from`, the foundation it names
    * altered, and an Imperial's Iron Hand discarded (R25). */
  def operations(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, lineage: LineageId, legacy: LegacyId,
      from: Location): Vector[CoreOperation] =
    val into = Location.Legacies(lineage)
    val activate: CoreOperation =
      if from == into then Flip(legacy, into, Orientation.FaceUp)
      else Move(Piece.Card(legacy), PositionedLocation(from),
        PositionedLocation(into),
        resultingOrientation = Some(Orientation.FaceUp))
    val alteration = catalog.legacy(legacy).flatMap(_.alters).filterNot(
      number => ready.game.campaign.foundations.get(number)
        .exists(_.alterationSources(legacy)))
      .map(number => FoundationAltered(number, legacy))
    val ironHand = Option.when(discardsIronHand(ready, player, legacy))(
      Move(Piece.Card(legacy), PositionedLocation(into),
        PositionedLocation.legacyDeckBottom))
    Vector[CoreOperation](activate) ++ alteration.toVector ++
      ironHand.toVector

  /** Whether activating `legacy` discards it at once: Iron Hand, activated
    * by an Imperial (R25). */
  def discardsIronHand(ready: ReadyGame, player: PlayerId,
      legacy: LegacyId): Boolean =
    legacy == IronHandCard.id && Imperials.isImperial(ready, player)

  /** The foundation the activation of `activated` altered. */
  def alterationNote(states: NoteStates, activated: Option[LegacyId])
      : Option[LogNote] = for
    (before, after) <- states.previous
    legacy <- activated
    number <- FoundationNumber.all.find(number =>
      after.game.campaign.foundations.get(number)
        .exists(_.alterationSources(legacy)) &&
        !before.game.campaign.foundations.get(number)
          .exists(_.alterationSources(legacy)))
  yield altered(number).sourceless(NoteArg.Card(legacy))

  /** "p1 discarded Iron Hand", when the activation of `activated` sent it
    * back from wherever it waited. */
  def ironHandNote(states: NoteStates, player: PlayerId,
      activated: Option[LegacyId]): Option[LogNote] = for
    (before, after) <- states.previous
    if activated.contains(IronHandCard.id)
    lineage <- ChronicleTasks.lineageOf(after, player)
    if !holdsIronHand(after, lineage) && (holdsIronHand(before, lineage) ||
      before.game.current.commonCards.publicLegacies
        .contains(IronHandCard.id) ||
      LegacyDraws.hand(before, player).contains(IronHandCard.id))
  yield IronHand.discardedNow.sourceless(NoteArg.Player(player))

  def holdsIronHand(ready: ReadyGame, lineage: LineageId): Boolean =
    ready.game.campaign.lineages.get(lineage)
      .exists(_.legacies.exists(_.id == IronHandCard.id))
