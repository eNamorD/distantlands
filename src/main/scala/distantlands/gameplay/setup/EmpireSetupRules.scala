package distantlands.gameplay.setup

import distantlands.model._
import distantlands.model.OathViolation.UnsupportedChronicle

/** How a Chronicle's game sets up its map and its roles (rulebook, "Setup
  * for First Game" and "Setup for Later Games": Players, Empire, Setup With
  * No Empire). */
sealed trait SetupMode extends Product with Serializable
object SetupMode:
  /** A first game: a brand-new Chronicle, which lists no Chancellor and no
    * Citizen, holds no Empire site and carries no history
    * (`EmpireSetupRules.isFirstGame`). */
  case object FirstGame extends SetupMode

  /** An exile-only game: a Chronicle shaped like a first game's that
    * carries history, an import for example (owner, 2026-11-12). `sites`
    * are the Atlas's first eight, from the Recent end, placed Hinterward
    * as bandit sites with their stored cards. The stored foundations apply
    * as stored. */
  final case class ExileOnly(sites: Vector[StoredSite]) extends SetupMode

  /** A later game with a Chancellor at the table. */
  final case class WithEmpire(empire: EmpireSetup) extends SetupMode

  /** Setup With No Empire: a later game where no Chancellor and no Citizen
    * is seated. `sites` are the former Empire's, Cradlemost first, and
    * `exiled` the Imperial lineages in the box, which become Exiles. */
  final case class NoEmpire(sites: Vector[StoredSite],
      exiled: Vector[LineageId]) extends SetupMode

/** Resolves how a game starts from its Chronicle and seats. */
object EmpireSetupRules:
  /** Whether `chronicle` starts a first game: it lists no Chancellor and
    * no Citizen, its Empire holds no site, and it carries no history. */
  def isFirstGame(chronicle: Chronicle): Boolean =
    chronicle.world.isEmpty && !hasHistory(chronicle) &&
      chronicle.lineages.forall(_.role == Role.Exile)

  /** What a brand-new Chronicle (`FirstGameChronicleGenerator`) never
    * holds, and a played or imported one may: an altered foundation face or
    * one with alteration sources, a lineage that played before or keeps
    * starting advisers, legacies or starting relics, a scored game on the
    * era scorepad, an Oathkeeper goal other than Supremacy, a stocked
    * Reliquary, and a denizen or relic stored at an Atlas site. A listed
    * normal face with no source, as a TTS import writes, is no history. */
  def hasHistory(chronicle: Chronicle): Boolean =
    chronicle.foundations.values.exists(state =>
      state.face == FoundationFace.Altered ||
        state.alterationSources.nonEmpty) ||
    chronicle.lineages.exists(lineage => lineage.previousPlayer.nonEmpty ||
      lineage.startingAdvisers.nonEmpty || lineage.legacies.nonEmpty ||
      lineage.startingRelics.nonEmpty) ||
    chronicle.era.games.nonEmpty ||
    chronicle.oathkeeperGoal != OathkeeperGoal.Supremacy ||
    chronicle.reliquary.nonEmpty ||
    chronicle.atlasBox.exists(_.items.exists {
      case _: DenizenId | _: RelicId => true
      case _ => false
    })

  /** The seated Chancellor leads the Empire. With none seated, because the
    * Chancellor's lineage sits out or the Chronicle lists none, a seated
    * Citizen stands in, and must be the first player (R6); with no Citizen
    * seated either, the game is set up with no Empire. A Chronicle with no
    * Chancellor, no Citizen and no Empire site starts a first game, or an
    * exile-only game when it carries history. A later game's
    * Empire, kept or former, holds 1 to 8 sites (R28). */
  def resolve(chronicle: Chronicle, orders: SetupOrders)
      : Either[OathViolation, SetupMode] =
    val seated = orders.participants.map(p => p.lineageId -> p.playerId).toMap
    def lineages(role: Role) = chronicle.lineages.filter(_.role == role)
      .map(_.id)
    val chancellors = lineages(Role.Chancellor)
    val citizens = lineages(Role.Citizen)
    if chancellors.isEmpty && citizens.isEmpty && chronicle.world.isEmpty then
      Right(if hasHistory(chronicle) then
        SetupMode.ExileOnly(chronicle.atlasBox.take(MapState.slots))
      else SetupMode.FirstGame)
    else
      val seatedCitizens = citizens.flatMap(seated.get)
      def empire(chancellor: PlayerId) = SetupMode.WithEmpire(EmpireSetup(
        chancellor, seatedCitizens.toSet + chancellor, chronicle.world,
        chancellors.headOption.filterNot(seated.contains)))
      for
        _ <- Either.cond(chronicle.world.nonEmpty, (),
          UnsupportedChronicle("an Empire needs at least one site"))
        _ <- Either.cond(chronicle.world.size <= MapState.slots, (),
          UnsupportedChronicle("an Empire cannot hold more than 8 sites"))
        mode <- chancellors.headOption.flatMap(seated.get) match
          case Some(player) => Either.cond(orders.firstPlayer == player,
            empire(player), UnsupportedChronicle(
              "the first player must be the Chancellor"))
          case None if seatedCitizens.nonEmpty =>
            Either.cond(seatedCitizens.contains(orders.firstPlayer),
              empire(orders.firstPlayer), UnsupportedChronicle(
                "the first player must be a seated Citizen"))
          case None => Right(SetupMode.NoEmpire(chronicle.world,
            chancellors ++ citizens))
      yield mode
