package distantlands.gameplay

import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.whenplayed.WizardsConclaveCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{ParkedDecisionAssertions, ProcedureWalker,
  WalkerPowers}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, PlacementPicks, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** The facedown-adviser play asks which adviser to play as the walk's first
  * decision (undo design, "The facedown-adviser pick"). p1 holds Wizard's
  * Conclave and Wrestlers facedown on the quiet table. */
class FacedownAdviserWalkerSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val parked = new ParkedDecisionAssertions(catalog)
  private val conclave = WizardsConclaveCard.id
  private val wrestlers = WrestlersCard.id
  private val holdingTwo = Table.start.adviser(p1, conclave, facedown = true)
    .adviser(p1, wrestlers, facedown = true).ready

  private def start(ready: ReadyGame,
      args: Vector[DecisionOptionRef] = Vector.empty) =
    rules.startWalker(Ready(ready), ActionRef.PlayFacedownAdviser, p1,
      startArgs = args)

  test("a start names no card and parks on the pick of a facedown adviser"):
    val started = accepted(start(holdingTwo))
    parked.assertParked(started.state, ActionRef.PlayFacedownAdviser,
      CardPlayProcedure.facedownAdviserDecisionId, p1)
    assertEquals(PlacementPicks.parkedOptions(started.state,
      WalkerPowers.empty).toSet, Set[DecisionOption](
      DecisionOption.Denizen(DecisionOptionRef.Denizen(conclave)),
      DecisionOption.Denizen(DecisionOptionRef.Denizen(wrestlers))))

  test("the picked card's placement follows, and playing it ends the action"):
    val started = accepted(start(holdingTwo))
    val picked = accepted(rules.resolveWalker(started.state, p1,
      CardPlayProcedure.facedownAdviserDecisionId,
      ChooseOneAnswer(DecisionOptionRef.Denizen(wrestlers))))
    parked.assertParked(picked.state, ActionRef.PlayFacedownAdviser,
      CardPlayProcedure.placementDecisionId(wrestlers), p1)
    val discarded = accepted(rules.resolveWalker(picked.state, p1,
      CardPlayProcedure.placementDecisionId(wrestlers),
      PlacementPicks.parkedAnswer(picked.state, PlayedAs.Discarded,
        WalkerPowers.empty)))
    parked.assertNotParked(discarded.state)
    assertEquals(Look(discarded.state).advisers(p1), Vector[CardId](conclave))

  test("a start that names a card is refused"):
    assertEquals(start(holdingTwo, Vector(DecisionOptionRef.Denizen(conclave))),
      Left(OathViolation.InvalidEventOrder(
        "facedown-adviser play takes no start selection")))

  test("with no facedown adviser the start is refused"):
    assertEquals(start(Table.start.ready), Left(
      OathViolation.InvalidSearchPlacement("no facedown adviser to play")))

  test("a start journaled with its card resumes on that card's placement"):
    // Before the pick moved into the walk, the start named the card, and its
    // tree was the placement alone.
    val tree = accepted(CardPlayProcedure.rebuildFacedown(catalog, holdingTwo,
      p1, Vector(DecisionOptionRef.Denizen(wrestlers))))
    val pending = PlacementFixture.park(holdingTwo, tree, WalkerPowers.empty)
    assertEquals(ProcedureWalker.parkedDecide(holdingTwo, tree, pending,
      WalkerPowers.empty).map(_.decisionId),
      Some(CardPlayProcedure.placementDecisionId(wrestlers)))
