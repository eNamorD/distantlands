package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  FestivalDistrictCard, NegotiationAdvisers, NegotiationDeal,
  NegotiationStartFacts}
import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.powers.action.AssassinCard
import distantlands.gameplay.powers.economy.BirdsongCard
import distantlands.gameplay.powers.rest.SilverTongueCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, ChooseManyAnswer, ProposeTerms}
import distantlands.model.OathState.Ready
import distantlands.testkit.{Look, Outcomes, ParkedNode, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Adviser terms (catalog batch 12, rulings 14 to 17): who may offer
  * them, which advisers, the adviser limit, and what settling moves. */
class NegotiationAdviserTermsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val site = BrokenPeaksSite.id
  private val arbiter: WorldCardId = CouncilArbiterCard.id
  private val assassin: WorldCardId = AssassinCard.id
  private val birdsong: WorldCardId = BirdsongCard.id

  /** Every player at Broken Peaks with 5 favor. p1 holds Council Arbiter
    * faceup; p2 holds Birdsong facedown. */
  private def arbitrated: Table = Table.start
    .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
    .favor(p1, 5).favor(p2, 5).favor(p3, 5)
    .adviser(p1, CouncilArbiterCard.id)
    .adviser(p2, BirdsongCard.id, facedown = true)

  /** p1's Negotiation with `who`, parked on the deal. */
  private def atDeal(table: Table, who: PlayerId*): OathState =
    val started = rules.startWalker(Ready(table.ready), ActionRef.Negotiation,
      p1).getOrElse(fail("Negotiation must start"))
    rules.resolveWalker(started.state, p1,
      NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(
        who.toVector.map(DecisionOptionRef.Player(_))))
      .getOrElse(fail("the negotiators must be accepted")).state

  private def say(state: OathState, by: PlayerId, answer: DecisionAnswer) =
    rules.resolveWalker(state, by, NegotiationDeal.dealDecisionId, answer)

  /** `by` offers `to` exactly `advisers`. */
  private def offer(to: PlayerId, advisers: WorldCardId*) =
    ProposeTerms(NegotiationTerms(Vector(NegotiationTransfer(to, 0,
      Vector.empty, advisers.toVector))))

  /** The deal `state` is parked on. */
  private def parkedDeal(state: OathState): DecisionQuery.Negotiate =
    ParkedNode.of(state, catalog) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide.query match
        case negotiate: DecisionQuery.Negotiate => negotiate
        case other => fail(s"not a deal: $other")
      case other => fail(s"not parked on a decision: $other")

  /** Proposes `p1`'s and `p2`'s terms in a two-party deal, then has both
    * accept. */
  private def agreed(table: Table, p1Terms: DecisionAnswer,
      p2Terms: DecisionAnswer): OathState =
    Vector(p1 -> p1Terms, p2 -> p2Terms, p2 -> AcceptDeal, p1 -> AcceptDeal)
      .foldLeft(atDeal(table, p2))((state, step) =>
        say(state, step._1, step._2).getOrElse(fail(s"$step is refused"))
          .state)

  test("Council Arbiter opens adviser terms only where its holder gives " +
      "or receives"):
    val bounds = parkedDeal(atDeal(arbitrated, p2, p3)).bounds
    assertEquals(bounds(p1).adviserRecipients, Vector(p2, p3))
    assertEquals(bounds(p2).adviserRecipients, Vector(p1))
    assertEquals(bounds(p3).adviserRecipients, Vector(p1))
    val dealt = atDeal(arbitrated, p2, p3)
    assert(say(dealt, p2, offer(p3, birdsong)).isLeft)
    assert(say(dealt, p2, offer(p1, birdsong)).isRight)

  test("the start stores Council Arbiter's holder"):
    val started = rules.startWalker(Ready(arbitrated.ready),
      ActionRef.Negotiation, p1).getOrElse(fail("Negotiation must start"))
    assertEquals(Outcomes.ready(started.state).game.current.walkerStartFacts,
      NegotiationStartFacts(Vector(p2, p3), Vector.empty, Vector(p1),
        Vector.empty).refs)

  test("without Council Arbiter or Festival District no adviser is offered"):
    val plain = Table.start.pawn(p1, at = site).pawn(p2, at = site)
      .pawn(p3, at = site).adviser(p2, BirdsongCard.id, facedown = true)
    val bounds = parkedDeal(atDeal(plain, p2)).bounds
    assert(bounds.values.forall(b => b.adviserRecipients.isEmpty &&
      b.advisers.isEmpty), bounds)
    assert(say(atDeal(plain, p2), p2, offer(p1, birdsong)).isLeft)

  test("Festival District opens advisers between every pair"):
    val festival = Table.start.pawn(p1, at = site).pawn(p2, at = site)
      .pawn(p3, at = site).adviser(p2, BirdsongCard.id, facedown = true)
      .edifice(FestivalDistrictCard, EdificeSide.Intact, at = site)
    val bounds = parkedDeal(atDeal(festival, p2, p3)).bounds
    assertEquals(bounds(p2).adviserRecipients, Vector(p1, p3))
    assertEquals(bounds(p3).adviserRecipients, Vector(p1, p2))
    assertEquals(bounds(p2).advisers, Vector(birdsong))

  test("a locked faceup adviser is never offered"):
    // Assassin is locked, and lowers p1's limit to 2.
    val armed = arbitrated.adviser(p1, AssassinCard.id)
    assertEquals(parkedDeal(atDeal(armed, p2)).bounds(p1).advisers,
      Vector(arbiter))
    assert(say(atDeal(armed, p2), p1, offer(p2, assassin)).isLeft)

  test("terms over a limit leave nobody able to accept, and name who"):
    // p2 holds Silver Tongue faceup (a limit of 2) and Birdsong.
    val tongue = arbitrated.adviser(p2, SilverTongueCard.id)
    val dealt = atDeal(tongue, p2)
    val over = say(dealt, p1, offer(p2, arbiter)).getOrElse(fail("terms"))
      .state
    assertEquals(parkedDeal(over).overAdviserLimit, Vector(p2))
    assertEquals(parkedDeal(over).acceptors, Set.empty[PlayerId])
    assert(say(over, p2, AcceptDeal).isLeft)
    // A swap keeps p2 at two.
    val swapped = say(over, p2, offer(p1, birdsong)).getOrElse(fail("swap"))
      .state
    assertEquals(parkedDeal(swapped).overAdviserLimit, Vector.empty[PlayerId])
    assertEquals(parkedDeal(swapped).acceptors, Set(p1, p2))

  test("a facedown adviser moves facedown, and its giver still knows it"):
    val settled = Outcomes.ready(agreed(arbitrated, offer(p2),
      offer(p1, birdsong)))
    val look = Look(settled)
    assert(look.player(p1).advisers.contains(
      DenizenState(BirdsongCard.id, Orientation.FaceDown, Tokens.empty)),
      look.player(p1).advisers)
    assert(!look.player(p2).advisers.exists(_.id == birdsong))
    assert(settled.knowledge.advisers.getOrElse(p2, Vector.empty)
      .contains(birdsong), settled.knowledge)

  test("a faceup adviser keeps its power with its new holder"):
    val settled = Outcomes.ready(agreed(arbitrated, offer(p2, arbiter),
      offer(p1, birdsong)))
    assertEquals(NegotiationAdvisers.arbiterUsers(catalog, settled,
      Vector(p1, p2, p3), Vector.empty), Vector(p2))
