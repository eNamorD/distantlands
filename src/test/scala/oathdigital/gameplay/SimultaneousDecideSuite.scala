package oathdigital.gameplay

import oathdigital.gameplay.walker.{ProcedureWalker, WalkerOutcome, WalkerPowers}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.model.OathState.Ready
import oathdigital.model.TestGameFixtures._

/** Two decisions open at once, answered in any order: the `Simultaneous`
  * node (simultaneous decisions design), exercised with a synthetic tree.
  * Each child's tail adds one Imperial warband to the bank, so a test can
  * see which tails ran. */
class SimultaneousDecideSuite extends munit.FunSuite:
  private val red = playerId
  private val blue = PlayerId("player-blue")
  private val noPowers = WalkerPowers.empty
  private val yes = DecisionOptionRef.Button("yes")
  /** The fixture has no Imperial supply; the tails add to one. */
  private val ready: ReadyGame =
    val base = ReadyGames.of(game)
    base.copy(banks = base.banks.copy(warbandSupply =
      base.banks.warbandSupply + (ForceKind.Imperial -> 0)))

  private def ask(id: String, owner: PlayerId, heading: String = "Answer") =
    Decide(id, owner, DecisionQuery.ChooseOne(Vector(
      DecisionOption.Button(yes, "Yes"),
      DecisionOption.Button(DecisionOptionRef.Button("no"), "No")),
      heading = Some(heading)))

  private val redAsk = ask("sim.red", red)
  private val blueAsk = ask("sim.blue", blue)
  private val addsWarband = BuildOps((_, _) =>
    Right(Vector[CoreOperation](WarbandsAdded(ForceKind.Imperial, 1))))
  private val afterwards = ask("sim.after", red, "Afterwards")

  /** The children, then a decision after the node: the node is child 0. */
  private def tree(children: Operation*): Operation =
    Sequence(Simultaneous(children.toVector), afterwards)

  private val both = tree(Sequence(redAsk, addsWarband),
    Sequence(blueAsk, addsWarband))

  private def answer(decide: Decide, by: PlayerId): Answered =
    Answered(decide.decisionId, ChooseOneAnswer(yes), by)

  /** The park, and `from` with the walk's recorded steps replayed onto it.
    * Replay checks each answer step against the park it resumed, `at`. */
  private def parkedAt(from: ReadyGame, at: Option[PendingTree],
      outcome: Either[OathViolation, WalkerOutcome])(using munit.Location)
      : (PendingTree, ReadyGame) = outcome match
    case Right(WalkerOutcome.Parked(pending, events)) =>
      val start = from.updateCurrent(_.copy(walkerPending = at))
      val after = events.collect { case event: WalkerEvent => event }
        .foldLeft[Either[OathViolation, OathState]](Right(Ready(start)))(
          (state, event) => state.flatMap(
            ProcedureWalker.applyRecorded(_, event)))
      after match
        case Right(Ready(next)) =>
          (pending, next.updateCurrent(_.copy(walkerPending = None)))
        case other => fail(s"could not apply the walk's steps: $other")
    case other => fail(s"expected a park, got $other")

  private def fresh(operation: Operation)(using munit.Location)
      : (PendingTree, ReadyGame) =
    parkedAt(ready, None, ProcedureWalker.advance(ready, operation, None,
      noPowers))

  private def imperialBank(state: ReadyGame): Int =
    state.banks.warbandSupply.getOrElse(ForceKind.Imperial, 0)

  test("the node parks at its own path with every child open"):
    val (pending, _) = fresh(both)
    assertEquals(pending.at, Vector("0"))
    assertEquals(ProcedureWalker.openDecisions(ready, both, pending,
      noPowers), Vector(redAsk, blueAsk))
    assertEquals(ProcedureWalker.awaitedPlayers(ready, both, pending,
      noPowers), Set(red, blue))
    assertEquals(ProcedureWalker.parkedDecide(ready, both, pending,
      noPowers), None)

  test("awaitedInSeatOrder follows the seats, not the children"):
    val reversed = tree(Sequence(blueAsk, addsWarband),
      Sequence(redAsk, addsWarband))
    val (pending, _) = fresh(reversed)
    val seats = ready.game.current.players.map(_.player)
    assertEquals(ProcedureWalker.awaitedInSeatOrder(ready, reversed, pending,
      noPowers), seats.filter(Set(red, blue)))

  test("children are answered in any order; each tail runs at its answer"):
    val (first, _) = fresh(both)
    val (second, afterBlue) = parkedAt(ready, Some(first),
      ProcedureWalker.resolve(ready, both, first, answer(blueAsk, blue), noPowers))
    assertEquals(second.at, Vector("0"))
    assertEquals(imperialBank(afterBlue), imperialBank(ready) + 1)
    assertEquals(ProcedureWalker.openDecisions(afterBlue, both, second,
      noPowers), Vector(redAsk))
    assertEquals(ProcedureWalker.parkedDecide(afterBlue, both, second,
      noPowers), Some(redAsk))
    val (third, afterRed) = parkedAt(afterBlue, Some(second),
      ProcedureWalker.resolve(afterBlue, both, second, answer(redAsk, red), noPowers))
    // The node completed, and the walk went on to the decision after it.
    assertEquals(third.at, Vector("1"))
    assertEquals(imperialBank(afterRed), imperialBank(ready) + 2)
    assertEquals(third.answered.map(_.decisionId),
      Vector("sim.blue", "sim.red"))

  test("a plain advance at the node re-parks it"):
    val (pending, _) = fresh(both)
    val (again, _) = parkedAt(ready, Some(pending),
      ProcedureWalker.advance(ready, both, Some(pending), noPowers))
    assertEquals(again, pending)

  test("a child answered twice, or an unknown id, is rejected"):
    val (first, _) = fresh(both)
    val (second, afterBlue) = parkedAt(ready, Some(first),
      ProcedureWalker.resolve(ready, both, first, answer(blueAsk, blue), noPowers))
    assert(ProcedureWalker.resolve(afterBlue, both, second,
      answer(blueAsk, blue), noPowers).isLeft)
    assert(ProcedureWalker.resolve(ready, both, first,
      Answered("sim.unknown", ChooseOneAnswer(yes), red), noPowers).isLeft)

  test("a player who does not own the child is rejected"):
    val (first, _) = fresh(both)
    assertEquals(ProcedureWalker.resolve(ready, both, first,
      answer(redAsk, blue), noPowers),
      Left(OathViolation.WrongPlayer(red, blue)))

  test("a child whose tail parks is rejected"):
    val parking = tree(Sequence(redAsk, ask("sim.more", red)),
      Sequence(blueAsk, addsWarband))
    val (first, _) = fresh(parking)
    assert(ProcedureWalker.resolve(ready, parking, first,
      answer(redAsk, red), noPowers).isLeft)

  test("a malformed node is rejected when the walk reaches it"):
    val malformed = Vector(
      "no children" -> tree(),
      "a child with no Decide first" -> tree(Sequence(addsWarband, redAsk)),
      "a shared decision id" -> tree(redAsk, blueAsk.copy(decisionId =
        redAsk.decisionId)),
      "a shared owner" -> tree(redAsk, blueAsk.copy(owner = red)),
      "co-owners" -> tree(redAsk.copy(coOwners = Vector(blue)), blueAsk),
      "a windowed Decide" -> tree(redAsk.copy(window =
        Some(PowerWindow.PayOrAsk)), blueAsk),
      "differing headings" -> tree(redAsk, ask("sim.blue", blue, "Other")))
    malformed.foreach((label, operation) => assert(
      ProcedureWalker.advance(ready, operation, None, noPowers).isLeft,
      label))
