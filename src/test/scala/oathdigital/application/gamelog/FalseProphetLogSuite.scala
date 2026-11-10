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

  /** `claimRevealed`, where p2 owns Sanctuary, which p3 claims, so p2's
    * reveal of Conquest discards it and p3 reveals it. */
  private def nestedClaimRevealed: Vector[String] =
    val table = Table.start.pawn(p1, AncientCitySite)
      .pawn(p2, BrokenPeaksSite).supply(p1, 5)
      .revealedVision(p1, VisionRules.Conquest)
      .warbandsOnCard(VisionRules.Conquest, p2, 1)
      .revealedVision(p2, VisionRules.Sanctuary)
      .warbandsOnCard(VisionRules.Sanctuary, p3, 1)
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
    val conquest = accepted(service.handle("g", faith.nextSequence,
      Commands.resolveWalker(p2, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Conquest),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p2))))))
    accepted(service.handle("g", conquest.nextSequence,
      Commands.resolveWalker(p3, TreeDecision(
        CardPlayProcedure.placementDecisionId(VisionRules.Sanctuary),
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p3))))))
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

  test("a nested claim reads in walk order: the inner claim whole, then " +
      "the outer claimant's reveal and kill, then the owner's reveal"):
    // A play's own line follows the follow-ups walked under its batch, so
    // p3's claim, started by p2's reveal, reads before p2's reveal line.
    val log = nestedClaimRevealed
    def at(line: String => Boolean): Int =
      val index = log.indexWhere(line)
      assert(index >= 0, log.toString)
      index
    val order = Vector(
      at(_.endsWith(" revealed Vision of Sanctuary")),
      at(_.endsWith("'s warband on Vision of Sanctuary.")),
      at(_.endsWith(" revealed Vision of Conquest")),
      at(_.endsWith("'s warband on Vision of Conquest.")),
      at(_.startsWith("Revealed Vision of Faith")))
    assertEquals(order, order.sorted, log.toString)
