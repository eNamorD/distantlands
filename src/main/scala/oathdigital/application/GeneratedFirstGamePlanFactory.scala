package oathdigital.application

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powers.PowerImplementationStatus

/**
 * Production first-game Chronicle derivation: draws a random Chronicle
 * through `FirstGameChronicleGenerator` (2026-09-21 Chronicle design,
 * slice 2). Trusted-game provisioning, development game creation, and
 * authenticated bootstrap all use this now that the dev-only loopback route
 * and its deterministic `DevelopmentFirstGamePlanFactory` are retired.
 *
 * A saved Chronicle is used as it is. Either way the seating order is
 * shuffled, and the first seat becomes the first player, so the requested
 * `firstPlayer` is ignored. The shuffled order is
 * recorded in the plan, so replay stays RNG-free.
 */
final class GeneratedFirstGamePlanFactory(
    catalog: ExecutableCatalog,
    random: ChronicleRandomPort = ChronicleRandomPort.random,
    policy: ShufflePolicy = ShufflePolicy.implementedFirst
) extends FirstGamePlanFactory:
  override def build(config: FirstGameBootstrapConfig, choice: ChronicleChoice)
      : Either[BootstrapPlanFailure, FirstGamePlan] =
    val resolvedConfig = shuffledSeating(config)
    val chronicle = choice match
      case ChronicleChoice.New(name) =>
        FirstGameChronicleGenerator.generate(catalog, name,
            PowerImplementationStatus.implemented(catalog), random, policy)
          .left.map {
            case ChronicleGeneratorFailure.InvalidName(reason) =>
              BootstrapPlanFailure(reason)
            case failure => BootstrapPlanFailure(failure.toString)
          }
      case ChronicleChoice.Saved(saved) => Right(saved)
    chronicle.map(FirstGamePlan(_, resolvedConfig))

  private def shuffledSeating(config: FirstGameBootstrapConfig)
      : FirstGameBootstrapConfig =
    val seats = random.shuffle(config.participants)
    seats.headOption.fold(config)(first =>
      FirstGameBootstrapConfig(seats, first.playerId))
