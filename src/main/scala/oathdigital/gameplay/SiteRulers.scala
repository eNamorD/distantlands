package oathdigital.gameplay

import oathdigital.model._

/** Who rules a site, for the powers whose reach is "the sites this card's
  * ruler rules": Toll Roads, Grasping Vines and Secret Police; a card's own
  * ruler is `CardRule`'s. It also names who may consent for a card's power
  * (`consentGranter`, `consentGranterOf`; a site's, `consentGranterAt`).
  * The Empire is covered through `SiteRule.rulingPlayer`.
  * Whether a player is an enemy of a ruler is `SiteRule.enemies(ruler,
  * SiteRuler.Player(player))`, the model's single definition.
  */
private[gameplay] object SiteRulers:

  def rulerOf(ready: ReadyGame, site: SiteId): Option[SiteRuler] =
    SiteRule.rulingPlayer(ready, site)

  /** Who may consent for a power whose ruler is `ruler`: that player, or
    * the Bandit Crown's holder when bandits rule (catalog batch 11, rulings
    * 5 and 6). Nobody for an unruled site or an Empire with no
    * Chancellor. A consent ask drops a granter who is the requester, so
    * nobody lets themselves off. */
  private def granterFor(ready: ReadyGame, ruler: SiteRuler)
      : Option[PlayerId] = ruler match
    case SiteRuler.Player(player) => Some(player)
    case SiteRuler.Bandits => BanditCrownRule.holder(ready)
    case _ => None

  /** Who may consent for a power that reads `site`'s ruler: Hidden
    * Passages (ruling 15). The Empire's sites resolve to its governor. */
  def consentGranterAt(ready: ReadyGame, site: SiteId): Option[PlayerId] =
    rulerOf(ready, site).flatMap(granterFor(ready, _))

  /** Who may consent for a power of `card`, which stands at `site`: its
    * ruler (`CardRule.rulerAt`), so the Grand Mask's wearer during their
    * turn. */
  def consentGranterOf(ready: ReadyGame, site: SiteId, card: CardId)
      : Option[PlayerId] =
    CardRule.rulerAt(ready, site, card).flatMap(granterFor(ready, _))

  /** Who may consent for a power of the faceup denizen `card`. */
  def consentGranter(ready: ReadyGame, card: DenizenId): Option[PlayerId] =
    CardRule.rulerOf(ready, card).flatMap(granterFor(ready, _))

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
    ready.game.current.map.inPlay.filter(isImperialSite(ready, _))

  /** Whether the Empire's warbands hold `site`. */
  def isImperialSite(ready: ReadyGame, site: SiteId): Boolean =
    ready.game.current.map.sites.get(site).exists(state =>
      SiteRule.ruler(state.forces, ready.game.current.players) ==
        Right(SiteRuler.Empire))
