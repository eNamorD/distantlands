package oathdigital.gameplay.powers.whenplayed

import oathdigital.gameplay.RevealedVisions
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.powers.NoteText
import oathdigital.model._
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

class FalseProphetSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val power = registered[FalseProphet.type]
  private val card = power.cardId
  private val played = hook(card)

  private def visionOptions(visions: VisionId*): Vector[DecisionOption] =
    visions.toVector.map(vision =>
      DecisionOption.Vision(DecisionOptionRef.Vision(vision)))

  private def chose(vision: VisionId): DecisionAnswer =
    chooseOne(DecisionOptionRef.Vision(vision))

  /** p1 holds False Prophet; p2 has revealed Faith. */
  private def faithRevealedByP2: Table = Table.start.adviser(p1, card)
    .revealedVision(p2, VisionRules.Faith)

  test("an Exile gains a warband and puts it on another player's Vision, " +
      "which is then theirs too"):
    val ready = faithRevealedByP2.ready
    val board = Look(ready).player(p1).board.warbands
    val first = Outcomes.parked(play(ready, power, card))
    val asked = question(ready, power, played, first.tree)
    assertEquals(asked.owner, p1)
    assertEquals(chooseOneOptions(asked), visionOptions(VisionRules.Faith))
    val done = Outcomes.finished(resume(ready, power, played, first.tree,
      FalseProphet.decisionId, chose(VisionRules.Faith)))
    val after = done.treeless
    assertEquals(Look(after).player(p1).board.warbands, board)
    assertEquals(after.game.current.warbandsOn(VisionRules.Faith,
      ForceKind.Exile(Look(after).player(p1).lineage)), 1)
    assertEquals(RevealedVisions.of(after, p1),
      Vector[VisionId](VisionRules.Faith))
    assertEquals(said(power, done.events), Vector(NoteText.Said("put",
      s"Put a warband on ${p2.value}'s ${VisionRules.Faith.value}.",
      covers = true)))
    assertJournalRebuilds(ready, done.events, after)

  test("every other player's revealed Vision is offered, never the " +
      "player's own"):
    val ready = faithRevealedByP2.revealedVision(p1, VisionRules.Conquest)
      .revealedVision(p3, VisionRules.Sanctuary).ready
    val first = Outcomes.parked(play(ready, power, card))
    assertEquals(chooseOneOptions(question(ready, power, played, first.tree)),
      visionOptions(VisionRules.Faith, VisionRules.Sanctuary))

  test("a Citizen gains nothing"):
    val ready = faithRevealedByP2.imperial(p3, p1).ready
    val done = Outcomes.finished(play(ready, power, card))
    assertEquals(ops(done.events), Vector.empty)
    assertEquals(said(power, done.events), Vector.empty)

  test("an Exile with no warband in their bank gains nothing"):
    val ready = faithRevealedByP2.warbandsInBank(p1, 0).ready
    val done = Outcomes.finished(play(ready, power, card))
    assertEquals(ops(done.events), Vector.empty)

  test("with no other player's revealed Vision nothing happens"):
    val ready = Table.start.adviser(p1, card)
      .revealedVision(p1, VisionRules.Conquest).ready
    val done = Outcomes.finished(play(ready, power, card))
    assertEquals(ops(done.events), Vector.empty)
