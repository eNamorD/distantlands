package distantlands.gameplay.powers

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.powers.action.{RovingTerror, RovingTerrorCard,
  SiteSlotMoves, TamingCharm, TamingCharmCard}
import distantlands.gameplay.powers.campaign.WildMountsCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{PhasePowerHarness, RecordedOps, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Each power's discard names the power whose use discards it (catalog
  * batch 11, slice 4), so Wild Pastures can tell a power's discard from a
  * rule's. */
class PowerDiscardCausesSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val wildMounts = WildMountsCard.id
  private val errandBoy = ErrandBoyCard.id

  /** Every discard the events recorded. */
  private def discards(events: Vector[OathEvent]): Vector[Discard.Denizen] =
    RecordedOps.all(events).collect { case d: Discard.Denizen => d }

  test("Taming Charm's discard names Taming Charm, making no room"):
    val ready = Table.start.turn(p1, Phase.Act).pawn(p1, at = home)
      .adviser(p1, TamingCharmCard.id).denizen(wildMounts, at = home)
      .bankFavor(Suit.Nomad, 5).ready
    val started = accepted(use(ready, TamingCharm,
      DecisionOptionRef.Denizen(TamingCharmCard.id)))
    val done = accepted(answer(started, p1, TamingCharm.decisionId,
      ChooseOneAnswer(DecisionOptionRef.Denizen(wildMounts))))
    assertEquals(discards(done.events).map(_.cause),
      Vector(Some(DiscardCause(TamingCharm.id))))

  test("a mover's replace makes room only at a full site"):
    val slots = new SiteSlotMoves(RovingTerrorCard.id, RovingTerror.id,
      empty = false)
    def heldAt(ready: ReadyGame, site: SiteId): Int =
      ready.game.current.map.sites(site).denizens.size
    def capacityOf(site: SiteId): Int =
      catalog.site(site).map(_.capacity).getOrElse(0)
    // Errand Boy goes to a site that keeps room after it, and Wild Mounts
    // fills Broken Peaks.
    val start = Table.start.denizen(RovingTerrorCard.id, at = home).ready
    val roomy = start.game.current.map.inPlay.find(site => site != home &&
      heldAt(start, site) < capacityOf(site) - 1)
    assert(roomy.nonEmpty)
    val fillers = (wildMounts +: SearchFixture.anyPlainDenizensOf(Suit.Hearth))
      .take(capacityOf(peaks) - heldAt(start, peaks))
    val ready = roomy.fold(start)(site => fillers.foldLeft(
      Table.from(start).denizen(errandBoy, at = site))(
      _.denizen(_, at = peaks)).ready)
    assertEquals(heldAt(ready, peaks), capacityOf(peaks))
    val replacing = slots.moves(catalog, ready).filter(_.replaced.nonEmpty)
    assert(replacing.exists(move => roomy.contains(move.site)))
    assert(replacing.exists(_.site == peaks))
    replacing.foreach(move => assertEquals(move.makesRoom,
      heldAt(ready, move.site) == capacityOf(move.site)))
