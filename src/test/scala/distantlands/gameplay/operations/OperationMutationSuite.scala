package distantlands.gameplay.operations

import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.powers.rest.SilverTongueCard
import distantlands.model._
import distantlands.model.TestGameFixtures._
import distantlands.testkit.{Look, RawOperationBatch, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

class OperationMutationSuite extends munit.FunSuite:
  /** TestGameFixtures' red with 1 faceup and 1 facedown secret. No Table step
    * empties the banks, so they are set empty here. */
  private val ready = Table.from(ReadyGames.of(game))
    .secrets(playerId, faceUp = 1, faceDown = 1).ready
    .copy(banks = MaterialBankState(Map.empty, Map.empty))

  test("secret planning resolves one aggregate solution independent of order"):
    val other = PlayerId("other")
    val flexible = Move(
      Piece.Secrets(1),
      PositionedLocation(Location.PlayArea(playerId)),
      PositionedLocation(Location.PlayArea(other))
    )
    val faceUpOnly = Move(
      Piece.Secrets(1),
      PositionedLocation(Location.PlayArea(playerId)),
      PositionedLocation(Location.Site(sites.head))
    )

    assertEquals(
      OperationSecretPlanner.plan(
        ready, Vector(flexible, faceUpOnly)),
      Right(Vector(
        flexible -> OperationSecretPlanner.SecretSplit(0, 1),
        faceUpOnly -> OperationSecretPlanner.SecretSplit(1, 0)
      ))
    )

  private def titled(holder: Option[PlayerId], side: TitleSide): ReadyGame =
    Table.start.oathkeeper(holder, side).ready
  private def set(ready: ReadyGame, holder: Option[PlayerId]) =
    RawOperationBatch.executeAll(ready, Vector(SetOathkeeper(holder)))
      .map(_.game.current.title)

  test("SetOathkeeper moves the title and always resets it to the Oathkeeper side"):
    assertEquals(set(titled(None, TitleSide.Oathkeeper), Some(p2)),
      Right(OathkeeperState(Some(p2), TitleSide.Oathkeeper)))
    assertEquals(set(titled(Some(p1), TitleSide.Usurper), Some(p2)),
      Right(OathkeeperState(Some(p2), TitleSide.Oathkeeper)))
    assertEquals(set(titled(Some(p1), TitleSide.Usurper), None),
      Right(OathkeeperState(None, TitleSide.Oathkeeper)))

  test("SetOathkeeper rejects leaving an Oathkeeper-side holder unchanged"):
    assertEquals(set(titled(Some(p1), TitleSide.Oathkeeper), Some(p1))
      .left.map(_.code), Left("oathkeeper-unchanged"))
    assertEquals(set(titled(None, TitleSide.Oathkeeper), None).left.map(_.code),
      Left("oathkeeper-unchanged"))

  test("SetOathkeeper turns an unchanged holder's Usurper title back"):
    // Becoming a Citizen flips the title to its Oathkeeper side (CR p. 34).
    assertEquals(set(titled(Some(p1), TitleSide.Usurper), Some(p1)),
      Right(OathkeeperState(Some(p1), TitleSide.Oathkeeper)))

  private def setGoal(from: OathkeeperGoal, to: OathkeeperGoal) =
    RawOperationBatch.executeAll(
      Table.start.oathkeeperGoal(from).ready, Vector(SetOathkeeperGoal(to)))
      .map(_.game.campaign.oathkeeperGoal)

  test("SetOathkeeperGoal changes the campaign's Oathkeeper goal"):
    assertEquals(setGoal(OathkeeperGoal.Supremacy, OathkeeperGoal.Devotion),
      Right(OathkeeperGoal.Devotion))

  test("SetOathkeeperGoal rejects leaving the goal unchanged"):
    assertEquals(setGoal(OathkeeperGoal.Protection, OathkeeperGoal.Protection)
      .left.map(_.code), Left("oathkeeper-goal-unchanged"))

  private def begin(ready: ReadyGame, operation: BeginTurn) =
    RawOperationBatch.executeAll(ready, Vector(operation))
      .map(_.game.current.turn)

  test("BeginTurn hands the turn to a seated player and clears used powers"):
    val used = PowerUseRef(PowerTiming.Rest,
      PowerSourceRef.Card(SilverTongueCard.id),
      PowerId("denizen.silver-tongue"))
    val resting = ready.updateCurrent(_.copy(turn = TurnState(playerId, Phase.Rest,
        Set(used))))
    assertEquals(begin(resting, BeginTurn(playerId, Phase.Wake)),
      Right(TurnState(playerId, Phase.Wake, Set.empty)))
    assertEquals(begin(resting, BeginTurn(playerId, Phase.RoundEnd)),
      Right(TurnState(playerId, Phase.RoundEnd, Set.empty)))

  test("BeginTurn rejects an unseated player and a phase no turn begins in"):
    assertEquals(begin(ready, BeginTurn(PlayerId("nobody"), Phase.Wake))
      .left.map(_.code), Left("unknown-player"))
    Vector(Phase.Act, Phase.Rest, Phase.WarExhaustion).foreach { phase =>
      assertEquals(begin(ready, BeginTurn(playerId, phase)).left.map(_.code),
        Left("invalid-turn-phase"), phase.key)
    }

  test("favor moves from the shared bank to a site without touching any suit bank"):
    val result = RawOperationBatch.executeAll(ready, Vector(
      Move(Piece.Favor(3), PositionedLocation(Location.SharedBank),
        PositionedLocation(Location.Site(sites.head)))))
    val moved = accepted(result)
    assertEquals(Look(moved).siteTokens(sites.head).favor,
      Look(ready).siteTokens(sites.head).favor + 3)
    assertEquals(moved.banks, ready.banks)

  test("a pawn may move from the player area to a site once, with no prior site"):
    // No Table step takes a pawn off the map.
    val unplaced = ready.updateCurrent(_.copy(players = ready.game.current.players.map {
      player => if player.player == playerId then player.copy(pawnSite = None) else player
    }))
    val result = RawOperationBatch.executeAll(unplaced, Vector(
      Move(Piece.Pawn(playerId),
        PositionedLocation(Location.PlayArea(playerId)),
        PositionedLocation(Location.Site(sites.head)))))
    assertEquals(Look(accepted(result)).player(playerId).pawnSite,
      Some(sites.head))

  test("a faceup Vision moved to a play area joins the revealed Visions"):
    // Card play discards the old Visions before the move; Desecrated
    // Ground keeps them, so the write itself must not refuse a second one.
    val held = Table.start.revealedVision(p1, VisionRules.Faith)
      .hand(p1, VisionRules.Conquest).ready
    val moved = accepted(RawOperationBatch.executeAll(held, Vector(Play(
      VisionRules.Conquest, PositionedLocation(Location.Hand(p1)),
      Location.PlayArea(p1), Orientation.FaceUp, required = true))))
    assertEquals(Look(moved).player(p1).revealedVisions.map(_.id),
      Vector[VisionId](VisionRules.Faith, VisionRules.Conquest))

  test("a pawn already on a site cannot move from the player area again"):
    val result = RawOperationBatch.executeAll(ready, Vector(
      Move(Piece.Pawn(playerId),
        PositionedLocation(Location.PlayArea(playerId)),
        PositionedLocation(Location.Site(sites.head)))))
    assertEquals(result.left.toOption, Some(OperationError.MissingPiece(Piece.Pawn(playerId),
      Location.PlayArea(playerId))))
