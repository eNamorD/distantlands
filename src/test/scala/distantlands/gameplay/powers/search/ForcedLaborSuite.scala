package distantlands.gameplay.powers.search

import distantlands.gameplay.BanditCrownCard
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.{NoteText, PayOrAsk, SearchFixture,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.campaign.{MercenariesCard, WrestlersCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, PlacementPicks, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.{accepted, present}
import distantlands.testkit.StartPreview

/** Forced Labor's ruler toll on a Search. The toll itself, including the
  * favor burnt when bandits rule, is shared with Curfew (`RulerToll`) and
  * pinned in [[distantlands.gameplay.powers.economy.CurfewSuite]]. */
class ForcedLaborSuite extends munit.FunSuite:
  import SearchFixture.rules

  private val labor = ForcedLaborCard.id
  private val power = ForcedLabor
  /** p1's pawn site. */
  private val home = AncientCitySite.id
  /** Where Forced Labor stands. */
  private val peaks = BrokenPeaksSite.id
  /** Plain denizens for the world deck; none is played. */
  private val deckTop = Vector(WrestlersCard.id, ErrandBoyCard.id,
    MercenariesCard.id)
  private val startingSupply = 5

  /** p1 in Act at Ancient City with `startingSupply` Supply and the start's
    * 1 favor, the world deck topped by three plain cards, and Forced Labor
    * at Broken Peaks. Nobody rules either site yet. */
  private def laborTable: Table = Table.start.pawn(p1, at = home)
    .worldDeckTop(deckTop*)
    .supply(p1, startingSupply).denizen(labor, at = peaks)

  /** p2 rules Broken Peaks, so Forced Labor is p2's, and Ancient City, where
    * p1's pawn stands. */
  private def rivalRulesBoth: Table =
    laborTable.warbandsAt(peaks, p2, 1).warbandsAt(home, p2, 1)

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(power.id, power.noteKeys, events)

  private val automatic = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)

  /** A Search of the world deck, with Forced Labor's step answered with
    * `choice`. */
  private def searchedAnswering(ready: ReadyGame, choice: DecisionOptionRef)(
      using munit.Location): OathTransition =
    val started = accepted(SearchFixture.start(ready))
    val done = accepted(rules.resolveWalker(started.state, p1,
      ForcedLabor.decisionId, DecisionAnswer.ChooseOneAnswer(choice)))
    done.copy(events = started.events ++ done.events)

  test("an enemy whose pawn stands at a site the ruler rules gives the ruler " +
      "1 favor on top of the Search's Supply"):
    val ready = rivalRulesBoth.ready
    val started = searchedAnswering(ready, PayOrAsk.pay)
    val after = Look(SearchFixture.after(started))
    assertEquals(after.favor(p1), 0)
    assertEquals(after.favor(p2), Look(ready).favor(p2) + 1)
    // The first world Search costs 2 Supply.
    assertEquals(after.supply(p1), startingSupply - 2)
    assertEquals(said(started.events), Vector(NoteText.Said("gave",
      s"Gave 1 favor to ${p2.value}.", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, started.events),
      SearchFixture.after(started))
    assertWireRoundTrips(started.events)

  test("a searcher who cannot pay may start the Search only to ask its " +
      "ruler; under bandits, not at all"):
    val startable = (ready: ReadyGame) =>
      StartPreview.opens(catalog, ready, p1, ActionRef.Search, automatic)
    assert(startable(rivalRulesBoth.favor(p1, 0).ready))
    val banditBroke = laborTable.bandits(peaks, 1).bandits(home, 1)
      .favor(p1, 0).ready
    assert(!startable(banditBroke))
    assert(SearchFixture.start(banditBroke).isLeft)

  test("the ruler searches without paying, and so does an enemy whose pawn " +
      "stands at a site the ruler does not rule"):
    val rulerSearches = laborTable.warbandsAt(peaks, p1, 1)
      .warbandsAt(home, p1, 1).ready
    val unruledSite = laborTable.warbandsAt(peaks, p2, 1).ready
    Vector(rulerSearches, unruledSite).foreach { ready =>
      val started = accepted(SearchFixture.start(ready))
      assertEquals(Look(SearchFixture.after(started)).favor(p1),
        Look(ready).favor(p1))
      assertEquals(said(started.events), Vector.empty)
    }

  test("an enemy of a player ruler may ask to search without paying; once " +
      "granted, it gives nothing"):
    val ready = rivalRulesBoth.ready
    val free = ForcedLabor.free(home)
    val asked = present(PlacementPicks.parkedOptions(
      accepted(SearchFixture.start(ready)).state, automatic)
      .find(_.ref == free), "the free option")
    assertEquals(asked, DecisionOption.Unavailable(
      DecisionOption.Button(free, "Search without paying"),
      OptionReason.Askable(ForcedLabor.id, ForcedLabor.noFreeSearchConsent(
        PowerSourceRef.Card(ForcedLaborCard.id)),
        Vector(ConsentAsk(p2, Vector(ForcedLabor.id))))))
    val granted = ready.copy(consent = ConsentState().granting(Vector(
      ConsentKey(ForcedLabor.id, p1, ForcedLabor.decisionId, free)),
      ConsentTurn.of(ready)))
    val done = searchedAnswering(granted, free)
    assertEquals(Look(SearchFixture.after(done)).favor(p1),
      Look(ready).favor(p1))
    assertEquals(said(done.events), Vector(NoteText.Said("waived",
      s"Gave nothing, with ${p2.value}'s consent.", covers = false)))

  test("under bandits the Bandit Crown's holder may let a searcher off; " +
      "paying still burns"):
    val ready = laborTable.bandits(peaks, 1).bandits(home, 1)
      .relic(p2, BanditCrownCard).ready
    val free = PayOrAsk.free(home.value)
    val options = PlacementPicks.parkedOptions(
      accepted(SearchFixture.start(ready)).state, automatic)
    assertEquals(options.find(_.ref == PayOrAsk.pay),
      Some(DecisionOption.Button(PayOrAsk.pay, "Burn 1 favor")))
    assert(present(options.find(_.ref == free), "the free option").askable)
    val paid = searchedAnswering(ready, PayOrAsk.pay)
    val after = Look(SearchFixture.after(paid))
    assertEquals(after.favor(p1), 0)
    assertEquals(after.favor(p2), Look(ready).favor(p2))
    assertEquals(said(paid.events), Vector(NoteText.Said("burned",
      "Burned 1 favor.", covers = false)))
