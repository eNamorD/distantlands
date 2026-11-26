package distantlands.server

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

/** The host's art (game art design, "Serving the art"): the manifest,
  * which every client fetches once, and the files it lists. */
object ArtRoutes:
  // A table loads hundreds of images; revalidating each on every page
  // load costs more than a day's staleness after a host replaces a file.
  private val ArtCacheControl =
    RawHeader("Cache-Control", "public, max-age=86400")
  // The manifest is small and changes when the host restarts with another
  // folder, so each page load asks again.
  private val ManifestCacheControl = RawHeader("Cache-Control", "no-cache")

  def route(art: ArtLibrary): Route =
    val manifest = ujson.write(ujson.Obj("files" ->
      ujson.Arr.from(art.files.keys.toVector.sorted.map(ujson.Str(_)))))
    pathPrefix("art"):
      get:
        path("manifest.json") {
          respondWithHeader(ManifestCacheControl):
            complete(HttpEntity(ContentTypes.`application/json`, manifest))
        } ~
          // The request's segments, decoded and joined, are looked up in
          // the scan. Anything the scan did not keep is not found, without
          // a look at the disk: a `..` segment names no catalog path.
          path(Segments) { segments =>
            art.files.get(segments.mkString("/")) match
              case Some(file) =>
                respondWithHeader(ArtCacheControl):
                  getFromFile(file.toFile)
              case None => complete(StatusCodes.NotFound)
          } ~
          complete(StatusCodes.NotFound)
