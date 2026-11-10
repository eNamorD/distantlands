# Catalog Batch 10, Slice 13: Askable Act Action Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let an enemy ask the ruler of Relic Patrols (E17i) or Bandit
Patrols (E17r) for consent to Recover, from the Act menu, and let any other
Act action refused only by waivable restrictions be asked about the same
way.

**Architecture:** `PatrolRule`'s restriction gets a `Waiver` whose granter
is the player who rules the face's site. The Act menu, when a start is
refused, asks `QueuedStart.waiting` for the grants that would let it start
and shows the option `OptionReason.Askable` of their granters. Those grants
are keyed on the menu pick: a new `PhaseMenu.menuPick` reads the open
walk's entry as its actor's answer to `phase.menu`, and a new
`WalkerProcedureRegistry.consentAnswers` adds it to the queued answers that
`waived` and `QueuedStart.waiting` search. The consent service already
asks the granter about an askable option and answers a choose-one with it
on a grant, so no application code changes.

**Tech Stack:** Scala 3, munit, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Catalog batch 10 design](../specs/2026-11-03-catalog-batch-10-design.md):
read "Slice 13: Askable Act action". The rules' basis is New Foundations,
"You choose how to enforce them": "You may give consent to enemies to let
them ignore persistent powers you rule that harm or limit them, except in
Campaign actions. ... Bandits always enforce them."

**Card text** (from `~/projects/cards`; no FAQ for either face):

- Relic Patrols: "Enemies of this card's ruler **cannot** recover relics at
  sites ruled by this card's ruler."
- Bandit Patrols: "Enemies of this card's ruler **cannot** recover relics at
  sites ruled by bandits."

## Plan-time findings

1. **The machinery exists for queued starts.** `QueuedStart.consent`
   already finds the waivable breaches of a refused start and the grants
   that would open it (catalog batch 8, slice 15, for Palanquin's queued
   Travel). It keys each grant on `WalkerProcedureRegistry.queuedAnswers`'
   first answer. A menu pick has no such answer today, so the menu never
   finds an ask.
2. **The menu pick must not join `queuedAnswers`.** The spec says
   "`queuedAnswers` treats a start from the menu as the answered
   `phase.menu` choice". But `gatesStart` is
   `requiresPlayableOption(procedure) && queuedAnswers(state).isEmpty`, so
   a pick there would switch off the playable-option gate for every menu
   start of Search, Travel, Muster and Trade. The plan adds
   `consentAnswers = queuedAnswers ++ menuPick` and points only the two
   consent readers at it: `WalkerPowerGather.waived` and
   `QueuedStart.waiting`. `gatesStart` keeps `queuedAnswers`. A test pins
   the gate.
3. **Only the menu queues an entry with no source.** Every other
   `QueuedProcedure(...)` in `src/main` (CampaignFromSite, Palanquin,
   Second Wind, Sneak Attack, Knights Errant, Hunting Party) passes a
   `Some(PowerSourceRef...)`. So `menuPick` reads an entry with no source,
   no sender, kind `Run`, whose procedure and arguments match an
   `ActOffers.all` offer, as the pick of that offer's button.
4. **The grant survives the whole walk.** `walkerEntry` is set by
   `ProcedureDequeued` and held until `WalkerCompleted`, so the Recover's
   restriction check on every resume finds the menu pick, and the grant
   under it, for as long as the Recover runs.
5. **A grant answers the menu.** `ConsentDesk` resolves the requester's
   parked choose-one with the target on a grant (`grantAnswers`). On the
   resolve, the menu rebuilds its options from the state with the grant
   remembered, so Recover is available, and the driver starts it. A
   refusal leaves the menu parked.
6. **A refusal shows the plain reason.** The Patrols' restriction keeps no
   plain note, so once the ruler refused this turn the menu falls back to
   today's `Written` reason: "Relic Patrols: Cannot recover relics at its
   ruler's sites." Bandits never consent, so under bandit-ruled Patrols the
   menu shows the same reason it shows today.
7. **A Recover another power starts is not askable.** Its entry has a
   source, so it has no menu pick, and its restriction refuses it as
   today. No power starts a Recover yet; Drowned Library is a modifier on
   the menu's own Recover.
8. **The consent grid lists the Patrols on its own.** `ConsentPowers.default`
   reads every `Restriction` that declares a `Waiver` from
   `WalkerPowerCatalog.contributing`, so the Patrols' ruler can set
   Always or Never for an enemy with no further change.
