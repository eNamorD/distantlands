package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.testkit.{Outcomes, RecordedOps, Table}
import distantlands.testkit.Table.p3
import distantlands.testkit.Unwrap.{accepted, rejected}

/** Shape Foundation, the Beacon's third step (CR p. 39). p3 holds the
  * Beacon. */
class ShapeFoundationSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** Tasks assigned, and the World's Separate Visions run: no Vision is
    * left in play, so the Chronicle can be recorded at the end. */
  private val tasksAssigned: Table = Table.from(
    ChronicleWalk.throneHeldByChancellorP1.ready.updateCurrent(current =>
      current.copy(commonCards = current.commonCards.copy(worldDeck =
        current.commonCards.worldDeck.filterNot(_.isInstanceOf[VisionId])))))

  /** `tasksAssigned` with Foundations I and III on their altered faces. */
  private val wideHorizonsAndPublicAmbitions: Table = tasksAssigned
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .foundation(FoundationNumber.III, FoundationFace.Altered)

  private val needle = LegacyId("L05")
  private val ironHand = LegacyId("L01")

  /** `tasksAssigned` with a lineage in the box holding The Needle active
    * and Iron Hand inactive; Foundation I is altered by The Needle and
    * Foundation VI by Iron Hand. */
  private val alteredByBoxedLegacies: Table = tasksAssigned.update(
    _.updateCampaign(campaign => campaign.copy(
      baseline = campaign.baseline.copy(lineages = Vector(LineageState(
        LineageId("lineage:boxed"), None, Role.Exile,
        Vector(LegacyState(needle, active = true),
          LegacyState(ironHand, active = false)), Vector.empty))),
      foundations = campaign.foundations
        .updated(FoundationNumber.I,
          FoundationState(FoundationFace.Altered, Set(needle)))
        .updated(FoundationNumber.VI,
          FoundationState(FoundationFace.Altered, Set(ironHand))))))

  /** The walk from the end of the World, every Beacon question before
    * Shape Foundation answered plainly: parked on Shape Foundation, or on
    * nothing when it asks nothing. */
  private def walkedToShapeFoundation(table: Table)(using munit.Location)
      : OathTransition =
    ChronicleWalk.beaconWalkedTo(rules, table, ShapeFoundation.decisionId)

  /** `table`'s Shape Foundation answered `answer`, then the rest of the
    * Chronicle answered plainly to its record. */
  private def answeredThenRecorded(table: Table, answer: DecisionOptionRef)(
      using munit.Location): OathTransition =
    val at = walkedToShapeFoundation(table)
    val next = accepted(rules.resolveWalker(at.state, p3,
      ShapeFoundation.decisionId, ChooseOneAnswer(answer)))
    ChronicleWalk.answeringEverything(rules,
      OathTransition(next.state, at.events ++ next.events))

  private def options(transition: OathTransition)(using munit.Location)
      : Vector[DecisionOption] =
    ChronicleWalk.parked(transition.state).get.query match
      case one: DecisionQuery.ChooseOne => one.options
      case other => fail(other.toString)

  private def labelOf(option: DecisionOption): String = option match
    case DecisionOption.Button(_, label) => label
    case DecisionOption.Unavailable(inner, _) => labelOf(inner)
    case other => fail(other.toString)

  /** The "restored" lines the walk journaled, as plain text. */
  private def restoredLines(transition: OathTransition): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon),
      ChronicleBeaconProcedure.noteKeys, transition.events)
      .filter(_.key.startsWith("restored-foundation-")).map(_.text)

  /** The last Chronicle `transition` journaled with `ChronicleUpdated`. */
  private def recordedChronicle(transition: OathTransition): Chronicle =
    RecordedOps.all(transition.events).collect {
      case ChronicleUpdated(chronicle) => chronicle
    }.last

  test("with no foundation altered the holder is not asked"):
    val walked = walkedToShapeFoundation(tasksAssigned)
    assertEquals(ChronicleWalk.parked(walked.state), None)
    assert(!RecordedOps.all(walked.events)
      .exists(_.isInstanceOf[FoundationRestored]), walked.events)

  test("the holder may keep every face or restore any altered one, III " +
      "included, and Keep comes first"):
    val walked = walkedToShapeFoundation(wideHorizonsAndPublicAmbitions)
    val parked = ChronicleWalk.parked(walked.state).get
    assertEquals(parked.owner, p3)
    assertEquals(options(walked).map(_.ref), Vector(ShapeFoundation.keep,
      ShapeFoundation.restore(FoundationNumber.I),
      ShapeFoundation.restore(FoundationNumber.III)))
    assertEquals(options(walked).map(labelOf), Vector(
      "Keep every foundation as it lies",
      "Restore Foundation I to Imperial Maps",
      "Restore Foundation III to Quiet Ambitions"))
    assert(options(walked).forall(_.available))

  test("restoring Wide Horizons turns Foundation I to Imperial Maps, says " +
      "so, and the journaled Chronicle carries it"):
    val done = answeredThenRecorded(wideHorizonsAndPublicAmbitions,
      ShapeFoundation.restore(FoundationNumber.I))
    assertEquals(RecordedOps.all(done.events)
      .collect { case op: FoundationRestored => op },
      Vector(FoundationRestored(FoundationNumber.I)))
    val normal = FoundationState(FoundationFace.Normal, Set.empty)
    val altered = FoundationState(FoundationFace.Altered, Set.empty)
    assertEquals(Outcomes.ready(done.state).game.campaign
      .foundations(FoundationNumber.I), normal)
    assertEquals(restoredLines(done),
      Vector("p3 restored Foundation I to Imperial Maps"))
    val record = recordedChronicle(done)
    assertEquals(record.foundations.get(FoundationNumber.I), Some(normal))
    assertEquals(record.foundations.get(FoundationNumber.III), Some(altered))

  test("keeping every foundation restores none and writes no line"):
    val done = answeredThenRecorded(wideHorizonsAndPublicAmbitions,
      ShapeFoundation.keep)
    assert(!RecordedOps.all(done.events)
      .exists(_.isInstanceOf[FoundationRestored]), done.events)
    assertEquals(restoredLines(done), Vector.empty)
    assertEquals(recordedChronicle(done).foundations,
      wideHorizonsAndPublicAmbitions.ready.game.campaign.foundations)

  test("a face an active legacy alters is shown unavailable with the " +
      "legacy's name, and cannot be restored; an inactive legacy's face can"):
    val walked = walkedToShapeFoundation(alteredByBoxedLegacies)
    assertEquals(options(walked), Vector(
      DecisionOption.Button(ShapeFoundation.keep,
        "Keep every foundation as it lies"),
      DecisionOption.Unavailable(DecisionOption.Button(
        ShapeFoundation.restore(FoundationNumber.I),
        "Restore Foundation I to Imperial Maps"),
        OptionReason.Written("An active legacy alters it: The Needle")),
      DecisionOption.Button(ShapeFoundation.restore(FoundationNumber.VI),
        "Restore Foundation VI to Wandering Flame")))
    rejected(rules.resolveWalker(walked.state, p3, ShapeFoundation.decisionId,
      ChooseOneAnswer(ShapeFoundation.restore(FoundationNumber.I))))
    val restored = accepted(rules.resolveWalker(walked.state, p3,
      ShapeFoundation.decisionId,
      ChooseOneAnswer(ShapeFoundation.restore(FoundationNumber.VI))))
    assertEquals(Outcomes.ready(restored.state).game.campaign
      .foundations(FoundationNumber.VI),
      FoundationState(FoundationFace.Normal, Set.empty))

  test("with every altered face held by an active legacy the holder is " +
      "still asked, and only Keep may be chosen"):
    val needleOnly = alteredByBoxedLegacies
      .foundation(FoundationNumber.VI, FoundationFace.Normal)
    val walked = walkedToShapeFoundation(needleOnly)
    assertEquals(ChronicleWalk.parked(walked.state).map(_.decisionId),
      Some(ShapeFoundation.decisionId))
    assertEquals(options(walked).map(option => option.ref -> option.available),
      Vector(ShapeFoundation.keep -> true,
        ShapeFoundation.restore(FoundationNumber.I) -> false))
    val done = answeredThenRecorded(needleOnly, ShapeFoundation.keep)
    assertEquals(recordedChronicle(done).foundations(FoundationNumber.I),
      FoundationState(FoundationFace.Altered, Set(needle)))

  test("a restore replays from its journal to the same game"):
    val done = answeredThenRecorded(wideHorizonsAndPublicAmbitions,
      ShapeFoundation.restore(FoundationNumber.III))
    assertEquals(accepted(done.events.foldLeft[
        Either[OathViolation, OathState]](
      Right(wideHorizonsAndPublicAmbitions.state))((state, event) =>
        state.flatMap(rules.evolve(_, event)))), done.state)
