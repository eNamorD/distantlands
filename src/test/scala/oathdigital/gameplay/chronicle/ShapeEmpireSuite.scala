package oathdigital.gameplay.chronicle

import oathdigital.catalog.Site
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import oathdigital.gameplay.powers.action.FaeMerchantCard
import oathdigital.gameplay.actions.challenge.SchoolOfVinesCard
import oathdigital.gameplay.powers.setup.GreatMarketCard
import oathdigital.gameplay.powers.whenplayed.ThreateningRoarCard
import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.{CardPlay, ProcedureNotes}
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.Table
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Shape Empire (CR p. 37, Chronicle rulings 6, 9, 11 and 14). p1 is the
  * Chancellor holding the Throne; the Empire's sites are the ones each test
  * gives an Imperial warband. */
class ShapeEmpireSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val ancientCity = AncientCitySite.id
  private val brokenPeaks = BrokenPeaksSite.id

  private def throneStarted(table: Table)(using munit.Location)
      : OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun))

  /** The Throne, started on `table`, walked to its end, every other
    * choice answered plainly. */
  private def throneDone(table: Table)(using munit.Location): ReadyGame =
    throneDone(table, throneStarted(table))

  /** `transition`, walked from `table`, to the Throne's end. */
  private def throneDone(table: Table, transition: OathTransition)(
      using munit.Location): ReadyGame = ChronicleWalk.endOf(rules,
    table.state, transition, TriggeredProcedureRef.ChronicleThrone)

  private def empireAt(sites: Site*): Table =
    sites.foldLeft(ChronicleWalk.throneHeldByChancellorP1)((table, site) =>
      table.warbandsAt(site, p1, 1))

  private def forcesAt(ready: ReadyGame, site: SiteId): SiteForces =
    ready.game.current.map.sites(site).forces

  test("the Chancellor may remove Imperial warbands but keeps the last one"):
    val table = empireAt(AncientCitySite, BrokenPeaksSite)
    // Offer Citizenship comes first; offering no one moves on.
    val started = ChronicleWalk.walkedTo(rules, throneStarted(table),
      ShapeEmpire.removeDecisionId)
    val parked = ChronicleWalk.parked(started.state).get
    assertEquals((parked.decisionId, parked.owner),
      (ShapeEmpire.removeDecisionId, p1))
    parked.query match
      case spread: DecisionQuery.Distribute =>
        assertEquals((spread.slots.map(_.maximum), spread.minTotal,
          spread.maxTotal), (Vector(1, 1), 0, 1))
      case other => fail(other.toString)
    val removed = accepted(rules.resolveWalker(started.state, p1,
      ShapeEmpire.removeDecisionId, DecisionAnswer.DistributeAnswer(Vector(
        DistributeAmount(DecisionOptionRef.Site(ancientCity), 0),
        DistributeAmount(DecisionOptionRef.Site(brokenPeaks), 1)))))
    val after = throneDone(table,
      OathTransition(removed.state, started.events ++ removed.events))
    assertEquals(forcesAt(after, brokenPeaks), SiteForces.Empty)
    assertEquals(forcesAt(after, ancientCity),
      SiteForces.Occupied(ForceKind.Imperial, 1))

  test("one Imperial warband on the map asks nothing"):
    val started = throneStarted(empireAt(AncientCitySite))
    assert(!ChronicleWalk.parked(started.state)
      .exists(_.decisionId == ShapeEmpire.removeDecisionId))

  test("with no Empire site the pawn's site gets one Imperial warband"):
    val after = throneDone(ChronicleWalk.throneHeldByChancellorP1
      .bandits(AncientCitySite, 2))
    assertEquals(forcesAt(after, ancientCity),
      SiteForces.Occupied(ForceKind.Imperial, 1))

  test("denizens at sites the Empire does not rule go to the next region's " +
      "discard pile"):
    val kept = ThreateningRoarCard.id
    val lost = FaeMerchantCard.id
    val after = throneDone(empireAt(BrokenPeaksSite)
      .denizen(ThreateningRoarCard, at = BrokenPeaksSite)
      .denizen(FaeMerchantCard, at = BuriedGiantSite))
    val map = after.game.current.map
    assert(map.sites(brokenPeaks).denizens.exists(_.id == kept))
    val region = map.regionOf(BuriedGiantSite.id).get
    assert(after.game.current.commonCards.discard(CardPlay.nextRegion(region))
      .contains(lost))

  test("intact edifices stay and ruined ones leave the map; Build Edifice " +
      "returns the set-aside one to the deck"):
    val intact = GreatMarketCard.id
    val ruined = SchoolOfVinesCard.id
    val after = throneDone(empireAt(AncientCitySite)
      .edifice(GreatMarketCard, EdificeSide.Intact, at = BrokenPeaksSite)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = BuriedGiantSite))
    assert(after.game.current.map.sites(brokenPeaks).denizens
      .exists(_.id == intact))
    assert(!after.game.current.map.sites(BuriedGiantSite.id)
      .denizens.exists(_.id == ruined))
    assert(after.game.current.commonCards.edificeDeck.contains(ruined))

  test("cards at an Enduring site stay"):
    val card = ThreateningRoarCard.id
    val after = throneDone(empireAt(BrokenPeaksSite)
      .denizen(ThreateningRoarCard, at = AncientCitySite))
    assert(after.game.current.map.sites(ancientCity).denizens
      .exists(_.id == card))

  test("each discard writes its line"):
    val lost = FaeMerchantCard.id
    val buriedGiant = BuriedGiantSite.id
    val done = ChronicleWalk.answeringEverything(rules, throneStarted(
      empireAt(BrokenPeaksSite).denizen(FaeMerchantCard, at = BuriedGiantSite)))
    val id = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
    assert(NoteText.said(id, ChronicleThroneProcedure.noteKeys, done.events)
      .map(_.text).contains(s"${lost.value} was discarded from " +
        s"${buriedGiant.value}"), done.events)
