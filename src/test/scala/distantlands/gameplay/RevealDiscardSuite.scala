package oathdigital.gameplay

import oathdigital.gameplay.operations.{OperationPipeline, OperationPolicy}
import oathdigital.gameplay.powers.campaign.{RainBootsCard, WrestlersCard}
import oathdigital.gameplay.powers.search.ErrandBoyCard
import oathdigital.model._
import oathdigital.testkit.Table

/** Revealing the top card of a regional discard shows it to the table: a
  * discarded card has no orientation state, so only what the players know
  * changes, and a facedown flip of it is still not accepted.
  */
class RevealDiscardSuite extends munit.FunSuite:
  private val region = Region.Cradle
  private val at = Location.RegionalDiscard(region)
  /** Errand Boy tops the Cradle's discard; Wrestlers lies in the Provinces'. */
  private val base = Table.start
    .discarded(Region.Cradle, RainBootsCard, ErrandBoyCard)
    .discarded(Region.Provinces, WrestlersCard)
    .ready
  private val top = ErrandBoyCard.id
  private def run(ops: CoreOperation*) = OperationPipeline.run(base,
    ops.toVector, OperationPolicy.Permissive, Vector.empty)(Right(_))

  test("revealing a discarded card is accepted and tells every player the card"):
    val result = run(Reveal(top, at)).toOption.get
    val players = base.game.current.players.map(_.player)
    assertEquals(result.state.game, base.game)
    assertEquals(result.state.knowledge.advisers,
      players.map(_ -> Vector[WorldCardId](top)).toMap)
    assertEquals(result.executed, Vector(Reveal(top, at)))

  test("a card that is not in that discard cannot be revealed there"):
    val other = WrestlersCard.id
    assertEquals(run(Reveal(other, at)).left.toOption, Some(OathViolation.CoreOperationRejected("missing-piece",
      "denizen card is not present at regional discard")))

  test("a facedown flip of a discarded card stays unsupported"):
    assertEquals(run(Flip(top, at, Orientation.FaceDown)).left.toOption, Some(OathViolation.CoreOperationRejected(
      "unsupported-orientation",
      "card kind denizen cannot use orientation at RegionalDiscard(Cradle)")))
