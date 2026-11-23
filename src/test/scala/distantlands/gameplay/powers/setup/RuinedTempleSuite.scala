package distantlands.gameplay.powers.setup

import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite,
  DeepWoodsSite}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.powers.action.TempleOfAncestorsCard
import distantlands.gameplay.setup.{FirstGameSetupFixture, PawnWindowOffers,
  SetupProcedure, SetupWalkDriver}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{WalkerCompleted, WalkerDice,
  WalkerPowers}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{Answers, PhasePowerHarness, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Ruined Temple (E10 ruined), SETUP / WHEN EXPLORED: "If you place your
  * pawn here, you may burn a secret. If you do and then win the game,
  * score double the legacy points and take rule of all bandit sites."
  * The pledge (catalog batch 13, ruling 17); `RuinedTempleWinSuite` holds
  * the win. */
class RuinedTempleSuite extends munit.FunSuite:
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)

  /** Ruined Temple at Ancient City in p1's game. */
  private def templeAtAncientCity: Table =
    Table.start.edifice(TempleOfAncestorsCard.id, EdificeSide.Ruined,
      at = AncientCitySite)

  /** Ruined Temple's Setup steps for p1, who placed their pawn at Ancient
    * City, walked with `answers`. */
  private def walkedAtSetup(table: Table,
      answers: Answers = PartialFunction.empty): SetupWalkDriver.SetupWalked =
    val ready = table.ready
    SetupWalkDriver.driveWithEvents(ready, Sequence(PawnWindowOffers.steps(
      catalog, RuinedTemple, ready, p1, AncientCitySite.id)),
      TriggeredProcedureRef.Setup, powers, answers = answers)

  private val setupPledge = RuinedTemple.pledgeDecisionId(Some(p1))

  private val burning: Answers =
    case park if park.decisionId.startsWith("setup.ruined-temple.pledge.") ||
        park.decisionId == RuinedTemple.pledgeDecisionId(None) =>
      ChooseOneAnswer(RuinedTemple.burn)

  private def boardOf(ready: ReadyGame, player: PlayerId): PlayerBoardState =
    ready.game.current.player(player).get.board

  private def lines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(RuinedTemple.id, RuinedTemple.noteKeys, events).map(_.text)

  test("the placer may keep their secrets or burn one, Keep first"):
    val run = walkedAtSetup(templeAtAncientCity.secrets(p1, faceUp = 1))
    val asked = run.asked.filter(_.decisionId == setupPledge)
    assertEquals(asked.map(_.owner), Vector(p1))
    asked.head.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref),
          Vector(RuinedTemple.keep, RuinedTemple.burn))
      case other => fail(s"expected a choose-one, got $other")

  test("keeping writes nothing and pledges nothing"):
    val run = walkedAtSetup(templeAtAncientCity.secrets(p1, faceUp = 1))
    assertEquals(run.ready.game.current.templePledges, Set.empty[PlayerId])
    assertEquals(boardOf(run.ready, p1).faceUpSecrets, 1)
    assertEquals(lines(run.events), Vector.empty)

  test("burning takes a facedown secret first, pledges, and says so"):
    val run = walkedAtSetup(templeAtAncientCity
      .secrets(p1, faceUp = 1, faceDown = 1), burning)
    assertEquals(run.ready.game.current.templePledges, Set(p1))
    assertEquals((boardOf(run.ready, p1).faceUpSecrets,
      boardOf(run.ready, p1).faceDownSecrets), (1, 0))
    assertEquals(lines(run.events), Vector("p1 burned a secret."))

  test("with only faceup secrets, burning takes a faceup one"):
    val run = walkedAtSetup(templeAtAncientCity.secrets(p1, faceUp = 2),
      burning)
    assertEquals(boardOf(run.ready, p1).faceUpSecrets, 1)

  test("a player with no secret is offered nothing"):
    val ready = templeAtAncientCity.secrets(p1, faceUp = 0).ready
    assertEquals(PawnWindowOffers.offer(catalog, RuinedTemple, ready, p1,
      AncientCitySite.id), None)

  test("a player who has pledged is still offered it, so the offer stands " +
      "after the burn"):
    val ready = templeAtAncientCity.secrets(p1, faceUp = 0)
      .pledgedAtRuinedTemple(p1).ready
    assert(PawnWindowOffers.offer(catalog, RuinedTemple, ready, p1,
      AncientCitySite.id).isDefined)

  test("every player who places their pawn at the Temple in Setup may " +
      "pledge"):
    // The generic driver places every pawn at Ancient City, the first site.
    val fresh = Table.from(FirstGameSetupFixture.freshReady)
      .edifice(TempleOfAncestorsCard.id, EdificeSide.Ruined,
        at = AncientCitySite)
    val seated = fresh.ready.game.current.players.map(_.player)
    val staged = seated.foldLeft(fresh)(_.secrets(_, faceUp = 1)).ready
    val tree = accepted(SetupProcedure.build(catalog, staged,
      staged.game.current.turn.activePlayer, Vector.empty))
    val walked = SetupWalkDriver.driveWithEvents(staged, tree,
      TriggeredProcedureRef.Setup, powers, answers = burning)
    assertEquals(walked.ready.game.current.templePledges, seated.toSet)
    // Pawns are placed from the first player, not in seat order.
    assertEquals(walked.asked.map(_.decisionId).filter(
      _.startsWith("setup.ruined-temple.pledge.")).toSet,
      seated.map(player => RuinedTemple.pledgeDecisionId(Some(player))).toSet)

  // ---- When explored ----

  /** Every defense die two shields, so an Explore succeeds on its first
    * roll, and every shuffle reverses its pile (as `LostDoorsSuite`). */
  private val shieldsAndReversedShuffles: WalkerDice = new WalkerDice:
    def roll(kind: DiceKind, count: Int)
        : Either[OathViolation, Vector[DieFace]] =
      Right(Vector.fill(count)(DefenseDieFace.TwoShields))
    override def shuffle(count: Int): Either[OathViolation, Vector[Int]] =
      Right((0 until count).reverse.toVector)

  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = shieldsAndReversedShuffles)

  /** p1 explores into Deep Woods, where the Temple of Ancestors is stored,
    * and the explored site's edifice comes ruined. */
  private def exploring(table: Table): Table = table
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
    .storedInAtlas(DeepWoodsSite, TempleOfAncestorsCard.id)

  test("when explored, the explorer may burn a secret under the Explore's " +
      "fixed id"):
    val rolled = accepted(rules.startWalker(Ready(
      exploring(Table.start.secrets(p1, faceUp = 1)).ready),
      ActionRef.Explore, p1))
    val placed = accepted(rules.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    // The walk parks at the pledge: resolving it under this id is the
    // proof, since the walker refuses an answer to any other question.
    val done = accepted(rules.resolveWalker(placed.state, p1,
      RuinedTemple.pledgeDecisionId(None), ChooseOneAnswer(RuinedTemple.burn)))
    assert(done.events.contains(WalkerCompleted(ActionRef.Explore)),
      done.events)
    val ready = PhasePowerHarness.after(done)
    assertEquals(ready.game.current.templePledges, Set(p1))
    assertEquals(lines(rolled.events ++ placed.events ++ done.events),
      Vector("p1 burned a secret."))

  test("when explored, an explorer with no secret is asked nothing"):
    val rolled = accepted(rules.startWalker(Ready(
      exploring(Table.start.secrets(p1, faceUp = 0)).ready),
      ActionRef.Explore, p1))
    val placed = accepted(rules.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    assert(placed.events.contains(WalkerCompleted(ActionRef.Explore)),
      placed.events)
