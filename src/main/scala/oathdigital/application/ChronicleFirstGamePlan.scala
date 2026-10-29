package oathdigital.application

import oathdigital.gameplay.setup.FirstGameRulesData
import oathdigital.model._

/**
 * Derives the concrete per-game deal (2026-09-21 Chronicle design, slice 2):
 * the five fixed Vision identities spliced into `denizenOrder`'s 10+2/15+3
 * packets, dealt after `config.participants.size * 3 + 6` cards are set
 * aside for hands and the seeded regional discards. The caller shuffles
 * `denizenOrder` and `relicOrder` from the Chronicle's decks; a Chronicle's
 * own order is shown to players and must not be the deal. Total: Setup
 * itself validates card counts (`GameStartRules`); this is pure arithmetic
 * over whatever it is given.
 */
object ChronicleFirstGamePlan:
  def dealOrder(config: FirstGameBootstrapConfig,
      denizenOrder: Vector[DenizenId], relicOrder: Vector[RelicId])
      : SetupOrders =
    val dealt = 6 + config.participants.size * 3
    val remaining = denizenOrder.drop(dealt)
    val worldDeckOrder: Vector[WorldCardId] =
      remaining.take(10) ++ FirstGameRulesData.visions.take(2) ++
        remaining.slice(10, 25) ++ FirstGameRulesData.visions.drop(2) ++
        remaining.drop(25)
    SetupOrders(config.participants, config.firstPlayer, denizenOrder,
      worldDeckOrder, relicOrder)
