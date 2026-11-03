package oathdigital.gameplay

import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.cards.{BrokenPeaksSite, DunesSite}
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.actions.negotiation.{NegotiationDeal,
  NegotiationProcedure}
import oathdigital.gameplay.powers.NoteText
import oathdigital.gameplay.roles.Citizenship
import oathdigital.gameplay.setup.FirstGameSetupFixture._
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{AcceptDeal, ChooseManyAnswer,
  DistributeAnswer, ProposeTerms}
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}

/** The Grand Scepter's term in a deal: its holder offers Citizenship to
  * Exiles among the negotiators (CR p. 34; Empire slice 3b). */
class NegotiationCitizenshipSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog)
  private val dealId = NegotiationDeal.dealDecisionId
  private val site = BrokenPeaksSite.id

  /** p1, the Chancellor, holds the Grand Scepter at Broken Peaks beside p2
    * and p3, Exiles with 2 board warbands each. `purple` is what the purple
    * bank holds. */
  private def gathered(purple: Int = 10): Table = Table.start.imperial(p1)
    .turn(p1, Phase.Act)
    .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
    .relic(p1, TheGrandScepterCard)
    .boardWarbands(p2, 2).boardWarbands(p3, 2)
    .boardWarbands(p1, MaterialBankState.ImperialWarbands - purple)
    .allowingCitizenshipPowers

  private def atDeal(ready: ReadyGame): OathState =
    val started = rules.startWalker(Ready(ready), ActionRef.Negotiation, p1)
      .getOrElse(fail("Negotiation must start"))
    rules.resolveWalker(started.state, p1,
      NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(Vector(
        DecisionOptionRef.Player(p2), DecisionOptionRef.Player(p3))))
      .getOrElse(fail("the negotiators must be accepted")).state

  private def say(state: OathState, by: PlayerId, answer: DecisionAnswer) =
    rules.resolveWalker(state, by, dealId, answer)

  private def offering(who: PlayerId*): DecisionAnswer =
    ProposeTerms(NegotiationTerms(citizenship = who.toVector))

  /** The deal proposed by p1 and accepted by all three. */
  private def agreed(ready: ReadyGame, who: PlayerId*)
      : (OathState, Vector[OathEvent]) =
    Vector[(PlayerId, DecisionAnswer)](p1 -> offering(who*), p1 -> AcceptDeal,
      p2 -> AcceptDeal, p3 -> AcceptDeal).foldLeft((atDeal(ready),
      Vector.empty[OathEvent])) { case ((state, events), (by, answer)) =>
      val next = say(state, by, answer).getOrElse(
        fail(s"${by.value} must be heard"))
      (next.state, events ++ next.events)
    }

  private def bounds(ready: ReadyGame): Map[PlayerId, NegotiationBounds] =
    val deal = NegotiationDeal.fold(Vector(p1, p2, p3), Vector.empty)
    NegotiationDeal.snapshot(ready, deal).bounds

  test("only the Scepter's holder may offer Citizenship, to Exiles other " +
      "than themself"):
    val all = bounds(gathered().ready)
    assertEquals(all(p1).citizenship, Vector(p2, p3))
    assertEquals(all(p2).citizenship, Vector.empty)
    assertEquals(all(p1).citizenshipRefused, None)

  test("under the first-game option the holder is told why they may offer " +
      "none, and an offer is refused"):
    val ready = gathered().update(r => r.copy(setup =
      r.setup.copy(ignoreCitizenshipPowers = true))).ready
    assertEquals(bounds(ready)(p1).citizenship, Vector(p2, p3))
    assertEquals(bounds(ready)(p1).citizenshipRefused,
      Some(Citizenship.ignoredReason))
    assert(say(atDeal(ready), p1, offering(p2)).isLeft)

  test("a Citizenship-only deal has substance"):
    val deal = NegotiationDeal.fold(Vector(p1, p2), Vector(
      Answered(dealId, offering(p2), p1)))
    assert(deal.hasSubstance)

  test("an author may not offer Citizenship they cannot"):
    assert(say(atDeal(gathered().ready), p2, offering(p3)).isLeft)

  test("accepting makes both offered Exiles Citizens, and the log says who " +
      "offered"):
    val (state, events) = agreed(gathered().ready, p2, p3)
    val after = Outcomes.ready(state)
    assertEquals(Imperials.role(after, p2), Some(Role.Citizen))
    assertEquals(Imperials.role(after, p3), Some(Role.Citizen))
    assertEquals(Look(after).player(p2).board.warbands, 2)
    assertEquals(NoteText.said(ProcedureNotes.id(ActionRef.Negotiation),
      Vector(NegotiationProcedure.offeredCitizenship), events)
      .filter(_.key == "offered-citizenship"),
      Vector(NoteText.Said("offered-citizenship",
        "p1 offered Citizenship to p2, p3.", covers = false)))

  test("with a short bank the Exile it runs short for answers their own " +
      "split"):
    // Three purple warbands: p2 takes two without a question; p3 has two on
    // their board and one at Broken Peaks, with one purple left for them.
    val ready = gathered(purple = 3).warbandsAt(site, p3, 1).ready
    val (parkedState, _) = agreed(ready, p2, p3)
    val done = rules.resolveWalker(parkedState, p3,
      Citizenship.replaceDecisionId(p3), DistributeAnswer(Vector(
        DistributeAmount(DecisionOptionRef.Player(p3), 0),
        DistributeAmount(DecisionOptionRef.Site(site), 1))))
      .getOrElse(fail("p3's split must be heard"))
    val after = Outcomes.ready(done.state)
    assertEquals(Look(after).player(p2).board.warbands, 2)
    assertEquals(Look(after).player(p3).board.warbands, 0)
    assertEquals(Look(after).forces(site),
      SiteForces.Occupied(ForceKind.Imperial, 1))
    assertEquals(Imperials.role(after, p3), Some(Role.Citizen))

  test("a bank short for warbands in one place asks nothing: they keep " +
      "what it holds"):
    val (state, _) = agreed(gathered(purple = 3).ready, p2, p3)
    val after = Outcomes.ready(state)
    assertEquals(Look(after).player(p2).board.warbands, 2)
    assertEquals(Look(after).player(p3).board.warbands, 1)
    assertEquals(Imperials.role(after, p3), Some(Role.Citizen))

  test("with no Empire the first Exile offered becomes the Chancellor and " +
      "takes the Scepter; the next becomes a Citizen"):
    val noEmpire = Table.start.turn(p1, Phase.Act)
      .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = site)
      .relic(p1, TheGrandScepterCard).allowingCitizenshipPowers
    val (state, _) = agreed(noEmpire.ready, p2, p3)
    val after = Outcomes.ready(state)
    assertEquals(Imperials.chancellor(after), Some(p2))
    assertEquals(Imperials.role(after, p3), Some(Role.Citizen))
    assertEquals(Imperials.role(after, p1), Some(Role.Exile))
    assert(Look(after).player(p2).relics.exists(_.id ==
      TheGrandScepterCard.id))

  test("a deal that also gives the Scepter away still applies the offer"):
    val terms = ProposeTerms(NegotiationTerms(Vector(NegotiationTransfer(p3,
      0, Vector(TheGrandScepterCard.id))), citizenship = Vector(p2)))
    val (state, _) = Vector[(PlayerId, DecisionAnswer)](p1 -> terms,
      p1 -> AcceptDeal, p2 -> AcceptDeal, p3 -> AcceptDeal).foldLeft((
      atDeal(gathered().ready), Vector.empty[OathEvent])) {
      case ((state, events), (by, answer)) =>
        val next = say(state, by, answer).getOrElse(
          fail(s"${by.value} must be heard"))
        (next.state, events ++ next.events)
    }
    val after = Outcomes.ready(state)
    assertEquals(Imperials.role(after, p2), Some(Role.Citizen))
    assert(Look(after).player(p3).relics.exists(_.id ==
      TheGrandScepterCard.id))

  test("an Exile who negotiates on their own turn and accepts ends their Act"):
    // p2, an Exile in their Act, negotiates with p1, the Scepter's holder.
    val table = Table.start.imperial(p1).turn(p2, Phase.Act)
      .pawn(p1, at = site).pawn(p2, at = site).pawn(p3, at = DunesSite)
      .relic(p1, TheGrandScepterCard).boardWarbands(p2, 2)
      .allowingCitizenshipPowers
    val started = rules.startWalker(Ready(table.ready), ActionRef.Negotiation,
      p2).getOrElse(fail("Negotiation must start"))
    val done = Vector[(PlayerId, DecisionAnswer)](p1 -> offering(p2),
      p1 -> AcceptDeal, p2 -> AcceptDeal).foldLeft(started.state) {
      case (state, (by, answer)) =>
        say(state, by, answer).getOrElse(fail(s"${by.value} must be heard"))
          .state
    }
    val after = Outcomes.ready(done)
    assertEquals(Imperials.role(after, p2), Some(Role.Citizen))
    // The Act ended: Rest ran and p3's turn began.
    assertEquals(after.game.current.turn.activePlayer, p3)