9. **The log line already exists.** `ConsentLines` writes
   "Asked p2 for consent to ignore Relic Patrols and select Recover.",
   naming the option by the label the parked menu gave it.
10. **The new hover reasons** follow Hall of Ministers (E16), whose consent
    key reads "... without ruler's consent.":
    - `edifice.e17.intact no-recover-consent = Cannot recover relics at its ruler's sites without ruler's consent.`
    - `edifice.e17.ruined no-recover-consent = Cannot recover relics at bandit sites without ruler's consent.`

## Global Constraints

- **Slice 12 is independent.** Slice 12 (Search Party) edits no file this
  plan's code touches. It does edit the spec's status line and
  `declared-keys.txt`. Before the merge, rebase onto `0.2.0-alpha`. If the
  status line conflicts, keep every slice that has landed (Task 2, Step 6
  says how to word it).
- Work only in this worktree (`.worktrees/batch-10-slice-13`). Its
  `.tooling` and `node_modules` are symlinks to the main checkout.
- Add files by name. Never `git add -A`. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Shell is zsh. Never write a bare `===` or `=word`, and quote globs.
- Every build compiles with `-Werror`, so an unused import fails it.
- Log lines name no actor (they omit "you").
- Test helper names say what they build.
- `python3 scripts/check-architecture.py` passes: 800 lines per file at
  most, and `gameplay.actions` imports nothing from `gameplay.powers`.

---

### Task 1: The Patrols' waiver and the askable Act menu

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/powers/recover/RelicPatrols.scala`
- Modify: `src/main/scala/distantlands/gameplay/phases/PhaseMenu.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/QueuedStart.scala`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala:495-501`
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala:363-382`
- Modify: `src/test/scala/distantlands/gameplay/powers/recover/RelicPatrolsSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/PhaseMenuSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt` (regenerated)

**Interfaces:**
- Produces:
  - `PatrolRule.granter(ready: ReadyGame): Option[PlayerId]`
  - `PatrolRule.card: PowerSourceRef`
  - `PatrolRule.askedConsent: NoteKey` (on both case objects)
  - `PhaseMenu.menuPick(state: ReadyGame): Option[Answered]`
  - `WalkerProcedureRegistry.consentAnswers(state: ReadyGame): Vector[Answered]`
  - `QueuedStart.waiting(catalog, ready, entry, powers, phasePowers): Option[Vector[QueuedStarts.Needed]]`

- [ ] **Step 1: Write the failing menu-pick tests**

In `src/test/scala/distantlands/gameplay/PhaseMenuSuite.scala`, add
`QueuedStart` to the walker import and add `WalkerProcedureRegistry` too:

```scala
import oathdigital.gameplay.walker.{ProcedureDequeued, QueuedStart,
  WalkerCompleted, WalkerPowers, WalkerProcedureRegistry}
```

If the import already lists other names, keep them; only add these two.
Then add these tests at the end of the class:

```scala
  /** An entry the Act menu queues for `procedure`: no source, no
    * sender. */
  private def menuEntry(procedure: ActionRef): QueuedProcedure =
    QueuedProcedure(p1, procedure, Vector.empty, None, QueueKind.Run)

  test("a menu pick is its actor's answer to the menu, for consent alone"):
    val ready = Table.start.ready
    val recover = QueuedStart.startingFrom(ready, menuEntry(ActionRef.Recover))
    assertEquals(PhaseMenu.menuPick(recover), Some(Answered(
      PhaseMenu.decisionId, DecisionAnswer.ChooseOneAnswer(
        DecisionOptionRef.Button(ActionRef.Recover.key)), p1)))
    assertEquals(WalkerProcedureRegistry.consentAnswers(recover),
      PhaseMenu.menuPick(recover).toVector)
    // A power's queued entry is no pick.
    val sent = QueuedStart.startingFrom(ready, menuEntry(ActionRef.Recover)
      .copy(source = Some(PowerSourceRef.Card(WrestlersCard.id))))
    assertEquals(PhaseMenu.menuPick(sent), None)

  test("a menu pick leaves the playable-option gate on"):
    val search = QueuedStart.startingFrom(Table.start.ready,
      menuEntry(ActionRef.Search))
    assert(PhaseMenu.menuPick(search).nonEmpty)
    assert(WalkerProcedureRegistry.gatesStart(ActionRef.Search, search))
