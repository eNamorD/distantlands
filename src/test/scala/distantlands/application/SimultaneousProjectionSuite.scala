package distantlands.application

import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.protocol.projection.WalkerWaitingProjection
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** The projections of a `Simultaneous` park (simultaneous decisions
  * design): each player who owes an answer sees their own decision, and
  * everyone else sees every such player named in seat order. */
class SimultaneousProjectionSuite extends munit.FunSuite:
  private final case class AtOnce(ready: ReadyGame,
      projector: WalkerDecisionProjector)

  /** In p1's Act, a `Simultaneous` node open to p3 and p2, children in that
    * order, sharing one heading. */
  private def parkedAtOnce(using munit.Location): AtOnce =
    def ask(owner: PlayerId) = Decide(s"test.at-once.${owner.value}", owner,
      DecisionQuery.ChooseOne(Vector(DecisionOption.Button(
        DecisionOptionRef.Button("ok"), "OK")), heading = Some("Answer")))
    val tree: Operation = Sequence(Simultaneous(Vector(ask(p3), ask(p2))))
    AtOnce(Table.start.parkedWalker(ActionRef.Recover).ready,
      new WalkerDecisionProjector(catalog,
        new GamePresentationProjector(catalog), WalkerPowers.empty,
        (_, _, _, _, _) => Right(tree)))

  test("at a Simultaneous park each owner sees their own decision, and " +
      "everyone else sees every owner named in seat order"):
    val AtOnce(ready, projector) = parkedAtOnce
    def ctx(state: ReadyGame, viewer: Option[PlayerId]) =
      ScopedProjectionContext(state, viewer)
    assertEquals(projector.project(ctx(ready, Some(p2))).map(_.decisionId),
      Some(s"test.at-once.${p2.value}"))
    assertEquals(projector.project(ctx(ready, Some(p3))).map(_.decisionId),
      Some(s"test.at-once.${p3.value}"))
    val waiting = Some(WalkerWaitingProjection(Vector(p2.value, p3.value),
      Some("Answer")))
    assertEquals(projector.waiting(ctx(ready, Some(p1))), waiting)
    assertEquals(projector.waiting(ctx(ready, None)), waiting)
    // Once p2 has answered, p2 waits on p3 like everyone else.
    val answered = ready.updateCurrent(current => current.copy(walkerPending =
      current.walkerPending.map(_.copy(answered = Vector(Answered(
        s"test.at-once.${p2.value}", DecisionAnswer.ChooseOneAnswer(
          DecisionOptionRef.Button("ok")), p2))))))
    assertEquals(projector.project(ctx(answered, Some(p2))), None)
    assertEquals(projector.waiting(ctx(answered, Some(p2))),
      Some(WalkerWaitingProjection(Vector(p3.value), Some("Answer"))))

