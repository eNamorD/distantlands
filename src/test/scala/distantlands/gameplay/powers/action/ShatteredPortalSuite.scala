package distantlands.gameplay.powers.action

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.phases.PhaseMenu
import distantlands.gameplay.powers.{NoteText, PhasePowerCatalog, PowerFixture}
import distantlands.gameplay.powers.dice.JinxCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay,
  PhasePowerHarness, Table}
import distantlands.testkit.Outcomes.{assertWireRoundTrips, ready}
import distantlands.testkit.Unwrap.accepted

/** Shattered Portal (catalog batch 10, slice 3): with the pawn at its site,
  * roll the round die and place the pawn that many sites Hinterward,
  * wrapping from the bottom Hinterland site to the top Cradle site. */
class ShatteredPortalSuite extends munit.FunSuite:
  import ActPowerHarness.{replayed, roundDie, rules,
    rulesWithPrintedRestrictions, use}
  import PowerFixture._

  private val walkerParked = PhasePowerHarness.parked
  private val portal = StonePortalCard.id
  private val source = DecisionOptionRef.Edifice(portal)

  /** The map's sites from the Cradle out. */
  private val hinterward: Vector[SiteId] =
    Table.from(quietTable).ready.game.current.map.hinterward

  /** p1's Act with 1 faceup secret, the ruined Portal at `portalAt` and p1's
    * pawn at `pawnAt`. */
  private def portalTable(portalAt: SiteId, pawnAt: SiteId): Table =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, pawnAt)
      .edifice(portal, EdificeSide.Ruined, at = portalAt).secrets(actor, 1)

  /** The pawn's site after using the Portal from `hinterward(index)` with
    * the round die showing `face`. */
  private def landingFrom(index: Int, face: RoundDieFace): SiteId =
    val site = hinterward(index)
    val done = accepted(use(rules(roundDie(face)),
      portalTable(site, site).ready, ShatteredPortal.id, source))
    Look(ready(done.state)).pawn(actor)

  test("the map has 8 sites, so a roll of 1 to 6 never lands where it " +
      "started"):
    assertEquals(hinterward.size, 8)

  test("it pays a secret, rolls 4 and places the pawn 4 sites Hinterward, " +
      "with the line, and the journal replays and round trips"):
    val home = hinterward(1)
    val start = portalTable(home, home).ready
    val rules0 = rules(roundDie(RoundDieFace.Four))
    val done = accepted(use(rules0, start, ShatteredPortal.id, source))
    val after = ready(done.state)
    assertEquals(Look(after).pawn(actor), hinterward(5))
    assertEquals(Look(after).tokensOn(portal), Tokens(0, 1))
    walkerParked.assertResumed(done.state, Phase.Act, actor)
    assertEquals(NoteText.said(ShatteredPortal, done.events), Vector(
      NoteText.Said(NoteKey.Used,
        s"Rolled 4; ${actor.value} placed at ${hinterward(5).value}.",
        covers = false)))
    assertEquals(replayed(rules0, start, done.events), after)
    assertWireRoundTrips(done.events)

  test("from the bottom Hinterland site a 1 wraps to the top Cradle site"):
    val top = landingFrom(7, RoundDieFace.One)
    assertEquals(top, hinterward(0))
    assertEquals(Table.from(quietTable).ready.game.current.map.regionOf(top),
      Some(Region.Cradle))

  test("every start and roll lands that many sites on, wrapping"):
    for
      index <- hinterward.indices
      face <- RoundDieFace.values
    do assertEquals(landingFrom(index, face),
      hinterward((index + face.value) % hinterward.size),
      s"from ${hinterward(index).value} rolling ${face.value}")

  test("with the pawn elsewhere at a site the user rules, it is shown " +
      "unavailable with the reason"):
    val home = AncientCitySite.id
    val ruledFromAfar = portalTable(home, BrokenPeaksSite.id)
      .warbandsAt(home, actor, 1).ready
    val option = PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, ruledFromAfar,
      actor, PhasePowerCatalog.default, WalkerPowers.empty)
      .map(offer => MenuPlay.withoutBand(offer.option))
      .find(_.ref == source)
    assertEquals(option.collect {
      case DecisionOption.Unavailable(_, reason) => reason },
      Some(OptionReason.Written(ShatteredPortal.NotHere)))
    assertEquals(use(rules(roundDie(RoundDieFace.Two)), ruledFromAfar,
      ShatteredPortal.id, source).left.toOption,
      Some(OathViolation.StartUnavailable(ShatteredPortal.NotHere)))

  test("Jinx offers no reroll of the round die"):
    val home = hinterward(2)
    val start = portalTable(home, home).adviser(actor, JinxCard.id)
      .secrets(actor, 2).ready
    val done = accepted(use(rulesWithPrintedRestrictions(
      roundDie(RoundDieFace.Three)), start, ShatteredPortal.id, source))
    val after = ready(done.state)
    walkerParked.assertResumed(done.state, Phase.Act, actor)
    assertEquals(Look(after).pawn(actor), hinterward(5))
