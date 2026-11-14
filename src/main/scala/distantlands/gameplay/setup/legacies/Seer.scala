package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.walker.DecisionQueries
import distantlands.model._

object SeerCard extends Legacy(LegacyId("L22"), "Seer",
    LegacyGoal.DarkestSecret, Suit.Arcane):
  val power = PrintedPower(PowerId("legacy.seer"),
    persistent = false, cost = Cost.free,
    text = "**SETUP:** When building the world deck, peek at the " +
      "Visions and secretly place them in any positions.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Seer (legacy L22), its SETUP power (Legacy Setup powers design, ruling
  * 15). In the `SetupWorldDeck` window, after Secret Society (R24), its
  * holder places every Vision in the world deck: one `ChooseAmounts`, a
  * slot per Vision, each 1 to the deck's size, distinct (R23). The Visions
  * are lifted out, the denizens keeping their order, and inserted in
  * ascending position, so each lands where chosen; the new order is a
  * filled `Shuffle` of the world deck. Line: "p2 placed the Visions with
  * Seer". */
case object Seer extends ContributingPower:
  val id: PowerId = SeerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupWorldDeck -> Vector(Transform((_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.seer.${player.value}"

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val placedVisions: NoteKey = notes.key("seer-placed", Vector(
    NotePart.Arg(0), NotePart.Text(" placed the Visions with "),
    NotePart.Arg(1)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** `deck` with its Visions at `positions` (1 from the top), the other
    * cards in their order. Inserting in ascending position puts each
    * exactly where chosen. */
  def placed(deck: Vector[WorldCardId], positions: Map[VisionId, Int])
      : Vector[WorldCardId] =
    val rest = deck.filterNot {
      case id: VisionId => positions.contains(id)
      case _ => false
    }
    positions.toVector.sortBy(_._2).foldLeft(rest) { case (built, (vision, at)) =>
      val (before, after) = built.splitAt(at - 1)
      (before :+ vision) ++ after
    }

  private def step: Operation = Branch((state, _) =>
    SetupLegacyRules.seer(state).toVector.flatMap { holder =>
      val deck = state.game.current.commonCards.worldDeck
      val visions = deck.collect { case id: VisionId => id }
      val slots = visions.map(vision => DistributeSlot(
        DecisionOptionRef.Vision(vision), 1, deck.size,
        Some(deck.indexOf(vision) + 1)))
      val query = DecisionQuery.ChooseAmounts(slots, distinct = true,
        heading = Some("Seer: place the Visions in the world deck"),
        confirmLabel = "Place the Visions")
      if DecisionQueries.wellFormed(decisionId(holder), query).isLeft then
        Vector.empty
      else Vector(
        Decide(decisionId(holder), holder, query),
        Branch((live, pending) => answered(pending, holder).toVector.flatMap(
          positions => Vector(
            Shuffle(ShufflePile.Search(SearchSource.WorldDeck), Some(placed(
              live.game.current.commonCards.worldDeck, positions)
              .map(card => card: CardId)))) ++
            // "peek at the Visions": the holder keeps knowing them, after
            // the reorder, which forgets a shuffled pile's peeks (R20).
            positions.keys.toVector.sortBy(_.value).map(vision =>
              Peek(holder, vision, Location.Deck(CardDeck.World))) :+
            Note(noteId, _ => Some(placedVisions.sourceless(
              NoteArg.Player(holder), NoteArg.Card(SeerCard.id)))))))
    })

  private def answered(pending: PendingTree, holder: PlayerId)
      : Option[Map[VisionId, Int]] = pending.answered.collectFirst {
    case Answered(id, DecisionAnswer.DistributeAnswer(amounts), _)
        if id == decisionId(holder) =>
      amounts.collect { case DistributeAmount(DecisionOptionRef.Vision(v), n) =>
        v -> n }.toMap
  }
