package oathdigital.application.gamelog

import oathdigital.engine.{RecordedEvent, ReplayStep}
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.powers.campaign.StickyFireCard
import oathdigital.gameplay.walker.{DeltaPayload, WalkerCompleted,
  WalkerStepRecorded}
import oathdigital.model._
import oathdigital.testkit.Table.p2
import LogScripts._

/** The log re-applies a recorded batch to read the state around each of its
  * operations. A journal that loaded has had that batch applied already --
  * `history` and `load` share one `reconstruct` -- so a rejection here is a
  * programming error, and the formatting fails rather than narrating lines
  * from a state the operations never reached (log operation strictness
  * decision).
  *
  * Only a hand-built step can reach it, which is what this suite builds:
  * production has no path that formats a journal the engine did not accept.
  */
class GameLogStrictnessSuite extends munit.FunSuite:
  test("a recorded operation that does not apply fails the formatting"):
    val script = recordedEventsBoard
    // The board's relic lies at Broken Peaks, so there is none at Ancient
    // City for this peek to find.
    val peekWhereNoRelicLies = Peek(p2, StickyFireCard.id,
      Location.Site(AncientCitySite.id))
    // Built by hand, as no helper would: `formatAppended` folds its batch
    // and refuses one that does not apply before the formatter sees it.
    val steps = script.history.steps
    val last = steps.last.after
    val unapplied = Vector[OathEvent](
      WalkerStepRecorded("batch",
        DeltaPayload(Vector(peekWhereNoRelicLies)), Vector.empty),
      WalkerCompleted(ActionRef.Travel)).zipWithIndex.map((event, index) =>
      ReplayStep(RecordedEvent(steps.size.toLong + index, event), last, last))
    val failure = intercept[IllegalStateException](
      formatter.format(steps ++ unapplied, None))
    assert(failure.getMessage.contains("does not apply"), failure.getMessage)
    assert(failure.getMessage.contains("missing-piece"), failure.getMessage)
