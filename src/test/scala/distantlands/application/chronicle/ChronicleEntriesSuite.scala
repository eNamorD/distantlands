package distantlands.application.chronicle

import distantlands.gameplay.chronicle.EvolvedChronicleFixture
import distantlands.gameplay.setup.FirstGameSetupFixture.chronicle
import distantlands.model._
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.gameplay.chronicle.legacies.IronHandCard
import distantlands.protocol.{ChronicleFoundation, ChronicleLegacy}

/** Digest and summary (Chronicle browsing design, "Digest" and "Summary"). */
class ChronicleEntriesSuite extends munit.FunSuite:
  private val entries = ChronicleEntries()

  test("the digest is the SHA-256 of the text, in lowercase hex"):
    assertEquals(ChronicleEntries.digest("abc"),
      "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")

  test("an entry's digest is stable across a decode and encode"):
    val entry = entries.entry(chronicle)
    val reread = entries.json.decode(entry.json).fold(fail(_), identity)
    assertEquals(entries.entry(reread), entry)
    assertEquals(entry.digest, ChronicleEntries.digest(entry.json))

  test("a first-game Chronicle's summary is game 1 with no scores"):
    val summary = ChronicleEntries.summary(chronicle)
    assertEquals(summary.name, "Fixture Chronicle")
    assertEquals(summary.game, Some(1))
    assertEquals(summary.eraTarget, 20)
    assertEquals(summary.scores, Vector.empty)

  test("an evolved Chronicle's summary is game 2 with each lineage's score"):
    val evolved = EvolvedChronicleFixture.record
    val summary = ChronicleEntries.summary(evolved)
    assertEquals(summary.game, Some(2))
    assertEquals(summary.scores.map(score => score.lineage -> score.score),
      evolved.lineages.map(lineage =>
        lineage.id.value -> evolved.era.score(lineage.id)))
    assert(summary.scores.exists(_.score == 3), summary.scores)

  test("a Chronicle with lineages but no scorepad has no game number"):
    val imported = EvolvedChronicleFixture.record.copy(
      era = EraState(20, Vector.empty))
    assertEquals(ChronicleEntries.summary(imported).game, None)

  test("a summary records each lineage's Imperial role, and none for an " +
      "Exile"):
    def lineage(id: String, role: Role) =
      LineageState(LineageId(id), None, role, Vector.empty, Vector.empty)
    val summary = ChronicleEntries.summary(chronicle.copy(lineages = Vector(
      lineage("red-lineage", Role.Chancellor),
      lineage("blue-lineage", Role.Citizen),
      lineage("yellow-lineage", Role.Exile))))
    assertEquals(summary.scores.map(_.role),
      Vector(Some("chancellor"), Some("citizen"), None))

  test("a summary lists the six foundations by the face the next game " +
      "starts on, a number the Chronicle does not list as normal"):
    val summary = ChronicleEntries.summary(chronicle.copy(foundations = Map(
      FoundationNumber.III -> FoundationState(FoundationFace.Altered,
        Set.empty))))
    assertEquals(summary.foundations, Vector(
      ChronicleFoundation(1, "normal", "Imperial Maps"),
      ChronicleFoundation(2, "normal", "Powerful Tribes"),
      ChronicleFoundation(3, "altered", "Public Ambitions"),
      ChronicleFoundation(4, "normal", "Ties of Fellowship"),
      ChronicleFoundation(5, "normal", "Mob's Favor"),
      ChronicleFoundation(6, "normal", "Wandering Flame")))

  test("each lineage's row names its legacies, active or dormant, in held " +
      "order (R39)"):
    val held = chronicle.copy(lineages = Vector(LineageState(LineageId("l1"),
      None, Role.Exile, Vector(LegacyState(SeerCard.id, active = true),
        LegacyState(IronHandCard.id, active = false)), Vector.empty)))
    assertEquals(ChronicleEntries.summary(held).scores.map(_.legacies),
      Vector(Vector(ChronicleLegacy(SeerCard.name, active = true),
        ChronicleLegacy(IronHandCard.name, active = false))))
