package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object TheBrightBeaconCard extends Legacy(LegacyId("L24"), "The Bright Beacon",
    LegacyGoal.DarkestSecret, Suit.Nomad):
  val power = PrintedPower(PowerId("legacy.the-bright-beacon"),
    persistent = false, cost = Cost.free,
    text = "**CHRONICLE (BEACON):** If you're resolving the Beacon, you " +
      "may take any six cards from the Dispossessed _(instead of " +
      "choosing suits and taking from their matching dividers)_.")
  val powers: Vector[PrintedPower] = Vector(power)

/** The Bright Beacon (legacy L24): "If you're resolving the Beacon, you may
  * take any six cards from the Dispossessed (instead of choosing suits and
  * taking from their matching dividers)." Introduce Newcomers asks
  * `ChronicleLegacyRules.beaconFromDispossessed` first (Legacy Chronicle
  * powers design, ruling 17; R25): the holder chooses the source before
  * seeing the Dispossessed, the suited box first. From the Dispossessed
  * they peek every Dispossessed denizen, the older then the newly
  * Dispossessed (R22), and choose exactly six, or take all six when exactly
  * six remain; the six go to their temporary hand, and Welcome a Stranger
  * runs on them. This power hooks no window; it is registered so the card
  * counts as implemented. Line: "p3 took six newcomers from the
  * Dispossessed with The Bright Beacon". */
case object TheBrightBeacon extends ContributingPower:
  val id: PowerId = TheBrightBeaconCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  /** The newcomers the Dispossessed give. */
  val Taken: Int = 6

  def sourceId(player: PlayerId): String =
    s"chronicle.beacon.legacy.the-bright-beacon.${player.value}"
  def cardsId(player: PlayerId): String = sourceId(player) + ".cards"
  val box: DecisionOptionRef.Button = DecisionOptionRef.Button("box")
  val dispossessed: DecisionOptionRef.Button =
    DecisionOptionRef.Button("dispossessed")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon)
  private val notes = NoteSet(noteId)
  val took: NoteKey = notes.key("bright-beacon-took", Vector(
    NotePart.Arg(0), NotePart.Text(" took six newcomers from the " +
      "Dispossessed with "), NotePart.Arg(1)))
  /** The Beacon lines this power writes, on the Beacon's note id. */
  def beaconNoteKeys: Vector[NoteKey] = notes.declared

  /** The source question, before the Dispossessed are seen. */
  def askSource(state: ReadyGame): Vector[Operation] =
    ChronicleLegacyRules.beaconFromDispossessed(state).toVector.map(holder =>
      Decide(sourceId(holder), holder, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(box, "Suited box"),
        DecisionOption.Button(dispossessed, "The Dispossessed")),
        heading = Some("The Bright Beacon: take newcomers from the suited " +
          "box or from the Dispossessed"))))

  /** Whether the holder chose the Dispossessed. */
  def fromDispossessed(state: ReadyGame, pending: PendingTree): Boolean =
    ChronicleLegacyRules.beaconFromDispossessed(state).exists(holder =>
      PowerAnswers.one(pending, sourceId(holder)).contains(dispossessed))

  /** The peeks, the choice of six or the six taken, and the line. The
    * options are what this step read, so a walk resumed at the choice finds
    * the same cards. */
  def fromDispossessedSteps(state: ReadyGame): Vector[Operation] =
    ChronicleLegacyRules.beaconFromDispossessed(state).toVector.flatMap(
      holder =>
        val cards = dispossessedCards(state)
        Option.when(cards.nonEmpty)(BuildOps((_, _) => Right(cards.map(
            (card, at) => Peek(holder, card, at))), required = true)).toVector ++
          (if cards.size <= Taken then taken(holder, cards)
          else Vector(
            Decide(cardsId(holder), holder, DecisionQuery.ChooseMany(Taken,
              Taken, cards.map((card, _) =>
                DecisionOption.Denizen(DecisionOptionRef.Denizen(card))),
              heading = Some("The Bright Beacon: take six newcomers from " +
                "the Dispossessed"),
              confirm = Some(CountedConfirm("Take", "newcomer",
                "newcomers")))),
            Branch((_, pending) =>
              val chosen = PowerAnswers.many(pending, cardsId(holder))
                .getOrElse(Vector.empty).toSet
              taken(holder, cards.filter((card, _) =>
                chosen(DecisionOptionRef.Denizen(card))))))))

  /** The Dispossessed denizens with their pile: the older, then the newly
    * Dispossessed. */
  private def dispossessedCards(state: ReadyGame)
      : Vector[(DenizenId, Location)] =
    state.game.campaign.dispossessed.collect { case id: DenizenId =>
      id -> (Location.Dispossessed: Location) } ++
      state.game.current.newlyDispossessed.collect { case id: DenizenId =>
        id -> (Location.NewlyDispossessed: Location) }

  private def taken(holder: PlayerId, cards: Vector[(DenizenId, Location)])
      : Vector[Operation] =
    if cards.isEmpty then Vector.empty
    else Vector(
      BuildOps((_, _) => Right(cards.map((card, at) => Move(Piece.Card(card),
        PositionedLocation(at), PositionedLocation(Location.Hand(holder))))),
        required = true),
      Note(noteId, _ => Some(took.sourceless(NoteArg.Player(holder),
        NoteArg.Card(TheBrightBeaconCard.id)))))
