package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.{NegotiationDeal,
  NegotiationSites}
import distantlands.gameplay.cards.DunesSite
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, ChooseAmountAnswer,
  DeclineDeal, ProposeTerms}
import distantlands.testkit.Table.{p1, p2}

/** A deal's answers live under its prefix (catalog batch 12, "One
  * decision id per deal"), so one walk can hold several deals. */
class NegotiationDealPrefixSuite extends munit.FunSuite:
  private val dunes = DunesSite.id
  private val gift = NegotiationTerms(Vector(
    NegotiationTransfer(p2, 1, Vector.empty)))

  test("a deal's ids are its prefix's, and the Negotiate action's stay"):
    assertEquals(NegotiationDeal.dealId("gathering.2"), "gathering.2.deal")
    assertEquals(NegotiationDeal.dealDecisionId, "negotiation.deal")
    assertEquals(NegotiationDeal.negotiatorsDecisionId,
      "negotiation.negotiators")
    assertEquals(NegotiationSites.warbandsDecisionId("gathering.2", dunes),
      s"gathering.2.warbands.${dunes.value}")

  test("a deal folds only the answers under its own prefix"):
    val answered = Vector(
      Answered("gathering.1.deal", ProposeTerms(gift), p1),
      Answered("gathering.2.deal", AcceptDeal, p2),
      Answered("gathering.2.deal", DeclineDeal, p2))
    val first = NegotiationDeal.fold("gathering.1", Vector(p1, p2), answered)
    assertEquals(first.prefix, "gathering.1")
    assertEquals(first.terms(p1), gift)
    assertEquals(first.accepted, Set.empty[PlayerId])
    assert(!first.declined)
    assert(NegotiationDeal.fold("gathering.2", Vector(p1, p2), answered)
      .declined)

  test("a placement is read under its own prefix"):
    val answered = Vector(Answered(
      NegotiationSites.warbandsDecisionId("gathering.1", dunes),
      ChooseAmountAnswer(2), p1))
    assertEquals(NegotiationSites.placed(answered, "gathering.1", dunes),
      Some(2))
    assertEquals(NegotiationSites.placed(answered, "gathering.2", dunes),
      None)
