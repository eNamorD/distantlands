package oathdigital.gameplay.powers.action

import oathdigital.catalog.holding.CrackedHornCard
import oathdigital.gameplay.powers.{NoteText, PowerFixture}
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, MenuPlay, PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.{assertWireRoundTrips, ready}
import oathdigital.testkit.Unwrap.accepted

/** Underground Library (catalog batch 9, slice 10): peek at the region's
  * relics, then swap one with a relic you hold, facedown. */
class UndergroundLibrarySuite extends munit.FunSuite:
  import PowerFixture._
  import PhasePowerHarness.{awaits, backToActing, replayed, rules}

  private val library = UndergroundLibraryCard.id
  private val source = DecisionOptionRef.Edifice(library)
  private val home = AncientCitySite.id
  /** The Cradle's other site. */
  private val peaks = BrokenPeaksSite.id
  /** A Provinces site. */
  private val giant = BuriedGiantSite.id
  private val atPeaks = DowsingSticksCard.id
  private val atHome = CrackedHornCard.id
  private val farAway = BarbedNetCard.id
  private val held = WhistleCard.id

  /** p1's Act at Ancient City beside the intact Library with 2 favor,
    * holding Whistle on `heldFace`; Dowsing Sticks lies at Broken Peaks,
    * Cracked Horn at Ancient City and Barbed Net at Buried Giant. */
  private def staged(heldFacedown: Boolean = false, holding: Boolean = true,
      relics: Table => Table = _.relicAt(DowsingSticksCard, peaks)
        .relicAt(CrackedHornCard, home).relicAt(BarbedNetCard, giant))
      : ReadyGame =
    val table = Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, home)
      .edifice(library, EdificeSide.Intact, at = home).favor(actor, 2)
    relics(if holding then table.relic(actor, WhistleCard,
      facedown = heldFacedown) else table).ready

  private def use(state: ReadyGame) = rules.startWalker(
    Ready(MenuPlay.withoutMenu(state)),
    ActionRef.UsePower(UndergroundLibrary.id), actor, Vector(source))

  private def choose(state: OathState, decisionId: String,
      ref: DecisionOptionRef) = rules.resolveWalker(state, actor, decisionId,
    DecisionAnswer.ChooseOneAnswer(ref))

  private def knows(state: ReadyGame, relic: RelicId): Boolean =
    state.knowledge.siteRelics.getOrElse(actor, Map.empty)
      .valuesIterator.exists(_.contains(relic))

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(UndergroundLibrary, events)

  test("it peeks at the region's relics, asks which to take and which to " +
      "give, and swaps them, the given relic facedown at the site"):
    val start = staged()
    val parked = accepted(use(start))
    assert(awaits(parked, UndergroundLibrary.takeDecisionId))
    val peekedAt = ready(parked.state)
    assertEquals(Look(peekedAt).tokensOn(library), Tokens(1, 0))
    assert(knows(peekedAt, atPeaks) && knows(peekedAt, atHome))
    assert(!knows(peekedAt, farAway))
    val gave = accepted(choose(parked.state, UndergroundLibrary.takeDecisionId,
      DecisionOptionRef.SiteRelicSlot(peaks, 0)))
    assert(awaits(gave, UndergroundLibrary.giveDecisionId))
    val done = accepted(choose(gave.state, UndergroundLibrary.giveDecisionId,
      DecisionOptionRef.RelicSlot(actor, 0)))
    assert(backToActing(done))
    val after = ready(done.state)
    assertEquals(Look(after).player(actor).relics,
      Vector(RelicState(atPeaks, Orientation.FaceDown, Tokens.empty)))
    assertEquals(after.game.current.map.sites(peaks).relics,
      Vector(RelicState(held, Orientation.FaceDown, Tokens.empty)))
    val events = parked.events ++ gave.events ++ done.events
    assertEquals(said(events), Vector(
      NoteText.Said("used.peeked", s"${actor.value} peeked at the relics " +
        s"in the Cradle: ${atHome.value}, ${atPeaks.value}.", covers = true),
      NoteText.Said(NoteKey.Used, s"Swapped ${held.value} for " +
        s"${atPeaks.value} at ${peaks.value}.", covers = true)))
    assertEquals(replayed(start, events), Right(done.state))
    assertWireRoundTrips(events)

  test("a relic held facedown is given as it is"):
    val parked = accepted(use(staged(heldFacedown = true)))
    val gave = accepted(choose(parked.state, UndergroundLibrary.takeDecisionId,
      DecisionOptionRef.SiteRelicSlot(home, 0)))
    val done = accepted(choose(gave.state, UndergroundLibrary.giveDecisionId,
      DecisionOptionRef.RelicSlot(actor, 0)))
    val after = ready(done.state)
    assertEquals(after.game.current.map.sites(home).relics.map(_.id),
      Vector(held))
    assertEquals(Look(after).player(actor).relics.map(_.id), Vector(atHome))
    assert(!PhasePowerHarness.ops(done.events).exists(_.isInstanceOf[Flip]))

  test("with no relic in the region, or none held, nothing is asked and " +
      "the cost stays paid"):
    val bareRegion = accepted(use(staged(relics =
      _.relicAt(BarbedNetCard, giant))))
    assert(backToActing(bareRegion))
    assertEquals(Look(ready(bareRegion.state)).tokensOn(library),
      Tokens(1, 0))
    assertEquals(said(bareRegion.events), Vector(NoteText.Said("used.none",
      "No relic could be swapped.", covers = true)))
    val noneHeld = accepted(use(staged(holding = false)))
    assert(backToActing(noneHeld))
    assertEquals(said(noneHeld.events).map(_.key),
      Vector("used.peeked", "used.none"))

  test("the ruined Drowned Library offers no action"):
    val ruined = Table.from(quietTable).turn(actor, Phase.Act)
      .pawn(actor, home).edifice(library, EdificeSide.Ruined, at = home)
      .favor(actor, 2).ready
    assert(!PhasePowerHarness.usableNow(ruined).exists(
      _.power.id == UndergroundLibrary.id))
