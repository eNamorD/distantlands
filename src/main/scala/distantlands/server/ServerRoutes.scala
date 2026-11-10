package oathdigital.server

import scala.concurrent.ExecutionContext

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object ServerRoutes:
  def route(
      runtime: ServerRuntime,
      blockingExecutionContext: ExecutionContext,
      config: ServerConfig,
      readiness: ServerReadiness
  ): Route =
    val publicOrigin = config.publicBaseUrl.getOrElse(
      new java.net.URI("http", null, config.host, config.port, null, null, null))
    // A browser on the host itself may reach the server through any loopback
    // alias on the listen port, for example when a home router cannot loop back
    // to its own public address. Accept those origins in every mode. A remote
    // page claiming a loopback origin gains nothing: the seat cookie is
    // SameSite=Lax, so its cross-site POSTs arrive without a seat, and anyone
    // may already create games from the public page.
    val loopbackAliases =
      Seq("localhost", "127.0.0.1", "[::1]").map(host =>
        new java.net.URI(s"http://$host:${config.port}"))
    val trustedSeats = new TrustedSeatRoutes(runtime.identities,
      runtime.trustedGameProvisioning, runtime.gateway, runtime.chronicles,
      publicOrigin,
      blockingExecutionContext, loopbackAliases).route
    val application = config.mode match
      case ServerMode.Development =>
        // The development start page creates games through the same trusted
        // provisioning as trusted-alpha mode, so both get the generated setup.
        DevelopmentRoutes.route(runtime.service, runtime.gateway,
          blockingExecutionContext) ~ trustedSeats
      case ServerMode.TrustedAlpha =>
        trustedSeats ~ ProductionFrontendRoutes.route
    HealthRoutes.route(readiness) ~ application
