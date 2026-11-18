package distantlands.gameplay.setup

import distantlands.catalog.ExecutableCatalog
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

  /** A first game played as an exile-only game: a Chronicle shaped like a
    * first game's (no Chancellor, no Citizen, no Empire site) that carries
    * history, an import for example (owner, 2026-11-12). `sites` are the
    * Atlas's first eight, from the Recent end, placed Hinterward as bandit
    * sites with their stored cards. The stored foundations apply as
    * stored. */
  final case class FirstGameExileOnly(sites: Vector[StoredSite]) extends SetupMode

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
  def isFirstGame(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Boolean =
    chronicle.world.isEmpty && !hasHistory(catalog, chronicle) &&
      chronicle.lineages.forall(_.role == Role.Exile)

  /** What a brand-new Chronicle (`FirstGameChronicleGenerator`) never holds,
    * and a played or imported one may: an altered foundation face or one with
    * alteration sources, a lineage that played before or keeps starting
    * advisers, legacies or starting relics, a scored game or a carried
    * lineage score on the era scorepad, an Oathkeeper goal other than
    * Supremacy, a stocked Reliquary, a denizen or relic stored at an Atlas
    * site, and an Atlas site whose edifice is not the one a new Chronicle
    * stores: an edifice at a site that is no Homeland, and a Homeland storing
    * no edifice, one of another suit, or more than one. A listed normal face
    * with no source, as a played Chronicle may list, is no history. */
  def hasHistory(catalog: ExecutableCatalog, chronicle: Chronicle): Boolean =
    chronicle.foundations.values.exists(state =>
      state.face == FoundationFace.Altered ||
        state.alterationSources.nonEmpty) ||
    chronicle.lineages.exists(lineage => lineage.previousPlayer.nonEmpty ||
      lineage.startingAdvisers.nonEmpty || lineage.legacies.nonEmpty ||
      lineage.startingRelics.nonEmpty) ||
    chronicle.era.games.nonEmpty || chronicle.era.carriedScores.nonEmpty ||
    chronicle.oathkeeperGoal != OathkeeperGoal.Supremacy ||
    chronicle.reliquary.nonEmpty ||
    chronicle.atlasBox.exists(stored => stored.items.exists {
      case _: DenizenId | _: RelicId => true
      case _ => false
    } || !storesItsOwnEdifice(catalog, stored))

  /** Whether `stored` holds the edifice a new Chronicle gives it: its
    * Homeland suit's one edifice, or none at a site that is no Homeland. */
  private def storesItsOwnEdifice(catalog: ExecutableCatalog,
      stored: StoredSite): Boolean =
    val suits = stored.items.collect { case id: EdificeId =>
      catalog.edifice(id).map(_.suit) }
    suits == catalog.site(stored.site).flatMap(_.homeland).toVector
      .map(Some(_))

  /** Whether `chronicle` starts a first game played exile-only: shaped like
    * a first game's, with no Chancellor, no Citizen and no Empire site, but
    * carrying history (owner, 2026-11-12). */
  def isFirstGameExileOnly(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Boolean =
    !isFirstGame(catalog, chronicle) && chronicle.world.isEmpty &&
      chronicle.lineages.forall(lineage => lineage.role != Role.Chancellor &&
        lineage.role != Role.Citizen)

  /** The Chronicle a game starts from: `chronicle` itself, but in a first
    * game played exile-only every lineage's stored legacies, seated and
    * boxed, are wiped back into the derived legacy deck before the deal, as
    * a first game has none (R40). Starting advisers and relics stay. The
    * game's setup mode is read from `chronicle`, before the wipe. */
  def startingChronicle(catalog: ExecutableCatalog, chronicle: Chronicle)
      : Chronicle =
    if !isFirstGameExileOnly(catalog, chronicle) then chronicle
    else chronicle.copy(lineages = chronicle.lineages.map(
      _.copy(legacies = Vector.empty)))

  /** The seated Chancellor leads the Empire. With none seated, because the
    * Chancellor's lineage sits out or the Chronicle lists none, a seated
    * Citizen stands in, and must be the first player (R6); with no Citizen
    * seated either, the game is set up with no Empire. A Chronicle with no
    * Chancellor, no Citizen and no Empire site starts a first game, or an
    * exile-only game when it carries history. A later game's
    * Empire, kept or former, holds 1 to 8 sites (R28). */
  def resolve(catalog: ExecutableCatalog, chronicle: Chronicle,
      orders: SetupOrders): Either[OathViolation, SetupMode] =
    val seated = orders.participants.map(p => p.lineageId -> p.playerId).toMap
    def lineages(role: Role) = chronicle.lineages.filter(_.role == role)
      .map(_.id)
    val chancellors = lineages(Role.Chancellor)
    val citizens = lineages(Role.Citizen)
    if isFirstGame(catalog, chronicle) then Right(SetupMode.FirstGame)
    else if isFirstGameExileOnly(catalog, chronicle)
    then Right(SetupMode.FirstGameExileOnly(chronicle.atlasBox.take(MapState.slots)))
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
