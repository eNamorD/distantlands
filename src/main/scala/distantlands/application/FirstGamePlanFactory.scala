package oathdigital.application

import oathdigital.model._

final case class FirstGameBootstrapConfig(
    participants: Vector[FirstGameParticipant],
    firstPlayer: PlayerId
)

final case class BootstrapPlanFailure(message: String)

/** A Chronicle, the config it was actually dealt against and the shuffled
  * deal (`orders`), which `GameCommand.Begin` records. The config:
  * `GeneratedFirstGamePlanFactory` shuffles seating and picks a new first
  * player before generating, so the config a caller passed in is stale the
  * moment `build` returns; `resolvedConfig` is the one to deal `SetupOrders`
  * from (2026-09-21 Chronicle design, slice 2).
  */
final case class FirstGamePlan(
    chronicle: Chronicle,
    resolvedConfig: FirstGameBootstrapConfig,
    orders: SetupOrders
)

/** What a new game starts from (Chronicle browsing design, "Create
  * request"): a fresh first-game Chronicle under a name, or a saved one. */
enum ChronicleChoice:
  case New(name: String)
  case Saved(chronicle: Chronicle)

trait FirstGamePlanFactory:
  def build(config: FirstGameBootstrapConfig, choice: ChronicleChoice)
      : Either[BootstrapPlanFailure, FirstGamePlan]
