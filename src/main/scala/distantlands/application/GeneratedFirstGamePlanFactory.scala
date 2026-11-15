package distantlands.application

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powers.PowerImplementationStatus
import distantlands.gameplay.setup.{EmpireSetupRules, LegacyDeal, SetupDeal}
import distantlands.model._

/**
 * Production first-game Chronicle derivation: draws a random Chronicle
 * through `FirstGameChronicleGenerator` (2026-09-21 Chronicle design,
 * slice 2). Trusted-game provisioning, development game creation, and
 * authenticated bootstrap all use this now that the dev-only loopback route
 * and its deterministic `DevelopmentFirstGamePlanFactory` are retired.
 *
 * A saved Chronicle is used as it is. Either way the seating order is
 * shuffled and the requested `firstPlayer` is ignored. The seated
 * Chancellor goes first, or a random seated Citizen when no Chancellor is
 * seated (Empire ruling R6); otherwise, in a first game and in a later game
 * with no Empire, the first seat does, which the shuffle makes a random
 * player. Both decks are shuffled into the deal by
 * `policy`, and the derived legacy deck by `random`, since a Chronicle's
 * own order is shown to players. The
 * shuffled orders are recorded in the plan, so replay stays RNG-free.
 * `implemented` is production's `PowerImplementationStatus.implemented`
 * unless a test stands one power in as unimplemented.
 */
final class GeneratedFirstGamePlanFactory(
    catalog: ExecutableCatalog,
    random: ChronicleRandomPort = ChronicleRandomPort.random,
    policy: ShufflePolicy = ShufflePolicy.implementedFirst,
    implemented: PowerId => Boolean = PowerImplementationStatus.implemented
) extends FirstGamePlanFactory:
  override def build(config: FirstGameBootstrapConfig, choice: ChronicleChoice)
      : Either[BootstrapPlanFailure, FirstGamePlan] =
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
      // The deal is the starting Chronicle's: a first game played
      // exile-only has its stored legacies back in the deck (R40).
      val starting = EmpireSetupRules.startingChronicle(catalog, chronicle)
      FirstGamePlan(chronicle, seated, ChronicleFirstGamePlan.dealOrder(
        seated, SetupDeal.of(starting, seated.participants),
        policy.order(chronicle.worldDeck,
          ImplementedCardCatalog.denizens(catalog, implemented), random),
        policy.order(chronicle.relicDeck,
          ImplementedCardCatalog.ordinaryRelics(catalog, implemented),
          random),
        random.shuffle(LegacyDeal.deck(catalog, starting)),
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
    val first = chancellor.orElse(
      if citizens.isEmpty then seats.headOption
      else random.shuffle(citizens).headOption)
    first.fold(config)(p => FirstGameBootstrapConfig(seats, p.playerId))
