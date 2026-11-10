package distantlands.server

import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

object ProductionFrontendRoutes:
  private val IndexCacheControl = RawHeader("Cache-Control", "no-cache")
  // Assets are served at fixed, unfingerprinted paths, so a long-lived
  // `immutable` policy would strand upgraded clients on a previous bundle.
  // Revalidate every request and let `getFromResource`'s ETag/Last-Modified
  // keep the response cheap until fingerprinted names land.
  private val AssetCacheControl = RawHeader(
    "Cache-Control",
    "public, max-age=0, must-revalidate"
  )

  /** The one font the stylesheet loads, served by name so the assets prefix
    * stays a fixed list rather than a directory. */
  private val Atkinson = "atkinson-hyperlegible-next-latin-wght-normal.woff2"

  val gamePage: Route =
    respondWithHeader(IndexCacheControl):
      getFromResource("distantlands/frontend/index.html")

  val route: Route =
    pathEndOrSingleSlash {
      gamePage
    } ~
      // The player selection page is drawn by the app; a reload there loads
      // the same shell, which returns to the Chronicle page.
      path("new-game") {
        gamePage
      } ~
      pathPrefix("assets"):
        respondWithHeader(AssetCacheControl):
          path("main.js") {
            getFromResource("distantlands/frontend/main.js")
          } ~
            path("styles.css") {
              getFromResource("distantlands/frontend/styles.css")
            } ~
            path("fonts" / Atkinson):
              getFromResource(s"distantlands/frontend/fonts/$Atkinson")
