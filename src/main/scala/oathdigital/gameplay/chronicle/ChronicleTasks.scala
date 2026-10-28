package oathdigital.gameplay.chronicle

import oathdigital.gameplay.powers.PowerAnswers
import oathdigital.model._

/** Who acts in the Chronicle, and the questions its steps share. */
object ChronicleTasks:
  def winner(ready: ReadyGame): Either[OathViolation, PlayerId] =
    ready.game.current.result.map(_.winner).toRight(
      OathViolation.InvalidEventOrder("the game has no result"))

  def holder(ready: ReadyGame, task: ChronicleTask)
      : Either[OathViolation, PlayerId] =
    ready.game.current.chronicleTasks.get(task).toRight(
      OathViolation.InvalidEventOrder(s"no one holds the ${task.key} task"))

  def seated(ready: ReadyGame): Vector[PlayerId] =
    ready.game.current.players.map(_.player)

  def lineageOf(ready: ReadyGame, player: PlayerId): Option[LineageId] =
    ready.game.current.players.find(_.player == player).map(_.lineage)

  def playerOptions(players: Vector[PlayerId]): Vector[DecisionOption] =
    players.map(player => DecisionOption.Player(DecisionOptionRef.Player(player)))

  def chosenPlayer(pending: PendingTree, decisionId: String)
      : Either[OathViolation, PlayerId] =
    PowerAnswers.one(pending, decisionId).collect {
      case DecisionOptionRef.Player(player) => player
    }.toRight(PowerAnswers.missing(decisionId))

  /** A node that refuses the walk, for a `Branch` that finds the state it
    * needs missing. */
  def failing(violation: OathViolation): Operation =
    BuildOps((_, _) => Left(violation), required = true)

  /** The nodes `steps` builds for the holder of `task`, or a refusal when
    * no one holds it. */
  def forHolder(ready: ReadyGame, task: ChronicleTask)(
      steps: PlayerId => Vector[Operation]): Vector[Operation] =
    holder(ready, task).fold(violation => Vector(failing(violation)), steps)

  /** Whether `player`'s side rules `site`: the Empire's sites for an
    * Imperial, the player's own for an Exile (Chronicle ruling 11). */
  def rules(ready: ReadyGame, player: PlayerId, site: SiteId): Boolean =
    val current = ready.game.current
    val imperial = lineageOf(ready, player)
      .flatMap(ready.game.campaign.lineages.get).exists(_.role.isImperial)
    current.map.sites.get(site).flatMap(state =>
      SiteRule.ruler(state.forces, current.players).toOption).exists {
        case SiteRuler.Empire => imperial
        case SiteRuler.Player(ruler) => !imperial && ruler == player
        case _ => false
      }
