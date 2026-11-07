package oathdigital.gameplay.powers.recover

import oathdigital.gameplay.{OathRules, WalkerDiceFixture}
import oathdigital.gameplay.actions.recover.RecoverProcedure
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powers.WalkerPowerCatalog
import oathdigital.gameplay.powers.action.DowsingSticksCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.{ProcedureDequeued, WalkerCompleted}
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{MenuPlay, PowerRules, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted

/** Relic Patrols and Bandit Patrols (catalog batch 9, slice 3): a Recover
  * by an enemy of the face's ruler is refused at its start, at the sites
  * the face reaches, and the Act menu says why. A player ruler may be asked
  * from the menu (catalog batch 10, slice 13); bandits never consent. */
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

  /** The Act menu's Recover button, the target of a Recover grant. */
  private val recoverPick: DecisionOptionRef =
    PhaseMenu.optionFor(ActionRef.Recover, Vector.empty).getOrElse(
      fail("the Act menu lists no Recover"))

  /** The key a grant to p1 for `power` is remembered under. */
  private def recoverKey(power: PowerId): ConsentKey =
    ConsentKey(power, p1, PhaseMenu.decisionId, recoverPick)

  /** p1 in Act at Broken Peaks, both it and the Relic Patrols' site ruled
    * by p2. */
  private val relicPatrolsRuledByP2: ReadyGame =
    patrolling(EdificeSide.Intact)(_.warbandsAt(patrolsSite, p2, 1)
      .warbandsAt(recoverSite, p2, 1))

  /** `ready` with p2's `answer` about `power` remembered for `turn`. */
  private def answeredByP2(ready: ReadyGame, power: PowerId,
      answer: ConsentAnswer, turn: ConsentTurn): ReadyGame =
    ready.copy(consent = ConsentState(answers = Map(recoverKey(power) ->
      answer.by(p2)), turn = Some(turn)))

  private def askableOfP2(ready: ReadyGame, power: PowerId, asked: LogNote)(
      using munit.Location): Unit =
    recoverOption(ready) match
      case DecisionOption.Unavailable(_, ask: OptionReason.Askable) =>
        assertEquals(ask, OptionReason.Askable(power, asked,
          Vector(ConsentAsk(p2, Vector(power))), Vector(recoverKey(power))))
      case other => fail(s"the Recover is not askable of p2: $other")
    // A start with no menu pick finds no grant to ask for.
    assert(PowerRules.withWalkerPowers.startWalker(
      Ready(MenuPlay.withoutMenu(ready)), ActionRef.Recover, p1).isLeft)

  // ---- Relic Patrols ----

  test("an enemy of Relic Patrols' ruler cannot recover at a site that " +
      "ruler rules, and the menu asks the ruler"):
    askableOfP2(relicPatrolsRuledByP2, RelicPatrols.id,
      RelicPatrols.askedConsent(RelicPatrols.card))

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

  test("an enemy of Bandit Patrols' ruler cannot recover at a bandit " +
      "site, and the menu asks the ruler"):
    askableOfP2(patrolling(EdificeSide.Ruined)(_
      .warbandsAt(patrolsSite, p2, 1).bandits(recoverSite, 1)),
      BanditPatrols.id, BanditPatrols.askedConsent(BanditPatrols.card))

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

  // ---- Consent ----

  test("a grant remembered this turn opens the Recover, and the Recover " +
      "the menu starts finds it"):
    val granted = answeredByP2(relicPatrolsRuledByP2, RelicPatrols.id,
      ConsentAnswer.Granted, ConsentTurn.of(relicPatrolsRuledByP2))
    allowed(granted)
    // Recover rolls as it walks, so the rules carry dice.
    val rolling = new OathRules(catalog,
      walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
      walkerDice = WalkerDiceFixture.shields)
    val picked = accepted(MenuPlay.picked(rolling,
      Ready(Table.from(granted).phaseMenuParked.ready), p1, recoverPick))
    assert(picked.events.exists {
      case ProcedureDequeued(entry) => entry.procedure == ActionRef.Recover
      case _ => false }, picked.events)
    // The Recover passes the Patrols again when it resumes, to its end.
    val done = accepted(rolling.resolveWalker(picked.state, p1,
      RecoverProcedure.relicDecisionId, DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Relic(DowsingSticksCard.id))))
    assert(done.events.contains(WalkerCompleted(ActionRef.Recover)),
      done.events)
    // A Recover with no menu pick finds no grant.
    assert(PowerRules.withWalkerPowers.startWalker(
      Ready(MenuPlay.withoutMenu(granted)), ActionRef.Recover, p1).isLeft)

  test("a refusal remembered this turn shows the plain reason"):
    refusedWith(answeredByP2(relicPatrolsRuledByP2, RelicPatrols.id,
      ConsentAnswer.Refused, ConsentTurn.of(relicPatrolsRuledByP2)),
      RelicPatrols.reason)

  test("a grant from another turn is forgotten, and the menu asks again"):
    val earlier = ConsentTurn.of(relicPatrolsRuledByP2).copy(player = p2)
    askableOfP2(answeredByP2(relicPatrolsRuledByP2, RelicPatrols.id,
      ConsentAnswer.Granted, earlier), RelicPatrols.id,
      RelicPatrols.askedConsent(RelicPatrols.card))

  test("the Patrols write no line and declare only their consent reason"):
    assertEquals(RelicPatrols.noteKeys, Vector(RelicPatrols.askedConsent))
    assertEquals(BanditPatrols.noteKeys, Vector(BanditPatrols.askedConsent))
