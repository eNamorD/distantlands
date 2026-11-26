package distantlands.server

import java.nio.file.{Files, Path}
import java.util.Base64

/** The startup scan of the host's art folder (game art design, "Serving
  * the art"). Every folder here is a temporary one of 1×1 PNGs; no test
  * reads a licensed file. */
class ArtLibrarySuite extends munit.FunSuite:
  private val catalog = Vector("cards/Alchemist (NF).png",
    "cards/Sticky Fire (NF).png", "tts/backs/relic-back.jpg")

  test("without a folder the library is empty and logs nothing"):
    val art = ArtLibrary.scan(None, catalog)
    assertEquals(art.files, Map.empty[String, Path])
    assertEquals(art.summary, None)
    assertEquals(art.warning, None)

  test("the library keeps exactly the catalog paths the folder holds"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png",
      "tts/backs/relic-back.jpg", "cards/Not A Card (NF).png",
      "cards/notes.txt")
    val art = ArtLibrary.scan(Some(folder), catalog)
    assertEquals(art.files.keySet,
      Set("cards/Alchemist (NF).png", "tts/backs/relic-back.jpg"))
    assertEquals(art.files("cards/Alchemist (NF).png"),
      folder.resolve("cards/Alchemist (NF).png"))

  test("a catalog path that is a directory in the folder is not kept"):
    val folder = ArtFolders.onePixelFolder()
    Files.createDirectories(folder.resolve("cards/Alchemist (NF).png"))
    assertEquals(ArtLibrary.scan(Some(folder), catalog).files,
      Map.empty[String, Path])

  test("the summary counts the files and names the folder"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png")
    assertEquals(ArtLibrary.scan(Some(folder), catalog).summary,
      Some(s"art: 1 files from $folder"))

  test("the warning names each catalog path the folder lacks"):
    val folder = ArtFolders.onePixelFolder("cards/Alchemist (NF).png")
    assertEquals(ArtLibrary.scan(Some(folder), catalog).warning, Some(
      s"art: $folder lacks 2 catalog files: cards/Sticky Fire (NF).png, " +
        "tts/backs/relic-back.jpg"))

  test("a complete folder gives no warning"):
    val folder = ArtFolders.onePixelFolder(catalog*)
    assertEquals(ArtLibrary.scan(Some(folder), catalog).warning, None)

  test("a path that is not a folder serves nothing and says so"):
    val missing = Files.createTempDirectory("art-missing-").resolve("none")
    val art = ArtLibrary.scan(Some(missing), catalog)
    assertEquals(art.files, Map.empty[String, Path])
    assertEquals(art.summary, Some(s"art: 0 files from $missing"))
    assertEquals(art.warning,
      Some(s"art: $missing is not a folder; no game art is served"))

/** Temporary art folders for the server suites. */
object ArtFolders:
  /** The bytes of a 1×1 transparent PNG. */
  val onePixel: Array[Byte] = Base64.getDecoder.decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAC" +
      "hwGA60e6kgAAAABJRU5ErkJggg==")

  /** A new temporary folder holding a 1×1 PNG at each of `paths`,
    * whatever the path's extension says. */
  def onePixelFolder(paths: String*): Path =
    val folder = Files.createTempDirectory("art-folder-")
    paths.foreach { path =>
      val file = folder.resolve(path)
      Files.createDirectories(file.getParent)
      Files.write(file, onePixel)
    }
    folder
