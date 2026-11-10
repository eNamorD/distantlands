package distantlands.server

import distantlands.model.PlayerColor

import java.nio.file.Files

import distantlands.protocol._
import distantlands.testkit.Table.p2
import distantlands.testkit.Unwrap.accepted

class ServerRuntimeSuite extends munit.FunSuite:
  private val request = TrustedGameCreateRequest(Vector(
    BootstrapParticipantRequest("p1", PlayerColor.Red),
    BootstrapParticipantRequest("p2", PlayerColor.Blue),
    BootstrapParticipantRequest("p3", PlayerColor.Yellow)),
    ChronicleRequest.New("Runtime Chronicle"))

  test("trusted-game provisioning draws a randomized board"):
    val runtime = accepted(ServerRuntime.open(
      Files.createTempDirectory("server-runtime-wiring-").resolve("database")))
    try
      def siteOrder(): Vector[String] =
        val gameId = accepted(runtime.trustedGameProvisioning
          .create(request, "https://games.example.test")).gameId
        val projection = accepted(runtime.gateway.load(gameId,
          GameAccess.Seat(p2)))
        projection.world.flatMap(_.sites).map(_.siteId)
      val orders = Vector.fill(4)(siteOrder())
      assert(orders.distinct.size > 1,
        "four separately provisioned trusted games should not share one fixed board")
    finally runtime.close()
