package oathdigital.gameplay.powers.action

import oathdigital.gameplay.powers.{NoteText, PowerFixture}
import oathdigital.gameplay.powers.campaign.WrestlersCard
import oathdigital.gameplay.powers.travel.BrokenPeaksSite
import oathdigital.gameplay.powers.wake.AncientCitySite
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, MenuPlay, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.{assertWireRoundTrips, ready}
import oathdigital.testkit.Unwrap.accepted

/** Giant's Rampage (catalog batch 9, slice 8): bury a denizen anywhere,
  * then the Rampage and the user's pawn go to that site. */
class GiantsRampageSuite extends munit.FunSuite:
  import PowerFixture._
  import PhasePowerHarness.{awaits, backToActing, continuedPast, replayed,
    rules}

  private val rampage = GiantsRampage.edifice
  private val source = DecisionOptionRef.Edifice(rampage)
  private val power = GiantsRampage.forCatalog(catalog)
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id

  /** p1's Act at Ancient City beside the ruined Rampage, with 1 favor and
    * 2 faceup secrets, and `cards` placed by the caller. */
  private def staged(cards: Table => Table = identity): ReadyGame =
    cards(Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, home)
      .edifice(rampage, EdificeSide.Ruined, at = home)
      .favor(actor, 1).secrets(actor, 2)).ready

  private def use(state: ReadyGame) = rules.startWalker(
    Ready(MenuPlay.withoutMenu(state)),
    ActionRef.UsePower(GiantsRampage.id), actor, Vector(source))

  private def choose(state: OathState, card: DenizenId) = rules.resolveWalker(
    state, actor, GiantsRampage.decisionId,
    DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Denizen(card)))

  private def deckBottom(state: ReadyGame): Option[WorldCardId] =
    state.game.current.commonCards.worldDeck.lastOption

  test("burying a denizen at another site moves the Rampage and the pawn " +
      "there, with the line, and the journal replays"):
    val start = staged(_.denizen(WrestlersCard, at = peaks)
      .tokens(WrestlersCard, favor = 1))
    val bank = Look(start).bankFavor(WrestlersCard.suit)
    val parked = accepted(use(start))
    assert(awaits(parked, GiantsRampage.decisionId))
    assertEquals(Look(ready(parked.state)).tokensOn(rampage), Tokens(1, 2))
    val done = accepted(choose(parked.state, WrestlersCard.id))
    assert(backToActing(done))
    val after = ready(done.state)
    assertEquals(deckBottom(after), Some(WrestlersCard.id))
    assertEquals(Look(after).bankFavor(WrestlersCard.suit), bank + 1)
    assertEquals(Look(after).cardsAt(peaks), Vector[CardId](rampage))
    assertEquals(Look(after).cardsAt(home), Vector.empty)
    assertEquals(Look(after).pawn(actor), peaks)
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      NoteKey.Used, s"Buried ${WrestlersCard.id.value} at ${peaks.value}; " +
        s"Giant's Rampage and ${actor.value} now stand there.",
      covers = true)))
    assertEquals(replayed(start, parked.events ++ done.events),
      Right(done.state))
    assertWireRoundTrips(parked.events ++ done.events)

  test("a denizen at the Rampage's own site is buried where it stands"):
    val start = staged(_.denizen(WolvesCard, at = home))
    val done = accepted(choose(accepted(use(start)).state, WolvesCard.id))
    val after = ready(done.state)
    assertEquals(deckBottom(after), Some(WolvesCard.id))
    assertEquals(Look(after).cardsAt(home), Vector[CardId](rampage))
    assertEquals(Look(after).pawn(actor), home)

  test("with no denizen at any site the question shows Continue, the cost " +
      "stays paid and the line says so"):
    val start = staged()
    val parked = accepted(use(start))
    assert(awaits(parked, GiantsRampage.decisionId))
    val done = accepted(continuedPast(parked, GiantsRampage.decisionId))
    assert(backToActing(done))
    assertEquals(Look(ready(done.state)).tokensOn(rampage), Tokens(1, 2))
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      "used.none", "No denizen could be buried.", covers = true)))

  test("the intact Giant's Path offers no action"):
    val intact = Table.from(quietTable).turn(actor, Phase.Act)
      .pawn(actor, home).edifice(rampage, EdificeSide.Intact, at = home)
      .favor(actor, 1).secrets(actor, 2).ready
    assert(!PhasePowerHarness.usableNow(intact).exists(
      _.power.id == GiantsRampage.id))
