package oathdigital.gameplay.powers.action

import oathdigital.catalog.AdviserOnly
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.operations.HallOfMinistersCard
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powers.{NoteText, PhasePowerCatalog,
  PowerFixture, SiteRoom, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{ActPowerHarness, Look, MenuPlay,
  PhasePowerHarness, Table}
import oathdigital.testkit.Outcomes.{assertWireRoundTrips, ready}
import oathdigital.testkit.Unwrap.accepted

/** Giant's Path (catalog batch 10, slice 4): move the locked Path to
  * another site with room, tokens and all, then place the user's pawn
  * there. The Path's own use ignores its lock; nothing else does. */
class GiantsPathSuite extends munit.FunSuite:
  import ActPowerHarness.{answer, replayed, rulesWithPrintedRestrictions,
    use}
  import PowerFixture._

  private val path = GiantsPathCard.id
  private val source = DecisionOptionRef.Edifice(path)
  private val home = AncientCitySite.id
  private val peaks = BrokenPeaksSite.id
  /** The production walker powers, so the printed locks hold. */
  private val rules0 = rulesWithPrintedRestrictions()
  private val walkerPowers = WalkerPowerCatalog.default(catalog)

  /** p1's Act with 1 faceup secret, the intact Path at Ancient City and
    * p1's pawn at `pawnAt`. */
  private def pathTable(pawnAt: SiteId = home): Table =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, pawnAt)
      .edifice(path, EdificeSide.Intact, at = home).secrets(actor, 1)

  /** A site in play that is neither Ancient City nor Broken Peaks. */
  private def thirdSite(ready: ReadyGame): SiteId =
    ready.game.current.map.inPlay.filterNot(Set(home, peaks)).head

  /** `table` with every empty slot at `sites` filled by a denizen that has
    * no persistent power and is not adviser-only. */
  private def withSitesFilled(table: Table, sites: Vector[SiteId]): Table =
    val fillers = catalog.denizens.iterator.filter(card =>
      !card.isInstanceOf[AdviserOnly] && card.powers.forall(!_.persistent))
    sites.foldLeft(table)((filled, site) =>
      SiteRoom.emptySlots(catalog, filled.ready, site).foldLeft(filled)(
        (more, _) => more.denizen(fillers.next().id, at = site)))

  private def toSite(site: SiteId) = DecisionOptionRef.Site(site)

  /** The Path's option in p1's Act menu, as the menu offers it. */
  private def menuOption(state: ReadyGame): Option[DecisionOption] =
    PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, state, actor,
      PhasePowerCatalog.default, walkerPowers)
      .map(offer => MenuPlay.withoutBand(offer.option))
      .find(_.ref == source)

  /** The code of the first restriction refusing `card`'s move from Ancient
    * City to Broken Peaks during `procedure`. */
  private def lockRefusal(procedure: ProcedureRef, card: CardId,
      state: ReadyGame): Option[String] =
    val move = Move(Piece.Card(card), PositionedLocation(Location.Site(home)),
      PositionedLocation(Location.Site(peaks)), by = Some(actor))
    walkerPowers.restrictionsDuring(Some(procedure))
      .flatMap(_.reason(state, move)).headOption.map(_.code)

  test("it pays a secret, moves the Path with its tokens to the chosen " +
      "site and places the pawn there, with the line, and the journal " +
      "replays and round trips"):
    val start = pathTable().ready
    val parked = accepted(use(rules0, start, GiantsPath.id, source))
    ActPowerHarness.parkedWithWalkerPowers.assertParked(parked.state,
      ActionRef.UsePower(GiantsPath.id), GiantsPath.decisionId, actor)
    assertEquals(Look(ready(parked.state)).tokensOn(path), Tokens(0, 1))
    val done = accepted(answer(rules0, parked.state, GiantsPath.decisionId,
      toSite(peaks)))
    val after = ready(done.state)
    assertEquals(Look(after).cardsAt(peaks), Vector[CardId](path))
    assertEquals(Look(after).cardsAt(home), Vector.empty)
    assertEquals(Look(after).tokensOn(path), Tokens(0, 1))
    assertEquals(Look(after).pawn(actor), peaks)
    PhasePowerHarness.parked.assertResumed(done.state, Phase.Act, actor)
    assertEquals(NoteText.said(GiantsPath, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"Giant's Path and ${actor.value} now " +
        s"stand at ${peaks.value}.", covers = true)))
    assertEquals(replayed(rules0, start, parked.events ++ done.events), after)
    assertWireRoundTrips(parked.events ++ done.events)

  test("used from afar at a site the user rules, the pawn is placed from " +
      "where it stood"):
    val start = pathTable(pawnAt = peaks).warbandsAt(home, actor, 1).ready
    val third = thirdSite(start)
    val parked = accepted(use(rules0, start, GiantsPath.id, source))
    val after = ready(accepted(answer(rules0, parked.state,
      GiantsPath.decisionId, toSite(third))).state)
    assertEquals(Look(after).cardsAt(third), Vector[CardId](path))
    assertEquals(Look(after).pawn(actor), third)

  test("its own site and a full site are not destinations"):
    val start = withSitesFilled(pathTable(), Vector(peaks)).ready
    assertEquals(GiantsPath.destinations(catalog, start),
      start.game.current.map.inPlay.filterNot(Set(home, peaks)))
    val parked = accepted(use(rules0, start, GiantsPath.id, source))
    assert(answer(rules0, parked.state, GiantsPath.decisionId,
      toSite(home)).isLeft, "its own site")
    assert(answer(rules0, parked.state, GiantsPath.decisionId,
      toSite(peaks)).isLeft, "a full site")

  test("with no other site in room it is shown unavailable with the " +
      "reason, and a direct use is refused"):
    val open = pathTable().ready
    assert(menuOption(open).exists {
      case _: DecisionOption.Unavailable => false
      case _ => true
    }, "available while a site has room")
    val full = withSitesFilled(pathTable(),
      open.game.current.map.inPlay.filterNot(_ == home)).ready
    assertEquals(menuOption(full).collect {
      case DecisionOption.Unavailable(_, reason) => reason },
      Some(OptionReason.Written(GiantsPath.NoRoom)))
    assertEquals(use(rules0, full, GiantsPath.id, source).left.toOption,
      Some(OathViolation.StartUnavailable(GiantsPath.NoRoom)))

  test("the Path's lock binds every other power, and its own use still " +
      "cannot move another locked card"):
    assertEquals(walkerPowers.lockExemptions,
      Map[PowerId, CardId](GiantsPath.id -> path))
    val state = pathTable()
      .edifice(HallOfMinistersCard, EdificeSide.Intact, at = home).ready
    assertEquals(lockRefusal(ActionRef.UsePower(GiantsRampage.id), path,
      state), Some("locked"))
    assertEquals(lockRefusal(ActionRef.UsePower(GiantsPath.id),
      HallOfMinistersCard.id, state), Some("locked"))
    assertEquals(lockRefusal(ActionRef.UsePower(GiantsPath.id), path, state),
      None)
