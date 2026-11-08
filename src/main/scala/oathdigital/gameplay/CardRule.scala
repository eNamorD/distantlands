package oathdigital.gameplay

import oathdigital.catalog.{ModifierMark, PrintedPower}
import oathdigital.gameplay.cards.NewFoundations
import oathdigital.model._

/** Who rules a card at a site (catalog batch 11, slice 2): the site's
  * ruling player (`SiteRule.rulingPlayer`), except that while the Grand
  * Mask works its wearer rules every card at an Imperial site but a battle
  * plan (`GrandMaskRule`). Every reading of "this card's ruler", "you rule
  * this card" or "cards ruled by" goes through here; a reading of the site
  * itself (Supremacy, Campaign defenders, the Chronicle, "sites ruled by")
  * reads `SiteRulers.rulerOf` or `SiteRule`.
  *
  * A face is a battle plan when a power it prints carries the battle-plan
  * mark, which `CardCatalogSuite` holds equal to the plan catalog. The
  * printed powers come from `NewFoundations.catalog`, the only catalog the
  * engine runs, because a waiver's granter sees the state alone.
  */
private[gameplay] object CardRule:
  /** Who rules `card`, which stands at `site`. Like
    * `SiteRule.rulingPlayer`, it may be the Empire or `Unruled`. */
  def rulerAt(ready: ReadyGame, site: SiteId, card: CardId)
      : Option[SiteRuler] =
    val masked = for
      wearer <- GrandMaskRule.maskingAt(ready, site)
      state <- ready.game.current.map.sites.get(site)
        .flatMap(_.denizens.find(_.id == card))
      if !battlePlan(state)
    yield SiteRuler.Player(wearer)
    masked.orElse(SiteRule.rulingPlayer(ready, site))

  /** Who rules the faceup denizen `card` at a site, when that is a player
    * or bandits. The Empire resolves to its governor; it stays the Empire
    * only with no Chancellor, which rules nothing. */
  def rulerOf(ready: ReadyGame, card: DenizenId): Option[SiteRuler] =
    ready.game.current.faceUpDenizenSite(card)
      .flatMap(rulerAt(ready, _, card)).filter:
      case SiteRuler.Player(_) | SiteRuler.Bandits => true
      case _ => false

  private def battlePlan(state: SiteDenizenState): Boolean =
    printed(state).exists(_.modifier.exists {
      case _: ModifierMark.BattlePlan => true
      case _ => false
    })

  /** The powers `state`'s face up now prints. */
  private def printed(state: SiteDenizenState): Vector[PrintedPower] =
    val catalog = NewFoundations.catalog
    state match
      case DenizenState(id, _, _) =>
        catalog.denizen(id).toVector.flatMap(_.powers)
      case EdificeState(id, side, _) =>
        catalog.edifice(id).toVector.flatMap(edifice =>
          (if side == EdificeSide.Intact then edifice.intact
           else edifice.ruined).powers)
