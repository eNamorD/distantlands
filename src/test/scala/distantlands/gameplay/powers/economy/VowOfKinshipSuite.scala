package oathdigital.gameplay.powers.economy

import oathdigital.gameplay.VowOfKinshipCard
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.walker.{ProcedureWalker, WalkerPowers}
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseAmountAnswer
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Outcomes.{finished, parked}
import oathdigital.testkit.Table.{p1, p2}

/** Vow of Kinship's source question (catalog batch 11, ruling 10): with
  * favor on the holder's board and in the Nomad bank, a spend asks how
  * much comes from the bank. */
class VowOfKinshipSuite extends munit.FunSuite:
  private val powers = WalkerPowers(Vector(VowOfKinship))

  /** `holder` holds the Vow with `board` favor on their board and `bank`
    * in the Nomad bank. It is p1's turn. */
  private def vowHolder(holder: PlayerId, board: Int, bank: Int): Table =
    Table.start.adviser(holder, VowOfKinshipCard).favor(holder, board)
      .bankFavor(Suit.Nomad, bank)

  private def burning(player: PlayerId, n: Int): PayCost =
    PayCost(player, Location.SharedBank, Cost(favorBurnt = n))

  private def batchOf(ops: CoreOperation*): BuildOps =
    BuildOps((_, _) => Right(ops.toVector))

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(VowOfKinship.id, VowOfKinship.noteKeys, events)

  /** The question the walk of `tree` parked on. */
  private def asked(ready: ReadyGame, tree: Operation)(
      using munit.Location): Decide =
    val park = parked(ProcedureWalker.advance(ready, tree, None, powers))
    ProcedureWalker.parkedDecide(ready, tree, park.tree, powers)
      .getOrElse(fail(s"no question at ${park.tree.at}"))

  test("with both sources, a spend asks how much comes from the bank, " +
      "and the answer draws it"):
    val ready = vowHolder(p1, board = 2, bank = 4).ready
    val tree = batchOf(burning(p1, 3))
    val question = asked(ready, tree)
    assertEquals(question.decisionId, VowOfKinship.decisionId)
    assertEquals(question.owner, p1)
    assertEquals(question.query, DecisionQuery.ChooseAmount(1, 3,
      Some("Vow of Kinship: how much of the 3 favor comes from the " +
        "Nomad bank?"), "Take from the Nomad bank"))
    val park = parked(ProcedureWalker.advance(ready, tree, None, powers))
    val done = finished(ProcedureWalker.resolve(ready, tree, park.tree,
      Answered(VowOfKinship.decisionId, ChooseAmountAnswer(2), p1), powers))
    val look = Look(done.treeless)
    assertEquals(look.favor(p1), 1)
    assertEquals(look.bankFavor(Suit.Nomad), 2)
    assertEquals(said(done.events), Vector(NoteText.Said("spent-from-bank",
      "Spent 2 favor from the Nomad bank.", covers = false)))
    Outcomes.assertWireRoundTrips(park.events ++ done.events)

  test("a cost walked as its moves asks too"):
    val ready = vowHolder(p1, board = 2, bank = 4).ready
    assertEquals(asked(ready, burning(p1, 3)).decisionId,
      VowOfKinship.decisionId)

  test("a cost walked as its moves draws what the answer says"):
    val ready = vowHolder(p1, board = 2, bank = 4).ready
    val tree = burning(p1, 3)
    val park = parked(ProcedureWalker.advance(ready, tree, None, powers))
    val done = finished(ProcedureWalker.resolve(ready, tree, park.tree,
      Answered(VowOfKinship.decisionId, ChooseAmountAnswer(3), p1), powers))
    assertEquals(Look(done.treeless).favor(p1), 2)
    assertEquals(Look(done.treeless).bankFavor(Suit.Nomad), 1)

  test("an answer of none pays from the board and writes no line"):
    val ready = vowHolder(p1, board = 3, bank = 4).ready
    val tree = batchOf(burning(p1, 2))
    val park = parked(ProcedureWalker.advance(ready, tree, None, powers))
    val done = finished(ProcedureWalker.resolve(ready, tree, park.tree,
      Answered(VowOfKinship.decisionId, ChooseAmountAnswer(0), p1), powers))
    assertEquals(Look(done.treeless).favor(p1), 1)
    assertEquals(Look(done.treeless).bankFavor(Suit.Nomad), 4)
    assertEquals(said(done.events), Vector.empty)

  test("one question covers every spend of the batch, capped by the bank"):
    // Board 1, bank 2, spends of 1 and 2: 3 in all, of which 2 to 2 may
    // come from the bank, so least < most fails and nothing is asked; with
    // a bank of 3, 2 to 3 may.
    val spends = batchOf(burning(p1, 1), burning(p1, 2))
    val none = finished(ProcedureWalker.advance(
      vowHolder(p1, board = 1, bank = 2).ready, spends, None, powers))
    assertEquals(Look(none.treeless).bankFavor(Suit.Nomad), 0)
    asked(vowHolder(p1, board = 1, bank = 3).ready, spends).query match
      case amount: DecisionQuery.ChooseAmount =>
        assertEquals((amount.min, amount.max), (2, 3))
      case other => fail(s"expected an amount, got $other")

  test("a take from the board before the spend counts against the board"):
    // Board 2, bank 4: p2 takes 1 first, so of the spend of 2 at least 1
    // comes from the bank.
    val taken = Take(Piece.Favor(1), p2, Location.PlayArea(p1),
      Location.PlayArea(p2))
    asked(vowHolder(p1, board = 2, bank = 4).ready,
        batchOf(taken, burning(p1, 2))).query match
      case amount: DecisionQuery.ChooseAmount =>
        assertEquals((amount.min, amount.max), (1, 2))
      case other => fail(s"expected an amount, got $other")

  test("with only one source, nothing is asked"):
    val bankOnly = finished(ProcedureWalker.advance(
      vowHolder(p1, board = 0, bank = 4).ready, batchOf(burning(p1, 3)),
      None, powers))
    assertEquals(Look(bankOnly.treeless).bankFavor(Suit.Nomad), 1)
    val boardOnly = finished(ProcedureWalker.advance(
      vowHolder(p1, board = 4, bank = 0).ready, batchOf(burning(p1, 3)),
      None, powers))
    assertEquals(Look(boardOnly.treeless).favor(p1), 1)

  test("a board that must give all it has still asks only for the rest"):
    // Board 1, bank 4, a spend of 3: at least 2 come from the bank.
    asked(vowHolder(p1, board = 1, bank = 4).ready,
        batchOf(burning(p1, 3))).query match
      case amount: DecisionQuery.ChooseAmount =>
        assertEquals((amount.min, amount.max), (2, 3))
      case other => fail(s"expected an amount, got $other")

  test("an off-turn holder's line names them"):
    val ready = vowHolder(p2, board = 1, bank = 4).favor(p1, 0).ready
    val tree = batchOf(Give(Piece.Favor(2), p2, Location.PlayArea(p2),
      Location.PlayArea(p1), required = true))
    assertEquals(asked(ready, tree).owner, p2)
    val park = parked(ProcedureWalker.advance(ready, tree, None, powers))
    val done = finished(ProcedureWalker.resolve(ready, tree, park.tree,
      Answered(VowOfKinship.decisionId, ChooseAmountAnswer(2), p2), powers))
    assertEquals(Look(done.treeless).favor(p2), 1)
    assertEquals(Look(done.treeless).favor(p1), 2)
    assertEquals(said(done.events), Vector(NoteText.Said(
      "spent-from-bank-off-turn",
      s"${p2.value} spent 2 favor from the Nomad bank.", covers = false)))

  test("turned faceup, it moves its holder's board favor to the Nomad " +
      "bank"):
    // p1 holds the Vow facedown with 3 favor; revealing it is an arrival.
    val ready = Table.start.adviser(p1, VowOfKinshipCard, facedown = true)
      .favor(p1, 3).bankFavor(Suit.Nomad, 1).ready
    val done = finished(ProcedureWalker.advance(ready,
      Reveal(VowOfKinshipCard.id, Location.PlayArea(p1)), None, powers))
    val look = Look(done.treeless)
    assertEquals(look.favor(p1), 0)
    assertEquals(look.bankFavor(Suit.Nomad), 4)
    assertEquals(said(done.events), Vector(NoteText.Said("moved",
      "Moved 3 favor to the Nomad bank.", covers = false)))

  test("an arrival with no favor on the board moves nothing and says " +
      "nothing"):
    val ready = Table.start.adviser(p1, VowOfKinshipCard, facedown = true)
      .favor(p1, 0).ready
    val done = finished(ProcedureWalker.advance(ready,
      Reveal(VowOfKinshipCard.id, Location.PlayArea(p1)), None, powers))
    assertEquals(said(done.events), Vector.empty)
