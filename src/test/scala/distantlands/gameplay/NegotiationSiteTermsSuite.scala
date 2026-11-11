package distantlands.gameplay

import distantlands.gameplay.actions.negotiation.{DeedWriterCard,
  NegotiationDeal, NegotiationSites, NegotiationStartFacts}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  DeepWoodsSite, DunesSite}
import distantlands.gameplay.powers.economy.VowOfWanderingCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, ChooseAmountAnswer,
  ChooseManyAnswer, ProposeTerms}
import distantlands.model.OathState.Ready
import distantlands.testkit.{Outcomes, ParkedNode, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Site terms (catalog batch 12, rulings 18 to 20 and 27 to 29): who may
  * offer which sites to whom, the warbands a recipient needs, and what
  * settling moves. */
class NegotiationSiteTermsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val site = BrokenPeaksSite.id
  private val dunes = DunesSite.id
  private val woods = DeepWoodsSite.id
  private val city = AncientCitySite.id

  /** Every player at Broken Peaks, each board holding its 3 warbands. p1
    * holds Deed Writer faceup and rules Deep Woods with 2 warbands; p2
    * rules Dunes with 2. */
  private def deeded: Table = Table.start
    .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
    .adviser(p1, DeedWriterCard.id)
    .warbandsAt(woods, p1, 2).warbandsAt(dunes, p2, 2)

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

  private def place(state: OathState, by: PlayerId, at: SiteId, n: Int) =
    rules.resolveWalker(state, by, NegotiationSites.warbandsDecisionId(
      NegotiationDeal.NegotiationPrefix, at), ChooseAmountAnswer(n))

  /** A row giving `to` exactly `sites`. */
  private def give(to: PlayerId, sites: SiteId*) =
    ProposeTerms(NegotiationTerms(Vector(NegotiationTransfer(to, 0,
      Vector.empty, Vector.empty, sites.toVector))))

  /** The question `state` is parked on. */
  private def parkedOn(state: OathState): Decide =
    ParkedNode.of(state, catalog) match
      case Right(Some(ParkedNode.Decision(_, decide, _))) => decide
      case other => fail(s"not parked on a decision: $other")

  private def parkedDeal(state: OathState): DecisionQuery.Negotiate =
    parkedOn(state).query match
      case negotiate: DecisionQuery.Negotiate => negotiate
      case other => fail(s"not a deal: $other")

  /** The id, owner and range of the placement question `state` is parked
    * on. */
  private def asked(state: OathState): (String, PlayerId, Int, Int) =
    val decide = parkedOn(state)
    decide.query match
      case amount: DecisionQuery.ChooseAmount =>
        (decide.decisionId, decide.owner, amount.min, amount.max)
      case other => fail(s"not an amount: $other")

  /** Runs `steps` in order from `state`, failing on any refusal. */
  private def run(state: OathState,
      steps: (PlayerId, DecisionAnswer)*): OathState =
    steps.foldLeft(state)((now, step) =>
      say(now, step._1, step._2).getOrElse(fail(s"$step is refused")).state)

  private def forces(state: OathState, at: SiteId): SiteForces =
    Outcomes.ready(state).game.current.map.sites(at).forces

  private def board(state: OathState, player: PlayerId): Int =
    Outcomes.ready(state).game.current.player(player).get.board.warbands

  private def ruler(state: OathState, at: SiteId): SiteRuler =
    val ready = Outcomes.ready(state)
    SiteRule.ruler(forces(state, at), ready.game.current.players)
      .getOrElse(fail(s"$at has no ruler"))

  test("Deed Writer opens site terms only where its holder gives or " +
      "receives, for the sites each author's warbands hold"):
    val bounds = parkedDeal(atDeal(deeded, p2, p3)).bounds
    assertEquals(bounds(p1).siteRecipients, Vector(p2, p3))
    assertEquals(bounds(p2).siteRecipients, Vector(p1))
    assertEquals(bounds(p3).siteRecipients, Vector(p1))
    assertEquals(bounds(p1).sites, Vector(woods))
    assertEquals(bounds(p2).sites, Vector(dunes))
    val dealt = atDeal(deeded, p2, p3)
    assert(say(dealt, p2, give(p3, dunes)).isLeft)
    assert(say(dealt, p2, give(p1, dunes)).isRight)

  test("the start stores Deed Writer's holder"):
    val started = rules.startWalker(Ready(deeded.ready),
      ActionRef.Negotiation, p1).getOrElse(fail("Negotiation must start"))
    assertEquals(Outcomes.ready(started.state).game.current.walkerStartFacts,
      NegotiationStartFacts(Vector(p2, p3), Vector.empty, Vector.empty,
        Vector.empty, Vector(p1)).refs)

  test("a bandit site, or another player's, is never offered"):
    val held = deeded.bandits(city, 2)
    assertEquals(parkedDeal(atDeal(held, p2)).bounds(p1).sites,
      Vector(woods))
    assert(say(atDeal(held, p2), p1, give(p2, dunes)).isLeft)
    assert(say(atDeal(held, p2), p1, give(p2, city)).isLeft)

  test("two Imperial players are offered no site between them"):
    // p3 is the Chancellor and p2 a Citizen holding Deed Writer.
    val empire = Table.start.imperial(p3, p2)
      .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
      .adviser(p2, DeedWriterCard.id).warbandsAt(dunes, p2, 2)
    val bounds = parkedDeal(atDeal(empire, p2, p3)).bounds
    assertEquals(bounds(p2).siteRecipients, Vector(p1))
    assertEquals(bounds(p3).siteRecipients, Vector.empty[PlayerId])

  test("a Citizen gives an Imperial site on an Exile's turn, which the " +
      "Chancellor may then not offer too"):
    // p1, the actor, is an Exile holding Deed Writer. p3 is the Chancellor
    // and p2 a Citizen; Dunes holds 2 Imperial warbands.
    val empire = Table.start.imperial(p3, p2)
      .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
      .adviser(p1, DeedWriterCard.id).warbandsAt(dunes, p2, 2)
    val offered = run(atDeal(empire, p2, p3), p2 -> give(p1, dunes))
    assertEquals(parkedDeal(offered).bounds(p3).sites, Vector.empty[SiteId])
    assert(say(offered, p3, give(p1, dunes)).isLeft)
    val settled = run(offered, p1 -> AcceptDeal, p2 -> AcceptDeal,
      p3 -> AcceptDeal)
    assertEquals(asked(settled),
      (NegotiationSites.warbandsDecisionId(NegotiationDeal.NegotiationPrefix,
        dunes), p1, 1, 3))
    val placed = place(settled, p1, dunes, 2).getOrElse(fail("placed")).state
    assertEquals(board(placed, p2), 5)
    assertEquals(ruler(placed, dunes), SiteRuler.Player(p1))

  test("a Vow of Wandering holder is offered no site"):
    val vowed = deeded.adviser(p2, VowOfWanderingCard.id)
    assertEquals(parkedDeal(atDeal(vowed, p2, p3)).bounds(p1).siteRecipients,
      Vector(p3))

  test("a recipient with an empty board leaves nobody able to accept, " +
      "until a swap returns their warbands"):
    val empty = deeded.boardWarbands(p2, 0)
    val short = run(atDeal(empty, p2), p1 -> give(p2, woods))
    assertEquals(parkedDeal(short).shortOfWarbands, Vector(p2))
    assertEquals(parkedDeal(short).acceptors, Set.empty[PlayerId])
    assert(say(short, p2, AcceptDeal).isLeft)
    val swapped = run(short, p2 -> give(p1, dunes))
    assertEquals(parkedDeal(swapped).shortOfWarbands, Vector.empty[PlayerId])
    assertEquals(parkedDeal(swapped).acceptors, Set(p1, p2))

  test("a swap returns both old rulers' warbands, asks each new ruler 1 " +
      "to N, and passes rule"):
    val agreed = run(atDeal(deeded, p2), p1 -> give(p2, woods),
      p2 -> give(p1, dunes), p1 -> AcceptDeal, p2 -> AcceptDeal)
    // Both boards hold 3 + 2 once the returns run; p1's row comes first.
    assertEquals(asked(agreed),
      (NegotiationSites.warbandsDecisionId(NegotiationDeal.NegotiationPrefix,
        woods), p2, 1, 5))
    assertEquals(forces(agreed, woods), SiteForces.Empty)
    assert(place(agreed, p2, woods, 6).isLeft)
    val first = place(agreed, p2, woods, 4).getOrElse(fail("woods")).state
    assertEquals(asked(first),
      (NegotiationSites.warbandsDecisionId(NegotiationDeal.NegotiationPrefix,
        dunes), p1, 1, 5))
    val done = place(first, p1, dunes, 5).getOrElse(fail("dunes")).state
    assertEquals(ruler(done, woods), SiteRuler.Player(p2))
    assertEquals(ruler(done, dunes), SiteRuler.Player(p1))
    assertEquals(forces(done, woods).total, 4)
    assertEquals((board(done, p1), board(done, p2)), (0, 1))

  test("a recipient of two sites keeps a warband for the second"):
    val two = deeded.warbandsAt(city, p2, 1)
    val agreed = run(atDeal(two, p2), p2 -> give(p1, dunes, city),
      p1 -> AcceptDeal, p2 -> AcceptDeal)
    assertEquals(asked(agreed),
      (NegotiationSites.warbandsDecisionId(NegotiationDeal.NegotiationPrefix,
        dunes), p1, 1, 2))
    val first = place(agreed, p1, dunes, 2).getOrElse(fail("dunes")).state
    assertEquals(asked(first),
      (NegotiationSites.warbandsDecisionId(NegotiationDeal.NegotiationPrefix,
        city), p1, 1, 1))
