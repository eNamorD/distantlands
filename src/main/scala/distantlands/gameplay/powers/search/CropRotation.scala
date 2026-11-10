package distantlands.gameplay.powers.search

import distantlands.catalog.{Denizen, ModifierMark, PrintedPower}
import distantlands.gameplay.actions.{CardPlayRules, DiscardPermission}
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.powerresolver.{Contribution, Transform}
import distantlands.gameplay.powers.SelectedModifier
import distantlands.model._

object CropRotationCard extends Denizen(DenizenId("128"), "Crop Rotation", Suit.Hearth):
  val power = PrintedPower(PowerId("denizen.crop-rotation"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "If playing to a site, you may discard a denizen there first.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Crop Rotation (card 128), a selected Search modifier with no cost: "If
  * playing to a site, you may discard a denizen there first."
  *
  * It adds one site discard permission through the same
  * `SearchPlayAdviser` window as the Mob's, so the two stack with each other
  * and with the Homeland rule, each allowing one discard. The
  * Play-Facedown-Adviser action selects its modifiers at the Search's window,
  * so it applies there too. An edifice is a denizen for placement, so a
  * ruined one may go. The generic discard rules decide which cards may go:
  * an intact edifice is locked, and Crop Rotation itself, being selected, is
  * refused by the active-modifier restriction.
  *
  * Its line, "May discard a card at {site} first.", travels in the
  * permission, and card play writes it for the discard it allowed.
  */
case object CropRotation extends SelectedModifier:
  val id: PowerId = CropRotationCard.power.id

  val card: Denizen = CropRotationCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Search)
  private val notes = NoteSet(id)
  notes.include(CardPlayRules.notes)
  override def noteKeys: Vector[NoteKey] = notes.declared

  def effects: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SearchPlayAdviser -> Vector(Transform((ctx, children) =>
      ctx.operation match {
        case tree: CardPlayProcedure.PlacementTree =>
          tree.adjust(children)(_.permitSiteDiscard(permission))
        case _ => children
      })))

  /** One discard of a site card, credited with Crop Rotation's line. An
    * edifice is a denizen for placement, so a ruined one may go. */
  private def permission: DiscardPermission =
    DiscardPermission.lined(id, PowerSourceRef.Card(cardId))