```

`WrestlersCard` and `Table` are already imported by the suite; if `Table`
is not, add it to the `oathdigital.testkit` import.

- [ ] **Step 2: Run them to verify they fail**

```bash
./sbtw "testOnly oathdigital.gameplay.PhaseMenuSuite"
```

Expected: compile error, `value menuPick is not a member of object
PhaseMenu`.

- [ ] **Step 3: Add `menuPick` and `consentAnswers`**

In `PhaseMenu.scala`, add after `optionFor`:

```scala
  /** The menu pick that queued the open walk's entry, as its actor's answer
    * to the menu (catalog batch 10, slice 13): a grant for an Act action
    * the menu showed askable is keyed on it, and the started action finds
    * the grant under it. Only the menu queues an entry with no source and
    * no sender, and only an Act action's pick counts. */
  def menuPick(state: ReadyGame): Option[Answered] = for
    entry <- state.game.current.walkerEntry
    if entry.source.isEmpty && entry.sender.isEmpty &&
      entry.kind == QueueKind.Run
    offer <- ActOffers.all.find(offer => offer.procedure == entry.procedure &&
      offer.args == entry.startArgs)
  yield Answered(decisionId, DecisionAnswer.ChooseOneAnswer(offer.ref),
    entry.actor)
```

In `WalkerProcedureRegistry.scala`, after `queuedAnswers`, add:

```scala
  /** The answers a grant may be keyed on beyond the walk's own: the queued
    * entry's (`queuedAnswers`), then the menu pick that queued it
    * (`PhaseMenu.menuPick`, catalog batch 10, slice 13). Only consent reads
    * the pick. The start gate reads `queuedAnswers` alone, since a pick
    * answers no decision the gated walk previews. */
  def consentAnswers(state: ReadyGame): Vector[Answered] =
    queuedAnswers(state) ++ PhaseMenu.menuPick(state)
