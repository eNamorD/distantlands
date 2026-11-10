package distantlands.server

import java.nio.file.Path

import distantlands.application.{ChronicleRandomPort, IdentityRepository}
import distantlands.application.chronicle.ChronicleConversion
import distantlands.gameplay.cards.NewFoundations
import distantlands.persistence.HsqldbDatabaseOwner
final class ServerRuntime private (
    val service: distantlands.application.GameApplicationService,
    val gateway: GameGateway,
    val identities: IdentityRepository,
    val trustedGameProvisioning: TrustedGameProvisioning,
    val chronicles: ChronicleConversion,
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
      case distantlands.application.RepositoryFailure.StorageFailure(message) =>
        message
      case distantlands.application.RepositoryFailure.InvalidConfiguration(
            message
          ) =>
        message
    }.map { database =>
      val repository = database.eventStreams
      val catalog = NewFoundations.catalog
      val firstGameService =
        new distantlands.application.GameApplicationService(
          catalog,
          repository
        )
      val projector =
        new distantlands.application.GameProjector(catalog)
      val generatedPlanFactory =
        new distantlands.application.GeneratedFirstGamePlanFactory(
          catalog,
          random
        )
      val chronicles = new ChronicleConversion(catalog)
      new ServerRuntime(
        firstGameService,
        new GameGateway(firstGameService, projector),
        database.identities,
        new TrustedGameProvisioning(firstGameService, generatedPlanFactory,
          database.trustedGames, chronicles, generateGameId = gameIds),
        chronicles,
        database
      )
    }
