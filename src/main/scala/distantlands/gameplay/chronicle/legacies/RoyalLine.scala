package distantlands.gameplay.chronicle.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.EmpireEndings
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, ChronicleTasks}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.model._

object RoyalLineCard extends Legacy(LegacyId("L10"), "Royal Line",
    LegacyGoal.GrandScepter, Suit.Beast):
  val power = PrintedPower(PowerId("legacy.royal-line"),
    persistent = false, cost = Cost.free,
    text = "**IF YOU ARE A CITIZEN:** At the end of the game, you " +
      "may choose which Imperial meets the Successor goal. " +
      "_Ignore the printed goals. You may choose no one._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Royal Line (legacy L10): "If you are a Citizen: At the end of the game, you
  * may choose which Imperial meets the Successor goal. Ignore the printed
  * goals. You may choose no one." Its step is the window before Start Chronicle
  * (Legacy Chronicle powers design, ruling 22; R40, moved there from the
  * Sun's first window by Legacy in-play powers R36). Only after a Chancellor's or a
  * Successor's win, a seated Citizen holding it active, read when reached,
  * may decline, which keeps the result, or choose any Imperial, the
  * Chancellor first (R38, re-ruled by the owner 2026-11-14). A
  * Citizen then wins as the Successor; the Chancellor wins by the ended
  * game's Empire win (`EmpireEndings.empireWin`, R37). `WinnerNamed` (R39)
  * journals a changed winner (ruling 23); every later step reads it from
  * the result. Line, only when the winner changes: "p3 named p2 the winner
  * with Royal Line" (R41). */
case object RoyalLine extends ContributingPower:
  val id: PowerId = RoyalLineCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleFinalWinner -> Vector(Transform(
      (_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"chronicle.final-winner.royal-line.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleWinner)
  private val notes = NoteSet(noteId)
  val named: NoteKey = notes.key("royal-line-named", Vector(NotePart.Arg(0),
    NotePart.Text(" named "), NotePart.Arg(1),
    NotePart.Text(" the winner with "), NotePart.Arg(2)))
  /** The lines this power writes, on the final winner step's note id. */
  def winnerNoteKeys: Vector[NoteKey] = notes.declared

  /** The question and its answer. The result and the Chancellor it reads
    * do not change before the answer, so a resumed walk finds the same
    * options. */
  val step: Operation = Branch((state, _) =>
    (for
      result <- state.game.current.result if EmpireEndings.empireWins(result.kind)
      holder <- ChronicleLegacyRules.holder(state, RoyalLineCard.id,
        Set(Role.Citizen))
      chancellor <- Imperials.chancellor(state)
    yield Vector(
      Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
        DecisionOption.Button(decline, "Decline") +:
          ChronicleTasks.playerOptions(chancellor +:
            Imperials.players(state).filterNot(_ == chancellor)),
        heading = Some("Royal Line: you may choose the winner"),
        confirmVerb = Some("Name"))),
      Branch((live, pending) => ChronicleTasks.chosenPlayer(pending,
          decisionId(holder)).toOption.toVector.flatMap(chosen =>
        named(live, holder, chancellor, result, chosen))))
    ).getOrElse(Vector.empty))

  /** The nodes for `chosen`: none when they already win. */
  private def named(state: ReadyGame, holder: PlayerId, chancellor: PlayerId,
      result: GameResult, chosen: PlayerId): Vector[Operation] =
    val kind =
      if chosen != chancellor then VictoryKind.Successor
      else if result.kind == VictoryKind.Successor then
        EmpireEndings.empireWin(state)
      else result.kind
    if GameResult(chosen, kind) == result then Vector.empty
    else Vector(
      BuildOps((_, _) => Right(Vector(WinnerNamed(chosen, kind))),
        required = true),
      Note(noteId, _ => Some(named.sourceless(NoteArg.Player(holder),
        NoteArg.Player(chosen), NoteArg.Card(RoyalLineCard.id)))))
