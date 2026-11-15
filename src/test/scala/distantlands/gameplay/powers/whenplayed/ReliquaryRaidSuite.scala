package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.WalkerDiceFixture
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus}
import distantlands.model._
import distantlands.testkit.Look
import distantlands.testkit.Table.p1

/** Reliquary Raid (card 220), WHEN PLAYED: take a random relic from the
  * Reliquary (catalog batch 13, ruling 7). */
class ReliquaryRaidSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val raid = registered[ReliquaryRaid.type]

  private def line(key: String, text: String): NoteText.Said =
    NoteText.Said(key, text, covers = false)

  private def played(ready: ReadyGame): Walked =
    walkShuffling(ready, raid, hook(ReliquaryRaidCard.id),
      WalkerDiceFixture.reversingShuffle)

  test("the player takes a random Reliquary relic facedown"):
    val start = p1AtHomeSite.turn(p1, Phase.Act).reliquaryFromDeck(3).ready
    val stocked = start.game.campaign.reliquary
    val walked = played(start)
    assertEquals(walked.asked, Vector.empty)
    // The shuffle reverses the Reliquary, so its last relic comes first.
    val taken = Look(walked.end).player(p1).relics
      .filter(relic => stocked.contains(relic.id))
    assertEquals(taken.map(_.id), Vector(stocked.last))
    assertEquals(taken.map(_.orientation), Vector(Orientation.FaceDown))
    assertEquals(walked.end.game.campaign.reliquary.size, 2)
    assertEquals(said(raid, walked.events),
      Vector(line("took", "Took a relic from the Reliquary.")))
    assertJournalRebuilds(start, walked.events, walked.end)

  test("an empty Reliquary takes nothing and says so"):
    val start = p1AtHomeSite.turn(p1, Phase.Act).ready
    assertEquals(start.game.campaign.reliquary, Vector.empty)
    val walked = played(start)
    assertEquals(said(raid, walked.events),
      Vector(line("reliquary-empty", "The Reliquary was empty.")))
    // Only the line is written: no operation runs, so the game is as it was.
    assertEquals(walked.end, start)

  test("a Reliquary Setup With No Empire set aside gives no relic"):
    val start = p1AtHomeSite.turn(p1, Phase.Act).reliquaryFromDeck(2)
      .reliquarySetAside.ready
    val stocked = start.game.campaign.reliquary
    assertEquals(stocked.size, 2)
    val walked = played(start)
    assertEquals(walked.end.game.campaign.reliquary, stocked)
    assert(!Look(walked.end).player(p1).relics.exists(relic =>
      stocked.contains(relic.id)))
    assertEquals(said(raid, walked.events),
      Vector(line("reliquary-empty", "The Reliquary was empty.")))
    // Only the line is written: no operation runs, so the game is as it was.
    assertEquals(walked.end, start)

  test("its power counts as implemented"):
    assert(PowerImplementationStatus.implemented(ReliquaryRaidCard.power.id))
