package distantlands.gameplay

import distantlands.catalog.{PrintedPower, Relic}
import distantlands.model._

object GrandMaskCard extends Relic(RelicId("R05"), "Grand Mask", value = 36, defense = 1):
  val power = PrintedPower(PowerId("relic.grand-mask"),
    persistent = true, cost = Cost.free,
    text = "If you're an Exile, during your turn you rule cards except " +
      "battle plans at Imperial sites, and Imperial players do not.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Grand Mask (R05): while its holder is an Exile, during their own turn,
  * they rule every denizen (edifices included) at an Imperial site except
  * battle plans, and the Imperials rule none of those cards (catalog batch
  * 11, ruling 7). The sites stay Imperial (the card's FAQ). `CardRule`
  * reads this for every card ruler, and `PowerAccess.rulingSites` for
  * access. Only a faceup Mask counts.
  */
private[gameplay] object GrandMaskRule:
  /** The player who holds the Mask faceup, if any. */
  def holder(ready: ReadyGame): Option[PlayerId] =
    PowerAccess.faceupRelicHolder(ready, GrandMaskCard.id)

  /** The holder while the Mask works: an Exile who is the active player,
    * in their Wake, Act or Rest (the turn as `Imperials.governor` reads
    * it). */
  def wearer(ready: ReadyGame): Option[PlayerId] =
    holder(ready).filter(player => Imperials.inOwnTurn(ready) &&
      ready.game.current.turn.activePlayer == player &&
      Imperials.role(ready, player).contains(Role.Exile))

  /** The wearer, when `site` is an Imperial site. */
  def maskingAt(ready: ReadyGame, site: SiteId): Option[PlayerId] =
    wearer(ready).filter(_ => SiteRulers.isImperialSite(ready, site))

  /** The Imperial sites while the Mask works, else none. */
  def maskedSites(ready: ReadyGame): Set[SiteId] =
    if wearer(ready).isEmpty then Set.empty
    else SiteRulers.imperialSites(ready).toSet

  /** The sites whose cards `player` rules through the Mask: the Imperial
    * sites while they wear it, else none. */
  def sitesFor(ready: ReadyGame, player: PlayerId): Set[SiteId] =
    if wearer(ready).contains(player) then maskedSites(ready)
    else Set.empty
