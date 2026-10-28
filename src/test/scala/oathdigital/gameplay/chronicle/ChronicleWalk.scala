package oathdigital.gameplay.chronicle

import oathdigital.gameplay.OathRules
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{ParkedNode, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Shared steps for Chronicle task tests: the table a task starts from, and
  * a walk that answers every parked Chronicle decision with its plainest
  * answer. */
object ChronicleWalk:
  /** The Chronicle phase of a game p1 won, with the Begin decision already
    * answered: the driver's next step after `ChronicleBegin` starts the Sun.
    */
  def chronicleWonByP1: Table = Table.start
    .turn(p1, Phase.Chronicle)
    .update(_.updateCurrent(_.copy(result = Some(GameResult(p1)))))

  /** The decision `state` is parked on, if any. */
  def parked(state: OathState)(using munit.Location): Option[Decide] =
    ParkedNode.of(state, catalog, WalkerPowerCatalog.default(catalog)) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => Some(decide)
      case Right(None) => None
      case other => munit.Assertions.fail(s"expected a parked decision, got $other")

  /** The plainest legal answer: the first available option, the fewest
    * picks a choose-many allows, and nothing given in a distribution. */
  def plainestAnswer(decide: Decide)(using munit.Location): DecisionAnswer =
    decide.query match
      case one: DecisionQuery.ChooseOne =>
        DecisionAnswer.ChooseOneAnswer(one.options.filter(_.available).head.ref)
      case many: DecisionQuery.ChooseMany =>
        DecisionAnswer.ChooseManyAnswer(
          many.options.filter(_.available).take(many.min).map(_.ref))
      case spread: DecisionQuery.Distribute =>
        DecisionAnswer.DistributeAnswer(spread.slots.map(slot =>
          DistributeAmount(slot.ref, slot.minimum)))
      case other => munit.Assertions.fail(s"no plain answer for $other")

  /** `transition`, then every parked decision answered with
    * `plainestAnswer`, until nothing is parked. */
  def answeringEverything(rules: OathRules, transition: OathTransition)(
      using munit.Location): OathTransition =
    Iterator.iterate(Option(transition))(_.flatMap(step =>
      parked(step.state).map(decide => accepted(rules.resolveWalker(
        step.state, decide.owner, decide.decisionId, plainestAnswer(decide)))
        .pipe(next => OathTransition(next.state, step.events ++ next.events)))))
      .takeWhile(_.nonEmpty).flatten.take(100).toVector.last

  extension [A](value: A) private def pipe[B](f: A => B): B = f(value)
