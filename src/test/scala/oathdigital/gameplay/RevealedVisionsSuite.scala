package oathdigital.gameplay

import oathdigital.gameplay.actions.VisionRules
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2, p3}

/** The victory read path and the Visions that qualify (catalog batch 11,
  * slices 6 and 7). */
class RevealedVisionsSuite extends munit.FunSuite:
  test("a player's revealed Visions are theirs, in reveal order"):
    val ready = Table.start.revealedVision(p1, VisionRules.Faith)
      .revealedVision(p1, VisionRules.Conquest).ready
    assertEquals(RevealedVisions.of(ready, p1),
      Vector[VisionId](VisionRules.Faith, VisionRules.Conquest))
    assertEquals(RevealedVisions.of(ready, p2), Vector.empty)

  test("only the Visions whose goal is met qualify"):
    // Faith's goal is met (the Darkest Secret); Conquest's is not.
    val ready = Table.start.visionsDrawn(3)
      .revealedVisionMet(p1, VisionRules.Faith)
      .revealedVision(p1, VisionRules.Conquest).ready
    assertEquals(VisionVictoryEligibility.qualifying(p1, ready),
      Vector[VisionId](VisionRules.Faith))

  test("nothing qualifies before three Visions are drawn"):
    val ready = Table.start.visionsDrawn(2)
      .revealedVisionMet(p1, VisionRules.Faith).ready
    assertEquals(VisionVictoryEligibility.qualifying(p1, ready), Vector.empty)

  test("a Vision that carries a player's warband is theirs too"):
    // p1's Faith carries one of p2's warbands.
    val ready = Table.start.revealedVision(p1, VisionRules.Faith)
      .warbandsOnCard(VisionRules.Faith, p2, 1).ready
    assertEquals(RevealedVisions.of(ready, p2),
      Vector[VisionId](VisionRules.Faith))
    assertEquals(RevealedVisions.of(ready, p1),
      Vector[VisionId](VisionRules.Faith))
    assertEquals(RevealedVisions.claimants(ready, VisionRules.Faith),
      Vector(p2))

  test("a claim is the claimant's lineage's, so a Citizen keeps it"):
    // p2's warband goes on before p2 turns Citizen: the card keeps it.
    val ready = Table.start.revealedVision(p1, VisionRules.Faith)
      .warbandsOnCard(VisionRules.Faith, p2, 1).imperial(p3, p2).ready
    assertEquals(RevealedVisions.of(ready, p2),
      Vector[VisionId](VisionRules.Faith))

  test("a warband on a Vision no one has revealed claims nothing"):
    val ready = Table.start.warbandsOnCard(VisionRules.Faith, p2, 1).ready
    assertEquals(RevealedVisions.of(ready, p2), Vector.empty)
    assertEquals(RevealedVisions.claimants(ready, VisionRules.Faith),
      Vector(p2))
