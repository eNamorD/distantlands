package distantlands.gameplay.powers.banner

import distantlands.gameplay.{FacedownAdviserPlay, PlacementFixture}
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{NoteText, PowerFixture, SearchFixture,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.{ArcaneBrokersCard, HomesteadersCard,
  WardOfSilenceCard}
import distantlands.gameplay.powers.cardplay.WelcomingPartyCard
import distantlands.gameplay.powers.rest.{SilverTongue, SilverTongueCard}
import distantlands.gameplay.powers.search.{ErrandBoyCard, LandWardenCard,
  NewsFromAfarCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, PlacementPicks, Refusals,
  Table}
import distantlands.testkit.Table.p3
import distantlands.testkit.Unwrap.accepted

class PeoplesFavorMobSuite extends munit.FunSuite:
  import PowerFixture._
  import SearchFixture.{keep, place, placeReplacing, rules, start}

  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  private val ancientCity = AncientCitySite.id
  /** The Beast card each Search keeps and plays, above two Arcane cards. */
  private val played = ErrandBoyCard.id
  private val drawn = Vector[WorldCardId](played,
    WardOfSilenceCard.id,
    ArcaneBrokersCard.id)
  // Hearth denizens whose powers these tests do not use.
  private val homesteaders = HomesteadersCard.id
  private val landWarden = LandWardenCard.id
  private val newsFromAfar = NewsFromAfarCard.id
  private val capacity = 3 // Ancient City's printed capacity

  /** p1's Act at Ancient City, which holds `siteCards`, searching a world
    * deck topped by Errand Boy, Ward of Silence and Arcane Brokers with 5
    * Supply. The Banner of the People's Favor is as `favor` arranges it: held
    * by p1, on the Mob face, unless a test says otherwise.
    */
  private def searchAtAncientCityHolding(siteCards: Vector[DenizenId],
    favor: ReadyGame => ReadyGame = (r => Table.from(r)
        .peoplesFavor(Some(actor), 1).ready)): ReadyGame =
    favor(
      siteCards.foldLeft(SearchFixture.staged(drawn, supply = 5))(atActorsSite))

  /** Searches with `selected` and keeps the played card, parked on its
    * placement. The events are the whole Search's so far. */
  private def toPlacement(ready: ReadyGame,
      selected: Vector[PowerId] = Vector.empty): OathTransition = (for
    started <- start(ready, selected)
    kept <- keep(started, played)
  yield kept.copy(events = started.events ++ kept.events))
    .fold(error => throw new AssertionError(error.toString), identity)

  /** The parked placement's site slots, each as the card it replaces. */
  private def siteSlots(at: OathTransition): Vector[Option[CardId]] =
    PlacementPicks.parkedSiteSlotReplacements(at.state, walkerPowers)

  private def siteCards(ready: ReadyGame): Vector[CardId] =
    Look(ready).cardsAt(ancientCity)

  private def mobSaid(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(PeoplesFavorMob.id, PeoplesFavorMob.noteKeys, events)

  test("the source index lists Mob on the Mob face and the Grand Council's " +
      "own power on the other"):
    def favor(ready: ReadyGame) = distantlands.gameplay.RuleSourceIndex
      .enumerate(catalog, ready).find(_.source ==
        RuleSourceRef.Banner(Banner.PeoplesFavor.key))
          .getOrElse(fail("no People's Favor source"))
    assertEquals(favor(quietTable).powerIds, Vector(PeoplesFavorMob.id))
    assertEquals(favor(Table.from(quietTable).peoplesFavor(Some(actor), 1,
      PeoplesFavorFace.GrandCouncil).ready)
      .powerIds, Vector(PowerId("banner.peoples-favor.grand-council")))

  test("its holder is offered each site card as Replace beside the empty " +
      "slot, and the empty slot discards nothing"):
    val (kept, other) = (homesteaders, landWarden)
    val ready = searchAtAncientCityHolding(Vector(kept, other))
    assert(capacity > 2, s"the pawn site must have room, capacity $capacity")
    val asked = toPlacement(ready)
    assertEquals(siteSlots(asked), Vector(None, Some(kept), Some(other)))
    val done = accepted(place(asked, played, PlayedAs.AtSite))
    val end = SearchFixture.after(done)
    assertEquals(siteCards(end), Vector[CardId](kept, other, played))
    assertEquals(ActPowerHarness.replayed(rules, ready,
      asked.events ++ done.events), end)

  test("an empty site has nothing to replace"):
    val asked = toPlacement(searchAtAncientCityHolding(Vector.empty))
    assertEquals(siteSlots(asked), Vector(None))
    assertEquals(siteCards(SearchFixture.after(
      accepted(place(asked, played, PlayedAs.AtSite)))), Vector[CardId](played))

  test("a Replace slot discards by the standard discard: facedown to the " +
      "next region's pile, its favor to the bank, its secret to the holder"):
    val kept = homesteaders
    val ready = Table.from(searchAtAncientCityHolding(Vector(kept)))
      .tokens(kept, favor = 1, secrets = 1).ready
    val asked = toPlacement(ready)
    val done = accepted(placeReplacing(asked, played, PlayedAs.AtSite, kept))
    val end = SearchFixture.after(done)
    assertEquals(siteCards(end), Vector[CardId](played))
    assertEquals(Look(end).discardAfter(ancientCity).last, kept)
    assertEquals(end.banks.favor.getOrElse(Suit.Hearth, 0),
      ready.banks.favor.getOrElse(Suit.Hearth, 0) + 1)
    assertEquals(Look(end).player(actor).board.faceDownSecrets,
      Look(ready).player(actor).board.faceDownSecrets + 1)
    assertEquals(ActPowerHarness.replayed(rules, ready,
      asked.events ++ done.events), end)

  test("a full site with no matching homeland takes the play only through a " +
      "Replace slot, and only for the holder"):
    // Ancient City is no suit's homeland, so the Beast card matches none.
    assertEquals(catalog.site(ancientCity).flatMap(_.homeland), None)
    val fillers = Vector(homesteaders, landWarden, newsFromAfar)
    assertEquals(fillers.size, capacity)
    val asked = toPlacement(searchAtAncientCityHolding(fillers))
    assertEquals(siteSlots(asked), fillers.map(Some(_)))
    val done = accepted(placeReplacing(asked, played, PlayedAs.AtSite,
      fillers.head))
    assertEquals(siteCards(SearchFixture.after(done)).size, capacity)
    assert(siteCards(SearchFixture.after(done)).contains(played))
    val without = toPlacement(searchAtAncientCityHolding(fillers, identity))
    assertEquals(place(without, played, PlayedAs.AtSite).left.toOption,
      Some(OathViolation.InvalidSearchPlacement("the placement offers no way " +
        "to play the card AtSite")))

  test("nobody else may replace: not another holder, not the Grand Council " +
      "face, not the other banner, not an unheld banner"):
    val kept = homesteaders
    val unheld: Vector[(String, ReadyGame => ReadyGame)] = Vector(
      "held by p3" -> (r => Table.from(r).peoplesFavor(Some(p3), 1).ready),
      "Grand Council face" -> (r => Table.from(r)
      .peoplesFavor(Some(actor), 1, PeoplesFavorFace.GrandCouncil).ready),
      "unheld" -> (r => Table.from(r).peoplesFavor(None, 1).ready),
      "Wandering Flame" -> (r => Table.from(r).darkestSecret(Some(actor), 1)
      .ready))
    unheld.foreach { case (label, favor) =>
      val asked = toPlacement(searchAtAncientCityHolding(Vector(kept), favor))
      assertEquals(siteSlots(asked), Vector(None), label)
      assertEquals(siteCards(SearchFixture.after(
        accepted(place(asked, played, PlayedAs.AtSite)))),
        Vector[CardId](kept, played), label)
    }

  test("it applies to a facedown adviser played to a site as well"):
    val kept = homesteaders
    val ready = asAdviser(searchAtAncientCityHolding(Vector(kept)), played,
      Orientation.FaceDown)
    val started = accepted(FacedownAdviserPlay.parkedOnPlacement(rules,
      OathState.Ready(ready), actor, played))
    assert(siteSlots(started).contains(Some(kept)))

  test("it composes with Silver Tongue's limit of two advisers, both from " +
      "the production walker powers"):
    val tongue = SilverTongueCard.id
    val (kept, adviser) = (homesteaders, landWarden)
    // p1 plays `played` from hand at Ancient City, which holds `kept`, while
    // holding the People's Favor on Mob, a faceup Silver Tongue and a
    // facedown `adviser`.
    val who = actor
    val ready = Table.start.hand(who, played).pawn(who, ancientCity)
      .denizen(kept, at = ancientCity).adviser(who, tongue)
      .adviser(who, adviser, facedown = true).peoplesFavor(Some(who), 1).ready
    val tree = PlacementFixture.build(ready, who, played)
    // The play picks no modifier, so only the automatic powers apply: the
    // whole pool would apply every selected modifier as if picked.
    val unpicked = WalkerPowers.selected(walkerPowers, Vector.empty)
    val shown = PlacementFixture.placementOptions(ready, tree,
      PlacementFixture.park(ready, tree, unpicked), unpicked)
    // Two advisers held under Silver Tongue: a faceup play replaces one, and
    // the faceup Silver Tongue is a locked card, so only the other may go.
    assertEquals(shown.filter(option => option.available &&
      CardPlayProcedure.playedAs(option.ref).contains(PlayedAs.Faceup))
      .map(PlacementPicks.replaces), Vector(Some(adviser)))
    // The site still offers the Mob's Replace slot beside the empty one.
    assertEquals(PlacementPicks.siteSlotOptions(shown)
      .map(PlacementPicks.replaces), Vector(None, Some(kept)))
    assert(walkerPowers.powers.exists(_.id == SilverTongue.id))

  test("it leaves a selected card's power alone: Welcoming Party still pays " +
      "once, and the selected card itself cannot be replaced"):
    val party = WelcomingPartyCard.id
    val kept = homesteaders
    val ready = atActorsSite(searchAtAncientCityHolding(Vector(kept)), party)
    val selected = Vector(distantlands.gameplay.powers.cardplay.WelcomingParty.id)
    val asked = toPlacement(ready, selected)
    assertEquals(
      placeReplacing(asked, played, PlayedAs.AtSite, party).left.toOption,
      Some(Refusals.notOffered(
        CardPlayProcedure.placementDecisionId(played))))
    val done = accepted(placeReplacing(asked, played, PlayedAs.AtSite, kept))
    val end = SearchFixture.after(done)
    val hearth = (s: ReadyGame) => s.banks.favor.getOrElse(Suit.Hearth, 0)
    // The play gains 1 favor from the played card's own bank, Welcoming Party
    // gains 1 from the Hearth bank.
    assertEquals(Look(end).player(actor).board.favor, Look(ready).player(actor)
      .board.favor + 2)
    assertEquals(hearth(end), hearth(ready) - 1)
    assertEquals(siteCards(end), Vector[CardId](party, played))

  // ---- Lines ----

  test("the discard the Mob permits writes its line when it is made"):
    val asked = toPlacement(
      searchAtAncientCityHolding(Vector(homesteaders, landWarden)))
    assertEquals(mobSaid(asked.events), Vector.empty)
    val done = accepted(placeReplacing(asked, played, PlayedAs.AtSite,
      homesteaders))
    assertEquals(mobSaid(done.events), Vector(NoteText.Said("discard-first",
      s"May discard a card at ${ancientCity.value} first.", covers = false)))

  test("a play to the empty slot writes no Mob line"):
    val asked = toPlacement(searchAtAncientCityHolding(Vector(homesteaders)))
    assertEquals(mobSaid(accepted(place(asked, played, PlayedAs.AtSite))
      .events), Vector.empty)
