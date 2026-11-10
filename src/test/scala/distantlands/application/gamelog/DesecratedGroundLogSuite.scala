package oathdigital.application.gamelog

import oathdigital.application.{GameApplicationService,
  ParkedServiceFixture, TreeDecision}
import oathdigital.gameplay.SacredGroundCard
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.model._
import oathdigital.testkit.{Commands, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.{accepted, present}
import LogScripts.formatter

/** The kept line as the Game Log shows it (catalog batch 11, ruling 31). */
class DesecratedGroundLogSuite extends munit.FunSuite:
  /** p1 holds Conquest revealed, stands at Ancient City where E08 lies
    * ruined, Searches the world deck and reveals Faith. The log as p2
    * sees it. */
  private def revealedFaithKeepingConquest: Vector[String] =
    val table = Table.start.pawn(p1, AncientCitySite).supply(p1, 5)
      .edifice(SacredGroundCard.id, EdificeSide.Ruined, at = AncientCitySite.id)
      .revealedVision(p1, VisionRules.Conquest)
      .worldDeckTop(VisionRules.Faith)
    val (service, _) = table.service()
    val started = accepted(service.handle("g", 0L,
      ParkedServiceFixture.startSearch(p1)))
    val picked = accepted(service.handle("g", started.nextSequence,
      ParkedServiceFixture.pickSearchSource(p1, SearchSource.WorldDeck)))
    accepted(service.handle("g", picked.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Faith),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p1))))))
    texts(service)

  private def texts(service: GameApplicationService): Vector[String] =
    formatter.format(present(accepted(service.history("g")),
      "a stored game").steps, Some(p2)).map(LogScripts.text)

  test("a kept reveal reads as the reveal, then the kept Vision"):
    val log = revealedFaithKeepingConquest
    val reveal = log.indexWhere(_.startsWith("Revealed Vision of Faith"))
    assert(reveal >= 0, log.toString)
    assertEquals(log.lift(reveal + 1),
      Some("Desecrated Ground: Kept Vision of Conquest."), log.toString)
