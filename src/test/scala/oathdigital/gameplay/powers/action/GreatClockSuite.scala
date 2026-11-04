package oathdigital.gameplay.powers.action

import oathdigital.gameplay.StateBasedEvaluation
import oathdigital.gameplay.cards.AncientCitySite
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powers.{NoteText, PhasePowerCatalog, PowerFixture}
import oathdigital.gameplay.powers.setup.GreatClockCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerPowers
import oathdigital.model._
import oathdigital.model.OathEvent.{EndDieRolled, RoundEnded}
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{Look, MenuPlay, PhasePowerHarness, RoundEndChoices,
  Table}
import oathdigital.testkit.Outcomes.{assertWireRoundTrips, ready}
import oathdigital.testkit.Unwrap.accepted

/** Great Clock (catalog batch 10, slice 2): advance the round marker once,
  * if possible, without the end die. */
class GreatClockSuite extends munit.FunSuite:
  import PowerFixture._
  import PhasePowerHarness.{backToActing, replayed, rules}

  private val clock = GreatClockCard.id
  private val source = DecisionOptionRef.Edifice(clock)
  private val home = AncientCitySite.id

  /** p1's Act at Ancient City beside the intact Great Clock, with 1 favor
    * and 3 faceup secrets to pay its cost, the round marker on `round` and
    * the Usurper Limiter on the track. */
  private def clockAtRound(round: Int): Table =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, home)
      .edifice(GreatClockCard, EdificeSide.Intact, at = home)
      .favor(actor, 1).secrets(actor, 3)
      .roundMarkerAt(round).usurperLimiter(onTrack = true)

  private def use(state: ReadyGame) = rules.startWalker(
    Ready(MenuPlay.withoutMenu(state)), ActionRef.UsePower(GreatClock.id),
    actor, Vector(source))

  /** The Clock's option on the actor's Act menu, or `None` when the menu
    * leaves it out. */
  private def clockOnMenu(ready: ReadyGame): Option[DecisionOption] =
    PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, ready, actor,
      PhasePowerCatalog.default, WalkerPowers.empty)
      .map(offer => MenuPlay.withoutBand(offer.option))
      .find(_.ref == source)

  test("it pays its cost and advances the marker from round 2 to 3, " +
      "leaving the Usurper Limiter on"):
    val start = clockAtRound(2).ready
    val done = accepted(use(start))
    assert(backToActing(done))
    val after = ready(done.state)
    assertEquals(after.game.current.tracks.round, 3)
    assert(after.game.current.tracks.usurperLimited)
    assertEquals(Look(after).tokensOn(clock), Tokens(1, 2))
    assertEquals(Look(after).favor(actor), 0)
    assertEquals(NoteText.said(GreatClock, done.events), Vector(NoteText.Said(
      NoteKey.Used, "Advanced the round marker to round 3.", covers = false)))
    assertEquals(replayed(start, done.events), Right(done.state))
    assertWireRoundTrips(done.events)

  test("advancing from round 3 to 4 takes the Usurper Limiter off"):
    val after = ready(accepted(use(clockAtRound(3).ready)).state)
    assertEquals(after.game.current.tracks.round, 4)
    assert(!after.game.current.tracks.usurperLimited)

  /** The events of the round's end after the Clock moves the marker from
    * `round` in an Empire, with `face` on any end die. The Chancellor holds
    * the Oathkeeper title at the round's end, since the title is settled
    * again after the Clock's command. */
  private def roundEndAfterClockFrom(round: Int, face: Int)
      : Vector[OathEvent] =
    val start = clockAtRound(round).usurperLimiter(onTrack = false)
      .imperial(actor, Table.p2).ready
    val advanced = ready(accepted(use(start)).state)
    accepted(StateBasedEvaluation.endRound(Ready(Table.from(advanced)
      .oathkeeper(Some(actor), TitleSide.Oathkeeper)
      .turn(actor, Phase.RoundEnd).ready), RoundEndChoices.endDie(face)))

  test("a jump from round 5 rolls no die for round 5; the round's end " +
      "rolls round 6's and moves on to round 7"):
    val events = roundEndAfterClockFrom(5, face = 1)
    assertEquals(events.collect { case EndDieRolled(round, _, _) => round },
      Vector(6))
    assert(events.contains(RoundEnded(6, Some(7))))

  test("a jump to round 8 rolls no die, and the round's end goes to War " +
      "Exhaustion"):
    val events = roundEndAfterClockFrom(7, face = 1)
    assert(!events.exists(_.isInstanceOf[EndDieRolled]))
    assert(events.contains(RoundEnded(8, None)))

  test("on round 8 the Clock is shown unavailable with the reason"):
    val last = clockAtRound(8).ready
    assertEquals(clockOnMenu(last).collect {
      case DecisionOption.Unavailable(_, reason) => reason },
      Some(OptionReason.Written(GreatClock.LastSpace)))
