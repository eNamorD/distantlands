package oathdigital.testkit

import oathdigital.catalog.{Denizen, Edifice, Legacy, Relic, Site}
import oathdigital.gameplay.setup.FirstGameSetupFixture
import oathdigital.model._

/** A card or site as a test names it: by its catalog object
  * (`AlchemistCard`, `DeepWoodsSite`), so the code reads the card's name,
  * or by the id a state already holds. Test steps take these references
  * and resolve them here. */
object CardRefs:
  type SiteRef = Site | SiteId
  type DenizenRef = Denizen | DenizenId
  type WorldCardRef = Denizen | WorldCardId
  type RelicRef = Relic | RelicId
  type EdificeRef = Edifice | EdificeId
  type LegacyRef = Legacy | LegacyId
  type CardRef = Denizen | Relic | Edifice | CardId

  def siteId(site: SiteRef): SiteId = site match
    case card: Site => card.id
    case id: SiteId => id

  def denizenId(card: DenizenRef): DenizenId = card match
    case denizen: Denizen => denizen.id
    case id: DenizenId => id

  def worldCardId(card: WorldCardRef): WorldCardId = card match
    case denizen: Denizen => denizen.id
    case id: WorldCardId => id

  def relicId(card: RelicRef): RelicId = card match
    case relic: Relic => relic.id
    case id: RelicId => id

  def edificeId(card: EdificeRef): EdificeId = card match
    case edifice: Edifice => edifice.id
    case id: EdificeId => id

  def cardId(card: CardRef): CardId = card match
    case denizen: Denizen => denizen.id
    case relic: Relic => relic.id
    case edifice: Edifice => edifice.id
    case id: CardId => id

  /** The printed name of `card`, for failure messages: a state holds ids,
    * and the id alone would not say which card went wrong. A card the
    * catalog does not print, such as a fixture's placeholder, keeps its id. */
  def nameOf(card: CardId): String =
    val catalog = FirstGameSetupFixture.catalog
    card match
      case DenizenId(id) => catalog.denizens.find(_.id.value == id)
        .fold(id)(_.name)
      case RelicId(id) => catalog.relics.find(_.id.value == id).fold(id)(_.name)
      case EdificeId(id) => catalog.edifices.find(_.id.value == id)
        .fold(id)(_.intact.name)
      case other => other.toString
