package distantlands.gameplay.setup

import distantlands.model._
import distantlands.model.OathViolation.UnsupportedChronicle

/** Resolves a later game's Empire from its Chronicle and seats. */
object EmpireSetupRules:
  /** None for a Chronicle with no Chancellor. Such a Chronicle must have no
    * Citizens and no `world` either: CR p. 8 makes a random Citizen the
    * Chancellor only when "there's no Chancellor", a Wide Horizons case the
    * plan factory cannot seat here. The first player must be the seated
    * Chancellor, or a seated Citizen standing in for an absent one (R6). */
  def resolve(chronicle: Chronicle, orders: SetupOrders)
      : Either[OathViolation, Option[EmpireSetup]] =
    val seated = orders.participants.map(p => p.lineageId -> p.playerId).toMap
    def lineages(role: Role) = chronicle.lineages.filter(_.role == role)
      .map(_.id)
    val empireless =
      UnsupportedChronicle("an Empire-less later game requires Wide Horizons")
    lineages(Role.Chancellor).headOption match
      case None => Either.cond(lineages(Role.Citizen).isEmpty &&
        chronicle.world.isEmpty, None, empireless)
      case Some(lineage) =>
        val citizens = lineages(Role.Citizen).flatMap(seated.get)
        for
          _ <- Either.cond(chronicle.world.nonEmpty, (),
            UnsupportedChronicle("an Empire needs at least one site"))
          _ <- Either.cond(chronicle.world.size <= 8, (),
            UnsupportedChronicle("an Empire cannot hold more than 8 sites"))
          chancellor <- seated.get(lineage) match
            case Some(player) => Either.cond(orders.firstPlayer == player,
              player, UnsupportedChronicle(
                "the first player must be the Chancellor"))
            case None if citizens.isEmpty => Left(empireless)
            case None => Either.cond(citizens.contains(orders.firstPlayer),
              orders.firstPlayer, UnsupportedChronicle(
                "the first player must be a seated Citizen"))
        yield Some(EmpireSetup(chancellor, citizens.toSet + chancellor,
          chronicle.world, Option.when(!seated.contains(lineage))(lineage)))