```

`PhaseMenu` is already imported there.

In `WalkerPowerGather.scala`, in `waived`, replace
`WalkerProcedureRegistry.queuedAnswers(` with
`WalkerProcedureRegistry.consentAnswers(`, and in its doc comment replace

```
    * A queued entry's start arguments count as answers, by the procedure
    * that reads them.
```

with

```
    * A queued entry's start arguments count as answers, by the procedure
    * that reads them, and so does the menu pick that queued it.
```

If the doc's line breaks differ, keep its wording and add only ", and so
does the menu pick that queued it" after "that reads them".

- [ ] **Step 4: Run the menu-pick tests to verify they pass**

```bash
./sbtw "testOnly oathdigital.gameplay.PhaseMenuSuite"
```

Expected: PASS, every test.

- [ ] **Step 5: Write the failing Patrols consent tests**

In `RelicPatrolsSuite.scala`:

1. Replace the imports with:

```scala
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powers.action.DowsingSticksCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.ProcedureDequeued
import oathdigital.model._
import oathdigital.model.OathState.Ready
import oathdigital.testkit.{MenuPlay, PowerRules, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.accepted
```

2. Replace the class doc comment with:

```scala
/** Relic Patrols and Bandit Patrols (catalog batch 9, slice 3): a Recover
  * by an enemy of the face's ruler is refused at its start, at the sites
  * the face reaches, and the Act menu says why. A player ruler may be asked
  * from the menu (catalog batch 10, slice 13); bandits never consent. */
```

3. After `allowed`, add:

```scala
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
```

4. Replace the first Relic Patrols test with:

```scala
  test("an enemy of Relic Patrols' ruler cannot recover at a site that " +
      "ruler rules, and the menu asks the ruler"):
    askableOfP2(relicPatrolsRuledByP2, RelicPatrols.id,
      RelicPatrols.askedConsent(RelicPatrols.card))
```

5. Replace the first Bandit Patrols test with:

```scala
  test("an enemy of Bandit Patrols' ruler cannot recover at a bandit " +
      "site, and the menu asks the ruler"):
    askableOfP2(patrolling(EdificeSide.Ruined)(_
      .warbandsAt(patrolsSite, p2, 1).bandits(recoverSite, 1)),
      BanditPatrols.id, BanditPatrols.askedConsent(BanditPatrols.card))
```

The two "under bandits" tests stay as they are: `refusedWith` matches the
`Written` reason exactly, so they also prove the bandits' Patrols are not
askable.

6. Replace the last test ("the Patrols write no line and declare no note")
   with:

```scala
  test("the Patrols write no line and declare only their consent reason"):
    assertEquals(RelicPatrols.noteKeys, Vector(RelicPatrols.askedConsent))
    assertEquals(BanditPatrols.noteKeys, Vector(BanditPatrols.askedConsent))
```

7. Add a section before it:

```scala
  // ---- Consent ----

  test("a grant remembered this turn opens the Recover, and the Recover " +
      "the menu starts finds it"):
    val granted = answeredByP2(relicPatrolsRuledByP2, RelicPatrols.id,
      ConsentAnswer.Granted, ConsentTurn.of(relicPatrolsRuledByP2))
    allowed(granted)
    val picked = accepted(MenuPlay.picked(PowerRules.withWalkerPowers,
      Ready(Table.from(granted).phaseMenuParked.ready), p1, recoverPick))
    assert(picked.events.exists {
      case ProcedureDequeued(entry) => entry.procedure == ActionRef.Recover
      case _ => false }, picked.events)

  test("a refusal remembered this turn shows the plain reason"):
    refusedWith(answeredByP2(relicPatrolsRuledByP2, RelicPatrols.id,
      ConsentAnswer.Refused, ConsentTurn.of(relicPatrolsRuledByP2)),
      RelicPatrols.reason)

  test("a grant from another turn is forgotten, and the menu asks again"):
    val earlier = ConsentTurn.of(relicPatrolsRuledByP2).copy(player = p2)
    askableOfP2(answeredByP2(relicPatrolsRuledByP2, RelicPatrols.id,
      ConsentAnswer.Granted, earlier), RelicPatrols.id,
      RelicPatrols.askedConsent(RelicPatrols.card))
```

- [ ] **Step 6: Run them to verify they fail**

```bash
./sbtw "testOnly oathdigital.gameplay.powers.recover.RelicPatrolsSuite"
```

Expected: compile error, `value askedConsent is not a member of object
RelicPatrols`.

- [ ] **Step 7: Give the Patrols a waiver**

In `RelicPatrols.scala`, change the powerresolver import to:

```scala
import oathdigital.gameplay.powerresolver.{ContributingPower, Contribution,
  PowerCtx, Restriction, Waiver}
```

Replace the `PatrolRule` doc comment and class header through
`contributions` with:

```scala
/** A face of the Patrols edifice (E17), a persistent rule that refuses a
  * Recover at its start (catalog batch 9, slice 3), as Vow of Peace refuses
  * a Campaign: a `Restriction` at `RecoverActionEligibility`, judged for
  * the Recover's actor at their pawn's site.
  *
  * The ruler is the ruler of the site the face stands at. Every other
  * player is a player ruler's enemy, and every player is an enemy where
  * bandits rule (batch 8, slice 10, and the Tome Guardians FAQ). Nobody is
  * restricted at an unruled site, and Empire rulers are not supported.
  *
  * A player ruler may waive it (catalog batch 10, slice 13). The Act menu
  * shows Recover askable of them, and a grant is keyed on the menu's
  * Recover pick, which the started Recover finds through
  * `WalkerProcedureRegistry.consentAnswers`. Bandits never consent, so
  * under them the menu shows the face's reason, as it does once the ruler
  * refused this turn. A Recover another power starts has no menu pick, so
  * it is refused.
  */
sealed abstract class PatrolRule extends ContributingPower:
  def edifice: EdificeId
  protected def side: EdificeSide
  /** Whether a Recover at `site`, ruled by `ruler`, is within the face's
    * reach, given the face's own ruler `patrolsRuler`. */
  protected def patrolled(ruler: Option[SiteRuler],
      patrolsRuler: SiteRuler): Boolean
  protected def reason: String
  /** The menu's reason while the ruler may be asked. */
  def askedConsent: NoteKey

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  /** The card the consent prompt and the log name. */
  final def card: PowerSourceRef = PowerSourceRef.Card(edifice)

  /** The player who rules the face's site, who may consent: nobody under
    * bandits or at an unruled site. */
  final def granter(ready: ReadyGame): Option[PlayerId] =
    ready.game.current.edificeSite(edifice, side)
      .flatMap(SiteRulers.rulerOf(ready, _)).collect {
        case SiteRuler.Player(player) => player }

  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.RecoverActionEligibility -> Vector(Restriction(
      (ctx, _) => blocked(ctx),
      waiver = Some(Waiver(granter, card, (_, _) => askedConsent(card))))))
```

Keep `applicable` and `blocked` as they are.

In `RelicPatrols`, after `val reason`, add:

```scala
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Cannot recover relics at its ruler's sites without ruler's
    * consent.": the menu's reason while the ruler may be asked. */
  val askedConsent: NoteKey = notes.key("no-recover-consent", Vector(
    NotePart.Text("Cannot recover relics at its ruler's sites without " +
      "ruler's consent.")))
```

In `BanditPatrols`, after `val reason`, add:

```scala
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Cannot recover relics at bandit sites without ruler's consent.": the
    * menu's reason while the ruler may be asked. */
  val askedConsent: NoteKey = notes.key("no-recover-consent", Vector(
    NotePart.Text("Cannot recover relics at bandit sites without ruler's " +
      "consent.")))
```

`id` is declared above `notes` in both objects, so `NoteSet(id)` sees it.

- [ ] **Step 8: Split `QueuedStart.consent` and add `waiting`**

In `QueuedStart.scala`, replace the whole `consent` method (doc comment
included) with:

```scala
  /** `refusal`, and for a start only waivable restrictions refuse, the
    * grants that would let it start (`waiting`). */
  def consent(catalog: ExecutableCatalog, ready: ReadyGame,
      entry: QueuedProcedure, powers: WalkerPowers,
      phasePowers: PhasePowers): QueuedStarts.Verdict =
    priced(catalog, ready, entry, powers, phasePowers) match
      case Right(_) => QueuedStarts.Verdict.Starts
      case Left(violation) =>
        waiting(catalog, ready, entry, powers, phasePowers).fold(
          QueuedStarts.Verdict.Refused(violation))(
          QueuedStarts.Verdict.Waiting(_))

  /** For a start `priced` refused, the grants that would let it start, when
    * only waivable restrictions refuse it (catalog batch 8, slice 15). Each
    * grant is keyed on the entry's first consent answer
    * (`WalkerProcedureRegistry.consentAnswers`), so the started walk finds
    * it, and the start is tried again with every one of them supposed, as
    * the look-ahead tries a grant before it offers to ask (consent design,
    * ruling 8). `None` when no answer stands for the entry, or when some
    * refusal no grant lifts. The phase menu asks it of every option it
    * shows refused (catalog batch 10, slice 13). */
  def waiting(catalog: ExecutableCatalog, ready: ReadyGame,
      entry: QueuedProcedure, powers: WalkerPowers,
      phasePowers: PhasePowers): Option[Vector[QueuedStarts.Needed]] =
    val state = startingFrom(ready, entry)
    for
      answer <- WalkerProcedureRegistry.consentAnswers(state).headOption
      ref <- answer.answer match
        case DecisionAnswer.ChooseOneAnswer(ref) => Some(ref)
        case _ => None
      tree <- WalkerProcedureRegistry.build(entry.procedure, catalog,
        state, entry.actor, entry.startArgs, phasePowers,
        walkerPowers = powers).toOption
      breaches <- WalkerPowerGather.waivableBreaches(tree, state,
        entry.actor, powers, Vector(answer), Some(entry.procedure), ref)
      needed = breaches.map(breach => QueuedStarts.Needed(breach.power,
        breach.granter, ConsentKey(breach.power, answer.by,
          answer.decisionId, ref), breach.asked, breach.plain))
      supposed = ready.copy(consent = ready.consent.granting(
        needed.map(_.key), ConsentTurn.of(ready)))
      if priced(catalog, supposed, entry, powers, phasePowers).isRight
    yield needed
```

For a queued Travel, `consentAnswers` puts the destination first, so
Palanquin and the phase powers see the same key as before.

- [ ] **Step 9: Show a waivable refusal askable in the menu**

In `PhaseMenu.scala`, change the powerresolver import to:

```scala
import oathdigital.gameplay.powerresolver.{PhasePowers, QueuedStarts}
```

In `offers`, inside `offered`, replace the `Left` arm:

```scala
          case Left(violation) => DecisionOption.Unavailable(shown,
            OptionReason.Written(QueuedStart.needs(catalog, ready, entry,
              walking, judging).getOrElse(reason(procedure, violation)))),
```

with:

```scala
          case Left(violation) => DecisionOption.Unavailable(shown,
            QueuedStart.waiting(catalog, ready, entry, walking, judging)
              .flatMap(askFor(ready, _)).getOrElse(OptionReason.Written(
                QueuedStart.needs(catalog, ready, entry, walking, judging)
                  .getOrElse(reason(procedure, violation))))),
```

Add after `offers`:

```scala
  /** The ask for a start only waivable restrictions refuse (catalog batch
    * 10, slice 13): each granter still to answer, about their powers,
    * covering the grants the started walk looks up. `None` once a granter
    * refused this turn, so the option shows its plain reason. */
  private def askFor(ready: ReadyGame, needed: Vector[QueuedStarts.Needed])
      : Option[OptionReason.Askable] =
    val turn = ConsentTurn.of(ready)
    def answer(one: QueuedStarts.Needed) =
      ready.consent.answer(one.key, Some(one.granter), turn)
    val pending = needed.filterNot(answer(_).contains(ConsentAnswer.Granted))
    if needed.exists(answer(_).contains(ConsentAnswer.Refused)) then None
    else pending.headOption.map(first => OptionReason.Askable(first.power,
      first.asked, pending.map(_.granter).distinct.map(granter =>
        ConsentAsk(granter, pending.filter(_.granter == granter)
          .map(_.power).distinct)), pending.map(_.key)))
```

Then, in the `PhaseMenu` doc comment, replace the last paragraph:

```
  * Each available option carries what its start costs, and each unavailable
  * one why it cannot start (slice 4, plan-time rulings 2 and 3).
```

with:

```
  * Each available option carries what its start costs, and each unavailable
  * one why it cannot start (slice 4, plan-time rulings 2 and 3). An Act
  * action only waivable restrictions refuse is askable of their granters,
  * and a grant is keyed on its pick (`menuPick`; catalog batch 10, slice
  * 13).
```

- [ ] **Step 10: Regenerate the note-key snapshot**

```bash
NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"
```

```bash
git diff src/test/resources/notes/declared-keys.txt
```

Expected: exactly two added lines:

```
+edifice.e17.intact no-recover-consent = Cannot recover relics at its ruler's sites without ruler's consent.
+edifice.e17.ruined no-recover-consent = Cannot recover relics at bandit sites without ruler's consent.
```

If the diff shows anything else, stop and report.

- [ ] **Step 11: Run the touched suites to verify they pass**

```bash
./sbtw "testOnly oathdigital.gameplay.powers.recover.* oathdigital.gameplay.PhaseMenuSuite oathdigital.gameplay.powers.action.Palanquin* oathdigital.application.*Consent*"
```

Expected: PASS, every test. Palanquin and the consent service suites prove
`waiting` keeps the queued Travel's keys.

- [ ] **Step 12: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/recover/RelicPatrols.scala src/main/scala/distantlands/gameplay/phases/PhaseMenu.scala src/main/scala/distantlands/gameplay/walker/QueuedStart.scala src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala src/main/scala/distantlands/gameplay/walker/WalkerPowerGather.scala src/test/scala/distantlands/gameplay/powers/recover/RelicPatrolsSuite.scala src/test/scala/distantlands/gameplay/PhaseMenuSuite.scala src/test/resources/notes/declared-keys.txt
git commit -m "$(cat <<'EOF'
feat(powers): the Patrols' ruler may let an enemy recover

The Act menu shows an action only waivable restrictions refuse as
askable of their granters. A grant is keyed on the menu pick, which the
started action finds through consentAnswers; the start gate still reads
queuedAnswers alone.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint 1:** review Task 1 against spec slice 13 and findings
1 to 8. Check that `gatesStart` still reads `queuedAnswers`.

---

### Task 2: Consent through the service, docs and gates

**Files:**
- Create: `src/test/scala/distantlands/application/PatrolsConsentServiceSuite.scala`
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`
- Modify: `docs/ROADMAP.md`

**Interfaces:**
- Consumes: `RelicPatrols.id`, `RelicPatrols.reason`, `PhaseMenu.optionFor`,
  `PhaseMenu.decisionId` from Task 1.

- [ ] **Step 1: Write the service suite**

First, with the worktree clean after Task 1's commit, check whether slice
12 has landed, and rebase onto it if it has:

```bash
git log --oneline 0.2.0-alpha | grep "feat(powers): implement Search Party"
```

If that prints a line, run `git rebase 0.2.0-alpha`. Resolve a conflict in
`declared-keys.txt` by keeping both sides' lines in sorted order. Then
create `src/test/scala/distantlands/application/PatrolsConsentServiceSuite.scala`:

```scala
package oathdigital.application

import oathdigital.application.gamelog.LogScripts.{formatter, texts}
import oathdigital.gameplay.actions.travel.TravelProcedure
import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  NarrowPassSite}
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powers.action.DowsingSticksCard
import oathdigital.gameplay.powers.recover.{RelicPatrols, RelicPatrolsCard}
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.ProcedureDequeued
import oathdigital.model._
import oathdigital.model.OathEvent.{ConsentAnswered, ConsentRequested}
import oathdigital.testkit.{Commands, MenuPlay, Outcomes, Table}
import oathdigital.testkit.Table.{p1, p2}
import oathdigital.testkit.Unwrap.{accepted, present}

/** Asking the Patrols' ruler from the Act menu through the service
  * (catalog batch 10, slice 13): a grant starts the Recover, a refusal
  * leaves it unavailable, and bandits are never asked. */
class PatrolsConsentServiceSuite extends munit.FunSuite:
  private val gameId = "patrols-consent"
  private val peaks = BrokenPeaksSite.id
  private val city = AncientCitySite.id
  private val recoverPick: DecisionOptionRef =
    PhaseMenu.optionFor(ActionRef.Recover, Vector.empty).getOrElse(
      fail("the Act menu lists no Recover"))

  /** p1 in Act at Broken Peaks beside a facedown relic, with Relic Patrols
    * at Ancient City; `ruling` settles who rules both sites. */
  private def patrolled(ruling: Table => Table): Table =
    ruling(Table.start.pawn(p1, at = peaks)
      .relicAt(DowsingSticksCard.id, peaks)
      .edifice(RelicPatrolsCard.id, EdificeSide.Intact, at = city))

  private val ruledByP2: Table = patrolled(_.warbandsAt(city, p2, 1)
    .warbandsAt(peaks, p2, 1))

  private val request = ConsentRequest(p1, PhaseMenu.decisionId, recoverPick,
    Vector(ConsentAsk(p2, Vector(RelicPatrols.id))),
    Vector(ConsentKey(RelicPatrols.id, p1, PhaseMenu.decisionId,
      recoverPick)))

  private val askRecover: GameCommand =
    Commands.requestConsent(p1, PhaseMenu.decisionId, recoverPick)

  private def asking(service: GameApplicationService)(using munit.Location)
      : GameAccepted = accepted(service.handle(gameId, 0L, askRecover))

  private def answering(service: GameApplicationService, at: GameAccepted,
      choice: ConsentChoice)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence,
      Commands.answerConsent(p2, choice)))

  test("p1 asks p2 from the Act menu, naming Relic Patrols, and the log " +
      "says so"):
    val (service, _) = ruledByP2.service()
    val asked = asking(service)
    assertEquals(asked.events, Vector(ConsentRequested(request)))
    val lines = texts(formatter.format(present(accepted(
      service.history(gameId)), "a stored game").steps, Some(p1)))
    assert(lines.contains("Asked p2 for consent to ignore Relic Patrols " +
      "and select Recover."), lines)

  test("Yes starts the Recover, and the journal reads back the same"):
    val (service, repository) = ruledByP2.service()
    val yes = answering(service, asking(service), ConsentChoice.Yes)
    assertEquals(yes.events.head, ConsentAnswered(p2, true))
    assert(yes.events.exists {
      case ProcedureDequeued(entry) => entry.procedure == ActionRef.Recover
      case _ => false }, yes.events)
    val reloaded = ParkedServiceFixture.replayingSecond(service, repository)
    assertEquals(reloaded.load(gameId).map(_.map(game =>
      (game.state, game.undo))), Right(Some((yes.state, yes.undo))))

  test("No leaves the menu parked and the Recover unavailable, with the " +
      "Patrols' reason"):
    val (service, _) = ruledByP2.service()
    val no = answering(service, asking(service), ConsentChoice.No)
    assertEquals(no.events, Vector(ConsentAnswered(p2, false)))
    val after = Outcomes.ready(no.state)
    assertEquals(MenuPlay.procedureBeyondMenu(after), None)
    MenuPlay.menuOptionFor(catalog, after, ActionRef.Recover) match
      case DecisionOption.Unavailable(_, reason) =>
        assertEquals(reason, OptionReason.Written(RelicPatrols.reason))
      case other => fail(s"the Recover is offered: $other")

  test("under bandit-ruled Patrols nobody may be asked"):
    val (service, _) = patrolled(_.bandits(city, 1).bandits(peaks, 1))
      .service()
    assertEquals(service.handle(gameId, 0L, askRecover),
      Left(GameApplicationError.CommandRejected(
        OathViolation.ConsentUnavailable(
          s"${recoverPick.wireId} is not an option to ask consent for"))))

  test("a Travel from the menu still asks the Narrow Pass's ruler at its " +
      "destination"):
    val (service, _) = ConsentFixture.service()
    val started = ConsentFixture.startTravel(service)
    val asked = accepted(service.handle(ConsentFixture.gameId,
      started.nextSequence, ConsentFixture.requestDunes))
    assertEquals(asked.events.collect { case ConsentRequested(sent) =>
      sent.keys }, Vector(Vector(ConsentKey(NarrowPassSite.pass, p1,
        TravelProcedure.destinationDecisionId, ConsentFixture.dunes))))
```

The refused request's text is `OathRulesWalker.consentAsk`'s, as
`ConsentRequestServiceSuite` asserts it.

- [ ] **Step 2: Run it**

```bash
./sbtw "testOnly oathdigital.application.PatrolsConsentServiceSuite"
```

Expected: PASS, every test. Task 1 holds the behaviour, so this suite
passes at once. If the log line differs, stop and report the line the
formatter wrote; do not edit `ConsentLines`.

- [ ] **Step 3: Rewrite the spec's slice 13**

In `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`, replace
the body of "## Slice 13: Askable Act action" (from "`PatrolRule` gets a"
through "Narrow Pass's travel consent unchanged.") with:

```markdown
`PatrolRule`'s restriction gets a `Waiver` whose granter is the player who
rules the face's site; bandits never consent. Each face declares the
menu's reason while its ruler may be asked: "Cannot recover relics at its
ruler's sites without ruler's consent." and "Cannot recover relics at
bandit sites without ruler's consent."

`PhaseMenu.offers` shows a refused Act option as `OptionReason.Askable`
when `QueuedStart.waiting` finds that grants would let it start, and with
the plain reason once a granter refused this turn. The grants are keyed on
`ConsentKey(power, actor, "phase.menu", offer.ref)`: `PhaseMenu.menuPick`
reads the open walk's entry (no source, no sender, an Act offer's
procedure and arguments) as its actor's answer to the menu, and
`WalkerProcedureRegistry.consentAnswers` adds it to the queued answers
that `waived` and `QueuedStart.waiting` search. `gatesStart` still reads
`queuedAnswers` alone, so the menu pick leaves the playable-option gate
on. `walkerEntry` lasts the walk, so the started Recover finds the grant on
every resume, and the grant lasts the turn. The consent service already
asks the granter and, on a grant, answers the menu with the pick. Any
other Act refusal that carries a `Waiver` becomes askable the same way.

Tests: Recover askable under Relic Patrols and under player-ruled Bandit
Patrols; granted, refused and forgotten on another turn; not askable when
bandits rule the face; the request, its line, Yes and No through the
service; the menu pick and the start gate; Narrow Pass's travel consent
unchanged.
```

- [ ] **Step 4: Update the spec's seams row**

In the "## Seams opened" table, replace:

```markdown
| Askable Act options | 13 | both Patrols |
```

with:

```markdown
| Askable Act options: `PhaseMenu.menuPick`, `consentAnswers` | 13 | both Patrols |
```

- [ ] **Step 5: Update the ROADMAP**

In `docs/ROADMAP.md`, delete this line:

```markdown
- **An askable Act action:** consent for Relic Patrols and Bandit Patrols.
```

In the "Route Recover through the generic surfaces" item, replace:

```markdown
  2026-09-24 parked-decision route design. Wait for catalog batch 10 slices
  8 (rewrites `RecoverProcedure`) and 13 (makes Recover askable).
```

with:

```markdown
  2026-09-24 parked-decision route design.
```

- [ ] **Step 6: Update the spec's status line**

Check whether slice 12 has landed:

```bash
git log --oneline 0.2.0-alpha | grep "feat(powers): implement Search Party"
```

If it prints nothing, set the status line to:

```markdown
**Status:** designed 2026-11-03. Slices 1 to 11 and 13 landed.
```

If it prints a line, Step 1 already rebased onto it; set the status line
to:

```markdown
**Status:** designed 2026-11-03. Slices 1 to 13 landed.
```

If slice 12 lands after this step, the merge's rebase conflicts on this
line; keep the wording that names every landed slice.

- [ ] **Step 7: Run the gates**

```bash
python3 scripts/check-architecture.py
```

```bash
python3 scripts/check-markdown-links.py
```

Expected: both pass. Then run the full suite with coverage in the
background:

```bash
./sbtw clean coverage test coverageReport
```

Expected: every test passes, and statement coverage is at least 92%.

- [ ] **Step 8: Commit**

```bash
git add src/test/scala/distantlands/application/PatrolsConsentServiceSuite.scala docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md docs/ROADMAP.md
git commit -m "$(cat <<'EOF'
test(consent): ask the Patrols' ruler from the Act menu

The service asks the ruler, starts the Recover on a grant and leaves it
unavailable on a refusal; bandits are never asked, and the Narrow Pass
still asks at the destination.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint 2:** review Task 2 against spec slice 13 and findings
5, 6 and 9.
