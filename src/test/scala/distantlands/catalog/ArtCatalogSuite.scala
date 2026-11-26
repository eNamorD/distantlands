package distantlands.catalog

import distantlands.gameplay.cards.NewFoundations
import distantlands.protocol.art.ArtCatalog
import distantlands.protocol.projection.VisionCardPresentation

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
    assertEquals(ArtCatalog.allPaths.size, faces.size + 4)

  test("every path is a png, jpg or webp under cards/ or tts/, with no " +
      "step out of the folder"):
    ArtCatalog.allPaths.foreach { path =>
      assert(path.matches("""(cards|tts)/[^\\]+\.(png|jpg|webp)"""), path)
      assert(!path.split('/').contains(".."), path)
    }
