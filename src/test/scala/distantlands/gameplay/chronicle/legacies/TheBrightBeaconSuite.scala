package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.{ChronicleWalk, IntroduceNewcomers,
  WelcomeStranger}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p2, p3}
import distantlands.testkit.Unwrap.accepted

/** The Bright Beacon (L24): "If you're resolving the Beacon, you may take
  * any six cards from the Dispossessed (instead of choosing suits and
  * taking from their matching dividers)." (ruling 17; R25). p3 holds the
  * Beacon task and The Bright Beacon; five older Dispossessed and three
  * newly Dispossessed denizens wait. */
class TheBrightBeaconSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** Tasks assigned, no Vision left in the world deck (as
    * `BeaconTaskSuite`'s table), then `older` Dispossessed and `newly`
    * newly Dispossessed taken from its denizens. */
  private def beaconTable(older: Int, newly: Int): Table =
    val base = Table.from(ChronicleWalk.throneHeldByChancellorP1.ready
      .updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(worldDeck = current.commonCards.worldDeck
          .filterNot(_.isInstanceOf[VisionId])))))
    val denizens = base.ready.game.current.commonCards.worldDeck
      .collect { case id: DenizenId => id }
    val (old, fresh) = denizens.take(older + newly).splitAt(older)
    base.dispossessed(old*).update(_.updateCurrent(current => current.copy(
      commonCards = current.commonCards.copy(worldDeck =
        current.commonCards.worldDeck.filterNot(fresh.contains)),
      newlyDispossessed = fresh))).legacy(p3, TheBrightBeaconCard)

  private def all(table: Table): Vector[DenizenId] =
    (table.ready.game.campaign.dispossessed ++
      table.ready.game.current.newlyDispossessed)
      .collect { case id: DenizenId => id }

  private def beacon(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleBeacon,
      answers, after = TriggeredProcedureRef.ChronicleWorld)

  private def fromDispossessed(cards: Vector[DenizenId])
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == TheBrightBeacon.sourceId(p3) =>
      DecisionAnswer.ChooseOneAnswer(TheBrightBeacon.dispossessed)
    case decide if decide.decisionId == TheBrightBeacon.cardsId(p3) =>
      DecisionAnswer.ChooseManyAnswer(cards.map(DecisionOptionRef.Denizen(_)))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageState =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)

  test("the holder first chooses the source, the box first (ruling 17)"):
    val run = beacon(beaconTable(5, 3))
    val source = run.asked.head
    assertEquals((source.decisionId, source.owner),
      (TheBrightBeacon.sourceId(p3), p3))
    assertEquals(source.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref), Vector(TheBrightBeacon.box, TheBrightBeacon.dispossessed))

  test("the box runs the ordinary step"):
    val run = beacon(beaconTable(5, 3))
    assert(run.asked.exists(_.decisionId ==
      IntroduceNewcomers.suitDecisionId(3)))
    assert(!run.asked.exists(_.decisionId == TheBrightBeacon.cardsId(p3)))

  test("from the Dispossessed the holder chooses exactly six of all of " +
      "them, the newly Dispossessed included, and Welcome a Stranger runs " +
      "on those six (R25)"):
    val table = beaconTable(5, 3)
    val six = all(table).drop(2)
    val run = beacon(table, fromDispossessed(six))
    val asked = run.asked.find(_.decisionId == TheBrightBeacon.cardsId(p3)).get
    val query = asked.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((query.min, query.max), (6, 6))
    assertEquals(query.options.map(_.ref),
      all(table).map(DecisionOptionRef.Denizen(_)))
    assert(!run.asked.exists(_.decisionId ==
      IntroduceNewcomers.suitDecisionId(3)))
    val welcome = run.asked.find(_.decisionId ==
      WelcomeStranger.keepDecisionId).get
    assertEquals(welcome.query.asInstanceOf[DecisionQuery.Partition].options
      .map(_.ref).toSet, six.map(DecisionOptionRef.Denizen(_)).toSet)
    assertEquals(lineage(run.ready, p3).startingAdvisers.size, 1)
    assert(six.contains(lineage(run.ready, p3).startingAdvisers.head.id))
    val lines = NoteText.said(ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleBeacon),
      LegacyChroniclePowers.beaconNoteKeys, run.events).map(_.text)
    assert(lines.contains(s"p3 took six newcomers from the Dispossessed " +
      s"with ${TheBrightBeaconCard.id.value}"), lines)

  test("exactly six Dispossessed are taken without a question (R25)"):
    val table = beaconTable(3, 3)
    val run = beacon(table, fromDispossessed(Vector.empty))
    assert(!run.asked.exists(_.decisionId == TheBrightBeacon.cardsId(p3)))
    val welcome = run.asked.find(_.decisionId ==
      WelcomeStranger.keepDecisionId).get
    assertEquals(welcome.query.asInstanceOf[DecisionQuery.Partition].options
      .map(_.ref).toSet, all(table).map(DecisionOptionRef.Denizen(_)).toSet)

  test("a holder not resolving the Beacon is not asked (ruling 17)"):
    val table = beaconTable(5, 3).update(_.updateCampaign(campaign =>
      campaign.copy(lineages = campaign.lineages.view.mapValues(lineage =>
        lineage.copy(legacies = lineage.legacies.filterNot(
          _.id == TheBrightBeaconCard.id))).toMap)))
      .legacy(p2, TheBrightBeaconCard)
    val run = beacon(table)
    assert(!run.asked.exists(_.decisionId.startsWith(
      "chronicle.beacon.legacy.the-bright-beacon.")))

  test("a walk resumed at the six-card question finds the same cards"):
    val table = beaconTable(5, 3)
    val started = accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleWorld))
    val atSource = ChronicleWalk.walkedTo(rules, started,
      TheBrightBeacon.sourceId(p3))
    val next = accepted(rules.resolveWalker(atSource.state, p3,
      TheBrightBeacon.sourceId(p3), DecisionAnswer.ChooseOneAnswer(
        TheBrightBeacon.dispossessed)))
    val events = atSource.events ++ next.events
    val decide = ChronicleWalk.parked(next.state).get
    val replayed = accepted(events.foldLeft[Either[OathViolation, OathState]](
      Right(table.state))((state, event) => state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
