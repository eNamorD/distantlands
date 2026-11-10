package distantlands.gameplay.powers.action

import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.{NoteText, PowerFixture}
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, Table}
import distantlands.testkit.Outcomes.{assertWireRoundTrips, ready}
import distantlands.testkit.Unwrap.accepted

class WizardSchoolSuite extends munit.FunSuite:
  import ActPowerHarness._
  import PowerFixture._

  private val school = WizardSchoolCard.id
  private val source = DecisionOptionRef.Denizen(school)

  /** p1's Act at Ancient City with Wizard School (site-only) at the site
    * and `favor` favor. */
  private def staged(favor: Int = 2): ReadyGame =
    Table.start.turn(actor, Phase.Act).pawn(actor, AncientCitySite)
      .denizen(school, at = AncientCitySite).favor(actor, favor).ready

  test("it places a favor, gains a secret and ends the Act phase"):
    // 2 favor less the 1 placed leave 1.
    val ready0 = staged(favor = 2)
    val done = accepted(use(rules(), ready0, WizardSchool.id, source))
    val end = ready(done.state)
    // The power ends the Act phase. No REST power is usable, so the driver
    // finishes Rest and the turn passes, which returns the favor placed.
    val atRest = replayed(rules(), ready0, untilWalkEnds(done.events))
    assertEquals(Look(atRest).tokensOn(school), Tokens(1, 0))
    assertEquals(Look(atRest).player(actor).board.favor, 1)
    assertEquals(allSecrets(atRest), allSecrets(ready0) + 1)
    assertEquals(Look(atRest).phase, Phase.Rest)
    assertNotEquals(Look(end).active, actor)
    assertEquals(replayed(rules(), ready0, done.events), end)
    assertWireRoundTrips(done.events)

  test("it is unusable without a favor"):
    val broke = staged(favor = 0)
    assert(!usableIds(broke).contains(WizardSchool.id))
    assertEquals(use(rules(), broke, WizardSchool.id, source).left.toOption,
      Some(OathViolation.InsufficientFavor(1, 0)))

  test("it writes the gain, covering the generic line, then the phase's end"):
    val done = accepted(use(rules(), staged(), WizardSchool.id, source))
    assertEquals(NoteText.said(WizardSchool, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${actor.value} gained 1 secret.",
        covers = true),
      NoteText.Said("ended", s"${actor.value}'s Act phase ended.",
        covers = false)))
