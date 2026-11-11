package distantlands.gameplay.actions.negotiation

import distantlands.model._
import distantlands.model.DecisionAnswer.DeclineDeal
import distantlands.testkit.Table
import distantlands.testkit.Table.{p2, p3}

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
