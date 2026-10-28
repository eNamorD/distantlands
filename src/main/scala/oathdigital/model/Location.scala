package oathdigital.model

sealed trait Location extends Product with Serializable
object Location:
  final case class Hand(player: PlayerId) extends Location
  /** Player board and everything held around it, including Advisers. */
  final case class PlayArea(player: PlayerId) extends Location
  final case class Site(site: SiteId) extends Location
  final case class OnCard(card: CardId) extends Location
  final case class OnBanner(banner: Banner) extends Location
  final case class FavorBank(suit: Suit) extends Location
  final case class WarbandBank(kind: ForceKind) extends Location
  final case class Deck(deck: CardDeck) extends Location
  final case class RegionalDiscard(region: Region) extends Location
  case object SharedBank extends Location
  case object SetAsideRelics extends Location
  case object SetAsideEdifices extends Location
  case object Reliquary extends Location
  case object Atlas extends Location
  case object Dispossessed extends Location

  private[model] def ownedBy(location: Location,
      player: PlayerId): Boolean = location match
    case Hand(owner) => owner == player
    case PlayArea(owner) => owner == player
    case _ => false
