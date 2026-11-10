package oathdigital.gameplay

import oathdigital.catalog.{PrintedPower, Relic}
import oathdigital.model._

object BanditCrownCard extends Relic(RelicId("R11"), "Bandit Crown", value = 56, defense = 3):
  val power = PrintedPower(PowerId("relic.bandit-crown"),
    persistent = true, cost = Cost.free,
    text = "You may use cards at sites ruled by bandits as if you ruled " +
      "them. _You may use these battle plans against bandits._")
  val powers: Vector[PrintedPower] = Vector(power)

/** Bandit Crown (R11): its holder uses the cards at bandit-ruled sites as
  * if they ruled them (catalog batch 11, rulings 1 to 6). The bandits still
  * rule those sites and cards. The holder reaches them through
  * `PowerAccess.usableSites` and `PlanContext`'s sources, and gives the
  * consent their ruler would through `SiteRulers`' consent granters. The
  * battle plans are usable in any Campaign; the italic "against bandits"
  * is flavor (ruling 3). Only a faceup Crown counts.
  */
private[gameplay] object BanditCrownRule:
  /** The player who holds the Crown faceup, if any. */
  def holder(ready: ReadyGame): Option[PlayerId] =
    PowerAccess.faceupRelicHolder(ready, BanditCrownCard.id)

  /** The sites in play that bandits rule. */
  def banditSites(ready: ReadyGame): Set[SiteId] =
    ready.game.current.map.inPlay.filter(site =>
      SiteRule.rulingPlayer(ready, site).contains(SiteRuler.Bandits)).toSet

  /** The sites whose cards `player` uses through the Crown: the bandit
    * sites when they hold it, else none. */
  def sitesFor(ready: ReadyGame, player: PlayerId): Set[SiteId] =
    if holder(ready).contains(player) then banditSites(ready)
    else Set.empty
