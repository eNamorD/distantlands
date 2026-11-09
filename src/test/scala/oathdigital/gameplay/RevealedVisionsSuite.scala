package oathdigital.gameplay

import oathdigital.gameplay.actions.VisionRules
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.{p1, p2}

/** The victory read path and the Visions that qualify (catalog batch 11,
  * slice 6). */
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
