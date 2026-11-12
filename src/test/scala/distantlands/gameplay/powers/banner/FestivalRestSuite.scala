package distantlands.gameplay.powers.banner

import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.actions.cardplay.CardPlayProcedure
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.action.{GrandScepterAction, MurkyFountain,
  WizardSchool, WizardSchoolCard}
import distantlands.gameplay.powers.travel.{SpecialEnvoy, SpecialEnvoyCard,
  TravelFixture}
import distantlands.gameplay.powers.wake.{MarbleFountainsCard, TrackerAction,
  TrackerCard}
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, MenuPlay, Outcomes,
  PhasePowerHarness, PowerRules, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Festival, the Darkest Secret's altered face: "REST: You must place the
  * festival marker over the cards at your site." (Foundations design,
  * slice 6). The holder's Rest places it, asking nothing. */
class FestivalRestSuite extends munit.FunSuite:
  private val rules = PowerRules.withWalkerPowers
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id

  /** p1's Act at Ancient City, holding the Darkest Secret on its Festival
    * face, its marker beside the map. */
  private def festivalHolderActingAtHome: Table =
    Table.start.turn(p1, Phase.Act).pawn(p1, at = home)
      .darkestSecret(Some(p1), 1, DarkestSecretFace.Festival)

  /** p1 ends the Act and rests, through to the next player's turn. */
  private def rested(ready: ReadyGame): OathTransition =
    accepted(MenuPlay.started(rules, Ready(ready), p1,
      PhaseTransitionRef.BeginRest))

  private def marker(transition: OathTransition): Option[SiteId] =
    Outcomes.ready(transition.state).game.current.banners.darkestSecret
      .festival

  private def festivalSaid(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(DarkestSecretFestival.id, DarkestSecretFestival.noteKeys,
      events)

  test("the holder's Rest places the marker over the cards at their " +
      "pawn's site, with its line"):
    val done = rested(festivalHolderActingAtHome.ready)
    assertEquals(marker(done), Some(home))
    assertEquals(festivalSaid(done.events), Vector(NoteText.Said("held",
      s"Held the festival at ${home.value}", covers = false)))

  test("a later Rest moves it to the site the holder's pawn stands at now"):
    val done = rested(festivalHolderActingAtHome.festival(peaks).ready)
    assertEquals(marker(done), Some(home))

  test("nobody else's Rest moves it: not while an enemy holds it, not " +
      "while it is unheld, and not on the Wandering Flame"):
    val others: Vector[(String, Table)] = Vector(
      "held by p2" -> festivalHolderActingAtHome.festival(peaks)
        .darkestSecret(Some(p2), 1, DarkestSecretFace.Festival),
      "unheld" -> festivalHolderActingAtHome.festival(peaks)
        .darkestSecret(None, 1, DarkestSecretFace.Festival),
      "Wandering Flame" -> festivalHolderActingAtHome
        .darkestSecret(Some(p1), 1, DarkestSecretFace.WanderingFlame))
    others.foreach { case (label, table) =>
      val ready = table.ready
      val done = rested(ready)
      assertEquals(marker(done),
        ready.game.current.banners.darkestSecret.festival, label)
      assertEquals(festivalSaid(done.events), Vector.empty, label)
    }

  test("the Rest's events replay to the same marker and round-trip the " +
      "wire"):
    val ready = festivalHolderActingAtHome.festival(peaks).ready
    val done = rested(ready)
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events),
      Outcomes.ready(done.state))
    assertWireRoundTrips(done.events)

  // ---- every way into Rest ------------------------------------------------

  /** Every walker and phase power: the ACTIONs below are phase powers. */
  private val withPowers = PowerRules.withWalkerAndPhasePowers

  /** `events` place the marker once, at `site`, and write its line once. */
  private def placedOnceAt(events: Vector[OathEvent], end: OathState,
      site: SiteId)(using munit.Location): Unit =
    assertEquals(Outcomes.ready(end).game.current.banners.darkestSecret
      .festival, Some(site))
    assertEquals(PhasePowerHarness.ops(events).count {
      case _: SetFestivalMarker => true
      case _ => false
    }, 1)
    assertEquals(festivalSaid(events).map(_.key), Vector("held"))

  test("Wizard School ends the Act into a Rest that places the marker once"):
    val ready = festivalHolderActingAtHome.festival(peaks)
      .denizen(WizardSchoolCard.id, at = home).favor(p1, 2).ready
    val done = accepted(ActPowerHarness.use(withPowers, ready, WizardSchool.id,
      DecisionOptionRef.Denizen(WizardSchoolCard.id)))
    placedOnceAt(done.events, done.state, home)

  test("Murky Fountain's zero ends the Act into a Rest that places the " +
      "marker once"):
    val blank = ActPowerHarness.rulesWithPrintedRestrictions(
      ActPowerHarness.defenseDice(DefenseDieFace.Blank, DefenseDieFace.Blank))
    val fountain = MarbleFountainsCard.id
    val ready = festivalHolderActingAtHome.festival(peaks)
      .edifice(fountain, EdificeSide.Ruined, at = home).secrets(p1, 1).ready
    val done = accepted(ActPowerHarness.use(blank, ready, MurkyFountain.id,
      DecisionOptionRef.Edifice(fountain)))
    placedOnceAt(done.events, done.state, home)

  test("Tracker ends the Act into a Rest that places the marker once"):
    val ready = festivalHolderActingAtHome.festival(peaks).pawn(p2, at = home)
      .adviser(p1, TrackerCard.id).ready
    val parked = accepted(ActPowerHarness.use(withPowers, ready, TrackerAction.id,
      DecisionOptionRef.Denizen(TrackerCard.id)))
    val done = accepted(withPowers.resolveWalker(parked.state, p1,
      TrackerAction.decisionId,
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(p2))))
    placedOnceAt(parked.events ++ done.events, done.state, home)

  test("a Travel with Special Envoy ends the Act into a Rest that places " +
      "the marker once, at the site travelled to"):
    import TravelFixture.{board, coast, on}
    val ready = on(board(supply = 7))(_.adviser(p1, SpecialEnvoyCard.id)
      .darkestSecret(Some(p1), 1, DarkestSecretFace.Festival).festival(peaks))
    val done = accepted(TravelFixture.travel(ready, coast,
      Vector(SpecialEnvoy.id)))
    placedOnceAt(done.events, done.state, coast)

  test("a Citizen's self-exile ends the Act into a Rest that places the " +
      "marker once"):
    val ready = festivalHolderActingAtHome.festival(peaks).imperial(p2, p1)
      .boardWarbands(p1, 4).favor(p1, 6).ready
    val done = accepted(withPowers.startWalker(Ready(MenuPlay.withoutMenu(ready)),
      ActionRef.SelfExile, p1))
    placedOnceAt(done.events, done.state, home)

  test("an Exile becoming a Citizen by the Grand Scepter ends the Act into " +
      "a Rest that places the marker once"):
    val ready = festivalHolderActingAtHome.festival(peaks).imperial(p2)
      .relic(p1, TheGrandScepterCard).boardWarbands(p1, 2)
      .allowingCitizenshipPowers.ready
    val done = accepted(ActPowerHarness.use(withPowers, ready,
      GrandScepterAction.id, DecisionOptionRef.Relic(TheGrandScepterCard.id)))
    placedOnceAt(done.events, done.state, home)

  test("a role change that parks on a False Prophet claim still ends the " +
      "Act into a Rest that places the marker once"):
    // p1, an Exile with p2's warband claiming p1's Conquest, becomes a
    // Citizen by the Grand Scepter: the discard asks p2 first, and the
    // walk resumes after the role change has moved the turn to Rest.
    val conquest = VisionRules.Conquest
    val ready = festivalHolderActingAtHome.festival(peaks).imperial(p3)
      .relic(p1, TheGrandScepterCard).boardWarbands(p1, 2)
      .pawn(p2, at = peaks).revealedVision(p1, conquest)
      .warbandsOnCard(conquest, p2, 1).allowingCitizenshipPowers.ready
    val parked = accepted(ActPowerHarness.use(withPowers, ready,
      GrandScepterAction.id, DecisionOptionRef.Relic(TheGrandScepterCard.id)))
    val done = accepted(withPowers.resolveWalker(parked.state, p2,
      CardPlayProcedure.placementDecisionId(conquest),
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.RevealedVision(p2))))
    placedOnceAt(parked.events ++ done.events, done.state, home)
