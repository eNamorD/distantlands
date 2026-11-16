package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.{ContributingPower,
  OfferResolution, PowerCtx}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.Unwrap.present

/** A power's steps as Setup's pawn window offers them, so a suite can walk
  * one power's resolution without the whole Setup walk. */
object PawnWindowOffers:
  /** `power`'s offered steps in `ready`, once `placer` has placed their
    * pawn at `site`. Fails the test when the power offers nothing. */
  def steps(catalog: ExecutableCatalog, power: ContributingPower,
      ready: ReadyGame, placer: PlayerId, site: SiteId)(
      using munit.Location): Vector[Operation] =
    val placed = Vector(Answered(SetupProcedure.pawnDecisionId(placer),
      ChooseOneAnswer(DecisionOptionRef.Site(site)), placer))
    val ctx = PowerCtx(catalog, ready, placer, power.source,
      PowerWindow.SetupPawnPlaced, Vector.empty,
      OrderedWindow(placer, Vector.empty), answered = placed)
    present(power.contributions(PowerWindow.SetupPawnPlaced)
      .collectFirst { case resolution: OfferResolution =>
        resolution.offer(ctx) }.flatten, s"${power.id.value}'s offer").steps
