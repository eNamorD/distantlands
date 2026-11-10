package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.WalkerDiceFixture
import oathdigital.gameplay.operations.AncientBloodlineCard
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.powers.action.{HomesteadersCard, ScryerCard}
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.powers.economy.InitiationRiteCard
import oathdigital.gameplay.powers.setup.{ForestHornCard, GreatClockCard}
import oathdigital.gameplay.walker.{WalkerDice, WalkerOutcome}
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Outcomes.assertWireRoundTrips
import oathdigital.testkit.Table.{p1, p2, p3}

/** Pilgrimage (card 161, catalog batch 10, slice 6), WHEN PLAYED: the
  * denizens at p1's site go to the Dispossessed, and as many shuffled
  * Dispossessed cards go, peeked by p1 alone, onto p1's region's discard
  * pile. A ruined edifice leaves for the edifice deck uncounted, and a card
  * a lock keeps stays (ruling 9). */
class PilgrimageSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = Pilgrimage
  private val played = hook(Pilgrimage.cardId)
  private val keepingOrder: WalkerDice =
    WalkerDiceFixture.shufflingBy(count => (0 until count).toVector)
  private val region: Region =
    p1AtHomeSite.ready.game.current.map.regionOf(homeSite).get
  private val homesteaders = HomesteadersCard.id
  private val wrestlers = WrestlersCard.id

  /** p1 holds Pilgrimage faceup at `homeSite`, and the Dispossessed holds
    * Dazzle, Scryer and Bewitch, Dazzle at its head. */
  private val pilgrimageHeld: Table = p1AtHomeSite.adviser(p1, PilgrimageCard)
    .dispossessed(DazzleCard, ScryerCard, BewitchCard)

  /** `pilgrimageHeld` with Homesteaders (Hearth, 1 favor) and Wrestlers (2
    * secrets) at p1's site, and 3 favor in the Hearth bank. */
  private val twoDenizensAtSite: ReadyGame = pilgrimageHeld
    .denizen(homesteaders, at = homeSite).tokens(homesteaders, favor = 1)
    .denizen(wrestlers, at = homeSite).tokens(wrestlers, secrets = 2)
    .bankFavor(Suit.Hearth, 3).ready

  /** The walk parked on the Inspect, and the state its recorded steps
    * reach. */
  private def parkedOnInspect(ready: ReadyGame,
      dice: WalkerDice = keepingOrder): (WalkerOutcome.Parked, ReadyGame) =
    val first = Outcomes.parked(fire(ready, power, played, dice))
    (first, replayed(ready, first.events))

  private def sentLine(count: Int, noun: String): Vector[NoteText.Said] =
    Vector(NoteText.Said(NoteKey.Used, s"Sent $count $noun on Pilgrimage " +
      s"and drew $count from the Dispossessed.", covers = true))

  test("the denizens at p1's site go to the Dispossessed with their tokens " +
      "returned, and as many cards are drawn onto the region's pile"):
    val (first, mid) = parkedOnInspect(twoDenizensAtSite)
    assertEquals(Look(mid).cardsAt(homeSite), Vector.empty)
    assertEquals(mid.game.campaign.dispossessed,
      Vector[WorldCardId](BewitchCard.id, homesteaders, wrestlers))
    assertEquals(Look(mid).discardPile(region),
      Vector[WorldCardId](DazzleCard.id, ScryerCard.id))
    assertEquals(Look(mid).bankFavor(Suit.Hearth), 3 + 1)
    assertEquals(Look(mid).faceDownSecrets(p1),
      Look(twoDenizensAtSite).faceDownSecrets(p1) + 2)
    assertEquals(said(power, first.events), sentLine(2, "denizens"))

  test("only p1 learns the drawn cards, which the Inspect shows top " +
      "first, and the journal rebuilds the end"):
    val (first, mid) = parkedOnInspect(twoDenizensAtSite)
    val drawn = Vector[WorldCardId](ScryerCard.id, DazzleCard.id)
    val decide = question(mid, power, played, first.tree)
    assertEquals(decide.decisionId, Pilgrimage.inspectDecisionId)
    assertEquals(decide.query, DecisionQuery.Inspect(drawn,
      heading = Some("Pilgrimage: the cards you drew")))
    drawn.foreach(card => assert(Look(mid).knowsAdviser(p1, card)))
    Vector(p2, p3).foreach(other => drawn.foreach(card =>
      assert(!Look(mid).knowsAdviser(other, card), s"$other $card")))
    val done = Outcomes.finished(resumeBy(mid, power, played, first.tree,
      Pilgrimage.inspectDecisionId,
      DecisionAnswer.ChooseOneAnswer(DecisionQuery.Inspect.Done), p1,
      keepingOrder))
    assertJournalRebuilds(twoDenizensAtSite, first.events ++ done.events,
      done.treeless)
    assertWireRoundTrips(first.events ++ done.events)

  test("the shuffle includes the denizens just moved, so they can come " +
      "back"):
    val (_, mid) = parkedOnInspect(twoDenizensAtSite,
      WalkerDiceFixture.reversingShuffle)
    assertEquals(Look(mid).discardPile(region),
      Vector[WorldCardId](wrestlers, homesteaders))
    assertEquals(mid.game.campaign.dispossessed, Vector[WorldCardId](
      BewitchCard.id, ScryerCard.id, DazzleCard.id))

  test("a ruined edifice goes to the bottom of the edifice deck with its " +
      "favor returned, uncounted; an intact edifice stays"):
    val ready = pilgrimageHeld.denizen(homesteaders, at = homeSite)
      .edifice(GreatClockCard, EdificeSide.Ruined, at = homeSite)
      .tokens(GreatClockCard.id, favor = 1)
      .edifice(ForestHornCard, EdificeSide.Intact, at = homeSite)
      .bankFavor(Suit.Arcane, 2).ready
    val (first, mid) = parkedOnInspect(ready)
    assertEquals(Look(mid).cardsAt(homeSite),
      Vector[CardId](ForestHornCard.id))
    assertEquals(mid.game.current.commonCards.edificeDeck.lastOption,
      Some(GreatClockCard.id))
    assertEquals(Look(mid).bankFavor(Suit.Arcane), 2 + 1)
    assertEquals(Look(mid).discardPile(region),
      Vector[WorldCardId](DazzleCard.id))
    assertEquals(said(power, first.events), sentLine(1, "denizen"))

  test("a lock-icon denizen stays at the site with its tokens"):
    val ready = pilgrimageHeld
      .denizen(InitiationRiteCard, at = homeSite)
      .tokens(InitiationRiteCard.id, favor = 1)
      .denizen(homesteaders, at = homeSite).ready
    val (first, mid) = parkedOnInspect(ready)
    assertEquals(Look(mid).cardsAt(homeSite),
      Vector[CardId](InitiationRiteCard.id))
    assertEquals(Look(mid).tokensOn(InitiationRiteCard.id), Tokens(1, 0))
    assertEquals(Look(mid).discardPile(region),
      Vector[WorldCardId](DazzleCard.id))
    assertEquals(said(power, first.events), sentLine(1, "denizen"))

  test("a denizen Ancient Bloodline locks for p1 stays, nothing is " +
      "shuffled or drawn, and the line says none went"):
    // p2 rules p1's site and holds the Bloodline; the reversing source
    // would show any shuffle.
    val ready = pilgrimageHeld.warbandsAt(homeSite, p2, 1)
      .adviser(p2, AncientBloodlineCard.id)
      .denizen(homesteaders, at = homeSite).tokens(homesteaders, favor = 1)
      .ready
    val done = Outcomes.finished(fire(ready, power, played,
      WalkerDiceFixture.reversingShuffle))
    assertEquals(Look(done.treeless).cardsAt(homeSite),
      Vector[CardId](homesteaders))
    assertEquals(Look(done.treeless).tokensOn(homesteaders), Tokens(1, 0))
    assertEquals(done.treeless.game.campaign.dispossessed,
      Vector[WorldCardId](DazzleCard.id, ScryerCard.id, BewitchCard.id))
    assertEquals(said(power, done.events), Vector(NoteText.Said("used.none",
      "Sent no denizens on Pilgrimage.", covers = false)))
