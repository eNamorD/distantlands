package distantlands.protocol.projection

final case class SetupPlayerProjection(
    playerId: String,
    displayName: String,
    role: String,
    color: distantlands.model.PlayerColor
)
final case class CardDetailsProjection(
    cardId: String,
    cardKind: String,
    name: String,
    suit: Option[String] = None,
    restrictions: Option[String] = None,
    rulesText: Option[String] = None,
    orientation: Option[String] = None,
    side: Option[String] = None,
    favor: Int = 0,
    secrets: Int = 0,
    relicValue: Option[Int] = None,
    defense: Option[Int] = None,
    hidden: Boolean = false,
    implemented: Boolean = true,
    powers: Vector[PowerFaceProjection] = Vector.empty,
    /** Warbands standing on the card, one entry per force (Obsidian
      * Cage). */
    warbands: Vector[SiteForcesProjection] = Vector.empty,
    /** The pawn the Tracker's holder chose last turn (Tracker). */
    tracking: Option[TrackedPawnProjection] = None
)
/** A Tracker's record, as its card shows it: the player whose pawn was
  * chosen, their colour, and the card's words ("Tracking Red"). */
final case class TrackedPawnProjection(playerId: String,
    color: distantlands.model.PlayerColor, label: String)
/** One printed power as its card lays it out: a braid above it, black when
  * `persistent`, with the `cost` tokens on it in printed order ("favor",
  * "secret-burnt") and the modifier icon at its left, then `text`, the
  * power's words without the cost symbols they open with. */
final case class PowerFaceProjection(text: String,
    cost: Vector[String] = Vector.empty, persistent: Boolean = false,
    modifier: Option[ModifierMarkProjection] = None)
/** The modifier icon a power prints: the major actions it modifies, by key
  * ("travel"), or a battle plan, which is a Campaign modifier, with the
  * sides of a Campaign that may play it ("attack", "defense"). */
sealed trait ModifierMarkProjection extends Product with Serializable
object ModifierMarkProjection:
  final case class Action(actions: Vector[String])
      extends ModifierMarkProjection
  final case class BattlePlan(sides: Vector[String])
      extends ModifierMarkProjection
final case class SiteCardProjection(
    cardId: String,
    label: String,
    details: Option[CardDetailsProjection] = None):
  def denizenId: String = cardId
/** A site's relics in their places, as the viewer may see them: a relic
  * the viewer knows is its card, any other a hidden back. A relic's index
  * here is its `site-relic-slot`. */
final case class SiteRelicsProjection(
    relics: Vector[CardDetailsProjection] = Vector.empty):
  /** Every relic at the site; site relics lie facedown. */
  def facedownCount: Int = relics.size
  /** The relics the viewer knows. */
  def knownRelics: Vector[CardDetailsProjection] = relics.filterNot(_.hidden)
final case class ForgeCostProjection(favor: Int, secrets: Int)
/** Whose warbands hold a site. One case per kind of force, so a force, its
  * ruler and its colour cannot disagree.
  */
sealed trait SiteForcesProjection extends Product with Serializable:
  def count: Int
  def label: String
object SiteForcesProjection:
  final case class Exile(count: Int, rulerPlayerId: String,
      color: distantlands.model.PlayerColor,
      label: String) extends SiteForcesProjection
  final case class Imperial(count: Int, label: String)
      extends SiteForcesProjection
  final case class Bandit(count: Int, label: String)
      extends SiteForcesProjection
final case class SetupSiteProjection(
    siteId: String,
    label: String,
    looseFavor: Int,
    looseSecrets: Int,
    denizenCapacity: Int,
    relicCapacity: Int,
    denizens: Vector[SiteCardProjection],
    relics: SiteRelicsProjection,
    defense: Int = 0,
    recoverDifficulty: Option[Int] = None,
    forgeCost: Option[ForgeCostProjection] = None,
    powers: Vector[SitePowerProjection] = Vector.empty,
    forces: Option[SiteForcesProjection] = None
)
final case class SetupRegionProjection(
    regionId: String,
    sites: Vector[SetupSiteProjection],
    discardCount: Int = 0,
    discardTopCardKind: Option[String] = None,
    /** The pile top first. A card the viewer may not identify is the
      * projector's `hiddenCard`, so the pile's size is always public and
      * its faces are not. */
    discardCards: Vector[CardDetailsProjection] = Vector.empty
)
/** A site's power: its printed kind, label and text, and `id`, the power's
  * id, which a client keys the power's consent preferences on. */
final case class SitePowerProjection(kind: String, label: String,
    description: Option[String], id: String = "")
final case class PawnLocationProjection(playerId: String, siteId: String)
final case class ActivePlayerResourcesProjection(
    favor: Int,
    faceUpSecrets: Int,
    faceDownSecrets: Int,
    committedSecrets: Int,
    totalSecrets: Int,
    supply: Int
)
final case class CurrentSiteResourcesProjection(
    siteId: String,
    favor: Int,
    secrets: Int
)
final case class FavorBankProjection(suit: String, count: Int)
final case class GameTracksProjection(round: Int, visionsDrawn: Int,
    usurperLimited: Boolean, limiterRound: Int, firstPlayerId: String)
