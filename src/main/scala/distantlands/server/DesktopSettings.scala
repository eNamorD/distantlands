package distantlands.server

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.Properties

import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

/** The desktop profile's optional per-user settings file. */
object DesktopSettings:
  val Keys: Vector[String] = Vector(
    "DLQ_HOST",
    "DLQ_PORT",
    "DLQ_PUBLIC_BASE_URL",
    "DLQ_DATABASE_PATH",
    "DLQ_OPEN_BROWSER"
  )

  val template: String = Vector(
    "# Distant Lands settings, used when the server is started with Start Distant Lands.",
    "# Remove the leading # from a line to use it. Environment variables and",
    "# command-line options override this file. Restart the server after editing.",
    "# On Windows, write paths with forward slashes, for example C:/DistantLandsData/database.",
    "",
    "# Address to listen on. 0.0.0.0 accepts other computers on your network;",
    "# 127.0.0.1 accepts only this computer.",
    "#DLQ_HOST=0.0.0.0",
    "",
    "# Port to listen on. Seat links contain the port.",
    "#DLQ_PORT=8080",
    "",
    "# Address players open, if the detected one is wrong. Seat links contain it.",
    "#DLQ_PUBLIC_BASE_URL=http://192.168.1.20:8080",
    "",
    "# Database file prefix. Default: the data folder next to this file.",
    "#DLQ_DATABASE_PATH=/path/to/DistantLandsData/database",
    "",
    "# Set to false to stop the browser opening at startup.",
    "#DLQ_OPEN_BROWSER=true",
    ""
  ).mkString("\n")

  def load(file: Path): Either[String, Map[String, String]] =
    if !Files.exists(file) then Right(Map.empty)
    else
      try
        val properties = new Properties()
        val reader = Files.newBufferedReader(file, UTF_8)
        try properties.load(reader)
        finally reader.close()
        val values = properties.stringPropertyNames.asScala.toVector.sorted
          .map(key => key -> properties.getProperty(key).trim)
        values.map(_._1).find(key => !Keys.contains(key)) match
          case Some(unknown) => Left(s"$file: unknown setting $unknown")
          case None => Right(values.filter(_._2.nonEmpty).toMap)
      catch
        case NonFatal(_) => Left(s"$file: cannot read settings file")

  def writeTemplateIfMissing(file: Path): Option[String] =
    if Files.exists(file) then None
    else
      try
        Files.createDirectories(file.getParent)
        Files.write(
          file,
          template.getBytes(UTF_8),
          StandardOpenOption.CREATE_NEW,
          StandardOpenOption.WRITE
        )
        None
      catch
        case NonFatal(error) =>
          Some(s"could not create settings file $file: ${error.getMessage}")
