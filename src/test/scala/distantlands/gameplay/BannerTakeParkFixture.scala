package distantlands.gameplay

import distantlands.gameplay.powerresolver.{Contribution, ExecutedBatch,
  ExecutedTrigger}
import distantlands.model._

/** A trigger that parks right after a batch moves a banner into a player's
  * play area, as The Needle's question does (Legacy in-play powers ruling
  * 20; R20): a resume then rebuilds the taking walk on the state after the
  * take, so a taker whose tree's shape read the banner's holder breaks. */
object BannerTakeParkFixture:
  val decisionId: String = "test.after-banner-take"
  val ok: DecisionOptionRef.Button = DecisionOptionRef.Button("ok")

  case object ParkAfterBannerTake extends ExecutedTrigger:
    val id: PowerId = PowerId("test.park-after-banner-take")
    def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
    def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

    def onExecuted(batch: ExecutedBatch): Option[Operation] =
      batch.executed.collectFirst {
        case Move(Piece.Banner(_), _,
            PositionedLocation(Location.PlayArea(to), _, _), _, _) => to
        case Take(Piece.Banner(_), _, _, Location.PlayArea(to), _, _, _, _) =>
          to
      }.map(taker => Decide(decisionId, taker, DecisionQuery.ChooseOne(
        Vector(DecisionOption.Button(ok, "OK")))))

  /** The answer that resumes the walk parked on the trigger. */
  def resumeAnswer: DecisionAnswer = DecisionAnswer.ChooseOneAnswer(ok)
