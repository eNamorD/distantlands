package distantlands.gameplay.powers.action

import distantlands.gameplay.powers.{NoteText, PowerFixture}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.powers.setup.ForestHornCard
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, MenuPlay, PhasePowerHarness, Table}
import distantlands.testkit.Outcomes.{assertWireRoundTrips, ready}
import distantlands.testkit.Unwrap.accepted

/** Forest Horn (catalog batch 9, slice 8): choose a region and discard its
  * non-Beast denizens. */
class ForestHornSuite extends munit.FunSuite:
  import PowerFixture._
  import PhasePowerHarness.{awaits, backToActing, replayed, rules}

  private val horn = ForestHornCard.id
  private val source = DecisionOptionRef.Edifice(horn)
  private val power = ForestHorn
  private val home = AncientCitySite.id
  /** The Cradle's other site. */
  private val peaks = BrokenPeaksSite.id
  /** A Provinces site. */
  private val giant = BuriedGiantSite.id

  /** p1's Act at Ancient City beside the intact Horn with 2 faceup secrets;
    * Wrestlers (Order) and Wolves (Beast) stand at Broken Peaks, Elders
    * (Nomad) at Buried Giant, and the ruined School of Vines (Beast) at
    * Ancient City. */
  private def staged: ReadyGame =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, home)
      .edifice(horn, EdificeSide.Intact, at = home).secrets(actor, 2)
      .denizen(WrestlersCard, at = peaks).denizen(WolvesCard, at = peaks)
      .denizen(EldersCard, at = giant)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = home).ready

  private def use(state: ReadyGame) = rules.startWalker(
    Ready(MenuPlay.withoutMenu(state)), ActionRef.UsePower(ForestHorn.id),
    actor, Vector(source))

  private def choose(state: OathState, region: Region) = rules.resolveWalker(
    state, actor, ForestHorn.decisionId,
    DecisionAnswer.ChooseOneAnswer(ForestHorn.button(region)))

  test("the Cradle's non-Beast denizens are discarded; Beast cards, the " +
      "Horn and another region's cards stay"):
    val start = staged
    val parked = accepted(use(start))
    assert(awaits(parked, ForestHorn.decisionId))
    assertEquals(Look(ready(parked.state)).tokensOn(horn), Tokens(0, 2))
    val done = accepted(choose(parked.state, Region.Cradle))
    assert(backToActing(done))
    val after = ready(done.state)
    assertEquals(Look(after).cardsAt(peaks), Vector[CardId](WolvesCard.id))
    assertEquals(Look(after).cardsAt(home).toSet,
      Set[CardId](horn, SchoolOfVinesCard.id))
    assertEquals(Look(after).cardsAt(giant), Vector[CardId](EldersCard.id))
    assert(Look(after).inRegionalDiscard(WrestlersCard.id))
    assertEquals(NoteText.said(power, done.events), Vector(NoteText.Said(
      NoteKey.Used, s"Discarded ${WrestlersCard.id.value}.", covers = true)))
    assertEquals(replayed(start, parked.events ++ done.events),
      Right(done.state))
    assertWireRoundTrips(parked.events ++ done.events)

  test("another region's choice discards there, and a region with nothing " +
      "to discard says so"):
    val provinces = accepted(choose(accepted(use(staged)).state,
      Region.Provinces))
    assertEquals(Look(ready(provinces.state)).cardsAt(giant), Vector.empty)
    assertEquals(Look(ready(provinces.state)).cardsAt(peaks).toSet,
      Set[CardId](WrestlersCard.id, WolvesCard.id))
    val hinterland = accepted(choose(accepted(use(staged)).state,
      Region.Hinterland))
    assertEquals(NoteText.said(power, hinterland.events), Vector(
      NoteText.Said("used.none", "Nothing was discarded.", covers = true)))

  test("the ruined Broken Horn offers no action"):
    val ruined = Table.from(quietTable).turn(actor, Phase.Act)
      .pawn(actor, home).edifice(horn, EdificeSide.Ruined, at = home)
      .secrets(actor, 2).ready
    assert(!PhasePowerHarness.usableNow(ruined).exists(
      _.power.id == ForestHorn.id))
