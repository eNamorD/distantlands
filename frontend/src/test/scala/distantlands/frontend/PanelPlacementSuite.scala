package distantlands.frontend

import distantlands.model.PlayerColor

import org.scalajs.dom
import Dom.{click, one}

/** Which pane each standing line of state belongs to. The oath and the
  * usurper limit describe the table, not the acting player's options, so
  * they sit with the board rather than in the action panel.
  */
class PanelPlacementSuite extends munit.FunSuite:
  private val tracks = distantlands.protocol.projection.GameTracksProjection(round = 2, visionsDrawn = 0,
    usurperLimited = true, limiterRound = 4, firstPlayerId = "red")

  private val oath = OathkeeperStatus("supremacy", None, "oathkeeper",
    usurperLimited = true, None, None)

  private def projection(
      oathkeeper: Option[OathkeeperStatus] = Some(oath),
      trackState: Option[distantlands.protocol.projection.GameTracksProjection] = Some(tracks)): GameProjection =
    TestProjections.actingProjection(
      players = Vector(GamePlayer("red", "Red", "Exile", PlayerColor.Red)),
      // The tracker is drawn inside the cradle, so the region has to exist
      // for the notice under it to be reachable at all.
      world = Vector(GameRegion("cradle", Vector.empty)))
      .copy(oathkeeper = oathkeeper, tracks = trackState)

  private def world(value: GameProjection): dom.Element =
    WorldBoardRenderer.world(value, BoardOptions.none)

  test("the oath and its holder are listed in the shared bank"):
    val value = projection(oathkeeper = Some(
      oath.copy(goal = "supremacy", holderPlayerId = None)))
    val status = Dom.firstMatch(world(value), ".shared-bank .oathkeeper-status")
      .getOrElse(fail("no oath line in the shared bank"))
    assertEquals(Dom.all(status, ":scope > *").map(_.textContent),
      Vector("Oathkeeper of Supremacy", "Oathkeeper: unheld"))

  /** The Oath is a printed card the table shows as words, so its goal opens
    * the overlay at the card's real ratio (implementation ruling R2). The
    * holder's line is state, not the card, and stays text. */
  test("the oath's goal opens its card with the Oath goal's plate"):
    var opened = Vector.empty[(String, Vector[String], Option[String])]
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _, plate, _) =>
        opened :+= (title, lines, plate)
      case _ => ()
    val status = Dom.one(world(projection()), ".shared-bank .oathkeeper-status")
    Dom.one(status, ".oathkeeper-goal").asInstanceOf[dom.html.Button].click()
    CardInspection.clear()
    assertEquals(opened, Vector(("Oathkeeper of Supremacy",
      Vector("Rules the most sites",
        "Successor to the Chancellor: Holds more relics"),
      Some("oath-goal"))))
    assertEquals(Dom.one(status, ".oathkeeper-holder").tagName, "SPAN")

  test("a goal the table does not know opens as its key, with no lines"):
    var opened = Vector.empty[(String, Vector[String])]
    CardInspection.onOpen:
      case CardInspection.Request.Text(title, lines, _, _, _) =>
        opened :+= title -> lines
      case _ => ()
    Dom.one(world(projection(oathkeeper = Some(oath.copy(goal = "unknown")))),
      ".oathkeeper-goal").asInstanceOf[dom.html.Button].click()
    CardInspection.clear()
    assertEquals(opened, Vector("unknown" -> Vector.empty))

  /** The goal is projected, so the panel prints the oath actually in play
    * rather than the one the first game happens to start with.
    */
  test("a goal other than supremacy is named as itself"):
    assertEquals(Dom.firstMatch(world(projection(oathkeeper =
      Some(oath.copy(goal = "the-people", holderPlayerId = Some("Blue"))))),
      ".shared-bank .oathkeeper-goal").map(_.textContent),
      Some("Oathkeeper of the People"))

  private val foundations = Vector(
    distantlands.protocol.projection.FoundationProjection(1, "normal",
      "Imperial Maps", "Fill the map."),
    distantlands.protocol.projection.FoundationProjection(3, "altered",
      "Public Ambitions", "Deal 4 legacy cards."))
  private val detailsWithNoGamesNoLineagesAndNoAtlas =
    distantlands.protocol.projection.ChronicleDetailsProjection(20,
      Vector.empty, Vector.empty, None)

  test("the shared bank holds no foundation strip, and the Chronicle " +
      "Details button stands after it only while the snapshot carries " +
      "Chronicle Details"):
    val shown = world(projection().copy(foundations = foundations,
      chronicleDetails = Some(detailsWithNoGamesNoLineagesAndNoAtlas)))
    assertEquals(Dom.all(shown, ".foundations").size, 0)
    assertEquals(Dom.all(one(shown, ".table-edge"), ":scope > *")
      .map(_.getAttribute("class")),
      Vector("shared-bank", "chronicle-details-button"))
    val button = one(shown, "button.chronicle-details-button")
    assertEquals(Vector(button.textContent, button.getAttribute("type"),
      button.getAttribute("aria-haspopup"),
      button.getAttribute("aria-controls")),
      Vector("Chronicle Details", "button", "dialog", "chronicle-details"))
    assertEquals(Dom.all(world(projection().copy(foundations = foundations)),
      ".chronicle-details-button").size, 0)

  test("the Chronicle Details button opens through the shell's hook, from " +
      "itself"):
    var origins = Vector.empty[dom.Element]
    ChronicleDetails.onOpen(origin => origins :+= origin)
    try
      val button = one(world(projection().copy(chronicleDetails =
        Some(detailsWithNoGamesNoLineagesAndNoAtlas))),
        "button.chronicle-details-button")
      click(button)
      assertEquals(origins, Vector(button))
    finally ChronicleDetails.clear()

  /** Beside the regions rather than under them: the map pane is wider than
    * it is tall, so a column costs nothing at fit and a row below costs the
    * whole map's scale.
    */
  test("the shared bank stands on the table's edge, under the mat"):
    val panel = world(projection())
    assertEquals(Dom.all(panel, ":scope > div").map(_.getAttribute("class")),
      Vector("mat-board", "table-edge"))
    assertEquals(Dom.all(Dom.one(panel, ".mat-board"), ":scope > *")
      .map(_.getAttribute("class")), Vector("regions", "favor-banks"))
    assertEquals(Dom.all(panel, ".table-edge > .shared-bank").size, 1)

  test("the usurper notice sits under the round tracker, at the projected round"):
    val round4 = projection(trackState =
      Some(tracks.copy(usurperLimited = true, limiterRound = 4)))
    val notice = Dom.firstMatch(world(round4),
      ".round-tracker .usurper-notice").getOrElse(fail("no usurper notice"))
    assertEquals(notice.textContent, "Usurper locked until round 4")
    assertEquals(Dom.firstMatch(world(projection(trackState =
      Some(tracks.copy(limiterRound = 5)))), ".usurper-notice")
      .map(_.textContent), Some("Usurper locked until round 5"))

  test("the round tracker names the first player, not their id"):
    val tracker = Dom.firstMatch(world(projection()), ".round-tracker")
      .getOrElse(fail("no round tracker"))
    assertEquals(Dom.firstMatch(tracker, ".track-summary").map(_.textContent),
      Some("Visions Drawn 0 · First player Red"))
    assertEquals(Dom.firstMatch(tracker, ".track-summary .player-ref")
      .map(_.getAttribute("data-player-id")), Some("red"))
    assert(tracker.getAttribute("aria-label").endsWith("first player Red"),
      tracker.getAttribute("aria-label"))

  test("an unlimited usurper gets no notice"):
    assertEquals(Dom.all(world(projection(trackState =
      Some(tracks.copy(usurperLimited = false)))), ".usurper-notice"),
      Vector.empty)

  /** The player strip already carries the acting player's resources, and the
    * site's loose wealth is drawn on the site itself.
    */
  test("the action panel carries neither the oath, a second resource line " +
      "nor the usurper notice"):
    // The projection carries both the acting player's resources and the
    // current site's loose wealth, and the oath and the usurper limit too.
    val value = projection().copy(
      activePlayerResources = Some(ActivePlayerResources(favor = 3,
        faceUpSecrets = 1, faceDownSecrets = 0, committedSecrets = 0,
        totalSecrets = 1, supply = 5)),
      currentSiteResources = Some(CurrentSiteResources(siteId = "site:woods",
        favor = 2, secrets = 1)))
    val panel = ActionPanels.actionsPanelFor(value)
    assertEquals(Dom.all(panel, ".oathkeeper-status"), Vector.empty)
    assertEquals(Dom.all(panel, ".resources"), Vector.empty)
    assertEquals(Dom.all(panel, ".site-resources"), Vector.empty)
    assert(!panel.textContent.contains("loose wealth"), panel.textContent)
    assert(!panel.textContent.contains("Usurper"), panel.textContent)

  /** The far zoomed-out map hides the tracker's words, so the lock is also
    * in the tracker's name and on the limiter mark's hover note. */
  test("the round tracker's name and the limiter mark state the lock"):
    val tracker = Dom.one(world(projection(trackState =
      Some(tracks.copy(usurperLimited = true, limiterRound = 4)))),
      ".round-tracker")
    assert(tracker.getAttribute("aria-label")
      .contains("Usurper locked until round 4"),
      tracker.getAttribute("aria-label"))
    assertEquals(Dom.one(tracker, ".limiter-marker")
      .getAttribute(HoverTip.Attribute), "Usurper locked until round 4")
    val open = Dom.one(world(projection(trackState =
      Some(tracks.copy(usurperLimited = false)))), ".round-tracker")
    assert(!open.getAttribute("aria-label").contains("Usurper"))
