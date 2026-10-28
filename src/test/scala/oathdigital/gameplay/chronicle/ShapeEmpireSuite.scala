package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.{CardPlay, ProcedureNotes}
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.testkit.{CatalogNames, Outcomes, Table}
import oathdigital.testkit.Table.p1
import oathdigital.testkit.Unwrap.accepted

/** Shape Empire (CR p. 37, Chronicle rulings 6, 9, 11 and 14). p1 is the
  * Chancellor holding the Throne; the Empire's sites are the ones each test
  * gives an Imperial warband. */
class ShapeEmpireSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val ancientCity = CatalogNames.site("Ancient City")
  private val brokenPeaks = CatalogNames.site("Broken Peaks")

  private def throneStarted(table: Table)(using munit.Location)
      : OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun))

  /** The Throne walked to its end, every other choice answered plainly. */
  private def throneDone(transition: OathTransition)(using munit.Location)
      : ReadyGame =
    Outcomes.ready(ChronicleWalk.answeringEverything(rules, transition).state)

  private def empireAt(sites: String*): Table =
    sites.foldLeft(ChronicleWalk.throneHeldByChancellorP1)((table, site) =>
      table.warbandsAt(site, p1, 1))

  private def forcesAt(ready: ReadyGame, site: SiteId): SiteForces =
    ready.game.current.map.sites(site).forces

  test("the Chancellor may remove Imperial warbands but keeps the last one"):
    val started = throneStarted(empireAt("Ancient City", "Broken Peaks"))
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
    val after = throneDone(OathTransition(removed.state, removed.events))
    assertEquals(forcesAt(after, brokenPeaks), SiteForces.Empty)
    assertEquals(forcesAt(after, ancientCity),
      SiteForces.Occupied(ForceKind.Imperial, 1))

  test("one Imperial warband on the map asks nothing"):
    val started = throneStarted(empireAt("Ancient City"))
    assert(!ChronicleWalk.parked(started.state)
      .exists(_.decisionId == ShapeEmpire.removeDecisionId))

  test("with no Empire site the pawn's site gets one Imperial warband"):
    val after = throneDone(throneStarted(ChronicleWalk.throneHeldByChancellorP1
      .bandits("Ancient City", 2)))
    assertEquals(forcesAt(after, ancientCity),
      SiteForces.Occupied(ForceKind.Imperial, 1))

  test("denizens at sites the Empire does not rule go to the next region's " +
      "discard pile"):
    val kept = CatalogNames.denizen("Threatening Roar")
    val lost = CatalogNames.denizen("Fae Merchant")
    val after = throneDone(throneStarted(empireAt("Broken Peaks")
      .denizen("Threatening Roar", at = "Broken Peaks")
      .denizen("Fae Merchant", at = "Buried Giant")))
    val map = after.game.current.map
    assert(map.sites(brokenPeaks).denizens.exists(_.id == kept))
    val region = map.regionOf(CatalogNames.site("Buried Giant")).get
    assert(after.game.current.commonCards.discard(CardPlay.nextRegion(region))
      .contains(lost))

  test("intact edifices stay and ruined ones are set aside"):
    val intact = CatalogNames.edifice("Great Market")
    val ruined = CatalogNames.edifice("Ruined School")
    val after = throneDone(throneStarted(empireAt("Ancient City")
      .edifice("Great Market", EdificeSide.Intact, at = "Broken Peaks")
      .edifice("Ruined School", EdificeSide.Ruined, at = "Buried Giant")))
    assert(after.game.current.map.sites(brokenPeaks).denizens
      .exists(_.id == intact))
    assert(after.game.current.setAsideEdifices.contains(ruined))

  test("cards at an Enduring site stay"):
    val card = CatalogNames.denizen("Threatening Roar")
    val after = throneDone(throneStarted(empireAt("Broken Peaks")
      .denizen("Threatening Roar", at = "Ancient City")))
    assert(after.game.current.map.sites(ancientCity).denizens
      .exists(_.id == card))

  test("each discard writes its line"):
    val lost = CatalogNames.denizen("Fae Merchant")
    val buriedGiant = CatalogNames.site("Buried Giant")
    val done = ChronicleWalk.answeringEverything(rules, throneStarted(
      empireAt("Broken Peaks").denizen("Fae Merchant", at = "Buried Giant")))
    val id = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
    assert(NoteText.said(id, ChronicleThroneProcedure.noteKeys, done.events)
      .map(_.text).contains(s"${lost.value} was discarded from " +
        s"${buriedGiant.value}"), done.events)
