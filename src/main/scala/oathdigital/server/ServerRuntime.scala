package oathdigital.server

import java.nio.file.Path

import oathdigital.application.{ChronicleRandomPort, IdentityRepository}
import oathdigital.gameplay.cards.NewFoundations
import oathdigital.persistence.HsqldbDatabaseOwner
final class ServerRuntime private (
    val service: oathdigital.application.GameApplicationService,
    val gateway: GameGateway,
    val identities: IdentityRepository,
    val trustedGameProvisioning: TrustedGameProvisioning,
    private val database: HsqldbDatabaseOwner
) extends AutoCloseable:
  override def close(): Unit = database.close()

object ServerRuntime:
  def open(
      databasePath: Path,
      random: ChronicleRandomPort = ChronicleRandomPort.random,
      gameIds: () => String = () => TrustedGameProvisioning.generateGameId()
  ): Either[String, ServerRuntime] =
    HsqldbDatabaseOwner.open(databasePath).left.map {
      case oathdigital.application.RepositoryFailure.StorageFailure(message) =>
        message
      case oathdigital.application.RepositoryFailure.InvalidConfiguration(
            message
          ) =>
        message
    }.map { database =>
      val repository = database.eventStreams
      val catalog = NewFoundations.catalog
      val firstGameService =
        new oathdigital.application.GameApplicationService(
          catalog,
          repository
        )
      val projector =
        new oathdigital.application.GameProjector(catalog)
      val generatedPlanFactory =
        new oathdigital.application.GeneratedFirstGamePlanFactory(
          catalog,
          random
        )
      new ServerRuntime(
        firstGameService,
        new GameGateway(firstGameService, projector),
        database.identities,
        new TrustedGameProvisioning(firstGameService, generatedPlanFactory,
          database.trustedGames, generateGameId = gameIds),
        database
      )
    }
