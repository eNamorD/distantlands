package oathdigital.gameplay.operations

import oathdigital.model._

/** The `Shuffle` write: a shuffle pile takes the order the walker
  * generated. The order must hold exactly the pile's cards, so a recorded
  * shuffle can reorder a pile but never add, drop or replace a card.
  *
  * A shuffle forgets every card of its pile, for every player: a peek told
  * a player which card lay where, and after a shuffle nobody knows.
  */
private[operations] object PileOperations:
  import OperationError.InvalidDescription

  def shuffle(ready: ReadyGame, pile: ShufflePile,
      order: Option[Vector[CardId]]): Either[OperationError, ReadyGame] =
    val held = ShufflePile.cards(pile, ready.game)
    val name = ShufflePile.name(pile)
    order match
      case None => Left(InvalidDescription(
        s"a shuffle of the $name has no order"))
      case Some(cards) if cards.size != held.size || cards.diff(held).nonEmpty =>
        Left(InvalidDescription(
          s"a shuffle of the $name must hold exactly its cards"))
      case Some(cards) => Right(withCards(ready, pile, cards).copy(
        knowledge = cards.foldLeft(ready.knowledge)(CardKnowledgeMoves.forget)))

  private def withCards(ready: ReadyGame, pile: ShufflePile,
      cards: Vector[CardId]): ReadyGame = pile match
    case ShufflePile.SuitedReserve(suit) => ready.updateCampaign(campaign =>
      campaign.copy(suitedReserves = campaign.suitedReserves.updated(suit,
        cards.collect { case id: DenizenId => id })))
    case ShufflePile.Reliquary => ready.updateCampaign(_.copy(
      reliquary = cards.collect { case id: RelicId => id }))
    case other => ready.updateCurrent(current => other match
      case ShufflePile.Search(source) => current.copy(commonCards =
        current.commonCards.withPile(source,
          cards.collect { case id: WorldCardId => id }))
      case ShufflePile.EdificeDeck => current.copy(commonCards =
        current.commonCards.copy(
          edificeDeck = cards.collect { case id: EdificeId => id }))
      case ShufflePile.SetAsideRelics => current.copy(
        setAsideRelics = cards.collect { case id: RelicId => id })
      case ShufflePile.ChronicleGathering => current.copy(chronicleGathering =
        cards.collect { case id: WorldCardId => id })
      case ShufflePile.SuitedReserve(_) | ShufflePile.Reliquary => current)
