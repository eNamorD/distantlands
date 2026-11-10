package distantlands.gameplay.chronicle

import distantlands.model._

/** Separate Visions, the World's first step (CR p. 38): every Vision in
  * play leaves it. The Chronicle does not store Visions, and setup adds all
  * five again. */
object SeparateVisions:
  val steps: Vector[Operation] = Vector(BuildOps((state, _) =>
    state.game.cardIndex.left.map(problems => OathViolation.InvalidEventOrder(
      s"the card index is broken: $problems")).flatMap(index =>
      index.ids.toVector.collect { case id: VisionId => id }.sortBy(_.value)
        .foldLeft[Either[OathViolation, Vector[CoreOperation]]](
          Right(Vector.empty)) { (moves, vision) =>
          for
            done <- moves
            location <- index.locationOf(vision).flatMap(at =>
              from(at.container)).toRight(OathViolation.InvalidEventOrder(
                s"${vision.value} lies where a Vision cannot be moved from"))
          yield done :+ Move(Piece.Card(vision), PositionedLocation(location),
            PositionedLocation(Location.SharedBank))
        }),
    required = true))

  private def from(container: CardContainer): Option[Location] =
    container match
      case CardContainer.Deck(deck) => Some(Location.Deck(deck))
      case CardContainer.RegionalDiscard(region) =>
        Some(Location.RegionalDiscard(region))
      case CardContainer.Player(player, PlayerCardArea.Hand) =>
        Some(Location.Hand(player))
      case CardContainer.Player(player, _) => Some(Location.PlayArea(player))
      case CardContainer.Dispossessed => Some(Location.Dispossessed)
      case _ => None
