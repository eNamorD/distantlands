package distantlands.gameplay.setup

import distantlands.gameplay.powerresolver.{OfferHost, ResolutionHost}
import distantlands.model._

/** Setup's `SetupPawnPlaced` window for `placer` (catalog batch 13, ruling
  * 4): the powers that apply to the placer offer their steps, and the
  * placer orders them.
  *
  * One node is one round, and `resolved` names, by their options, the
  * powers the earlier rounds resolved. A round walks three slots:
  *
  *  - the placer's question, "Choose a power to resolve", over the offers
  *    not yet resolved, in power order. With one left it is answered alone
  *    (`Decide.answersAlone`), so every round records its pick;
  *  - the picked power's steps;
  *  - the next round, folded when the walk reaches it, so it gathers the
  *    window again against the live state: a power that no longer applies
  *    drops out, and a newly applicable one joins.
  *
  * A round with nothing left and no pick expands to nothing, which ends the
  * window. The shape reads only the offers and the answers, so a walk
  * resumed in any round rebuilds the rounds before it the same way, even
  * when what is offered has changed since. The pick is read when its slot
  * is reached, so the answer that resumes the question is seen.
  */
final case class OrderedWindow(placer: PlayerId,
    resolved: Vector[DecisionOptionRef]) extends ResolutionHost:
  override val window: Option[PowerWindow] =
    Some(PowerWindow.SetupPawnPlaced)
  override val children: Vector[Operation] = Vector.empty

  private val questionId: String =
    OrderedWindow.decisionId(placer, resolved.size)

  private def picked(answered: Vector[Answered]): Option[DecisionOptionRef] =
    answered.collectFirst {
      case Answered(`questionId`, DecisionAnswer.ChooseOneAnswer(ref), _) =>
        ref
    }

  def expand(offers: Vector[OfferedResolution], pass: OfferHost.Pass)
      : Vector[Operation] =
    val remaining = offers.filterNot(offered =>
      resolved.contains(offered.offer.option.ref))
    if remaining.isEmpty && picked(pass.answered).isEmpty then Vector.empty
    else Vector(
      Sequence(Option.when(remaining.nonEmpty)(Decide(questionId, placer,
        DecisionQuery.ChooseOne(remaining.map(_.offer.option),
          heading = Some(OrderedWindow.heading),
          confirmVerb = Some(OrderedWindow.confirmVerb)),
        answersAlone = true)).toVector),
      Branch((_, pending) => picked(pending.answered).toVector.flatMap(ref =>
        offers.find(_.offer.option.ref == ref).toVector
          .flatMap(_.offer.steps))),
      Branch((_, pending) => picked(pending.answered).toVector.map(ref =>
        OrderedWindow(placer, resolved :+ ref))))

object OrderedWindow:
  val heading: String = "Choose a power to resolve"
  val confirmVerb: String = "Resolve"

  /** `placer`'s question in round `round` of their window, from 0. */
  def decisionId(placer: PlayerId, round: Int): String =
    s"setup.power-order.${placer.value}.$round"
