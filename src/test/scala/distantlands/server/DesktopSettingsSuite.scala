package distantlands.server

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}

class DesktopSettingsSuite extends munit.FunSuite:
  private val directory = FunFixture[Path](
    setup = _ => Files.createTempDirectory("oathdigital-settings"),
    teardown = path => deleteRecursively(path)
  )

  directory.test("a missing file has no settings") { dir =>
    assertEquals(DesktopSettings.load(dir.resolve("distantlands.properties")), Right(Map.empty[String, String]))
  }

  directory.test("known keys load trimmed and blank values are dropped") { dir =>
    val file = write(dir, "DLQ_PORT = 9000 \nDLQ_HOST=\n# DLQ_OPEN_BROWSER=false\nDLQ_DATABASE_PATH=C:/OathData/database\n")
    assertEquals(
      DesktopSettings.load(file),
      Right(Map("DLQ_PORT" -> "9000", "DLQ_DATABASE_PATH" -> "C:/OathData/database"))
    )
  }

  directory.test("an unknown key is a configuration error naming the file") { dir =>
    val file = write(dir, "DLQ_PORT=9000\nDLQ_PROT=9001\n")
    assertEquals(DesktopSettings.load(file), Left(s"$file: unknown setting DLQ_PROT"))
  }

  directory.test("an unreadable settings path is a configuration error") { dir =>
    val file = Files.createDirectory(dir.resolve("distantlands.properties"))
    assertEquals(DesktopSettings.load(file), Left(s"$file: cannot read settings file"))
  }

  directory.test("the template comments out every key and loads as empty") { dir =>
    val file = dir.resolve("app").resolve("distantlands.properties")
    assertEquals(DesktopSettings.writeTemplateIfMissing(file), None)
    val lines = new String(Files.readAllBytes(file), UTF_8).linesIterator.toVector
    DesktopSettings.Keys.foreach(key => assert(lines.exists(_.startsWith(s"#$key=")), key))
    assert(lines.forall(line => line.trim.isEmpty || line.startsWith("#")), lines.mkString("\n"))
    assertEquals(DesktopSettings.load(file), Right(Map.empty[String, String]))
  }

  directory.test("an existing settings file is never overwritten") { dir =>
    val file = write(dir, "DLQ_PORT=9000\n")
    assertEquals(DesktopSettings.writeTemplateIfMissing(file), None)
    assertEquals(new String(Files.readAllBytes(file), UTF_8), "DLQ_PORT=9000\n")
  }

  directory.test("a template that cannot be written is a warning") { dir =>
    val blocker = write(dir, "not a directory")
    val warning = DesktopSettings.writeTemplateIfMissing(blocker.resolve("distantlands.properties"))
    assert(warning.exists(_.startsWith("could not create settings file ")), warning.toString)
  }

  private def write(dir: Path, content: String): Path =
    Files.write(dir.resolve("distantlands.properties"), content.getBytes(UTF_8))

  private def deleteRecursively(path: Path): Unit =
    if Files.isDirectory(path) then
      val children = Files.list(path)
      try children.toArray.foreach(child => deleteRecursively(child.asInstanceOf[Path]))
      finally children.close()
    Files.deleteIfExists(path)
