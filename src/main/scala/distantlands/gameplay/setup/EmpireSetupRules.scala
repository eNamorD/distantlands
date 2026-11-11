package distantlands.gameplay.setup

import distantlands.model._
import distantlands.model.OathViolation.UnsupportedChronicle

/** How a Chronicle's game sets up its map and its roles (rulebook, "Setup
  * for First Game" and "Setup for Later Games": Players, Empire, Setup With
  * No Empire). */
sealed trait SetupMode extends Product with Serializable
object SetupMode:
  /** A first game: the Chronicle lists no Chancellor and no Citizen, and
    * the Empire holds no site. */
  case object FirstGame extends SetupMode

  /** A later game with a Chancellor at the table. */
  final case class WithEmpire(empire: EmpireSetup) extends SetupMode

  /** Setup With No Empire: a later game where no Chancellor and no Citizen
    * is seated. `sites` are the former Empire's, Cradlemost first, and
    * `exiled` the Imperial lineages in the box, which become Exiles. */
  final case class NoEmpire(sites: Vector[StoredSite],
      exiled: Vector[LineageId]) extends SetupMode

/** Resolves how a game starts from its Chronicle and seats. */
object EmpireSetupRules:
  /** The seated Chancellor leads the Empire. With none seated, because the
    * Chancellor's lineage sits out or the Chronicle lists none, a seated
    * Citizen stands in, and must be the first player (R6); with no Citizen
    * seated either, the game is set up with no Empire. A later game's
    * Empire, kept or former, holds 1 to 8 sites (R28). */
  def resolve(chronicle: Chronicle, orders: SetupOrders)
      : Either[OathViolation, SetupMode] =
    val seated = orders.participants.map(p => p.lineageId -> p.playerId).toMap
    def lineages(role: Role) = chronicle.lineages.filter(_.role == role)
      .map(_.id)
    val chancellors = lineages(Role.Chancellor)
    val citizens = lineages(Role.Citizen)
    if chancellors.isEmpty && citizens.isEmpty && chronicle.world.isEmpty then
      Right(SetupMode.FirstGame)
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
