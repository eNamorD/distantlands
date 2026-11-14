package distantlands.gameplay.setup.legacies

import distantlands.application.GameCommand
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.{GameStartRules, LaterGameFixture,
  LegacyKeep, SetupLegacyFixture, SetupProcedure, SetupWalkDriver}
import distantlands.gameplay.powers.setup.{GreatForge, GreatForgeCard}
import distantlands.gameplay.walker.{ChoicePayload, LogNoted, WalkerPowers,
  WalkerStepRecorded}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, ParkedNode, Situation, Table}
import distantlands.testkit.Unwrap.{accepted, present}

/** The Standard Bearer: "After placing your pawn, you may take a banner
  * from the shared bank and place two more favor or secret on it from the
  * shared bank." (spec ruling 16; R27, R28). l2, a Citizen, holds it; the
  * Oath of Supremacy leaves both banners in the shared bank. */
class TheStandardBearerSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val rules = Situation.productionRules(catalog)
  private def holding(chronicle: Chronicle): Chronicle =
    SetupLegacyFixture.holding(chronicle, l2, Role.Citizen,
      TheStandardBearerCard.id -> true)
  private val record = holding(LaterGameFixture.chronicle(l1, l2))
  /** The People goal starts the People's Favor with the Chancellor. */
  private val peoplesGoal = holding(LaterGameFixture.chronicle(l1, l2)
    .copy(oathkeeperGoal = OathkeeperGoal.ThePeople))

  private def parkedAt(decisionId: String, chronicle: Chronicle = record,
      answers: Answers = PartialFunction.empty): Situation =
    Situation.start(rules.withAnswers(answers)).parkedAt(decisionId)(
      GameCommand.Begin(chronicle, LaterGameFixture.orders(chronicle, p1)))

  private def decide(situation: Situation): Decide =
    present(accepted(ParkedNode.of(situation.state, catalog,
      WalkerPowerCatalog.default(catalog))), "a park") match
      case ParkedNode.Decision(_, decide, _) => decide
      case other => fail(s"expected a decision, parked on $other")

  private def offered(situation: Situation): Vector[DecisionOptionRef] =
    decide(situation).query match
      case one: DecisionQuery.ChooseOne => one.options.map(_.ref)
      case other => fail(s"expected a choose-one, got $other")

  private def lines(situation: Situation): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.Setup),
      SetupProcedure.noteKeys, situation.events).map(_.text)

  private val taking: Answers =
    case park if park.decisionId == TheStandardBearer.decisionId(p2) =>
      ChooseOneAnswer(DecisionOptionRef.Banner(Banner.PeoplesFavor))

  test("with both banners in the bank the holder is offered Decline, the " +
      "People's Favor and the Darkest Secret, after their pawn"):
    val at = parkedAt(TheStandardBearer.decisionId(p2))
    assertEquals(decide(at).owner, p2)
    assertEquals(offered(at), Vector(TheStandardBearer.decline,
      DecisionOptionRef.Banner(Banner.PeoplesFavor),
      DecisionOptionRef.Banner(Banner.DarkestSecret)))
    assert(at.ready.game.current.pawnSite(p2).nonEmpty)

  test("a walk resumed at the question takes the banner and puts two of " +
      "its resource on it"):
    val at = parkedAt(TheStandardBearer.decisionId(p2))
    val favor = at.ready.game.current.banners.peoplesFavor.favor
    val after = at.withAnswers(taking).parkedAt(LegacyKeep.decisionId(p2))()
      .ready.game.current.banners.peoplesFavor
    assertEquals(after.holder, Some(p2))
    assertEquals(after.favor, favor + 2)

  test("the Darkest Secret takes two secrets"):
    val at = parkedAt(TheStandardBearer.decisionId(p2))
    val secrets = at.ready.game.current.banners.darkestSecret.secrets
    val after = at.withAnswers {
      case park if park.decisionId == TheStandardBearer.decisionId(p2) =>
        ChooseOneAnswer(DecisionOptionRef.Banner(Banner.DarkestSecret))
    }.parkedAt(LegacyKeep.decisionId(p2))().ready.game.current.banners
      .darkestSecret
    assertEquals(after.holder, Some(p2))
    assertEquals(after.secrets, secrets + 2)

  test("with one banner in the bank only it is offered"):
    assertEquals(offered(parkedAt(TheStandardBearer.decisionId(p2),
      peoplesGoal)), Vector(TheStandardBearer.decline,
      DecisionOptionRef.Banner(Banner.DarkestSecret)))

  test("declining changes nothing and writes no line"):
    val at = parkedAt(TheStandardBearer.decisionId(p2))
    val after = at.parkedAt(LegacyKeep.decisionId(p2))()
    assertEquals(after.ready.game.current.banners,
      at.ready.game.current.banners)
    assert(!lines(after).exists(_.contains(TheStandardBearerCard.id.value)))

  test("the line names the banner"):
    val woken = Situation.wake(rules.withAnswers(taking), record,
      LaterGameFixture.orders(record, p1))
    assert(lines(woken).contains(s"p2 took the People's Favor with " +
      TheStandardBearerCard.id.value), lines(woken))

  test("only the holder's own pawn asks, once"):
    val ready = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    val asked = SetupWalkDriver.driveWithEvents(ready, accepted(
      SetupProcedure.build(catalog, ready, ready.game.current.turn
        .activePlayer, Vector.empty)), TriggeredProcedureRef.Setup,
      WalkerPowers.selected(WalkerPowerCatalog.default(catalog),
        Vector.empty)).asked
    assertEquals(asked.filter(_.decisionId.contains("the-standard-bearer"))
      .map(_.owner), Vector(p2))

  test("with neither banner in the shared bank nothing is asked"):
    val started = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    val ready = started.updateCurrent(current => current.copy(banners =
      current.banners.copy(
        peoplesFavor = current.banners.peoplesFavor.copy(holder = Some(p1)),
        darkestSecret = current.banners.darkestSecret.copy(
          holder = Some(p1)))))
    val walked = SetupWalkDriver.driveWithEvents(ready, accepted(
      SetupProcedure.build(catalog, ready, ready.game.current.turn
        .activePlayer, Vector.empty)), TriggeredProcedureRef.Setup,
      WalkerPowers.selected(WalkerPowerCatalog.default(catalog),
        Vector.empty))
    assertEquals(walked.asked.filter(_.decisionId.contains(
      "the-standard-bearer")), Vector.empty)
    assertEquals(walked.ready.game.current.banners.peoplesFavor.holder,
      Some(p1))

  test("in the holder's pawn window Great Forge acts before The Standard " +
      "Bearer (R27)"):
    val started = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    // The driver places every pawn at the first site in play.
    val ready = Table.from(started).edifice(GreatForgeCard.id,
      EdificeSide.Intact, at = started.game.current.map.inPlay.head).ready
    val walked = SetupWalkDriver.driveWithEvents(ready, accepted(
      SetupProcedure.build(catalog, ready, ready.game.current.turn
        .activePlayer, Vector.empty)), TriggeredProcedureRef.Setup,
      WalkerPowers.selected(WalkerPowerCatalog.default(catalog),
        Vector.empty))
    val forge = walked.events.indexWhere {
      case LogNoted(GreatForge.id, note, _) =>
        note.args.headOption.contains(NoteArg.Player(p2))
      case _ => false
    }
    val bearer = walked.events.indexWhere {
      case WalkerStepRecorded(_, ChoicePayload(id, _, _), _) =>
        id == TheStandardBearer.decisionId(p2)
      case _ => false
    }
    assert(forge >= 0 && bearer >= 0, s"forge $forge, bearer $bearer")
    assert(forge < bearer, s"forge $forge, bearer $bearer")

  test("the card has left holding and its power is registered"):
    assert(LegacySetupPowers.all.contains(TheStandardBearer))
    assert(PowerImplementationStatus.implemented(
      TheStandardBearerCard.power.id))
