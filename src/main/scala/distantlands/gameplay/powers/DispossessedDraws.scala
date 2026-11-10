package distantlands.gameplay.powers

import distantlands.model._

/** Draws from the Dispossessed (catalog batch 10, slice 6): Great Spire,
  * Fallen Spire and Pilgrimage. A draw shuffles the Dispossessed, so the
  * walker journals its order, then takes cards from its head, as
  * [[ReliquaryDraws]] takes a relic. Nobody knows a card in the
  * Dispossessed, and one moved from it into a discard pile stays unknown
  * until someone peeks (`CardKnowledgeMoves`).
  */
object DispossessedDraws:
  /** The shuffle a draw starts with. It is a tree node, never a member of a
    * batch: the walker fills its order. */
  def shuffle: Shuffle = Shuffle(ShufflePile.Dispossessed)

  /** The first `count` cards of the Dispossessed, read when the batch is
    * built, each put on top of `region`'s discard pile in turn, so the last
    * one taken lies on top. */
  def toPile(ready: ReadyGame, region: Region, count: Int,
      by: Option[PlayerId]): Vector[CoreOperation] =
    ready.game.campaign.dispossessed.take(count).map(card => Move(
      Piece.Card(card), PositionedLocation(Location.Dispossessed),
      PositionedLocation(Location.RegionalDiscard(region), StackPosition.Top),
      by = by))

  /** The bottom `count` denizens of `region`'s discard pile, bottom first,
    * skipping Visions. A pile is stored top last, so its bottom is its
    * head. */
  def bottomDenizens(ready: ReadyGame, region: Region, count: Int)
      : Vector[DenizenId] =
    ready.game.current.commonCards.discard(region)
      .collect { case denizen: DenizenId => denizen }.take(count)

  /** `card` from `region`'s discard pile to the end of the Dispossessed. */
  def intoDispossessed(card: DenizenId, region: Region,
      by: Option[PlayerId]): CoreOperation =
    Move(Piece.Card(card),
      PositionedLocation(Location.RegionalDiscard(region)),
      PositionedLocation(Location.Dispossessed), by = by)
