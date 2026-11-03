package oathdigital.gameplay.chronicle

import oathdigital.catalog.holding.GrandMaskCard
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.powers.action.BrassHorseCard
import oathdigital.gameplay.powers.economy.VowOfWanderingCard
import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.operations.TheGrandScepterCard
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerCompleted
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2, p3}
import oathdigital.testkit.Unwrap.accepted

/** The Sun task (CR p. 36): reveal held relics, crown the winner, assign
  * the tasks. Every game here is p1's win, already in the Chronicle phase
  * with Start Chronicle answered. */
class ChronicleSunSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val ancientCity = AncientCitySite.id
  private val boxed = LineageId("lineage:boxed")

  /** The Sun started on `table`: the driver's step after Start Chronicle. */
  private def sunStarted(table: Table)(using munit.Location): OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleBegin))

  private def answerPlayer(transition: OathTransition, owner: PlayerId,
      decisionId: String, player: PlayerId)(using munit.Location)
      : OathTransition =
    answer(transition, owner, decisionId,
      ChooseOneAnswer(DecisionOptionRef.Player(player)))

  private def answer(transition: OathTransition, owner: PlayerId,
      decisionId: String, answer: DecisionAnswer)(using munit.Location)
      : OathTransition =
    val next = accepted(rules.resolveWalker(transition.state, owner,
      decisionId, answer))
    OathTransition(next.state, transition.events ++ next.events)

  /** The Sun with the unheld banners' tasks given: World to p3, Beacon to
    * p2. */
  private def sunWithTasksGiven(table: Table)(using munit.Location)
      : OathTransition =
    val world = answerPlayer(sunStarted(table), p1,
      ChronicleTaskAssignment.worldDecisionId, p3)
    answerPlayer(world, p1, ChronicleTaskAssignment.beaconDecisionId, p2)

  private def ready(transition: OathTransition): ReadyGame =
    Outcomes.ready(transition.state)

  /** The state the moment the Sun completes on `table`, World given to p3
    * and Beacon to p2: the Throne runs on after it, so this replays the
    * walk's events up to the Sun's `WalkerCompleted`. */
  private def sunEnded(table: Table)(using munit.Location): ReadyGame =
    val events = sunWithTasksGiven(table).events
    val sunEvents = events.take(events.indexOf(
      WalkerCompleted(TriggeredProcedureRef.ChronicleSun)) + 1)
    assert(sunEvents.nonEmpty, events)
    Outcomes.ready(accepted(sunEvents.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event)))))

  private def lineageOf(ready: ReadyGame, player: PlayerId): LineageId =
    ready.game.current.players.find(_.player == player).get.lineage

  private def roleOf(ready: ReadyGame, player: PlayerId): Role =
    ready.game.campaign.lineages(lineageOf(ready, player)).role

  private def relicsOf(ready: ReadyGame, player: PlayerId): Vector[RelicState] =
    ready.game.current.players.find(_.player == player).get.relics

  /** The table with `lineage` in the baseline only, nobody seated, as
    * `role`. */
  private def withBoxedLineage(table: Table, role: Role): Table =
    table.update(_.updateCampaign(campaign => campaign.copy(baseline =
      campaign.baseline.copy(lineages = Vector(LineageState(boxed, None, role,
        Vector.empty, Vector.empty))))))

  test("facedown relics held by players are revealed; relics at sites stay " +
      "facedown"):
    val after = sunEnded(ChronicleWalk.chronicleWonByP1
      .relic(p2, BrassHorseCard, facedown = true)
      .relicAt(GrandMaskCard, at = AncientCitySite))
    assertEquals(relicsOf(after, p2).map(_.orientation),
      Vector(Orientation.FaceUp))
    assertEquals(after.game.current.map.sites(ancientCity).relics
      .map(_.orientation), Vector(Orientation.FaceDown))

  test("an Exile winner becomes Chancellor and one Imperial warband " +
      "replaces theirs at each site they rule"):
    val after = sunEnded(ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 2))
    assertEquals(roleOf(after, p1), Role.Chancellor)
    assertEquals(after.game.current.map.sites(ancientCity).forces,
      SiteForces.Occupied(ForceKind.Imperial, 1))
    assertEquals(after.game.current.players.find(_.player == p1).get
      .board.warbands, 0)
    assertEquals(after.banks.warbandSupply.get(ForceKind.Imperial),
      Some(after.game.current.map.inPlay.size))

  test("a broken Empire leaves each of its sites one warband of the former " +
      "Chancellor's colour (Chronicle ruling 20)"):
    val peaks = BrokenPeaksSite.id
    val after = sunEnded(ChronicleWalk.chronicleWonByP1.imperial(p2)
      .warbandsAt(AncientCitySite, p2, 3).warbandsAt(peaks, p2, 1))
    assertEquals(roleOf(after, p2), Role.Exile)
    val colour = ForceKind.Exile(lineageOf(after, p2))
    Vector(ancientCity, peaks).foreach(site => assertEquals(
      after.game.current.map.sites(site).forces,
      SiteForces.Occupied(colour, 1), site.value))

  test("a former Chancellor's bank, short by their board, is topped up so " +
      "each of the Empire's sites takes one warband"):
    // 13 purple board warbands turn to p2's colour, leaving 1 in the bank.
    val peaks = BrokenPeaksSite.id
    val after = sunEnded(ChronicleWalk.chronicleWonByP1.imperial(p2)
      .boardWarbands(p2, 13).warbandsAt(AncientCitySite, p2, 1)
      .warbandsAt(peaks, p2, 1))
    val colour = ForceKind.Exile(lineageOf(after, p2))
    Vector(ancientCity, peaks).foreach(site => assertEquals(
      after.game.current.map.sites(site).forces,
      SiteForces.Occupied(colour, 1), site.value))
    assertEquals(after.banks.warbandSupply.get(colour),
      Some(MaterialBankState.ExileWarbandsPerLineage + 1))

  test("a boxed former Chancellor leaves the Empire's sites empty"):
    val table = withBoxedLineage(ChronicleWalk.chronicleWonByP1,
      Role.Chancellor).update(state => state.copy(banks = state.banks.copy(
        warbandSupply = state.banks.warbandSupply.updated(
          ForceKind.Imperial, 24)))).warbandsAt(AncientCitySite, p3, 1)
      .update(state => state.updateCurrent(current => current.copy(map =
        current.map.copy(sites = current.map.sites.updatedWith(ancientCity)(
          _.map(_.copy(forces = SiteForces.Occupied(ForceKind.Imperial, 2))))))))
    val after = sunEnded(table)
    assertEquals(after.game.current.map.sites(ancientCity).forces,
      SiteForces.Empty)

  test("a former Chancellor holding Vow of Wandering still takes the sites"):
    val after = sunEnded(ChronicleWalk.chronicleWonByP1.imperial(p2)
      .adviser(p2, VowOfWanderingCard).warbandsAt(AncientCitySite, p2, 2))
    assertEquals(after.game.current.map.sites(ancientCity).forces,
      SiteForces.Occupied(ForceKind.Exile(lineageOf(after, p2)), 1))

  test("the winner takes the Grand Scepter from the box, faceup"):
    val after = sunEnded(ChronicleWalk.chronicleWonByP1)
    assertEquals(relicsOf(after, p1).last, RelicState(TheGrandScepterCard.id,
      Orientation.FaceUp, Tokens.empty))

  test("the Grand Scepter moves from the player who holds it"):
    val after = sunEnded(ChronicleWalk.chronicleWonByP1
      .relic(p2, TheGrandScepterCard.id))
    assertEquals(relicsOf(after, p1).map(_.id), Vector(TheGrandScepterCard.id))
    assertEquals(relicsOf(after, p2), Vector.empty)

  test("a Citizen lineage in the box becomes an Exile when the Empire breaks"):
    val after = sunEnded(
      withBoxedLineage(ChronicleWalk.chronicleWonByP1, Role.Citizen))
    assertEquals(after.game.campaign.boxRoles, Map(boxed -> Role.Exile))

  test("the Reliquary's relics are discarded when the Empire breaks"):
    val relic = BrassHorseCard.id
    val after = sunEnded(ChronicleWalk.chronicleWonByP1
      .update(_.updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(relicDeck =
          current.commonCards.relicDeck.filterNot(_ == relic)))))
      .update(_.updateCampaign(_.copy(reliquary = Vector(relic)))))
    assertEquals(after.game.campaign.reliquary, Vector.empty)
    assertEquals(after.game.current.setAsideRelics, Vector(relic))

  test("a Citizen winner starts a new dynasty"):
    // A Citizen's board warbands are Imperial, so the table needs the
    // Imperial supply a Citizen game would have.
    val table = withBoxedLineage(ChronicleWalk.chronicleWonByP1,
      Role.Chancellor).update(state => state.updateCampaign(campaign =>
        campaign.copy(lineages = campaign.lineages.updatedWith(
          lineageOf(state, p1))(_.map(_.copy(role = Role.Citizen)))))
      .copy(banks = state.banks.copy(warbandSupply =
        state.banks.warbandSupply.updated(ForceKind.Imperial, 24))))
    val after = sunEnded(table)
    assertEquals(roleOf(after, p1), Role.Chancellor)
    assertEquals(after.game.campaign.boxRoles, Map(boxed -> Role.Citizen))
    assertEquals(after.game.current.players.find(_.player == p1).get
      .board.warbands, 3)

  test("the winner takes the Throne; unheld banners park the winner's " +
      "World, then Beacon, choice"):
    val started = sunStarted(ChronicleWalk.chronicleWonByP1)
    val world = ChronicleWalk.parked(started.state).get
    assertEquals((world.decisionId, world.owner),
      (ChronicleTaskAssignment.worldDecisionId, p1))
    val worldGiven = answerPlayer(started, p1,
      ChronicleTaskAssignment.worldDecisionId, p3)
    assertEquals(ChronicleWalk.parked(worldGiven.state).map(_.decisionId),
      Some(ChronicleTaskAssignment.beaconDecisionId))
    val done = answerPlayer(worldGiven, p1,
      ChronicleTaskAssignment.beaconDecisionId, p2)
    assertEquals(ready(done).game.current.chronicleTasks, Map(
      ChronicleTask.Throne -> p1, ChronicleTask.World -> p3,
      ChronicleTask.Beacon -> p2))

  test("banner holders take their tasks without a choice"):
    val done = sunStarted(ChronicleWalk.chronicleWonByP1
      .peoplesFavor(Some(p2), 1).darkestSecret(Some(p3), 1))
    assert(!ChronicleWalk.parked(done.state).exists(
      _.decisionId.startsWith("chronicle.assign.")), done.state)
    assertEquals(ready(done).game.current.chronicleTasks, Map(
      ChronicleTask.Throne -> p1, ChronicleTask.World -> p2,
      ChronicleTask.Beacon -> p3))

  test("one holder of World and Beacon gives one of them away"):
    val started = sunStarted(ChronicleWalk.chronicleWonByP1
      .peoplesFavor(Some(p2), 1).darkestSecret(Some(p2), 1))
    val give = ChronicleWalk.parked(started.state).get
    assertEquals((give.decisionId, give.owner),
      (ChronicleTaskAssignment.giveTaskDecisionId, p2))
    val chosen = answer(started, p2, ChronicleTaskAssignment.giveTaskDecisionId,
      ChooseOneAnswer(ChronicleTaskAssignment.giveBeacon))
    val recipient = ChronicleWalk.parked(chosen.state).get
    assertEquals(recipient.decisionId,
      ChronicleTaskAssignment.giveRecipientDecisionId)
    assertEquals(recipient.query match
      case one: DecisionQuery.ChooseOne => one.options.map(_.ref)
      case other => fail(other.toString)
    , Vector[DecisionOptionRef](DecisionOptionRef.Player(p1),
      DecisionOptionRef.Player(p3)))
    val done = answerPlayer(chosen, p2,
      ChronicleTaskAssignment.giveRecipientDecisionId, p3)
    assertEquals(ready(done).game.current.chronicleTasks, Map(
      ChronicleTask.Throne -> p1, ChronicleTask.World -> p2,
      ChronicleTask.Beacon -> p3))

  test("the Sun writes who was crowned and who took each task"):
    val done = sunWithTasksGiven(ChronicleWalk.chronicleWonByP1)
    val id = oathdigital.gameplay.actions.ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleSun)
    assertEquals(NoteText.said(id, ChronicleSunProcedure.noteKeys,
      done.events).map(_.text), Vector("The Empire broke",
      s"${p1.value} became Chancellor", s"${p1.value} took the Throne task",
      s"${p3.value} took the World task", s"${p2.value} took the Beacon task"))

  test("replaying the Sun's events reproduces its state"):
    val start = ChronicleWalk.chronicleWonByP1.state
    val done = sunWithTasksGiven(ChronicleWalk.chronicleWonByP1)
    assertEquals(accepted(done.events.foldLeft[
      Either[OathViolation, OathState]](Right(start))((state, event) =>
        state.flatMap(rules.evolve(_, event)))), done.state)
