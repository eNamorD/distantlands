package oathdigital.gameplay.powers.search

import oathdigital.catalog.ModifierMark
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.{PowerFixture, SearchFixture}
import oathdigital.gameplay.powers.whenplayed.DazzleCard
import oathdigital.model._
import oathdigital.testkit.{Look, Table}
import oathdigital.testkit.Unwrap.accepted

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
