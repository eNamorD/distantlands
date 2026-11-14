package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.{ProcedureNotes, VisionRules}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object SecretSocietyCard extends Legacy(LegacyId("L28"), "Secret Society",
    LegacyGoal.HighestRelic, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.secret-society"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** Before building the world deck, you may secretly " +
      "choose a Vision except the Conspiracy to take as a facedown " +
      "adviser _(discard excess advisers)_. If you do, advance the " +
      "Visions Drawn marker once.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Secret Society (legacy L28), its SETUP power (Legacy Setup powers
  * design, ruling 14). In the `SetupWorldDeck` window, before Seer, its
  * holder may take one Vision but the Conspiracy from the world deck, as a
  * facedown adviser, offered in catalog order and seen only through the
  * question's options (R20, R22). A take advances Visions Drawn once and rebuilds
  * the deck in the card's layout (`ShufflePile.VisionPackets`, R19). The
  * Vision is not a starting adviser, so Oaths of Loyalty does not bury it;
  * excess advisers are discarded after the holder's pawn (R21). Line: "p2
  * took a Vision as a facedown adviser with Secret Society". */
case object SecretSociety extends ContributingPower:
  val id: PowerId = SecretSocietyCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupWorldDeck -> Vector(Transform((_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.secret-society.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val tookVision: NoteKey = notes.key("secret-society-took", Vector(
    NotePart.Arg(0), NotePart.Text(" took a Vision as a facedown adviser with "),
    NotePart.Arg(1)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** The Visions the holder may take, in catalog order: deck order would
    * tell their places after a Decline (R22). */
  private def takeable(state: ReadyGame): Vector[VisionId] =
    state.game.current.commonCards.worldDeck.collect {
      case id: VisionId if id != VisionRules.Conspiracy => id }
      .sortBy(_.value)

  private def step: Operation = Branch((state, _) =>
    SetupLegacyRules.secretSociety(state).toVector.flatMap { holder =>
      takeable(state) match
        case Vector() => Vector.empty
        case visions => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
            DecisionOption.Button(decline, "Decline") +: visions.map(id =>
              DecisionOption.Vision(DecisionOptionRef.Vision(id))),
            heading = Some(
              "Secret Society: you may take a Vision as a facedown adviser"),
            confirmVerb = Some("Take"))),
          Branch((_, pending) =>
            PowerAnswers.one(pending, decisionId(holder)).toVector.collect {
              case DecisionOptionRef.Vision(vision) => vision
            }.flatMap(vision => Vector(
              BuildOps((_, _) => Right(Vector(
                Move(Piece.Card(vision),
                  PositionedLocation(Location.Deck(CardDeck.World)),
                  PositionedLocation(Location.PlayArea(holder)),
                  resultingOrientation = Some(Orientation.FaceDown)),
                AdvanceVisionsDrawn)), required = true),
              Shuffle(ShufflePile.VisionPackets),
              Note(noteId, _ => Some(tookVision.sourceless(
                NoteArg.Player(holder), NoteArg.Card(SecretSocietyCard.id))))))))
    })
