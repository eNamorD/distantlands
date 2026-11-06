package oathdigital.gameplay.powers.economy

import oathdigital.catalog.{Denizen, ExecutableCatalog}
import oathdigital.gameplay.{ActingTroupeCard, NoteSupport, PowerAccess}
import oathdigital.gameplay.powerresolver.{AsksOnSelection, Contribution}
import oathdigital.gameplay.powers.SelectedModifier
import oathdigital.model._

/** Acting Troupe (card 36, adviser-only), a free selected Trade modifier:
  * "Act as if Acting Troupe is a [suit-beast] or [suit-order] card
  * instead." (catalog batch 10, slice 7).
  *
  * It is offered while the Troupe is its user's faceup adviser. Picked, it
  * asks Beast or Order, in the same pass of the modifier decision, as
  * `ActingSite` asks its site. The answer is the whole effect:
  * `EffectiveSuit` reads it, so every reader of the Trade's matching
  * advisers counts the Troupe as that suit, beside a faceup Pledge to
  * Discord too (owner ruling, 2026-11-05). It hooks no window.
  *
  * Its line follows the answer: "Acting Troupe acted as a Beast card." or
  * "... as an Order card."
  */
case object ActingTroupe extends SelectedModifier, AsksOnSelection:
  val id: PowerId = ActingTroupeCard.power.id
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "{Acting Troupe} acted as a Beast card." */
  val actedBeast: NoteKey = notes.key("acted-beast", Vector(
    NotePart.Arg(0), NotePart.Text(" acted as a Beast card.")))
  /** "{Acting Troupe} acted as an Order card." */
  val actedOrder: NoteKey = notes.key("acted-order", Vector(
    NotePart.Arg(0), NotePart.Text(" acted as an Order card.")))

  val card: Denizen = ActingTroupeCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Trade)
  def effects: Map[PowerWindow, Vector[Contribution]] = Map.empty
  override def narratedDecisions: Set[String] =
    Set(ActingTroupeCard.suitDecision)

  /** Only as its user's faceup adviser, never from a site. */
  override def selectable(ready: ReadyGame, actor: PlayerId): Boolean =
    PowerAccess.holdsFaceupAdviser(ready, actor, ActingTroupeCard.id)

  /** The suit question, then the line. */
  def selectionQuestions(catalog: ExecutableCatalog,
      ready: ReadyGame, actor: PlayerId): Vector[Operation] = Vector(
    Decide(ActingTroupeCard.suitDecision, actor, DecisionQuery.ChooseOne(
      ActingTroupeCard.suits.map(suit =>
        DecisionOption.Button(ActingTroupeCard.button(suit), suit.toString)),
      heading = Some("Acting Troupe: act as which suit?"))),
    Note(id, actedNote))

  private def actedNote(states: NoteStates): Option[LogNote] =
    val troupe = NoteArg.Card(cardId)
    NoteSupport.answer(states, ActingTroupeCard.suitDecision).flatMap {
      case ref if ref == ActingTroupeCard.button(Suit.Beast) =>
        Some(actedBeast(PowerSourceRef.Card(cardId), troupe))
      case ref if ref == ActingTroupeCard.button(Suit.Order) =>
        Some(actedOrder(PowerSourceRef.Card(cardId), troupe))
      case _ => None
    }
