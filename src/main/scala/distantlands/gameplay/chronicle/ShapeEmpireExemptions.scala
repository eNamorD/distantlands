package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.operations.HallOfMinistersCard
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

/** The cards Shape Empire keeps on the map although the Throne holder does
  * not rule their site (Last Chronicle powers design, rulings 1 and 2).
  * Shape Empire reads these checks before it lists its discards, so Iron
  * Hand's Shape Empire and Rival's question see the same list.
  * Revolutionary's discard comes after Build Edifice and reads neither. */
object ShapeEmpireExemptions:
  /** Cards at an Enduring site are not discarded in Shape Empire (CR
    * p. 37). Ancient City is the only one: its catalog handlers carry
    * [[AncientCityEnduring]]. */
  def enduring(catalog: ExecutableCatalog, site: SiteId): Boolean =
    catalog.site(site).exists(_.handlers.contains(AncientCityEnduring.id))

  /** A ruined edifice at a site bandits rule, while Hall of Bandits (E16
    * ruined) is on the map, is not discarded in the Chronicle (ruling 2).
    * Shape Empire is the only Chronicle discard that reaches a bandit
    * site. The Hall keeps itself when bandits rule its site. */
  def banditRuin(ready: ReadyGame, site: SiteId, card: SiteDenizenState)
      : Boolean = card match
    case EdificeState(_, EdificeSide.Ruined, _) =>
      ready.game.current.edificeSite(HallOfMinistersCard.id,
        EdificeSide.Ruined).nonEmpty &&
        SiteRule.rulingPlayer(ready, site).contains(SiteRuler.Bandits)
    case _ => false

/** Ancient City's Enduring: Shape Empire reads it
  * (`ShapeEmpireExemptions.enduring`). It hooks no window. */
case object AncientCityEnduring extends ContributingPower:
  val id: PowerId = AncientCitySite.enduring
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** Hall of Bandits (E16 ruined): Shape Empire reads it
  * (`ShapeEmpireExemptions.banditRuin`). It hooks no window. */
case object HallOfBandits extends ContributingPower:
  val id: PowerId = HallOfMinistersCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
