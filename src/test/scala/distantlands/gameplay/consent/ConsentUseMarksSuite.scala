package distantlands.gameplay.consent

import distantlands.gameplay.OathRules
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{ChoicePayload, WalkerStepRecorded}
import distantlands.model._
import distantlands.model.DecisionAnswer.{ChooseAmountAnswer,
  ChooseManyAnswer, ChooseOneAnswer}
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** The use mark (reconsider design, "State"; ruling 21): set where a
  * recorded answer chooses an option a request of this turn granted, and
  * kept in the game, which undo rewinds. */
class ConsentUseMarksSuite extends munit.FunSuite:
  private val pass = PowerId("site.narrow-pass.pass")
  private val dunes = DecisionOptionRef.Site(SiteId("site:dunes"))
  private val steppe = DecisionOptionRef.Site(SiteId("site:steppe"))
  private val destination = "travel.destination"
  private val table = Outcomes.ready(Table.start.state)
  private val now = ConsentTurn.of(table)

  /** The table after p2 answered p1's request about `target`. */
  private def tableWhereP2Answered(target: DecisionOptionRef,
      granted: Boolean): ReadyGame =
    val request = ConsentRequest(p1, destination, target,
      Vector(ConsentAsk(p2, Vector(pass))))
    table.copy(consent = accepted(accepted(
      table.consent.requested(request)).answered(p2, granted, now)))

  private def marks(ready: ReadyGame): Set[UseMark] =
    ready.game.current.useMarks

  test("choosing a granted option marks it and locks it"):
    val chosen = ConsentUseMarks.answered(tableWhereP2Answered(dunes,
      granted = true), destination, ChooseOneAnswer(dunes), p1)
    assertEquals(marks(chosen), Set(UseMark(now, p1, destination, dunes)))
    assert(ConsentUseMarks.locked(chosen, p1, destination, dunes))
    assert(!ConsentUseMarks.locked(chosen, p1, destination, steppe))

  test("no mark for a refused option, an option nobody asked about, " +
      "another player's answer, another decision or an amount"):
    val granted = tableWhereP2Answered(dunes, granted = true)
    val refused = tableWhereP2Answered(dunes, granted = false)
    assertEquals(marks(ConsentUseMarks.answered(refused, destination,
      ChooseOneAnswer(dunes), p1)), Set.empty)
    assertEquals(marks(ConsentUseMarks.answered(granted, destination,
      ChooseOneAnswer(steppe), p1)), Set.empty)
    assertEquals(marks(ConsentUseMarks.answered(granted, destination,
      ChooseOneAnswer(dunes), p2)), Set.empty)
    assertEquals(marks(ConsentUseMarks.answered(granted,
      "campaign.targets", ChooseOneAnswer(dunes), p1)), Set.empty)
    assertEquals(marks(ConsentUseMarks.answered(granted, destination,
      ChooseAmountAnswer(2), p1)), Set.empty)

  test("a choose-many answer marks each granted option it includes"):
    val chosen = ConsentUseMarks.answered(tableWhereP2Answered(dunes,
      granted = true), destination,
      ChooseManyAnswer(Vector(steppe, dunes)), p1)
    assertEquals(marks(chosen), Set(UseMark(now, p1, destination, dunes)))

  test("a mark is of its turn: another turn's mark locks nothing and " +
      "goes when a new one is set"):
    val ofAnotherTurn = UseMark(ConsentTurn(now.round + 1, now.player),
      p1, destination, dunes)
    val stale = tableWhereP2Answered(dunes, granted = true)
      .updateCurrent(_.copy(useMarks = Set(ofAnotherTurn)))
    assert(!ConsentUseMarks.locked(stale, p1, destination, dunes))
    assertEquals(marks(ConsentUseMarks.answered(stale, destination,
      ChooseOneAnswer(dunes), p1)),
      Set(UseMark(now, p1, destination, dunes)))

  test("the fold of a recorded answer sets the mark from the ledger it " +
      "meets"):
    val rules = new OathRules(catalog)
    def folded(ready: ReadyGame): ReadyGame = Outcomes.ready(accepted(
      rules.evolve(OathState.Ready(ready), WalkerStepRecorded("0",
        ChoicePayload(destination, ChooseOneAnswer(dunes), p1,
          alone = true), Vector.empty))))
    assertEquals(marks(folded(tableWhereP2Answered(dunes, granted = true))),
      Set(UseMark(now, p1, destination, dunes)))
    assertEquals(marks(folded(tableWhereP2Answered(dunes,
      granted = false))), Set.empty)

  test("marks follow a turn that is renamed"):
    val mark = UseMark(now, p1, destination, dunes)
    val other = mark.copy(turn = ConsentTurn(now.round + 3, now.player))
    val later = ConsentTurn(now.round + 1, now.player)
    assertEquals(UseMark.renamed(Set(mark, other), now, later),
      Set(mark.copy(turn = later), other))
