package oathdigital.gameplay.chronicle

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.ProcedureNotes
import oathdigital.gameplay.powers.{NoteText, WalkerPowerCatalog}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.DecisionAnswer.ChooseOneAnswer
import oathdigital.testkit.{Outcomes, Table}
import oathdigital.testkit.Table.p3
import oathdigital.testkit.Unwrap.accepted

/** The Beacon task (CR p. 39). p3 holds the Beacon. */
class BeaconTaskSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** Tasks assigned, and the World's Separate Visions run: no Vision is
    * left in play, so the Chronicle can be recorded at the end. */
  private val tasksAssigned: Table = Table.from(
    ChronicleWalk.throneHeldByChancellorP1.ready.updateCurrent(current =>
      current.copy(commonCards = current.commonCards.copy(worldDeck =
        current.commonCards.worldDeck.filterNot(_.isInstanceOf[VisionId])))))

  /** The walk from the end of the World: the driver starts the Beacon. */
  private def beaconStarted(table: Table)(using munit.Location)
      : OathTransition =
    accepted(rules.next(OathTransition(table.state, Vector.empty),
      TriggeredProcedureRef.ChronicleWorld))

  /** The Beacon with `suits` chosen for three, two and one draws. */
  private def suitsChosen(table: Table, suits: Vector[Suit])(
      using munit.Location): OathTransition =
    suits.zip(IntroduceNewcomers.draws).foldLeft(beaconStarted(table)) {
      case (step, (suit, count)) =>
        val id = IntroduceNewcomers.suitDecisionId(count)
        val at = ChronicleWalk.walkedTo(rules, step, id)
        val next = accepted(rules.resolveWalker(at.state, p3, id,
          ChooseOneAnswer(DecisionOptionRef.FavorBank(suit))))
        OathTransition(next.state, at.events ++ next.events)
    }

  private def hand(transition: OathTransition): Vector[WorldCardId] =
    Outcomes.ready(transition.state).game.current.temporaryHands
      .getOrElse(p3, Vector.empty)

  /** The "returned to the box" lines the walk journaled, as plain text. */
  private def returnedLines(transition: OathTransition): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleBeacon),
      ChronicleBeaconProcedure.noteKeys, transition.events)
      .filter(_.key == IntroduceNewcomers.returned.name).map(_.text)

  private def suitOf(card: WorldCardId): Suit =
    catalog.suitOf(card.asInstanceOf[DenizenId]).get

  /** Denizens of `suit` that `ready` has nowhere in play. */
  private def boxed(ready: ReadyGame, suit: Suit): Vector[DenizenId] =
    val inPlay = ready.game.cardIndex.toOption.get.ids
    catalog.denizens.filter(_.suit == suit)
      .map(denizen => DenizenId(denizen.id.value))
      .filterNot(inPlay.contains)

  test("each suit choice offers only suits not chosen before it"):
    val first = ChronicleWalk.walkedTo(rules, beaconStarted(tasksAssigned),
      IntroduceNewcomers.suitDecisionId(3))
    val picked = accepted(rules.resolveWalker(first.state, p3,
      IntroduceNewcomers.suitDecisionId(3),
      ChooseOneAnswer(DecisionOptionRef.FavorBank(Suit.Beast))))
    val second = ChronicleWalk.parked(picked.state).get
    assertEquals(second.decisionId, IntroduceNewcomers.suitDecisionId(2))
    second.query match
      case one: DecisionQuery.ChooseOne => assertEquals(one.options.map(_.ref),
        Suit.all.filterNot(_ == Suit.Beast).map(DecisionOptionRef.FavorBank(_)))
      case other => fail(other.toString)
    val twice = accepted(rules.resolveWalker(picked.state, p3,
      IntroduceNewcomers.suitDecisionId(2),
      ChooseOneAnswer(DecisionOptionRef.FavorBank(Suit.Arcane))))
    ChronicleWalk.parked(twice.state).get.query match
      case one: DecisionQuery.ChooseOne => assertEquals(one.options.map(_.ref),
        Suit.all.filterNot(Set(Suit.Beast, Suit.Arcane))
          .map(DecisionOptionRef.FavorBank(_)))
      case other => fail(other.toString)

  test("the holder draws three, two and one boxed denizens of the chosen " +
      "suits"):
    val before = tasksAssigned.ready
    val drawn = hand(suitsChosen(tasksAssigned,
      Vector(Suit.Beast, Suit.Arcane, Suit.Order)))
    assertEquals(drawn.groupMapReduce(suitOf)(_ => 1)(_ + _),
      Map(Suit.Beast -> 3, Suit.Arcane -> 2, Suit.Order -> 1))
    assert(drawn.forall(card => Suit.all.exists(suit =>
      boxed(before, suit).contains(card))))

  test("a short suit first takes back the older Dispossessed, never the " +
      "newly Dispossessed"):
    val newly = tasksAssigned.ready.game.current.commonCards.worldDeck
      .collect { case id: DenizenId => id }.take(6)
    // Two boxed Nomads are the older Dispossessed and the rest are put in
    // the world deck, so the Nomad box is empty and the draw of three needs
    // the Dispossessed back.
    val table = tasksAssigned.update(ready =>
      val nomads = boxed(ready, Suit.Nomad)
      ready.updateCurrent(current => current.copy(
        commonCards = current.commonCards.copy(worldDeck =
          current.commonCards.worldDeck.filterNot(newly.contains) ++
            nomads.drop(2)),
        newlyDispossessed = newly))
        .updateCampaign(_.copy(dispossessed = nomads.take(2))))
    val older = table.ready.game.campaign.dispossessed
    val drawn = suitsChosen(table, Vector(Suit.Nomad, Suit.Arcane, Suit.Order))
    val after = Outcomes.ready(drawn.state)
    assertEquals(after.game.campaign.dispossessed, Vector.empty)
    assertEquals(after.game.current.newlyDispossessed, newly)
    assertEquals(hand(drawn).filter(suitOf(_) == Suit.Nomad).toSet,
      older.toSet)
    assertEquals(returnedLines(drawn), Vector(
      s"${older.map(_.value).mkString(", ")} returned to the box"))

  test("no line says the Dispossessed returned when no suit is short"):
    val drawn = suitsChosen(tasksAssigned,
      Vector(Suit.Beast, Suit.Arcane, Suit.Order))
    assertEquals(returnedLines(drawn), Vector.empty)

  test("a suit still short after the return draws what remains " +
      "(Chronicle ruling 15)"):
    val table = tasksAssigned.update(ready =>
      val nomads = boxed(ready, Suit.Nomad)
      ready.updateCurrent(current => current.copy(commonCards =
        current.commonCards.copy(worldDeck =
          current.commonCards.worldDeck ++ nomads.drop(1)))))
    val drawn = hand(suitsChosen(table,
      Vector(Suit.Nomad, Suit.Arcane, Suit.Order)))
    assertEquals(drawn.groupMapReduce(suitOf)(_ => 1)(_ + _),
      Map(Suit.Nomad -> 1, Suit.Arcane -> 2, Suit.Order -> 1))

  test("the holder keeps one newcomer facedown; five go to the world deck"):
    val drawn = suitsChosen(tasksAssigned,
      Vector(Suit.Beast, Suit.Arcane, Suit.Order))
    val after = Outcomes.ready(
      ChronicleWalk.answeringEverything(rules, drawn).state)
    val lineage = after.game.current.player(p3).get.lineage
    assertEquals(after.game.campaign.lineages(lineage).startingAdvisers
      .map(_.id), Vector(hand(drawn).head))
    assertEquals(after.game.current.commonCards.worldDeck.takeRight(5),
      hand(drawn).tail)
