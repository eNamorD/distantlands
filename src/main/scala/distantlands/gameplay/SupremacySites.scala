package oathdigital.gameplay

import oathdigital.model._

/** Each player's count toward Supremacy, for the Oathkeeper title and the
  * Vision of Conquest (Steward of Imperial Domains, CR p. 35): an Exile's
  * own sites, the Empire's sites for the Chancellor, and none for a
  * Citizen, who never holds the Supremacy title. */
object SupremacySites:
  def counts(ready: ReadyGame): Vector[(PlayerId, Int)] =
    val current = ready.game.current
    def sitesOf(kind: ForceKind): Int =
      current.map.sites.values.count(_.forces match
        case SiteForces.Occupied(`kind`, _) => true
        case _ => false)
    current.players.map { player =>
      player.player -> (Imperials.role(ready, player.player) match
        case Some(Role.Citizen) => 0
        case _ => PlayerForceKind.of(ready, player).fold(0)(sitesOf))
    }
