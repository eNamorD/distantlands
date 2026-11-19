package distantlands.protocol

import distantlands.protocol.projection.{ChronicleTaskFace,
  ChronicleTaskPresentation}

/** The five Chronicle task cards as the card data words them (Chronicle
  * Details design, slice 2): their order, their faces, and the steps each
  * face holds. */
class ChronicleTaskPresentationSuite extends munit.FunSuite:
  private val Heading = """^\*\*(.+?)\.\*\*""".r.unanchored

  /** Each line's bold step name, where it opens with one. */
  private def stepsOf(face: ChronicleTaskFace): Vector[String] =
    face.lines.collect { case Heading(step) => step }

  private def card(title: String): ChronicleTaskPresentation =
    ChronicleTaskPresentation.all.find(_.title == title)
      .getOrElse(fail(s"no task card $title"))

  test("the five cards stand in Chronicle order, and only the Sun and the " +
      "Throne have a back"):
    assertEquals(ChronicleTaskPresentation.all.map(task =>
      task.title -> task.faces.map(_.label)), Vector(
        "The Sun" -> Vector("Front", "Back"),
        "The Throne" -> Vector("Front", "Back"),
        "The World" -> Vector("Front"),
        "The Beacon" -> Vector("Front"),
        "The Stars" -> Vector("Front")))

  test("each face holds the steps the card data prints on it"):
    assertEquals(ChronicleTaskPresentation.all.map(_.faces.map(stepsOf)),
      Vector(
        Vector(Vector("Resolve Legacies"),
          Vector("Crown Chancellor", "Assign Tasks")),
        Vector(Vector("Offer Citizenship", "Shape Empire", "Build Edifice",
            "Stock Reliquary"),
          Vector("Scatter Relics", "Score Lineage Points", "Swear an Oath")),
        Vector(Vector("Separate Visions", "Gather Denizens",
          "Protect and Dispossess")),
        Vector(Vector("Introduce Newcomers", "Welcome a Stranger",
          "Shape Foundation")),
        Vector(Vector("Write in the Stars", "Archive Rival Sites",
          "Clean Up", "Play Again or Store"))))

  test("the Sun's front lists the six legacy goals in words, one a line"):
    assertEquals(card("The Sun").faces.head.lines.tail, Vector(
      "Hold People's Favor.", "Hold Darkest Secret.", "Rule the most sites.",
      "Hold Grand Scepter. _If none, win._",
      "Rule most cards of suit _(even advisers)_.",
      "Hold relic of highest value _(under name)_."))

  test("a two-sided card reads its front, a Back line, then its back; a " +
      "one-sided card reads its face"):
    val sun = card("The Sun")
    assertEquals(sun.lines,
      sun.faces(0).lines ++ Vector("Back") ++ sun.faces(1).lines)
    val world = card("The World")
    assertEquals(world.lines, world.faces.head.lines)

  test("no line carries a card data symbol or the flip instruction"):
    val lines = ChronicleTaskPresentation.all.flatMap(_.lines)
    assert(!lines.exists(line => line.contains("symbol:") ||
      line.contains("`") || line.contains("flip this card")), lines)
