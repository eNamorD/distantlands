package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.{DealRules, DealState,
  NegotiationDeal}
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.operations.OperationExecutor
import distantlands.gameplay.powers.AdviserLimit
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.DecisionQueries
import distantlands.model._
import distantlands.model.DecisionAnswer.ProposeTerms
import distantlands.model.OathState.Ready
import distantlands.model.OathViolation.InsufficientSecrets
import distantlands.testkit.{ParkedNode, Table}
import distantlands.testkit.Table.{p1, p2}

/** Secret terms (catalog batch 12, rulings 6 and 31): closed in the
  * Negotiate action, bounded by the author's faceup secrets where a deal's
  * rules open them, and moved faceup at settlement. The Gathering opens
  * them (slice 6b); until then these tests open them through `DealRules`.
  */
class NegotiationSecretTermsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val site = BrokenPeaksSite.id

  /** p1 and p2 at Broken Peaks; p1 holds `faceUp` faceup secrets and 1
    * facedown, p2 none. */
  private def withSecrets(faceUp: Int = 2): Table = Table.start
    .pawn(p1, at = site).pawn(p2, at = site)
    .secrets(p1, faceUp = faceUp, faceDown = 1).secrets(p2, faceUp = 0)

  private def give(secrets: Int): NegotiationTerms = NegotiationTerms(
    Vector(NegotiationTransfer(p2, 0, Vector.empty, secrets = secrets)))

  /** p1's deal with p2, folded from `answered`. */
  private def dealOf(answered: Answered*): DealState =
    NegotiationDeal.fold(NegotiationDeal.NegotiationPrefix, Vector(p1, p2),
      answered.toVector)

  private def query(table: Table, open: Boolean): DecisionQuery.Negotiate =
    NegotiationDeal.snapshot(catalog, table.ready, dealOf(),
      DealRules(secretsOpen = open), AdviserLimit)

  private def offered(query: DecisionQuery.Negotiate, secrets: Int) =
    DecisionQueries.accepts(NegotiationDeal.dealDecisionId, query,
      ProposeTerms(give(secrets)), p1)

  test("the Negotiate action keeps secrets closed"):
    val started = rules.startWalker(Ready(withSecrets().ready),
      ActionRef.Negotiation, p1).getOrElse(fail("Negotiation must start"))
    val negotiate = ParkedNode.of(started.state, catalog) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) =>
        decide.query match
          case negotiate: DecisionQuery.Negotiate => negotiate
          case other => fail(s"not a deal: $other")
      case other => fail(s"not parked on a decision: $other")
    assertEquals(negotiate.bounds(p1).maxSecrets, 0)
    assertEquals(negotiate.bounds(p1).secretRecipients,
      Vector.empty[PlayerId])
    assert(offered(negotiate, 1).isLeft)

  test("with secrets open, an author may give up to their faceup " +
      "secrets"):
    val open = query(withSecrets(), open = true)
    assertEquals(open.bounds(p1).maxSecrets, 2)
    assertEquals(open.bounds(p1).secretRecipients, Vector(p2))
    assertEquals(open.bounds(p2).maxSecrets, 0)
    assert(offered(open, 2).isRight)
    assert(offered(open, 3).isLeft)
    assert(offered(query(withSecrets(), open = false), 1).isLeft)

  test("settling moves faceup secrets, faceup, and leaves the facedown " +
      "one"):
    val ready = withSecrets().ready
    val ops = NegotiationDeal.settle(ready, dealOf(Answered(
      NegotiationDeal.dealDecisionId, ProposeTerms(give(2)), p1)))
      .getOrElse(fail("the settlement must hold"))
    val executor = new OperationExecutor()
    val after = ops.foldLeft(ready)((now, op) => executor.execute(now, op)
      .getOrElse(fail(s"$op must apply")))
    val (giver, recipient) = (after.game.current.player(p1).get.board,
      after.game.current.player(p2).get.board)
    assertEquals((giver.faceUpSecrets, giver.faceDownSecrets), (0, 1))
    assertEquals((recipient.faceUpSecrets, recipient.faceDownSecrets),
      (2, 0))

  test("settling is refused once the faceup secrets are gone"):
    val refused = NegotiationDeal.settle(withSecrets(faceUp = 1).ready,
      dealOf(Answered(NegotiationDeal.dealDecisionId, ProposeTerms(give(2)),
        p1)))
    assertEquals(refused, Left(InsufficientSecrets(2, 1)))
