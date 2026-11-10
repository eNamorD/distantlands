package oathdigital.application

import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.action.{DowsingSticksCard, FaeMerchant,
  FaeMerchantCard, GamblingHall, GamblingHallCard, WhistleCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, Table}
import oathdigital.testkit.Outcomes.ready
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** The parked decisions of the slice 1b powers reach their owner with every
  * option presented, and reach nobody else. A projection is dropped whole when
  * one option cannot be presented, so this is what proves a facedown relic in
  * the owner's own play area is nameable to the owner.
  */
class DicePowerDecisionProjectionSuite extends munit.FunSuite:
  import ActPowerHarness._

  private val projector = new WalkerDecisionProjector(catalog,
    new GamePresentationProjector(catalog))

  private val fae = FaeMerchantCard.id
  private val whistle = WhistleCard.id
  private val dowsingSticks = DowsingSticksCard.id

  private def owner(state: ReadyGame) =
    projector.project(ScopedProjectionContext(state, Some(p1)))
  private def other(state: ReadyGame) =
    projector.project(ScopedProjectionContext(state, Some(p2)))

  test("Gambling Hall offers the owner all six favor banks"):
    val hall = GamblingHallCard.id
    // p1 stands at Ancient City, where Gambling Hall is, with 3 favor.
    val ready0 = Table.start.pawn(p1, AncientCitySite)
      .denizen(hall, at = AncientCitySite).favor(p1, 3).ready
    val rules0 = rules(defenseDice(DefenseDieFace.OneShield,
      DefenseDieFace.OneShield, DefenseDieFace.TwoShields,
      DefenseDieFace.Blank))
    val parked = ready(accepted(use(rules0, ready0, GamblingHall.id,
      DecisionOptionRef.Denizen(hall))).state)
    val projection = owner(parked).get
    assertEquals(projection.decisionId, GamblingHall.decisionId)
    assertEquals(projection.query.get.offeredOptions.map(_.kind).distinct,
      Vector("favor-bank"))
    assertEquals(projection.query.get.offeredOptions.size, Suit.all.size)
    assertEquals(other(parked), None)

  test("Fae Merchant names both eligible relics to its owner, including the " +
      "facedown one just taken"):
    // p1 stands at Ancient City with Fae Merchant, holds the Whistle faceup
    // and 2 faceup secrets; Dowsing Sticks tops the relic deck.
    val ready0 = Table.start.pawn(p1, AncientCitySite)
      .denizen(fae, at = AncientCitySite)
      .relic(p1, whistle).secrets(p1, faceUp = 2)
      .relicDeckTop(dowsingSticks).ready
    val parked = ready(accepted(use(rulesWithPrintedRestrictions(), ready0,
      FaeMerchant.id, DecisionOptionRef.Denizen(fae))).state)
    val projection = owner(parked).get
    assertEquals(projection.decisionId, FaeMerchant.decisionId)
    assertEquals(projection.query.get.offeredOptions.map(_.id),
      Vector(whistle.value, dowsingSticks.value))
    assert(projection.query.get.offeredOptions
      .forall(_.card.exists(!_.hidden)))
    assertEquals(other(parked), None)

  test("Fae Merchant does not offer the Grand Scepter"):
    val scepter = TheGrandScepterCard.id
    val ready0 = Table.start.pawn(p1, AncientCitySite)
      .denizen(fae, at = AncientCitySite)
      .relic(p1, scepter).relic(p1, whistle).secrets(p1, faceUp = 2)
      .relicDeckTop(dowsingSticks).ready
    val parked = ready(accepted(use(rulesWithPrintedRestrictions(), ready0,
      FaeMerchant.id, DecisionOptionRef.Denizen(fae))).state)
    assertEquals(owner(parked).get.query.get.offeredOptions.map(_.id),
      Vector(whistle.value, dowsingSticks.value))
