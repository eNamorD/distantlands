package distantlands.server

import java.nio.file.{Files, Path}

import distantlands.protocol.art.ArtCatalog

/** The host's art folder as the server serves it: the catalog paths the
  * folder held when the server started (game art design, "Serving the
  * art"). The folder is read once; a rescan needs a restart. A request is
  * answered from `files` alone, so no path a client sends reaches the
  * disk unless the catalog lists it.
  */
final class ArtLibrary private (
    folder: Option[Path],
    /** Each kept catalog path and the file it names. */
    val files: Map[String, Path],
    missing: Vector[String],
    isFolder: Boolean
):
  /** The startup line, when the host named a folder. */
  def summary: Option[String] =
    folder.map(dir => s"art: ${files.size} files from $dir")

  /** What the host should know is wrong with the folder, if anything: it
    * is not a folder, or it lacks catalog files, each named. */
  def warning: Option[String] = folder.flatMap { dir =>
    if !isFolder then
      Some(s"art: $dir is not a folder; no game art is served")
    else Option.when(missing.nonEmpty)(
      s"art: $dir lacks ${missing.size} catalog files: " +
        missing.mkString(", "))
  }

object ArtLibrary:
  private val Extensions = Set("png", "jpg", "webp")

  /** A server with no art folder. */
  val empty: ArtLibrary = new ArtLibrary(None, Map.empty, Vector.empty, true)

  /** Reads `folder` against `catalog`: a path is kept when its extension
    * is an image's and the folder holds a regular file there. */
  def scan(folder: Option[Path],
      catalog: Vector[String] = ArtCatalog.allPaths): ArtLibrary =
    folder.fold(empty) { dir =>
      if !Files.isDirectory(dir) then
        new ArtLibrary(Some(dir), Map.empty, Vector.empty, false)
      else
        val (kept, missing) = catalog.partition(path =>
          Extensions(path.drop(path.lastIndexOf('.') + 1)) &&
            Files.isRegularFile(dir.resolve(path)))
        new ArtLibrary(Some(dir),
          kept.map(path => path -> dir.resolve(path)).toMap, missing, true)
    }
