package oathdigital.application.gamelog

import oathdigital.application._
import oathdigital.gameplay.actions.negotiation.NegotiationDeal
import oathdigital.gameplay.powers.economy.BirdsongCard
import oathdigital.gameplay.powers.wake.AncientCitySite
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{AcceptDeal, DeclineDeal, ProposeTerms}
import oathdigital.testkit.{MenuPlay, Situation, Table}
import oathdigital.testkit.Table.{p1, p2}
import LogScriptSupport.{atTable, tableWithOnePawnPerSite}
import oathdigital.testkit.Commands

/** The Negotiation scripts: a deal declined, a deal agreed, and a facedown
  * adviser disclosed. Reached through `LogScripts`.
  */
object NegotiationLogScripts:
  /** p1 and p2 share Ancient City, so p1 can negotiate with exactly one
    * other: the negotiators decision is not asked. `extra` adds to the
    * table. */
  private def negotiating(name: String, extra: Table => Table = identity)(
      using munit.Location)
      : (GameApplicationService, Situation, PlayerId, PlayerId) =
    val (service, act) = atTable(name,
      extra(tableWithOnePawnPerSite.pawn(p2, AncientCitySite)).banditsRefilled)
    (service, act.parkedAfter(MenuPlay.start(p1, ActionRef.Negotiation)), p1, p2)

  private def deal(by: PlayerId, answer: DecisionAnswer): GameCommand =
    Commands.resolveWalker(by, TreeDecision(NegotiationDeal.dealDecisionId,
      answer))

  def negotiationDeclined(using munit.Location): Script =
    val (service, parked, actor, partner) = negotiating("negotiation-declined")
    parked.after(deal(partner, DeclineDeal))
    Script("negotiation-declined", service, actor)

  def negotiationAgreed(using munit.Location): Script =
    val (service, parked, actor, partner) = negotiating("negotiation-agreed")
    parked
      .parkedAfter(deal(actor, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(partner, 1, Vector.empty))))))
      .parkedAfter(deal(partner, AcceptDeal))
      .after(deal(actor, AcceptDeal))
    Script("negotiation-agreed", service, actor)

  /** The partner shows the actor its facedown adviser. */
  def negotiationDisclosed(using munit.Location): Script =
    val birdsong = BirdsongCard.id
    val (service, parked, actor, partner) = negotiating(
      "negotiation-disclosed", _.adviser(p2, birdsong, facedown = true))
    val adviser: WorldCardId = birdsong
    parked
      .parkedAfter(deal(partner, ProposeTerms(NegotiationTerms(Vector.empty,
        Vector(NegotiationDisclosure(actor,
          NegotiationDisclosureRef.Adviser(partner, adviser)))))))
      .parkedAfter(deal(actor, AcceptDeal))
      .after(deal(partner, AcceptDeal))
    Script("negotiation-disclosed", service, actor)
