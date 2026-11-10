package distantlands.gameplay

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.campaign.RainBootsCard
import distantlands.gameplay.powers.economy.GolemLegionsCard
import distantlands.gameplay.powers.search.DisciplesCard
import distantlands.gameplay.powers.whenplayed.BanditChiefCard
import distantlands.model.OathState.Ready
import distantlands.gameplay.powers.{PhasePowerCatalog, WalkerPowerCatalog}
import distantlands.gameplay.powers.rest.{LeagueTreatyContribution,
  LeagueTreatyFixture, SilverTongue, SilverTongueFixture}
import distantlands.gameplay.powers.rest.LeagueTreatyFixture.{
  leagueTreatyAct, withFavoredCard}
import distantlands.gameplay.setup.FirstGameSetupFixture._
import distantlands.gameplay.walker.ParkedDecisionAssertions
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Unwrap.accepted

/** The rules half of the pending-walker invariant: over a parked walker,
  * neither a walker start nor any legacy `handle` overload runs.
  */
class PendingWalkerRulesSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    phasePowerCatalog = PhasePowerCatalog.default)
  private val walkerParked = new ParkedDecisionAssertions(catalog,
    WalkerPowerCatalog.default(catalog), PhasePowerCatalog.default)
  private val pending = OathViolation.InvalidEventOrder(
    "a walker procedure is already pending")

  private def leagueTreatyPark: OathState =
    // p2 rules League Treaty's site, off p1's turn; an Arcane and a Discord
    // card in the Cradle hold 2 favor each.
    val ruler = Table.p2
    val ready = leagueTreatyAct(Some(ruler))
      .withFavoredCard(DisciplesCard, at = AncientCitySite, favor = 2)
      .withFavoredCard(BanditChiefCard, at = BrokenPeaksSite, favor = 2).ready
    val site = LeagueTreatyFixture.treatySite
    val actor = ready.game.current.turn.activePlayer
    val parked = accepted(
      rules.startWalker(Ready(ready), PhaseTransitionRef.BeginRest, actor))
    walkerParked.assertParked(parked.state, PhaseTransitionRef.FinishRest,
      LeagueTreatyContribution.destinationDecisionId(ready, actor, site,
        LeagueTreatyFixture.treatyCard),
      ruler)
    parked.state

  private def silverTonguePark: OathState =
    val ready = SilverTongueFixture.silverTongueRest(
      Vector(GolemLegionsCard, RainBootsCard), Set(Suit.Arcane, Suit.Nomad))
    val actor = Table.p1
    val parked = accepted(rules.startWalker(Ready(ready),
      ActionRef.UsePower(SilverTongue.id), actor, Vector(DecisionOptionRef.Denizen(SilverTongueFixture.tongue))))
    walkerParked.assertParked(parked.state, ActionRef.UsePower(SilverTongue.id),
      SilverTongue.choiceDecisionId(ready, actor), actor)
    parked.state

  Vector("off-turn League Treaty" -> (() => leagueTreatyPark),
    "Silver Tongue choice" -> (() => silverTonguePark)).foreach:
    case (name, park) =>
      test(s"no walker start runs over a parked $name"):
        val state = park()
        val ready = Outcomes.ready(state)
        val actor = ready.game.current.turn.activePlayer
        (StartableRef.all :+ ActionRef.UsePower(SilverTongue.id)).foreach { ref =>
          assertEquals(rules.startWalker(state, ref, actor).left.toOption,
            Some(pending), ref.key)
        }
