package distantlands.gameplay.setup

import distantlands.gameplay.chronicle.ChronicleLegacyRules
import distantlands.gameplay.chronicle.legacies.{IronHandCard, RivalCard}
import distantlands.gameplay.setup.legacies.{CircleOfSwords,
  CircleOfSwordsCard, ScepterBearerCard}
import distantlands.model._

/** The legacies' SETUP powers that change what game start builds (Legacy
  * Setup powers design, slice 1). Game start asks no question, so these
  * read the Chronicle's stored lineages and the seats, never a walk. A
  * later walker step that needs the same answer asks again from the
  * Chronicle, which keeps its shape fixed on resume. */
object StartingLegacies:
  val exileRoles: Set[Role] = Set(Role.Exile)

  /** The seated lineage whose stored record holds `legacy` active, if its
    * stored role is one of `roles`. A lineage the Chronicle does not list
    * holds nothing (R1). */
  def holder(chronicle: Chronicle, seated: Vector[LineageId],
      legacy: LegacyId, roles: Set[Role] = Role.values.toSet)
      : Option[LineageId] =
    chronicle.lineages.find(lineage => seated.contains(lineage.id) &&
      roles(lineage.role) &&
      lineage.legacies.contains(LegacyState(legacy, active = true)))
      .map(_.id)

  /** The purple warbands an Imperial Circle of Swords holder takes when
    * `others` other Imperial boards each take 3 first (R3). */
  def imperialBoard(others: Int): Int =
    math.max(0, math.min(CircleOfSwords.warbands,
      MaterialBankState.ImperialWarbands - 3 * others))

  /** What the SETUP powers change for `chronicle` seated as `seats` in
    * `mode`. A first game's legacies are all dormant, so nothing acts. */
  def of(chronicle: Chronicle, seats: Vector[FirstGameParticipant],
      mode: SetupMode): StartLegacies =
    val seated = seats.map(_.lineageId)
    val playerOf = seats.map(seat => seat.lineageId -> seat.playerId).toMap
    val imperial = mode match
      case SetupMode.WithEmpire(empire) => empire.imperial
      case _ => Set.empty[PlayerId]
    val circle = holder(chronicle, seated, CircleOfSwordsCard.id)
      .map(playerOf).map(player => player ->
        (if imperial(player) then imperialBoard(imperial.size - 1)
        else CircleOfSwords.warbands))
    // A seated Chancellor or Citizen always sets up an Empire, so an
    // Imperial holder is one of its players.
    val bearer = holder(chronicle, seated, ScepterBearerCard.id,
      ChronicleLegacyRules.imperialRoles).map(playerOf)
    // Only an Exile's Iron Hand acts: an Imperial discarded it (R4). An
    // exile-only game restored no Empire, so it has no site up to the
    // Empire divider for the holder's warbands. "If you were in the
    // previous game": a holder whose lineage sat it out does nothing (R39).
    val ironHand = mode match
      case SetupMode.ExileOnly(_) => None
      case _ => holder(chronicle, seated, IronHandCard.id, exileRoles)
        .filter(lineage => chronicle.lineages.exists(stored =>
          stored.id == lineage && stored.previousPlayer.nonEmpty))
    val (rival, sites) = rivalSites(chronicle, seated,
      placedBefore(chronicle, mode))
    StartLegacies(circle.toMap, ironHand, rival, sites, bearer)

  /** Rival's Exile holder and the sites it places before the foundations:
    * the Atlas's next sites from the Recent end, into the three Cradlemost
    * slots left empty once `placed` sites are on the map, fewer when the
    * map or the Atlas runs short (ruling 8). */
  def rivalSites(chronicle: Chronicle, seated: Vector[LineageId],
      placed: Int): (Option[LineageId], Vector[StoredSite]) =
    holder(chronicle, seated, RivalCard.id, exileRoles) match
      case None => None -> Vector.empty
      case some => some -> chronicle.atlasBox.take(math.min(3,
        math.max(0, MapState.slots - placed)))

  /** The sites game start places before Rival in `mode`: a first game's
    * and an exile-only game's eight, else the Empire's stored sites, kept
    * or former. */
  def placedBefore(chronicle: Chronicle, mode: SetupMode): Int = mode match
    case SetupMode.FirstGame | SetupMode.ExileOnly(_) => MapState.slots
    case _ => chronicle.world.size
