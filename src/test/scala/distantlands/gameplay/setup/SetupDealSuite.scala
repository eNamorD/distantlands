package distantlands.gameplay.setup

import distantlands.model._

/** Foundation IV's deal of the starting denizen stack, on both faces
  * (Foundations design, slice 2). */
class SetupDealSuite extends munit.FunSuite:
  private val seats = FirstGameSetupFixture.participants
  private val Vector(p1, p2, p3) = seats.map(_.playerId)
  private val stack = FirstGameSetupFixture.denizens

  /** The fixture's Chronicle with lineage l`n` storing `count` starting
    * advisers, taken from the end of its world deck, for each pair given,
    * and Foundation IV on `face`. */
  private def chronicleStoringAdvisers(face: FoundationFace,
      stored: (Int, Int)*): Chronicle =
    val base = FirstGameSetupFixture.chronicle
    val (deck, lineages) = stored.foldLeft(
        (base.worldDeck, Vector.empty[LineageState])) {
      case ((deck, lineages), (lineage, count)) =>
        (deck.dropRight(count), lineages :+ LineageState(
          LineageId(s"l$lineage"), None, Role.Exile, Vector.empty,
          deck.takeRight(count).map(DenizenState(_, Orientation.FaceDown,
            Tokens.empty))))
    }
    base.copy(worldDeck = deck, lineages = lineages, foundations =
      Map(FoundationNumber.IV -> FoundationState(face, Set.empty)))

  test("Ties of Fellowship seeds 2 denizens in each discard pile, deals a " +
      "hand of 3 to each player with no stored adviser, and leaves the rest " +
      "for the world deck"):
    val deal = SetupDeal.of(
      chronicleStoringAdvisers(FoundationFace.Normal, 2 -> 1), seats)
    assertEquals(deal, SetupDeal(FoundationFace.Normal,
      Vector(p1 -> 3, p2 -> 0, p3 -> 3)))
    assertEquals(deal.needed, 12)
    assertEquals(deal.discards(stack), Map[Region, Vector[WorldCardId]](
      Region.Cradle -> stack.slice(0, 2),
      Region.Provinces -> stack.slice(2, 4),
      Region.Hinterland -> stack.slice(4, 6)))
    assertEquals(seats.map(seat => deal.hand(stack, seat.playerId)),
      Vector(stack.slice(6, 9), Vector.empty, stack.slice(9, 12)))
    assertEquals(seats.map(seat => deal.kept(stack, seat.playerId)),
      Vector.fill(3)(Vector.empty))
    assertEquals(deal.deck(stack), stack.drop(12))

  test("a first game's deal is Ties of Fellowship with every player drawing"):
    assertEquals(SetupDeal.firstGame(seats),
      SetupDeal.of(FirstGameSetupFixture.chronicle, seats))
    assertEquals(SetupDeal.firstGame(seats).needed, 15)

  test("under Teeming World each player draws until they hold three " +
      "starting advisers, and keeps every card drawn"):
    // l2 stores one adviser and l3 stores three.
    val deal = SetupDeal.of(
      chronicleStoringAdvisers(FoundationFace.Altered, 2 -> 1, 3 -> 3), seats)
    assertEquals(deal, SetupDeal(FoundationFace.Altered,
      Vector(p1 -> 3, p2 -> 2, p3 -> 0)))
    assertEquals(deal.needed, 5)
    assertEquals(seats.map(seat => deal.kept(stack, seat.playerId)),
      Vector(stack.slice(0, 3), stack.slice(3, 5), Vector.empty))
    assertEquals(seats.map(seat => deal.hand(stack, seat.playerId)),
      Vector.fill(3)(Vector.empty))

  test("under Teeming World a lineage storing more than three advisers " +
      "draws none"):
    val deal = SetupDeal.of(
      chronicleStoringAdvisers(FoundationFace.Altered, 1 -> 4), seats)
    assertEquals(deal.draws, Vector(p1 -> 0, p2 -> 3, p3 -> 3))

  test("Teeming World deals the rest of the stack to the Cradle, the " +
      "Provinces and the Hinterland in turn, the last card dealt on top, " +
      "and leaves the world deck no denizen"):
    val deal = SetupDeal(FoundationFace.Altered,
      Vector(p1 -> 3, p2 -> 3, p3 -> 3))
    // 9 drawn of 19 leaves 10: the Cradle takes the odd card.
    val nineteen = stack.take(19)
    assertEquals(deal.discards(nineteen), Map[Region, Vector[WorldCardId]](
      Region.Cradle -> Vector(9, 12, 15, 18).map(nineteen),
      Region.Provinces -> Vector(10, 13, 16).map(nineteen),
      Region.Hinterland -> Vector(11, 14, 17).map(nineteen)))
    assertEquals(deal.deck(nineteen), Vector.empty)

  test("a player the deal does not seat draws nothing"):
    val deal = SetupDeal.firstGame(seats.take(2))
    assertEquals(deal.hand(stack, p3), Vector.empty)
    assertEquals(SetupDeal(FoundationFace.Altered, Vector(p1 -> 3))
      .kept(stack, p3), Vector.empty)
