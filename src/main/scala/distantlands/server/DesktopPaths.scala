package distantlands.server

import java.nio.file.{Path, Paths}

/** Per-user locations used by the desktop launch profile. */
object DesktopPaths:
  def appDataDirectory(
      osName: String,
      environment: Map[String, String],
      fallbackHome: String
  ): Path =
    val home = nonBlank(environment, "HOME").getOrElse(fallbackHome)
    val os = osName.toLowerCase
    if os.startsWith("mac") then
      Paths.get(home, "Library", "Application Support", "DistantLands")
    else if os.startsWith("windows") then
      nonBlank(environment, "LOCALAPPDATA")
        .map(Paths.get(_, "DistantLands"))
        .getOrElse(Paths.get(home, "AppData", "Local", "DistantLands"))
    else
      nonBlank(environment, "XDG_DATA_HOME")
        .map(Paths.get(_, "distantlands"))
        .getOrElse(Paths.get(home, ".local", "share", "distantlands"))

  def settingsFile(appData: Path): Path =
    appData.resolve("distantlands.properties")

  def databasePath(appData: Path): Path =
    appData.resolve("data").resolve("database")

  private def nonBlank(
      environment: Map[String, String],
      name: String
  ): Option[String] = environment.get(name).filter(_.trim.nonEmpty)
