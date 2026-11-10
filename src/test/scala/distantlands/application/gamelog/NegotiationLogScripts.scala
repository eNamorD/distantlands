package distantlands.application.gamelog

import distantlands.application._
import distantlands.gameplay.actions.negotiation.NegotiationDeal
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.economy.BirdsongCard
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, DeclineDeal, ProposeTerms}
import distantlands.testkit.{MenuPlay, Situation, Table}
import distantlands.testkit.Table.{p1, p2}
import LogScriptSupport.{atTable, tableWithOnePawnPerSite}
import distantlands.testkit.Commands

/** The Negotiation scripts: a deal declined, a deal agreed, a deal of one
  * promise, and a facedown adviser disclosed. Reached through `LogScripts`.
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

  /** A deal that holds nothing but the actor's promise: it may be accepted
    * and settles with the promise's line (catalog batch 12, ruling 13). */
  def negotiationPromised(using munit.Location): Script =
    val (service, parked, actor, partner) = negotiating("negotiation-promised")
    parked
      .parkedAfter(deal(actor, ProposeTerms(NegotiationTerms(promises =
        Vector("I will not attack you this round.")))))
      .parkedAfter(deal(partner, AcceptDeal))
      .after(deal(actor, AcceptDeal))
    Script("negotiation-promised", service, actor)

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
