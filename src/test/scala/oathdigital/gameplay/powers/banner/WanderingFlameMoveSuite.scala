package oathdigital.gameplay.powers.banner

import oathdigital.catalog.Site
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import oathdigital.gameplay.powers.{NoteText, PowerFixture}
import oathdigital.gameplay.powers.whenplayed.GarrisonCard
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, Look, PhasePowerHarness, Refusals,
  Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p3}
import oathdigital.testkit.Unwrap.accepted

class WanderingFlameMoveSuite extends munit.FunSuite:
  import PowerFixture._
  import BannerFixture._
  import PhasePowerHarness.{after, answer, awaits, backToActing, ops, pick,
    replayed, use}
  import ActPowerHarness.usableIds

  private val ancientCity = AncientCitySite.id
  private val brokenPeaks = BrokenPeaksSite.id
  private val buriedGiant = BuriedGiantSite.id
  private val deepWoods = DeepWoodsSite.id
  private val power = WanderingFlameMove

  /** p1's Act at Ancient City holding the Darkest Secret, with its one
    * secret, on Wandering Flame, and exactly one secret on each site in
    * `marked`. */
  private def holdingFlameWithSiteSecretsTable(marked: Site*): Table =
    marked.foldLeft(Table.start.turn(p1, Phase.Act).pawn(p1, AncientCitySite)
      .darkestSecret(Some(p1), 1))(_.siteTokens(_, secrets = 1))
  private def holdingFlameWithSiteSecrets(marked: Site*): ReadyGame =
    holdingFlameWithSiteSecretsTable(marked*).ready

  test("one secret-bearing site: the pawn goes there with no question, by a " +
      "plain Move that leaves the secret where it is"):
    val start = holdingFlameWithSiteSecrets(BrokenPeaksSite)
    val done = accepted(use(start, power, darkestSecretOption))
    assert(backToActing(done))
    assertEquals(Look(after(done)).pawn(actor), brokenPeaks)
    assertEquals(Look(after(done)).siteTokens(brokenPeaks).secrets, 1)
    assertEquals(ops(done.events), Vector[CoreOperation](
      Move(Piece.Pawn(actor),
        PositionedLocation(Location.Site(ancientCity)),
        PositionedLocation(Location.Site(brokenPeaks)))))
    assertEquals(replayed(start, done.events), Right(done.state))
    assertWireRoundTrips(done.events)

  test("several candidates: the player chooses among exactly the other " +
      "sites that hold a secret"):
    val start = holdingFlameWithSiteSecrets(BrokenPeaksSite, BuriedGiantSite,
      AncientCitySite)
    val parked = accepted(use(start, power, darkestSecretOption))
    assert(awaits(parked, power.decisionId))
    assertEquals(answer(parked, actor, power.decisionId,
      pick(DecisionOptionRef.Site(ancientCity))).left.toOption,
      Some(Refusals.notOffered(WanderingFlameMove.decisionId)))
    assertEquals(answer(parked, actor, power.decisionId,
      pick(DecisionOptionRef.Site(deepWoods))).left.toOption,
      Some(Refusals.notOffered(WanderingFlameMove.decisionId)))
    val done = accepted(answer(parked, actor, power.decisionId,
      pick(DecisionOptionRef.Site(buriedGiant))))
    assert(backToActing(done))
    assertEquals(Look(after(done)).pawn(actor), buriedGiant)
    assertEquals(replayed(start, parked.events ++ done.events),
      Right(done.state))

  test("a secret on a card at a site does not count, only one on the site"):
    val card = GarrisonCard.id
    val onCard = holdingFlameWithSiteSecretsTable()
      .denizen(card, at = BrokenPeaksSite).tokens(card, secrets = 1)
    assert(!usableIds(onCard.ready).contains(power.id))
    assert(usableIds(onCard.siteTokens(BrokenPeaksSite, secrets = 1).ready)
      .contains(power.id))

  test("it is unlimited: it can be used again in the same turn"):
    val start = holdingFlameWithSiteSecrets(BrokenPeaksSite, BuriedGiantSite)
    val first = accepted(use(start, power, darkestSecretOption))
    val parked = accepted(answer(first, actor, power.decisionId,
      pick(DecisionOptionRef.Site(brokenPeaks))))
    val second = accepted(use(after(parked), power, darkestSecretOption))
    // Only Buried Giant still holds a secret, so the pawn goes without a
    // question.
    assertEquals(Look(after(second)).pawn(actor), buriedGiant)
    assertEquals(after(parked).game.current.turn.usedPowers, Set.empty[PowerUseRef])

  test("it is unusable with no other site holding a secret, without the " +
      "banner, with the banner held by another player, or on the Festival " +
      "face, and usable with one other secret-bearing site"):
    assert(!usableIds(holdingFlameWithSiteSecrets()).contains(power.id))
    assertEquals(use(holdingFlameWithSiteSecrets(), power, darkestSecretOption)
      .left.toOption,
      Some(Refusals.nothingToDo(power.id, darkestSecretOption)))
    assert(!usableIds(holdingFlameWithSiteSecrets(AncientCitySite))
      .contains(power.id), "only the pawn's own site")
    assert(usableIds(holdingFlameWithSiteSecrets(BrokenPeaksSite))
      .contains(power.id))
    val notHeld = Table.start.turn(p1, Phase.Act).darkestSecret(None, 1)
      .siteTokens(BrokenPeaksSite, secrets = 1).ready
    assert(!usableIds(notHeld).contains(power.id))
    assertEquals(use(notHeld, power, darkestSecretOption).left.toOption,
      Some(Refusals.inaccessibleSource(power.id, darkestSecretOption)))
    val theirs = Table.start.turn(p1, Phase.Act).darkestSecret(Some(p3), 1)
      .siteTokens(BrokenPeaksSite, secrets = 1).ready
    assert(!usableIds(theirs).contains(power.id))
    val festival = Table.start.turn(p1, Phase.Act)
      .darkestSecret(Some(p1), 1, DarkestSecretFace.Festival)
      .siteTokens(BrokenPeaksSite, secrets = 1).ready
    assert(!usableIds(festival).contains(power.id))
    assertEquals(use(festival, power, darkestSecretOption).left.toOption,
      Some(Refusals.inaccessibleSource(power.id, darkestSecretOption)))

  test("it is usable in the Act phase only"):
    val wake = Table.start.turn(p1, Phase.Wake).darkestSecret(Some(p1), 1)
      .siteTokens(BrokenPeaksSite, secrets = 1).ready
    assert(!usableIds(wake).contains(power.id))

  test("it writes where the pawn went"):
    val done = accepted(
      use(holdingFlameWithSiteSecrets(BrokenPeaksSite), power,
      darkestSecretOption))
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      NoteKey.Used, s"${actor.value} moved to ${brokenPeaks.value}.",
      covers = false)))
