package distantlands.gameplay.setup

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.walker.{LogNoted, WalkerPowers}
import distantlands.model._
import distantlands.testkit.Situation

/** Setup's windows (Legacy Setup powers design, "Walker steps"): a power
  * folded into each runs where the design places it. */
class SetupWindowsSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))

  private case object MarkBanks extends ContributingPower:
    val id: PowerId = PowerId("test.setup.mark-banks")
    val source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    private val notes = NoteSet(ProcedureNotes.id(TriggeredProcedureRef.Setup))
    val marked: NoteKey = notes.key("test-banks",
      Vector(NotePart.Text("banks")))
    val markedDeck: NoteKey = notes.key("test-world-deck",
      Vector(NotePart.Text("world deck")))
    private def mark(key: NoteKey): Vector[Contribution] =
      Vector(Transform((_, ops) => ops :+ Note(
        ProcedureNotes.id(TriggeredProcedureRef.Setup),
        _ => Some(key.sourceless()))))
    val contributions: Map[PowerWindow, Vector[Contribution]] = Map(
      PowerWindow.SetupBanks -> mark(marked),
      PowerWindow.SetupWorldDeck -> mark(markedDeck))

  private def keys(chronicle: Chronicle): Vector[String] =
    Situation.wake(Situation.rules(catalog, WalkerPowers(Vector(MarkBanks))),
      chronicle, LaterGameFixture.orders(chronicle, PlayerId("p1")))
      .events.collect { case LogNoted(_, note, _) => note.key }

  test("SetupBanks runs once, after Imperial Maps and before the first " +
      "pawn"):
    val said = keys(LaterGameFixture.chronicle(l1, l2))
    assertEquals(said.count(_ == "test-banks"), 1)
    assert(said.lastIndexOf("placed-site") < said.indexOf("test-banks"), said)
    assert(said.indexOf("test-banks") < said.indexOf("placed-pawn"), said)

  test("SetupBanks runs where Imperial Maps would, under Wide Horizons"):
    val record = LaterGameFixture.chronicle(l1, l2)
    val said = keys(record.copy(foundations = record.foundations.updated(
      FoundationNumber.I, FoundationState(FoundationFace.Altered, Set.empty))))
    assertEquals(said.count(_ == "test-banks"), 1)
    assert(said.indexOf("test-banks") < said.indexOf("placed-pawn"), said)

  test("SetupWorldDeck runs once, after SetupBanks and before the first " +
      "pawn"):
    val said = keys(LaterGameFixture.chronicle(l1, l2))
    assertEquals(said.count(_ == "test-world-deck"), 1)
    assert(said.indexOf("test-banks") < said.indexOf("test-world-deck"), said)
    assert(said.indexOf("test-world-deck") < said.indexOf("placed-pawn"),
      said)

  test("Setup window keys stay as power data spells them"):
    assertEquals(PowerWindow.SetupBanks.key, "setup.banks")
    assertEquals(PowerWindow.SetupWorldDeck.key, "setup.world-deck")
