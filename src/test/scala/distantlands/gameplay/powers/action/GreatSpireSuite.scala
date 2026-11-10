package oathdigital.gameplay.powers.action

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.{NoteText, PowerFixture}
import oathdigital.gameplay.powers.setup.GreatSpireCard
import oathdigital.gameplay.powers.whenplayed.{BewitchCard, DazzleCard,
  ThreateningRoarCard}
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, Look, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.{assertWireRoundTrips, ready}
import oathdigital.testkit.Unwrap.accepted

/** Great Spire (E11 intact, catalog batch 10, slice 6): pay 1 secret, put
  * up to 3 shuffled Dispossessed cards on top of the Spire's region's
  * discard pile, then send as many of that pile's bottom denizens to the
  * Dispossessed (ruling 7). */
class GreatSpireSuite extends munit.FunSuite:
  import ActPowerHarness._
  import PowerFixture._

  private val spire = GreatSpireCard.id
  private val source = DecisionOptionRef.Edifice(spire)
  private val home = AncientCitySite.id
  private val region: Region = quietTable.game.current.map.regionOf(home).get
  private val pileName: String =
    SearchSource.name(SearchSource.RegionalDiscard(region))
  private val conspiracy = VisionRules.Conspiracy

  private val keepingOrder: OathRules =
    rules(WalkerDiceFixture.shufflingBy(count => (0 until count).toVector))
  private val reversing: OathRules = rules(WalkerDiceFixture.reversingShuffle)

  private val six = Vector[WorldCardId](DazzleCard.id, ScryerCard.id,
    BewitchCard.id, ThreateningRoarCard.id, FaeMerchantCard.id,
    SecondChanceCard.id)

  /** p1's Act at Ancient City beside the intact Great Spire, with 2 faceup
    * secrets. The Dispossessed holds `dispossessed`, head first, and the
    * Spire's region's discard pile holds `pile`, bottom first. */
  private def spireWith(dispossessed: Vector[WorldCardId],
      pile: Vector[WorldCardId]): ReadyGame =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, home)
      .edifice(GreatSpireCard, EdificeSide.Intact, at = home)
      .secrets(actor, 2).dispossessed(dispossessed*)
      .discarded(region, pile*).ready

  private def spireUsed(start: ReadyGame, under: OathRules = keepingOrder)
      : OathTransition = accepted(use(under, start, GreatSpire.id, source))

  private def swappedLine(count: Int): Vector[NoteText.Said] =
    Vector(NoteText.Said(NoteKey.Used,
      s"Swapped $count cards between the Dispossessed and the $pileName.",
      covers = false))

  test("it pays 1 secret, tops the pile with 3 Dispossessed cards, and " +
      "sends the pile's bottom 3 denizens to the Dispossessed past the " +
      "Vision"):
    val start = spireWith(six, Vector(EldersCard.id, conspiracy,
      MessengerCard.id, HomesteadersCard.id))
    val done = spireUsed(start)
    assert(PhasePowerHarness.backToActing(done))
    val after = ready(done.state)
    assertEquals(Look(after).discardPile(region), Vector[WorldCardId](
      conspiracy, DazzleCard.id, ScryerCard.id, BewitchCard.id))
    assertEquals(after.game.campaign.dispossessed, Vector[WorldCardId](
      ThreateningRoarCard.id, FaeMerchantCard.id, SecondChanceCard.id,
      EldersCard.id, MessengerCard.id, HomesteadersCard.id))
    assertEquals(Look(after).tokensOn(spire), Tokens(0, 1))
    assertEquals(NoteText.said(GreatSpire, done.events), swappedLine(3))

  test("nobody learns a card the Spire draws"):
    val after = ready(spireUsed(spireWith(six, Vector(EldersCard.id,
      MessengerCard.id, HomesteadersCard.id))).state)
    Vector(Table.p1, Table.p2, Table.p3).foreach(player =>
      Vector(DazzleCard.id, ScryerCard.id, BewitchCard.id).foreach(card =>
        assert(!Look(after).knowsAdviser(player, card), s"$player $card")))

  test("the draw takes the shuffle's order, and replay applies the " +
      "recorded order"):
    val start = spireWith(six, Vector(EldersCard.id, MessengerCard.id,
      HomesteadersCard.id))
    val done = spireUsed(start, reversing)
    val after = ready(done.state)
    assertEquals(Look(after).discardPile(region), Vector[WorldCardId](
      SecondChanceCard.id, FaeMerchantCard.id, ThreateningRoarCard.id))
    assertEquals(after.game.campaign.dispossessed, Vector[WorldCardId](
      BewitchCard.id, ScryerCard.id, DazzleCard.id, EldersCard.id,
      MessengerCard.id, HomesteadersCard.id))
    // Replay has no shuffle source: it applies the recorded order.
    assertEquals(replayed(rules(), start, done.events), after)
    assertWireRoundTrips(done.events)

  test("a pile with fewer than 3 denizens sends back some of the cards " +
      "just added"):
    val after = ready(spireUsed(spireWith(six,
      Vector(EldersCard.id, conspiracy))).state)
    assertEquals(Look(after).discardPile(region),
      Vector[WorldCardId](conspiracy, BewitchCard.id))
    assertEquals(after.game.campaign.dispossessed, Vector[WorldCardId](
      ThreateningRoarCard.id, FaeMerchantCard.id, SecondChanceCard.id,
      EldersCard.id, DazzleCard.id, ScryerCard.id))

  test("a Dispossessed of 2 cards moves 2 in each direction"):
    val done = spireUsed(spireWith(Vector(DazzleCard.id, ScryerCard.id),
      Vector(EldersCard.id, MessengerCard.id, HomesteadersCard.id)))
    val after = ready(done.state)
    assertEquals(Look(after).discardPile(region), Vector[WorldCardId](
      HomesteadersCard.id, DazzleCard.id, ScryerCard.id))
    assertEquals(after.game.campaign.dispossessed,
      Vector[WorldCardId](EldersCard.id, MessengerCard.id))
    assertEquals(NoteText.said(GreatSpire, done.events), swappedLine(2))
