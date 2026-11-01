package oathdigital.application

import oathdigital.model._
import oathdigital.protocol.{GameIntent, WalkerStartArgWire}

/** The Imperial Tactics intent binds its sender as the ally (Empire phase
  * design, "1b-ii"). */
class PlanAidIntentMapperSuite extends munit.FunSuite:
  private val red = PlayerId("red")

  test("an aid offer binds its sender as the ally"):
    assertEquals(GameIntentMapper.bind(red, GameIntent.SavePlanAid(
      Vector(WalkerStartArgWire("denizen", "108")),
      Vector(WalkerStartArgWire("banner", "peoples-favor"),
        WalkerStartArgWire("player", "red")), 2, 1, 0, 3)),
      Right(GameCommand.SavePlanAid(red, AidOffer(
        Vector(DecisionOptionRef.Denizen(DenizenId("108"))),
        Vector(DecisionOptionRef.Banner(Banner.PeoplesFavor),
          DecisionOptionRef.Player(red)),
        AidAmounts(favor = 2, secrets = 1, secretsBurnt = 3)))))

  test("an unknown ref is refused at its place in the offer"):
    assert(GameIntentMapper.bind(red, GameIntent.SavePlanAid(Vector.empty,
      Vector(WalkerStartArgWire("nonsense", "x")), 0, 0, 0, 0)) match
      case Left(GameIntentMappingFailure(path, _)) =>
        path.startsWith("$.intent.requirements[0]")
      case _ => false)

  test("a negative amount is refused"):
    assertEquals(GameIntentMapper.bind(red, GameIntent.SavePlanAid(
      Vector.empty, Vector.empty, 0, -1, 0, 0)),
      Left(GameIntentMappingFailure("$.intent",
        "aid amounts are never negative")))
