package oathdigital.model

/** A card [[Bury]] can send to the bottom of its deck. Split out of
  * `CoreOperations.scala`, which holds the sealed `CoreOperation` family
  * and is close to the project's per-file bound. */
sealed trait BuryableCard extends Product with Serializable:
  def id: CardId
  def deck: CardDeck
object BuryableCard:
  final case class Denizen(id: DenizenId) extends BuryableCard:
    override val deck: CardDeck = CardDeck.World
  final case class Relic(id: RelicId) extends BuryableCard:
    override val deck: CardDeck = CardDeck.Relic
  final case class Edifice(id: EdificeId) extends BuryableCard:
    override val deck: CardDeck = CardDeck.Edifice
  final case class Vision(id: VisionId) extends BuryableCard:
    override val deck: CardDeck = CardDeck.World
