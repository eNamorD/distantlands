package oathdigital.gameplay.walker

import oathdigital.gameplay.powerresolver.{AsksBeforeBatch, BatchAsk,
  BatchContext, ContributingPower, Replacement}
import oathdigital.model.{Answered, Branch, BuildOps, CoreOperation, Note,
  OathViolation, Operation, PendingTree, PlayerId, PowerId, ReadyGame,
  Sequence, Take}

/** The walker's side of batch questions (catalog batch 11, slice 4). Before
  * a `BuildOps` leaf's batch runs, each power that asks before a batch
  * looks at its operations, a `Take`'s leaving included. When one asks,
  * the leaf is walked as a subtree of two children under
  * `path :+ segment`:
  *
  *  - at `0`, a `Branch` of one `Branch` per operation, in order, which
  *    holds that operation's question, if any, then a `Branch` of each
  *    taken answer's line. A question is selected only when the walk
  *    reaches it, after the answers before it, so a power sees what they
  *    took (`BatchContext.taken`);
  *  - at `1`, the batch, rebuilt from the unchanged state, with each taken
  *    answer's replacement in its operation's place.
  *
  * The batch in the subtree asks nothing again. A resume rebuilds the same
  * subtree. The batch's place does not depend on how many questions were
  * asked, and the `Branch` selects its questions only when the walk enters
  * it, so a resume inside the batch's follow-ups, where the batch has run,
  * never asks again. A builder reads only state and answers, as every
  * `BuildOps` does, and the questions change nothing. A search does not
  * ask: it walks the batch as built, which declining every question
  * leaves.
  */
private[walker] object BatchQuestions:
  /** The path segment under a leaf where its batch's questions are
    * walked. It is not a number, so no composite's child shares it. */
  val segment: String = "asked"

  /** A question one power asks about one operation. */
  private final case class Asked(power: PowerId, operation: CoreOperation,
      ask: BatchAsk)

  /** Whether the leaf at `path` is the batch a question subtree runs. */
  def answering(path: Vector[String]): Boolean =
    path.dropRight(1).lastOption.contains(segment)

  /** The subtree that asks about `build`'s batch at `path`, or `None` when
    * no power asks. */
  def subtree(build: BuildOps, ready: ReadyGame, answered: Vector[Answered],
      powers: WalkerPowers, actor: PlayerId, path: Vector[String])
      : Either[OathViolation, Option[Operation]] =
    if answering(path) || asking(powers, ready).isEmpty then Right(None)
    else build.build(ready, PendingTree(at = path, answered = answered))
      .map(operations => Option.when(of(asking(powers, ready), powers, actor,
        ready, operations, answered).exists(_.nonEmpty))(
        walked(build, powers, actor, path)))

  /** The subtree a walk that asked enters, at the leaf `build` at `path`.
    * A resume and a navigation build it whether or not a power asks now:
    * past the questions, the batch may have run. */
  def walked(build: BuildOps, powers: WalkerPowers, actor: PlayerId,
      path: Vector[String]): Operation =
    def questions(now: ReadyGame, answered: Vector[Answered])
        : Vector[Option[Asked]] =
      build.build(now, PendingTree(at = path, answered = answered)).fold(
        _ => Vector.empty, operations =>
          of(asking(powers, now), powers, actor, now, operations, answered))
    Sequence(Vector[Operation](
      Branch((now, pending) => build.build(now, pending.copy(at = path))
        .fold(_ => Vector.empty, operations =>
          flat(operations).indices.toVector.map[Operation](index =>
            Branch((later, again) => questions(later, again.answered)
              .lift(index).flatten.map(_.ask.decide).toVector)) :+
          Branch((later, again) => questions(later, again.answered).flatten
            .map(one => Note(one.power, states =>
              answer(states.answered, one).map(_.line)))))),
      BuildOps((now, pending) =>
        build.build(now, pending.copy(at = path)).map(rebuilt =>
          replaced(rebuilt, of(asking(powers, now), powers, actor, now,
            rebuilt, pending.answered).flatten, pending.answered)),
        required = build.required)))

  /** The powers that may ask about a batch built in `ready`. */
  private def asking(powers: WalkerPowers, ready: ReadyGame)
      : Vector[AsksBeforeBatch] =
    powers.powers.collect { case power: AsksBeforeBatch => power }
      .filter(_.watching(ready)).sortBy(ContributingPower.sortKey)

  /** The first asking power's question on each operation, by operation,
    * a `Take`'s leaving included. Each power sees the replacements that
    * `answered` took for the questions before it. Equal operations share
    * one question, which the first of them asks. */
  private def of(asking: Vector[AsksBeforeBatch], powers: WalkerPowers,
      actor: PlayerId, ready: ReadyGame, operations: Vector[CoreOperation],
      answered: Vector[Answered]): Vector[Option[Asked]] =
    flat(operations).foldLeft((Vector.empty[Option[Asked]],
        Vector.empty[Replacement], Set.empty[String])) {
      case ((done, taken, ids), operation) =>
        val context = BatchContext(powers.catalog, actor, ready, taken)
        val found = asking.iterator.flatMap(power =>
          power.askBefore(context, operation).map(Asked(power.id, operation,
            _))).nextOption().filterNot(one => ids(one.ask.decide.decisionId))
        (done :+ found, taken ++ found.flatMap(answer(answered, _)),
          ids ++ found.map(_.ask.decide.decisionId))
    }._1

  /** The replacement the latest answer to `asked` takes, if any. */
  private def answer(answered: Vector[Answered], asked: Asked)
      : Option[Replacement] =
    answered.reverseIterator.find(_.decisionId ==
      asked.ask.decide.decisionId).flatMap(found =>
      asked.ask.instead(found.answer))

  /** `operations` with each answered operation replaced, a `Take`'s
    * leaving included. */
  private def replaced(operations: Vector[CoreOperation],
      asked: Vector[Asked], answered: Vector[Answered])
      : Vector[CoreOperation] = operations.flatMap { operation =>
    asked.find(_.operation == operation).flatMap(answer(answered, _)) match
      case Some(replacement) => replacement.operations
      case None => operation match
        case take: Take if take.leaving.nonEmpty =>
          Vector(take.copy(leaving = replaced(take.leaving, asked, answered)))
        case other => Vector(other)
  }

  private def flat(operations: Vector[CoreOperation]): Vector[CoreOperation] =
    operations.flatMap {
      case take: Take => take +: flat(take.leaving)
      case other => Vector(other)
    }
