package distantlands.gameplay.chronicle

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.action.{BookOfRecordsCard, BrassHorseCard,
  FaeMerchantCard}
import distantlands.gameplay.powers.setup.HallOfDebateCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** Reading the finished table into the Chronicle's table fields (Chronicle
  * steps design, "Exit and extraction"; Chronicle rulings 17 and 18). */
class ChronicleExtractionSuite extends munit.FunSuite:
  private val ancientCity = AncientCitySite.id
  private val brokenPeaks = BrokenPeaksSite.id

  /** p1's won game, p1 an Exile ruling Ancient City and Broken Peaks. */
  private val p1RulesTwoSites: Table = ChronicleWalk.chronicleWonByP1
    .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BrokenPeaksSite, p1, 1)

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
    val cards = Vector[CardId](FaeMerchantCard.id, ThreateningRoarCard.id,
      BrassHorseCard.id)
    assertEquals(ChronicleExtraction.storedSite(ancientCity, cards),
      Right(StoredSite(ancientCity, cards)))
    val four = cards :+ BookOfRecordsCard.id
    assertEquals(ChronicleExtraction.storedSite(ancientCity, four),
      Left(OathViolation.UnsupportedChronicle(s"${ancientCity.value} holds " +
        "4 cards; a stored site holds at most 3")))

  test("a held adviser blocks the record"):
    val card = FaeMerchantCard.id
    val ready = p1RulesTwoSites.adviser(p2, FaeMerchantCard).ready
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
    val roar = ThreateningRoarCard.id
    val hall = HallOfDebateCard.id
    val horse = BrassHorseCard.id
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
