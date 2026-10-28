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

  /** p1's won game after the Sun: p1 is Chancellor and holds the Throne,
    * p2 the World and p3 the Beacon, and the Empire has a supply of 24
    * Imperial warbands but no site yet. The driver's step after
    * `ChronicleSun` starts the Throne. */
  def throneHeldByChancellorP1: Table = chronicleWonByP1.update(state =>
    val lineage = state.game.current.players.find(_.player == p1).get.lineage
    state.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.updatedWith(lineage)(_.map(_.copy(
        role = Role.Chancellor)))))
      .updateCurrent(_.copy(chronicleTasks = Map(
        ChronicleTask.Throne -> p1, ChronicleTask.World -> Table.p2,
        ChronicleTask.Beacon -> Table.p3)))
      .copy(banks = state.banks.copy(warbandSupply =
        state.banks.warbandSupply.updated(ForceKind.Imperial, 24))))

  /** The World, after `throneHeldByChancellorP1`, parked on its keep
    * decision: p2 protected the second stack, which is in their hand, and
    * the other six are still gathered. */
  def worldParkedOnKeepWithSecondStack(rules: OathRules)(
      using munit.Location): OathState =
    val started = accepted(rules.next(OathTransition(
      throneHeldByChancellorP1.state, Vector.empty),
      TriggeredProcedureRef.ChronicleThrone))
    val walked = walkedTo(rules, started,
      ProtectAndDispossess.protectDecisionId)
    val nine = oathdigital.testkit.Outcomes.ready(walked.state).game.current
      .chronicleGathering
    accepted(rules.resolveWalker(walked.state, Table.p2,
      ProtectAndDispossess.protectDecisionId,
      DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Denizen(nine(4).asInstanceOf[DenizenId])))).state

  /** The decision `state` is parked on, if any. */
  def parked(state: OathState)(using munit.Location): Option[Decide] =
    ParkedNode.of(state, catalog, WalkerPowerCatalog.default(catalog)) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => Some(decide)
      case Right(None) => None
      case other => munit.Assertions.fail(s"expected a parked decision, got $other")

  /** The plainest legal answer: the first available option, the fewest
    * picks a choose-many allows, nothing given in a distribution, and, in
    * a partition, each section's minimum. */
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
      // Each section takes its minimum in option order; the rest go to the
      // first section with no maximum.
      case split: DecisionQuery.Partition =>
        val available = split.options.filter(_.available).map(_.ref)
        val (placed, rest) = split.sections.foldLeft(
          (Vector.empty[DecisionPlacement], available)) {
          case ((done, left), section) =>
            (done ++ left.take(section.minRequired).map(
              DecisionPlacement(_, section.key)), left.drop(section.minRequired))
        }
        val overflow = split.sections.find(_.maxAllowed.isEmpty).get.key
        DecisionAnswer.PartitionAnswer(
          placed ++ rest.map(DecisionPlacement(_, overflow)))
      case other => munit.Assertions.fail(s"no plain answer for $other")

  /** `transition`, answering every other parked decision plainly, until
    * the walk parks on `decisionId` or parks on nothing. */
  def walkedTo(rules: OathRules, transition: OathTransition,
      decisionId: String)(using munit.Location): OathTransition =
    Iterator.iterate(transition)(step =>
      val decide = parked(step.state).get
      val next = accepted(rules.resolveWalker(step.state, decide.owner,
        decide.decisionId, plainestAnswer(decide)))
      OathTransition(next.state, step.events ++ next.events))
      .find(step => parked(step.state).forall(_.decisionId == decisionId))
      .get

  /** The state when `procedure` completed: `transition`, walked from
    * `base`, with every parked decision answered plainly, then replayed
    * from `base` only up to that completion. A Throne test reads the table
    * as the Throne left it, before the World gathers its cards. */
  def endOf(rules: OathRules, base: OathState, transition: OathTransition,
      procedure: TriggeredProcedureRef)(using munit.Location): ReadyGame =
    val events = answeringEverything(rules, transition).events
    val through = events.indexOf(
      oathdigital.gameplay.walker.WalkerCompleted(procedure))
    munit.Assertions.assert(through >= 0, s"${procedure.key} never completed")
    oathdigital.testkit.Outcomes.ready(accepted(events.take(through + 1)
      .foldLeft[Either[OathViolation, OathState]](Right(base))((state, event) =>
        state.flatMap(rules.evolve(_, event)))))

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
