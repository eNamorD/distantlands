package oathdigital.gameplay

import oathdigital.model._

/** Who rules a site, and where a rule card stands, for the powers whose reach
  * is "the sites this card's ruler rules": Toll Roads, Grasping Vines and
  * Secret Police. The Empire is covered through `SiteRule.rulingPlayer`.
  * Whether a player is an enemy of a ruler is `SiteRule.enemies(ruler,
  * SiteRuler.Player(player))`, the model's single definition.
  */
private[gameplay] object SiteRulers:

  def rulerOf(ready: ReadyGame, site: SiteId): Option[SiteRuler] =
    SiteRule.rulingPlayer(ready, site)

  /** The ruler of the site holding `card`, when that is a player or bandits.
    * The Empire resolves to its governor; it stays the Empire only with no
    * Chancellor, which rules nothing.
    */
  def rulerOfCard(ready: ReadyGame, card: DenizenId): Option[SiteRuler] =
    ready.game.current.faceUpDenizenSite(card).flatMap(rulerOf(ready, _))
      .filter:
      case SiteRuler.Player(_) | SiteRuler.Bandits => true
      case _ => false

  /** The players a protection printed at `site` covers, for a card that
    * says "If ruled by Empire, all Imperials have this power" (Forgotten
    * Vault, Oaken Fortress): every Imperial player while the Empire holds
    * the site, else the player who rules it. */
  def protectedAt(ready: ReadyGame, site: SiteId): Set[PlayerId] =
    ready.game.current.map.sites.get(site).flatMap(state =>
      SiteRule.ruler(state.forces, ready.game.current.players).toOption) match
      case Some(SiteRuler.Empire) => Imperials.players(ready).toSet
      case Some(SiteRuler.Player(player)) => Set(player)
      case _ => Set.empty

  /** The Imperial sites in play, in map order: the sites the Empire holds
    * with its warbands (rules: "Sites with Imperial warbands are called
    * Imperial sites"). */
  def imperialSites(ready: ReadyGame): Vector[SiteId] =
    ready.game.current.map.inPlay.filter(site =>
      ready.game.current.map.sites.get(site).exists(state =>
        SiteRule.ruler(state.forces, ready.game.current.players) ==
          Right(SiteRuler.Empire)))
