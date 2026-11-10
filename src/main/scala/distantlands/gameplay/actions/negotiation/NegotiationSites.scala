package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.PowerAccess
import distantlands.gameplay.powerresolver.{PowerUse, PowerUseRestriction}
import distantlands.model._

/** Site terms in a Negotiation deal (catalog batch 12, rulings 18 to 20 and
  * 27 to 29). The Negotiation reads it at its start and while the deal is
  * open, so it sits beside the action, outside `powers`; the power that
  * registers Deed Writer hooks no window.
  *
  * Deed Writer (faceup adviser) lets its holder give and receive sites
  * ([[DealRules.kinds]] opens the pairs that hold them). It is
  * non-persistent, so its use passes `restrictions`, the walk's power-use
  * restrictions (City Wall, Giant Python, Bandit Forest), judged as its
  * holder's own use. The Negotiation asks for no consent.
  */
object NegotiationSites:
  /** The players among `players` who hold Deed Writer faceup and may use
    * it: at most one. */
  def deedWriterUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    PowerAccess.faceupAdviserHolder(ready, DeedWriterCard.id).toVector
      .filter(holder => players.contains(holder) &&
        NegotiationReach.usable(catalog, ready, PowerUse.of(holder,
          DeedWriterCard.power, DeedWriterCard.id), restrictions))
