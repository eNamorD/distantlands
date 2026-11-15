package distantlands.gameplay.powers.banner

import distantlands.gameplay.{FacedownAdviserPlay, GrandMaskCard,
  RuleSourceIndex, SiteRulers}
import distantlands.gameplay.actions.CardPlayRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DesolateShoreSite, DunesSite}
import distantlands.gameplay.powers.{NoteText, PowerFixture, SearchFixture,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.MessengerCard
import distantlands.gameplay.powers.campaign.{CodeOfHonorCard,
  ShieldWallCard, ToweringRampartCard}
import distantlands.gameplay.powers.economy.WildPasturesCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, Look, PlacementPicks, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Grand Council, the People's Favor's altered face: "When playing a card,
  * you may play it to any Imperial site on the map, and you may discard a
  * denizen from that site first." (Foundations design, slice 5). */
class PeoplesFavorGrandCouncilSuite extends munit.FunSuite:
  import SearchFixture.{keep, placeAt, placeReplacing, rules, start}

  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  /** The Beast card each Search keeps and plays. */
  private val played = ErrandBoyCard.id
  private val fillers = Vector(CodeOfHonorCard.id, MessengerCard.id,
    ShieldWallCard.id)
  /** p1's pawn site: room for three, no Homeland. */
  private val home = AncientCitySite.id
  /** The Empire's sites, each with room for one and no Homeland. */
  private val peaks = BrokenPeaksSite.id
  private val shore = DesolateShoreSite.id
  private val dunes = DunesSite.id
  /** p3's site. */
  private val giant = BuriedGiantSite.id

  /** p1, an Exile, in Act at Ancient City with 5 Supply and `played` on the
    * world deck, holding the People's Favor on its Grand Council face. p2
    * is the Chancellor; the Empire holds Broken Peaks, where `atPeaks`
    * stand, and Desolate Shore. p3 rules Buried Giant. */
  private def councilHolderBesideTheEmpire(atPeaks: Vector[DenizenId])
      : Table =
    atPeaks.foldLeft(councilHolderWithEmpireSeated(_.imperial(p2), p2))(
      (table, card) => table.denizen(card, at = peaks))

  /** p1 in Act at Ancient City with 5 Supply and `played` on the world
    * deck, holding the People's Favor on its Grand Council face, with the
    * Empire's roles set by `seat`. One warband of `imperial`, who plays for
    * the Empire, holds Broken Peaks and one holds Desolate Shore. p3 rules
    * Buried Giant. */
  private def councilHolderWithEmpireSeated(seat: Table => Table,
      imperial: PlayerId): Table =
    seat(Table.start).pawn(p1, home).worldDeckTop(played).supply(p1, 5)
      .peoplesFavor(Some(p1), 1, PeoplesFavorFace.GrandCouncil)
      .warbandsAt(peaks, imperial, 1).warbandsAt(shore, imperial, 1)
      .warbandsAt(giant, p3, 1)

  /** Searches with no modifier and keeps `played`, parked on its
    * placement. The events are the whole Search's so far. */
  private def searchedAndParkedOnPlacement(ready: ReadyGame): OathTransition = accepted(for
    started <- start(ready)
    kept <- keep(started, played)
  yield kept.copy(events = started.events ++ kept.events))

  /** The parked placement's site slots, each with the card it replaces. */
  private def slotsOf(at: OathTransition): Vector[(DecisionOptionRef,
      Option[CardId])] =
    PlacementPicks.siteSlotOptions(PlacementPicks.parkedOptions(at.state,
      walkerPowers)).map(option => option.ref -> PlacementPicks.replaces(
      option))

  private def councilSaid(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(PeoplesFavorGrandCouncil.id,
      Vector(CardPlayRules.discardFirst), events)

  test("the source index names this power on the Grand Council face"):
    val favor = RuleSourceIndex.enumerate(catalog,
      councilHolderBesideTheEmpire(Vector.empty).ready).find(_.source ==
        RuleSourceRef.Banner(Banner.PeoplesFavor.key))
    assertEquals(favor.map(_.powerIds),
      Some(Vector(PeoplesFavorGrandCouncil.id)))

  test("its holder may play to every Imperial site after the pawn site, " +
      "and to no site another player or nobody rules"):
    val ready = councilHolderBesideTheEmpire(Vector.empty).ready
    assertEquals(SiteRulers.imperialSites(ready), Vector(peaks, shore))
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(home, 0) -> None,
      DecisionOptionRef.SiteSlot(peaks, 0) -> None,
      DecisionOptionRef.SiteSlot(shore, 0) -> None))

  test("a full Imperial site offers its denizen and a ruined edifice as " +
      "Replace, and shows an intact edifice locked"):
    val ready = councilHolderBesideTheEmpire(fillers.take(1))
      .edifice(ToweringRampartCard, EdificeSide.Ruined, at = shore)
      .warbandsAt(dunes, p2, 1)
      .edifice(WildPasturesCard, EdificeSide.Intact, at = dunes).ready
    def slot(site: SiteId) =
      DecisionOption.SiteSlot(DecisionOptionRef.SiteSlot(site, 0))
    assertEquals(PlacementPicks.siteSlotOptions(PlacementPicks.parkedOptions(
      searchedAndParkedOnPlacement(ready).state, walkerPowers)), Vector(slot(home),
      DecisionOption.Replacing(slot(peaks), fillers(0)),
      DecisionOption.Replacing(slot(shore), ToweringRampartCard.id),
      DecisionOption.Unavailable(DecisionOption.Replacing(slot(dunes),
        WildPasturesCard.id),
        OptionReason.Written("Locked: cannot be discarded"))))

  test("a Citizen holder plays to every Imperial site, as an Exile does"):
    val ready = councilHolderWithEmpireSeated(_.imperial(p2, p1), p2).ready
    assertEquals(Imperials.role(ready, p1), Some(Role.Citizen))
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(home, 0) -> None,
      DecisionOptionRef.SiteSlot(peaks, 0) -> None,
      DecisionOptionRef.SiteSlot(shore, 0) -> None))

  test("the Chancellor holder plays to every Imperial site, their own " +
      "warbands' sites"):
    val ready = councilHolderWithEmpireSeated(_.imperial(p1), p1).ready
    assertEquals(Imperials.role(ready, p1), Some(Role.Chancellor))
    assertEquals(SiteRulers.imperialSites(ready), Vector(peaks, shore))
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(home, 0) -> None,
      DecisionOptionRef.SiteSlot(peaks, 0) -> None,
      DecisionOptionRef.SiteSlot(shore, 0) -> None))

  test("its discard holds only at Imperial sites: a full pawn site the " +
      "Empire does not hold offers no Replace"):
    val ready = fillers.foldLeft(councilHolderBesideTheEmpire(Vector.empty))(
      (table, card) => table.denizen(card, at = home)).ready
    assert(!SiteRulers.isImperialSite(ready, home))
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(peaks, 0) -> None,
      DecisionOptionRef.SiteSlot(shore, 0) -> None))

  test("its discard holds at a pawn site the Empire holds too"):
    val ready = councilHolderBesideTheEmpire(Vector.empty)
      .warbandsAt(home, p2, 1).denizen(fillers(1), at = home).ready
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)).take(2), Vector(
      DecisionOptionRef.SiteSlot(home, 1) -> None,
      DecisionOptionRef.SiteSlot(home, 0) -> Some(fillers(1))))

  test("a play to a distant Imperial site in place of its card discards " +
      "that card to the pile after the site's region, with the Grand " +
      "Council's line"):
    val ready = councilHolderBesideTheEmpire(fillers.take(1)).ready
    val asked = searchedAndParkedOnPlacement(ready)
    assertEquals(councilSaid(asked.events), Vector.empty)
    val done = accepted(placeReplacing(asked, played, PlayedAs.AtSite,
      fillers(0)))
    val end = SearchFixture.after(done)
    assertEquals(Look(end).cardsAt(peaks), Vector[CardId](played))
    assert(Look(end).discardAfter(peaks).contains(fillers(0)))
    assertEquals(Look(end).pawn(p1), home)
    assertEquals(councilSaid(done.events), Vector(NoteText.Said(
      "discard-first", s"May discard a card at ${peaks.value} first.",
      covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready,
      asked.events ++ done.events), end)
    assertWireRoundTrips(asked.events ++ done.events)

  test("a play to an Imperial site's empty slot discards nothing and " +
      "writes no line"):
    val asked = searchedAndParkedOnPlacement(councilHolderBesideTheEmpire(Vector.empty).ready)
    val done = accepted(placeAt(asked, played,
      DecisionOptionRef.SiteSlot(shore, 0)))
    assertEquals(Look(SearchFixture.after(done)).cardsAt(shore),
      Vector[CardId](played))
    assertEquals(councilSaid(done.events), Vector.empty)

  test("with no Imperial site on the map, as with no Empire, only the pawn " +
      "site is offered and nothing there may be replaced"):
    val ready = Table.start.pawn(p1, home).worldDeckTop(played).supply(p1, 5)
      .peoplesFavor(Some(p1), 1, PeoplesFavorFace.GrandCouncil)
      .denizen(fillers(0), at = home).ready
    assertEquals(SiteRulers.imperialSites(ready), Vector.empty)
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(home, 1) -> None))

  test("nobody else plays to the Imperial sites: not another holder, not " +
      "an unheld banner, not the Mob face"):
    val others: Vector[(String, Table => Table)] = Vector(
      "held by p3" -> (_.peoplesFavor(Some(p3), 1,
        PeoplesFavorFace.GrandCouncil)),
      "unheld" -> (_.peoplesFavor(None, 1, PeoplesFavorFace.GrandCouncil)),
      "Mob face" -> (_.peoplesFavor(Some(p1), 1)))
    others.foreach { case (label, favor) =>
      val ready = favor(councilHolderBesideTheEmpire(Vector.empty)).ready
      assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)),
        Vector(DecisionOptionRef.SiteSlot(home, 0) -> None), label)
    }

  test("it applies to a facedown adviser played to a site as well"):
    val ready = PowerFixture.asAdviser(
      councilHolderBesideTheEmpire(fillers.take(1)).ready, played,
      Orientation.FaceDown)
    val started = accepted(FacedownAdviserPlay.parkedOnPlacement(rules,
      Ready(ready), p1, played))
    assertEquals(slotsOf(started), Vector(
      DecisionOptionRef.SiteSlot(home, 0) -> None,
      DecisionOptionRef.SiteSlot(peaks, 0) -> Some(fillers(0)),
      DecisionOptionRef.SiteSlot(shore, 0) -> None))

  test("the Grand Mask leaves the Imperial sites Imperial: its wearer may " +
      "still play to them"):
    val ready = councilHolderBesideTheEmpire(Vector.empty)
      .relic(p1, GrandMaskCard).ready
    assertEquals(slotsOf(searchedAndParkedOnPlacement(ready)), Vector(
      DecisionOptionRef.SiteSlot(home, 0) -> None,
      DecisionOptionRef.SiteSlot(peaks, 0) -> None,
      DecisionOptionRef.SiteSlot(shore, 0) -> None))
