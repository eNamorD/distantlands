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
      faces.size + 4 + 24 + 4 + 12 + 4 + 1 + 45)

  test("every path is a png, jpg or webp under cards/, symbols/ or tts/, " +
      "with no " +
      "step out of the folder"):
    ArtCatalog.allPaths.foreach { path =>
      assert(path.matches("""(cards|symbols|tts)/[^\\]+\.(png|jpg|webp)"""), path)
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

  private val symbols = Vector("favor" -> "symbol-favor.png",
    "favor-burnt" -> "symbol-favorb.png", "secret" -> "symbol-secret.png",
    "secret-burnt" -> "symbol-secretb.png",
    "suit-arcane" -> "symbol-suit-arcane.png",
    "suit-beast" -> "symbol-suit-beast.png",
    "suit-discord" -> "symbol-suit-discord.png",
    "suit-hearth" -> "symbol-suit-hearth.png",
    "suit-nomad" -> "symbol-suit-nomad.png",
    "suit-order" -> "symbol-suit-order.png",
    "attack-die" -> "symbol-dicer.png", "defense-die" -> "symbol-diceb.png",
    "round-die" -> "symbol-dicep.png", "skull" -> "symbol-skull.png",
    "shield" -> "symbol-shield.png", "sword" -> "symbol-sword.png",
    "hollow-sword" -> "symbol-swordh.png")

  test("every sprite token but the warband has its symbol (R24)"):
    symbols.foreach((token, file) => assertEquals(
      ArtCatalog.symbol(token).map(_.path), Some(s"symbols/$file"), token))
    assertEquals(ArtCatalog.symbol("warband"), None)
    assertEquals(ArtCatalog.symbol("unknown"), None)

  test("the adviser-only and site-only banners have their symbols, and " +
      "the towers none (R32)"):
    assertEquals(ArtCatalog.restriction("adviser").map(_.path),
      Some("symbols/symbol-restrictionperson.png"))
    assertEquals(ArtCatalog.restriction("site").map(_.path),
      Some("symbols/symbol-restrictiontree.png"))
    Vector("tower", "tower-ruined", "locked").foreach(kind =>
      assertEquals(ArtCatalog.restriction(kind), None, kind))

  test("favor and secrets have a token's picture, and nothing else does " +
      "(R23)"):
    assertEquals(ArtCatalog.token("favor").map(_.path),
      Some("tts/tokens/favor.png"))
    assertEquals(ArtCatalog.token("secret").map(_.path),
      Some("tts/tokens/secret.png"))
    Vector("favor-burnt", "secret-burnt", "warband", "suit-order")
      .foreach(token => assertEquals(ArtCatalog.token(token), None, token))

  test("each player colour has a pawn and a warband (R26)"):
    distantlands.model.PlayerColor.all.foreach { colour =>
      assertEquals(ArtCatalog.pawn(colour.key).map(_.path),
        Some(s"tts/pieces/pawn-${colour.key}.png"))
      assertEquals(ArtCatalog.warband(colour.key).map(_.path),
        Some(s"tts/pieces/warband-${colour.key}.png"))
    }
    assertEquals(ArtCatalog.pawn("green"), None)

  test("Imperial warbands are purple and bandits have their own piece"):
    assertEquals(ArtCatalog.warband("empire").map(_.path),
      Some("tts/pieces/warband-purple.png"))
    assertEquals(ArtCatalog.warband("bandit").map(_.path),
      Some("tts/tokens/bandit.png"))

  test("the markers, the titles and the die sheets have their files"):
    assertEquals(ArtCatalog.visionsDrawnMarker.path,
      "tts/markers/visions-drawn-marker.png")
    assertEquals(ArtCatalog.festivalMarker.path,
      "tts/tokens/festival-underway.jpg")
    assertEquals(ArtCatalog.usurperMarker.path, "tts/tokens/usurper.png")
    assertEquals(Vector("oathkeeper", "usurper").map(side =>
      ArtCatalog.title(side).map(_.path)), Vector(
      Some("tts/titles/oathkeeper.jpg"), Some("tts/titles/usurper.jpg")))
    assertEquals(ArtCatalog.title("chancellor"), None)
    assertEquals(ArtCatalog.dieSheet("attack").map(_.path),
      Some("tts/attack-die/attack_die.png"))
    assertEquals(ArtCatalog.dieSheet("defense").map(_.path),
      Some("tts/defense-die/defense_die.png"))
    Vector(ArtCatalog.visionsDrawnMarker.path, ArtCatalog.usurperMarker.path,
      "tts/attack-die/attack_die.png", "tts/tokens/bandit.png",
      "symbols/symbol-dicep.png").foreach(path =>
      assertEquals(ArtCatalog.allPaths.count(_ == path), 1, path))

