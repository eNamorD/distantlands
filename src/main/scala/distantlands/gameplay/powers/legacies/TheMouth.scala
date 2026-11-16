package distantlands.gameplay.powers.legacies

import distantlands.catalog.{ExecutableCatalog, FoundationCards, Legacy,
  LegacyGoal, PrintedPower}
import distantlands.gameplay.chronicle.{ChronicleTasks, LegacyGoals,
  ResolveLegacies}
import distantlands.gameplay.chronicle.legacies.{IronHand, IronHandCard}
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.PaidAction
import distantlands.model._

object TheMouthCard extends Legacy(LegacyId("L34"), "The Mouth",
    LegacyGoal.MostOfSuit, Suit.Arcane, Some(FoundationNumber.VI)):
  val power = PrintedPower(PowerId("legacy.the-mouth"),
    persistent = false, cost = Cost(favorBurnt = 2),
    text = "Set Foundation VI to Festival. **ACTION:** Burn " +
      "[favor-burnt] [favor-burnt] to activate a dormant legacy if " +
      "you meet its goal. _You can even activate legacies in the " +
      "Public Ambitions foundation._")
  val powers: Vector[PrintedPower] = Vector(power)

/** The Mouth (legacy L34): "ACTION: Burn 2 favor to activate a dormant
  * legacy if you meet its goal. You can even activate legacies in the
  * Public Ambitions foundation" (Legacy in-play powers design, rulings 23
  * and 24). Its "Set Foundation VI to Festival" is the card's `alters`,
  * which the Sun applies.
  *
  * The targets are the holder's own dormant legacies, then the Public
  * Ambitions row, whose goal the holder meets now, read as Resolve
  * Legacies reads it but counting only faceup advisers and faceup relics,
  * for every player (`LegacyGoals.meeting` with `faceupOnly`; R31). With
  * none the ACTION is shown unavailable with its reason (R30). An own legacy flips
  * faceup,
  * which reveals it; a public one moves faceup into the lineage. The same
  * batch alters the foundation the legacy names, unless it already lists
  * that legacy, and discards an Iron Hand its Imperial holder activated,
  * whose standing rule says to (R25). Then a holder above three active
  * legacies discards to three, flipping no foundation back
  * (`ResolveLegacies.cap`).
  *
  * The tree's shape never reads what it changes: the question offers the
  * answered legacy too once it is active, so a walk rebuilt at the cap
  * finds its answer (R20's lesson). The lines read the activation's own
  * step, which nothing parks after.
  *
  * Lines (R27): "p1 activated Arbiter with The Mouth", "p1 took Golden
  * Key from the Public Ambitions with The Mouth", "Beloved set Foundation
  * IV to Teeming World", "p1 discarded Seer with The Mouth"; and for an
  * Imperial's Iron Hand, "p1 activated Iron Hand with The Mouth" and Iron
  * Hand's own "p1 discarded Iron Hand", in words (R32).
  */
