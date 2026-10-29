package oathdigital.application.chronicle

import oathdigital.gameplay.chronicle.EvolvedChronicleFixture
import oathdigital.gameplay.setup.FirstGameSetupFixture.chronicle
import oathdigital.model._
import oathdigital.protocol.ChronicleScore

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
    assertEquals(summary.scores, evolved.lineages.map(lineage =>
      ChronicleScore(lineage.id.value, evolved.era.score(lineage.id))))
    assert(summary.scores.exists(_.score == 3), summary.scores)

  test("a Chronicle with lineages but no scorepad has no game number"):
    val imported = EvolvedChronicleFixture.record.copy(
      era = EraState(20, Vector.empty))
    assertEquals(ChronicleEntries.summary(imported).game, None)
