package distantlands.application.gamelog

import distantlands.application._
import distantlands.model._

/** A journal built by real play through `GameApplicationService` on every
  * run (spec, "Test journals"). No stored game is a fixture: a script a new
  * card changes fails at the command that no longer applies.
  */
final case class Script(name: String, service: GameApplicationService,
    actor: PlayerId):
  def history(using munit.Location): GameHistory =
    service.history(name) match
      case Right(Some(history)) => history
      case other => munit.Assertions.fail(s"$name has no history: $other")
  def players(using munit.Location): Vector[PlayerId] =
    history.steps.last.after match
      case OathState.Ready(ready) => ready.game.current.players.map(_.player)
      case other => munit.Assertions.fail(s"$name is not ready: $other")

  /** The first seated player who is not the actor: p2 when p1 acts, p1
    * otherwise. */
  def otherSeat(using munit.Location): PlayerId =
    players.find(_ != actor).getOrElse(munit.Assertions.fail(
      s"$name has no player besides $actor among $players"))

/** The one name the log suites import: the support every script leans on
  * and every script, each kept in the file of the procedure it plays
  * (`LogScriptSupport`, `PhaseLogScripts`, `ActionLogScripts`,
  * `RaidLogScripts`, `NegotiationLogScripts`, `PowerLogScripts`). A new
  * script goes in the file of its procedure and is exported here.
  */
object LogScripts:
  export LogScriptSupport.{presentation, formatter, format, text, texts,
    lineStarting, entryStarting, playerLabel, inserted, withoutNotes,
    formatAppended, formatWithoutNotes, atTable}
  export PhaseLogScripts.{recordedEventsBoard, wealthFromMenu, restFromMenu,
    wealthStarted, woken, wokenRevealingAll, round, oathkeeperTie, takeWealth}
  export ActionLogScripts.{search, forcedLaborSearch, facedownAdviser, muster,
    initiatedMuster, vowMuster, golemMuster, trade, recoverFailed,
    recoverSucceeded, revealRelic, peekRelics, warbandsMoved, recoverWorship,
    forge, banners}
  export RaidLogScripts.{raid, raidUnderCarefulPlans, queuedRaid,
    queuedRaidThenSkip, raidAnswers}
  export NegotiationLogScripts.{negotiationDeclined, negotiationAgreed,
    negotiationDisclosed}
  export PowerLogScripts.{silverTongue, augury, twoPicks, facedownWithCry,
    travelWithTents, gamblingHall, wolves, oracle, barbedNet, hunger,
    familyHeirloom, heirloomKept, heirloomReturned}

  /** Every script by its stream name, for the suites that hold for each. */
  val named: Vector[(String, () => Script)] = Vector(
    "woken" -> (() => woken), "round" -> (() => round),
    "oathkeeper" -> (() => oathkeeperTie), "search" -> (() => search),
    "augury" -> (() => augury), "facedown-adviser" -> (() => facedownAdviser),
    "muster" -> (() => muster), "trade" -> (() => trade),
    "take-wealth" -> (() => takeWealth),
    "recover-failed" -> (() => recoverFailed),
    "recover-succeeded" -> (() => recoverSucceeded),
    "reveal-relic" -> (() => revealRelic),
    "peek-relics" -> (() => peekRelics),
    "warbands-moved" -> (() => warbandsMoved), "forge" -> (() => forge),
    "banners" -> (() => banners), "raid" -> (() => raid),
    "negotiation-declined" -> (() => negotiationDeclined),
    "negotiation-agreed" -> (() => negotiationAgreed),
    "negotiation-disclosed" -> (() => negotiationDisclosed),
    "use-power" -> (() => silverTongue),
    "gambling-hall" -> (() => gamblingHall), "wolves" -> (() => wolves),
    "oracle" -> (() => oracle), "barbed-net" -> (() => barbedNet),
    "hunger" -> (() => hunger),
    "menu-wealth" -> (() => wealthFromMenu),
    "menu-rest" -> (() => restFromMenu),
    "queued-raid-skip" -> (() => queuedRaidThenSkip),
    "heirloom-kept" -> (() => heirloomKept),
    "heirloom-returned" -> (() => heirloomReturned))
