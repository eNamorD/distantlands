package distantlands.application

import distantlands.gameplay.setup.{FirstGameRulesData, SetupDeal}
import distantlands.model._

/**
 * Derives the concrete per-game deal (2026-09-21 Chronicle design, slice 2):
 * the five fixed Vision identities spliced into `denizenOrder`'s 10+2/15+3
 * packets, dealt after Foundation IV has taken its cards from the stack
 * (`SetupDeal`). A deck of fewer than 25 denizens after the deal is instead
 * shuffled with every Vision by `mix` (CR p. 12); Teeming World leaves no
 * denizen, so its deck is the five Visions, mixed. The caller shuffles
 * `denizenOrder` and
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
    dealOrder(config, SetupDeal.firstGame(config.participants),
      denizenOrder, relicOrder, identity)

  def dealOrder(config: FirstGameBootstrapConfig, deal: SetupDeal,
      denizenOrder: Vector[DenizenId], relicOrder: Vector[RelicId],
      mix: Vector[WorldCardId] => Vector[WorldCardId]): SetupOrders =
    val remaining = deal.deck(denizenOrder)
    val visions = FirstGameRulesData.visions
    val worldDeckOrder: Vector[WorldCardId] =
      if remaining.size < 25 then mix(remaining ++ visions)
      else remaining.take(10) ++ visions.take(2) ++
        remaining.slice(10, 25) ++ visions.drop(2) ++ remaining.drop(25)
    SetupOrders(config.participants, config.firstPlayer, denizenOrder,
      worldDeckOrder, relicOrder)
