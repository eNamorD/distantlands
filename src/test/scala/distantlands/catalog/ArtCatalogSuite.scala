package distantlands.catalog

import java.nio.file.{Files, Path}

import distantlands.gameplay.cards.NewFoundations
import distantlands.protocol.art.ArtCatalog
import distantlands.protocol.projection.{OathkeeperPresentation,
  VisionCardPresentation}

/** `ArtCatalog` names files by the cards' printed names, which the card
  * objects own (game art design, "The catalog"). A renamed card, or a card
  * the table forgot, fails here. */
class ArtCatalogSuite extends munit.FunSuite:
  private val catalog = NewFoundations.catalog

  private def pathOf(name: String): Option[String] =
    ArtCatalog.cardFace(name).map(_.path)

  test("every relic, edifice face, legacy and Vision has one face path, " +
      "named for the card"):
    val names = catalog.relics.map(_.name) ++
      catalog.edifices.flatMap(e => Vector(e.intact.name, e.ruined.name)) ++
      catalog.legacies.map(_.name) ++
      VisionCardPresentation.byId.values.map(_.name)
    names.foreach(name =>
      assertEquals(pathOf(name), Some(s"cards/$name (NF).png"), name))

  test("every denizen has one face path: its New Foundations file when " +
      "the set has one, else its base file"):
    assertEquals(ArtCatalog.reprintedDenizens.size, 33)
    assertEquals(ArtCatalog.baseDenizens.size, 222)
    assertEquals((ArtCatalog.reprintedDenizens ++ ArtCatalog.baseDenizens)
      .sorted, catalog.denizens.map(_.name).sorted)
    ArtCatalog.reprintedDenizens.foreach(name =>
      assertEquals(pathOf(name), Some(s"cards/$name (NF).png"), name))
    ArtCatalog.baseDenizens.foreach(name =>
      assertEquals(pathOf(name), Some(s"cards/$name.png"), name))

  test("a name no card prints has no face path"):
    assertEquals(pathOf("Facedown denizen"), None)

  test("the lists hold no name the card objects do not print"):
    assertEquals(ArtCatalog.relics.toSet, catalog.relics.map(_.name).toSet)
    assertEquals(ArtCatalog.edificeFaces.toSet, catalog.edifices
      .flatMap(e => Vector(e.intact.name, e.ruined.name)).toSet)
    assertEquals(ArtCatalog.legacies.toSet,
      catalog.legacies.map(_.name).toSet)
    assertEquals(ArtCatalog.visions.toSet,
      VisionCardPresentation.byId.values.map(_.name).toSet)

  test("each kind of card a viewer may not identify has its back"):
    assertEquals(Vector("denizen", "vision", "relic", "legacy")
      .map(kind => ArtCatalog.cardBack(kind).map(_.path)), Vector(
      Some("tts/backs/denizen-back.jpg"), Some("tts/backs/vision-back.jpg"),
      Some("tts/backs/relic-back.jpg"), Some("tts/backs/legacy-back.jpg")))
    assertEquals(ArtCatalog.cardBack("edifice"), None)

  test("no two faces share a path, and every path is listed once"):
    assertEquals(ArtCatalog.allPaths.distinct, ArtCatalog.allPaths)
    val faces = ArtCatalog.reprintedDenizens ++ ArtCatalog.baseDenizens ++
      ArtCatalog.relics ++ ArtCatalog.edificeFaces ++ ArtCatalog.legacies ++
      ArtCatalog.visions
    assertEquals(faces.distinct, faces)
    assertEquals(ArtCatalog.allPaths.size,
      faces.size + 4 + 24 + 4 + 12 + 4 + 1)

  test("every path is a png, jpg or webp under cards/ or tts/, with no " +
      "step out of the folder"):
    ArtCatalog.allPaths.foreach { path =>
      assert(path.matches("""(cards|tts)/[^\\]+\.(png|jpg|webp)"""), path)
      assert(!path.split('/').contains(".."), path)
    }

  /** The page a host assembles the folder from must list what the server
    * serves, no more and no less. */
  test("game-art.md lists exactly the catalog's paths, in order"):
    val page = Files.readString(Path.of("docs", "operations", "game-art.md"))
    val listed = page.linesIterator.dropWhile(_ != "## Paths")
      .collect { case s"- `$path`" => path }.toVector
    assertEquals(listed, ArtCatalog.allPaths)

  test("every site has one face path, named for the site"):
    assertEquals(ArtCatalog.sites.sorted, catalog.sites.map(_.name).sorted)
    catalog.sites.foreach(site => assertEquals(pathOf(site.name),
      Some(s"cards/${site.name} (NF).png"), site.name))

  test("each banner face has its file"):
    assertEquals(Vector("mob", "grand-council", "wandering-flame", "festival")
      .map(face => ArtCatalog.banner(face).map(_.path)), Vector(
      Some("cards/Banner of the People's Favor - Mob (NF).png"),
      Some("cards/Banner of the People's Favor - Grand Council (NF).png"),
      Some("cards/Banner of the Darkest Secret - Wandering Flame (NF).png"),
      Some("cards/Banner of the Darkest Secret - Festival (NF).png")))
    assertEquals(ArtCatalog.banner("unknown"), None)

  test("both faces of every foundation card have their file"):
    val numerals = Vector("I", "II", "III", "IV", "V", "VI")
    FoundationCards.all.zipWithIndex.foreach { (card, at) =>
      Vector(card.normal, card.altered).foreach(face =>
        assertEquals(ArtCatalog.foundation(at + 1, face.name).map(_.path),
          Some(s"cards/Foundation ${numerals(at)} - ${face.name} (NF).png"),
          face.name))
    }
    assertEquals(ArtCatalog.foundation(7, "Imperial Maps"), None)
    assertEquals(ArtCatalog.foundation(1, "Festival"), None)

  test("each Oath goal has its file"):
    assertEquals(OathkeeperPresentation.byGoal.keySet,
      Set("supremacy", "protection", "devotion", "the-people"))
    assertEquals(Vector("supremacy", "protection", "devotion", "the-people")
      .map(goal => ArtCatalog.oathGoal(goal).map(_.path)), Vector(
      Some("cards/Supremacy Oath (NF).png"),
      Some("cards/Protection Oath (NF).png"),
      Some("cards/Devotion Oath (NF).png"),
      Some("cards/People Oath (NF).png")))
    assertEquals(ArtCatalog.oathGoal("unknown"), None)

  test("the mat is one file"):
    assertEquals(ArtCatalog.mat.path, "tts/mat/mat.jpg")
    assert(ArtCatalog.allPaths.contains("tts/mat/mat.jpg"))
