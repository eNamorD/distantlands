package oathdigital.gameplay.actions.minor

import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.roles.Citizenship
import oathdigital.model._
import oathdigital.model.OathViolation._

/** Self-exile (CR p. 34): as a minor action a Citizen gives the Chancellor
  * 1 favor per Imperial warband on their board, but no less than 3, and is
  * exiled. A Citizen without that much favor sees the option disabled. */
object SelfExile:
  val Minimum: Int = 3
  private val notes = NoteSet(ProcedureNotes.id(ActionRef.SelfExile))
  /** "Gave {3 favor} to {Purple} and became an Exile." */
  val left: NoteKey = notes.key("left", Vector(NotePart.Text("Gave "),
    NotePart.Arg(0), NotePart.Text(" to "), NotePart.Arg(1),
    NotePart.Text(" and became an Exile.")))
  def keys: Vector[NoteKey] = notes.declared

  def price(ready: ReadyGame, actor: PlayerId): Int =
    math.max(Minimum, ready.game.current.players.find(_.player == actor)
      .fold(0)(_.board.warbands))

  val build: MinorActionProcedures.Build = (_, ready, actor, args) => for
    _ <- MinorActionProcedures.started(ready, actor, args,
      ActionRef.SelfExile)
    _ <- Either.cond(Imperials.role(ready, actor).contains(Role.Citizen), (),
      MinorActionUnavailable("Only a Citizen exiles themself"))
    chancellor <- Imperials.chancellor(ready).toRight(
      MinorActionUnavailable("There is no Chancellor to pay"))
    state <- ready.game.current.players.find(_.player == actor)
      .toRight(WrongPlayer(ready.game.current.turn.activePlayer, actor))
    cost = price(ready, actor)
    _ <- Either.cond(state.board.favor >= cost, (), MinorActionUnavailable(
      s"You need $cost favor to give the Chancellor"))
  yield tree(actor, chancellor, cost)

  /** The tree never parks, so a rebuild runs only on replay of the
    * starting command, while the board is still as it was. */
  val rebuild: MinorActionProcedures.Build = (_, ready, actor, _) =>
    Imperials.chancellor(ready).toRight(MinorActionUnavailable(
      "There is no Chancellor to pay")).map(chancellor =>
      tree(actor, chancellor, price(ready, actor)))

  private def tree(actor: PlayerId, chancellor: PlayerId, cost: Int)
      : Operation = Sequence(Vector(
    BuildOps((_, _) => Right(Vector(Give(Piece.Favor(cost), actor,
      Location.PlayArea(actor), Location.PlayArea(chancellor),
      required = true))), required = true),
    Citizenship.exile(actor),
    Note(ProcedureNotes.id(ActionRef.SelfExile), states =>
      Option.when(Imperials.role(states.now, actor).contains(Role.Exile))(
        left.sourceless(NoteArg.Amount(cost, NoteUnit.Favor),
          NoteArg.Player(chancellor))))))
