package oathdigital.model

/** Builds the children of the composite operations in
  * `CoreOperations.scala`: a world card's discard move, the favor and
  * secrets a card returns as it leaves play, and a payment out of a
  * player's play area. Split out of that file, which holds the sealed
  * `CoreOperation` family and is close to the project's per-file bound
  * (catalog batch 11, slice 0).
  */
private[model] object OperationChildren:
  def discardWorld(card: WorldCardId, from: PositionedLocation,
      destination: Region, by: Option[PlayerId],
      position: StackPosition): Vector[Operation] =
    Vector(Move(Piece.Card(card), from,
      PositionedLocation(Location.RegionalDiscard(destination), position),
      resultingOrientation = Some(Orientation.FaceDown), by = by))

  // The suit fallback is never reached with a positive favor: positiveMove
  // returns empty for zero and Bury.standard requires the suit.
  /** The returns a discard makes: favor to `suit`'s bank and secrets to
    * `actingPlayer`, facedown. */
  def returns(card: CardId, suit: Option[Suit], favor: Int,
      secrets: Int, actingPlayer: PlayerId): Vector[CoreOperation] =
    (positiveMove(favor)(Piece.Favor.apply,
      PositionedLocation(Location.OnCard(card)),
      PositionedLocation(Location.FavorBank(suit.getOrElse(Suit.Arcane)))) ++
      returnedSecrets(card, secrets, actingPlayer)).collect:
      case operation: CoreOperation => operation

  def returnedResources(card: CardId, suit: Suit, favor: Int,
      secrets: Int, actingPlayer: PlayerId): Vector[Operation] =
    positiveMove(favor)(Piece.Favor.apply,
      PositionedLocation(Location.OnCard(card)),
      PositionedLocation(Location.FavorBank(suit))) ++
    returnedSecrets(card, secrets, actingPlayer)

  def returnedSecrets(card: CardId, amount: Int,
      actingPlayer: PlayerId): Vector[Operation] =
    if amount == 0 then Vector.empty
    else Vector(
      Move(Piece.Secrets(amount), PositionedLocation(Location.OnCard(card)),
        PositionedLocation(Location.PlayArea(actingPlayer))),
      FlipSecrets(actingPlayer, amount, SecretSide.FaceUp,
        SecretSide.FaceDown))

  /** `amount` of `piece` leaving `player`'s play area for `to`, or nothing
    * for a zero amount. */
  def fromPlayArea(player: PlayerId, amount: Int, piece: Int => Piece,
      to: Location): Vector[Operation] =
    positiveMove(amount)(piece,
      PositionedLocation(Location.PlayArea(player)), PositionedLocation(to))

  private def positiveMove(amount: Int)(piece: Int => Piece,
      from: PositionedLocation,
      to: PositionedLocation): Vector[Operation] =
    if amount == 0 then Vector.empty
    else Vector(Move(piece(amount), from, to))
