package distantlands.gameplay

import distantlands.catalog.ExecutableCatalog
import distantlands.model._
import distantlands.gameplay._
import distantlands.model.OathEvent._
import distantlands.model.OathState._
import distantlands.model.OathViolation._
import distantlands.gameplay.actions.VisionRules
import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.gameplay.phases.rest.RoundEndRandomPort
import distantlands.model.{Move => CoreMove}

/**
 * State-based checks: bandit refill, the Wake victories, the end of a round
 * and War Exhaustion. With an Empire, `EmpireEndings` decides the end die's
 * win and War Exhaustion's winner; this object journals and replays them.
 *
 * CR p.16 gives each goal a strict qualification rule, then gives the current
 * holder two special tie duties: retain a qualifying tie, or choose among tied
 * leaders when no longer tied. The latter is a player decision and is not
 * guessed here. This is deliberately not a reusable, context-free tie breaker.
 */
object StateBasedEvaluation:
  private val operationAllowlist: OperationPolicy =
    StateBasedOperationPolicy
  private val visionPriority = Vector(VisionRules.Conquest,
    VisionRules.Rebellion, VisionRules.Sanctuary, VisionRules.Faith)
  def banditRefill(catalog: ExecutableCatalog, state: OathState)
      : Either[OathViolation, Option[OathEvent]] = supported(state).map { ready =>
    val refills = emptySites(catalog, ready)
    Option.when(refills.nonEmpty)(BanditsRefilled(refills))
  }

  /** The in-play sites with no warbands and a capacity above 0, each with
    * the bandits it takes: its capacity. Later-game setup fills them too
    * (Empire ruling R30). */
  def emptySites(catalog: ExecutableCatalog, ready: ReadyGame)
      : Vector[(SiteId, Int)] =
    ready.game.current.map.inPlay.flatMap { siteId =>
      ready.game.current.map.sites.get(siteId).collect:
        case SiteState(SiteForces.Empty, _, _, _) if capacity(catalog, siteId) > 0 =>
          siteId -> capacity(catalog, siteId)
    }

  private def capacity(catalog: ExecutableCatalog, site: SiteId): Int =
    catalog.site(site).fold(0)(_.capacity)

  def atWake(state: OathState): Either[OathViolation, Option[OathEvent]] =
    supported(state).map { ready =>
      val current = ready.game.current
      val active = current.turn.activePlayer
      if Imperials.isImperial(ready, active) then None
      else current.title match
        case OathkeeperState(Some(player), TitleSide.Usurper)
            if player == active => Some(UsurperVictory(player))
        case OathkeeperState(Some(player), TitleSide.Oathkeeper)
            if player == active && !current.tracks.usurperLimited =>
          Some(UsurperFlipped(player))
        case _ => None
    }

  def visionAtWake(state: OathState): Either[OathViolation, Option[OathEvent]] =
    supported(state).map { ready =>
      val current = ready.game.current
      current.player(current.turn.activePlayer)
        .filterNot(actor => Imperials.isImperial(ready, actor.player))
        .flatMap { actor =>
        val won = VisionVictoryEligibility.qualifying(actor.player, ready)
        Option.when(won.nonEmpty)(VisionVictory(actor.player, won))
      }
    }

  def endRound(state: OathState, random: RoundEndRandomPort)
      : Either[OathViolation, Vector[OathEvent]] = supported(state).flatMap { ready =>
    val current = ready.game.current
    val turnOrder = TurnOrder.order(ready)
    if current.turn.phase != Phase.RoundEnd ||
        current.turn.activePlayer != turnOrder.head then
      Left(InvalidEventOrder("round ending requires the completed round's final Rest"))
    else if EmpireEndings.rollsEndDie(ready) then
      val round = current.tracks.round
      val face = random.endDie()
      val result = EmpireEndings.stableRegime(ready, face)
      Right(EndDieRolled(round, face, result) +: Option.when(
        result.isEmpty)(RoundEnded(round, Some(round + 1))).toVector)
    else if current.tracks.round < 8 then
      Right(Vector(RoundEnded(current.tracks.round,
        Some(current.tracks.round + 1))))
    else if EmpireEndings.hasEmpire(ready) then
      EmpireEndings.warExhaustion(ready).toRight(InvalidEventOrder(
        "an Empire's War Exhaustion found no winner"))
        .map(won => Vector(RoundEnded(8, None), won))
    else
      val usurper = current.title match
        case OathkeeperState(Some(player), TitleSide.Usurper) => Some(
          WarExhaustionResolved(player, VictoryKind.Usurper, Vector.empty, Vector.empty))
        case _ => None
      val visionary = visionaryWin(ready)
      val fallback = current.title.holder.map(player => WarExhaustionResolved(
        player, VictoryKind.Oathkeeper, Vector.empty, Vector.empty)).getOrElse:
        val candidates = turnOrder
        WarExhaustionResolved(random.choose(candidates),
          VictoryKind.RandomSelection, Vector.empty, candidates)
      Right(Vector(RoundEnded(8, None), usurper.orElse(visionary).getOrElse(fallback)))
  }

  def evolve(catalog: ExecutableCatalog, state: OathState, event: OathEvent,
      restrictions: Vector[OperationRestriction])
      : Either[OathViolation, OathState] =
    event match
      case recorded: BanditsRefilled => banditRefill(catalog, state).flatMap:
        case Some(expected: BanditsRefilled) if expected == recorded =>
          supported(state).flatMap { ready =>
            val operations = recorded.sites.map { case (site, count) =>
              CoreMove(
                Piece.Warbands(ForceKind.Bandit, count),
                PositionedLocation(Location.WarbandBank(ForceKind.Bandit)),
                PositionedLocation(Location.Site(site))
              )
            }
            OperationPipeline.run(
              ready, operations, operationAllowlist, restrictions)(Right(_))
              .flatMap(_.expectEffects(operations,
                "Bandit refill effect differs from recorded outcome"))
              .map(Ready(_))
          }
        case expected => Left(InvalidEventOrder(
          s"Bandit refill mismatch: expected $expected, recorded $recorded"))
      case recorded: UsurperFlipped =>
        atWake(state).flatMap:
          case Some(expected: UsurperFlipped) if expected == recorded =>
            update(state)(current => current.copy(
              title = current.title.copy(side = TitleSide.Usurper)))
          case expected => Left(InvalidEventOrder(
            s"Usurper flip mismatch: expected $expected, recorded $recorded"))
      case recorded: UsurperVictory =>
        atWake(state).flatMap:
          case Some(expected: UsurperVictory) if expected == recorded =>
            update(state)(current => current.copy(result = Some(GameResult(
              recorded.playerId, VictoryKind.Usurper))))
          case expected => Left(InvalidEventOrder(
            s"Usurper victory mismatch: expected $expected, recorded $recorded"))
      case recorded: VisionVictory =>
        visionAtWake(state).flatMap:
          case Some(expected: VisionVictory) if expected == recorded =>
            update(state)(current => current.copy(result = Some(GameResult(
              recorded.playerId, VictoryKind.Visionary, recorded.visions))))
          case expected => Left(InvalidEventOrder(
            s"Vision victory mismatch: expected $expected, recorded $recorded"))
      case recorded: RoundEnded => state match
        case Ready(ready) =>
          val round = ready.game.current.tracks.round
          val expected = RoundEnded(round, Option.when(round < 8)(round + 1))
          val first = TurnOrder.roundStart(ready)
          // A winning end die ends the game, so no round follows it.
          if ready.game.current.turn.phase != Phase.RoundEnd ||
              ready.game.current.result.nonEmpty ||
              ready.game.current.turn.activePlayer != first then
            Left(InvalidEventOrder("round-end event is outside the round-end procedure"))
          else if recorded != expected then Left(InvalidEventOrder(
            s"round-end mismatch: expected $expected, recorded $recorded"))
          else
            // After round 8 War Exhaustion runs from the last round's starter.
            val next = recorded.nextRound.fold(first)(_ =>
              TurnOrder.nextRoundStart(ready))
            update(state)(current => current.copy(
              tracks = current.tracks.copy(
                round = recorded.nextRound.getOrElse(round),
                usurperLimited = current.tracks.usurperLimited &&
                  recorded.completedRound < 3,
                roundStarter = next),
              turn = current.turn.copy(activePlayer = next,
                phase = recorded.nextRound.fold[Phase](
                  Phase.WarExhaustion)(_ => Phase.Wake))))
        case _ => Left(GameNotStarted)
      case recorded: WarExhaustionResolved => expectedWarExhaustion(state).flatMap:
        case ExactWar(expected) if expected == recorded => update(state)(current => current.copy(
          result = Some(GameResult(recorded.winner, recorded.kind,
            recorded.visions))))
        case LeftRandom(candidates) if recorded.kind == VictoryKind.RandomSelection &&
            recorded.visions.isEmpty && recorded.randomCandidates == candidates &&
            candidates.contains(recorded.winner) => update(state)(current => current.copy(
          result = Some(GameResult(recorded.winner, recorded.kind,
            recorded.visions))))
        case expected => Left(InvalidEventOrder(
          s"War Exhaustion mismatch: expected $expected, recorded $recorded"))
      case recorded: EndDieRolled => supported(state).flatMap { ready =>
        val current = ready.game.current
        val round = current.tracks.round
        if current.turn.phase != Phase.RoundEnd || current.result.nonEmpty ||
            current.turn.activePlayer != TurnOrder.roundStart(ready) then
          Left(InvalidEventOrder("the end die is rolled only at a round's end"))
        else if !EmpireEndings.rollsEndDie(ready) then
          Left(InvalidEventOrder("this round's end rolls no end die"))
        else if recorded.round != round || recorded.face < 1 ||
            recorded.face > 6 then
          Left(InvalidEventOrder(s"end die mismatch: recorded $recorded"))
        else
          val expected = EmpireEndings.stableRegime(ready, recorded.face)
          if expected != recorded.result then Left(InvalidEventOrder(
            s"Stable Regime mismatch: expected $expected, recorded " +
              s"${recorded.result}"))
          else update(state)(_.copy(result = recorded.result))
      }
      case _ => Left(InvalidEventOrder("not a state-based evaluation event"))

  /** War Exhaustion's Visionary step: an Exile with a revealed Vision whose
    * goal they meet, in printed priority order (CR p. 19), read Vision by
    * Vision. The winner's every qualifying Vision is recorded (catalog
    * batch 11, ruling 30). */
  private[gameplay] def visionaryWin(ready: ReadyGame)
      : Option[WarExhaustionResolved] =
    val exiles = ready.game.current.players.filterNot(p =>
      Imperials.isImperial(ready, p.player)).map(p =>
      p.player -> VisionVictoryEligibility.qualifying(p.player, ready))
    visionPriority.iterator.flatMap { vision =>
      exiles.find(_._2.contains(vision)).map((player, won) =>
        WarExhaustionResolved(player, VictoryKind.Visionary, won,
          Vector.empty))
    }.nextOption()

  private[gameplay] def supported(state: OathState): Either[OathViolation, ReadyGame] = state match
    case Ready(ready) => Right(ready)
    case _ => Left(GameNotStarted)

  private sealed trait ExpectedWarExhaustion
  private final case class ExactWar(value: WarExhaustionResolved)
      extends ExpectedWarExhaustion
  private final case class LeftRandom(candidates: Vector[PlayerId])
      extends ExpectedWarExhaustion

  private def expectedWarExhaustion(state: OathState)
      : Either[OathViolation, ExpectedWarExhaustion] = supported(state).flatMap { ready =>
    val current = ready.game.current
    if current.tracks.round != 8 || current.result.nonEmpty ||
        current.turn.phase != Phase.WarExhaustion then
      Left(InvalidEventOrder("War Exhaustion is only resolved after round eight"))
    else endRoundWinner(ready, TurnOrder.roundStart(ready))
  }

  private def endRoundWinner(ready: ReadyGame, first: PlayerId)
      : Either[OathViolation, ExpectedWarExhaustion] =
    val current = ready.game.current
    if EmpireEndings.hasEmpire(ready) then
      EmpireEndings.warExhaustion(ready).map(ExactWar(_)).toRight(
        InvalidEventOrder("an Empire's War Exhaustion found no winner"))
    else
      current.title match
        case OathkeeperState(Some(player), TitleSide.Usurper) => Right(ExactWar(
          WarExhaustionResolved(player, VictoryKind.Usurper, Vector.empty, Vector.empty)))
        case _ =>
          visionaryWin(ready).orElse(current.title.holder.map(player =>
            WarExhaustionResolved(player, VictoryKind.Oathkeeper, Vector.empty, Vector.empty))) match
            case Some(value) => Right(ExactWar(value))
            case None =>
              val players = current.players.map(_.player)
              val i = players.indexOf(first)
              val candidates = players.drop(i) ++ players.take(i)
              Right(LeftRandom(candidates))

  private def update(state: OathState)(f: CurrentGameState => CurrentGameState) = state match
    case Ready(ready) => Right(Ready(ready.copy(game = ready.game.copy(
      current = f(ready.game.current)))))
    case _ => Left(GameNotStarted)
