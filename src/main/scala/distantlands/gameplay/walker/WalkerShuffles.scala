package distantlands.gameplay.walker

import distantlands.model.{CardId, OathViolation, ReadyGame, Shuffle,
  ShufflePile, VisionId}

/** Fills a declared `Shuffle`'s order from the walker's random source. Split
  * out of [[ProcedureWalker]], which is close to the project's per-file
  * bound, as [[WalkerRolls]] is. The filled operation is what the walker runs
  * and records, so replay applies the same order without asking again. A pile
  * of fewer than two cards keeps its order and asks nothing.
  */
private[walker] object WalkerShuffles:
  def ordered(shuffle: Shuffle, state: ReadyGame, dice: WalkerDice)
      : Either[OathViolation, Shuffle] =
    val cards = ShufflePile.cards(shuffle.pile, state.game)
    if shuffle.pile == ShufflePile.VisionPackets then
      packets(cards, dice).map(order => shuffle.copy(order = Some(order)))
    else if cards.size < 2 then Right(shuffle.copy(order = Some(cards)))
    else dice.shuffle(cards.size).flatMap(order => Either.cond(
      order.sorted == cards.indices.toVector,
      shuffle.copy(order = Some(order.map(cards))),
      OathViolation.InvalidEventOrder(
        s"shuffle source gave $order for a pile of ${cards.size} cards")))

  /** The card's layout for `deck` (R19): denizens in their order, the
    * Visions assigned at random, two into the first 10 denizens' packet of
    * 12 and two into the next 15's of 17, each at a random place in its
    * packet; with fewer than 25 denizens, every Vision at a random place in
    * the whole deck. */
  private[walker] def packets(deck: Vector[CardId], dice: WalkerDice)
      : Either[OathViolation, Vector[CardId]] =
    val (visions, denizens) = deck.partition(_.isInstanceOf[VisionId])
    def placed(cards: Vector[CardId], among: Vector[CardId])
        : Either[OathViolation, Vector[CardId]] =
      val size = cards.size + among.size
      if cards.isEmpty then Right(among)
      else permutation(size).map { order =>
        val at = order.take(cards.size).sorted
        (0 until size).foldLeft((Vector.empty[CardId], cards, among)) {
          case ((done, left, rest), slot) =>
            if at.contains(slot) then (done :+ left.head, left.tail, rest)
            else (done :+ rest.head, left, rest.tail)
        }._1
      }
    def permutation(size: Int): Either[OathViolation, Vector[Int]] =
      if size < 2 then Right((0 until size).toVector)
      else dice.shuffle(size).flatMap(order => Either.cond(
        order.sorted == (0 until size).toVector, order,
        OathViolation.InvalidEventOrder(
          s"shuffle source gave $order for $size places")))
    for
      mixed <- permutation(visions.size).map(_.map(visions))
      // Secret Society leaves four Visions; any other count, which no
      // Setup builds, falls back to the whole-deck placement.
      laid <-
        if denizens.size < 25 || mixed.size != 4 then placed(mixed, denizens)
        else for
          first <- placed(mixed.take(2), denizens.take(10))
          second <- placed(mixed.drop(2), denizens.slice(10, 25))
        yield first ++ second ++ denizens.drop(25)
    yield laid
