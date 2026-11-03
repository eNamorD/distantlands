package oathdigital.application

import oathdigital.gameplay.setup.{FirstGameRulesData, SetupDeal}
import oathdigital.model._

/**
 * Derives the concrete per-game deal (2026-09-21 Chronicle design, slice 2):
 * the five fixed Vision identities spliced into `denizenOrder`'s 10+2/15+3
 * packets, dealt after 6 cards for the seeded regional discards and 3 for
 * each player with no starting advisers are set aside (Foundation IV). A
 * deck of fewer than 25 denizens after the deal is instead shuffled with
 * every Vision by `mix` (CR p. 12). The caller shuffles `denizenOrder` and
 * `relicOrder` from the Chronicle's decks; a Chronicle's own order is shown
 * to players and must not be the deal. Total: Setup itself validates card
 * counts (`GameStartRules`); this is pure arithmetic over whatever it is
 * given.
 */
object ChronicleFirstGamePlan:
  /** A first game: every player draws. A generated first-game deck is long
    * enough that nothing is mixed; a shorter one is mixed with `identity`,
    * which keeps it in order with the Visions below it. */
  def dealOrder(config: FirstGameBootstrapConfig,
      denizenOrder: Vector[DenizenId], relicOrder: Vector[RelicId])
      : SetupOrders =
    dealOrder(config, config.participants.size, denizenOrder, relicOrder,
      identity)

  def dealOrder(config: FirstGameBootstrapConfig, drawers: Int,
      denizenOrder: Vector[DenizenId], relicOrder: Vector[RelicId],
      mix: Vector[WorldCardId] => Vector[WorldCardId]): SetupOrders =
    val remaining = denizenOrder.drop(SetupDeal.dealt(drawers))
    val visions = FirstGameRulesData.visions
    val worldDeckOrder: Vector[WorldCardId] =
      if remaining.size < 25 then mix(remaining ++ visions)
      else remaining.take(10) ++ visions.take(2) ++
        remaining.slice(10, 25) ++ visions.drop(2) ++ remaining.drop(25)
    SetupOrders(config.participants, config.firstPlayer, denizenOrder,
      worldDeckOrder, relicOrder)
