package oathdigital.gameplay.powers.search

import oathdigital.catalog.{ModifierMark, PrintedPower, Relic}
import oathdigital.gameplay.actions.search.SearchProcedure
import oathdigital.model._

object RingOfDevotionCard extends Relic(RelicId("R12"), "Ring of Devotion", value = 90, defense = 1):
  val power = PrintedPower(PowerId("relic.ring-of-devotion"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Search)),
    text = "You may discard cards to your region's discard pile instead " +
      "of the next region out.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Ring of Devotion (relic R12), a free selected Search modifier: "You may
  * discard cards to your region's discard pile instead of the next region
  * out." (catalog batch 10, slice 9).
  *
  * Every discard of the Search goes to the pile of the region the user's
  * pawn stands in ([[SearchDiscards]]). The pawn does not move during a
  * Search. Its id was misspelled `relic.king-of-devotion` in the card data;
  * old journals need not decode. The generic discard lines name the pile,
  * so it writes no line.
  */
case object RingOfDevotion extends SearchDiscards:
  val id: PowerId = RingOfDevotionCard.power.id
  val card: Relic = RingOfDevotionCard

  def redirected(ready: ReadyGame, answered: Vector[Answered],
      actor: PlayerId)(discard: SearchDiscards.UserDiscard)
      : Vector[CoreOperation] =
    SearchProcedure.actorRegion(ready, actor).toOption.fold(
        Vector[CoreOperation](discard)) { region =>
      discard match
        case denizen: Discard.Denizen => Vector(denizen.copy(to = region))
        case vision: Discard.Vision => Vector(vision.copy(to = region))
    }
