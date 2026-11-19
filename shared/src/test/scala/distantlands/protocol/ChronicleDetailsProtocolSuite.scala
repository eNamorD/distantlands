package distantlands.protocol

import distantlands.protocol.projection._
import distantlands.testkit.Unwrap

/** The Chronicle Details wire (Chronicle Details design, slice 1): every
  * key is required; a role and the Atlas's Recent site are null when
  * absent. */
class ChronicleDetailsProtocolSuite extends munit.FunSuite:
  private val faceupLegacy = CardDetailsProjection("L22", "legacy", "Seer",
    orientation = Some("face-up"))
  private val legacyBack = CardDetailsProjection("hidden", "legacy",
    "Facedown legacy", orientation = Some("face-down"), hidden = true)
  private val relicBack = CardDetailsProjection("hidden", "relic",
    "Facedown relic", orientation = Some("face-down"), hidden = true)
  private val deepWoodsWithADenizenAndARelic = SetupSiteProjection(
    "site:deep-woods", "Deep Woods", 0, 0, 3, 1,
    Vector(SiteCardProjection("208", "Wizard's Conclave",
      Some(CardDetailsProjection("208", "denizen", "Wizard's Conclave",
        orientation = Some("face-up"))))),
    SiteRelicsProjection(Vector(relicBack)))

  /** Two games on the scorepad, a Citizen and an Exile, and a Recent site
    * with a denizen and a relic. */
  private val detailsAfterTwoGames = ChronicleDetailsProjection(20,
    Vector(
      Vector(ChronicleScorepadEntry("red-lineage", 3, Some("chancellor")),
        ChronicleScorepadEntry("blue-lineage", 2)),
      Vector(ChronicleScorepadEntry("red-lineage", 5, Some("citizen")),
        ChronicleScorepadEntry("blue-lineage", 6))),
    Vector(
      LineageDetailsProjection("red-lineage", 5, Some("citizen"),
        Vector(faceupLegacy, legacyBack)),
      LineageDetailsProjection("blue-lineage", 6, None, Vector.empty)),
    Some(deepWoodsWithADenizenAndARelic))

  private def snapshotWith(details: Option[ChronicleDetailsProjection])
      : GameProjection =
    GameProjection("game-1", 7, "act", Some("red"), Vector.empty,
      Vector.empty, Vector.empty, ready = true, completed = false,
      chronicleDetails = details)

  private def assertRoundTrips(candidate: GameProjection)(
      using munit.Location): Unit =
    assertEquals(
      GameProjectionCodec.decode(GameProjectionCodec.encode(candidate)),
      Right(candidate))

  /** The encoded snapshot with `edit` applied, decoded. */
  private def decodedAfter(candidate: GameProjection)(
      edit: ujson.Value => Unit) =
    val raw = ujson.read(GameProjectionCodec.encode(candidate))
    edit(raw)
    GameProjectionCodec.decode(ujson.write(raw))

  test("Chronicle Details round-trips with its scorepad, its lineages and " +
      "the Recent site"):
    assertRoundTrips(snapshotWith(Some(detailsAfterTwoGames)))

  test("an empty Atlas, an empty scorepad and a snapshot before a game " +
      "round-trip"):
    assertRoundTrips(snapshotWith(Some(detailsAfterTwoGames.copy(
      scorepad = Vector.empty, atlasRecent = None))))
    assertRoundTrips(snapshotWith(None))

  test("a snapshot without chronicleDetails is refused"):
    val failure = Unwrap.rejected(decodedAfter(snapshotWith(
      Some(detailsAfterTwoGames)))(_.obj.remove("chronicleDetails")))
    assertEquals(failure.path, "$.chronicleDetails")

  test("a lineage without its role key, or a Chronicle Details without " +
      "its Recent site key, is refused"):
    val snapshot = snapshotWith(Some(detailsAfterTwoGames))
    assertEquals(Unwrap.rejected(decodedAfter(snapshot)(
      _("chronicleDetails")("lineages")(1).obj.remove("role"))).path,
      "$.chronicleDetails.lineages[1].role")
    assertEquals(Unwrap.rejected(decodedAfter(snapshot)(
      _("chronicleDetails").obj.remove("atlasRecent"))).path,
      "$.chronicleDetails.atlasRecent")

  test("a lineage role other than chancellor or citizen is refused"):
    val failure = Unwrap.rejected(decodedAfter(snapshotWith(
      Some(detailsAfterTwoGames)))(raw =>
      raw("chronicleDetails")("lineages")(0)("role") = "exile"))
    assertEquals(failure.path, "$.chronicleDetails.lineages[0].role")
