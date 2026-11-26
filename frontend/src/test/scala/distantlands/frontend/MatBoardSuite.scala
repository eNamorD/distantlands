package distantlands.frontend

import distantlands.model.PlayerColor
import Dom.{all, one}
import TestSites.*
import org.scalajs.dom

/** The map is the mat (real proportions design): one board, with every
  * piece at the rectangle `MatLayout` gives it.
  */
class MatBoardSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val tracks = distantlands.protocol.projection.GameTracksProjection(
    round = 1, visionsDrawn = 2, usurperLimited = false, limiterRound = 4,
    firstPlayerId = "red")
  private val relic = CardDetails("r1", "relic", "Brass Horse",
    orientation = Some("face-up"))
  private val woods = quietSite("woods", "Deep Woods")
    .withDenizenCapacity(2).withRelicCapacity(1).withRelics(1, relic)
  private val table = TestProjections.actingProjection(players = Vector(red),
    world = Vector(
      GameRegion("cradle", Vector(quietSite("keep", "The Keep"))),
      GameRegion("provinces", Vector(quietSite("ford", "Old Ford"), woods),
        emptySlots = 1),
      GameRegion("hinterland", Vector.empty, emptySlots = 3))).copy(
    worldDeckCount = 53, relicDeckCount = 9, tracks = Some(tracks),
    favorBanks = MatLayout.FavorBankSuits.map(FavorBankState(_, 3)))

  private val world = WorldBoardRenderer.world(table, BoardOptions.none)

  /** The four placement properties of `node`, as its inline style has
    * them. */
  private def box(node: dom.Element): Vector[(String, String)] =
    val style = node.asInstanceOf[dom.html.Element].style
    Vector("left", "top", "width", "height")
      .map(name => name -> style.getPropertyValue(name))

  private def at(rect: MatRect, parent: MatRect = MatLayout.Mat)
      : Vector[(String, String)] = rect.percentOf(parent)

  test("the world is the mat board and then the table's edge"):
    assertEquals(all(world, ":scope > *").map(_.getAttribute("class")),
      Vector(null, "mat-board", "table-edge"))
    assertEquals(all(one(world, ".mat-board"), ":scope > *")
      .map(_.getAttribute("class")), Vector("regions", "favor-banks"))

  test("a site is placed at its row, with its face first"):
    val site = one(world, """.site[aria-label="Deep Woods"]""")
    val row = MatLayout.siteRow(1, 1)
    assert(site.classList.contains("mat-slot"))
    assertEquals(box(site), at(row))
    val face = site.firstElementChild
    assert(face.classList.contains("site-face"))
    assertEquals(box(face), at(MatLayout.siteCard(1, 1), row))
    val style = site.asInstanceOf[dom.html.Element].style
    val faceBox = at(MatLayout.siteCard(1, 1), row).toMap
    assertEquals(style.getPropertyValue("--face-left"), faceBox("left"))
    assertEquals(style.getPropertyValue("--face-w"), faceBox("width"))
    // The face reaches past the row's foot by the frame's line, and the
    // name band stands at the face's foot.
    assertEquals(style.getPropertyValue("--face-bottom"), "-1.5717%")

  test("a site's cards stand in the three slots to its right, a relic " +
      "in the square top of its slot"):
    val site = one(world, """.site[aria-label="Deep Woods"]""")
    val row = MatLayout.siteRow(1, 1)
    val cards = all(site, ".site-cards > *")
    assertEquals(cards.size, 3)
    assertEquals(box(cards(0)), at(MatLayout.cardSlot(1, 1, 0), row))
    assertEquals(box(cards(1)), at(MatLayout.cardSlot(1, 1, 1), row))
    assertEquals(box(cards(2)), at(MatLayout.relicSlot(1, 1, 2), row))

  test("an empty site slot is the mat's frame"):
    val frames = all(world, ".site-slot-empty")
    assertEquals(frames.size, 4)
    assertEquals(box(frames(0)), at(MatLayout.siteFrame(1, 2)))
    assertEquals(box(frames(1)), at(MatLayout.siteFrame(2, 0)))

  test("a region's name and discard are placed in its column"):
    val provinces = one(world, """section.region[aria-label="Provinces"]""")
    assertEquals(box(one(provinces, ".region-label")),
      at(MatLayout.regionName(1)))
    assertEquals(box(one(provinces, ".region-header > .deck-figure")),
      at(MatLayout.discard(1)))

  test("the Cradle's foot holds the wheel, the track and both decks"):
    val foot = one(world, """section.region[aria-label="Cradle"] .deck-row""")
    assertEquals(all(foot, ":scope > *").map(_.classList.item(0)),
      Vector("round-tracker", "visions-drawn", "deck-figure", "deck-figure"))
    assertEquals(box(one(foot, ".round-tracker svg")),
      at(MatLayout.roundWheel))
    assertEquals(box(one(foot, ".track-note")), at(MatLayout.trackNote))
    val decks = all(foot, ":scope > .deck-figure")
    // The relic deck comes first, as the mat prints it left of the world
    // deck, so the reading order follows the board.
    assertEquals(box(decks(0)), at(MatLayout.relicDeck))
    assertEquals(box(decks(1)), at(MatLayout.worldDeck))
    assertEquals(decks.map(deck => one(deck, ".deck-caption").textContent),
      Vector("Relic deck", "World deck"))
    assertEquals(one(decks(0), ".pile-count").textContent, "×9")

  test("Visions Drawn is a marker on the mat's six cells"):
    val track = one(world, ".visions-drawn")
    assertEquals(track.getAttribute("aria-label"), "Visions Drawn 2")
    val cells = all(track, ".vision-cell")
    assertEquals(cells.map(_.textContent), Vector("0", "1", "2", "3", "4", "5"))
    assertEquals(cells.map(box), MatLayout.visionCells.map(at(_)))
    assertEquals(box(one(track, ".vision-marker")),
      at(MatLayout.visionCells(2)))

  test("a count past the track's end stands on its last cell"):
    val many = WorldBoardRenderer.world(table.copy(tracks =
      Some(tracks.copy(visionsDrawn = 7))), BoardOptions.none)
    assertEquals(box(one(many, ".vision-marker")),
      at(MatLayout.visionCells(5)))

  test("each favor bank stands on its circle"):
    val banks = all(world, ".favor-banks > .favor-bank")
    assertEquals(banks.map(box),
      MatLayout.FavorBankSuits.flatMap(MatLayout.favorBank).map(at(_)))

  test("the shared bank is on the table's edge and holds no relic deck"):
    val bank = one(world, ".table-edge > .shared-bank")
    assertEquals(all(bank, ".pile-display"), Vector.empty)

  test("what the mat and a site print is marked printed"):
    assertEquals(all(world, ".region-label.printed").size, 3)
    val site = one(world, """.site[aria-label="Deep Woods"]""")
    assertEquals(all(site, ".site-name.printed").size, 1)
    assertEquals(all(site, ".site-defense.printed").size, 1)
    assertEquals(all(site, ".site-footer.printed"), Vector.empty)
    assertEquals(all(site, ".site-tokens.printed, .site-pawns.printed"),
      Vector.empty)

  test("the relic deck says a relic is on top, as it shows an R"):
    val relicDeck = all(world, ".deck-row > .deck-figure")
      .find(deck => one(deck, ".deck-caption").textContent == "Relic deck")
      .getOrElse(fail("no relic deck"))
    val letter = one(relicDeck, ".deck-letter")
    assertEquals(letter.textContent, "R")
    assertEquals(letter.getAttribute("aria-label"), "Relic card on top")

  /** The mat prints each slot's name and each bank's suit; the counts are
    * play (real proportions design). */
  test("the mat's deck labels and bank suits are printed, the counts not"):
    val captions = all(world, ".deck-figure > .deck-caption")
    assertEquals(captions.size, 5)
    assert(captions.forall(_.classList.contains("printed")))
    val arcane = one(world, ".favor-bank.suit-arcane")
    assertEquals(arcane.textContent, "Arcane: 3")
    assertEquals(all(arcane, "span.printed").map(_.textContent), Vector("Arcane: "))
    assert(one(arcane, ".token-suit-arcane").classList.contains("printed"))
    assertEquals(one(arcane, ".favor-bank-count").textContent, "3")
    assert(!one(arcane, ".favor-bank-count").classList.contains("printed"))

  /** The review's finding: on the far zoomed-out map a count of two
    * figures left its disc. The count says how many figures it has, so
    * the stylesheet can set two smaller. */
  test("a bank's count says how many figures it has"):
    def countOf(favor: Int): dom.Element =
      one(WorldBoardRenderer.world(table.copy(favorBanks = Vector(
        FavorBankState("arcane", favor))), BoardOptions.none),
        ".favor-bank-count")
    assertEquals(countOf(9).getAttribute("data-figures"), "1")
    assertEquals(countOf(10).getAttribute("data-figures"), "2")
    assertEquals(countOf(10).textContent, "10")
    assertEquals(countOf(36).getAttribute("data-figures"), "2")

  /** The owner could not read a bank's favor (game art ruling R42): the
    * favor fills the mat's circle with the count over it, and the suit's
    * glyph stands on the mat's emblem. */
  test("a bank's suit stands on its emblem, and its favor fills its " +
      "circle with the count over it"):
    val arcane = one(world, ".favor-bank.suit-arcane")
    val bank = MatLayout.favorBank("arcane").get
    assertEquals(box(one(arcane, ":scope > .token-suit-arcane")),
      at(MatLayout.favorBankEmblem("arcane").get, bank))
    val token = one(arcane, ":scope > .favor-bank-token")
    assertEquals(box(token), at(MatLayout.favorBankCircle("arcane").get, bank))
    assertEquals(all(token, ":scope > *").map(_.getAttribute("class")),
      Vector("token-glyph token-favor", "favor-bank-count"))
    assertEquals(Option(token.closest(".printed")), None)

  /** An offered card is wrapped with its inspect button; the wrapper is what
    * stands in the slot, and a relic inside one still takes the square top. */
  test("an offered relic's wrapper stands in the square top of its slot"):
    val offered = TestBoardOptions.recording(TestBoardOptions.available(
      TestBoardOptions.option("relic", "r1"))).options
    val site = one(WorldBoardRenderer.world(table, offered),
      """.site[aria-label="Deep Woods"]""")
    val wrapper = all(site, ".site-cards > *")(2)
    assert(wrapper.classList.contains("board-option-card"),
      wrapper.getAttribute("class"))
    assertEquals(box(wrapper),
      at(MatLayout.relicSlot(1, 1, 2), MatLayout.siteRow(1, 1)))

  /** A pawn carries its long and short forms together, so a zoom never
    * re-renders (ruling R6): the dot in the player's colour, the player's
    * name for assistive technology and at rest, and the initial the
    * zoomed-out map shows. */
  test("a pawn carries its dot, its player's name and the initial"):
    val maximilian = GamePlayer("blue", "Maximilian", "Exile", PlayerColor.Blue)
    val placed = WorldBoardRenderer.world(table.copy(
      players = Vector(red, maximilian),
      pawnLocations = Vector(GamePawn("blue", "woods"))), BoardOptions.none)
    val pawn = one(placed, """.site[aria-label="Deep Woods"] .pawn""")
    val dot = one(pawn, ".pawn-dot")
    assertEquals(dot.getAttribute("aria-hidden"), "true")
    assert(dot.classList.contains("player-blue"), dot.getAttribute("class"))
    val initial = one(pawn, ".pawn-initial")
    assertEquals(initial.getAttribute("aria-hidden"), "true")
    assertEquals(initial.textContent, "M")
    assertEquals(one(pawn, ".player-ref").textContent, "Maximilian")
    assertEquals(pawn.getAttribute(HoverTip.Attribute), "Maximilian")

  /** The drawn wheel is printed and the round on it is play (ruling R8):
    * eight segments and numbers in one fill, the current round a wedge of
    * its own over its segment, and the limiter mark state. */
  test("the wheel's segments and numbers are printed, the round and the " +
      "limiter are not"):
    val locked = WorldBoardRenderer.world(table.copy(tracks = Some(tracks.copy(
      round = 3, usurperLimited = true, limiterRound = 5))), BoardOptions.none)
    val wheel = one(locked, ".round-tracker svg")
    val segments = all(wheel, "path.round-segment")
    assertEquals(segments.size, 8)
    assert(segments.forall(_.classList.contains("printed")))
    val numbers = all(wheel, "text.round-number")
    assertEquals(numbers.map(_.textContent), (1 to 8).map(_.toString).toVector)
    assert(numbers.forall(_.classList.contains("printed")))
    val current = one(wheel, ".round-current")
    assert(!current.classList.contains("printed"))
    assertEquals(current.parentNode.asInstanceOf[dom.Element]
      .getAttribute("data-round"), "3")
    assert(!one(wheel, ".limiter-marker").classList.contains("printed"))

  private val chancellor = GamePlayer("purple", "Purple", "Chancellor",
    PlayerColor.Purple)
  private val citizen = GamePlayer("blue", "Blue", "Citizen", PlayerColor.Blue)

  /** The table with an Empire seated beside the red Exile, the Oath held
    * by `holder`. */
  private def empireTable(holder: Option[String]) = table.copy(
    players = Vector(chancellor, citizen, red),
    oathkeeper = Some(distantlands.protocol.projection.OathkeeperProjection(
      goal = "supremacy", holderPlayerId = holder, side = "oathkeeper",
      usurperLimited = false, winnerPlayerId = None)))

  private def endDice(value: GameProjection): Vector[dom.Element] =
    all(WorldBoardRenderer.world(value, BoardOptions.none),
      ".round-tracker svg .end-die")

  /** The mat prints the Stable Regime targets on the wheel's lines after
    * rounds 5, 6 and 7: a purple die showing 6, 5 and 3 pips. */
  test("an Empire game's wheel prints the end die's target after rounds " +
      "five, six and seven"):
    val dice = endDice(empireTable(Some("purple")))
    assertEquals(dice.map(_.getAttribute("data-after-round")),
      Vector("5", "6", "7"))
    assertEquals(dice.map(all(_, ".end-die-pip").size), Vector(6, 5, 3))
    assert(dice.forall(_.classList.contains("printed")))
    assertEquals(dice.map(_.getAttribute("aria-label")), Vector(
      "End die after round 5: 6", "End die after round 6: 5 or 6",
      "End die after round 7: 3 or more"))
    assertEquals(dice(1).getAttribute(HoverTip.Attribute),
      "End of round 6: if the Chancellor or a Citizen is the Oathkeeper, " +
        "roll the end die. The Chancellor wins on a 5 or 6.")

  test("a game without a Chancellor prints no end die"):
    assertEquals(endDice(table), Vector.empty)

  /** The dice stand down while no roll would be made: the Oath with an
    * Exile, or with no one. */
  test("the end dice dim unless the Chancellor or a Citizen holds the Oath"):
    def idle(holder: Option[String]) =
      endDice(empireTable(holder)).map(_.classList.contains("end-die-idle"))
    assertEquals(idle(Some("purple")), Vector(false, false, false))
    assertEquals(idle(Some("blue")), Vector(false, false, false))
    assertEquals(idle(Some("red")), Vector(true, true, true))
    assertEquals(idle(None), Vector(true, true, true))
    assert(endDice(empireTable(Some("red"))).head
      .getAttribute(HoverTip.Attribute).endsWith(
        "No roll now: the Oathkeeper is not the Chancellor or a Citizen."))

  test("the tracker's name carries the end die's targets in an Empire game"):
    val name = one(WorldBoardRenderer.world(empireTable(Some("purple")),
      BoardOptions.none), ".round-tracker").getAttribute("aria-label")
    assert(name.contains(
      "end die after rounds 5, 6 and 7: 6, 5 or 6, 3 or more"), name)
