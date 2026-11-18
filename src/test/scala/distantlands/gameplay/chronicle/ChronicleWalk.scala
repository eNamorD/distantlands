package distantlands.gameplay.chronicle

import distantlands.gameplay.OathRules
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{ParkedNode, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Shared steps for Chronicle task tests: the table a task starts from, and
  * a walk that answers every parked Chronicle decision with its plainest
  * answer. */
object ChronicleWalk:
  /** The Chronicle phase of a game p1 won, with the Begin decision already
    * answered: the driver's next step after `ChronicleBegin` starts the Sun.
    */
  def chronicleWonByP1: Table = Table.start
    .turn(p1, Phase.Chronicle)
    .update(_.updateCurrent(_.copy(
      result = Some(GameResult(p1, VictoryKind.Usurper)))))

  /** p1's won game after the Sun: p1 is Chancellor and holds the Throne,
    * p2 the World and p3 the Beacon, and the Empire has a supply of 24
    * Imperial warbands but no site yet. The driver's step after
    * `ChronicleSun` starts the Throne. */
  def throneHeldByChancellorP1: Table = chronicleWonByP1.update(state =>
    val lineage = state.game.current.player(p1).get.lineage
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
    val nine = distantlands.testkit.Outcomes.ready(walked.state).game.current
      .cardGathering
    accepted(rules.resolveWalker(walked.state, Table.p2,
      ProtectAndDispossess.protectDecisionId,
      DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Denizen(nine(4).asInstanceOf[DenizenId])))).state

  /** A Chronicle walked to the end of one procedure: the state replayed to
    * that end, the events up to it, and every decision it parked on. */
  final case class Run(ready: ReadyGame, events: Vector[OathEvent],
      asked: Vector[Decide])

  /** The Chronicle started on `table` from the end of `after` (the driver's
    * next procedure: Begin starts the Sun, the Sun starts the Throne), each
    * park answered by `answers` where it names one and plainly otherwise,
    * up to `WalkerCompleted(procedure)`.
    * Every answer goes through `rules.resolveWalker`, which rebuilds the
    * walk from the journaled state, so each is a resumed walk. */
  def through(rules: OathRules, table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty,
      after: ProcedureRef = TriggeredProcedureRef.ChronicleBegin)(
      using munit.Location): Run =
    val done = distantlands.gameplay.walker.WalkerCompleted(procedure)
    var asked = Vector.empty[Decide]
    val started = accepted(rules.next(OathTransition(table.state, Vector.empty),
      after))
    val walked = Iterator.iterate(started)(step =>
      val decide = parked(step.state).get
      asked :+= decide
      val next = accepted(rules.resolveWalker(step.state, decide.owner,
        decide.decisionId, answers.applyOrElse(decide, plainestAnswer)))
      OathTransition(next.state, step.events ++ next.events))
      .find(_.events.contains(done)).get
    val events = walked.events.take(walked.events.indexOf(done) + 1)
    Run(distantlands.testkit.Outcomes.ready(accepted(events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))), events, asked)

  /** The Chronicle started on `table` from the end of `after`, as
    * [[through]] starts it, for a suite outside `gameplay`, which cannot
    * reach the driver. */
  def started(rules: OathRules, table: Table,
      after: ProcedureRef)(using munit.Location): OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty), after))

  /** The decision `state` is parked on, if any. */
  def parked(state: OathState)(using munit.Location): Option[Decide] =
    ParkedNode.of(state, catalog, WalkerPowerCatalog.default(catalog)) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => Some(decide)
      case Right(None) => None
      case other => munit.Assertions.fail(s"expected a parked decision, got $other")

  /** The plainest legal answer: the first available option, a
    * choose-many's preselected options, else the fewest picks it allows, nothing given in a distribution, and, in
    * a partition, each section's minimum. */
  def plainestAnswer(decide: Decide)(using munit.Location): DecisionAnswer =
    decide.query match
      // Build Edifice builds nothing: the first option would bury a card.
      case _ if decide.decisionId == BuildEdifice.cardDecisionId =>
        DecisionAnswer.ChooseOneAnswer(BuildEdifice.skip)
      case one: DecisionQuery.ChooseOne =>
        DecisionAnswer.ChooseOneAnswer(one.options.filter(_.available).head.ref)
      case many: DecisionQuery.ChooseMany =>
        DecisionAnswer.ChooseManyAnswer(
          if many.selected.nonEmpty then many.selected.map(_.ref)
          else many.options.filter(_.available).take(many.min).map(_.ref))
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

  /** `table`'s Beacon, which the driver starts where the World ends,
    * walked as [[walkedTo]] walks to `decisionId`. */
  def beaconWalkedTo(rules: OathRules, table: Table, decisionId: String)(
      using munit.Location): OathTransition =
    walkedTo(rules, accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleWorld)), decisionId)

  /** The state when `procedure` completed: `transition`, walked from
    * `base`, with every parked decision answered plainly, then replayed
    * from `base` only up to that completion. A Throne test reads the table
    * as the Throne left it, before the World gathers its cards. */
  def endOf(rules: OathRules, base: OathState, transition: OathTransition,
      procedure: TriggeredProcedureRef)(using munit.Location): ReadyGame =
    val events = answeringEverything(rules, transition).events
    val through = events.indexOf(
      distantlands.gameplay.walker.WalkerCompleted(procedure))
    munit.Assertions.assert(through >= 0, s"${procedure.key} never completed")
    distantlands.testkit.Outcomes.ready(accepted(events.take(through + 1)
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
