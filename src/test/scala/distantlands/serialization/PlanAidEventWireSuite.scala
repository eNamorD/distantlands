package distantlands.serialization

import distantlands.gameplay.powers.banner.YewStaffCard
import distantlands.gameplay.powers.campaign.ShieldWallCard
import distantlands.model._

/** The Imperial Tactics facts round-trip through the envelope (Empire phase
  * design, "1b-ii"). */
class PlanAidEventWireSuite extends munit.FunSuite:
  private def roundTrip(event: OathEvent): Either[Any, OathEvent] =
    GameEventWire.encodeEvent("g", 7L, event).flatMap(encoded =>
      GameEventWire.decode(encoded)).map(_.event)

  private val window = AidWindow(PlayerId("p1"), CampaignPlanSide.Defender,
    PlayerId("p3"))
  private val offer = AidOffer(
    plans = Vector(
      DecisionOptionRef.Denizen(ShieldWallCard.id),
      DecisionOptionRef.Button("title")),
    requirements = Vector(DecisionOptionRef.Banner(Banner.DarkestSecret),
      DecisionOptionRef.Relic(YewStaffCard.id),
      DecisionOptionRef.Player(PlayerId("p2"))),
    amounts = AidAmounts(favor = 2, secrets = 1, favorBurnt = 0,
      secretsBurnt = 3))

  test("every aid fact round-trips"):
    Vector(OathEvent.PlanAidSaved(PlayerId("p2"), window, offer),
      OathEvent.PlanAidSaved(PlayerId("p2"), window, AidOffer()),
      OathEvent.PlanAidCleared).foreach(event =>
      assertEquals(roundTrip(event), Right(event)))

  test("aid facts carry their own types"):
    assertEquals(GameEventWire.encodeEvent("g", 7L,
      OathEvent.PlanAidSaved(PlayerId("p2"), window, offer))
      .map(_("eventType").str), Right("plan-aid.saved"))
    assertEquals(GameEventWire.encodeEvent("g", 7L, OathEvent.PlanAidCleared)
      .map(_("eventType").str), Right("plan-aid.cleared"))

  test("an unknown plan side is refused"):
    val encoded = GameEventWire.encodeEvent("g", 7L,
      OathEvent.PlanAidSaved(PlayerId("p2"), window, offer)).toOption.get
    encoded("payload")("side") = "middle"
    assert(GameEventWire.decode(encoded).isLeft)
