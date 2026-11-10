package distantlands.gameplay.actions.negotiation

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.{BanditCrownRule, PowerAccess, RuleSourceFace}
import distantlands.gameplay.operations.PowerUseRules
import distantlands.gameplay.powerresolver.{PowerUse, PowerUseRestriction}
import distantlands.model._

/** Who a Negotiation reaches beyond the actor's site (catalog batch 12,
  * rulings 7 to 9, 24 and 25). The Negotiation reads it, so it sits beside
  * the action, outside `powers`; the three powers that register the cards
  * hook no window.
  *
  *  - **Traveling Negotiator** (faceup adviser): its holder reaches any
  *    player, and any player reaches its holder.
  *  - **The Tribunal** (intact): a player with access to it (its site,
  *    a site they rule, or another route `PowerAccess` grants) reaches
  *    any player, and any player reaches them.
  *  - **Bandit Tribunal** (ruined, persistent): while it is on the map,
  *    two players whose pawns stand at bandit-ruled sites reach each other.
  *
  * The two non-persistent powers pass `restrictions`, the walk's power-use
  * restrictions (City Wall, Giant Python, Bandit Forest), judged as the use
  * of the player who holds the card or has the access (ruling 25). A grant
  * remembered this turn lifts a refusal; the Negotiation itself asks for
  * none. Bandit Tribunal ignores access and the blockers (CR "Persistent
  * Powers").
  */
object NegotiationReach:
  def reaches(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      other: PlayerId, restrictions: Vector[PowerUseRestriction]): Boolean =
    travelingNegotiator(catalog, ready, actor, other, restrictions) ||
      tribunalUsers(catalog, ready, Vector(actor, other), restrictions)
        .nonEmpty ||
      banditTribunal(ready, actor, other)

  /** Whether one of the two holds Traveling Negotiator faceup and may use
    * it. */
  def travelingNegotiator(catalog: ExecutableCatalog, ready: ReadyGame,
      actor: PlayerId, other: PlayerId,
      restrictions: Vector[PowerUseRestriction]): Boolean =
    PowerAccess.faceupAdviserHolder(ready, TravelingNegotiatorCard.id)
      .exists(holder => (holder == actor || holder == other) &&
        usable(catalog, ready, PowerUse.of(holder,
          TravelingNegotiatorCard.power, TravelingNegotiatorCard.id),
          restrictions))

  /** The players among `players` with access to The Tribunal (intact) who
    * may use it, in the order given: they reach any player, and a deal
    * holding one of them takes binding promises (ruling 11). */
  def tribunalUsers(catalog: ExecutableCatalog, ready: ReadyGame,
      players: Vector[PlayerId],
      restrictions: Vector[PowerUseRestriction]): Vector[PlayerId] =
    ready.game.current.edificeSite(TheTribunalCard.id, EdificeSide.Intact)
      .toVector.flatMap(site => players.filter(player =>
        PowerAccess.accessible(RuleSourceRef.Edifice(site, TheTribunalCard.id),
          RuleSourceFace.Intact, ready, player) &&
          usable(catalog, ready, PowerUse.of(player,
            TheTribunalCard.intact.power, TheTribunalCard.id), restrictions)))

  /** Whether Bandit Tribunal is on the map and both pawns stand at
    * bandit-ruled sites. */
  def banditTribunal(ready: ReadyGame, actor: PlayerId,
      other: PlayerId): Boolean =
    val current = ready.game.current
    lazy val bandits = BanditCrownRule.banditSites(ready)
    current.edificeSite(TheTribunalCard.id, EdificeSide.Ruined).isDefined &&
      Vector(actor, other).forall(player =>
        current.pawnSite(player).exists(bandits))

  private def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      use: PowerUse, restrictions: Vector[PowerUseRestriction]): Boolean =
    PowerUseRules.refusals(catalog, ready, use, restrictions).isEmpty
