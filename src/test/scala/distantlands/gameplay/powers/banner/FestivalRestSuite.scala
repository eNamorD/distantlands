package distantlands.gameplay.powers.banner

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, MenuPlay, Outcomes, PowerRules,
  Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.{p1, p2}
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
