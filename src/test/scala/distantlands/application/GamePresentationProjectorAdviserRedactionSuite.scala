package distantlands.application

import distantlands.gameplay.actions.challenge.MagiciansCodeCard
import distantlands.gameplay.setup.{GameStartRules, LaterGameFixture}
import distantlands.gameplay.setup.FirstGameSetupFixture._
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted
import distantlands.protocol.projection.{CardDetailsProjection,
  PlayerBoardProjection}

/** A face-down adviser's card back is public: any player can tell a denizen
  * back from a Vision back without being told the card's identity. A viewer
  * who cannot identify the card must still be told which kind it is -- see
  * `GamePresentationProjector.hiddenCard`'s callers.
  */
class GamePresentationProjectorAdviserRedactionSuite extends munit.FunSuite:
  private val projector = new GamePresentationProjector(catalog)
  private val visionId = VisionId("vision:vision-of-conquest")

  private def assertRedacted(details: Vector[CardDetailsProjection]): Unit =
    assertEquals(details.map(_.cardKind), Vector("denizen", "vision"))
    assertEquals(details.map(_.name), Vector("Facedown denizen", "Facedown vision"))
    assert(details.forall(_.cardId == "hidden"))
    assert(details.forall(_.suit.isEmpty))
    assert(details.forall(_.rulesText.isEmpty))
    assert(details.forall(_.hidden))

  /** `player`'s board among `boards`; fails naming the boards there are. */
  private def boardOf(boards: Vector[PlayerBoardProjection], player: PlayerId)(
      using munit.Location): PlayerBoardProjection =
    boards.find(_.playerId == player.value).getOrElse(fail(
      s"no board for ${player.value} among ${boards.map(_.playerId)}"))

  test("playerBoards tells an unidentifying viewer the adviser's kind, not its identity"):
    val ready = Table.start
      .adviser(p1, MagiciansCodeCard, facedown = true)
      .adviser(p1, visionId, facedown = true)
      .ready
    val board = boardOf(projector.playerBoards(ready, Some(p2)), p1)
    assertRedacted(board.advisers)

  test("a Teeming World game names the advisers a player drew only to that " +
      "player: another player and a spectator see facedown denizens"):
    val teemingWorld = chronicle.copy(foundations = Map(FoundationNumber.IV ->
      FoundationState(FoundationFace.Altered, Set.empty)))
    val ready = accepted(GameStartRules.evolve(catalog, teemingWorld,
      LaterGameFixture.orders(teemingWorld, p1)))
    val drawn = teemingWorld.worldDeck.take(3).map(_.value)
    def p1Advisers(viewer: Option[PlayerId]) =
      boardOf(projector.playerBoards(ready, viewer), p1).advisers
    assertEquals(p1Advisers(Some(p1)).map(_.cardId), drawn)
    Vector(Some(p2), None).foreach { viewer =>
      val shown = p1Advisers(viewer)
      assertEquals(shown.map(_.cardId), Vector.fill(3)("hidden"),
        clue(viewer))
      assertEquals(shown.map(_.name), Vector.fill(3)("Facedown denizen"),
        clue(viewer))
      assert(shown.forall(_.hidden), clue(viewer))
    }

  test("playerBoards shows the tokens resting on the owner's faceup adviser"):
    val ready = Table.start.adviser(p1, MagiciansCodeCard)
      .tokens(MagiciansCodeCard, favor = 1, secrets = 2).ready
    val board = boardOf(projector.playerBoards(ready, Some(p1)), p1)
    assertEquals(board.advisers.map(card => (card.favor, card.secrets)),
      Vector((1, 2)))
