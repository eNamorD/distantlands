package oathdigital.testkit

import oathdigital.catalog.{Denizen, Edifice, Legacy, Relic, Site}
import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.model._

/** A card or site as a test names it: by its catalog object
  * (`AlchemistCard`, `DeepWoodsSite`), so the code reads the card's name,
  * or by the id a state already holds. Test steps take these references
  * and resolve them here. A printed name as a string is still accepted
  * while the sweep to card objects runs; it goes through `CatalogNames`. */
object CardRefs:
  type SiteRef = String | Site | SiteId
  type DenizenRef = String | Denizen | DenizenId
  type WorldCardRef = String | Denizen | WorldCardId
  type RelicRef = String | Relic | RelicId
  type EdificeRef = String | Edifice | EdificeId
  type LegacyRef = String | Legacy | LegacyId
  type CardRef = String | Denizen | Relic | Edifice | CardId

  def siteId(site: SiteRef)(using munit.Location): SiteId = site match
    case card: Site => card.id
    case id: SiteId => id
    case name: String => CatalogNames.site(name)

  def denizenId(card: DenizenRef)(using munit.Location): DenizenId = card match
    case denizen: Denizen => denizen.id
    case id: DenizenId => id
    case name: String => CatalogNames.denizen(name)

  def worldCardId(card: WorldCardRef)(using munit.Location): WorldCardId =
    card match
      case denizen: Denizen => denizen.id
      case id: WorldCardId => id
      case name: String => CatalogNames.worldCard(name)

  def relicId(card: RelicRef)(using munit.Location): RelicId = card match
    case relic: Relic => relic.id
    case id: RelicId => id
    case name: String => CatalogNames.relic(name)

  def edificeId(card: EdificeRef)(using munit.Location): EdificeId = card match
    case edifice: Edifice => edifice.id
    case id: EdificeId => id
    case name: String => CatalogNames.edifice(name)

  def legacyId(card: LegacyRef)(using munit.Location): LegacyId = card match
    case legacy: Legacy => legacy.id
    case id: LegacyId => id
    case name: String => CatalogNames.legacy(name)

  def cardId(card: CardRef)(using munit.Location): CardId = card match
    case denizen: Denizen => denizen.id
    case relic: Relic => relic.id
    case edifice: Edifice => edifice.id
    case id: CardId => id
    case name: String => CatalogNames.card(name)

  /** The printed name of `card`, for failure messages: a state holds ids,
    * and the id alone would not say which card went wrong. A card the
    * catalog does not print, such as a fixture's placeholder, keeps its id. */
  def nameOf(card: CardId): String =
    val catalog = FirstGameSetupFixture.catalog
    card match
      case DenizenId(id) => catalog.denizens.find(_.id.value == id).fold(id)(_.name)
      case RelicId(id) => catalog.relics.find(_.id.value == id).fold(id)(_.name)
      case EdificeId(id) => catalog.edifices.find(_.id.value == id)
        .fold(id)(_.intact.name)
      case other => other.toString
