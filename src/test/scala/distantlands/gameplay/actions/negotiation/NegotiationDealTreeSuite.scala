package distantlands.gameplay.actions.negotiation

import distantlands.model._
import distantlands.model.DecisionAnswer.{DeclineDeal, ProposeTerms}
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** The deal walk's lines, read under its prefix. */
class NegotiationDealTreeSuite extends munit.FunSuite:
  test("the decline line names who declined the deal under its own " +
      "prefix"):
    val answered = Vector(Answered("gathering.1.deal", DeclineDeal, p3),
      Answered("gathering.2.deal", DeclineDeal, p2))
    val states = NoteStates(Table.start.ready, None, answered)
    assertEquals(NegotiationDealTree.endedNote("gathering.1")(states),
      Some(NegotiationProcedure.ended.sourceless(NoteArg.Player(p3))))
    assertEquals(NegotiationDealTree.endedNote("gathering.2")(states),
      Some(NegotiationProcedure.ended.sourceless(NoteArg.Player(p2))))
    assertEquals(NegotiationDealTree.endedNote("negotiation")(states), None)

  test("a row's secrets line follows its favor line and comes before " +
      "its relics"):
    val relic = RelicId("r1")
    val deal = NegotiationDeal.fold(NegotiationDeal.NegotiationPrefix,
      Vector(p1, p2), Vector(Answered(NegotiationDeal.dealDecisionId,
        ProposeTerms(NegotiationTerms(Vector(NegotiationTransfer(p2, 1,
          Vector(relic), secrets = 2)))), p1)))
    assertEquals(NegotiationDealTree.transferred(deal), Vector(
      NegotiationProcedure.gaveFavor.sourceless(NoteArg.Player(p1),
        NoteArg.Number(1), NoteArg.Player(p2)),
      NegotiationProcedure.gaveSecrets.sourceless(NoteArg.Player(p1),
        NoteArg.Number(2), NoteArg.Player(p2)),
      NegotiationProcedure.gaveCard.sourceless(NoteArg.Player(p1),
        NoteArg.Card(relic), NoteArg.Player(p2))))
