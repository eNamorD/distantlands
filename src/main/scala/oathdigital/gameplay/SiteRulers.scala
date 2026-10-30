package oathdigital.gameplay

import oathdigital.model._

/** Who rules a site, and where a rule card stands, for the powers whose reach
  * is "the sites this card's ruler rules": Toll Roads, Grasping Vines and
  * Secret Police. The Empire is covered through `SiteRule.rulingPlayer`.
  * Whether a player is an enemy of a ruler is
  * `SiteRule.enemies(ruler, SiteRuler.Player(player))`, the model's single
  * definition.
  */
private[gameplay] object SiteRulers:

  /** The site holding `card` faceup. */
  def siteOf(ready: ReadyGame, card: DenizenId): Option[SiteId] =
    ready.game.current.map.sites.collectFirst:
      case (id, site) if site.denizens.exists {
        case DenizenState(`card`, Orientation.FaceUp, _) => true
        case _ => false
      } => id

  def rulerOf(ready: ReadyGame, site: SiteId): Option[SiteRuler] =
    SiteRule.rulingPlayer(ready, site)

  /** The ruler of the site holding `card`, when that is a player or bandits.
    * The Empire resolves to its governor; it stays the Empire only with no
    * Chancellor, which rules nothing.
    */
  def rulerOfCard(ready: ReadyGame, card: DenizenId): Option[SiteRuler] =
    siteOf(ready, card).flatMap(rulerOf(ready, _)).filter:
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
