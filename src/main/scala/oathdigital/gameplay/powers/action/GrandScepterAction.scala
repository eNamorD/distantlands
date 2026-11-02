package oathdigital.gameplay.powers.action

import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powerresolver.QueuedStarts
import oathdigital.gameplay.roles.Citizenship
import oathdigital.model._

/** The Grand Scepter's ACTION: "You may become a Citizen if an Exile"
  * (CR p. 34). In a game with no Empire the holder becomes the Chancellor
  * instead. An Imperial holder's menu leaves it out; under the first-game
  * option an Exile holder sees it disabled (ambiguity F8).
  *
  * Its line names the new role: "Became a Citizen." */
case object GrandScepterAction extends PaidAction(TheGrandScepterCard.action):
  private val notes = NoteSet(id)
  val becameCitizen: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Became a Citizen.")))
  val becameChancellor: NoteKey = notes.key("used.chancellor", Vector(
    NotePart.Text("Became the Chancellor.")))
  override def noteKeys: Vector[NoteKey] = notes.declared

  override def listed(ready: ReadyGame, player: PlayerId): Boolean =
    !Imperials.isImperial(ready, player)

  override def usable(ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Boolean = listed(ready, player)

  override def refusal(ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef, starts: QueuedStarts): Option[String] =
    Option.when(Citizenship.ignored(ready))(Citizenship.ignoredReason)

  def build(ready: ReadyGame, player: PlayerId, source: DecisionOptionRef)
      : Either[OathViolation, Operation] = source match
    case DecisionOptionRef.Relic(TheGrandScepterCard.id) =>
      Right(Sequence(Vector(Citizenship.become(player),
        Note(id, becameNote(_, player, source)))))
    case other => Left(OathViolation.InvalidEventOrder(
      s"${other.kind} is not the Grand Scepter"))

  private def becameNote(states: NoteStates, player: PlayerId,
      source: DecisionOptionRef): Option[LogNote] = for
    card <- PowerSourceRef.of(source)
    role <- Imperials.role(states.now, player)
    if role.isImperial
  yield if role == Role.Chancellor then becameChancellor(card)
    else becameCitizen(card)
