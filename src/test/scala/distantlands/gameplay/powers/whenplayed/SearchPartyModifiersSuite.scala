package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.{ModifierPlay, SearchPlay}
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.actions.search.SearchProcedure
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{PhasePowerCatalog, PowerFixture,
  SearchFixture, WalkerPowerCatalog}
import distantlands.gameplay.powers.action.{TempleOfAncestors,
  TempleOfAncestorsCard}
import distantlands.gameplay.powers.cardplay.{WelcomingParty,
  WelcomingPartyCard}
import distantlands.gameplay.powers.search.{Bracken, BrackenCard, NewGrowth,
  NewGrowthCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, PlacementPicks, PowerRules, Table}
import distantlands.testkit.Unwrap.accepted

/** Search Party's play is not the Search's (catalog batch 10, slice 12,
  * ruling 5): no modifier picked for the Search reaches it, and Temple of
  * Ancestors limits only its own play. p1 searches the Cradle discard
  * pile, which holds one card, so nothing else is drawn. */
class SearchPartyModifiersSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchFixture.{after, rules}
  import SearchPartyFixture._

  private val hearth = plainHearth
  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  private val phasePowers = PhasePowerCatalog.default

  /** p1 at Ancient City with `kept` alone in the Cradle pile and `arrange`
    * applied. */
  private def cradlePileHolding(kept: WorldCardId)(arrange: Table => Table)
      : ReadyGame =
    arrange(Table.start.pawn(actor, AncientCitySite)
      .discarded(Region.Cradle, kept)).ready

  /** p1 searches the Cradle pile with `modifiers` picked: parked on the
    * kept card's placement. */
  private def cradleSearched(ready: ReadyGame, modifiers: Vector[PowerId])
      : OathTransition =
    accepted(SearchPlay.searched(rules, Ready(ready), actor,
      SearchSource.RegionalDiscard(Region.Cradle), modifiers))

  /** The sites the parked placement offers a slot at, available or not. */
  private def sitesOffered(state: OathState): Set[SiteId] =
    PlacementPicks.siteSlotOptions(PlacementPicks.parkedOptions(state,
      walkerPowers)).flatMap(option =>
      CardPlayProcedure.siteOf(option.ref)).toSet

  test("Welcoming Party, picked for the Search, rewards Search Party's " +
      "play but not the card Search Party plays"):
    val ready = cradlePileHolding(SearchPartyCard.id)(_
      .denizen(WelcomingPartyCard, at = AncientCitySite)
      .discarded(Region.Hinterland, hearth(0)))
    val before = Look(ready).favor(actor)
    val kept = placedAs(cradleSearched(ready, Vector(WelcomingParty.id)),
      SearchPartyCard.id, PlayedAs.Faceup)
    val done = placedAs(peekedAndPicked(kept, Region.Hinterland, hearth(0)),
      hearth(0), PlayedAs.Faceup)
    assertEquals(Look(after(done)).favor(actor),
      before + WelcomingParty.Favor)

  test("New Growth, picked for the Search, opens other sites to a Hearth " +
      "card the Search keeps, but not to one Search Party plays"):
    val direct = cradleSearched(cradlePileHolding(hearth(0))(_
      .adviser(actor, NewGrowthCard)), Vector(NewGrowth.id))
    assert(sitesOffered(direct.state).exists(_ != AncientCitySite.id),
      "New Growth should open another site to the Search's own card")
    val ready = cradlePileHolding(SearchPartyCard.id)(_
      .adviser(actor, NewGrowthCard)
      .discarded(Region.Hinterland, hearth(0)))
    val kept = placedAs(cradleSearched(ready, Vector(NewGrowth.id)),
      SearchPartyCard.id, PlayedAs.Faceup)
    val asked = peekedAndPicked(kept, Region.Hinterland, hearth(0))
    assertEquals(sitesOffered(asked.state), Set(AncientCitySite.id))

  test("Bracken, picked for the Search, leaves the discard of the card " +
      "Search Party plays to the next region's pile"):
    // Bracken sends the Search's discards to the Hinterland top. p1 is in
    // the Cradle, so the nested discard belongs on the Provinces pile.
    val ready = cradlePileHolding(SearchPartyCard.id)(_
      .denizen(BrackenCard, at = AncientCitySite).secrets(actor, faceUp = 1)
      .discarded(Region.Provinces, hearth.take(2)*))
    val searched = (for
      picked <- ModifierPlay.started(rules, Ready(ready), ActionRef.Search,
        actor, PowerWindow.SearchModifierSelection, Vector(Bracken.id))
      piled <- rules.resolveWalker(picked.state, actor, Bracken.pileDecision,
        ChooseOneAnswer(Bracken.pileRef(Region.Hinterland)))
      ended <- rules.resolveWalker(piled.state, actor,
        Bracken.positionDecision, ChooseOneAnswer(Bracken.top))
      started <- ModifierPlay.picking(rules, ended, actor,
        PowerWindow.SearchModifierSelection, Vector.empty)
      sourced <- rules.resolveWalker(started.state, actor,
        SearchProcedure.sourceDecisionId,
        SearchPlay.source(SearchSource.RegionalDiscard(Region.Cradle)))
    yield sourced).fold(error => fail(error.toString), identity)
    val kept = placedAs(searched, SearchPartyCard.id, PlayedAs.Faceup)
    val done = placedAs(peekedAndPicked(kept, Region.Provinces, hearth(0)),
      hearth(0), PlayedAs.Discarded)
    assertEquals(Look(after(done)).discardPile(Region.Provinces),
      Vector[WorldCardId](hearth(1), hearth(0)))
    assertEquals(Look(after(done)).discardPile(Region.Hinterland),
      Vector.empty[WorldCardId])

  test("Search Party drawn by Temple of Ancestors may play its card to a " +
      "site"):
    val temple = PowerRules.withWalkerAndPhasePowers
    val ready = Table.start.pawn(actor, AncientCitySite)
      .edifice(TempleOfAncestorsCard.id, EdificeSide.Intact,
        at = AncientCitySite)
      .favor(actor, 2).worldDeckBottom(SearchPartyCard)
      .discarded(Region.Hinterland, hearth(0)).ready
    val drew = accepted(temple.startWalker(Ready(ready),
      ActionRef.UsePower(TempleOfAncestors.id), actor,
      Vector(DecisionOptionRef.Edifice(TempleOfAncestorsCard.id))))
    val played = accepted(temple.resolveWalker(drew.state, actor,
      CardPlayProcedure.placementDecisionId(SearchPartyCard.id),
      PlacementPicks.parkedAnswer(drew.state, PlayedAs.Faceup, walkerPowers,
        phasePowers)))
    val piled = accepted(temple.resolveWalker(played.state, actor,
      SearchParty.pileDecisionId,
      ChooseOneAnswer(SearchParty.pileRef(Region.Hinterland))))
    val asked = accepted(temple.resolveWalker(piled.state, actor,
      SearchParty.cardDecisionId,
      ChooseOneAnswer(PlacementPicks.optionNaming(hearth(0)))))
    val slots = PlacementPicks.siteSlotOptions(PlacementPicks.parkedOptions(
      asked.state, walkerPowers, phasePowers))
    assert(slots.nonEmpty)
    assert(slots.forall(_.available), clue(slots))
