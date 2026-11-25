package distantlands.server

import distantlands.testkit.HttpTestServer
import distantlands.testkit.HttpTestServer.cacheControl

class ProductionFrontendRoutesSuite extends munit.FunSuite:
  /** What the build packaged under distantlands/frontend, which the routes
    * serve as is. */
  private def packagedBytes(name: String): Array[Byte] =
    val stream = getClass.getClassLoader
      .getResourceAsStream(s"distantlands/frontend/$name")
    try stream.readAllBytes()
    finally stream.close()

  private def packaged(name: String): String =
    new String(packagedBytes(name), java.nio.charset.StandardCharsets.UTF_8)

  test("serves the packaged shell and assets with production cache policy"):
    val server = HttpTestServer.start("production-frontend-routes-test")(_ =>
      ProductionFrontendRoutes.route)

    try
      val index = server.get("/")
      assertEquals(index.statusCode(), 200)
      assert(index.body().contains("/assets/main.js"))
      assertEquals(cacheControl(index), Some("no-cache"))

      val newGame = server.get("/new-game")
      assertEquals(newGame.statusCode(), 200)
      assertEquals(newGame.body(), index.body())
      assertEquals(cacheControl(newGame), Some("no-cache"))

      val javascript = server.get("/assets/main.js")
      assertEquals(javascript.statusCode(), 200)
      assertEquals(javascript.body(), packaged("main.js"))
      assertEquals(
        cacheControl(javascript),
        Some("public, max-age=0, must-revalidate")
      )

      val stylesheet = server.get("/assets/styles.css")
      assertEquals(stylesheet.statusCode(), 200)
      assertEquals(stylesheet.body(), packaged("styles.css"))
      assertEquals(
        cacheControl(stylesheet),
        Some("public, max-age=0, must-revalidate")
      )

      val fontFile = "atkinson-hyperlegible-next-latin-wght-normal.woff2"
      val font = server.get(s"/assets/fonts/$fontFile")
      assertEquals(font.statusCode(), 200)
      assertEquals(server.getBytes(s"/assets/fonts/$fontFile").body().toVector,
        packagedBytes(s"fonts/$fontFile").toVector)
      assertEquals(
        cacheControl(font),
        Some("public, max-age=0, must-revalidate")
      )

      // A client that sends no Accept-Encoding gets the bytes as packaged.
      assertEquals(javascript.headers().firstValue("Content-Encoding").isPresent,
        false)

      assertEquals(server.get("/missing.js").statusCode(), 404)
      assertEquals(server.get("/assets/missing.js").statusCode(), 404)
      assertEquals(
        server.get("/assets/fonts/missing.woff2").statusCode(), 404)
    finally server.close()

  test("gzips text assets for a browser that accepts gzip"):
    val server = HttpTestServer.start("production-frontend-gzip-test")(_ =>
      ProductionFrontendRoutes.route)

    try
      for name <- Seq("main.js", "styles.css") do
        val response = server.getBytes(s"/assets/$name",
          Seq("Accept-Encoding" -> "gzip, deflate, br"))
        assertEquals(response.statusCode(), 200, name)
        assertEquals(
          response.headers().firstValue("Content-Encoding").orElse(""), "gzip",
          name)
        assertEquals(cacheControl(response),
          Some("public, max-age=0, must-revalidate"), name)
        assertEquals(response.headers().firstValue("Vary").orElse(""),
          "Accept-Encoding", name)
        val unzipped =
          val stream = new java.util.zip.GZIPInputStream(
            new java.io.ByteArrayInputStream(response.body()))
          try stream.readAllBytes()
          finally stream.close()
        assertEquals(unzipped.toVector, packagedBytes(name).toVector, name)
    finally server.close()
