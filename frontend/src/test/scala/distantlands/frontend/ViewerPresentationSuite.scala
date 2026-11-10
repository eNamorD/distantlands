package distantlands.frontend

import distantlands.frontend.Dom.all
import distantlands.frontend.ServerModeFixtures.twoSeatProjection
import distantlands.protocol.projection.WalkerStatusProjection

import munit.FunSuite

/** What each viewer is shown for the projected phase: who waits for whom,
  * and the status line.
  */
class ViewerPresentationSuite extends FunSuite:

  /** A finished game won by blue, as every viewer is projected it. */
  private def wonByBlue: GameProjection =
    twoSeatProjection(phase = "game-over", activeParticipantId = "red-exile")
      .copy(oathkeeper = Some(OathkeeperStatus("supremacy", Some("blue-exile"),
        "usurper", usurperLimited = false, winnerPlayerId = Some("blue-exile"),
        winnerVictoryKind = Some("usurper"))))

  private val startChronicle: WalkerDecisionState = WalkerDecisionState(
    "chronicle-begin", "chronicle.begin", query = Some(
      DecisionQueryState.ChooseOne(Vector(DecisionOptionState("button",
        "chronicle.start", "Start Chronicle")),
        heading = Some("Start Chronicle"))))

  test("the winner gets controls for their parked Start Chronicle decision"):
    val presentation = ServerUiSupport.viewerPresentation(
      wonByBlue.copy(walkerDecision = Some(startChronicle)), "blue-exile")
    assert(presentation.showGameplayControls)

  test("after the game, other players wait on the winner's Start Chronicle"):
    val value = wonByBlue.copy(walkerWaiting =
      Some(WalkerWaitingState(Vector("blue-exile"), Some("Start Chronicle"))))
    val presentation = ServerUiSupport.viewerPresentation(value, "red-exile")
    assert(!presentation.showGameplayControls)
    assertEquals(presentation.waitingForPlayerIds, Vector("blue-exile"))
    assertEquals(ActionDecisionRenderer.status(value, presentation).textContent,
      "Waiting for Blue Exile: Start Chronicle")

  test("the waiting line names every awaited player"):
    val value = wonByBlue.copy(walkerWaiting = Some(WalkerWaitingState(
      Vector("blue-exile", "red-exile"), Some("Start Chronicle"))))
    val presentation = ServerUiSupport.viewerPresentation(value, "spectator")
    assertEquals(presentation.waitingForPlayerIds,
      Vector("blue-exile", "red-exile"))
    assertEquals(presentation.waitingForDisplayNames,
      Vector("Blue Exile", "Red Exile"))
    assertEquals(ActionDecisionRenderer.status(value, presentation).textContent,
      "Waiting for Blue Exile, Red Exile: Start Chronicle")

  test("a finished game with nothing parked hides every control"):
    val presentation = ServerUiSupport.viewerPresentation(wonByBlue, "red-exile")
    assert(!presentation.showGameplayControls)
    assertEquals(presentation.waitingForPlayerIds, Vector.empty)

  test("the game-over line names the winner as a player, not by id"):
    val status = ActionDecisionRenderer.status(wonByBlue,
      ServerUiSupport.viewerPresentation(wonByBlue, "red-exile"))
    assertEquals(status.textContent,
      "Game over — Blue Exile wins (Usurper victory).")
    assertEquals(status.querySelector(".player-ref").textContent, "Blue Exile")

  test("the active player's own turn reads as their turn in the phase"):
    val value = twoSeatProjection(phase = "act")
    val status = ActionDecisionRenderer.status(value,
      ServerUiSupport.viewerPresentation(value, "red-exile"))
    assertEquals(status.textContent, "Red Exile's turn · Act")
    assertEquals(status.querySelector(".player-ref").textContent, "Red Exile")

  test("inactive Wake viewer waits and receives no gameplay controls"):
    val value = twoSeatProjection(
      phase = "wake",
      activeParticipantId = "red-exile"
    )
    val presentation = ServerUiSupport.viewerPresentation(value, "blue-exile")

    assert(!presentation.showGameplayControls)
    assertEquals(presentation.waitingForPlayerIds, Vector("red-exile"))
    assertEquals(presentation.waitingForDisplayNames, Vector("Red Exile"))

  /** Fix round 1: a parked walker `Decide`'s owner is not always the active
    * participant (Task 5). `walkerDecision` is projected to the owner alone
    * regardless of whose turn it is, so `viewerPresentation` must grant
    * controls off that field directly rather than off `activeParticipantId`
    * -- the bug this guards against left the owner and the active player
    * each waiting on the other.
    */
  test("an off-turn owner of a parked walker decision keeps gameplay controls"):
    val value = twoSeatProjection(activeParticipantId = "red-exile")
      .copy(walkerDecision = Some(ForgeFixture.parked))
    val owner = ServerUiSupport.viewerPresentation(value, "blue-exile")
    assert(owner.showGameplayControls)
    assertEquals(owner.waitingForPlayerIds, Vector.empty)
    assertEquals(owner.waitingForDisplayNames, Vector.empty)

  test("the active participant waits for the walker decision's off-turn owner"):
    val value = twoSeatProjection(activeParticipantId = "red-exile")
      .copy(walkerWaiting = Some(WalkerWaitingState(Vector("blue-exile"),
        Some("Choose the Oathkeeper"))))
    val active = ServerUiSupport.viewerPresentation(value, "red-exile")
    assert(!active.showGameplayControls)
    assertEquals(active.waitingForPlayerIds, Vector("blue-exile"))
    assertEquals(active.waitingForDisplayNames, Vector("Blue Exile"))

  /** The player waited on is named once: the status line carries the
    * question their parked decision asks, so the pane draws no second
    * waiting line under it.
    */
  test("the status line names who is waited on and their decision's question"):
    val value = twoSeatProjection(activeParticipantId = "red-exile")
      .copy(walkerWaiting = Some(WalkerWaitingState(Vector("blue-exile"),
        Some("Choose your starting site"))))
    val status = ActionDecisionRenderer.status(value,
      ServerUiSupport.viewerPresentation(value, "red-exile"))
    assertEquals(status.textContent,
      "Waiting for Blue Exile: Choose your starting site")
    assertEquals(status.querySelector(".player-ref").textContent, "Blue Exile")

  test("a viewer who waits sees the facts the awaited player sees"):
    val value = twoSeatProjection(activeParticipantId = "red-exile")
      .copy(walkerWaiting = Some(WalkerWaitingState(Vector("blue-exile"),
        Some("Sacrifice up to 2 warbands for one attack each"),
        status = Vector(WalkerStatusProjection("Attack total", "3"),
          WalkerStatusProjection("Defense dice", "2")))))
    val panel = ActionDecisionRenderer.actionsPanel(value,
      ServerUiSupport.viewerPresentation(value, "red-exile"),
      ParkedDecision.Routed(None, None), canControl = false,
      SessionDrafts.empty, new RecordingControls())
    assertEquals(all(panel, ".walker-status-line").map(_.textContent),
      Vector("Attack total3", "Defense dice2"))

  test("a parked Roll's wait names only the player, having no question"):
    val value = twoSeatProjection(activeParticipantId = "red-exile")
      .copy(walkerWaiting = Some(WalkerWaitingState(Vector("blue-exile"))))
    val status = ActionDecisionRenderer.status(value,
      ServerUiSupport.viewerPresentation(value, "red-exile"))
    assertEquals(status.textContent, "Waiting for Blue Exile")

  test("an inactive Act viewer waits for the active player"):
    val value = twoSeatProjection(phase = "act")
    val inactive = ServerUiSupport.viewerPresentation(value, "blue-exile")
    val active = ServerUiSupport.viewerPresentation(value, "red-exile")

    assertEquals(inactive.waitingForPlayerIds, Vector("red-exile"))
    assertEquals(active.waitingForPlayerIds, Vector.empty)

  /** Regression: the owner of a parked walker decision has
    * `waitingForPlayerIds = Vector.empty` (Task 5, tested above), but the status line
    * matched on that field alone, so this viewer fell through every named
    * phase case straight to the `activeParticipantId` debug fallback --
    * showing "setup-walker-decision; active participant: <someone else>"
    * to the very player who needs to act. Assert the decision's own heading
    * renders instead, for both the owner and an off-turn viewer.
    */
  test("a parked walker decision's owner sees its heading, not the debug fallback"):
    val site = DecisionOptionState("site", "site:ancient-city", "Ancient City")
    val pawnQuery = DecisionQueryState.ChooseOne(Vector(site),
      heading = Some("Choose your starting site"))
    val pawnDecision = WalkerDecisionState("setup", "setup.pawn-placement.blue-exile",
      query = Some(pawnQuery))
    val value = twoSeatProjection(
      phase = "setup-walker-decision",
      activeParticipantId = "red-exile",
      ready = false
    ).copy(walkerDecision = Some(pawnDecision))

    val ownerStatus = ActionDecisionRenderer.status(value,
      ServerUiSupport.viewerPresentation(value, "blue-exile"))
    assertEquals(ownerStatus.textContent, "Choose your starting site")
    assert(!ownerStatus.textContent.contains("active participant"))

  test("a parked walker decision without a heading still avoids the debug fallback"):
    val vote = DecisionOptionState("button", "yes", "Yes")
    val query = DecisionQueryState.ChooseOne(Vector(vote))
    val decision = WalkerDecisionState("setup", "setup.some-decision.blue-exile",
      query = Some(query))
    val value = twoSeatProjection(
      phase = "setup-walker-decision",
      activeParticipantId = "red-exile",
      ready = false
    ).copy(walkerDecision = Some(decision))

    val ownerStatus = ActionDecisionRenderer.status(value,
      ServerUiSupport.viewerPresentation(value, "blue-exile"))
    assertEquals(ownerStatus.textContent, "Your decision.")

  test("a parked Act menu states its heading as the active player's status"):
    // A live Act always has its menu parked, so the status is its heading.
    val query = DecisionQueryState.ChooseOne(
      Vector(DecisionOptionState("button", "end-act", "End Act and Rest")),
      heading = Some("Act: take an action, use a power or rest"))
    val value = twoSeatProjection(
      phase = "act-menu-walker-decision").copy(walkerDecision = Some(
      WalkerDecisionState("act-menu", "phase.menu",
        query = Some(query))))
    val status = ActionDecisionRenderer.status(value,
      ServerUiSupport.viewerPresentation(value, "red-exile"))
    assertEquals(status.textContent,
      "Act: take an action, use a power or rest")
