package oathdigital.application.gamelog

import oathdigital.application.{GameApplicationService,
  ParkedServiceFixture, TreeDecision}
import oathdigital.gameplay.actions.VisionRules
import oathdigital.gameplay.actions.cardplay.CardPlayProcedure
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.powers.whenplayed.FalseProphetCard
import oathdigital.model._
import oathdigital.testkit.{Commands, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.{accepted, present}
import LogScripts.formatter

/** False Prophet's lines as the Game Log shows them (catalog batch 11,
  * ruling 34). */
class FalseProphetLogSuite extends munit.FunSuite:
  /** p1 owns Conquest, which p2 claims, with False Prophet faceup among
    * p2's advisers; p1 Searches, reveals Faith over
    * it, and p2 reveals Conquest. The log as p3 sees it, replayed from
    * the stored history. */
  private def claimRevealed: Vector[String] =
    val table = Table.start.pawn(p1, AncientCitySite)
      .pawn(p2, BrokenPeaksSite).supply(p1, 5)
      .revealedVision(p1, VisionRules.Conquest)
      .warbandsOnCard(VisionRules.Conquest, p2, 1)
      .adviser(p2, FalseProphetCard.id).worldDeckTop(VisionRules.Faith)
    val (service, _) = table.service()
    val started = accepted(service.handle("g", 0L,
      ParkedServiceFixture.startSearch(p1)))
    val picked = accepted(service.handle("g", started.nextSequence,
      ParkedServiceFixture.pickSearchSource(p1, SearchSource.WorldDeck)))
    val faith = accepted(service.handle("g", picked.nextSequence,
      Commands.resolveWalker(p1, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Faith),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p1))))))
    accepted(service.handle("g", faith.nextSequence,
      Commands.resolveWalker(p2, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Conquest),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p2))))))
    texts(service)

  private def texts(service: GameApplicationService): Vector[String] =
    formatter.format(present(accepted(service.history("g")),
      "a stored game").steps, Some(p3)).map(LogScripts.text)

  test("the claimant's reveal names them, then the kill names them"):
    val log = claimRevealed
    val reveal = log.indexWhere(_.endsWith(" revealed Vision of Conquest"))
    assert(reveal >= 0, log.toString)
    val killed = log.lift(reveal + 1).getOrElse("")
    assert(killed.startsWith("False Prophet: Killed "), log.toString)
    assert(killed.endsWith("'s warband on Vision of Conquest."), log.toString)
    assert(!log.exists(_.startsWith("Revealed Vision of Conquest")),
      log.toString)
