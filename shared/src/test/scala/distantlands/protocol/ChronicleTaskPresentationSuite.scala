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

  // Typed from OATH-CHRON01a-NF and OATH-CHRON01b-NF, symbols left out.
  private val resolveLegaciesAsTyped =
    "**Resolve Legacies.** Reveal facedown relics held by players. Dormant " +
      "legacies become active if their goal is met. If not, discard them. " +
      "All Imperials rule Imperial sites for legacy goals. Ties between " +
      "only Imperials are friendly. All other ties are unfriendly. Active " +
      "legacies of Exiles become dormant if goal is not met. Players " +
      "discard down to 3 active legacies. Shuffle discarded legacies into " +
      "legacy deck."
  private val assignTasksAsTyped =
    "**Assign Tasks.** The winner takes the Throne task card. The player " +
      "with the People's Favor takes the World task card. The player with " +
      "the Darkest Secret takes the Beacon task card. **If no one holds a " +
      "banner,** the winner may give its task to anyone. **If a player " +
      "holds both the Beacon and the World tasks,** they must give away " +
      "one of them."

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

  test("the Throne's Build Edifice and the Sun's Assign Tasks read as the " +
      "card data words them, symbols left out (R6)"):
    // Typed from OATH-CHRON02a-NF and OATH-CHRON01b-NF.
    assertEquals(card("The Throne").faces(0).lines(2),
      "**Build Edifice.** Draw the topmost edifice of each suit. You may " +
        "bury a denizen at a site you rule to replace it with the matching " +
        "drawn edifice. _Burying ignores the locked restriction and places " +
        "the card on the bottom of its respective deck._ Reshuffle the " +
        "edifice deck.")
    assertEquals(card("The Sun").faces(1).lines(1), assignTasksAsTyped)

  test("the Sun's front lists the six legacy goals in words, one a line"):
    assertEquals(card("The Sun").faces.head.lines.tail, Vector(
      "Hold People's Favor.", "Hold Darkest Secret.", "Rule the most sites.",
      "Hold Grand Scepter. _If none, win._",
      "Rule most cards of suit _(even advisers)_.",
      "Hold relic of highest value _(under name)_."))

  test("a two-sided card reads its front, a Back line, then its back; a " +
      "one-sided card reads its face"):
    // The Sun: Resolve Legacies and the six goals, Back, then Crown
    // Chancellor and Assign Tasks.
    val sun = card("The Sun").lines
    assertEquals((sun.size, sun.indexOf("Back")), (10, 7))
    assertEquals((sun.head, sun.last),
      (resolveLegaciesAsTyped, assignTasksAsTyped))
    val world = card("The World").lines
    assertEquals((world.size, world.head), (3,
      "**Separate Visions.** Set aside all 5 Vision cards."))
    assert(!world.contains("Back"), world)

  test("no line carries a card data symbol or the flip instruction"):
    val lines = ChronicleTaskPresentation.all.flatMap(_.lines)
    assert(!lines.exists(line => line.contains("symbol:") ||
      line.contains("`") || line.contains("flip this card")), lines)
