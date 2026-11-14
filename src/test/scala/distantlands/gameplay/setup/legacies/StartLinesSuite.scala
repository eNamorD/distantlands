package distantlands.gameplay.setup.legacies

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.legacies.{IronHandCard, RivalCard}
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.setup.{LaterGameFixture, SetupLegacyFixture,
  SetupProcedure}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Situation

/** The lines Setup writes first for game start's legacy powers (R7). */
class StartLinesSuite extends munit.FunSuite:
  private val l1 = LineageId("l1")
  private val l2 = LineageId("l2")
  private val l3 = LineageId("l3")
  private val p1 = PlayerId("p1")

  private def lines(chronicle: Chronicle): Vector[String] =
    val situation = Situation.wake(Situation.rules(catalog), chronicle,
      LaterGameFixture.orders(chronicle, p1))
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, situation.events).map(_.text)

  test("Circle of Swords and Scepter Bearer open Setup's log"):
    val record = SetupLegacyFixture.holding(SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      CircleOfSwordsCard.id -> true), l2, Role.Citizen,
      ScepterBearerCard.id -> true)
    assertEquals(lines(record).take(2), Vector(
      s"p3 started with 8 warbands with ${CircleOfSwordsCard.id.value}",
      s"p2 started with the Grand Scepter with ${ScepterBearerCard.id.value}"))

  test("a Chancellor holding Scepter Bearer writes no line (ruling 6)"):
    val record = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l1, Role.Chancellor,
      ScepterBearerCard.id -> true)
    assert(!lines(record).exists(_.contains(ScepterBearerCard.id.value)))

  test("Iron Hand and Rival write one line per site, Iron Hand's first"):
    val record = SetupLegacyFixture.holding(
      LaterGameFixture.chronicle(l1, l2), l3, Role.Exile,
      IronHandCard.id -> true, RivalCard.id -> true)
    val expected = record.world.map(stored =>
      s"p3 placed a warband at ${stored.site.value} with " +
        IronHandCard.id.value) ++
      record.atlasBox.take(3).map(stored =>
        s"p3 placed a warband at ${stored.site.value} with " +
          RivalCard.id.value)
    assertEquals(lines(record).take(expected.size), expected)

  test("with no legacy acting Setup's log is unchanged"):
    val record = LaterGameFixture.chronicle(l1, l2)
    assert(!lines(record).exists(_.contains(" with L")))
