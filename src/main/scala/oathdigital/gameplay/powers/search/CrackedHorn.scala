package oathdigital.gameplay.powers.search

import oathdigital.catalog.{ModifierMark, PrintedPower, Relic}
import oathdigital.model._

object CrackedHornCard extends Relic(RelicId("R10"), "Cracked Horn", value = 21, defense = 1):
  val power = PrintedPower(PowerId("relic.cracked-horn"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "You may put all of the cards you discard on the bottom of " +
      "the world deck.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Cracked Horn (relic R10), a free selected Search modifier: "You may put
  * all of the cards you discard on the bottom of the world deck." (catalog
  * batch 10, slice 9).
  *
  * Every discard of the Search ([[SearchDiscards]]) becomes a bury to the
  * bottom of the world deck, with the returns a discard makes: favor to the
  * card's suit bank and secrets to the user, facedown. A buried Vision
  * leaves the Visions Drawn track alone (Bury FAQ). The bury keeps the
  * discard's `required`. The generic "Buried {card}" line tells each one,
  * so it writes no line of its own.
  */
case object CrackedHorn extends SearchDiscards:
  val id: PowerId = CrackedHornCard.power.id
  val card: Relic = CrackedHornCard

  def redirected(ready: ReadyGame, answered: Vector[Answered],
      actor: PlayerId)(discard: SearchDiscards.UserDiscard)
      : Vector[CoreOperation] = discard match
    case denizen: Discard.Denizen =>
      Bury.standard(BuryableCard.Denizen(denizen.card), denizen.from,
        Some(denizen.suit), denizen.favor, denizen.secrets,
        denizen.actingPlayer).map {
        case bury: Bury => bury.copy(required = denizen.required)
        case other => other
      }
    case vision: Discard.Vision =>
      Vector(Bury(BuryableCard.Vision(vision.card), vision.from,
        required = vision.required))
