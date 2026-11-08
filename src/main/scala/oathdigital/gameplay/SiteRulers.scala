package oathdigital.gameplay

import oathdigital.model._

/** Who rules a site, for the powers whose reach is "the sites this card's
  * ruler rules": Toll Roads, Grasping Vines and Secret Police; a card's own
  * ruler is `CardRule`'s. It also names who may consent for a card's power
  * (`consentGranterAt`). The Empire is covered through
  * `SiteRule.rulingPlayer`.
  * Whether a player is an enemy of a ruler is `SiteRule.enemies(ruler,
  * SiteRuler.Player(player))`, the model's single definition.
  */
private[gameplay] object SiteRulers:

  def rulerOf(ready: ReadyGame, site: SiteId): Option[SiteRuler] =
    SiteRule.rulingPlayer(ready, site)

  /** Who may consent for a power of a card at `site`: the player who rules
    * it, or the Bandit Crown's holder when bandits rule it (catalog batch
    * 11, rulings 5 and 6). Nobody at an unruled site. The Empire's sites
    * resolve to its governor. A consent ask drops a granter who is the
    * requester, so the holder never lets themselves off. */
  def consentGranterAt(ready: ReadyGame, site: SiteId): Option[PlayerId] =
    rulerOf(ready, site).flatMap:
      case SiteRuler.Player(player) => Some(player)
      case SiteRuler.Bandits => BanditCrownRule.holder(ready)
      case _ => None

  /** [[consentGranterAt]] the site where `card` stands faceup. */
  def consentGranter(ready: ReadyGame, card: DenizenId): Option[PlayerId] =
    ready.game.current.faceUpDenizenSite(card)
      .flatMap(consentGranterAt(ready, _))

  /** The players a protection printed on `card` at `site` covers, for a
    * card that says "If ruled by Empire, all Imperials have this power"
    * (Forgotten Vault, Oaken Fortress): the Grand Mask's wearer alone
    * while they rule the card (`CardRule.rulerAt`); else every Imperial
    * player while the Empire holds the site, or the player who rules it. */
  def protectedBy(ready: ReadyGame, site: SiteId, card: CardId)
      : Set[PlayerId] =
    CardRule.rulerAt(ready, site, card) match
      case Some(SiteRuler.Player(wearer))
          if GrandMaskRule.maskingAt(ready, site).contains(wearer) =>
        Set(wearer)
      case _ => ready.game.current.map.sites.get(site).flatMap(state =>
        SiteRule.ruler(state.forces, ready.game.current.players).toOption)
        match
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
