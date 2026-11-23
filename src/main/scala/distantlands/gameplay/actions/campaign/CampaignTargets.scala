package distantlands.gameplay.actions.campaign

import distantlands.catalog.ExecutableCatalog
import distantlands.model._

/** The Campaign targets question as the table shows it (board decisions
  * design, "Campaign targets"): the target the Campaign already has and the
  * two sums a player weighs. Only the defender's targets are options; a
  * card power that forbids one keeps it, dimmed with the power's reason.
  */
private[campaign] object CampaignTargets:
  val DefenseLabel = "Defense dice (before plans and powers)"
  val ForceLabel = "Defending force"

  /** The mandatory target: a Conquest's origin, a Raid defender's pawn. */
  def lockedOf(kind: CampaignKind, origin: SiteId,
      defender: CampaignDefender): Option[DecisionOption] =
    (kind, defender) match
      case (CampaignKind.Conquest, _) =>
        Some(DecisionOption.Site(DecisionOptionRef.Site(origin)))
      case (CampaignKind.Raid, CampaignDefender.Player(player)) =>
        Some(DecisionOption.Player(DecisionOptionRef.Player(player)))
      case _ => None

  /** The printed defense and the defending force of each target, from the
    * same sources the battle reads (`CampaignBattle`).
    */
  def tallies(catalog: ExecutableCatalog, ready: ReadyGame, actor: PlayerId,
      targets: Vector[DecisionOptionRef]): Vector[DecisionTally] =
    val defended = CampaignBattle.defendedSites(ready, actor,
      targets.collect { case DecisionOptionRef.Site(site) => site })
    Vector(
      DecisionTally(DefenseLabel, "defense", targets.map(target =>
        target -> CampaignBattle.defenseOf(catalog, target)).toMap),
      DecisionTally(ForceLabel, "warbands", targets.map(target =>
        target -> (target match
          case DecisionOptionRef.Site(site) if !defended.contains(site) => 0
          case _ => CampaignBattle.forceOf(ready, target))).toMap))

  def summary(kind: CampaignKind): DecisionSummary = kind match
    case CampaignKind.Conquest => DecisionSummary("Target", "site", "sites")
    case CampaignKind.Raid => DecisionSummary("Raid", "target", "targets")

  /** The confirm button counts the mandatory target with the additions, so
    * it reads "Raid 1 target" before any pick.
    */
  def confirm(kind: CampaignKind): CountedConfirm =
    CountedConfirm(kind match
      case CampaignKind.Conquest => "Conquer"
      case CampaignKind.Raid => "Raid", "target", "targets",
      countsLocked = true)
