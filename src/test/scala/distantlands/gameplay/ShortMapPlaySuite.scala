package distantlands.gameplay

import distantlands.gameplay.phases.PhaseMenu
import distantlands.gameplay.powers.{PhasePowerCatalog, PowerFixture,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.{ShatteredPortal, StonePortal,
  StonePortalCard}
import distantlands.gameplay.powers.wake.HiddenPassagesCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay, Table}
import distantlands.testkit.Outcomes.ready
import distantlands.testkit.Unwrap.accepted

/** Play on a map with empty slots (Foundations design, slice 3, "Empty
  * slots"): what reads the map's shape counts the sites in play, and a
  * map of one site offers nothing that needs another. p1 acts. */
class ShortMapPlaySuite extends munit.FunSuite:
  import PowerFixture.{actor, quietTable}

  private val home = Look(quietTable).pawn(actor)
  private val portal = DecisionOptionRef.Edifice(StonePortalCard.id)
  private val walkerPowers = WalkerPowerCatalog.default(catalog)
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = walkerPowers,
    phasePowerCatalog = PhasePowerCatalog.default)

  /** p1's Act on a map cut to its first `sites` sites, every pawn at the
    * first, with 1 faceup secret and the Stone Portal there on `side`. */
  private def portalOnMapOf(sites: Int, side: EdificeSide): ReadyGame =
    Table.from(quietTable).pawn(Table.p2, home).pawn(Table.p3, home)
      .sitesInPlay(sites).turn(actor, Phase.Act)
      .edifice(StonePortalCard.id, side, at = home).secrets(actor, 1).ready

  /** p1's Wake on a map cut to its first `sites` sites, every pawn at the
    * first, with 2 favor and the Passages edifice there on `side`. */
  private def passagesOnMapOf(sites: Int, side: EdificeSide): ReadyGame =
    Table.from(quietTable).pawn(Table.p2, home).pawn(Table.p3, home)
      .sitesInPlay(sites).turn(actor, Phase.Wake)
      .edifice(HiddenPassagesCard.id, side, at = home).favor(actor, 2).ready

  /** The option `ref` of `menu` as p1 is offered it. */
  private def offered(menu: PhaseMenu.Menu, state: ReadyGame,
      ref: DecisionOptionRef): DecisionOption =
    PhaseMenu.offers(menu, catalog, state, actor,
      PhasePowerCatalog.default, walkerPowers)
      .map(offer => MenuPlay.withoutBand(offer.option)).find(_.ref == ref)
      .getOrElse(fail(s"the $menu menu does not list $ref"))

  test("on a map of one site, Travel is shown unavailable with its reason"):
    val oneSite = portalOnMapOf(1, EdificeSide.Ruined)
    assertEquals(oneSite.game.current.map.inPlay, Vector(home))
    assertEquals(offered(PhaseMenu.Menu.Act, oneSite,
      PhaseMenu.optionFor(ActionRef.Travel, Vector.empty).get),
      DecisionOption.Unavailable(
        DecisionOption.Button(DecisionOptionRef.Button("travel"), "Travel"),
        OptionReason.Written("No site to travel to")))

  test("on a map of one site, Stone Portal has no other site to offer: it " +
      "is shown unavailable, and starting it is refused"):
    val oneSite = portalOnMapOf(1, EdificeSide.Intact)
    assert(!offered(PhaseMenu.Menu.Act, oneSite, portal).available,
      offered(PhaseMenu.Menu.Act, oneSite, portal))
    assertEquals(rules.startWalker(Ready(MenuPlay.withoutMenu(oneSite)),
      ActionRef.UsePower(StonePortal.id), actor, Vector(portal)).left.toOption,
      Some(OathViolation.InvalidEventOrder("the decision offers no answer")))
    // With a second site it is offered again.
    assert(offered(PhaseMenu.Menu.Act,
      portalOnMapOf(2, EdificeSide.Intact), portal).available)

  test("on a map of one site, Hidden Passages and Sinking Passages have " +
      "no other site to offer and are shown unavailable"):
    val passages = DecisionOptionRef.Edifice(HiddenPassagesCard.id)
    Vector(EdificeSide.Intact, EdificeSide.Ruined).foreach { side =>
      val oneSite = offered(PhaseMenu.Menu.Wake, passagesOnMapOf(1, side),
        passages)
      assert(!oneSite.available, s"$side: $oneSite")
      // With a second site the same face is offered.
      assert(offered(PhaseMenu.Menu.Wake, passagesOnMapOf(2, side),
        passages).available, side)
    }

  test("Shattered Portal counts the sites on the map, not its slots: on " +
      "three sites a 1 moves one site on, a 2 wraps to the last, and a 3 " +
      "comes back to where it started"):
    val threeSites = portalOnMapOf(3, EdificeSide.Ruined)
    val sites = threeSites.game.current.map.hinterward
    assertEquals(sites.size, 3)
    def landing(face: RoundDieFace): SiteId =
      Look(ready(accepted(ActPowerHarness.use(ActPowerHarness.rules(
        ActPowerHarness.roundDie(face)), threeSites, ShatteredPortal.id,
        portal)).state)).pawn(actor)
    assertEquals(RoundDieFace.values.toVector.map(landing), Vector(
      sites(1), sites(2), sites(0), sites(1), sites(2), sites(0)))
