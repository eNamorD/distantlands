package distantlands.gameplay.powers.search

import distantlands.catalog.ModifierMark
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.powers.{PowerFixture, SearchFixture}
import distantlands.gameplay.powers.campaign.SealingWardCard
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Unwrap.accepted

/** Cracked Horn (relic R10), a free selected Search modifier: "You may put
  * all of the cards you discard on the bottom of the world deck." (catalog
  * batch 10, slice 9). */
class CrackedHornSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchDiscardFixture._

  private val picked = Vector(CrackedHorn.id)

  private def holdingTheHorn(table: Table): Table =
    table.relic(actor, CrackedHornCard)

  private def searched(ready: ReadyGame): OathTransition =
    accepted(SearchFixture.start(ready, picked))

  /** The world deck, top first, after `transition`. */
  private def worldDeck(transition: OathTransition): Vector[WorldCardId] =
    SearchFixture.after(transition).game.current.commonCards.worldDeck
  private def provinces(transition: OathTransition): Vector[WorldCardId] =
    Look(SearchFixture.after(transition)).discardPile(Region.Provinces)

  test("it carries the Search icon"):
    assertEquals(CrackedHornCard.power.modifier,
      Some(ModifierMark.Action(MajorActionType.Search)))

  test("picked, the cards not kept go to the bottom of the world deck, in " +
      "the order discarded"):
    val done = keptFacedown(searched(searchAtAncientCity(holdingTheHorn)))
    assertEquals(worldDeck(done).takeRight(2), unkept)
    assert(!provinces(done).exists(unkept.contains), provinces(done))

  test("picked, an adviser bumped over the limit goes to the bottom of the " +
      "world deck"):
    val done = keptFaceupBumpingAnAdviser(searched(searchAtAncientCity(
      table => withThreeFacedownAdvisers(holdingTheHorn(table)))))
    assertEquals(worldDeck(done).last, bumped)

  test("picked, the People's Favor's site discard goes to the bottom of " +
      "the world deck"):
    val done = keptAtSiteReplacingTheSiteCard(searched(searchAtAncientCity(
      table => withPeoplesFavorAndSiteCard(holdingTheHorn(table)))))
    assertEquals(worldDeck(done).last, siteCard)

  test("a Vision it buries leaves the Visions Drawn track as the draw left " +
      "it"):
    val vision = VisionRules.Faith
    // A Vision ends the draw, so it is drawn third, and buried last.
    val ready = searchAtAncientCity(holdingTheHorn,
      top = Vector[WorldCardId](kept, unkept.head, vision))
    val before = ready.game.current.tracks.visionsDrawn
    val done = keptFacedown(searched(ready))
    assertEquals(worldDeck(done).takeRight(2),
      Vector[WorldCardId](unkept.head, vision))
    // The draw advanced the track once; the bury does nothing to it.
    assertEquals(SearchFixture.after(done).game.current.tracks.visionsDrawn,
      before + 1)

  test("a bumped adviser and a site card it buries replay and round-trip " +
      "the wire"):
    Vector(
      searchAtAncientCity(table =>
        withThreeFacedownAdvisers(holdingTheHorn(table))) ->
        (keptFaceupBumpingAnAdviser(_: OathTransition)),
      searchAtAncientCity(table =>
        withPeoplesFavorAndSiteCard(holdingTheHorn(table))) ->
        (keptAtSiteReplacingTheSiteCard(_: OathTransition))
    ).foreach { (ready, finish) =>
      val done = finish(searched(ready))
      assertEquals(ActPowerHarness.replayed(SearchFixture.rules, ready,
        done.events), SearchFixture.after(done))
      assertWireRoundTrips(done.events)
    }

  test("a locked faceup adviser still cannot be bumped and buried"):
    // Sealing Ward is locked. The placement judges the plain discard before
    // the Horn rewrites it, and the restrictions judge the rewritten
    // `Discard.Buried` again when it runs (OperationRestrictionsSuite).
    val ready = searchAtAncientCity(table => holdingTheHorn(table)
      .adviser(actor, SealingWardCard.id)
      .adviser(actor, heldAdvisers(1), facedown = true)
      .adviser(actor, heldAdvisers(2), facedown = true))
    val chosen = accepted(SearchFixture.keep(searched(ready), kept))
    assert(SearchFixture.placeReplacing(chosen, kept, PlayedAs.Faceup,
      SealingWardCard.id).isLeft)
