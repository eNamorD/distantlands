package oathdigital.application

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.powers.PowerImplementationStatus
import oathdigital.gameplay.setup.SetupDeal
import oathdigital.model._

/**
 * Production first-game Chronicle derivation: draws a random Chronicle
 * through `FirstGameChronicleGenerator` (2026-09-21 Chronicle design,
 * slice 2). Trusted-game provisioning, development game creation, and
 * authenticated bootstrap all use this now that the dev-only loopback route
 * and its deterministic `DevelopmentFirstGamePlanFactory` are retired.
 *
 * A saved Chronicle is used as it is. Either way the seating order is
 * shuffled and the requested `firstPlayer` is ignored. With an Empire, the
 * seated Chancellor goes first, or a random seated Citizen when the
 * Chancellor's lineage sits out (Empire ruling R6); otherwise the first
 * seat does. Both decks are shuffled into the deal by
 * `policy`, since a Chronicle's own order is shown to players. The
 * shuffled orders are recorded in the plan, so replay stays RNG-free.
 */
final class GeneratedFirstGamePlanFactory(
    catalog: ExecutableCatalog,
    random: ChronicleRandomPort = ChronicleRandomPort.random,
    policy: ShufflePolicy = ShufflePolicy.implementedFirst
) extends FirstGamePlanFactory:
  override def build(config: FirstGameBootstrapConfig, choice: ChronicleChoice)
      : Either[BootstrapPlanFailure, FirstGamePlan] =
    val implemented = PowerImplementationStatus.implemented
    val chronicle = choice match
      case ChronicleChoice.New(name) =>
        FirstGameChronicleGenerator.generate(catalog, name, implemented,
            random, policy)
          .left.map {
            case ChronicleGeneratorFailure.InvalidName(reason) =>
              BootstrapPlanFailure(reason)
            case failure => BootstrapPlanFailure(failure.toString)
          }
      case ChronicleChoice.Saved(saved) => Right(saved)
    chronicle.map { chronicle =>
      val seated = seating(config, chronicle)
      FirstGamePlan(chronicle, seated, ChronicleFirstGamePlan.dealOrder(
        seated, SetupDeal.drawers(chronicle, seated.participants),
        policy.order(chronicle.worldDeck,
          ImplementedCardCatalog.denizens(catalog, implemented), random),
        policy.order(chronicle.relicDeck,
          ImplementedCardCatalog.ordinaryRelics(catalog, implemented),
          random),
        random.shuffle))
    }

  private def seating(config: FirstGameBootstrapConfig, chronicle: Chronicle)
      : FirstGameBootstrapConfig =
    val seats = random.shuffle(config.participants)
    def lineages(role: Role) = chronicle.lineages.filter(_.role == role)
      .map(_.id).toSet
    val chancellor = seats.find(p => lineages(Role.Chancellor)(p.lineageId))
    val citizens = seats.filter(p => lineages(Role.Citizen)(p.lineageId))
    // A random seated Citizen stands in for an absent Chancellor (R6).
    val first = if lineages(Role.Chancellor).isEmpty then seats.headOption
      else chancellor.orElse(random.shuffle(citizens).headOption)
        .orElse(seats.headOption)
    first.fold(config)(p => FirstGameBootstrapConfig(seats, p.playerId))
