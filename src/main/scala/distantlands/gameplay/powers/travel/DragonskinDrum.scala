package distantlands.gameplay.powers.travel

import distantlands.catalog.{ModifierMark, PrintedPower, Relic}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powerresolver.{Contribution, PowerCtx, Transform}
import distantlands.gameplay.powers.{PlayerFacts, SelectedModifier}
import distantlands.model._

object DragonskinDrumCard extends Relic(RelicId("R20"), "Dragonskin Drum", value = 59, defense = 2):
  val power = PrintedPower(PowerId("relic.dragonskin-drum"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Travel)),
    text = "After traveling, gain one warband.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Dragonskin Drum (relic R20), a selected Travel modifier: after traveling,
  * gain one warband. The gain follows the pawn's move in the same cost node, so
  * a Travel that is rejected gains nothing. The gain is best-effort, so an
  * empty warband bank gives nothing.
  *
  * Its line, "{Red} gained 1 warband.", reads the gain's step.
  */
case object DragonskinDrum extends SelectedModifier:
  val id: PowerId = DragonskinDrumCard.power.id
  val Warbands: Int = 1
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  val gained: NoteKey = NoteSupport.gainedKey(notes, "gained")

  val card: Relic = DragonskinDrumCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Travel)


  def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.TravelCost -> Vector(Transform((ctx, operations) =>
      operations :+ gain(ctx.actor) :+ Note(id,
        NoteSupport.gainedNote(DragonskinDrum.gained,
          PowerSourceRef.Card(cardId), ctx.actor, NoteUnit.Warband,
          NoteSupport.warbands)))))

  override def appliesAt(ctx: PowerCtx): Boolean =
    TravelRoute.pawnMove(ctx.operation).nonEmpty

  private def gain(actor: PlayerId): Operation = BuildOps((ready, _) =>
    PlayerFacts.forceKind(ready, actor).map(kind =>
      Vector(Gain.Warbands(actor, kind, DragonskinDrum.Warbands))))
