package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** The Gathering (card 27): who is asked to come, in what order, and what
  * coming does (catalog batch 12, rulings 1 and 2). The card stands at
  * `homeSite`. */
class TheGatheringSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[TheGathering.type]
  private val gathering = power.cardId
  private val played = hookAt(gathering, homeSite)
  private val come = DecisionAnswer.ChooseOneAnswer(TheGathering.join)
  private val stay = DecisionAnswer.ChooseOneAnswer(TheGathering.stay)

  /** The Gathering at `homeSite`, and each pawn where `at` puts it. */
  private def gatheringWith(at: (PlayerId, SiteId)*): Table =
    at.foldLeft(Table.start.denizen(gathering, at = homeSite)) {
      case (table, (player, site)) => table.pawn(player, site) }

  private def pawnSite(ready: ReadyGame, player: PlayerId): Option[SiteId] =
    ready.game.current.pawnSite(player)

  private def gathered(players: String): NoteText.Said =
    NoteText.Said("gathered", s"Gathered at ${homeSite.value}: $players.",
      covers = false)

  test("each player elsewhere is asked, the actor included, in turn order " +
      "from the first player"):
    // No Chancellor, so the order is p1, p2, p3. p2 is already there.
    val walked = walkAnswering(gatheringWith(p1 -> nearSite,
      p2 -> homeSite, p3 -> awaySite).ready, power, played, stay, stay)
    assertEquals(walked.asked.map(question =>
      (question.decisionId, question.owner)), Vector(
      TheGathering.pawnDecisionId -> p1, TheGathering.pawnDecisionId -> p3))
    // One player there: the deals are skipped.
    assertEquals(said(power, walked.events), Vector(gathered("p2")))

  test("with a Chancellor, the questions start from the Chancellor"):
    val ready = gatheringWith(p1 -> nearSite, p2 -> awaySite,
      p3 -> nearSite).imperial(p3).ready
    val walked = walkAnswering(ready, power, played, stay, stay, stay)
    assertEquals(walked.asked.map(_.owner), Vector(p3, p1, p2))
    assertEquals(said(power, walked.events), Vector(NoteText.Said("nobody",
      s"Nobody gathered at ${homeSite.value}.", covers = false)))

  test("a yes moves the pawn with a plain move, not Travel"):
    val ready = gatheringWith(p1 -> nearSite, p2 -> awaySite,
      p3 -> awaySite).ready
    val walked = walkAnswering(ready, power, played, come, stay, stay)
    assertEquals(pawnSite(walked.end, p1), Some(homeSite))
    assertEquals(pawnSite(walked.end, p2), Some(awaySite))
    // The move is the one recorded operation: no Supply, no travel window.
    assertEquals(ops(walked.events), Vector(Move(Piece.Pawn(p1),
      PositionedLocation(Location.Site(nearSite)),
      PositionedLocation(Location.Site(homeSite)))))
    assertEquals(said(power, walked.events), Vector(gathered("p1")))
    assertJournalRebuilds(ready, walked.events, walked.end)
