package distantlands.gameplay.setup.legacies

import distantlands.catalog.{Legacy, LegacyGoal, PrintedPower}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferResolution}
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.setup.SetupPawnPlacement
import distantlands.model._

object WealthyCard extends Legacy(LegacyId("L31"), "Wealthy",
    LegacyGoal.MostOfSuit, Suit.Hearth, Some(FoundationNumber.II)):
  val power = PrintedPower(PowerId("legacy.wealthy"),
    persistent = false, cost = Cost.free,
    text = "Set Foundation II to Movers and Shakers. **SETUP:** After " +
      "placing your pawn, take [favor] from two players or take " +
      "[favor] [favor] [favor] from the shared bank.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Wealthy (legacy L31), its SETUP power (Legacy Setup powers design,
  * ruling 17). In the holder's own `SetupPawnPlaced` window, in the order
  * the holder chooses among its powers (catalog batch 13, ruling 4), with
  * no "may": 1 favor from each of two other
  * players, or 3 favor from the shared bank. "Two players" is offered only
  * when two other players hold favor, and with one option nothing is asked
  * (R29). Lines: "p2 took 1 favor from p1 and p3 with Wealthy", "p2 took 3
  * favor from the shared bank with Wealthy". */
case object Wealthy extends ContributingPower:
  val id: PowerId = WealthyCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      SetupPawnPlacement.latest(ctx.answered).map(_._1)
        .filter(player => SetupLegacyRules.wealthy(ctx.state).contains(player))
        .map(holder => ResolutionOffer(option, Vector(step(holder)))))))

  /** This legacy in the placer's power-order question. */
  val option: DecisionOption =
    DecisionOption.Legacy(DecisionOptionRef.Legacy(WealthyCard.id))

  def decisionId(player: PlayerId): String =
    s"setup.legacy.wealthy.${player.value}"
  def playersDecisionId(player: PlayerId): String =
    s"${decisionId(player)}.players"
  val fromPlayers: DecisionOptionRef.Button = DecisionOptionRef.Button("players")
  val fromBank: DecisionOptionRef.Button = DecisionOptionRef.Button("bank")

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.Setup)
  private val notes = NoteSet(noteId)
  val tookFromPlayers: NoteKey = notes.key("wealthy-took-players", Vector(
    NotePart.Arg(0), NotePart.Text(" took 1 favor from "), NotePart.Arg(1),
    NotePart.Text(" and "), NotePart.Arg(2), NotePart.Text(" with "),
    NotePart.Arg(3)))
  val tookFromBank: NoteKey = notes.key("wealthy-took-bank", Vector(
    NotePart.Arg(0), NotePart.Text(" took 3 favor from the shared bank with "),
    NotePart.Arg(1)))
  def setupNoteKeys: Vector[NoteKey] = notes.declared

  /** The other seated players holding favor, in seat order from the first
    * player, as Setup's turns run. */
  private def givers(state: ReadyGame, holder: PlayerId): Vector[PlayerId] =
    val seats = state.game.current.players.map(_.player)
    val start = seats.indexOf(state.setup.firstPlayer).max(0)
    (seats.drop(start) ++ seats.take(start)).filter(player =>
      player != holder &&
        state.game.current.player(player).exists(_.board.favor > 0))

  private def step(holder: PlayerId): Operation = Branch((state, _) =>
    // The holder is read again at the step, so a legacy lost earlier in
    // the window does not resolve (catalog batch 13, ruling 5).
    if !SetupLegacyRules.wealthy(state).contains(holder) then Vector.empty
    else
      val holding = givers(state, holder)
      val options = Option.when(holding.sizeIs >= 2)(DecisionOption.Button(
        fromPlayers, "Take 1 favor from two players")).toVector :+
        DecisionOption.Button(fromBank, "Take 3 favor from the shared bank")
      Option.when(options.sizeIs > 1)(Decide(decisionId(holder), holder,
        DecisionQuery.ChooseOne(options,
          heading = Some("Wealthy: take favor"))))
        .toVector :+ Branch((_, pending) =>
          if PowerAnswers.one(pending, decisionId(holder))
            .contains(fromPlayers)
          then fromTwo(holder, holding)
          else fromTheBank(holder)))

  /** 1 favor from each of two players: chosen by one choose-many of
    * exactly two, or, when exactly two hold favor, from both with no
    * question, since a choose-many of every option decides nothing. */
  private def fromTwo(holder: PlayerId, holding: Vector[PlayerId])
      : Vector[Operation] =
    val id = playersDecisionId(holder)
    def chosen(answered: Vector[Answered]): Vector[PlayerId] =
      if holding.sizeIs == 2 then holding
      else answered.collectFirst {
        case Answered(`id`, DecisionAnswer.ChooseManyAnswer(refs), _) => refs
      }.toVector.flatten.collect { case DecisionOptionRef.Player(player) =>
        player }
    Option.when(holding.sizeIs > 2)(
      Decide(id, holder, DecisionQuery.ChooseMany(2, 2,
        holding.map(player =>
          DecisionOption.Player(DecisionOptionRef.Player(player))),
        heading = Some("Wealthy: take 1 favor from two players"),
        confirm = Some(CountedConfirm("Take from", "player", "players")))))
      .toVector ++ Vector(
      BuildOps((_, pending) => Right(chosen(pending.answered).map(player =>
        Move(Piece.Favor(1), PositionedLocation(Location.PlayArea(player)),
          PositionedLocation(Location.PlayArea(holder))))), required = true),
      Note(noteId, states => chosen(states.answered) match
        case Vector(first, second) => Some(tookFromPlayers.sourceless(
          NoteArg.Player(holder), NoteArg.Player(first),
          NoteArg.Player(second), NoteArg.Card(WealthyCard.id)))
        case _ => None))

  private def fromTheBank(holder: PlayerId): Vector[Operation] = Vector(
    BuildOps((_, _) => Right(Vector(Move(Piece.Favor(3),
      PositionedLocation(Location.SharedBank),
      PositionedLocation(Location.PlayArea(holder))))), required = true),
    Note(noteId, _ => Some(tookFromBank.sourceless(NoteArg.Player(holder),
      NoteArg.Card(WealthyCard.id)))))
