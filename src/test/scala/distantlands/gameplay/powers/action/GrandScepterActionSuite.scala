package distantlands.gameplay.powers.action

import distantlands.gameplay.cards.BrokenPeaksSite
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.phases.PhaseMenu
import distantlands.gameplay.powers.PhasePowerCatalog
import distantlands.gameplay.roles.Citizenship
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.WalkerPowers
import distantlands.model._
import distantlands.testkit.{ActPowerHarness, Look, PhasePowerHarness, Table}
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** The Grand Scepter's Action: "You may become a Citizen if an Exile"
  * (CR p. 34; Empire slice 3a). The acting player is p1. */
class GrandScepterActionSuite extends munit.FunSuite:
  private val rules = ActPowerHarness.rules()
  private val scepter = DecisionOptionRef.Relic(TheGrandScepterCard.id)

  /** p2 is the Chancellor; p1, an Exile in their Act, holds the Scepter. */
  private def exileHolder: Table = Table.start.imperial(p2)
    .turn(p1, Phase.Act).pawn(p1, at = BrokenPeaksSite)
    .relic(p1, TheGrandScepterCard).boardWarbands(p1, 2)
    .allowingCitizenshipPowers

  private def used(ready: ReadyGame)(using munit.Location) =
    accepted(ActPowerHarness.use(rules, ready, GrandScepterAction.id,
      scepter))

  /** The Scepter's row in `ready`'s Act menu, if it lists one. */
  private def scepterRow(ready: ReadyGame): Option[DecisionOption] =
    PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, ready, p1,
      PhasePowerCatalog.default, WalkerPowers.empty)
      .map(_.option).find(_.ref == scepter)

  private def reasonOf(option: DecisionOption): Option[String] = option match
    case DecisionOption.Grouped(inner, _) => reasonOf(inner)
    case DecisionOption.Unavailable(_, OptionReason.Written(text)) =>
      Some(text)
    case _ => None

  test("an Exile holder becomes a Citizen and their Act ends"):
    val start = exileHolder.ready
    val done = used(start)
    // The journal up to the walk's end replays to the Rest it began.
    val after = ActPowerHarness.replayed(rules, start,
      ActPowerHarness.untilWalkEnds(done.events))
    assertEquals(Imperials.role(after, p1), Some(Role.Citizen))
    assertEquals(after.game.current.turn.phase, Phase.Rest)

  test("with no Empire the holder becomes the Chancellor"):
    val start = Table.start.turn(p1, Phase.Act)
      .pawn(p1, at = BrokenPeaksSite).relic(p1, TheGrandScepterCard)
      .allowingCitizenshipPowers.ready
    val after = ActPowerHarness.replayed(rules, start, used(start).events)
    assertEquals(Imperials.chancellor(after), Some(p1))

  test("a short purple bank asks which warbands turn purple, and the split holds"):
    // The bank holds 1; p1 has 2 on their board and 1 at Broken Peaks.
    val start = exileHolder.boardWarbands(p2, 23)
      .warbandsAt(BrokenPeaksSite, p1, 1).ready
    val parked = used(start)
    assert(PhasePowerHarness.awaits(parked, Citizenship.replaceDecisionId(p1)))
    val peaks = DecisionOptionRef.Site(BrokenPeaksSite.id)
    val done = accepted(rules.resolveWalker(parked.state, p1,
      Citizenship.replaceDecisionId(p1), DecisionAnswer.DistributeAnswer(Vector(
        DistributeAmount(DecisionOptionRef.Player(p1), 0),
        DistributeAmount(peaks, 1)))))
    val after = ActPowerHarness.replayed(rules, start,
      ActPowerHarness.untilWalkEnds(parked.events ++ done.events))
    assertEquals(Imperials.role(after, p1), Some(Role.Citizen))
    assertEquals(Look(after).player(p1).board.warbands, 0)
    assertEquals(Look(after).forces(BrokenPeaksSite),
      SiteForces.Occupied(ForceKind.Imperial, 1))

  test("the first-game option shows the Action disabled with its reason"):
    val start = exileHolder.update(ready => ready.copy(setup =
      ready.setup.copy(ignoreCitizenshipPowers = true))).ready
    assertEquals(scepterRow(start).flatMap(reasonOf),
      Some(Citizenship.ignoredReason))
    assert(ActPowerHarness.use(rules, start, GrandScepterAction.id,
      scepter).isLeft)

  test("an Exile holder's menu offers it"):
    assertEquals(scepterRow(exileHolder.ready).flatMap(reasonOf), None)
    assert(scepterRow(exileHolder.ready).nonEmpty)

  test("an Imperial holder's menu leaves the Action out"):
    val start = Table.start.imperial(p1).turn(p1, Phase.Act)
      .relic(p1, TheGrandScepterCard).allowingCitizenshipPowers.ready
    assertEquals(scepterRow(start), None)
