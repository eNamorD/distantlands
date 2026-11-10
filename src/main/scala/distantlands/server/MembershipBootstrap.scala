package distantlands.server

import distantlands.application._
import distantlands.protocol.FirstGameBootstrapRequest
import distantlands.protocol.projection.GameProjection

enum BootstrapFailure:
  case Authorization(error: AuthorizationFailure)
  case Identity(error: IdentityFailure)
  case Configuration(message: String)
  case Application(error: GameApplicationError)

/** Begins a provisioned game for its owner, seating exactly the player
  * memberships the game holds (server event journal design, "Authentication
  * and membership boundary"). Kept for X6, which mounts a route on it;
  * `TrustedGameProvisioning` is the seat transport's creation path. */
final class MembershipBootstrap(
    service: GameApplicationService,
    projector: GameProjector,
    authorization: MembershipAuthorizationService,
    identities: IdentityRepository,
    planFactory: FirstGamePlanFactory
):
  import BootstrapFailure._

  def begin(gameId: String, principal: AuthenticatedPrincipal,
      request: FirstGameBootstrapRequest)
      : Either[BootstrapFailure, GameProjection] = for
    _ <- authorization.authorizeBootstrap(gameId, principal)
      .left.map(Authorization.apply)
    memberships <- identities.listMemberships(gameId).left.map(Identity.apply)
    config = FirstGameBootstrapMapper.map(request)
    _ <- validateSeats(memberships, config)
    plan <- planFactory.build(config, ChronicleChoice.New(s"Chronicle $gameId"))
      .left.map(failure => Configuration(failure.message))
    accepted <- service.handle(gameId, request.expectedNextSequence,
      GameCommand.Begin(plan.chronicle, plan.orders))
      .left.map(Application.apply)
  yield projector.projectPublic(gameId, accepted.loaded)

  /** The participants must be the provisioned player seats, each once, and
    * the first player one of them. */
  private def validateSeats(memberships: Vector[GameMembership],
      config: FirstGameBootstrapConfig): Either[BootstrapFailure, Unit] =
    val playerMemberships = memberships.filter(_.role == MembershipRole.Player)
    val provisionedSeats = playerMemberships.flatMap(_.playerId)
    val requestedSeats = config.participants.map(_.playerId.value)
    if provisionedSeats.size != playerMemberships.size ||
        provisionedSeats.distinct.size != provisionedSeats.size then
      Left(Configuration("provisioned player memberships are invalid"))
    else if requestedSeats.distinct.size != requestedSeats.size then
      Left(Configuration("participant player IDs must be unique"))
    else if requestedSeats.toSet != provisionedSeats.toSet then
      Left(Configuration(
        "participants must exactly match provisioned player memberships"))
    else if !provisionedSeats.contains(config.firstPlayer.value) then
      Left(Configuration(
        "first player must be a provisioned player membership"))
    else Right(())
