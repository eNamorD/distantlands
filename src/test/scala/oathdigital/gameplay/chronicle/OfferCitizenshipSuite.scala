package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.{ChooseManyAnswer, ChooseOneAnswer}
import oathdigital.testkit.{CatalogNames, Look, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** Offer Citizenship, the Throne's first step (CR p. 37, Chronicle rulings
  * 20-22). p1 is the Chancellor holding the Throne; p2 and p3 are Exiles. */
class OfferCitizenshipSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)
  private val ancientCity = CatalogNames.site("Ancient City")
  private val brokenPeaks = CatalogNames.site("Broken Peaks")

  /** p2 rules Ancient City with 3 warbands and holds 2 on their board; p3
    * rules Broken Peaks with 1. */
  private def exilesHoldSites: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(ancientCity, p2, 3).boardWarbands(p2, 2)
    .warbandsAt(brokenPeaks, p3, 1)

  private def throneStarted(table: Table)(using munit.Location)
      : OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleSun))

  private def answer(step: OathTransition, by: PlayerId, decisionId: String,
      value: DecisionAnswer)(using munit.Location): OathTransition =
    val next = accepted(rules.resolveWalker(step.state, by, decisionId, value))
    OathTransition(next.state, step.events ++ next.events)

  private def offered(step: OathTransition, who: PlayerId*)(
      using munit.Location): OathTransition =
    answer(step, p1, OfferCitizenship.offerDecisionId, ChooseManyAnswer(
      who.toVector.map(DecisionOptionRef.Player(_))))

  private def replied(step: OathTransition, by: PlayerId,
      ref: DecisionOptionRef)(using munit.Location): OathTransition =
    answer(step, by, OfferCitizenship.answerDecisionId(by),
      ChooseOneAnswer(ref))

  private def lines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone),
      OfferCitizenship.noteKeys, events).map(_.text)

  private def ready(step: OathTransition): ReadyGame =
    Outcomes.ready(step.state)

  test("the Throne holder is asked first whom to offer, any of the seated " +
      "Exiles or none"):
    val parked = ChronicleWalk.parked(throneStarted(exilesHoldSites).state).get
    assertEquals((parked.decisionId, parked.owner),
      (OfferCitizenship.offerDecisionId, p1))
    parked.query match
      case many: DecisionQuery.ChooseMany =>
        assertEquals((many.min, many.max, many.options.map(_.ref)), (0, 2,
          Vector(DecisionOptionRef.Player(p2), DecisionOptionRef.Player(p3))))
      case other => fail(other.toString)

  test("offering no one moves on to Shape Empire"):
    val next = offered(throneStarted(exilesHoldSites))
    assert(!ChronicleWalk.parked(next.state).exists(_.decisionId
      .startsWith("chronicle.throne.citizenship")))
    assertEquals(lines(next.events), Vector.empty)

  test("each offered Exile answers in seat order, Decline first"):
    val asked = offered(throneStarted(exilesHoldSites), p3, p2)
    val first = ChronicleWalk.parked(asked.state).get
    assertEquals((first.decisionId, first.owner),
      (OfferCitizenship.answerDecisionId(p2), p2))
    first.query match
      case one: DecisionQuery.ChooseOne => assertEquals(
        one.options.map(_.ref).head, OfferCitizenship.decline)
      case other => fail(other.toString)
    val second = replied(asked, p2, OfferCitizenship.decline)
    assertEquals(ChronicleWalk.parked(second.state).map(_.owner), Some(p3))

  test("an Exile who accepts becomes a Citizen: their board empties and each " +
      "site they rule keeps one Imperial warband"):
    val done = replied(replied(offered(throneStarted(exilesHoldSites), p2, p3),
      p2, OfferCitizenship.accept), p3, OfferCitizenship.decline)
    val after = ready(done)
    assertEquals(Imperials.role(after, p2), Some(Role.Citizen))
    assertEquals(Imperials.role(after, p3), Some(Role.Exile))
    assertEquals(Look(after).player(p2).board.warbands, 0)
    assertEquals(Look(after).forces(ancientCity),
      SiteForces.Occupied(ForceKind.Imperial, 1))
    assertEquals(Look(after).forces(brokenPeaks),
      SiteForces.Occupied(ForceKind.Exile(Look(after).player(p3).lineage), 1))
    assertEquals(lines(done.events), Vector("p1 offered Citizenship to p2, p3",
      "p2 accepted Citizenship", "p3 declined Citizenship"))

  test("a short purple bank is topped up: the step assumes unlimited " +
      "warbands (Chronicle ruling 22)"):
    val short = exilesHoldSites.boardWarbands(p1, 24)
    val done = replied(offered(throneStarted(short), p3), p3,
      OfferCitizenship.accept)
    val after = ready(done)
    assertEquals(Look(after).forces(brokenPeaks),
      SiteForces.Occupied(ForceKind.Imperial, 1))
    assertEquals(after.banks.warbandSupply.get(ForceKind.Imperial), Some(25))

  test("with no seated Exile nothing is asked"):
    val imperialTable = ChronicleWalk.throneHeldByChancellorP1.update(state =>
      state.updateCampaign(campaign => campaign.copy(lineages =
        campaign.lineages.map((id, lineage) => id -> (if lineage.role ==
          Role.Exile then lineage.copy(role = Role.Citizen) else lineage)))))
    assert(!ChronicleWalk.parked(throneStarted(imperialTable).state)
      .exists(_.decisionId == OfferCitizenship.offerDecisionId))

  test("a former Chancellor who accepts turns the former Imperial sites " +
      "Imperial again (Chronicle ruling 20)"):
    // Crown Chancellor left p2, the former Chancellor, one warband at each
    // of the old Empire's sites.
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(ancientCity, p2, 1).warbandsAt(brokenPeaks, p2, 1)
    val done = replied(offered(throneStarted(table), p2), p2,
      OfferCitizenship.accept)
    val after = ready(done)
    Vector(ancientCity, brokenPeaks).foreach(site => assertEquals(
      Look(after).forces(site), SiteForces.Occupied(ForceKind.Imperial, 1)))

  test("with one seated Exile the holder may offer them or no one"):
    val one = exilesHoldSites.update(state => state.updateCampaign(campaign =>
      campaign.copy(lineages = campaign.lineages.updatedWith(
        Look(state).player(p3).lineage)(_.map(_.copy(role = Role.Citizen))))))
    ChronicleWalk.parked(throneStarted(one).state).get.query match
      case many: DecisionQuery.ChooseMany =>
        assertEquals((many.min, many.max, many.options.map(_.ref)),
          (0, 1, Vector(DecisionOptionRef.Player(p2))))
      case other => fail(other.toString)

  test("two Exiles who accept both become Citizens, and the walk replays"):
    val table = exilesHoldSites
    val done = replied(replied(offered(throneStarted(table), p2, p3), p2,
      OfferCitizenship.accept), p3, OfferCitizenship.accept)
    val after = ready(done)
    assertEquals(Vector(p2, p3).map(Imperials.role(after, _)),
      Vector(Some(Role.Citizen), Some(Role.Citizen)))
    Vector(ancientCity, brokenPeaks).foreach(site => assertEquals(
      Look(after).forces(site), SiteForces.Occupied(ForceKind.Imperial, 1)))
    val replayed = done.events.foldLeft[Either[OathViolation, OathState]](
      Right(table.state))((state, event) => state.flatMap(rules.evolve(_,
      event)))
    assertEquals(replayed.map(Outcomes.ready), Right(after))

  test("an Exile who rules no site becomes a Citizen with nothing to swap"):
    val done = replied(offered(throneStarted(
      ChronicleWalk.throneHeldByChancellorP1.boardWarbands(p3, 2)), p3), p3,
      OfferCitizenship.accept)
    val after = ready(done)
    assertEquals(Imperials.role(after, p3), Some(Role.Citizen))
    assertEquals(Look(after).player(p3).board.warbands, 0)
