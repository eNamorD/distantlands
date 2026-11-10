package distantlands.application

import distantlands.gameplay.powers.campaign.ShieldWallCard
import distantlands.model._
import distantlands.protocol.{GameIntent, WalkerStartArgWire}

/** The Imperial Tactics intent maps to the offer it carries (Empire phase
  * design, "1b-ii"); the transport binds its sender as the ally. */
class PlanAidIntentMapperSuite extends munit.FunSuite:
  test("an aid offer maps to the offer it carries"):
    assertEquals(GameIntentMapper.map(GameIntent.SavePlanAid(
      Vector(WalkerStartArgWire("denizen", ShieldWallCard.id.value)),
      Vector(WalkerStartArgWire("banner", "peoples-favor"),
        WalkerStartArgWire("player", "red")), 2, 1, 0, 3)),
      Right(PlayerIntent.SavePlanAid(AidOffer(
        Vector(DecisionOptionRef.Denizen(ShieldWallCard.id)),
        Vector(DecisionOptionRef.Banner(Banner.PeoplesFavor),
          DecisionOptionRef.Player(PlayerId("red"))),
        AidAmounts(favor = 2, secrets = 1, secretsBurnt = 3)))))

  test("an unknown ref is refused at its place in the offer"):
    assert(GameIntentMapper.map(GameIntent.SavePlanAid(Vector.empty,
      Vector(WalkerStartArgWire("nonsense", "x")), 0, 0, 0, 0)) match
      case Left(GameIntentMappingFailure(path, _)) =>
        path.startsWith("$.intent.requirements[0]")
      case _ => false)

  test("a negative amount is refused"):
    assertEquals(GameIntentMapper.map(GameIntent.SavePlanAid(
      Vector.empty, Vector.empty, 0, -1, 0, 0)),
      Left(GameIntentMappingFailure("$.intent",
        "aid amounts are never negative")))
