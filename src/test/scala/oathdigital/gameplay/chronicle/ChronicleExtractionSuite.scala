package oathdigital.gameplay.chronicle

import oathdigital.model._
import oathdigital.testkit.{CatalogNames, Table}
import oathdigital.testkit.Table.{p1, p2}

/** Reading the finished table into the Chronicle's table fields (Chronicle
  * steps design, "Exit and extraction"; Chronicle rulings 17 and 18). */
class ChronicleExtractionSuite extends munit.FunSuite:
  private val ancientCity = CatalogNames.site("Ancient City")
  private val brokenPeaks = CatalogNames.site("Broken Peaks")

  /** p1's won game, p1 an Exile ruling Ancient City and Broken Peaks. */
  private val p1RulesTwoSites: Table = ChronicleWalk.chronicleWonByP1
    .warbandsAt("Ancient City", p1, 1).warbandsAt("Broken Peaks", p1, 1)

  test("the winner's sites are the Empire's and the rest are rivals, " +
      "each Cradlemost first"):
    val ready = p1RulesTwoSites.ready
    val inPlay = ready.game.current.map.hinterward
    val layout = ChronicleExtraction.layout(ready)
    assertEquals(layout, Right(ChronicleExtraction.SiteLayout(
      inPlay.filter(Set(ancientCity, brokenPeaks)),
      inPlay.filterNot(Set(ancientCity, brokenPeaks)))))

  test("a game with no result has no layout"):
    assert(ChronicleExtraction.layout(Table.start.ready).isLeft)

  test("a stored site holds at most three cards"):
    val cards = Vector("Fae Merchant", "Threatening Roar")
      .map(CatalogNames.denizen) ++ Vector("Brass Horse")
      .map(CatalogNames.relic)
    assertEquals(ChronicleExtraction.storedSite(ancientCity, cards),
      Right(StoredSite(ancientCity, cards)))
    val four = cards :+ CatalogNames.relic("Book of Records")
    assertEquals(ChronicleExtraction.storedSite(ancientCity, four),
      Left(OathViolation.UnsupportedChronicle(s"${ancientCity.value} holds " +
        "4 cards; a stored site holds at most 3")))

  test("a held adviser blocks the record"):
    val card = CatalogNames.denizen("Fae Merchant")
    val ready = p1RulesTwoSites.adviser(p2, "Fae Merchant").ready
    ChronicleExtraction.extract(ready, ready.game.campaign.baseline) match
      case Left(OathViolation.UnsupportedChronicle(reason)) =>
        assert(reason.contains(card.value), reason)
      case other => fail(other.toString)

  test("a Vision still in play blocks the record"):
    // The quiet table's world deck still holds the Visions the World's
    // Separate Visions would remove.
    val ready = p1RulesTwoSites.ready
    val vision = ready.game.current.commonCards.worldDeck
      .collectFirst { case id: VisionId => id }.get
    ChronicleExtraction.extract(ready, ready.game.campaign.baseline) match
      case Left(OathViolation.UnsupportedChronicle(reason)) =>
        assert(reason.contains(vision.value), reason)
      case other => fail(other.toString)

  test("a stored site lists its edifice, then its denizens, then its relics"):
    val roar = CatalogNames.denizen("Threatening Roar")
    val hall = CatalogNames.edifice("Hall of Debate")
    val horse = CatalogNames.relic("Brass Horse")
    // An empty Broken Peaks and no Visions left in play, so the record
    // stores.
    val ready = Table.from(p1RulesTwoSites.ready.updateCurrent(current =>
      current.copy(
        map = current.map.copy(sites = current.map.sites.updatedWith(
          brokenPeaks)(_.map(_.copy(denizens = Vector.empty,
            relics = Vector.empty)))),
        commonCards = current.commonCards.copy(worldDeck = current
          .commonCards.worldDeck.filterNot(_.isInstanceOf[VisionId])))))
      .denizen(roar, brokenPeaks)
      .edifice(hall, EdificeSide.Intact, brokenPeaks)
      .relicAt(horse, brokenPeaks).ready
    val record = ChronicleExtraction.extract(ready,
      ready.game.campaign.baseline)
    assertEquals(record.map(_.world.find(_.site == brokenPeaks)),
      Right(Some(StoredSite(brokenPeaks, Vector(hall, roar, horse)))))
