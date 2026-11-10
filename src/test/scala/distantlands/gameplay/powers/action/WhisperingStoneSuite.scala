package distantlands.gameplay.powers.action

import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  NegotiationDeal, NegotiationProcedure}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.ParkedDecisionAssertions
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseManyAnswer
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay, ParkedNode,
  Table}
import distantlands.testkit.Outcomes.ready
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Whispering Stone (relic R34): "**ACTION:** Negotiate with any one
  * player in private, even if their pawn is not at your site." (catalog
  * batch 12, rulings 21 to 23). */
class WhisperingStoneSuite extends munit.FunSuite:
  private val rules = ActPowerHarness.rules()
  private val parked = new ParkedDecisionAssertions(catalog)
  private val stone = WhisperingStoneCard.id
  private val source = DecisionOptionRef.Relic(stone)

  /** p1 at Ancient City holds Whispering Stone with 1 secret to pay; p2
    * stands at Broken Peaks and p3 at Buried Giant, so no pawn shares p1's
    * site. */
  private def strangers: Table = Table.start
    .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
    .pawn(p3, BuriedGiantSite).relic(p1, stone).secrets(p1, faceUp = 1)

  /** The query `state` is parked on. */
  private def parkedQuery(state: OathState)(using munit.Location)
      : DecisionQuery = ParkedNode.of(state, catalog) match
    case Right(Some(ParkedNode.Decision(_, decide, _))) => decide.query
    case other => fail(s"not parked on a decision: $other")

  /** The Stone used, then `partner` chosen: parked on the deal. */
  private def whisperingWith(table: Table, partner: PlayerId)(
      using munit.Location): OathState =
    val used = accepted(ActPowerHarness.use(rules, table.ready,
      WhisperingStone.id, source))
    accepted(rules.resolveWalker(used.state, p1,
      NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(Vector(
        DecisionOptionRef.Player(partner))))).state

  test("the Stone asks for exactly one other player, wherever their pawn " +
      "is"):
    val used = accepted(ActPowerHarness.use(rules, strangers.ready,
      WhisperingStone.id, source))
    parked.assertParked(used.state, ActionRef.Negotiation,
      NegotiationDeal.negotiatorsDecisionId, p1)
    parkedQuery(used.state) match
      case many: DecisionQuery.ChooseMany =>
        assertEquals((many.min, many.max), (1, 1))
        assertEquals(many.options.map(_.ref),
          Vector(p2, p3).map(DecisionOptionRef.Player(_)))
      case other => fail(s"not the negotiators question: $other")

  test("the Stone costs 1 secret, paid onto it"):
    val used = accepted(ActPowerHarness.use(rules, strangers.ready,
      WhisperingStone.id, source))
    val look = Look(ready(used.state))
    assertEquals(look.faceUpSecrets(p1), 0)
    assertEquals(look.tokensOn(WhisperingStoneCard).secrets, 1)

  test("the chosen player joins a private deal"):
    val state = whisperingWith(strangers, p3)
    parked.assertParked(state, ActionRef.Negotiation,
      NegotiationDeal.dealDecisionId, p1)
    parkedQuery(state) match
      case deal: DecisionQuery.Negotiate =>
        assertEquals(deal.participants, Vector(p1, p3))
        assert(deal.privateDeal)
      case other => fail(s"not a deal: $other")

  test("a Negotiation from the menu may not carry the private marker"):
    assertEquals(rules.startWalker(
        OathState.Ready(MenuPlay.withoutMenu(strangers.ready)),
        ActionRef.Negotiation, p1, NegotiationProcedure.privateArgs)
      .left.toOption,
      Some(OathViolation.InvalidEventOrder("walker procedure negotiation " +
        "takes no start selection but a power's private marker, got " +
        "button")))

  test("Council Arbiter widens the private deal"):
    val state = whisperingWith(strangers.adviser(p1, CouncilArbiterCard.id),
      p3)
    parkedQuery(state) match
      case deal: DecisionQuery.Negotiate =>
        assertEquals(deal.bounds(p3).adviserRecipients, Vector(p1))
      case other => fail(s"not a deal: $other")
