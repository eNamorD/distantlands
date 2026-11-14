package distantlands.gameplay.powers.legacies

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.TheEarCard
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.action.BrassHorseCard
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.economy.CupOfPlentyCard
import distantlands.model._
import distantlands.testkit.{PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** The Ear (L19), ACTION: peek at the relics at any site (Legacy in-play
  * powers rulings 12 and 13; implementation rulings R2-R4). */
class TheEarActionSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val source = DecisionOptionRef.Legacy(TheEarCard.id)
  private val peaks = DecisionOptionRef.Site(BrokenPeaksSite.id)

  /** p1 at Ancient City holds The Ear active; Broken Peaks holds Sticky
    * Fire and Brass Horse, Ancient City holds Cup of Plenty. */
  private def table: Table =
    Table.start.turn(actor, Phase.Act).pawn(actor, AncientCitySite)
      .legacy(actor, TheEarCard).relicAt(StickyFireCard, BrokenPeaksSite)
      .relicAt(BrassHorseCard, BrokenPeaksSite)
      .relicAt(CupOfPlentyCard, AncientCitySite)
  private def usable(ready: ReadyGame): Boolean =
    usableNow(ready).exists(_.power.id == TheEarAction.id)
  private def known(ready: ReadyGame, site: SiteId): Vector[RelicId] =
    ready.knowledge.siteRelics.getOrElse(actor, Map.empty)
      .getOrElse(site, Vector.empty)

  test("it is free, offers every site with a relic the holder has not " +
      "seen, their own included, and peeks at all the chosen site's " +
      "relics (R2)"):
    val ready = table.ready
    assert(usable(ready))
    val started = accepted(use(ready, TheEarAction, source))
    assertEquals(offered(started, actor).map(_.toSet),
      Some(Set("site" -> AncientCitySite.id.value,
        "site" -> BrokenPeaksSite.id.value)))
    val done = accepted(answer(started, actor, TheEarAction.decisionId,
      pick(peaks)))
    assertEquals(known(after(done), BrokenPeaksSite.id).toSet,
      Set(StickyFireCard.id, BrassHorseCard.id))
    assertEquals(after(done).game.current.player(actor).map(_.board.favor),
      ready.game.current.player(actor).map(_.board.favor))
    assertEquals(replayed(ready, started.events ++ done.events),
      Right(done.state))
    assertEquals(NoteText.said(TheEarAction, started.events ++ done.events),
      Vector(NoteText.Said(NoteKey.Used, s"${actor.value} peeked at the " +
        s"relics at ${BrokenPeaksSite.id.value} with " +
        s"${TheEarCard.id.value}", covers = true)))

  test("the holder keeps knowing them after the action, so a second use " +
      "offers only the site still unseen"):
    val peeked = after(accepted(answer(accepted(use(table.ready,
      TheEarAction, source)), actor, TheEarAction.decisionId, pick(peaks))))
    val again = accepted(use(peeked, TheEarAction, source))
    assertEquals(offered(again, actor),
      Some(Vector("site" -> AncientCitySite.id.value)))

  test("a site whose relics the holder knows is not offered, and with " +
      "none left it is not offered at all (R4)"):
    val seen = table.knowsRelicAt(actor, StickyFireCard, BrokenPeaksSite)
      .knowsRelicAt(actor, BrassHorseCard, BrokenPeaksSite)
    val started = accepted(use(seen.ready, TheEarAction, source))
    assertEquals(offered(started, actor),
      Some(Vector("site" -> AncientCitySite.id.value)))
    val all = seen.knowsRelicAt(actor, CupOfPlentyCard, AncientCitySite).ready
    assert(!usable(all))
    assertEquals(use(all, TheEarAction, source).left.toOption,
      Some(OathViolation.StartUnavailable(
        "No site holds a relic you have not seen")))

  test("a walk resumed at The Ear's question peeks at the chosen site"):
    val ready = table.ready
    val started = accepted(use(ready, TheEarAction, source))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      TheEarAction.decisionId, pick(peaks)))
    assertEquals(known(after(done), BrokenPeaksSite.id).size, 2)
