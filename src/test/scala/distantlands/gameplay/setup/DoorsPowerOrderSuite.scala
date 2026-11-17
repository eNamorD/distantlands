package distantlands.gameplay.setup

import distantlands.gameplay.chronicle.legacies.{RivalCard, SteadfastCard}
import distantlands.gameplay.phases.rest.ArbiterCard
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.economy.BelovedCard
import distantlands.gameplay.powers.legacies.{GoldenKeyCard, RuthlessCard}
import distantlands.gameplay.powers.setup.{AmberDoors, AmberDoorsCard,
  LostDoors}
import distantlands.gameplay.powers.travel.PathfinderCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.{TheStandardBearer,
  TheStandardBearerCard, Wealthy, WealthyCard}
import distantlands.gameplay.walker.{ChoicePayload, LogNoted, WalkerPowers,
  WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{DecisionAnswers, Table}
import distantlands.testkit.Unwrap.accepted

/** Amber Doors in Setup's ordered pawn window (catalog batch 13, rulings
  * 4, 5 and 12). l2, a Citizen, holds Wealthy active. Amber Doors stands
  * intact at the first site in play, where the driver places every pawn.
  * The legacy deck holds, from the top, the three keeps' draws (Rival and
  * Arbiter for p1, Ruthless and Golden Key for p2, Beloved and Pathfinder
  * for p3), then The Standard Bearer and Steadfast, which Amber Doors draws
  * for p2. p1 keeps Rival, p2 Ruthless and p3 Beloved, the driver's first
  * option; p1 declines Amber Doors, also its first option. */
class DoorsPowerOrderSuite extends munit.FunSuite:
  private val (l1, l2) = (LineageId("l1"), LineageId("l2"))
  private val (p1, p2) = (PlayerId("p1"), PlayerId("p2"))
  private val record = SetupLegacyFixture.holding(
    LaterGameFixture.chronicle(l1, l2), l2, Role.Citizen,
    WealthyCard.id -> true)
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)
  private val doors = DecisionOptionRef.Edifice(AmberDoorsCard.id)
  private val wealthy = DecisionOptionRef.Legacy(WealthyCard.id)

  private val ready: ReadyGame = doorsOn(EdificeSide.Intact)

  /** The later game's start with E05 on `side` at the first site in play,
    * and the legacy deck above. */
  private def doorsOn(side: EdificeSide): ReadyGame =
    val started = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    Table.from(started).edifice(AmberDoorsCard.id, side,
      at = started.game.current.map.inPlay.head)
      .legacyDeck(RivalCard, ArbiterCard, RuthlessCard, GoldenKeyCard,
        BelovedCard, PathfinderCard, TheStandardBearerCard, SteadfastCard)
      .ready

  /** The Setup walk with p2 resolving `first` first, discarding at Amber
    * Doors and activating `activated`. */
  private def walked(first: DecisionOptionRef, activated: LegacyId)
      : SetupWalkDriver.SetupWalked =
    SetupWalkDriver.driveWithEvents(ready, accepted(SetupProcedure.build(
      catalog, ready, ready.game.current.turn.activePlayer, Vector.empty)),
      TriggeredProcedureRef.Setup, powers, answers = {
        case park if park.decisionId == LegacyKeep.decisionId(p1) =>
          DecisionAnswers.pickOfferedLegacy(park.decide.query,
            RivalCard.id)
        case park if park.decisionId == LegacyKeep.decisionId(p2) =>
          DecisionAnswers.pickOfferedLegacy(park.decide.query,
            RuthlessCard.id)
        case park if park.decisionId == OrderedWindow.decisionId(p2, 0) =>
          ChooseOneAnswer(first)
        case park if park.decisionId == AmberDoors.useDecisionId(Some(p2)) =>
          ChooseOneAnswer(AmberDoors.discard)
        case park
            if park.decisionId == AmberDoors.activateDecisionId(Some(p2)) =>
          DecisionAnswers.pickOfferedLegacy(park.decide.query,
            activated)
      })

  private def l2Legacies(run: SetupWalkDriver.SetupWalked)
      : Vector[LegacyState] = run.ready.game.campaign.lineages(l2).legacies

  /** Where in `events` Wealthy wrote its line, or -1. */
  private def wealthyLine(events: Vector[OathEvent]): Int =
    events.indexWhere {
      case note: LogNoted => Set(Wealthy.tookFromPlayers.name,
        Wealthy.tookFromBank.name).contains(note.note.key)
      case _ => false
    }

  /** Where in `events` Amber Doors wrote p2's discard line, or -1. */
  private def doorsDiscardLine(events: Vector[OathEvent]): Int =
    events.indexWhere {
      case note: LogNoted => note.power == AmberDoors.id &&
        note.note.key == AmberDoors.discardedAll.name &&
        note.note.args.headOption.contains(NoteArg.Player(p2))
      case _ => false
    }

  test("p2 is asked to order Amber Doors and Wealthy, the edifice first"):
    val asked = walked(doors, SteadfastCard.id).asked
      .filter(_.decisionId == OrderedWindow.decisionId(p2, 0))
    assertEquals(asked.size, 1)
    asked.head.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref), Vector(doors, wealthy))
      case other => fail(s"expected a choose-one, got $other")

  test("Amber Doors first discards Wealthy, so Wealthy does not resolve " +
      "(ruling 5)"):
    val run = walked(doors, SteadfastCard.id)
    assert(doorsDiscardLine(run.events) >= 0, run.events)
    assertEquals(wealthyLine(run.events), -1)
    assertEquals(l2Legacies(run), Vector(
      LegacyState(SteadfastCard.id, active = true),
      LegacyState(TheStandardBearerCard.id, active = false)))
    assert(run.ready.game.current.commonCards.legacyDeck
      .contains(WealthyCard.id))

  test("Wealthy chosen first resolves before Amber Doors discards it"):
    val run = walked(wealthy, SteadfastCard.id)
    val (atWealthy, atDoors) =
      (wealthyLine(run.events), doorsDiscardLine(run.events))
    assert(atWealthy >= 0 && atDoors >= 0, s"$atWealthy, $atDoors")
    assert(atWealthy < atDoors, s"$atWealthy, $atDoors")
    assertEquals(run.events.count {
      case note: LogNoted => Set(Wealthy.tookFromPlayers.name,
        Wealthy.tookFromBank.name).contains(note.note.key)
      case _ => false
    }, 1)

  test("a Standard Bearer that Amber Doors activated resolves in the same " +
      "window, picked alone (rulings 4 and 5)"):
    val run = walked(doors, TheStandardBearerCard.id)
    assert(run.asked.exists(_.decisionId ==
      TheStandardBearer.decisionId(p2)), run.asked.map(_.decisionId))
    assert(!run.asked.exists(_.decisionId ==
      OrderedWindow.decisionId(p2, 1)), run.asked.map(_.decisionId))
    assert(run.events.exists {
      case WalkerStepRecorded(_, ChoicePayload(id, ChooseOneAnswer(ref),
          _, true), _) =>
        id == OrderedWindow.decisionId(p2, 1) &&
          ref == DecisionOptionRef.Legacy(TheStandardBearerCard.id)
      case _ => false
    }, run.events)

  test("two players placing at Lost Doors each answer their own discard, " +
      "under their own id"):
    val lost = doorsOn(EdificeSide.Ruined)
    val (p1Discard, p2Discard) = (LostDoors.discardDecisionId(Some(p1)),
      LostDoors.discardDecisionId(Some(p2)))
    val run = SetupWalkDriver.driveWithEvents(lost, accepted(
      SetupProcedure.build(catalog, lost, lost.game.current.turn.activePlayer,
        Vector.empty)), TriggeredProcedureRef.Setup, powers, answers = {
        case park if park.decisionId == LegacyKeep.decisionId(p1) =>
          DecisionAnswers.pickOfferedLegacy(park.decide.query,
            RivalCard.id)
        case park if park.decisionId == LegacyKeep.decisionId(p2) =>
          DecisionAnswers.pickOfferedLegacy(park.decide.query,
            RuthlessCard.id)
        case park if park.decisionId == p1Discard =>
          DecisionAnswers.pickOfferedLegacy(park.decide.query,
            TheStandardBearerCard.id)
        case park if park.decisionId == p2Discard =>
          DecisionAnswers.pickOfferedLegacy(park.decide.query,
            RuthlessCard.id)
      })
    val discards = run.asked.filter(decide =>
      Set(p1Discard, p2Discard).contains(decide.decisionId))
    assertEquals(discards.map(decide => decide.decisionId -> decide.owner),
      Vector(p1Discard -> p1, p2Discard -> p2))
    assertEquals(run.ready.game.campaign.lineages(l1).legacies,
      Vector(LegacyState(RivalCard.id, active = false)))
    assertEquals(l2Legacies(run), Vector(
      LegacyState(WealthyCard.id, active = true),
      LegacyState(SteadfastCard.id, active = false)))
