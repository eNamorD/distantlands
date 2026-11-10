package oathdigital.gameplay.powers.action

import oathdigital.gameplay.powers.whenplayed.PilgrimageCard
import oathdigital.gameplay.MarriageCard
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.powers.search.{ErrandBoyCard, LandWardenCard}
import oathdigital.model._
import oathdigital.testkit.{Look, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

class HomesteadersSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val homesteaders = HomesteadersCard.id
  private val source = DecisionOptionRef.Denizen(homesteaders)
  private val power = Homesteaders
  /** p1's pawn site, with room for three cards. */
  private val home = AncientCitySite.id
  /** A plain Hearth denizen. */
  private val landWarden = LandWardenCard.id
  /** A plain Beast denizen. */
  private val errandBoy = ErrandBoyCard.id
  /** Adviser-only. */
  private val pilgrimage = PilgrimageCard.id
  /** Locked and adviser-only. */
  private val marriage = MarriageCard.id

  /** p1's Act at Ancient City, holding Homesteaders faceup. */
  private def holdingHomesteaders: Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, at = home)
      .adviser(p1, homesteaders)

  /** The walk Homesteaders started and the walk after `card` was chosen. */
  private def choose(ready: ReadyGame, card: DenizenId): UsedAndAnswered =
    useAndAnswer(ready, power, source,
      Homesteaders.decisionId, DecisionOptionRef.Denizen(card))

  test("it offers the player's faceup advisers, itself included, and not a " +
      "facedown one"):
    val ready = holdingHomesteaders.adviser(p1, landWarden)
      .adviser(p1, errandBoy, facedown = true).ready
    val t = accepted(use(ready, power, source))
    assert(awaits(t, Homesteaders.decisionId),
      parked.parkedDecision(t.state).toString)
    assertEquals(offered(t, p1), Some(Vector("denizen" -> homesteaders.value,
      "denizen" -> landWarden.value)))

  test("adviser-only and locked advisers are not offered"):
    val ready = holdingHomesteaders.adviser(p1, pilgrimage)
      .adviser(p1, marriage).ready
    val t = accepted(use(ready, power, source))
    assertEquals(offered(t, p1),
      Some(Vector("denizen" -> homesteaders.value)))

  test("the chosen adviser moves to the pawn's site with its tokens, and the " +
      "line names it"):
    val ready = holdingHomesteaders.adviser(p1, landWarden)
      .tokens(landWarden, favor = 1).ready
    val UsedAndAnswered(t, done) = choose(ready, landWarden)
    parked.assertResumed(done.state, Phase.Act, p1)
    val end = Look(after(done))
    assertEquals(end.cardsAt(home), Vector[CardId](landWarden))
    assertEquals(end.tokensOn(landWarden), Tokens(1, 0))
    assertEquals(end.advisers(p1), Vector[CardId](homesteaders))
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      NoteKey.Used, s"Moved ${landWarden.value} to ${home.value}.",
      covers = false)))
    assertEquals(replayed(ready, t.events ++ done.events), Right(done.state))
    assertWireRoundTrips(t.events ++ done.events)

  test("it can move itself"):
    val UsedAndAnswered(_, done) = choose(holdingHomesteaders.ready, homesteaders)
    assertEquals(Look(after(done)).cardsAt(home), Vector[CardId](homesteaders))
    assertEquals(Look(after(done)).advisers(p1), Vector.empty[CardId])

  test("with the pawn's site full it shows Continue and the line says so"):
    val ready = holdingHomesteaders.adviser(p1, landWarden)
      .denizen(errandBoy, at = home).denizen(WrestlersCard, at = home)
      .denizen(EldersCard, at = home).ready
    val asked = accepted(use(ready, power, source))
    assert(awaits(asked, Homesteaders.decisionId))
    val done = accepted(continuedPast(asked, Homesteaders.decisionId))
    parked.assertResumed(done.state, Phase.Act, p1)
    assertEquals(Look(after(done)).advisers(p1),
      Vector[CardId](homesteaders, landWarden))
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      "used.none", "No adviser could be moved.", covers = false)))
