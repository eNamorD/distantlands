package oathdigital.gameplay.powers.action

import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powers.{NoteText, PhasePowerCatalog}
import oathdigital.gameplay.walker.WalkerPowers
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{Look, MenuPlay, PhasePowerHarness, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** Imperial Seal (R26): "ACTION: If no Imperial pawns are at your site,
  * move any number of Imperial warbands from your site to any Imperial
  * site or Imperial player board." (catalog batch 10, slice 1, ruling 4).
  */
class ImperialSealSuite extends munit.FunSuite:
  import PhasePowerHarness._
  private val source = DecisionOptionRef.Relic(ImperialSealCard.id)
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  private val giant = BuriedGiantSite.id

  /** p1, an Exile, acts at Ancient City holding the Seal with a secret to
    * pay. p2 is the Chancellor at Broken Peaks and p3 a Citizen at Buried
    * Giant. The Empire holds Ancient City with 3 warbands and Broken Peaks
    * with 1. */
  private def exileAmongImperialWarbands: Table =
    Table.start.imperial(p2, p3).turn(p1, Phase.Act)
      .pawn(p1, at = home).pawn(p2, at = peaks).pawn(p3, at = giant)
      .relic(p1, ImperialSealCard).secrets(p1, faceUp = 1)
      .warbandsAt(home, p2, 3).warbandsAt(peaks, p2, 1)

  private def imperialAt(ready: ReadyGame, site: SiteId): Int =
    ready.game.current.map.sites(site).forces match
      case SiteForces.Occupied(ForceKind.Imperial, n) => n
      case _ => 0

  /** The Seal's last step, and the events of all three steps in order. */
  private final case class SealMoved(answered: OathTransition,
      events: Vector[OathEvent])

  private def moving(ready: ReadyGame, count: Int,
      to: DecisionOptionRef): SealMoved =
    val started = accepted(use(ready, ImperialSeal, source))
    val counted = accepted(answer(started, p1, ImperialSeal.countDecisionId,
      DecisionAnswer.ChooseAmountAnswer(count)))
    val answered = accepted(answer(counted, p1,
      ImperialSeal.destinationDecisionId, pick(to)))
    SealMoved(answered,
      started.events ++ counted.events ++ answered.events)

  test("it moves the chosen warbands to another Imperial site"):
    val ready = exileAmongImperialWarbands.ready
    val done = moving(ready, 2, DecisionOptionRef.Site(peaks))
    val end = after(done.answered)
    assertEquals(imperialAt(end, home), 1)
    assertEquals(imperialAt(end, peaks), 3)
    assertEquals(NoteText.said(ImperialSeal, done.answered.events), Vector(
      NoteText.Said(NoteKey.Used,
        s"Moved 2 Imperial warbands to ${peaks.value}.", covers = true)))

  test("it moves them to an Imperial player's board"):
    val ready = exileAmongImperialWarbands.ready
    val before = Look(ready).player(p3).board.warbands
    val done = moving(ready, 1, DecisionOptionRef.Player(p3))
    assertEquals(Look(after(done.answered)).player(p3).board.warbands,
      before + 1)
    assertEquals(NoteText.said(ImperialSeal, done.answered.events), Vector(
      NoteText.Said("used.board",
        s"Moved 1 Imperial warband to ${p3.value}'s board.", covers = true)))

  test("it offers the other Imperial sites and every Imperial board, not " +
      "its own site"):
    val started = accepted(use(exileAmongImperialWarbands.ready,
      ImperialSeal, source))
    val counted = accepted(answer(started, p1, ImperialSeal.countDecisionId,
      DecisionAnswer.ChooseAmountAnswer(1)))
    assertEquals(offered(counted, p1).toVector.flatten.map(_._2).toSet,
      Set[DecisionOptionRef](DecisionOptionRef.Site(peaks),
        DecisionOptionRef.Player(p2), DecisionOptionRef.Player(p3))
        .map(_.wireId))

  /** The Seal's option on `player`'s Act menu, or `None` when the menu
    * leaves it out. */
  private def sealOnMenu(ready: ReadyGame, player: PlayerId)
      : Option[DecisionOption] =
    PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, ready, player,
      PhasePowerCatalog.default, WalkerPowers.empty)
      .map(offer => MenuPlay.withoutBand(offer.option))
      .find(_.ref == source)

  private def reasonOnMenu(ready: ReadyGame): Option[OptionReason] =
    sealOnMenu(ready, p1).collect {
      case DecisionOption.Unavailable(_, reason) => reason }

  test("an Imperial pawn at the site disables it with the reason"):
    val withPawn = exileAmongImperialWarbands.pawn(p3, at = home).ready
    assertEquals(reasonOnMenu(withPawn),
      Some(OptionReason.Written(ImperialSeal.ImperialPawnHere)))

  test("no Imperial warbands at the site disables it with the reason"):
    val noWarbands = Table.start.imperial(p2, p3).turn(p1, Phase.Act)
      .pawn(p1, at = home).relic(p1, ImperialSealCard)
      .secrets(p1, faceUp = 1).warbandsAt(peaks, p2, 1).ready
    assertEquals(reasonOnMenu(noWarbands),
      Some(OptionReason.Written(ImperialSeal.NoImperialWarbands)))

  test("an Imperial holder's menu leaves it out"):
    val citizenHolds = Table.start.imperial(p2, p3).turn(p3, Phase.Act)
      .pawn(p3, at = home).relic(p3, ImperialSealCard)
      .secrets(p3, faceUp = 1).warbandsAt(home, p2, 3).ready
    assertEquals(sealOnMenu(citizenHolds, p3), None)

  test("moving every warband empties the site, and bandits refill it"):
    val ready = exileAmongImperialWarbands.ready
    val end = after(moving(ready, 3, DecisionOptionRef.Site(peaks)).answered)
    assertEquals(imperialAt(end, home), 0)
    assert(end.game.current.map.sites(home).forces match
      case SiteForces.Occupied(ForceKind.Bandit, _) => true
      case _ => false)
    assertEquals(imperialAt(end, peaks), 4)

  test("replay rebuilds the same game"):
    val ready = exileAmongImperialWarbands.ready
    val done = moving(ready, 2, DecisionOptionRef.Site(peaks))
    assertEquals(replayed(ready, done.events), Right(done.answered.state))
