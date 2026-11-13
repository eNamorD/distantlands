package distantlands.frontend

import distantlands.model.PlayerColor

import org.scalajs.dom

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
      case CardInspection.Request.Text(title, lines, _, plate) =>
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
      case CardInspection.Request.Text(title, lines, _, _) =>
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

  test("the foundations close the shared bank, and a projection with " +
      "none draws no strip"):
    val foundations = Vector(
      distantlands.protocol.projection.FoundationProjection(1, "normal",
        "Imperial Maps", "Fill the map."),
      distantlands.protocol.projection.FoundationProjection(3, "altered",
        "Public Ambitions", "Deal 4 legacy cards."))
    val bank = Dom.firstMatch(world(projection().copy(
      foundations = foundations)), ".shared-bank")
      .getOrElse(fail("no shared bank"))
    assertEquals(bank.lastElementChild.getAttribute("class"), "foundations")
    assertEquals(Dom.all(bank, ".foundation-name").map(_.textContent),
      Vector("Imperial Maps", "Public Ambitions"))
    assertEquals(Dom.all(world(projection()), ".foundations").size, 0)

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
