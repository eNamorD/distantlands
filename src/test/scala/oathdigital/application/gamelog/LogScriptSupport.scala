package oathdigital.application.gamelog

import oathdigital.application._
import oathdigital.engine.{RecordedEvent, ReplayStep}
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import oathdigital.gameplay.phases.rest.FinishRestProcedure
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.{DeltaPayload, LogNoted, ProcedureWalker,
  WalkerCompleted, WalkerStepRecorded}
import oathdigital.model._
import oathdigital.protocol.projection.{LogEntry, LogSpan}
import oathdigital.testkit.{Dice, Look, MenuPlay, Situation, Step, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

/** What every log script and log suite leans on: the table the scripts
  * begin at, the formatter the suites read lines through, the helpers that
  * pick lines out of a log, and the journal edits (inserted events, notes
  * removed, events appended) the suites apply to a script's steps. The
  * scripts themselves live in `PhaseLogScripts`, `ActionLogScripts`,
  * `RaidLogScripts`, `NegotiationLogScripts` and `PowerLogScripts`, and
  * `LogScripts` re-exports them all.
  */
object LogScriptSupport:
  /** The table every script begins at: the quiet table with each pawn
    * stated at the site the scripts' log lines and goldens name, p1 at
    * Ancient City, p2 at Broken Peaks and p3 at Buried Giant. A script that
    * needs a pawn elsewhere moves it after this. */
  private[gamelog] def tableWithOnePawnPerSite: Table =
    Table.start.pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite)

  val presentation = new GamePresentationProjector(catalog)
  val formatter = new GameLogFormatter(catalog, presentation)

  def format(script: Script, viewer: Option[PlayerId])(using munit.Location)
      : Vector[LogEntry] =
    formatter.format(script.history.steps, viewer)

  /** An entry as a client that ignores span kinds shows it; the cost span
    * set apart by a space, as the pane sets it apart by a margin. */
  def text(entry: LogEntry): String = entry.spans.map {
    case cost: LogSpan.Cost => " " + cost.text
    case span => span.text
  }.mkString

  def texts(entries: Vector[LogEntry]): Vector[String] = entries.map(text)

  /** The first of `lines` that starts with `prefix`; fails with the lines. */
  def lineStarting(lines: Vector[String], prefix: String)(
      using munit.Location): String =
    lines.find(_.startsWith(prefix)).getOrElse(munit.Assertions.fail(
      s"no line starts with '$prefix' in $lines"))

  /** The first of `entries` whose text starts with `prefix`; fails with the
    * entries' texts. */
  def entryStarting(entries: Vector[LogEntry], prefix: String)(
      using munit.Location): LogEntry =
    entries.find(entry => text(entry).startsWith(prefix)).getOrElse(
      munit.Assertions.fail(
        s"no entry starts with '$prefix' in ${texts(entries)}"))

  def playerLabel(player: PlayerId): String = presentation.playerLabel(player)

  /** `steps` with `events` journaled right after position `after`. Each
    * changes no state, and every later sequence moves up to make room. */
  def inserted(steps: Vector[ReplayStep[OathState, OathEvent]], after: Int,
      events: OathEvent*): Vector[ReplayStep[OathState, OathEvent]] =
    val state = steps(after).after
    val at = steps(after).event.index + 1
    val added = events.toVector.zipWithIndex.map { case (event, offset) =>
      ReplayStep(RecordedEvent(at + offset, event), state, state) }
    steps.take(after + 1) ++ added ++ steps.drop(after + 1).map(step =>
      step.copy(event = step.event.copy(index = step.event.index + events.size)))

  /** `steps` without their notes, renumbered: the journal of a power
    * that writes no line of its own. Notes change no state, so every other
    * step is unchanged. */
  def withoutNotes(steps: Vector[ReplayStep[OathState, OathEvent]])
      : Vector[ReplayStep[OathState, OathEvent]] =
    steps.filterNot(_.event.event.isInstanceOf[LogNoted]).zipWithIndex.map {
      case (step, index) => step.copy(event = step.event.copy(index = index.toLong))
    }

  /** `script`'s log for `viewer` with `events` journaled after its last
    * step. The first appended step starts from `stateBefore` of the
    * script's last state (unchanged unless a test gives the state an event
    * needs), and each step after it from where the one before ended. A
    * recorded batch ends where its operations take it, and any other event
    * changes no state, so every bracket a line reads is one the events
    * reach (log operation strictness decision). */
  def formatAppended(script: Script, events: Vector[OathEvent],
      viewer: Option[PlayerId] = None,
      stateBefore: OathState => OathState = identity)(
      using munit.Location): Vector[LogEntry] =
    val steps = script.history.steps
    // The appended events stand for a walk of their own: the script's phase
    // menu, if it ends at one, is picked from first and so completed.
    val last = MenuPlay.withoutMenu(steps.last.after)
    val picked: Vector[OathEvent] = MenuPlay.parked(steps.last.after)
      .map(menu => WalkerCompleted(menu.procedure)).toVector
    val appended = (picked ++ events).zipWithIndex.foldLeft(
        (stateBefore(last), Vector.empty[ReplayStep[OathState, OathEvent]])) {
      case ((before, done), (event, index)) =>
        val after = reached(before, event)
        (after, done :+ ReplayStep(
          RecordedEvent(steps.size.toLong + index, event), before, after))
    }._2
    formatter.format(steps ++ appended, viewer)

  /** The state `event` leaves from `before`: a recorded batch's operations
    * applied, through the fold replay uses; anything else changes none. */
  private def reached(before: OathState, event: OathEvent)(
      using munit.Location): OathState = (before, event) match
    case (OathState.Ready(start),
        WalkerStepRecorded(_, DeltaPayload(ops), _)) =>
      ProcedureWalker.applyRecordedBatch(start, ops).fold(
        violation => munit.Assertions.fail(
          s"the appended batch does not apply: $violation"),
        states => states.lastOption.fold(before)(OathState.Ready(_)))
    case _ => before

  /** `format` over `withoutNotes`: the log of the script as a power that
    * writes no note would leave it. */
  def formatWithoutNotes(script: Script, viewer: Option[PlayerId])(using
      munit.Location): Vector[LogEntry] =
    formatter.format(withoutNotes(script.history.steps), viewer)

  /** A fresh service whose stream `name` begins at `table`, and the
    * journaled situation there. A script's table ends with `banditsRefilled`,
    * or its first command journals the bandit refill as well and the log
    * reads a "Bandits returned" line no script means to test. */
  def atTable(name: String, table: Table,
      dice: CampaignDicePort = Dice.steady)(
      using munit.Location): (GameApplicationService, Situation) =
    val (service, repository) = table.service(campaignDice = dice)
    (service, table.situation(Situation.journaled(service, catalog, repository,
      name)))

  /** The active player starts `action` with `args`. */
  private[gamelog] def start(situation: Situation, action: StartableRef,
      args: DecisionOptionRef*)(using munit.Location): Situation =
    situation.after(MenuPlay.start(Look(situation.ready).active, action, args.toVector))

  /** Arranges `ops` at the end of the first player's turn, just before
    * their Begin Rest, and plays on to the second player's Wake, returning
    * it and that player. An arrangement has no closing event of its own, so
    * the formatter reads it as the first step of the run that follows. The
    * run that follows here is the Rest, which tells only its deltas, so the
    * action a script tests stays whole. `ops` reads the situation it is
    * arranged in and the second player. */
  private[gamelog] def arrangedForNext(acting: Situation)(
      ops: (Situation, PlayerId) => Vector[CoreOperation])(
      using munit.Location): (Situation, PlayerId) =
    val first = Look(acting.ready).active
    val next = FinishRestProcedure.turnOrder(acting.ready)(1)
    val resting = acting.after(Step.Arrange(ops(acting, next)),
      MenuPlay.starting(first, PhaseTransitionRef.BeginRest))
    val rested = if Look(resting.ready).active == first then
      resting.finishingRest(first) else resting
    (rested, next)
