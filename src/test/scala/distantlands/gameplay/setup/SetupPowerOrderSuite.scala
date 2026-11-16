package distantlands.gameplay.setup

import distantlands.gameplay.powerresolver.{OfferResolution, PowerCtx}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.setup.{GreatForge, GreatForgeCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{TheStandardBearer,
  TheStandardBearerCard, Wealthy, WealthyCard}
import distantlands.gameplay.walker.{ChoicePayload, LogNoted, WalkerPowers,
  WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Answers, Table}
import distantlands.testkit.Unwrap.{accepted, present}

/** The pawn window's power order with real cards (catalog batch 13,
  * rulings 4 and 5). l2, a Citizen, holds The Standard Bearer. Great Forge
  * stands intact at the first site in play, where the driver places every
  * pawn, so p1's window offers Great Forge alone and p2's offers both. */
class SetupPowerOrderSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    TheStandardBearerCard.id -> true)
  private val forge = DecisionOptionRef.Edifice(GreatForgeCard.id)
  private val bearer = DecisionOptionRef.Legacy(TheStandardBearerCard.id)
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)

  private val ready: ReadyGame =
    val started = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    Table.from(started).edifice(GreatForgeCard.id, EdificeSide.Intact,
      at = started.game.current.map.inPlay.head).ready

  private def walked(answers: Answers = PartialFunction.empty)
      : SetupWalkDriver.SetupWalked =
    SetupWalkDriver.driveWithEvents(ready, accepted(SetupProcedure.build(
      catalog, ready, ready.game.current.turn.activePlayer, Vector.empty)),
      TriggeredProcedureRef.Setup, powers, answers = answers)

  private def forgeLine(note: LogNoted, player: PlayerId): Boolean =
    note.power == GreatForge.id &&
      note.note.args.headOption.contains(NoteArg.Player(player))

  /** Where in `events` Great Forge wrote its line for `player`. */
  private def forged(events: Vector[OathEvent], player: PlayerId): Int =
    events.indexWhere {
      case note: LogNoted => forgeLine(note, player)
      case _ => false
    }

  /** Where in `events` p2 answered The Standard Bearer. */
  private def bore(events: Vector[OathEvent]): Int = events.indexWhere {
    case WalkerStepRecorded(_, ChoicePayload(id, _, _, _), _) =>
      id == TheStandardBearer.decisionId(p2)
    case _ => false
  }

  test("with two powers applying, the placer chooses which resolves " +
      "first, listed edifice first (ruling 4; R27)"):
    val asked = walked().asked.filter(_.decisionId.startsWith(
      "setup.power-order."))
    assertEquals(asked.map(_.decisionId),
      Vector(OrderedWindow.decisionId(p2, 0)))
    assertEquals(asked.head.owner, p2)
    asked.head.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector(forge, bearer))
        assertEquals(one.heading, Some(OrderedWindow.heading))
      case other => fail(s"expected a choose-one, got $other")

  test("the first option keeps R27's order: Great Forge acts before The " +
      "Standard Bearer"):
    val events = walked().events
    val (atForge, atBearer) = (forged(events, p2), bore(events))
    assert(atForge >= 0 && atBearer >= 0, s"forge $atForge, bearer $atBearer")
    assert(atForge < atBearer, s"forge $atForge, bearer $atBearer")

  test("the power chosen first resolves first, and each resolves once " +
      "(ruling 4)"):
    val events = walked({
      case park if park.decisionId == OrderedWindow.decisionId(p2, 0) =>
        ChooseOneAnswer(bearer)
    }).events
    val (atForge, atBearer) = (forged(events, p2), bore(events))
    assert(atForge >= 0 && atBearer >= 0, s"forge $atForge, bearer $atBearer")
    assert(atBearer < atForge, s"forge $atForge, bearer $atBearer")
    assertEquals(events.count {
      case note: LogNoted => forgeLine(note, p2)
      case _ => false
    }, 1)

  test("one power left resolves without a question, and its pick is " +
      "recorded"):
    val run = walked()
    assert(!run.asked.exists(_.decisionId ==
      OrderedWindow.decisionId(p1, 0)), run.asked.map(_.decisionId))
    assert(run.events.exists {
      case WalkerStepRecorded(_, ChoicePayload(id, ChooseOneAnswer(ref),
          by, _), _) =>
        id == OrderedWindow.decisionId(p1, 0) && ref == forge && by == p1
      case _ => false
    })
    assert(forged(run.events, p1) >= 0)

  test("every power at the pawn window offers its steps and folds no " +
      "transform there"):
    val hooked = WalkerPowerCatalog.default(catalog).powers.flatMap(power =>
      power.contributions.getOrElse(PowerWindow.SetupPawnPlaced,
        Vector.empty).map(power.id -> _))
    assert(hooked.map(_._1).contains(TheStandardBearer.id), hooked)
    assertEquals(hooked.filterNot(_._2.isInstanceOf[OfferResolution]),
      Vector.empty)

  test("The Standard Bearer and Wealthy read their holder at their own " +
      "step: steps offered while p2 held them do nothing once p2 does not " +
      "(ruling 5)"):
    val both = SetupLegacyFixture.holding(LaterGameFixture.chronicle(l1, l2),
      l2, Role.Citizen, TheStandardBearerCard.id -> true,
      WealthyCard.id -> true)
    val plain = LaterGameFixture.chronicle(l1, l2)
    val holding = accepted(GameStartRules.evolve(catalog, both,
      LaterGameFixture.orders(both, p1)))
    val without = accepted(GameStartRules.evolve(catalog, plain,
      LaterGameFixture.orders(plain, p1)))
    val placed = Vector(Answered(SetupProcedure.pawnDecisionId(p2),
      ChooseOneAnswer(DecisionOptionRef.Site(
        holding.game.current.map.inPlay.head)), p2))
    Vector(TheStandardBearer, Wealthy).foreach { power =>
      val ctx = PowerCtx(catalog, holding, p1, power.source,
        PowerWindow.SetupPawnPlaced, Vector.empty,
        OrderedWindow(p2, Vector.empty), answered = placed)
      val steps = present(power.contributions(PowerWindow.SetupPawnPlaced)
        .collectFirst { case resolution: OfferResolution =>
          resolution.offer(ctx) }.flatten, s"${power.id.value}'s offer").steps
      val run = SetupWalkDriver.driveWithEvents(without, Sequence(steps),
        TriggeredProcedureRef.Setup, powers)
      assertEquals(run.asked, Vector.empty[Decide], power.id.value)
      assertEquals(run.events, Vector.empty[OathEvent], power.id.value)
    }
