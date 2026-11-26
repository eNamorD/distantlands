package distantlands.server

import distantlands.testkit.HttpTestServer
import distantlands.testkit.HttpTestServer.cacheControl

/** `/art/manifest.json` and `/art/<path>` (game art design, "Serving the
  * art"), over temporary folders of 1×1 PNGs. */
class ArtRoutesSuite extends munit.FunSuite:
  private val catalog = Vector("cards/Alchemist (NF).png",
    "cards/Giant's Path (NF).png", "tts/backs/relic-back.jpg")

  private def serving(art: ArtLibrary)(check: HttpTestServer => Unit): Unit =
    val server = HttpTestServer.start("art-routes-test")(_ =>
      ArtRoutes.route(art))
    try check(server)
    finally server.close()

  private def files(server: HttpTestServer): Vector[String] =
    val manifest = server.get("/art/manifest.json")
    assertEquals(manifest.statusCode(), 200)
    ujson.read(manifest.body())("files").arr.map(_.str).toVector

  test("without a folder the manifest is served and lists nothing"):
    serving(ArtLibrary.empty) { server =>
      assertEquals(files(server), Vector.empty)
      assertEquals(server.get("/art/cards/Alchemist%20(NF).png").statusCode(),
        404)
    }

  test("the manifest lists exactly the files present and in the catalog, " +
      "and is revalidated"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png",
      "cards/Giant's Path (NF).png", "cards/Not A Card (NF).png")
    serving(ArtLibrary.scan(Some(folder), catalog)) { server =>
      assertEquals(files(server), Vector("cards/Alchemist (NF).png",
        "cards/Giant's Path (NF).png"))
      assertEquals(cacheControl(server.get("/art/manifest.json")),
        Some("no-cache"))
    }

  test("a listed path is served with a day's cache, an ETag and its type"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png",
      "cards/Giant's Path (NF).png", "tts/backs/relic-back.jpg")
    serving(ArtLibrary.scan(Some(folder), catalog)) { server =>
      val card = server.getBytes("/art/cards/Alchemist%20(NF).png")
      assertEquals(card.statusCode(), 200)
      assertEquals(card.body().toVector, ArtFolders.onePixel.toVector)
      assertEquals(card.headers().firstValue("Cache-Control").get(),
        "public, max-age=86400")
      assert(card.headers().firstValue("ETag").isPresent)
      assertEquals(card.headers().firstValue("Content-Type").get(),
        "image/png")
      assertEquals(server.get("/art/cards/Giant's%20Path%20(NF).png")
        .statusCode(), 200)
      assertEquals(server.getBytes("/art/tts/backs/relic-back.jpg")
        .headers().firstValue("Content-Type").get(), "image/jpeg")
    }

  test("an unlisted path, a step out of the folder and a wrong extension " +
      "are not found"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png",
      "cards/Not A Card (NF).png", "cards/Alchemist (NF).gif")
    java.nio.file.Files.write(folder.resolveSibling("outside.png"),
      ArtFolders.onePixel)
    serving(ArtLibrary.scan(Some(folder), catalog)) { server =>
      Vector("/art/cards/Not%20A%20Card%20(NF).png",
        "/art/cards/Alchemist%20(NF).gif",
        "/art/../outside.png", "/art/cards/../../outside.png",
        "/art/cards/%2e%2e/%2e%2e/outside.png", "/art/", "/art",
        "/art/cards").foreach(path =>
        assertEquals(server.get(path).statusCode(), 404, path))
    }
