package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.cards.AncientCitySite
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

/** Ancient City's Enduring: Shape Empire reads it
  * (`ShapeEmpireExemptions.enduring`). It hooks no window; it is
  * registered so the power counts as implemented, as the Baths are. */
case object AncientCityEnduring extends ContributingPower:
  val id: PowerId = AncientCitySite.enduring
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
