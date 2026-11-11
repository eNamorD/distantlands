package distantlands.gameplay.setup

import distantlands.model._
import distantlands.testkit.Unwrap.accepted

/** Game start under the two altered faces that only change setup, Movers
  * and Shakers (II) and Teeming World (IV) (Foundations design, slice 2).
  * Every test starts from a Chronicle: game start is the subject. */
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

  /** The fixture's first-game Chronicle: no lineage listed and no Empire. */
  private val firstGameChronicle = FirstGameSetupFixture.chronicle

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
    val onMap = suitsOnMap(ready)
    assert(onMap.size > 2, "the Empire's denizens are on the map")
    assertEquals(ready.banks.favor,
      Suit.all.map(suit => suit -> onMap.count(_ == suit)).toMap)

  test("with five players Powerful Tribes adds 1 favor to each bank, and " +
      "Movers and Shakers adds none"):
    val fiveSeats = LaterGameFixture.seats(5)
    val tribes = started(firstGameChronicle, fiveSeats)
    val movers = started(withAlteredFoundation(firstGameChronicle,
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
