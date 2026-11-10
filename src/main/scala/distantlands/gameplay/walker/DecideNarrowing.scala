package distantlands.gameplay.walker

import distantlands.model.{Answered, ConsentKey, ConsentTurn, Decide, DecisionAnswer, DecisionOptionRef, DieFace, OathViolation, Operation, OptionReason, PowerId}

/** How a resumed command treats the leaf parked at `pending.at`: a plain
  * `advance` re-parks an unanswered Decide/Roll (or passes a Decide the
  * caller already recorded in `answered`); a `roll` consumes the parked
  * Roll (validating faces, merging the outcome, recording the event); a
  * `resolve` consumes the parked Decide (validating the answer, recording
  * the ChoicePayload event) — both then walk on from the following node.
  */
private[walker] sealed trait Resume extends Product with Serializable
private[walker] case object PlainResume extends Resume
private[walker] final case class RollResume(faces: Vector[DieFace]) extends Resume
private[walker] final case class AnswerResume(answer: Answered) extends Resume

/** The decision-narrowing half of [[ProcedureWalker]], split out to keep that
  * file under the project's line bound, like [[WalkerReplay]] and
  * [[WalkerPowerGather]]. It is not a seam: the object is its only
  * implementor, and the members here recurse into the object's `walk`
  * (declared abstract below) to search ahead from a decision. `runLeaf`
  * narrows a decision where it is asked ([[narrowAt]]) or asks whether a
  * search reaches on from it ([[reach]]); `narrowParked` re-narrows a parked
  * one the same way.
  */
