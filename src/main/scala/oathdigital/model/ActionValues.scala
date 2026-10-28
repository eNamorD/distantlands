package oathdigital.model

enum AttackDieFace extends DieFace { case HollowSword, OneSword, TwoSwordsSkull }
object AttackDieFace:
  def score(faces: Vector[AttackDieFace]): Int =
    faces.count(_ == OneSword) + faces.count(_ == HollowSword) / 2 +
      faces.count(_ == TwoSwordsSkull) * 2

  def skulls(faces: Vector[AttackDieFace]): Int =
    faces.count(_ == TwoSwordsSkull)

enum DefenseDieFace extends DieFace { case Blank, OneShield, TwoShields, Doubler }
object DefenseDieFace:
  def score(faces: Vector[DefenseDieFace]): Int =
    val shields = faces.map {
      case OneShield => 1
      case TwoShields => 2
      case _ => 0
    }.sum
    shields * (1 << faces.count(_ == Doubler))

sealed trait SearchSource extends Product with Serializable
object SearchSource:
  case object WorldDeck extends SearchSource
  final case class RegionalDiscard(region: Region) extends SearchSource

  /** "world deck" or "Cradle discard pile". A log line or a label supplies
    * the article. */
  def name(source: SearchSource): String = source match
    case WorldDeck => "world deck"
    case RegionalDiscard(region) => s"$region discard pile"

  /** The board option that names `source`: the world deck, or a region's
    * discard pile. A Search's source decision and Clay Rattle offer these. */
  def ref(source: SearchSource): DecisionOptionRef = source match
    case WorldDeck => DecisionOptionRef.Deck(CardDeck.World)
    case RegionalDiscard(region) => DecisionOptionRef.DiscardPile(region)

  /** The pile `ref` names, when it names a pile a Search could draw from. */
  def fromRef(ref: DecisionOptionRef): Option[SearchSource] = ref match
    case DecisionOptionRef.Deck(CardDeck.World) => Some(WorldDeck)
    case DecisionOptionRef.DiscardPile(region) => Some(RegionalDiscard(region))
    case _ => None

  /** `source` as a decision option. */
  def option(source: SearchSource): DecisionOption = source match
    case WorldDeck =>
      DecisionOption.Deck(DecisionOptionRef.Deck(CardDeck.World))
    case RegionalDiscard(region) =>
      DecisionOption.DiscardPile(DecisionOptionRef.DiscardPile(region))

sealed trait SearchPlacement extends Product with Serializable
object SearchPlacement:
  case object Discard extends SearchPlacement
  /** A play to `site`'s row, discarding `replace` there first to make
    * room when it names a card. */
  final case class Site(site: SiteId, replace: Option[CardId])
      extends SearchPlacement
  final case class Adviser(
      orientation: Orientation,
      replace: Option[CardId]
  ) extends SearchPlacement


/** A pile the walker can shuffle with a recorded order: a pile Search draws
  * from, the edifice deck (Build Edifice draws a random edifice per suit),
  * or the set-aside relics (Scatter Relics deals them out). */
sealed trait ShufflePile extends Product with Serializable
object ShufflePile:
  final case class Search(source: SearchSource) extends ShufflePile
  case object EdificeDeck extends ShufflePile
  case object SetAsideRelics extends ShufflePile

  /** The pile's cards in stored order. */
  def cards(pile: ShufflePile, current: CurrentGameState): Vector[CardId] =
    pile match
      case Search(source) => current.commonCards.pile(source)
      case EdificeDeck => current.commonCards.edificeDeck
      case SetAsideRelics => current.setAsideRelics

  /** How a failure names the pile. */
  def name(pile: ShufflePile): String = pile match
    case Search(source) => SearchSource.name(source)
    case EdificeDeck => "edifice deck"
    case SetAsideRelics => "set-aside relics"
