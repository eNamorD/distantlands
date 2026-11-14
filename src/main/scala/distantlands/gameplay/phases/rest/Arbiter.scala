package distantlands.gameplay.phases.rest

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.EmpireEndings
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.model._

object ArbiterCard extends Legacy(LegacyId("L23"), "Arbiter",
    LegacyGoal.DarkestSecret, Suit.Discord):
  val power = PrintedPower(PowerId("legacy.arbiter"),
    persistent = false, cost = Cost.free,
    text = "**END OF ROUND:** Before the end die is rolled, you may " +
      "choose to modify the roll by −1 to +3.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Arbiter (legacy L23): "End of round: Before the end die is rolled, you
  * may choose to modify the roll by −1 to +3" (Legacy in-play powers
  * design, ruling 21). Hooks no window: the driver starts [[ArbiterStep]]
  * before the roll. Registered so the card counts as implemented. */
case object Arbiter extends ContributingPower:
  val id: PowerId = ArbiterCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** Arbiter's step, a triggered walker procedure the driver starts at a
  * round's end when the end die will be rolled and a seated player holds
  * Arbiter active, before `TurnBoundary.finishRound` rolls it.
  *
  * {{{
  * Sequence(
  *   Decide(round-end.legacy.arbiter.<holder>, holder,
  *     ChooseOne(Decline, -1, +1, +2, +3)),               // public (R13)
  *   BuildOps(SetEndDieModifier(n) unless Decline),
  *   Note("p2 modified the end die by +2 with Arbiter"))  // not on Decline
  * }}}
  *
  * The question is asked whatever the die could change, so its shape reads
  * only who holds Arbiter, which the walk does not change; `build` is also
  * the rebuild. The roll reads the modifier
  * (`CurrentGameState.endDieModifier`, R11) and clears it.
  */
object ArbiterStep:
  def decisionId(player: PlayerId): String =
    s"round-end.legacy.arbiter.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")
  /** The modifiers offered, in the order asked after Decline (R13). */
  val Amounts: Vector[Int] = Vector(-1, 1, 2, 3)
  def by(amount: Int): DecisionOptionRef.Button =
    DecisionOptionRef.Button(s"modify-$amount")

  private val procedure = TriggeredProcedureRef.Arbiter
  private val noteId = ProcedureNotes.id(procedure)
  private val notes = NoteSet(noteId)
  /** "{p2} modified the end die by +{2} with {Arbiter}" (R12). */
  val raised: NoteKey = notes.key("raised", Vector(NotePart.Arg(0),
    NotePart.Text(" modified the end die by +"), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  /** "{p2} modified the end die by -{1} with {Arbiter}" (R12). */
  val lowered: NoteKey = notes.key("lowered", Vector(NotePart.Arg(0),
    NotePart.Text(" modified the end die by -"), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The holder asked now: the round is at its end, the end die will be
    * rolled, and a seated player holds Arbiter active. */
  def due(ready: ReadyGame): Option[PlayerId] =
    Option.when(ready.game.current.turn.phase == Phase.RoundEnd &&
      ready.game.current.result.isEmpty &&
      EmpireEndings.rollsEndDie(ready))(()).flatMap(_ =>
      ChronicleLegacyRules.holder(ready, ArbiterCard.id))

  private def label(amount: Int): String =
    if amount > 0 then s"+$amount" else s"-${-amount}"

  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- Either.cond(args.isEmpty, (), OathViolation.InvalidEventOrder(
        "Arbiter's step selects nothing"))
      holder <- due(state).toRight(OathViolation.InvalidEventOrder(
        "no Arbiter's step is due"))
    yield
      val id = decisionId(holder)
      def chosen(answered: Vector[Answered]): Option[Int] =
        PowerAnswers.last(answered, id).flatMap(ref =>
          Amounts.find(by(_) == ref))
      Sequence(Vector(
        Decide(id, holder, DecisionQuery.ChooseOne(
          DecisionOption.Button(decline, "Decline") +: Amounts.map(amount =>
            DecisionOption.Button(by(amount), label(amount))),
          heading = Some("Arbiter: modify the end die before it is " +
            "rolled?"))),
        BuildOps((_, pending) => Right(chosen(pending.answered).map(amount =>
          SetEndDieModifier(amount): CoreOperation).toVector),
          required = true),
        Note(noteId, states => chosen(states.answered).map(amount =>
          (if amount > 0 then raised else lowered).sourceless(
            NoteArg.Player(holder), NoteArg.Number(amount.abs),
            NoteArg.Card(ArbiterCard.id))))))
