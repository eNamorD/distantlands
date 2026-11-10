package distantlands.application

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite}
import distantlands.gameplay.phases.PhaseMenu
import distantlands.gameplay.powers.action.{RovingTerror, RovingTerrorCard}
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.poweruse.{SpellBreaker, SpellBreakerCard}
import distantlands.model._
import distantlands.model.OathEvent.{ConsentAnswered, ConsentRequested}
import distantlands.testkit.{Commands, MenuPlay, Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Asking Spell Breaker's ruler about a phase power the Act menu shows
  * askable, through the service: the menu wraps every option in its band,
  * and the request reads the option inside it (catalog batch 10, slice
  * 13). */
class SpellBreakerConsentServiceSuite extends munit.FunSuite:
  private val gameId = "spell-breaker-consent"
  private val home = AncientCitySite.id
  private val woods = DeepWoodsSite.id
  private val terrorRef = DecisionOptionRef.Denizen(RovingTerrorCard.id)

  /** p1's Act at Ancient City beside Roving Terror, with Wrestlers at
    * Broken Peaks to replace, and Spell Breaker at Deep Woods, ruled by
    * p2. */
  private val terrorBesideBreakerRuledByP2: Table = Table.start
    .turn(p1, Phase.Act).pawn(p1, at = home)
    .denizen(RovingTerrorCard.id, at = home)
    .denizen(WrestlersCard.id, at = BrokenPeaksSite.id)
    .denizen(SpellBreakerCard.id, at = woods).warbandsAt(woods, p2, 1)

  test("p1 asks p2 about Roving Terror from the Act menu, and Yes starts " +
      "it"):
    val (service, _) = terrorBesideBreakerRuledByP2.service()
    val asked = accepted(service.handle(gameId, 0L,
      Commands.requestConsent(p1, PhaseMenu.decisionId, terrorRef)))
    asked.events match
      case Vector(ConsentRequested(request)) =>
        assertEquals((request.target, request.asks), (terrorRef,
          Vector(ConsentAsk(p2, Vector(SpellBreaker.id)))))
      case other => fail(s"expected one request, got $other")
    val yes = accepted(service.handle(gameId, asked.nextSequence,
      Commands.answerConsent(p2, ConsentChoice.Yes)))
    assertEquals(yes.events.head, ConsentAnswered(p2, true))
    assertEquals(MenuPlay.procedureBeyondMenu(Outcomes.ready(yes.state)),
      Some(ActionRef.UsePower(RovingTerror.id)))
