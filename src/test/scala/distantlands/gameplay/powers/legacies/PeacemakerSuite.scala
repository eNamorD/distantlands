package distantlands.gameplay.powers.legacies

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** Peacemaker (L04): return a banner you hold to the shared bank, then
  * every site and board keeps one warband (Legacy in-play powers rulings
  * 8 and 13). */
class PeacemakerSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val p2 = Table.p2
  private val source = DecisionOptionRef.Legacy(PeacemakerCard.id)

  /** p1 holds Peacemaker and the People's Favor with 2 favor; p2 is the
    * Chancellor; p1 holds Ancient City with 2 warbands, p2 Broken Peaks
    * with 3 purple ones, and 3 bandits hold Buried Giant. Every board has
    * 3 warbands. */
  private def table: Table =
    Table.start.imperial(p2).turn(actor, Phase.Act)
      .legacy(actor, PeacemakerCard).peoplesFavor(Some(actor), 2)
      .warbandsAt(AncientCitySite, actor, 2)
      .warbandsAt(BrokenPeaksSite, p2, 3).bandits(BuriedGiantSite, 3)

  test("it returns the banner with its favor and leaves one warband on " +
      "every site and board, the holder's, the Empire's and the bandits'"):
    val ready = table.ready
    val done = accepted(use(ready, Peacemaker, source))
    val now = Look(after(done))
    assertEquals(now.peoplesFavorHolder, None)
    assertEquals(now.favorOnPeoplesFavor, 2)
    Vector(AncientCitySite, BrokenPeaksSite, BuriedGiantSite).foreach(site =>
      assertEquals(now.forces(site).total, 1, site.toString))
    after(done).game.current.players.foreach(player =>
      assertEquals(player.board.warbands, 1, player.player.toString))
    assertEquals(replayed(ready, done.events), Right(done.state))
    assertEquals(NoteText.said(Peacemaker, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${actor.value} returned the People's " +
        s"Favor with ${PeacemakerCard.id.value}", covers = false)))

  test("holding both banners, the holder chooses which; the other stays"):
    val ready = table.darkestSecret(Some(actor), 1).ready
    val done = useAndAnswer(ready, Peacemaker, source, Peacemaker.decisionId,
      DecisionOptionRef.Banner(Banner.DarkestSecret))
    val now = Look(after(done.answered))
    assertEquals(now.darkestSecretHolder, None)
    assertEquals(now.secretsOnDarkestSecret, 1)
    assertEquals(now.peoplesFavorHolder, Some(actor))

  test("a walk resumed at Peacemaker's question returns the chosen banner"):
    val ready = table.darkestSecret(Some(actor), 1).ready
    val started = accepted(use(ready, Peacemaker, source))
    assert(awaits(started, Peacemaker.decisionId))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      Peacemaker.decisionId,
      pick(DecisionOptionRef.Banner(Banner.DarkestSecret))))
    assertEquals(Look(after(done)).darkestSecretHolder, None)
    assertEquals(Look(after(done)).peoplesFavorHolder, Some(actor))

  test("it is not offered while the holder holds no banner (R4)"):
    val bare = Table.start.turn(actor, Phase.Act)
      .legacy(actor, PeacemakerCard).ready
    assert(!usableNow(bare).exists(_.power.id == Peacemaker.id))
    assertEquals(use(bare, Peacemaker, source).left.toOption,
      Some(OathViolation.StartUnavailable("You hold no banner")))
