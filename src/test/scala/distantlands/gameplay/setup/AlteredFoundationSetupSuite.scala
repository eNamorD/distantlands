package distantlands.gameplay.setup

import distantlands.catalog.SiteOnly
import distantlands.gameplay.{OathRules, SearchPlay}
import distantlands.model._
import distantlands.testkit.{Answers, Outcomes, Situation}
import distantlands.testkit.Unwrap.{accepted, as, rejected}

/** Game start under the altered faces that change setup: Movers and
  * Shakers (II) and Teeming World (IV) (Foundations design, slice 2), the
  * face Grand Council (V) turns the People's Favor to (slice 5), and the
  * face Festival (VI) turns the Darkest Secret to (slice 6). Every test
  * starts from a Chronicle: game start is the subject. */
class AlteredFoundationSetupSuite extends munit.FunSuite:
  private val catalog = FirstGameSetupFixture.catalog
  private val p1 = PlayerId("p1")

  /** `chronicle` with foundation `number` turned to its altered face. */
  private def withAlteredFoundation(chronicle: Chronicle,
      number: FoundationNumber): Chronicle =
    chronicle.copy(foundations = chronicle.foundations.updated(number,
      FoundationState(FoundationFace.Altered, Set.empty)))

  /** A later game's Chronicle for three seats: l1 is the Chancellor and l2
    * a Citizen, and each of the Empire's two sites stores a denizen. */
  private val chronicleWithEmpire =
    LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))

  /** The fixture's brand-new Chronicle: no lineage listed and no Empire.
    * An altered face is history, so with one it starts an exile-only game
    * (owner, 2026-11-12). */
  private val brandNewChronicle = FirstGameSetupFixture.chronicle

  /** The game `chronicle` starts for `seats`, p1 first, dealt from the
    * Chronicle's decks in their own order. */
  private def started(chronicle: Chronicle,
      seats: Vector[FirstGameParticipant] = FirstGameSetupFixture.participants)
      : ReadyGame =
    accepted(GameStartRules.evolve(catalog, chronicle,
      LaterGameFixture.orders(chronicle, p1, seats)))

  /** The suit of every denizen on the map, edifices included. */
  private def suitsOnMap(ready: ReadyGame): Vector[Suit] =
    ready.game.current.map.sites.values.flatMap(_.denizens).flatMap {
      case EdificeState(id, _, _) => catalog.edifice(id).map(_.suit)
      case DenizenState(id, _, _) => catalog.denizen(id).map(_.suit)
    }.toVector

  test("Movers and Shakers gives every board 6 favor, and each bank 1 " +
      "favor per denizen of its suit on the map and no more"):
    val ready = started(
      withAlteredFoundation(chronicleWithEmpire, FoundationNumber.II))
    assertEquals(ready.game.current.players.map(_.board),
      Vector.fill(3)(PlayerBoardState(6, 1, 0, 3, SupplyTrack.full)))
    // Game start places the Empire's sites; the sites Imperial Maps adds
    // in Setup's walk add their own (ImperialMapsSetupSuite).
    val onMap = suitsOnMap(ready)
    assert(onMap.nonEmpty, "the Empire's denizens are on the map")
    assertEquals(ready.banks.favor,
      Suit.all.map(suit => suit -> onMap.count(_ == suit)).toMap)

  test("with five players Powerful Tribes adds 1 favor to each bank, and " +
      "Movers and Shakers adds none"):
    val fiveSeats = LaterGameFixture.seats(5)
    val tribes = started(brandNewChronicle, fiveSeats)
    val movers = started(withAlteredFoundation(brandNewChronicle,
      FoundationNumber.II), fiveSeats)
    val onMap = suitsOnMap(movers)
    assert(onMap.nonEmpty, "the Homeland edifices are on the map")
    assertEquals(tribes.banks.favor,
      Suit.all.map(suit => suit -> (4 + onMap.count(_ == suit))).toMap)
    assertEquals(movers.banks.favor,
      Suit.all.map(suit => suit -> onMap.count(_ == suit)).toMap)
    assertEquals(tribes.game.current.players.map(_.board.favor),
      Vector.fill(5)(1))
    assertEquals(movers.game.current.players.map(_.board.favor),
      Vector.fill(5)(6))

  test("Movers and Shakers changes nothing else a game starts with"):
    val tribes = started(chronicleWithEmpire)
    val movers = started(
      withAlteredFoundation(chronicleWithEmpire, FoundationNumber.II))
    assertEquals(movers.game.current.commonCards,
      tribes.game.current.commonCards)
    assertEquals(movers.game.current.map, tribes.game.current.map)
    assertEquals(movers.game.current.temporaryHands,
      tribes.game.current.temporaryHands)
    assertEquals(movers.game.current.banners, tribes.game.current.banners)

  test("Grand Council flips the People's Favor to its Grand Council face " +
      "with 1 favor, and gives it to the Chancellor when the Oathkeeper " +
      "goal is the People's Favor, as Mob's Favor does"):
    val people = chronicleWithEmpire.copy(
      oathkeeperGoal = OathkeeperGoal.ThePeople)
    val council = started(withAlteredFoundation(people, FoundationNumber.V))
    assertEquals(council.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, Some(p1), 1))
    assertEquals(council.game.campaign.foundations(FoundationNumber.V),
      FoundationState(FoundationFace.Altered, Set.empty))
    assertEquals(started(people).game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.Mob, Some(p1), 1))

  test("under another Oathkeeper goal, or with no Chancellor, the Grand " +
      "Council starts in the shared bank, and nothing else changes"):
    val supremacy = chronicleWithEmpire.copy(
      oathkeeperGoal = OathkeeperGoal.Supremacy)
    val mob = started(supremacy)
    val council = started(withAlteredFoundation(supremacy, FoundationNumber.V))
    assertEquals(council.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, None, 1))
    assertEquals(council.game.current.copy(banners = mob.game.current.banners),
      mob.game.current)
    assertEquals(council.banks, mob.banks)
    val exileOnly = started(withAlteredFoundation(brandNewChronicle.copy(
      oathkeeperGoal = OathkeeperGoal.ThePeople), FoundationNumber.V))
    assertEquals(exileOnly.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, None, 1))

  test("Festival flips the Darkest Secret to its Festival face with 1 " +
      "secret and its marker beside the map, and gives it to the " +
      "Chancellor when the Oathkeeper goal is the Darkest Secret"):
    val devotion = chronicleWithEmpire.copy(
      oathkeeperGoal = OathkeeperGoal.Devotion)
    val festival = started(withAlteredFoundation(devotion, FoundationNumber.VI))
    assertEquals(festival.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, Some(p1), 1,
        festival = None))
    assertEquals(festival.game.campaign.foundations(FoundationNumber.VI),
      FoundationState(FoundationFace.Altered, Set.empty))
    assertEquals(started(devotion).game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.WanderingFlame, Some(p1), 1,
        festival = None))

  test("under another Oathkeeper goal, or with no Chancellor, the Festival " +
      "starts in the shared bank, and nothing else changes"):
    val supremacy = chronicleWithEmpire.copy(
      oathkeeperGoal = OathkeeperGoal.Supremacy)
    val flame = started(supremacy)
    val festival = started(withAlteredFoundation(supremacy,
      FoundationNumber.VI))
    assertEquals(festival.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, None, 1,
        festival = None))
    assertEquals(festival.game.current.copy(banners =
      flame.game.current.banners), flame.game.current)
    assertEquals(festival.banks, flame.banks)
    val exileOnly = started(withAlteredFoundation(brandNewChronicle.copy(
      oathkeeperGoal = OathkeeperGoal.Devotion), FoundationNumber.VI))
    assertEquals(exileOnly.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, None, 1,
        festival = None))

  private val (p2, p3) = (PlayerId("p2"), PlayerId("p3"))

  /** The brand-new Chronicle with Foundation IV on Teeming World: an
    * exile-only game. */
  private val teemingWorldChronicle =
    withAlteredFoundation(brandNewChronicle, FoundationNumber.IV)

  private def faceDown(denizens: Vector[DenizenId]): Vector[AdviserState] =
    denizens.map(DenizenState(_, Orientation.FaceDown, Tokens.empty))

  private def advisers(ready: ReadyGame, player: PlayerId)
      : Vector[AdviserState] = ready.game.current.player(player).get.advisers

  test("under Teeming World each player with no stored adviser starts with " +
      "three facedown advisers from the top of the stack, and no hand"):
    val ready = started(teemingWorldChronicle)
    val stack = brandNewChronicle.worldDeck
    assertEquals(Vector(p1, p2, p3).map(advisers(ready, _)), Vector(
      faceDown(stack.slice(0, 3)), faceDown(stack.slice(3, 6)),
      faceDown(stack.slice(6, 9))))
    assertEquals(ready.game.current.temporaryHands.values.flatten.toVector,
      Vector.empty)

  /** The adviser l2 stores in `teemingWorldChronicleWithStoredAdviser`:
    * the last card of `chronicleWithEmpire`'s world deck. */
  private val storedAdviser = DenizenState(
    chronicleWithEmpire.worldDeck.last, Orientation.FaceUp, Tokens.empty)

  /** `chronicleWithEmpire` with Foundation IV on Teeming World and l2
    * storing `storedAdviser`, which leaves the world deck. */
  private val teemingWorldChronicleWithStoredAdviser = withAlteredFoundation(
    chronicleWithEmpire.copy(worldDeck = chronicleWithEmpire.worldDeck.init,
      lineages = chronicleWithEmpire.lineages.map(lineage =>
        if lineage.id == LineageId("l2") then
          lineage.copy(startingAdvisers = Vector(storedAdviser))
        else lineage)), FoundationNumber.IV)

  test("under Teeming World a lineage with one stored adviser draws two " +
      "and holds all three facedown, the stored one first"):
    val ready = started(teemingWorldChronicleWithStoredAdviser)
    val stack = teemingWorldChronicleWithStoredAdviser.worldDeck
    assertEquals(advisers(ready, p1), faceDown(stack.slice(0, 3)))
    // A facedown adviser's place cannot change, and the stored one was
    // taken before Foundation IV dealt the other two (ruling 8).
    assertEquals(advisers(ready, p2),
      faceDown(storedAdviser.id +: stack.slice(3, 5)))
    assertEquals(advisers(ready, p3), faceDown(stack.slice(5, 8)))
    assertEquals(ready.game.campaign.lineages(LineageId("l2"))
      .startingAdvisers, Vector.empty)

  /** The advisers l2 and l3 store in
    * `teemingWorldChronicleStoringTwoAndThree`: the last five cards of
    * `chronicleWithEmpire`'s world deck, two and then three. */
  private val (twoStored, threeStored) =
    chronicleWithEmpire.worldDeck.takeRight(5).splitAt(2)

  /** `chronicleWithEmpire` with Foundation IV on Teeming World, l2 storing
    * `twoStored` and a new Exile lineage l3 storing `threeStored`, all five
    * out of the world deck. */
  private val teemingWorldChronicleStoringTwoAndThree =
    def storing(lineage: LineageState, advisers: Vector[DenizenId]) =
      lineage.copy(startingAdvisers = advisers.map(
        DenizenState(_, Orientation.FaceUp, Tokens.empty)))
    withAlteredFoundation(chronicleWithEmpire.copy(
      worldDeck = chronicleWithEmpire.worldDeck.dropRight(5),
      lineages = chronicleWithEmpire.lineages.map(lineage =>
        if lineage.id == LineageId("l2") then storing(lineage, twoStored)
        else lineage) :+ storing(LineageState(LineageId("l3"), None,
          Role.Exile, Vector.empty, Vector.empty), threeStored)),
      FoundationNumber.IV)

  test("under Teeming World a lineage with two stored advisers draws one " +
      "and one with three draws none, each holding three facedown, the " +
      "stored ones first"):
    val ready = started(teemingWorldChronicleStoringTwoAndThree)
    val stack = teemingWorldChronicleStoringTwoAndThree.worldDeck
    assertEquals(advisers(ready, p1), faceDown(stack.slice(0, 3)))
    assertEquals(advisers(ready, p2), faceDown(twoStored :+ stack(3)))
    assertEquals(advisers(ready, p3), faceDown(threeStored))
    // The four drawn leave the rest of the stack to the discard piles.
    assertEquals(Region.all.flatMap(
      ready.game.current.commonCards.discard).size, stack.size - 4)
    assertEquals(Vector("l2", "l3").flatMap(lineage => ready.game.campaign
      .lineages(LineageId(lineage)).startingAdvisers), Vector.empty)

  test("a Teeming World game's recorded events replay to the same state"):
    val rules = new OathRules(catalog)
    val transition = accepted(rules.beginGame(OathState.NoGame,
      teemingWorldChronicleWithStoredAdviser,
      LaterGameFixture.orders(teemingWorldChronicleWithStoredAdviser, p1)))
    val replayed = transition.events
      .foldLeft[Either[OathViolation, OathState]](Right(OathState.NoGame)):
        case (Right(state), event) => rules.evolve(state, event)
        case (left, _) => left
    assertEquals(replayed, Right(transition.state))

  test("Teeming World deals the rest of the stack to the Cradle, the " +
      "Provinces and the Hinterland discard piles in turn, with no pile " +
      "seeded"):
    val ready = started(teemingWorldChronicle)
    val rest = brandNewChronicle.worldDeck.drop(9)
    /** Every third card of the rest from `from`, the first dealt last: a
      * pile is stored top last, and its first card dealt is its top. */
    def everyThird(from: Int): Vector[WorldCardId] =
      rest.indices.filter(_ % 3 == from).map(rest).toVector.reverse
    assertEquals(rest.size, 51)
    assertEquals(ready.game.current.commonCards.regionalDiscards, Map(
      Region.Cradle -> everyThird(0), Region.Provinces -> everyThird(1),
      Region.Hinterland -> everyThird(2)))
    // The first three cards dealt top the piles, and the last three lie at
    // their bottoms.
    assertEquals(Region.all.map(region =>
      ready.game.current.commonCards.discard(region).last), rest.take(3))
    assertEquals(Region.all.map(region =>
      ready.game.current.commonCards.discard(region).head), rest.takeRight(3))

  test("a Teeming World stack that does not divide by three leaves the " +
      "Cradle pile the odd card"):
    // 57 denizens less the 8 drawn leave 49.
    val ready = started(teemingWorldChronicleWithStoredAdviser)
    assertEquals(Region.all.map(region =>
      ready.game.current.commonCards.discard(region).size), Vector(17, 16, 16))

  test("under Teeming World the world deck is the five Visions alone"):
    val ready = started(teemingWorldChronicle)
    assertEquals(ready.game.current.commonCards.worldDeck,
      FirstGameRulesData.visions: Vector[WorldCardId])

  test("a Teeming World game holds every card once"):
    Vector(teemingWorldChronicle, teemingWorldChronicleWithStoredAdviser)
        .foreach { chronicle =>
      val ready = started(chronicle)
      assert(ready.game.cardIndex.isRight, ready.game.cardIndex)
      // Every denizen of the stack and every stored adviser is an adviser
      // or in a discard pile.
      val stored = chronicle.lineages.flatMap(_.startingAdvisers).map(_.id)
      val held = ready.game.current.players.flatMap(_.advisers.map(_.id)) ++
        Region.all.flatMap(ready.game.current.commonCards.discard)
      assertEquals(held.map(_.value).sorted,
        (chronicle.worldDeck ++ stored).map(_.value).sorted)
    }

  test("Teeming World refuses a stack too short for every player's draws"):
    val short = teemingWorldChronicle.copy(
      worldDeck = brandNewChronicle.worldDeck.take(8))
    assertEquals(rejected(GameStartRules.evolve(catalog, short,
        LaterGameFixture.orders(short, p1))),
      OathViolation.UnsupportedChronicle("not enough world-deck cards to deal"))

  test("Movers and Shakers and Teeming World together each set up their " +
      "own part"):
    val ready = started(withAlteredFoundation(
      teemingWorldChronicleWithStoredAdviser, FoundationNumber.II))
    assertEquals(ready.game.current.players.map(_.board.favor),
      Vector(6, 6, 6))
    val onMap = suitsOnMap(ready)
    assertEquals(ready.banks.favor,
      Suit.all.map(suit => suit -> onMap.count(_ == suit)).toMap)
    assertEquals(ready.game.current.players.map(_.advisers.size),
      Vector(3, 3, 3))
    assertEquals(ready.game.current.commonCards.worldDeck,
      FirstGameRulesData.visions: Vector[WorldCardId])

  /** Every decision id the walk parked on, in order, while it answers each
    * by default. */
  private def decisionsAskedDuringSetup(chronicle: Chronicle): Vector[String] =
    var asked = Vector.empty[String]
    val recording: Answers =
      case park if { asked :+= park.decisionId; false } => ???
    Situation.wake(Situation.rules(catalog).withAnswers(recording), chronicle,
      LaterGameFixture.orders(chronicle, p1))
    asked

  test("under Teeming World Setup asks every player to keep a legacy in " +
      "Foundation III, then each for a pawn site and a reveal, and no one " +
      "to choose an adviser"):
    // Teeming World is history, so this is not a first game: the keeps
    // run in Foundation III (catalog batch 13, ruling 3).
    val asked = decisionsAskedDuringSetup(teemingWorldChronicle)
    assertEquals(asked, Vector(p1, p2, p3).map(
      LegacyKeep.decisionId) ++ Vector(p1, p2, p3).flatMap(player =>
      Vector(SetupProcedure.pawnDecisionId(player),
        SetupProcedure.revealDecisionId(player))))

  test("under Teeming World Reveal Cards offers a player's three facedown " +
      "advisers, and Setup ends with each player holding three"):
    var offered = Vector.empty[Int]
    val countingOffersAndRevealingNothing: Answers =
      case park if park.decisionId ==
          SetupProcedure.revealDecisionId(park.awaiting) =>
        offered :+= as[DecisionQuery.ChooseMany](park.decide.query)
          .options.size
        DecisionAnswer.ChooseManyAnswer(Vector.empty)
    val woken = Situation.wake(Situation.rules(catalog)
      .withAnswers(countingOffersAndRevealingNothing), teemingWorldChronicle,
      LaterGameFixture.orders(teemingWorldChronicle, p1))
    val siteOnly = Vector(p1, p2, p3).map(player =>
      advisers(woken.ready, player).count {
        case DenizenState(id, _, _) =>
          catalog.denizen(id).exists(_.isInstanceOf[SiteOnly])
        case _ => false
      })
    assertEquals(offered, siteOnly.map(3 - _))
    assertEquals(woken.ready.game.current.players.map(_.advisers.size),
      Vector(3, 3, 3))
    assertNotEquals(woken.ready.game.current.turn.phase, Phase.Setup)

  test("the first Search of a Teeming World game draws a Vision from the " +
      "world deck and stops"):
    val woken = Situation.wake(Situation.rules(catalog), teemingWorldChronicle,
      LaterGameFixture.orders(teemingWorldChronicle, p1)).endingWake(p1)
    val searched = Outcomes.ready(accepted(SearchPlay.searched(
      new OathRules(catalog), woken.state, p1, SearchSource.WorldDeck)).state)
    assertEquals(searched.game.current.temporaryHands(p1),
      Vector[WorldCardId](FirstGameRulesData.visions.head))
    assertEquals(searched.game.current.tracks.visionsDrawn, 1)
    assertEquals(searched.game.current.commonCards.worldDeck,
      FirstGameRulesData.visions.tail: Vector[WorldCardId])
