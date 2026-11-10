package oathdigital.gameplay.actions.campaign

import oathdigital.gameplay.CampaignFixture
import oathdigital.gameplay.CampaignFixture.{Board, board, on}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.{ParkedDecisionAssertions, ProcedureWalker,
  WalkerPowers, WalkerProcedureRegistry}
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Outcomes.ready
import oathdigital.testkit.Unwrap.{accepted, as, present}

/** Imperial Infighting (CR p. 35): who defends the Empire's sites, which
  * Imperial player may target them, and Imperial players raiding each
  * other. */
class ImperialInfightingSuite extends munit.FunSuite:
  /** The board's p1 at Ancient City, now held by the Empire with two purple
    * warbands, and a second Empire site, `secondEmpireSite`, with two more.
    * p2 is the Chancellor; `citizens` are seated as Citizens. p2 and p3
    * stand elsewhere. p1 has 5 board warbands and 7 Supply. */
  private def empireOrigin(citizens: PlayerId*): Board =
    val b = board(warbands = 5, supply = 7)
    on(b)(_.imperial(p2, citizens*).warbandsAt(b.origin, p2, 2)
      .warbandsAt(secondEmpireSite(b), p2, 2))

  /** The first in-play site after the origin. */
  private def secondEmpireSite(b: Board): SiteId =
    b.ready.game.current.map.inPlay.filter(_ != b.origin).head

  private def defender(b: Board, site: SiteId): Option[CampaignDefender] =
    CampaignSetup.defenderAt(b.ready, site)

  test("the Chancellor defends every site the Empire holds"):
    val b = empireOrigin()
    assertEquals(defender(b, b.origin), Some(CampaignDefender.Player(p2)))
    assertEquals(defender(b, secondEmpireSite(b)),
      Some(CampaignDefender.Player(p2)))

  test("with no Chancellor seated, a purple site has no defender"):
    val b = board(warbands = 5, supply = 7)
    val noChancellor = on(b)(_.imperial(p2).warbandsAt(b.origin, p2, 2)
      .update(ready => ready.updateCampaign(campaign => campaign.copy(
        lineages = campaign.lineages.map { case (id, lineage) =>
          id -> lineage.copy(role = Role.Exile) }))))
    assertEquals(defender(noChancellor, b.origin), None)

  test("an Exile at an Empire site conquers against the Chancellor"):
    val b = empireOrigin()
    assertEquals(CampaignSetup.conquestDefender(b.ready, p1),
      Some(CampaignDefender.Player(p2)))
    assert(CampaignSetup.legalKinds(b.ready, p1)
      .contains(CampaignKind.Conquest))

  test("a Citizen conquers against the Chancellor and may add the Empire's " +
      "other sites"):
    val b = empireOrigin(p1)
    assertEquals(CampaignSetup.conquestDefender(b.ready, p1),
      Some(CampaignDefender.Player(p2)))
    assertEquals(CampaignSetup.targetOptions(b.ready, p1,
      CampaignKind.Conquest, CampaignDefender.Player(p2)),
      Vector(DecisionOption.Site(DecisionOptionRef.Site(secondEmpireSite(b)))))

  test("the Chancellor cannot conquer the Empire's own sites"):
    val b = board(warbands = 5, supply = 7)
    val chancellorAtHome = on(b)(_.imperial(p1, p2)
      .warbandsAt(b.origin, p1, 2))
    assertEquals(CampaignSetup.conquestDefender(chancellorAtHome.ready, p1),
      None)
    assertEquals(CampaignSetup.legalKinds(chancellorAtHome.ready, p1),
      Vector.empty)

  test("a Chancellor conquering a bandit site sees the Empire's sites as " +
      "theirs"):
    val b = board(warbands = 5, supply = 7)
    val site = secondEmpireSite(b)
    val chancellor = on(b)(_.imperial(p1).warbandsAt(site, p1, 2))
    val reasons = CampaignTargets.withheld(chancellor.ready, p1,
      CampaignKind.Conquest, b.origin, CampaignDefender.Bandits,
      Vector.empty).collect {
        case DecisionOption.Unavailable(DecisionOption.Site(ref),
            OptionReason.Written(text)) => ref.id -> text }.toMap
    assertEquals(reasons.get(site), Some("You rule this site"))

  test("an Exile conquering a bandit site sees the Empire's sites as not " +
      "the defender's"):
    val b = board(warbands = 5, supply = 7)
    val site = secondEmpireSite(b)
    val exile = on(b)(_.imperial(p2).warbandsAt(site, p2, 2))
    val reasons = CampaignTargets.withheld(exile.ready, p1,
      CampaignKind.Conquest, b.origin, CampaignDefender.Bandits,
      Vector.empty).collect {
        case DecisionOption.Unavailable(DecisionOption.Site(ref),
            OptionReason.Written(text)) => ref.id -> text }.toMap
    assertEquals(reasons.get(site), Some("Not ruled by the defender"))

  test("a Citizen may raid the Chancellor and another Citizen at their site"):
    val b = empireOrigin(p1, p3)
    val shared = on(b)(_.pawn(p2, at = b.origin).pawn(p3, at = b.origin))
    assertEquals(CampaignSetup.raidDefenders(shared.ready, p1).toSet,
      Set(p2, p3))

  test("the Chancellor may raid a Citizen at their site"):
    val b = board(warbands = 5, supply = 7)
    val shared = on(b)(_.imperial(p1, p2).pawn(p2, at = b.origin))
    assertEquals(CampaignSetup.raidDefenders(shared.ready, p1), Vector(p2))

  test("a Citizen's Conquest targets question tallies the Empire's sites"):
    val b = empireOrigin(p1)
    val game = CampaignFixture.rules()
    val started = accepted(game.startWalker(Ready(b.ready),
      ActionRef.Campaign, p1))
    val current = ready(started.state)
    val parked = new ParkedDecisionAssertions(catalog,
      WalkerPowerCatalog.default(catalog))
    val facts = present(parked.parkedDecision(started.state), "a park")
    assertEquals(facts.decision, CampaignIds.targets)
    val pending = present(current.game.current.walkerPending, "a pending walk")
    val tree = accepted(WalkerProcedureRegistry.rebuild(facts.procedure,
      catalog, current, p1, Vector.empty))
    val query = as[DecisionQuery.ChooseMany](ProcedureWalker.openDecisions(
      current, tree, pending, WalkerPowers.selected(
        WalkerPowerCatalog.default(catalog), Vector.empty)).head.query)
    val second = DecisionOptionRef.Site(secondEmpireSite(b))
    assert(query.options.contains(DecisionOption.Site(second)),
      query.options.toString)
    assertEquals(query.tallies.find(_.label == CampaignTargets.ForceLabel)
      .flatMap(_.perOption.get(second)), Some(2))
