package oathdigital.gameplay.powers.action

import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powers.PhasePowerCatalog
import oathdigital.gameplay.roles.Citizenship
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerPowers
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, PhasePowerHarness, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** The Grand Scepter's Action: "You may become a Citizen if an Exile"
  * (CR p. 34; Empire slice 3a). The acting player is p1. */
class GrandScepterActionSuite extends munit.FunSuite:
  private val rules = ActPowerHarness.rules()
  private val scepter = DecisionOptionRef.Relic(TheGrandScepterCard.id)

  /** p2 is the Chancellor; p1, an Exile in their Act, holds the Scepter. */
  private def exileHolder: Table = Table.start.imperial(p2)
    .turn(p1, Phase.Act).pawn(p1, at = "Broken Peaks")
    .relic(p1, "The Grand Scepter").boardWarbands(p1, 2)
    .allowingCitizenshipPowers

  private def used(ready: ReadyGame)(using munit.Location) =
    accepted(ActPowerHarness.use(rules, ready, GrandScepterAction.id,
      scepter))

  /** The Scepter's row in `ready`'s Act menu, if it lists one. */
  private def scepterRow(ready: ReadyGame): Option[DecisionOption] =
    PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, ready, p1,
      PhasePowerCatalog.default(catalog), WalkerPowers.empty)
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
      .pawn(p1, at = "Broken Peaks").relic(p1, "The Grand Scepter")
      .allowingCitizenshipPowers.ready
    val after = ActPowerHarness.replayed(rules, start, used(start).events)
    assertEquals(Imperials.chancellor(after), Some(p1))

  test("a short purple bank asks which warbands turn purple"):
    val start = exileHolder.boardWarbands(p2, 23).ready
    assert(PhasePowerHarness.awaits(used(start), Citizenship.replaceDecisionId))

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
      .relic(p1, "The Grand Scepter").allowingCitizenshipPowers.ready
    assertEquals(scepterRow(start), None)
