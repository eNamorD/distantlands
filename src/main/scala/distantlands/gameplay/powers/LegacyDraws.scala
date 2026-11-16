package distantlands.gameplay.powers

import distantlands.model._

/** A draw from the top of the legacy deck into the drawer's hand (catalog
  * batch 13, rulings 1 and 6): Ancient Writ's 3, Amber Doors' 2 and Lost
  * Doors' 1. The deck is not shuffled during a game, so a discard at its bottom comes
  * back only once every legacy above it is drawn. The draw is a `Draw`, a
  * hidden outcome, so the legality search stops at it and undo never takes
  * it back. A short deck draws what remains. An empty deck draws nothing,
  * and its line says so.
  */
object LegacyDraws:
  /** The keys this object owns, which no one power owns. A power that
    * writes one borrows it. */
  val notes: NoteSet = NoteSet.unowned("LegacyDraws")
  /** "The legacy deck was empty." */
  val empty: NoteKey = notes.key("legacy-deck-empty",
    Vector(NotePart.Text("The legacy deck was empty.")))

  /** The top `count` legacies of the deck in `ready`, fewer from a short
    * deck. */
  def top(ready: ReadyGame, count: Int): Vector[LegacyId] =
    ready.game.current.commonCards.legacyDeck.take(count)

  /** `drawer` takes `legacies`, the deck's top, into their hand. Callers
    * read `legacies` with [[top]] in the same `Branch`. */
  def draw(drawer: PlayerId, legacies: Vector[LegacyId]): Operation =
    BuildOps((_, _) => Right(Vector(Draw(drawer, legacies,
      Location.Deck(CardDeck.Legacy), Location.Hand(drawer)))),
      required = true)

  /** A power's draw: `drawer` draws up to `count` legacies, written under
    * `power` from `source`. `drew` is the line for the number actually
    * drawn. The deck is read when the step is reached, not when the tree
    * is built. Setup's keep uses [[top]] and [[draw]] alone: it writes no
    * draw line. */
  def steps(power: PowerId, drawer: PlayerId, count: Int,
      source: PowerSourceRef, drew: Int => LogNote): Vector[Operation] =
    Vector(Branch((live, _) =>
      top(live, count) match
        case Vector() => Vector(Note(power, _ => Some(empty(source))))
        case taken => Vector(draw(drawer, taken),
          Note(power, _ => Some(drew(taken.size))))))

  /** The legacies waiting in `player`'s hand, in draw order. */
  def hand(ready: ReadyGame, player: PlayerId): Vector[LegacyId] =
    ready.game.current.legacyHands.getOrElse(player, Vector.empty)
