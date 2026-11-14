package distantlands.gameplay.chronicle

import distantlands.gameplay.chronicle.legacies.{GreatArchitectCard,
  OathsOfLoyaltyCard, RivalCard, SteadfastCard, TheBrightBeaconCard,
  TheEarCard}
import distantlands.model._

/** The seam the legacies' Chronicle powers share (Legacy Chronicle powers
  * design, "In effect is read live"). Every power, and every step a power
  * changes, asks it when its window is reached, never when the walk is
  * built: a `Branch` whose children depended on state the walk itself
  * changes would lose its shape on resume (Legacies slice 3). */
object ChronicleLegacyRules:
  /** The seated player whose lineage holds `legacy` active, if their role
    * is one of `roles`. A boxed lineage's legacies never act and a dormant
    * legacy has no power (ruling 1); a role condition ("if you are an
    * Imperial") is read now, so the Crown and Offer Citizenship change it
    * for every later step (ruling 2). */
  def holder(state: ReadyGame, legacy: LegacyId,
      roles: Set[Role] = Role.values.toSet): Option[PlayerId] =
    state.game.current.players.find(player =>
      state.game.campaign.lineages.get(player.lineage).exists(lineage =>
        roles(lineage.role) &&
          lineage.legacies.contains(LegacyState(legacy, active = true))))
      .map(_.player)

  /** Whether `player` holds Steadfast in effect, so none of their other
    * legacies go dormant (ruling 5). */
  def keepsOthersActive(state: ReadyGame, player: PlayerId): Boolean =
    holder(state, SteadfastCard.id).contains(player)

  /** Who keeps cards from Shape Empire's discard: Rival's holder (ruling 9). */
  def protector(state: ReadyGame): Option[PlayerId] =
    holder(state, RivalCard.id)

  /** The Imperial roles, for a power that says "if you are an Imperial". */
  val imperialRoles: Set[Role] = Role.values.filter(_.isImperial).toSet

  /** Who performs Build Edifice: an Imperial holding Great Architect, else
    * the Throne holder (ruling 10). */
  def builder(state: ReadyGame): Either[OathViolation, PlayerId] =
    holder(state, GreatArchitectCard.id, imperialRoles)
      .fold(ChronicleTasks.holder(state, ChronicleTask.Throne))(Right(_))

  /** Whether the builder may bury any card for any available edifice
    * (ruling 10). */
  def anyEdifice(state: ReadyGame): Boolean =
    holder(state, GreatArchitectCard.id, imperialRoles).nonEmpty

  /** Who places the scattered relics: The Ear's holder (ruling 14). */
  def scatterer(state: ReadyGame): Option[PlayerId] =
    holder(state, TheEarCard.id)

  /** Who keeps faceup advisers in Gather Denizens: Oaths of Loyalty's
    * holder (ruling 15). */
  def keepsAdvisers(state: ReadyGame): Option[PlayerId] =
    holder(state, OathsOfLoyaltyCard.id)

  /** Whether a starting adviser `player` would gain is buried instead
    * (ruling 16; R24). */
  def buriesStartingAdvisers(state: ReadyGame, player: PlayerId): Boolean =
    keepsAdvisers(state).contains(player)

  /** Whether a stored lineage holds Oaths of Loyalty active, for a deal
    * read from the Chronicle before a game starts (ruling 16). */
  def oathsHolds(lineage: LineageState): Boolean =
    lineage.legacies.contains(LegacyState(OathsOfLoyaltyCard.id, active = true))

  /** The Beacon task holder when they hold The Bright Beacon in effect, so
    * may take newcomers from the Dispossessed (ruling 17). */
  def beaconFromDispossessed(state: ReadyGame): Option[PlayerId] =
    ChronicleTasks.holder(state, ChronicleTask.Beacon).toOption
      .filter(holder(state, TheBrightBeaconCard.id).contains)
