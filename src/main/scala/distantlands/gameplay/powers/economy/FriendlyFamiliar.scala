package distantlands.gameplay.powers.economy

import distantlands.catalog.{AdviserOnly, Denizen, ExecutableCatalog,
  ModifierMark, PrintedPower}
import distantlands.model._

object FriendlyFamiliarCard extends Denizen(DenizenId("224"), "Friendly Familiar", Suit.Discord) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.friendly-familiar"),
    persistent = false, cost = Cost(secret = 1),
    modifier = Some(ModifierMark.Action(MajorActionType.Trade)),
    text = "Act as if your pawn is at any site. _You may use Trade " +
      "modifiers at the site._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Friendly Familiar (card 224, adviser-only), a selected Trade modifier
  * costing one secret, paid onto it when picked: "Act as if your pawn is at
  * any site. You may use Trade modifiers at the site." It offers the sites
  * in play, other than the pawn site, that hold a card a Trade can draw on
  * ([[ActingSite]]).
  */
case object FriendlyFamiliar extends ActingSite:
  val id: PowerId = FriendlyFamiliarCard.power.id

  val card: Denizen = FriendlyFamiliarCard
  override def cost: Cost = FriendlyFamiliarCard.power.cost
  val noSite: String = "No other site has a card to trade with"
  val heading: String = "Friendly Familiar: act as if at which site?"

  def sites(catalog: ExecutableCatalog,
      ready: ReadyGame, actor: PlayerId): Vector[SiteId] =
    ActingSite.otherSites(ready, actor)(card =>
      catalog.suitOf(card.id).nonEmpty)
