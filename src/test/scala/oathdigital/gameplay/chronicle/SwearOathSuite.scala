package oathdigital.gameplay.chronicle

import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.{ProcedureNotes, VisionRules}
import oathdigital.gameplay.powers.{NoteText, OathkeeperGoalChoice,
  WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Outcomes, RecordedOps, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Swear an Oath (CR p. 37). p1 won and holds the Throne. */
class SwearOathSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val empire: Table =
    ChronicleWalk.throneHeldByChancellorP1.warbandsAt(AncientCitySite, p1, 1)

  /** `empire` won by p1's Vision of Conquest, with `goal` current. */
  private def wonByConquest(goal: OathkeeperGoal): Table = empire
    .revealedVision(p1, VisionRules.Conquest).oathkeeperGoal(goal)
    .update(_.updateCurrent(_.copy(
      result = Some(GameResult(p1, VictoryKind.Visionary)))))

  private def throneStarted(table: Table)(using munit.Location)
      : OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun))

  private def lines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone),
      ChronicleThroneProcedure.noteKeys, events).map(_.text)

  test("a win by a Vision swears that Vision's goal without asking"):
    val done = ChronicleWalk.answeringEverything(rules,
      throneStarted(wonByConquest(OathkeeperGoal.Devotion)))
    assertEquals(Outcomes.ready(done.state).game.campaign.oathkeeperGoal,
      OathkeeperGoal.Supremacy)
    assert(!done.events.exists(_.toString.contains(SwearOath.goalDecisionId)),
      done.events)
    assert(lines(done.events).contains(
      "The Oathkeeper goal became Supremacy"), done.events)

  test("a win by a Vision whose goal is already current changes nothing"):
    val done = ChronicleWalk.answeringEverything(rules,
      throneStarted(wonByConquest(OathkeeperGoal.Supremacy)))
    assert(!RecordedOps.all(done.events).exists(_.isInstanceOf[
      SetOathkeeperGoal]), done.events)

  test("otherwise the Throne holder chooses a goal other than the current one"):
    val at = ChronicleWalk.walkedTo(rules, throneStarted(empire),
      SwearOath.goalDecisionId)
    val parked = ChronicleWalk.parked(at.state).get
    assertEquals(parked.owner, p1)
    val current = Outcomes.ready(at.state).game.campaign.oathkeeperGoal
    parked.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.filterNot(_.available).map(_.ref),
          Vector[DecisionOptionRef](OathkeeperGoalChoice.ref(current)))
      case other => fail(other.toString)
    val chosen = OathkeeperGoal.all.find(_ != current).get
    val next = accepted(rules.resolveWalker(at.state, p1,
      SwearOath.goalDecisionId, DecisionAnswer.ChooseOneAnswer(
        OathkeeperGoalChoice.ref(chosen))))
    assertEquals(Outcomes.ready(next.state).game.campaign.oathkeeperGoal,
      chosen)
    assert(lines(next.events).contains("The Oathkeeper goal became " +
      OathkeeperGoalChoice.named(chosen)), next.events)
