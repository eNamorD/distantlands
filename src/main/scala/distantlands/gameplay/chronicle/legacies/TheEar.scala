package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ReliquaryAndRelics
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object TheEarCard extends Legacy(LegacyId("L19"), "The Ear",
    LegacyGoal.DarkestSecret, Suit.Hearth):
  val power = PrintedPower(PowerId("legacy.the-ear"),
    persistent = false, cost = Cost.free,
    text = "**ACTION:** You may peek at the relics at any site. " +
      "**CHRONICLE (THRONE):** In Scatter Relics, you examine the " +
      "relics and choose which sites they go to.")
  val powers: Vector[PrintedPower] = Vector(power)

/** The Ear (legacy L19), its Chronicle half: "In Scatter Relics, you
  * examine the relics and choose which sites they go to." Scatter Relics
  * asks `ChronicleLegacyRules.scatterer` once its relics are set aside and
  * shuffled (Legacy Chronicle powers design, ruling 14; R21): the holder
  * peeks every set-aside relic, then fills the empty slots one at a time in
  * Hinterward order, each read when reached under one reused decision id,
  * while two or more relics remain; the last is placed without a question.
  * It stops when relics or empty slots run out, and the rest return to the
  * relic deck as before. Each placed relic is peeked again for the holder
  * at its site, so they still know it there (R22). Its ACTION half waits
  * for the in-play batch (R7). This power hooks no window; it is registered
  * so the card counts as implemented. Line: "p2 chose where the relics went
  * with The Ear". */
case object TheEar extends ContributingPower:
  val id: PowerId = TheEarCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.legacy.the-ear.${player.value}"

  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val placed: NoteKey = notes.key("the-ear-placed", Vector(NotePart.Arg(0),
    NotePart.Text(" chose where the relics went with "), NotePart.Arg(1)))
  /** The Throne lines this power writes, on the Throne's note id. */
  def throneNoteKeys: Vector[NoteKey] = notes.declared

  /** The holder's part of Scatter Relics on `state`, once its relics are
    * set aside and shuffled: the peeks, then the slot by slot placement,
    * then the line, when there is a relic and a slot. */
  def steps(catalog: ExecutableCatalog, state: ReadyGame, holder: PlayerId)
      : Vector[Operation] =
    val relics = state.game.current.setAsideRelics
    if relics.isEmpty || ReliquaryAndRelics.emptySlots(catalog, state).isEmpty
    then Vector.empty
    else Vector(
      BuildOps((_, _) => Right(relics.map(relic =>
        Peek(holder, relic, Location.SetAsideRelics))), required = true),
      Repeat((live, _) => live.game.current.setAsideRelics.nonEmpty &&
          ReliquaryAndRelics.emptySlots(catalog, live).nonEmpty,
        Sequence(Vector(
          Branch((live, _) => ask(catalog, live, holder)),
          Branch((live, pending) => place(catalog, live, holder, pending))))),
      Note(noteId, _ => Some(placed.sourceless(NoteArg.Player(holder),
        NoteArg.Card(TheEarCard.id)))))

  /** The question for the first empty slot, while two or more relics
    * remain. */
  private def ask(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId): Vector[Operation] =
    val relics = state.game.current.setAsideRelics
    ReliquaryAndRelics.emptySlots(catalog, state).headOption
      .filter(_ => relics.size >= 2).toVector.map(site => Decide(
        decisionId(holder), holder, DecisionQuery.ChooseOne(
          relics.map(relic => DecisionOption.Relic(DecisionOptionRef.Relic(relic))),
          heading = Some("The Ear: choose the relic for " +
            catalog.site(site).fold(site.value)(_.name)),
          confirmVerb = Some("Place"))))

  /** The pass's relic, the latest answer or the only one left, facedown at
    * the first empty slot's site, peeked there for the holder. A pass with
    * two or more relics parks on its question before this runs, so the
    * latest answer is always this pass's. */
  private def place(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId, pending: PendingTree): Vector[Operation] =
    val relics = state.game.current.setAsideRelics
    val relic = relics match
      case Vector(only) => Some(only)
      case _ => PowerAnswers.last(pending.answered, decisionId(holder))
        .collect { case DecisionOptionRef.Relic(id) => id }
        .filter(relics.contains)
    (for
      chosen <- relic
      site <- ReliquaryAndRelics.emptySlots(catalog, state).headOption
    yield BuildOps((_, _) => Right(Vector(
      Move(Piece.Card(chosen), PositionedLocation(Location.SetAsideRelics),
        PositionedLocation(Location.Site(site)),
        resultingOrientation = Some(Orientation.FaceDown)),
      Peek(holder, chosen, Location.Site(site)))), required = true)).toVector
