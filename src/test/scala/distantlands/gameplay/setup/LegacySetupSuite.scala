package distantlands.gameplay.setup

import distantlands.catalog.holding.SeerCard
import distantlands.model._
import distantlands.testkit.Unwrap.accepted

/** Foundation III at game start and in Setup (Legacies design, slice 2). */
class LegacySetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val chronicle = FirstGameSetupFixture.chronicle
  private val orders = FirstGameSetupFixture.orders
  private val seats = orders.participants

  private def publicAmbitions(base: Chronicle): Chronicle = base.copy(
    foundations = base.foundations.updated(FoundationNumber.III,
      FoundationState(FoundationFace.Altered, Set.empty)))

  test("Quiet Ambitions deals each seated lineage 2 dormant legacies, " +
      "after those it holds"):
    val seated = seats.head.lineageId
    val kept = LegacyState(SeerCard.id, active = true)
    val keeping = chronicle.copy(lineages = Vector(LineageState(seated,
      None, Role.Exile, Vector(kept), Vector.empty)))
    val order = LegacyDeal.deck(catalog, keeping)
    val ready = accepted(GameStartRules.evolve(catalog, keeping,
      orders.copy(legacyOrder = order)))
    val deal = LegacyDeal.of(keeping, seats)
    seats.foreach(seat => assertEquals(
      ready.game.campaign.lineages(seat.lineageId).legacies,
      (if seat.lineageId == seated then Vector(kept) else Vector.empty) ++
        deal.drawn(order, seat.playerId).map(LegacyState(_, active = false)),
      seat.playerId.value))
    assertEquals(ready.game.current.commonCards.publicLegacies, Vector.empty)

  test("Public Ambitions deals 4 legacies faceup to the row and nobody " +
      "draws"):
    val ready = accepted(GameStartRules.evolve(catalog,
      publicAmbitions(chronicle), orders))
    assertEquals(ready.game.current.commonCards.publicLegacies,
      orders.legacyOrder.take(4))
    assert(seats.forall(seat =>
      ready.game.campaign.lineages(seat.lineageId).legacies.isEmpty))
