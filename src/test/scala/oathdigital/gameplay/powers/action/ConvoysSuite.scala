package oathdigital.gameplay.powers.action

import oathdigital.gameplay.cards.{AncientCitySite, BuriedGiantSite}
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.campaign.{MountainGiantCard, WildMountsCard}
import oathdigital.gameplay.powers.search.ErrandBoyCard
import oathdigital.model._
import oathdigital.testkit.{Look, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

class ConvoysSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val convoys = ConvoysCard.id
  private val source = DecisionOptionRef.Denizen(convoys)
  /** In the Cradle, and in the Provinces. */
  private val home = AncientCitySite.id
  private val giant = BuriedGiantSite.id
  /** Plain cards for the piles. */
  private val errandBoy = ErrandBoyCard.id
  private val wildMounts = WildMountsCard.id
  private val mountainGiant = MountainGiantCard.id

  /** p1's Act at Ancient City beside Convoys, site-only. The Cradle pile
    * holds Errand Boy; the Provinces pile holds Wild Mounts under Mountain
    * Giant. The Hinterland pile is empty. */
  private def staged: Table = Table.start.turn(p1, Phase.Act)
    .pawn(p1, at = home).denizen(convoys, at = home)
    .discarded(Region.Cradle, errandBoy)
    .discarded(Region.Provinces, wildMounts, mountainGiant)

  /** The walk Convoys started and the walk after `region`'s pile was
    * chosen. */
  private def pileOf(ready: ReadyGame, region: Region): Vector[WorldCardId] =
    ready.game.current.commonCards.discard(region)

  private def choosePile(ready: ReadyGame, region: Region): UsedAndAnswered =
    useAndAnswer(ready, Convoys, source,
      Convoys.decisionId, Convoys.ref(region))

  test("it places a favor and offers the other regions' piles as buttons " +
      "with their own labels"):
    val t = accepted(use(staged.ready, Convoys, source))
    assert(awaits(t, Convoys.decisionId),
      parked.parkedDecision(t.state).toString)
    assertEquals(offered(t, p1), Some(Vector("button" -> "provinces",
      "button" -> "hinterland")))
    assertEquals(queryOf(t, p1).map(_.offeredOptions.map(_.label)),
      Some(Vector("Move the Provinces discard pile",
        "Move the Hinterland discard pile")))
    assertEquals(Look(after(t)).tokensOn(convoys), Tokens(1, 0))

  test("the chosen pile goes on top of the region's pile in its order, and " +
      "the line names both"):
    val ready = staged.ready
    val walked = choosePile(ready, Region.Provinces)
    assert(backToActing(walked.answered))
    val end = after(walked.answered)
    assertEquals(pileOf(end, Region.Cradle),
      Vector[WorldCardId](errandBoy, wildMounts, mountainGiant))
    assertEquals(pileOf(end, Region.Provinces), Vector.empty[WorldCardId])
    assertEquals(NoteText.said(Convoys, walked.answered.events), Vector(
      NoteText.Said(NoteKey.Used,
        "Moved the Provinces discard pile onto the Cradle discard pile.",
        covers = false)))
    assertEquals(replayed(ready, walked.events), Right(walked.answered.state))
    assertWireRoundTrips(walked.events)

  test("an empty pile may be chosen: nothing moves and the line still names " +
      "both"):
    val walked = choosePile(staged.ready, Region.Hinterland)
    assertEquals(pileOf(after(walked.answered), Region.Cradle),
      Vector[WorldCardId](errandBoy))
    assertEquals(NoteText.said(Convoys, walked.answered.events), Vector(
      NoteText.Said(NoteKey.Used,
        "Moved the Hinterland discard pile onto the Cradle discard pile.",
        covers = false)))

  test("the region is the pawn's: from the Provinces it offers the Cradle " +
      "and the Hinterland"):
    val ready = Table.start.turn(p1, Phase.Act).pawn(p1, at = giant)
      .denizen(convoys, at = giant).ready
    val t = accepted(use(ready, Convoys, source))
    assertEquals(offered(t, p1), Some(Vector("button" -> "cradle",
      "button" -> "hinterland")))