case object TheMouth extends PaidAction(TheMouthCard.power):
  val decisionId: String = "power.the-mouth.legacy"
  /** The cap's question. One id for every holder, since only the walk's
    * actor answers it, so the power can narrate it by its exact id. */
  val capDecisionId: String = "power.the-mouth.cap"

  private val notes = NoteSet(id)
  /** "{p1} activated {Arbiter} with {The Mouth}". */
  val activated: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" activated "), NotePart.Arg(1), NotePart.Text(" with "),
    NotePart.Arg(2)))
  /** "{p1} took {Golden Key} from the Public Ambitions with {The Mouth}". */
  val tookPublic: NoteKey = notes.key(s"${NoteKey.Used}.public", Vector(NotePart.Arg(0),
    NotePart.Text(" took "), NotePart.Arg(1),
    NotePart.Text(" from the Public Ambitions with "), NotePart.Arg(2)))
  /** "{p1} activated Iron Hand with {The Mouth}", and the public row's
    * line, for an Iron Hand the same batch discards: it never shows
    * faceup, so a card reference would read "a Legacy" (R32). */
  val activatedIronHand: NoteKey = notes.key(s"${NoteKey.Used}.iron-hand",
    Vector(NotePart.Arg(0),
      NotePart.Text(s" activated ${IronHandCard.name} with "),
      NotePart.Arg(1)))
  val tookIronHand: NoteKey = notes.key(s"${NoteKey.Used}.public-iron-hand",
    Vector(NotePart.Arg(0), NotePart.Text(s" took ${IronHandCard.name} " +
      "from the Public Ambitions with "), NotePart.Arg(1)))
  /** "{Beloved} set Foundation {IV} to {Teeming World}", one key per
    * foundation, as the Sun's are. */
  val altered: Map[FoundationNumber, NoteKey] = FoundationNumber.all.map(
    number => number -> notes.key(
      s"altered-foundation-${number.toString.toLowerCase}", Vector(
      NotePart.Arg(0), NotePart.Text(s" set Foundation $number to " +
        FoundationCards.face(number, FoundationFace.Altered).name)))).toMap
  /** "{p1} discarded {Seer} with The Mouth": a discard over the cap. */
  val capped: NoteKey = notes.key("capped", Vector(NotePart.Arg(0),
    NotePart.Text(" discarded "), NotePart.Arg(1),
    NotePart.Text(" with The Mouth")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Set(decisionId, capDecisionId)

  /** The holder's dormant legacies, then the Public Ambitions row, whose
    * goal `player` meets now (ruling 23). */
  def targets(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId): Vector[LegacyId] =
    def meets(legacy: LegacyId): Boolean = catalog.legacy(legacy).exists(
      card => LegacyGoals.meeting(ready, catalog, card, faceupOnly = true)(
        player))
    val own = ChronicleTasks.lineageOf(ready, player).toVector.flatMap(
      lineage => ready.game.campaign.lineages.get(lineage).toVector
        .flatMap(_.legacies).collect {
          case LegacyState(legacy, false) => legacy })
    (own ++ ready.game.current.commonCards.publicLegacies).filter(meets)

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(targets(catalog, ready, player).isEmpty)(
      "No legacy whose goal you meet")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, pending) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseOne(offered(catalog, live, player, pending)
        .map(legacy => DecisionOption.Legacy(DecisionOptionRef.Legacy(legacy))),
        heading = Some("The Mouth: activate a legacy whose goal you meet"),
        confirmVerb = Some("Activate"))))),
    BuildOps((live, pending) => activation(catalog, live, player, pending),
      required = true),
    Note(id, states => activationNote(states, player)),
    Note(id, alterationNote),
    Note(IronHand.id, states => ironHandNote(states, player)),
    ResolveLegacies.cap(player, capDecisionId, id, capped))))

  private def chosen(answered: Vector[Answered]): Option[LegacyId] =
    PowerAnswers.last(answered, decisionId).collect {
      case DecisionOptionRef.Legacy(legacy) => legacy }

  /** The live targets, and the answered legacy once it is active. */
  private def offered(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, pending: PendingTree): Vector[LegacyId] =
    val live = targets(catalog, ready, player)
    live ++ chosen(pending.answered).filterNot(live.contains)

  /** The chosen legacy faceup in the holder's lineage, the foundation it
    * names altered, and an Imperial's Iron Hand discarded (R25). */
  private def activation(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] = for
    legacy <- chosen(pending.answered)
      .filter(targets(catalog, ready, player).contains)
      .toRight(PowerAnswers.missing(decisionId))
    lineage <- ChronicleTasks.lineageOf(ready, player).toRight(
      OathViolation.InvalidEventOrder(s"${player.value} has no lineage"))
  yield
    val into = Location.Legacies(lineage)
    val activate: CoreOperation =
      if ready.game.current.commonCards.publicLegacies.contains(legacy) then
        Move(Piece.Card(legacy), PositionedLocation(Location.PublicLegacies),
          PositionedLocation(into),
          resultingOrientation = Some(Orientation.FaceUp))
      else Flip(legacy, into, Orientation.FaceUp)
    val alteration = catalog.legacy(legacy).flatMap(_.alters).filterNot(
      number => ready.game.campaign.foundations.get(number)
        .exists(_.alterationSources(legacy)))
      .map(number => FoundationAltered(number, legacy))
    val ironHand = Option.when(legacy == IronHandCard.id &&
      Imperials.isImperial(ready, player))(Move(Piece.Card(legacy),
        PositionedLocation(into),
        PositionedLocation.legacyDeckBottom))
    Vector[CoreOperation](activate) ++ alteration.toVector ++
      ironHand.toVector

  private def activationNote(states: NoteStates, player: PlayerId)
      : Option[LogNote] = for
    (before, after) <- states.previous
    legacy <- chosen(states.answered)
  yield
    val public =
      before.game.current.commonCards.publicLegacies.contains(legacy)
    val discarded = legacy == IronHandCard.id &&
      ChronicleTasks.lineageOf(after, player).forall(!holdsIronHand(after, _))
    val mouth = NoteArg.Card(TheMouthCard.id)
    if discarded then
      (if public then tookIronHand else activatedIronHand).sourceless(
        NoteArg.Player(player), mouth)
    else (if public then tookPublic else activated).sourceless(
      NoteArg.Player(player), NoteArg.Card(legacy), mouth)

  /** The foundation the activation altered, and the legacy that did. */
  private def alterationNote(states: NoteStates): Option[LogNote] = for
    (before, after) <- states.previous
    legacy <- chosen(states.answered)
    number <- FoundationNumber.all.find(number =>
      after.game.campaign.foundations.get(number)
        .exists(_.alterationSources(legacy)) &&
        !before.game.campaign.foundations.get(number)
          .exists(_.alterationSources(legacy)))
  yield altered(number).sourceless(NoteArg.Card(legacy))

  /** "p1 discarded Iron Hand", when the activation sent it back. */
  private def ironHandNote(states: NoteStates, player: PlayerId)
      : Option[LogNote] = for
    (before, after) <- states.previous
    if chosen(states.answered).contains(IronHandCard.id)
    lineage <- ChronicleTasks.lineageOf(after, player)
    if !holdsIronHand(after, lineage) && (holdsIronHand(before, lineage) ||
      before.game.current.commonCards.publicLegacies
        .contains(IronHandCard.id))
  yield IronHand.discardedNow.sourceless(NoteArg.Player(player))

  private def holdsIronHand(ready: ReadyGame, lineage: LineageId): Boolean =
    ready.game.campaign.lineages.get(lineage)
      .exists(_.legacies.exists(_.id == IronHandCard.id))
