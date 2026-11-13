package distantlands.frontend

import distantlands.frontend.Dom.{all, firstMatch}
import distantlands.frontend.TestBoards.*
import distantlands.model.PlayerColor
import distantlands.protocol.projection.LegacyGoalProjection

/** The player areas' legacy strips and the World pane's Public Ambitions row
  * (Legacies design, slice 5; implementation rulings R36-R38), drawn from a
  * projection. */
class LegacyStripSuite extends munit.FunSuite:
  private val redExile = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val seer = CardDetails("L22", "legacy", "Seer", suit = Some("arcane"),
    orientation = Some("face-up"),
    legacyGoal = Some(LegacyGoalProjection("darkest-secret",
      "Hold the Darkest Secret.")))
  private val arbiter = seer.copy(cardId = "L23", name = "Arbiter",
    suit = Some("discord"), orientation = Some("face-down"))
  private val back = CardDetails("hidden", "legacy", "Legacy",
    orientation = Some("face-down"), hidden = true)

  private def players(legacies: CardDetails*) =
    WorldBoardRenderer.playerBoards(
      TestProjections.actingProjection(players = Vector(redExile))
        .copy(playerBoards = Vector(quietBoard("red")
          .copy(legacies = legacies.toVector))), "red")

  test("a player's legacies sit in a row of their own, below the cards in " +
      "play (R37)"):
    val node = players(seer, arbiter)
    val strip = firstMatch(node, ".board-legacies")
    assertEquals(strip.map(_.getAttribute("aria-label")), Some("Legacies"))
    assertEquals(all(node, ".board-legacies .legacy-chip-name")
      .map(_.textContent), Vector("Seer", "Arbiter"))
    assertEquals(all(node, ".board-legacies .legacy-chip")
      .map(_.getAttribute("data-tip")), Vector("Hold the Darkest Secret.",
        "Hold the Darkest Secret."))
    assertEquals(all(node, ".board-legacies .legacy-dormant")
      .map(_.textContent), Vector("Dormant"))
    assertEquals(all(node, ".board-cards .card-name").map(_.textContent)
      .filter(Set("Seer", "Arbiter")), Vector.empty)

  test("another seat's dormant legacy is an L back (R36)"):
    val node = players(seer, back)
    assertEquals(all(node, ".board-legacies .card-back-letter")
      .map(_.textContent), Vector("L"))

  test("a player with no legacy has no strip"):
    assertEquals(firstMatch(players(), ".board-legacies"), None)

  test("the World pane heads its faceup Public Ambitions row, and has none " +
      "while the row is empty (R38)"):
    val table = TestProjections.actingProjection(players = Vector(redExile))
    val row = WorldBoardRenderer.world(table.copy(publicLegacies =
      Vector(seer)), BoardOptions.none)
    assertEquals(firstMatch(row, ".public-legacies h3").map(_.textContent),
      Some("Public Ambitions"))
    assertEquals(all(row, ".public-legacies .card-name").map(_.textContent),
      Vector("Seer"))
    assertEquals(firstMatch(WorldBoardRenderer.world(table, BoardOptions.none),
      ".public-legacies"), None)
