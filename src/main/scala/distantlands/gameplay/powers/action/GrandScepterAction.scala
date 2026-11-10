package distantlands.gameplay.powers.action

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.roles.{Citizenship, RoleNotes}
import distantlands.model._

/** The Grand Scepter's ACTION: "You may become a Citizen if an Exile"
  * (CR p. 34). In a game with no Empire the holder becomes the Chancellor
  * instead. An Imperial holder's menu leaves it out; under the first-game
  * option an Exile holder sees it disabled (ambiguity F8).
  *
  * Its line names the new role: "Became a Citizen." */
case object GrandScepterAction extends PaidAction(TheGrandScepterCard.action):
  private val notes = NoteSet(id)
  val roleNotes: RoleNotes = RoleNotes.declare(notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  override def listed(ready: ReadyGame, player: PlayerId): Boolean =
    !Imperials.isImperial(ready, player)

  override def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef): Boolean = listed(ready, player)

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts): Option[String] =
    Option.when(Citizenship.ignored(ready))(Citizenship.ignoredReason)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] = source match
    case DecisionOptionRef.Relic(TheGrandScepterCard.id) =>
      Right(Sequence(Vector(Citizenship.become(player),
        Note(id, states => PowerSourceRef.of(source).flatMap(
          roleNotes.became(states, player, _))))))
    case other => Left(OathViolation.InvalidEventOrder(
      s"${other.kind} is not the Grand Scepter"))