private[walker] trait DecideNarrowing:

  /** The walking core, which [[ProcedureWalker]] holds: walks `node` from
    * `path`, from scratch or resuming at `cursor`, under `hooks`. */
  protected def walk(node: Operation, ctx: WalkCtx, path: Vector[String],
      cursor: Option[Vector[String]], resume: Resume,
      hooks: WalkerHooks): Either[OathViolation, Step]

  /** `decide` narrowed to the options from which some path reaches a legal
    * end (global operation restrictions design, "Lazy pruning"), each hidden
    * option a restriction's note explains kept as unavailable (the note is
    * its hover reason and nothing is journaled), or as declared when the
    * powers do not search. An option its own window's waivable restriction
    * bars is made askable when a grant would open it (consent slice 4), and
    * so is an option only waivable operation refusals hide ([[OperationAsks]],
    * catalog batch 8, slice 10), or it shows their plain reason when nobody
    * may be asked. A required decision with nothing left is kept when an
    * askable option remains, as the search kept it ([[reach]]): its player
    * may ask, or when its builder declared an askable option (catalog
    * batch 8, slice 15). A plain reason alone keeps nothing, since nothing
    * then can be answered. A choose-one passed when empty with nothing left
    * parks on its Continue screen (catalog batch 8, slice 10, part 2b). */
  protected def narrowAt(decide: Decide, ctx: WalkCtx, path: Vector[String])
      : Either[OathViolation, Option[Decide]] =
    if !ctx.powers.probing then Right(Some(
      // Unsearched, only a choose-one declared with nothing to choose is
      // its Continue screen.
      if decide.passWhenEmpty && WalkerSearch.choosable(decide).isEmpty
      then EmptyChoices.offered(decide, Map.empty, Map.empty) else decide))
    else WalkerSearch.narrow(decide, verdict(decide, ctx, path)) match
      case Left(refused) =>
        val asks = declaredAt(decide, ctx, path) ++
          asksAt(decide, ctx, path) ++ operationAsksAt(decide, ctx, path,
            WalkerSearch.choosable(decide))
        if !asks.values.exists(askable) then Left(refused)
        else Right(Some(WalkerSearch.reasoned(WalkerSearch.narrowed(decide,
          _ => false, asks.get).getOrElse(decide), asks)))
      case Right(narrowed) =>
        // A choose-one passed when empty parks on its Continue screen when
        // nothing is left (catalog batch 8, slice 10, part 2b); a search
        // still passes it ([[reach]]).
        val cut = narrowed.getOrElse(EmptyChoices.nothingLeft(decide))
        lazy val reasons =
          val kept = WalkerSearch.choosable(cut).toSet
          val hidden = WalkerSearch.choosable(decide).filterNot(kept)
          operationAsksAt(decide, ctx, path, hidden) ++
            WalkerPowerGather.lookAheadNoted(ctx.restrictionRoot, decide, cut,
              ctx.state, ctx.actor, ctx.powers, ctx.answered,
              ctx.procedure, grantable(decide, ctx, path)).toMap
        narrowed match
          case Some(_) => Right(Some(WalkerSearch.reasoned(
            WalkerSearch.explained(decide, cut, reasons),
            asksAt(decide, ctx, path))))
          case None if decide.passWhenEmpty => Right(Some(
            EmptyChoices.offered(decide, reasons, asksAt(decide, ctx, path))))
          case None => Right(None)

  /** A search at a decision it reaches. Back at a decision it already stands
    * at in the same state, it stops: the loop leads on exactly when the first
    * visit does, and the first visit tries the other answers. A decision no
    * answer leads on from is still a stop when one of its options may be
    * asked about and a grant would open it: a payer who cannot pay may ask
    * to go on free (consent slice 4), an option only waivable operation
    * refusals hide may be asked about (catalog batch 8, slice 10), or the
    * decision's builder declared an askable option (slice 15). */
  protected def reach(decide: Decide, ctx: WalkCtx, path: Vector[String])
      : Either[OathViolation, Step] =
    if ctx.visited((path, WalkCtx.searchKey(ctx.state))) then
      Right(Stopped(ctx))
    else WalkerSearch.reach(decide, verdict(decide, ctx, path)) match
      case Left(refused) =>
        if declaredAt(decide, ctx, path).nonEmpty ||
            asksAt(decide, ctx, path).nonEmpty || operationAsksAt(decide,
            ctx, path, WalkerSearch.choosable(decide)).values
            .exists(askable) then
          Right(Stopped(ctx))
        else Left(refused)
      case Right(WalkerSearch.Reach.Skipped) => Right(Done(ctx))
      case Right(WalkerSearch.Reach.Answerable) => Right(Stopped(ctx))

  /** The asks `decide`'s own window allows, each kept only where a grant
    * would open its option. */
  private def asksAt(decide: Decide, ctx: WalkCtx, path: Vector[String])
      : Map[DecisionOptionRef, OptionReason] =
    WalkerPowerGather.optionAsks(decide, ctx.state, ctx.actor,
      ctx.powers, path, ctx.procedure, ctx.answered,
      grantable(decide, ctx, path)).toMap

  /** The asks `decide` declares itself, kept where a grant would open
    * their option ([[OperationAsks.declared]]). */
  private def declaredAt(decide: Decide, ctx: WalkCtx, path: Vector[String])
      : Map[DecisionOptionRef, OptionReason] = OperationAsks.declared(decide,
    ctx.state, state => verdict(decide, ctx.copy(state = state), path))

  private def askable(reason: OptionReason): Boolean =
    reason.isInstanceOf[OptionReason.Askable]

  /** The reasons of `refs`, options of `decide` the search hid, that only
    * powers' operation refusals stop (catalog batch 8, slice 10). */
  private def operationAsksAt(decide: Decide, ctx: WalkCtx,
      path: Vector[String], refs: Vector[DecisionOptionRef])
      : Map[DecisionOptionRef, OptionReason] =
    OperationAsks.of(decide, ctx.state, refs, state =>
      verdict(decide, ctx.copy(state = state), path))

  /** Whether one answer to `decide`, given by its owner, can reach a legal
    * end: it adds no tree-level `Restriction` violation the answers so far do
    * not already produce, and a search's walk from it on a copy of the state
    * is not rejected. The search runs the real pipeline, so any rejection of
    * a required operation fails the path. */
  private def verdict(decide: Decide, ctx: WalkCtx, path: Vector[String])
      : DecisionAnswer => Either[OathViolation, Unit] =
    lazy val baseline = WalkerPowerGather.breaches(ctx.restrictionRoot,
      ctx.state, ctx.actor, ctx.powers, ctx.answered, ctx.procedure).toSet
    // The search walks again from the root, which no follow-up encloses:
    // it re-enters each level by the path, as a resume does.
    val searching = ctx.copy(events = Vector.empty, searching = true,
      visited = ctx.visited + ((path, WalkCtx.searchKey(ctx.state))),
      followUps = Vector.empty, triggersOff = false)
    def reached(answer: DecisionAnswer): Either[OathViolation, Unit] =
      val hypothetical = Answered(decide.decisionId, answer, decide.owner)
      WalkerPowerGather.breaches(ctx.restrictionRoot, ctx.state, ctx.actor,
          ctx.powers, ctx.answered :+ hypothetical, ctx.procedure)
        .find(!baseline(_)).toLeft(())
        .flatMap(_ => searched(walk(ctx.root, searching, Vector.empty,
          Some(path), AnswerResume(hypothetical), WalkerHooks.none)))
    // Only the live walk's verdicts are kept: one asked inside a search also
    // depends on the decisions that search stands at.
    if ctx.searching then reached
    else
      val base = new SearchMemo.Base(ctx.root, path, ctx.state, ctx.answered,
        ctx.procedure, ctx.actor, decide.decisionId, decide.owner)
      answer => ctx.powers.searchMemo.verdict(base, answer)(reached(answer))

  /** Whether a grant of `powers` on `ref` would leave some path from it to a
    * legal end: the verdict, asked of the state as it would stand with the
    * grant remembered this turn (consent design, ruling 8). */
  private def grantable(decide: Decide, ctx: WalkCtx, path: Vector[String])(
      ref: DecisionOptionRef, powers: Vector[PowerId]): Boolean =
    val turn = ConsentTurn.of(ctx.state)
    val keys = powers.map(ConsentKey(_, decide.owner, decide.decisionId, ref))
    val granted = ctx.state.copy(consent =
      ctx.state.consent.granting(keys, turn))
    verdict(decide, ctx.copy(state = granted), path)(
      DecisionAnswer.ChooseOneAnswer(ref)).isRight

  /** A search's walk as a verdict. A broken position is a rejection, as it
    * is for a simulation. */
  private def searched(walked: => Either[OathViolation, Step])
      : Either[OathViolation, Unit] =
    try walked.map(_ => ())
    catch
      case error: IllegalArgumentException => Left(
        OathViolation.InvalidEventOrder(Option(error.getMessage)
          .getOrElse("invalid searched walker position")))
