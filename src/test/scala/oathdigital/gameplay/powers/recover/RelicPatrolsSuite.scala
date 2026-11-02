package oathdigital.gameplay.powers.recover

import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.powers.action.DowsingSticksCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{MenuPlay, PowerRules, Table}
import oathdigital.testkit.Table.{p1, p2}

/** Relic Patrols and Bandit Patrols (catalog batch 9, slice 3): a Recover
  * by an enemy of the face's ruler is refused at its start, at the sites
  * the face reaches, and the Act menu says why. */
class RelicPatrolsSuite extends munit.FunSuite:
  private val patrols = RelicPatrolsCard.id
  /** Where the Recover happens: Broken Peaks, difficulty 4, with Dowsing
    * Sticks facedown. */
  private val recoverSite = BrokenPeaksSite.id
  /** Where the Patrols stand. */
  private val patrolsSite = AncientCitySite.id

  /** p1 in Act at Broken Peaks beside a facedown relic, the Patrols on
    * `side` at Ancient City. `staging` then settles who rules which site. */
  private def patrolling(side: EdificeSide)(staging: Table => Table)
      : ReadyGame =
    staging(Table.start.pawn(p1, at = recoverSite)
      .relicAt(DowsingSticksCard.id, recoverSite)
      .edifice(patrols, side, at = patrolsSite)).ready

  private def recoverOption(ready: ReadyGame)(using munit.Location)
      : DecisionOption = MenuPlay.menuOptionFor(catalog, ready, ActionRef.Recover)

  private def refusedWith(ready: ReadyGame, reason: String)(
      using munit.Location): Unit =
    recoverOption(ready) match
      case DecisionOption.Unavailable(_, shown) =>
        assertEquals(shown, OptionReason.Written(reason))
      case other => fail(s"the Recover is offered: $other")
    assert(PowerRules.withWalkerPowers.startWalker(
      Ready(MenuPlay.withoutMenu(ready)), ActionRef.Recover, p1).isLeft)

  private def allowed(ready: ReadyGame)(using munit.Location): Unit =
    assert(recoverOption(ready).available, recoverOption(ready).toString)

  // ---- Relic Patrols ----

  test("an enemy of Relic Patrols' ruler cannot recover at a site that " +
      "ruler rules, and the menu says why"):
    refusedWith(patrolling(EdificeSide.Intact)(_
      .warbandsAt(patrolsSite, p2, 1).warbandsAt(recoverSite, p2, 1)),
      RelicPatrols.reason)

  test("the ruler recovers freely at their own sites"):
    allowed(patrolling(EdificeSide.Intact)(_
      .warbandsAt(patrolsSite, p1, 1).warbandsAt(recoverSite, p1, 1)))

  test("an enemy recovers at a site the ruler does not rule"):
    allowed(patrolling(EdificeSide.Intact)(_
      .warbandsAt(patrolsSite, p2, 1).warbandsAt(recoverSite, p1, 1)))
    allowed(patrolling(EdificeSide.Intact)(_
      .warbandsAt(patrolsSite, p2, 1).bandits(recoverSite, 1)))

  test("Relic Patrols ruled by bandits refuses every player at bandit " +
      "sites, as Bandit Patrols does, and leaves a player's site alone"):
    refusedWith(patrolling(EdificeSide.Intact)(_
      .bandits(patrolsSite, 1).bandits(recoverSite, 1)), RelicPatrols.reason)
    allowed(patrolling(EdificeSide.Intact)(_
      .bandits(patrolsSite, 1).warbandsAt(recoverSite, p1, 1)))

  test("unruled Relic Patrols protect nothing"):
    allowed(patrolling(EdificeSide.Intact)(_.warbandsAt(recoverSite, p2, 1)))
    allowed(patrolling(EdificeSide.Intact)(_.bandits(recoverSite, 1)))

  // ---- Bandit Patrols ----

  test("an enemy of Bandit Patrols' ruler cannot recover at a bandit site"):
    refusedWith(patrolling(EdificeSide.Ruined)(_
      .warbandsAt(patrolsSite, p2, 1).bandits(recoverSite, 1)),
      BanditPatrols.reason)

  test("under bandits every player is refused at bandit sites"):
    refusedWith(patrolling(EdificeSide.Ruined)(_
      .bandits(patrolsSite, 1).bandits(recoverSite, 1)),
      BanditPatrols.reason)

  test("Bandit Patrols' ruler recovers at bandit sites, and an enemy at a " +
      "player's site"):
    allowed(patrolling(EdificeSide.Ruined)(_
      .warbandsAt(patrolsSite, p1, 1).bandits(recoverSite, 1)))
    allowed(patrolling(EdificeSide.Ruined)(_
      .warbandsAt(patrolsSite, p2, 1).warbandsAt(recoverSite, p2, 1)))

  test("the other face is inert: intact Patrols leave bandit sites alone, " +
      "ruined Patrols leave the ruler's sites alone"):
    allowed(patrolling(EdificeSide.Intact)(_
      .warbandsAt(patrolsSite, p2, 1).bandits(recoverSite, 1)))
    allowed(patrolling(EdificeSide.Ruined)(_
      .warbandsAt(patrolsSite, p2, 1).warbandsAt(recoverSite, p2, 1)))

  test("the Patrols write no line and declare no note"):
    assertEquals(RelicPatrols.noteKeys, Vector.empty)
    assertEquals(BanditPatrols.noteKeys, Vector.empty)
