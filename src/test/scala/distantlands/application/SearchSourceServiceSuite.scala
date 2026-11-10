package distantlands.application

import distantlands.gameplay.ModifierPlay
import distantlands.gameplay.powers.action.{FaeMerchantCard, SecondChanceCard}
import distantlands.gameplay.powers.search.{NewsFromAfar, NewsFromAfarCard}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted
import distantlands.testkit.Commands

/** The source decision through the service: Undo takes back the start, and
  * the answer draws, so nothing takes it back (search source design,
  * ruling 6). */
class SearchSourceServiceSuite extends munit.FunSuite:
  private val table = Table.start
    .worldDeckTop(ThreateningRoarCard, FaeMerchantCard, SecondChanceCard)
    .banditsRefilled

  test("Undo takes back the start, leaving the table as it was"):
    val (service, _) = table.service()
    val started = accepted(service.handle("g", 0L,
      ParkedServiceFixture.startSearch(p1)))
    val undone = accepted(service.handle("g", started.nextSequence,
      Commands.undo(p1)))
    assertEquals(undone.state, table.phaseMenuParked.state)

  test("Undo takes back a pick, and then the start"):
    val withNews = table.adviser(p1, NewsFromAfarCard).favor(p1, 2)
    val (service, _) = withNews.service()
    val started = accepted(service.handle("g", 0L,
      ParkedServiceFixture.startSearch(p1)))
    val picked = accepted(service.handle("g", started.nextSequence,
      Commands.resolveWalker(p1, TreeDecision("search.modifiers",
        ModifierPlay.pick(NewsFromAfar.id)))))
    val unpicked = accepted(service.handle("g", picked.nextSequence,
      Commands.undo(p1)))
    assertEquals(unpicked.state, started.state)
    val undone = accepted(service.handle("g", unpicked.nextSequence,
      Commands.undo(p1)))
    assertEquals(undone.state, withNews.phaseMenuParked.state)

  test("the source answer draws, so it cannot be taken back"):
    val (service, _) = table.service()
    val started = accepted(service.handle("g", 0L,
      ParkedServiceFixture.startSearch(p1)))
    val picked = accepted(service.handle("g", started.nextSequence,
      ParkedServiceFixture.pickSearchSource(p1, SearchSource.WorldDeck)))
    assertEquals(service.handle("g", picked.nextSequence, Commands.undo(p1)),
      Left(GameApplicationError.CommandRejected(OathViolation.NothingToUndo)))
