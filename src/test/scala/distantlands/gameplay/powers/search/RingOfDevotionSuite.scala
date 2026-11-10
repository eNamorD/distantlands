package distantlands.gameplay.powers.search

import distantlands.catalog.ModifierMark
import distantlands.gameplay.FacedownAdviserPlay
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{PowerFixture, SearchFixture}
import distantlands.gameplay.powers.whenplayed.DazzleCard
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Unwrap.accepted

/** Ring of Devotion (relic R12), a free selected Search modifier: "You may
  * discard cards to your region's discard pile instead of the next region
  * out." (catalog batch 10, slice 9). */
class RingOfDevotionSuite extends munit.FunSuite:
  import PowerFixture.actor
  import SearchDiscardFixture._

  private val picked = Vector(RingOfDevotion.id)

  private def holdingTheRing(table: Table): Table =
    table.relic(actor, RingOfDevotionCard)

  private def searched(ready: ReadyGame,
      modifiers: Vector[PowerId] = picked): OathTransition =
    accepted(SearchFixture.start(ready, modifiers))

  private def cradle(transition: OathTransition): Vector[WorldCardId] =
    Look(SearchFixture.after(transition)).discardPile(Region.Cradle)
  private def provinces(transition: OathTransition): Vector[WorldCardId] =
    Look(SearchFixture.after(transition)).discardPile(Region.Provinces)

  test("its id is spelled right and it carries the Search icon"):
    assertEquals(RingOfDevotionCard.power.id,
      PowerId("relic.ring-of-devotion"))
    assertEquals(RingOfDevotionCard.power.modifier,
      Some(ModifierMark.Action(MajorActionType.Search)))

  test("unpicked, the cards not kept go to the next region out"):
    val done = keptFacedown(searched(
      searchAtAncientCity(holdingTheRing), Vector.empty))
    assertEquals(provinces(done).takeRight(2), unkept)

  test("picked, the cards not kept go to the Cradle pile, the pawn's own " +
      "region"):
    val done = keptFacedown(searched(searchAtAncientCity(holdingTheRing)))
    assertEquals(cradle(done).takeRight(2), unkept)
    assert(!provinces(done).exists(unkept.contains), provinces(done))

  test("picked, an adviser bumped over the limit goes to the Cradle pile"):
    val done = keptFaceupBumpingAnAdviser(searched(searchAtAncientCity(
      table => withThreeFacedownAdvisers(holdingTheRing(table)))))
    assertEquals(cradle(done).last, bumped)
    assert(!provinces(done).contains(bumped), provinces(done))

  test("picked, the People's Favor's site discard goes to the Cradle pile"):
    val done = keptAtSiteReplacingTheSiteCard(searched(searchAtAncientCity(
      table => withPeoplesFavorAndSiteCard(holdingTheRing(table)))))
    assertEquals(cradle(done).last, siteCard)
    assert(!provinces(done).contains(siteCard), provinces(done))

  test("picked, Dazzle's own discard still goes to the next region out"):
    // Dazzle is kept and played to Ancient City. Its WHEN PLAYED discards
    // the Hearth card there, which is not a discard of the Search.
    val dazzle = DazzleCard.id
    val ready = searchAtAncientCity(
      table => holdingTheRing(table).denizen(siteCard, at = AncientCitySite),
      top = dazzle +: unkept)
    val done = keptAtSite(searched(ready), dazzle)
    assertEquals(provinces(done).last, siteCard)
    assertEquals(cradle(done).takeRight(2), unkept)

  test("picked, a Vision not kept goes to the Cradle pile too"):
    // A Vision ends the draw, so it is drawn third.
    val vision = VisionRules.Faith
    val done = keptFacedown(searched(searchAtAncientCity(holdingTheRing,
      top = Vector[WorldCardId](kept, unkept.head, vision))))
    assertEquals(cradle(done).takeRight(2),
      Vector[WorldCardId](unkept.head, vision))
    assert(!provinces(done).contains(vision), provinces(done))

  test("picked for a facedown adviser's play, the People's Favor's site " +
      "discard goes to the Cradle pile"):
    // Playing a facedown adviser offers the Search modifiers too.
    val adviser = heldAdvisers.head
    val ready = searchAtAncientCity(table => withPeoplesFavorAndSiteCard(
      holdingTheRing(table)).adviser(actor, adviser, facedown = true))
    val done = accepted(for
      parked <- FacedownAdviserPlay.parkedOnPlacement(SearchFixture.rules,
        Ready(ready), actor, adviser, picked)
      placed <- SearchFixture.placeReplacing(parked, adviser,
        PlayedAs.AtSite, siteCard)
    yield placed)
    assertEquals(cradle(done).last, siteCard)
    assert(!provinces(done).contains(siteCard), provinces(done))
