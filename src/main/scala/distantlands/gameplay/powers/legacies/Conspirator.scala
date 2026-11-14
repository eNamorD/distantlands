package distantlands.gameplay.powers.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.chronicle.{ChronicleLegacyRules, ChronicleTasks}
import distantlands.gameplay.powerresolver.PhasePower
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object ConspiratorCard extends Legacy(LegacyId("L26"), "Conspirator",
    LegacyGoal.HighestRelic, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.conspirator"),
    persistent = false, cost = Cost.free,
    text = "**WAKE:** On your first turn, secretly write down which " +
      "round the game will end on and who will win. If you are " +
      "correct, you win instead. _This even beats a Successor win._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Conspirator (legacy L26), its WAKE half: "On your first turn, secretly
  * write down which round the game will end on and who will win"
  * (Legacy in-play powers design, rulings 25 and 26).
  *
  * A forced Wake power in round 1, where every seat's first turn falls: a
  * Conspirator gained later, by The Mouth, never asks. A Quest to Distant
  * Lands holder places their pawn first, since the driver runs the first
  * Wake's placement before the forced powers. The holder writes a round
  * from 1 to 8, an amount no log line names, then a winner among the
  * seated players, themselves included; both questions are narrated, so
  * no "chose" line names an answer (R28). The prediction is stored by
  * `WritePrediction` only after both answers, so what is due does not
  * change before the last question. Every viewer reads "p2 wrote down a
  * prediction with Conspirator"; only the holder reads "p2 predicted p3
  * wins in round 6" (R26, R27). `ConspiratorReveal` reveals it at the
  * game's end.
  */
case object ConspiratorPrediction extends PhasePower:
  val id: PowerId = ConspiratorCard.power.id
  val roundDecisionId: String = "power.conspirator.round"
  val winnerDecisionId: String = "power.conspirator.winner"
  def timing: PowerTiming = PowerTiming.Wake
  override def forced: Boolean = true
  override def narratedDecisions: Set[String] =
    Set(roundDecisionId, winnerDecisionId)

  private val notes = NoteSet(id)
  /** "{p2} wrote down a prediction with {Conspirator}", for everyone. */
  val wrote: NoteKey = notes.key("wrote", Vector(NotePart.Arg(0),
    NotePart.Text(" wrote down a prediction with "), NotePart.Arg(1)))
  /** "{p2} predicted {p3} wins in round {6}", read by the holder only. */
  val predicted: NoteKey = notes.key("predicted", Vector(NotePart.Arg(0),
    NotePart.Text(" predicted "), NotePart.Arg(1),
    NotePart.Text(" wins in round "), NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared

  /** Round 1, Conspirator held active, and no prediction written yet
    * (rulings 1 and 25). */
  def usable(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Boolean =
    ready.game.current.tracks.round == 1 &&
      ChronicleLegacyRules.holder(ready, ConspiratorCard.id)
        .contains(player) &&
      !ready.game.current.predictions.contains(player)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Decide(roundDecisionId, player, DecisionQuery.ChooseAmount(1, 8,
      Some("Conspirator: secretly write down the round the game ends in"),
      "Write down")),
    // The seated players, a set the walk never changes.
    Decide(winnerDecisionId, player, DecisionQuery.ChooseOne(
      ChronicleTasks.playerOptions(ChronicleTasks.seated(ready)),
      heading = Some("Conspirator: secretly write down who will win"),
      confirmVerb = Some("Write down"))),
    BuildOps((_, pending) => for
      round <- PowerAnswers.amount(pending, roundDecisionId)
        .toRight(PowerAnswers.missing(roundDecisionId))
      winner <- ChronicleTasks.chosenPlayer(pending, winnerDecisionId)
    yield Vector(WritePrediction(player, round, winner)), required = true),
    Note(id, _ => Some(wrote.sourceless(NoteArg.Player(player),
      NoteArg.Card(ConspiratorCard.id)))),
    Note(id, states => states.now.game.current.predictions.get(player)
      .map(prediction => predicted.sourceless(NoteArg.Player(player),
        NoteArg.Player(prediction.winner), NoteArg.Number(prediction.round))
        .readBy(Vector(player)))))))
