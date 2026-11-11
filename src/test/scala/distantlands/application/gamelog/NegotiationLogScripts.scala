package distantlands.application.gamelog

import distantlands.application._
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.actions.cardplay.CardPlayProcedure.PlayedAs
import distantlands.gameplay.actions.negotiation.{CouncilArbiterCard,
  DeedWriterCard, NegotiationDeal, NegotiationSites, TheTribunalCard}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.action.{WhisperingStone,
  WhisperingStoneCard}
import distantlands.gameplay.powers.economy.BirdsongCard
import distantlands.gameplay.powers.whenplayed.{GatheringDeals, TheGathering,
  TheGatheringCard}
import distantlands.model._
import distantlands.model.DecisionAnswer.{AcceptDeal, ChooseAmountAnswer,
  ChooseManyAnswer, ChooseOneAnswer, DeclineDeal, OpenDeal, PassGathering,
  ProposeTerms}
import distantlands.testkit.{MenuPlay, PlacementPicks, Situation, Table}
import distantlands.testkit.Table.{p1, p2, p3}
import LogScriptSupport.{atTable, tableWithOnePawnPerSite}
import distantlands.testkit.Commands

/** The Negotiation scripts: a deal declined, a deal agreed, a deal of one
  * promise, a binding promise through The Tribunal, a facedown adviser
  * disclosed, a facedown adviser given, a site given, a private deal
  * through Whispering Stone, and The Gathering. Reached through
  * `LogScripts`.
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

  /** p2 rules Broken Peaks, where The Tribunal stands, so p2 may use it and
    * the deal takes binding promises (catalog batch 12, ruling 11). p1, who
    * has no access, makes one. Nobody else is in reach, so the negotiators
    * decision is not asked. p2 already holds the Oathkeeper title, so ruling
    * Broken Peaks writes no title line. */
  def negotiationBindingPromised(using munit.Location): Script =
    val (service, parked, actor, partner) = negotiating(
      "negotiation-binding-promised", _.edifice(TheTribunalCard,
        EdificeSide.Intact, at = BrokenPeaksSite)
        .warbandsAt(BrokenPeaksSite, p2, 1).oathkeeper(Some(p2)))
    parked
      .parkedAfter(deal(actor, ProposeTerms(NegotiationTerms(bindingPromises =
        Vector("I will give you 2 favor next round.")))))
      .parkedAfter(deal(partner, AcceptDeal))
      .after(deal(actor, AcceptDeal))
    Script("negotiation-binding-promised", service, actor)

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

  /** The partner gives the actor, who holds Council Arbiter, a facedown
    * adviser (catalog batch 12, ruling 16). */
  def negotiationAdviserGiven(using munit.Location): Script =
    val birdsong: WorldCardId = BirdsongCard.id
    val (service, parked, actor, partner) = negotiating(
      "negotiation-adviser-given", _.adviser(p1, CouncilArbiterCard.id)
        .adviser(p2, BirdsongCard.id, facedown = true))
    parked
      .parkedAfter(deal(partner, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(actor, 0, Vector.empty, Vector(birdsong)))))))
      .parkedAfter(deal(actor, AcceptDeal))
      .after(deal(partner, AcceptDeal))
    Script("negotiation-adviser-given", service, actor)

  /** The partner gives the actor, who holds Deed Writer, Broken Peaks; the
    * actor places 2 warbands there (catalog batch 12, rulings 18 and 20).
    * The actor holds the Oathkeeper title already, so ruling Broken Peaks
    * writes no title line. */
  def negotiationSiteGiven(using munit.Location): Script =
    val peaks = BrokenPeaksSite.id
    val (service, parked, actor, partner) = negotiating(
      "negotiation-site-given", _.adviser(p1, DeedWriterCard.id)
        .warbandsAt(BrokenPeaksSite, p2, 2).oathkeeper(Some(p1)))
    parked
      .parkedAfter(deal(partner, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(actor, 0, Vector.empty, Vector.empty,
          Vector(peaks)))))))
      .parkedAfter(deal(actor, AcceptDeal))
      .parkedAfter(deal(partner, AcceptDeal))
      .after(Commands.resolveWalker(actor, TreeDecision(
        NegotiationSites.warbandsDecisionId(NegotiationDeal.NegotiationPrefix,
          peaks), ChooseAmountAnswer(2))))
    Script("negotiation-site-given", service, actor)

  /** p1 uses Whispering Stone to negotiate in private with p3, whose pawn
    * stands at Buried Giant, away from p1's. p1 gives p3 1 favor and makes
    * a promise. p2, outside the deal and the script's other seat, reads
    * the opening line and the transfer, never the promise (catalog batch
    * 12, rulings 21 to 23). */
  def negotiationWhispered(using munit.Location): Script =
    val stone = WhisperingStoneCard.id
    val (service, act) = atTable("negotiation-whispered",
      tableWithOnePawnPerSite.relic(p1, stone).secrets(p1, faceUp = 1)
        .banditsRefilled)
    act
      .parkedAfter(MenuPlay.start(p1, ActionRef.UsePower(WhisperingStone.id),
        Vector(DecisionOptionRef.Relic(stone))))
      .parkedAfter(Commands.resolveWalker(p1, TreeDecision(
        NegotiationDeal.negotiatorsDecisionId, ChooseManyAnswer(Vector(
          DecisionOptionRef.Player(p3))))))
      .parkedAfter(deal(p1, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(p3, 1, Vector.empty)),
        promises = Vector("I will not attack you this round.")))))
      .parkedAfter(deal(p3, AcceptDeal))
      .after(deal(p1, AcceptDeal))
    Script("negotiation-whispered", service, p1)

  /** p1 Searches The Gathering off the world deck and plays it at Ancient
    * City. p2 comes from Broken Peaks; p3 stays at Buried Giant. p2 opens
    * the one deal: p2 gives p1 a secret, p1 gives p2 a favor, and both
    * accept. Then both pass (catalog batch 12, slice 6b). */
  def theGathering(using munit.Location): Script =
    val (service, act) = atTable("the-gathering", tableWithOnePawnPerSite
      .worldDeckTop(TheGatheringCard).secrets(p2, faceUp = 1).favor(p1, 1)
      .banditsRefilled)
    val dealId = NegotiationDeal.dealId(GatheringDeals.dealPrefix(1))
    def say(by: PlayerId, answer: DecisionAnswer): GameCommand =
      Commands.resolveWalker(by, TreeDecision(dealId, answer))
    def open(by: PlayerId, answer: DecisionAnswer): GameCommand =
      Commands.resolveWalker(by, TreeDecision(GatheringDeals.openDecisionId,
        answer))
    act.withAnswers {
      case park if park.decisionId ==
          CardPlayProcedure.placementDecisionId(TheGatheringCard.id) =>
        ChooseOneAnswer(PlacementPicks.pick(park.decide, PlayedAs.AtSite))
      case park if park.decisionId == TheGathering.pawnDecisionId =>
        ChooseOneAnswer(if park.awaiting == p2 then TheGathering.join
          else TheGathering.stay)
    }.parkedAt(GatheringDeals.openDecisionId)(
      MenuPlay.start(p1, ActionRef.Search))
      .parkedAfter(open(p2, OpenDeal(Vector(p1))))
      .parkedAfter(say(p2, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(p1, 0, Vector.empty, secrets = 1))))))
      .parkedAfter(say(p1, ProposeTerms(NegotiationTerms(Vector(
        NegotiationTransfer(p2, 1, Vector.empty))))))
      .parkedAfter(say(p2, AcceptDeal))
      .parkedAfter(say(p1, AcceptDeal))
      .parkedAfter(open(p1, PassGathering))
      .after(open(p2, PassGathering))
    Script("the-gathering", service, p1)
