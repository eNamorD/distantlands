package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{ActingTroupeCard, OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.economy.{BirdsongCard, GolemLegionsCard}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p2, p3}
import distantlands.testkit.Unwrap.accepted

/** Oaths of Loyalty (L25) in the Chronicle: "When you would gain a starting
  * adviser, bury it, except for— CHRONICLE (WORLD): In Gather Denizens,
  * keep your faceup advisers. They will be your facedown starting advisers
  * in the next game." (rulings 15 and 16; R23, R24). p1 holds the Throne,
  * p2 the World and p3 the Beacon. */
class OathsOfLoyaltySuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val five = Vector(BirdsongCard, GolemLegionsCard, ActingTroupeCard,
    FaeMerchantCard, ThreateningRoarCard)

  /** p2 holds Oaths of Loyalty and the five advisers, faceup. */
  private def fiveAdvisers: Table =
    five.foldLeft(ChronicleWalk.throneHeldByChancellorP1)(_.adviser(p2, _))
      .legacy(p2, OathsOfLoyaltyCard)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, procedure, answers,
      after = TriggeredProcedureRef.ChronicleSun)

  private def keeping(player: PlayerId, cards: distantlands.catalog.Denizen*)
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == OathsOfLoyalty.keepDecisionId(player) =>
      DecisionAnswer.ChooseManyAnswer(cards.toVector.map(card =>
        DecisionOptionRef.Denizen(card.id)))

  private def lineage(ready: ReadyGame, player: PlayerId): LineageState =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)

  private def lines(run: ChronicleWalk.Run, procedure: TriggeredProcedureRef)
      : Vector[String] =
    NoteText.said(ProcedureNotes.id(procedure),
      OathsOfLoyalty.noteKeys(procedure), run.events).map(_.text)

  test("the holder keeps three of five faceup advisers as facedown " +
      "starting advisers, and the rest are gathered (ruling 15; R23)"):
    val run = through(fiveAdvisers, TriggeredProcedureRef.ChronicleWorld,
      keeping(p2, BirdsongCard, GolemLegionsCard, ActingTroupeCard))
    val asked = run.asked.find(_.decisionId ==
      OathsOfLoyalty.keepDecisionId(p2)).get
    val query = asked.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((asked.owner, query.min, query.max, query.options.size),
      (p2, 0, 3, 5))
    val kept = lineage(run.ready, p2).startingAdvisers
    assert(Vector(BirdsongCard, GolemLegionsCard, ActingTroupeCard)
      .forall(card => kept.exists(_.id == card.id)), kept)
    assert(kept.forall {
      case DenizenState(_, orientation, tokens) =>
        orientation == Orientation.FaceDown && tokens.isEmpty
      case _ => false })
    assert(!kept.exists(_.id == FaeMerchantCard.id))
    assert(lines(run, TriggeredProcedureRef.ChronicleWorld).contains(
      // NoteText joins a card list with commas; the Game Log words it.
      s"p2 kept ${BirdsongCard.id.value}, ${GolemLegionsCard.id.value}, " +
        s"${ActingTroupeCard.id.value} with ${OathsOfLoyaltyCard.id.value}"))

  test("a kept adviser's tokens return to their banks first"):
    val suit = catalog.denizen(BirdsongCard.id).get.suit
    val table = fiveAdvisers.tokens(BirdsongCard, favor = 2)
      .bankFavor(suit, 0)
    val run = through(table, TriggeredProcedureRef.ChronicleWorld,
      keeping(p2, BirdsongCard))
    assertEquals(run.ready.banks.favor(suit), 2)

  test("facedown advisers are not offered, and none faceup asks nothing"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .adviser(p2, BirdsongCard, facedown = true)
      .legacy(p2, OathsOfLoyaltyCard)
    val run = through(table, TriggeredProcedureRef.ChronicleWorld)
    assert(!run.asked.exists(_.decisionId ==
      OathsOfLoyalty.keepDecisionId(p2)))

  test("the holder's Protect and Dispossess keep is buried at the world " +
      "deck bottom (ruling 16; R24)"):
    // p2 keeps no adviser with Oaths, then keeps the plainest card in
    // Protect and Dispossess: it is buried, so p2's lineage stores none.
    val run = through(fiveAdvisers, TriggeredProcedureRef.ChronicleWorld)
    assertEquals(lineage(run.ready, p2).startingAdvisers, Vector.empty)
    val buried = lines(run, TriggeredProcedureRef.ChronicleWorld)
      .filter(_.startsWith("p2 buried "))
    assertEquals(buried.size, 1)
    assert(buried.head.endsWith(s" with ${OathsOfLoyaltyCard.id.value}"))
    val card = buried.head.stripPrefix("p2 buried ")
      .stripSuffix(s" with ${OathsOfLoyaltyCard.id.value}")
    // The protected stack's three cards all went to the bottom.
    assert(run.ready.game.current.commonCards.worldDeck.takeRight(3)
      .map(_.value).contains(card))

  test("the holder's Welcome a Stranger keep is buried (ruling 16)"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .legacy(p3, OathsOfLoyaltyCard)
    val run = through(table, TriggeredProcedureRef.ChronicleBeacon)
    assertEquals(lineage(run.ready, p3).startingAdvisers, Vector.empty)
    assertEquals(lines(run, TriggeredProcedureRef.ChronicleBeacon)
      .count(_.startsWith("p3 buried ")), 1)

  test("a dormant Oaths keeps nothing and buries nothing (ruling 1)"):
    val table = five.foldLeft(ChronicleWalk.throneHeldByChancellorP1)(
      _.adviser(p2, _)).legacy(p2, OathsOfLoyaltyCard, active = false)
    val run = through(table, TriggeredProcedureRef.ChronicleWorld)
    assert(!run.asked.exists(_.decisionId ==
      OathsOfLoyalty.keepDecisionId(p2)))
    assertEquals(lineage(run.ready, p2).startingAdvisers.size, 1)

  test("a walk resumed at the keep question finds the same five advisers"):
    val started = accepted(rules.next(OathTransition(fiveAdvisers.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    val parked = ChronicleWalk.walkedTo(rules, started,
      OathsOfLoyalty.keepDecisionId(p2))
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.decisionId, OathsOfLoyalty.keepDecisionId(p2))
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(fiveAdvisers.state))(
      (state, event) => state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
