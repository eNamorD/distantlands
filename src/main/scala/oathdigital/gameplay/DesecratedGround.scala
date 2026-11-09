package oathdigital.gameplay

import oathdigital.catalog.{Edifice, EdificeFace, Locked, ModifierMark,
  PrintedPower}
import oathdigital.model._

object SacredGroundCard extends Edifice(EdificeId("E08"), Suit.Nomad):
  object intact extends EdificeFace("Sacred Ground") with Locked:
    val power = PrintedPower(PowerId("edifice.e08.intact"),
      persistent = true, cost = Cost.free,
      modifier = Some(ModifierMark.Action(MajorActionType.Search)),
      text = "Players **cannot** play Visions, except the Conspiracy, " +
        "faceup unless their pawn is at this site.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Desecrated Ground"):
    val power = PrintedPower(PowerId("edifice.e08.ruined"),
      persistent = true, cost = Cost.free,
      text = "If an Exile reveals a Vision while their pawn is at this " +
        "site, they do not discard their current revealed Vision. " +
        "_They can have any number!_")
    val powers: Vector[PrintedPower] = Vector(power)

/** Desecrated Ground (E08's ruined face): an Exile who reveals a Vision
  * while their pawn is at its site keeps the Visions already revealed
  * (catalog batch 11, rulings 13 and 31). `CardSlots` and `CardPlay` read
  * `keeps`; the card play writes `kept`. It sits in `gameplay` because card
  * play may not import `gameplay.powers`; the power registered there hooks
  * no window.
  */
private[gameplay] object DesecratedGroundRule:
  val power: PowerId = SacredGroundCard.ruined.power.id

  private val notes = NoteSet(power)
  /** "Desecrated Ground: Kept Vision of Faith." */
  val kept: NoteKey = notes.key("kept", Vector(NotePart.Text("Kept "),
    NotePart.Arg(0), NotePart.Text(".")))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Whether `player` keeps their revealed Visions when they reveal one
    * now: they are an Exile and their pawn is at the site where E08
    * stands ruined, in play. */
  def keeps(ready: ReadyGame, player: PlayerId): Boolean =
    val current = ready.game.current
    Imperials.role(ready, player).contains(Role.Exile) && current
      .edificeSite(SacredGroundCard.id, EdificeSide.Ruined)
      .filter(current.map.inPlay.contains)
      .exists(site => current.pawnSite(player).contains(site))

  /** The line after a reveal: the player's other revealed Visions, if any
    * are left. A reveal elsewhere has just discarded them, so only a kept
    * Vision is named. */
  def keptNote(card: WorldCardId, ref: DecisionOptionRef)(
      states: NoteStates): Option[LogNote] = ref match
    case DecisionOptionRef.RevealedVision(owner) =>
      for
        player <- states.now.game.current.player(owner)
        others = player.revealedVisions.map(_.id).filterNot(_ == card)
        if others.nonEmpty
      yield kept(PowerSourceRef.Card(SacredGroundCard.id),
        NoteArg.Cards(others))
    case _ => None
