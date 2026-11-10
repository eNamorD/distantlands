package distantlands.gameplay.powers.economy

import distantlands.gameplay.{NoteSupport, VowOfKinshipCard}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.actions.economy.TradeProcedure
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.model._
import distantlands.testkit.{EconomyActions, Look, PowerRules, Table}
import distantlands.testkit.Table.{p1, p2}

/** A Vow of Kinship holder's favor lines (catalog batch 11, ruling 29):
  * they count the Nomad bank with the board, and a gain line says the
  * favor went to the bank. */
class VowOfKinshipLinesSuite extends munit.FunSuite:
  /** p1 holds the Vow with an empty board; the Nomad bank holds 2 and the
    * Order bank 3. */
  private val vowHolder: Table = Table.start.adviser(p1, VowOfKinshipCard)
    .favor(p1, 0).bankFavor(Suit.Nomad, 2).bankFavor(Suit.Order, 3)

  /** The states around `ops`, run as one batch. */
  private def step(ready: ReadyGame, ops: CoreOperation*)(
      using munit.Location): NoteSupport.Step =
    OperationPipeline.run(ready, ops.toVector, OperationPolicy.Permissive,
      Vector.empty)(Right(_)).fold(violation => fail(violation.toString),
      run => (ready, run.state))

  test("a gain counts as the holder's and is marked for the bank"):
    val gained = step(vowHolder.ready, Gain.Favor(p1, Suit.Order, 2))
    assertEquals(NoteSupport.favor(gained, p1), 2)
    assert(NoteSupport.banked(gained, p1))
    val note = LogNote(None, "gained", Vector(NoteArg.Number(2)))
    assertEquals(NoteSupport.toBank(gained, p1)(note).args,
      Vector(NoteArg.Number(2), NoteArg.ToNomadBank))

  test("a spend from the bank counts as paid, and is not marked"):
    val paid = step(vowHolder.ready, DrawNomadFavor(p1, 1),
      PayCost(p1, Location.SharedBank, Cost(favorBurnt = 1)))
    assertEquals(NoteSupport.favor(paid, p1), -1)
    assert(!NoteSupport.banked(paid, p1))

  test("another player's gain is neither counted for the holder nor " +
      "marked"):
    val gained = step(vowHolder.ready, Gain.Favor(p2, Suit.Order, 2))
    assertEquals(NoteSupport.favor(gained, p1), 0)
    assert(!NoteSupport.banked(gained, p2))

  test("a holder's Trade for favor says the favor went to the Nomad bank"):
    val wrestlers = WrestlersCard.id
    val ready = vowHolder.pawn(p1, at = AncientCitySite.id)
      .denizen(wrestlers, at = AncientCitySite.id).secrets(p1, faceUp = 1)
      .supply(p1, 3).ready
    val done = EconomyActions.trade(PowerRules.withWalkerPowers, ready, p1,
      Vector.empty, "favor", DecisionOptionRef.Denizen(wrestlers))
    assertEquals(Look(done.result).favor(p1), 0)
    assertEquals(Look(done.result).bankFavor(Suit.Nomad), 3)
    val lines = NoteText.said(ProcedureNotes.id(ActionRef.Trade),
      TradeProcedure.noteKeys, done.journal.events).map(_.text)
    assert(lines.exists(_.endsWith(" for 1 favor, to the Nomad bank")),
      lines.toString)
