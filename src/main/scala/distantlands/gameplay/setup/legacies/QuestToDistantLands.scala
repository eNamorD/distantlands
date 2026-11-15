package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object QuestToDistantLandsCard extends Legacy(LegacyId("L30"), "Quest to Distant Lands",
    LegacyGoal.HighestRelic, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.quest-to-distant-lands"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** You may choose to not place your pawn. If you " +
      "do, you may choose one card from the Dispossessed and swap " +
      "it with a starting adviser or card you drew in Foundation " +
      "IV. _It is not buried by the Oaths of Loyalty legacy._ " +
      "**WAKE:** On your first turn, you **must** place your pawn " +
      "at any site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Quest to Distant Lands (legacy L30), its SETUP and first-Wake powers
  * (Legacy Setup powers design, rulings 18 to 20). Its holder's pawn
  * question offers `stayOff` (R30); a holder who placed no pawn may then
  * swap one Dispossessed card for one of their own (`swapStep`, R31). The
  * pawn is placed at the start of their first Wake (`QuestFirstWake`),
  * keyed on the missing pawn. This power hooks no window. Lines: "p2 placed
  * no pawn with Quest to Distant Lands", "p2 swapped a card with the
  * Dispossessed with Quest to Distant Lands". */
case object QuestToDistantLands extends ContributingPower:
  val id: PowerId = QuestToDistantLandsCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  /** The pawn question's answer for staying off the map (R30). */
  val stayOff: DecisionOptionRef.Button = DecisionOptionRef.Button("stay-off")
  val stayOffLabel: String = "Don't place your pawn"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  def swapDecisionId(player: PlayerId): String =
    s"setup.legacy.quest.${player.value}"
  def giveDecisionId(player: PlayerId): String =
    s"${swapDecisionId(player)}.give"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val placedNoPawn: NoteKey = notes.key("quest-placed-no-pawn", Vector(
    NotePart.Arg(0), NotePart.Text(" placed no pawn with "), NotePart.Arg(1)))
  val swapped: NoteKey = notes.key("quest-swapped", Vector(
    NotePart.Arg(0), NotePart.Text(" swapped a card with the Dispossessed with "),
    NotePart.Arg(1)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** "p2 placed no pawn with Quest to Distant Lands". */
  def placedNoPawnLine(player: PlayerId): LogNote =
    placedNoPawn.sourceless(NoteArg.Player(player),
      NoteArg.Card(QuestToDistantLandsCard.id))

  /** The swap, after the holder's keep and before where Choose Adviser
    * would run, for Quest's holder with no pawn (R31), when the
    * Dispossessed holds a denizen and the holder a card to give (R37). The
    * holder peeks
    * every Dispossessed card first, as The Bright Beacon's holder does, and
    * keeps knowing them; Setup's peeks write no line (R26). */
  def swapStep(player: PlayerId): Operation = Branch((state, _) =>
    val cards = state.game.campaign.dispossessed
      .collect { case id: DenizenId => id }.sortBy(_.value)
    // A swap needs a card on both sides; with none to give, the Peeks
    // would show the Dispossessed for nothing (R37).
    if !SetupLegacyRules.mayStayOff(state, player) ||
        state.game.current.pawnSite(player).nonEmpty || cards.isEmpty ||
        giveOptions(state, player).isEmpty then
      Vector.empty
    else cards.map(card => Peek(player, card, Location.Dispossessed)) ++
      Vector(
        Decide(swapDecisionId(player), player, DecisionQuery.ChooseOne(
          DecisionOption.Button(decline, "Decline") +: cards.map(card =>
            DecisionOption.Denizen(DecisionOptionRef.Denizen(card))),
          heading = Some("Quest to Distant Lands: you may swap a card with " +
            "the Dispossessed"),
          confirmVerb = Some("Take"))),
        Branch((live, pending) =>
          PowerAnswers.one(pending, swapDecisionId(player)).toVector.collect {
            case DecisionOptionRef.Denizen(taken) => taken
          }.flatMap(taken => give(live, player, taken)))))

  /** The card to give: a facedown adviser that is no Vision, by slot, or,
    * with none, a card of the hand drawn in Foundation IV (R31). */
  private def give(ready: ReadyGame, player: PlayerId, taken: DenizenId)
      : Vector[Operation] =
    val options = giveOptions(ready, player)
    if options.isEmpty then Vector.empty
    else Vector(
      Decide(giveDecisionId(player), player, DecisionQuery.ChooseOne(options,
        heading = Some("Quest to Distant Lands: choose the card to give"),
        confirmVerb = Some("Give"))),
      BuildOps((live, pending) => traded(live, player, taken,
        PowerAnswers.one(pending, giveDecisionId(player))), required = true),
      Note(noteId, _ => Some(swapped.sourceless(NoteArg.Player(player),
        NoteArg.Card(QuestToDistantLandsCard.id)))))

  /** The cards `player` may give, by slot for a facedown adviser that is
    * no Vision, or, with none, from the hand drawn in Foundation IV. */
  private def giveOptions(ready: ReadyGame, player: PlayerId)
      : Vector[DecisionOption] =
    val advisers = ready.game.current.player(player).toVector
      .flatMap(_.advisers).zipWithIndex.collect {
        case (_: DenizenState, slot) => slot }
    val hand = ready.game.current.temporaryHands.getOrElse(player, Vector.empty)
      .collect { case id: DenizenId => id }
    if advisers.nonEmpty then advisers.map(slot =>
      DecisionOption.AdviserSlot(DecisionOptionRef.AdviserSlot(player, slot)))
    else hand.map(id => DecisionOption.Denizen(DecisionOptionRef.Denizen(id)))

  /** The two cards trade places: a facedown adviser's replacement joins
    * the advisers facedown, a hand card's the hand, and the given card goes
    * to the Dispossessed. A swap is no gain, so Oaths of Loyalty buries
    * nothing here. */
  private def traded(ready: ReadyGame, player: PlayerId, taken: DenizenId,
      giving: Option[DecisionOptionRef])
      : Either[OathViolation, Vector[CoreOperation]] =
    def swap(card: CardId, from: Location, orientation: Option[Orientation]) =
      Vector[CoreOperation](
        Move(Piece.Card(card), PositionedLocation(from),
          PositionedLocation(Location.Dispossessed)),
        Move(Piece.Card(taken), PositionedLocation(Location.Dispossessed),
          PositionedLocation(from), resultingOrientation = orientation))
    giving match
      case Some(DecisionOptionRef.AdviserSlot(_, slot)) =>
        ready.game.current.player(player).flatMap(_.advisers.lift(slot))
          .map(adviser => swap(adviser.id, Location.PlayArea(player),
            Some(Orientation.FaceDown)))
          .toRight(OathViolation.InvalidEventOrder(
            s"no adviser in slot $slot to give"))
      case Some(DecisionOptionRef.Denizen(card)) =>
        Right(swap(card, Location.Hand(player), None))
      case _ => Left(OathViolation.InvalidEventOrder(
        s"no card to give is recorded for ${giveDecisionId(player)}"))
