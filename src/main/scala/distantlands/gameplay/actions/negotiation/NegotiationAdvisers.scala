package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.{PowerAccess, RuleSourceFace}
import distantlands.gameplay.powerresolver.{PowerUse, PowerUseRestriction}
import distantlands.model._

/** Who may let advisers into a Negotiation deal (catalog batch 12,
  * rulings 14, 15 and 26). The Negotiation reads it at its start, so it
  * sits beside the action, outside `powers`; the two powers that register
  * the cards hook no window.
  *
  *  - **Council Arbiter** (faceup adviser): its holder may give and
  *    receive advisers ([[DealRules.kinds]] opens the pairs that hold
  *    them).
  *  - **Festival District** (intact): a player with access to it (its
  *    site, a site they rule, or another route `PowerAccess` grants) opens
  *    advisers between every pair of the deal.
  *
  * Both are non-persistent, so both pass `restrictions`, the walk's
  * power-use restrictions (City Wall, Giant Python, Bandit Forest), judged
  * as the use of the player who holds the card or has the access. The
  * Negotiation asks for no consent.
  */
object NegotiationAdvisers:
  /** The players among `players` who hold Council Arbiter faceup and may
    * use it: at most one. */
  def arbiterUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    PowerAccess.faceupAdviserHolder(ready, CouncilArbiterCard.id).toVector
      .filter(holder => players.contains(holder) &&
        NegotiationReach.usable(catalog, ready, PowerUse.of(holder,
          CouncilArbiterCard.power, CouncilArbiterCard.id), restrictions))

  /** The players among `players` with access to Festival District
    * (intact) who may use it, in the order given. */
  def festivalUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    ready.game.current.edificeSite(FestivalDistrictCard.id, EdificeSide.Intact)
      .toVector.flatMap(site => players.filter(player =>
        PowerAccess.accessible(
          RuleSourceRef.Edifice(site, FestivalDistrictCard.id),
          RuleSourceFace.Intact, ready, player) &&
          NegotiationReach.usable(catalog, ready, PowerUse.of(player,
            FestivalDistrictCard.intact.power, FestivalDistrictCard.id),
            restrictions)))
