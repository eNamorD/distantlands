# Legacies, Slice 3: Resolve Legacies Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The Sun's steps 2-4 resolve legacies. Each seated player's dormant
legacies become active when their goal is met and return to the deck
otherwise. Each public legacy goes active to the one player who meets its
goal; on a tie between Imperials alone the outgoing Chancellor chooses
among them; otherwise it returns to the deck. Every active legacy that
alters a foundation, held by any lineage, seated or in the box, alters it.
Exiles' active legacies whose goals they miss go dormant. A player with
more than three active legacies discards the excess. Crown Chancellor makes
an Exile winner's newly dormant legacies active again. Step 5, Shuffle,
stays an empty window (ruling 3). The Game Log names every legacy these
steps move.

**Architecture:** `LegacyGoals`, a pure module in `gameplay/chronicle`,
answers which seated players meet a goal. `ResolveLegacies`, beside it,
builds the operations of Sun steps 2-4 as `Branch`es read when each step
is reached, with their notes and decisions; `ChronicleSunProcedure` puts
them in the existing windows. `ChronicleCrown.empireBreaks` flips the
winner's dormant legacies faceup. The presentation layer names every
legacy to every viewer in the Chronicle Phase (R18), so the
lines can name the dormant legacies the Sun reveals.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacies design](../../specs/2026-11/2026-11-13-legacies-design.md),
rulings 2, 3, 6-9, and section "Slice 3: Resolve Legacies".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacies-implementation-rulings.md).
CR "Task 1: The Sun" and "Legacies in Detail" in
`~/projects/bgs-rules/content/rules/oathnf/en-US/p1/rules.yml`;
`docs/rules/chronicle.md`, the Sun, steps 2-5; Chronicle ruling 4 (ties)
in `docs/rules/ambiguities.md`.

## Prerequisite

Slices 1 and 2 have landed (through `cc8ab6c1f`). Check before Task 1:

```bash
grep -q "object LegacyDeal" src/main/scala/distantlands/gameplay/setup/LegacyDeal.scala &&
grep -q "Location.Deck(CardDeck.Legacy)" src/main/scala/distantlands/gameplay/operations/OperationPipeline.scala &&
grep -q "final case class Legacy(id: LegacyId)" src/main/scala/distantlands/model/Decisions.scala &&
echo ready
```

Expected: `ready`.

## Rulings

The plan's open questions were ruled on 2026-11-13 as implementation
rulings R18-R26 ([rulings](../../specs/2026-11/2026-11-13-legacies-implementation-rulings.md)):

- **R18** (Task 2): during the Chronicle Phase every legacy is shown to
  every viewer; strictly in that phase.
- **R19** (Task 6): after a reactivation leaves a player above three
  active legacies, the cap's choice runs for them at once.
- **R20** (Task 6): both Crown Chancellor's and Offer Citizenship's
  reactivations are in this slice.
- **R21** (Tasks 3-5): "turn order" is seat order starting from
  `tracks.roundStarter`. Every per-player order in this slice uses it.
- **R22** (Task 1): a Grand Scepter in play that no player holds is met by
  nobody.
- **R23** (Task 1): "cards of the suit" are denizens and edifices, intact
  or ruined, at the sites the player's side rules, plus their own
  advisers of the suit, faceup or facedown.
- **R24** (Tasks 3-6): the lines as worded below, and a foundation line
  for each `FoundationAltered`.
- **R25** (Task 3): dormant legacies first, in R21's order, in one batch;
  then the public legacies in row order; then the alterations, I to VI.
- **R26** (Task 3): an Imperial tie with no Chancellor seated is refused
  with `InvalidEventOrder`.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`). `ChronicleSunSuite` is at 252 lines;
  the new behavior gets its own suites.
- `model` imports nothing from `gameplay`; `gameplay` never reads a catalog
  `.text` or `rulesText`. It may read a card's `name`, `suit`, `goal`,
  `alters` and a relic's `value`.
- A new test names cards by their card objects (`SeerCard`).
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4%.
- A rewritten golden file is reviewed line by line and committed in the
  task that changes it; every changed line must be one the task explains.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

### Task 1: `LegacyGoals`

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/LegacyGoals.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/LegacyGoalsSuite.scala`

**Interfaces:**
- Produces: `LegacyGoals.meeting(ready: ReadyGame, catalog:
  ExecutableCatalog, legacy: Legacy): Set[PlayerId]`, the seated players
  who meet `legacy`'s goal, read with its own suit for `MostOfSuit`; and
  `LegacyGoals.score(ready, catalog, goal, suit, player): Int` for the
  counted goals, which the tests read; `LegacyGoals.turnOrder(ready):
  Vector[PlayerId]`, seat order from `tracks.roundStarter` (R21).

- [ ] **Step 1: Write the failing tests**

Create `LegacyGoalsSuite` on `ChronicleWalk.chronicleWonByP1` (p1 won;
add the Table steps each test needs). One test per row of the design's
table, each asserting `meeting` for a legacy object of that goal:

- `MostSites` (`IronHandCard`): p2 rules 2 sites, p3 rules 1: `Set(p2)`.
  With p1 Chancellor and p3 Citizen and the Empire ruling 3 sites to p2's
  2: `Set(p1, p3)` (friendly tie: every Imperial is credited with each
  Empire site). With the Empire on 2 and p2 on 2: `Set()` (a tie with the
  Empire includes an Exile). Two Exiles tied on 2: `Set()`.
- A score of 0 never meets (ruling 6): nobody rules a site: `Set()`; bandit
  sites count for nobody.
- `MostOfSuit` (`WealthyCard`, Hearth): the Hearth denizens at sites p2
  rules plus p2's own Hearth advisers, faceup and facedown, beat p3's; an
  edifice of the suit at a ruled site counts (R23); a Vision
  adviser does not; Imperials share the Empire sites' cards.
- `HighestRelic` (`OathsOfLoyaltyCard`): the player holding the relic of
  highest printed `value`, faceup or facedown, meets it; a tie between two
  Exiles meets nobody; holding no relic scores 0.
- `PeoplesFavor` (`MatchmakerCard`) and `DarkestSecret` (`SeerCard`): the
  banner's holder; an unheld banner, `Set()`.
- `GrandScepter` (`KeeperOfOrderCard`): its holder; with no Grand Scepter
  in play, the winner (ruling 7); lying at a site, nobody (R22).
- Only seated players compete: a boxed lineage's goal is never asked.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.LegacyGoalsSuite"`
Expected: FAIL to compile: `Not found: LegacyGoals`.

- [ ] **Step 3: Implement**

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal}
import distantlands.gameplay.operations.TheGrandScepterCard
import distantlands.model._

/** Who meets a legacy's goal in Resolve Legacies (CR "Task 1: The Sun";
  * Legacies design, slice 3). Only seated players compete, and bandits
  * never do: only sites and cards that players rule count. Every Imperial
  * rules each Empire site and its cards. For a counted goal only the
  * highest positive score meets it (ruling 6); a tie between Imperials
  * alone is friendly, and every tied Imperial meets it; a tie that includes
  * any Exile is unfriendly, and nobody meets it (Chronicle ruling 4). */
object LegacyGoals:
  def meeting(ready: ReadyGame, catalog: ExecutableCatalog, legacy: Legacy)
      : Set[PlayerId] = legacy.goal match
    case LegacyGoal.PeoplesFavor =>
      ready.game.current.banners.peoplesFavor.holder.toSet
    case LegacyGoal.DarkestSecret =>
      ready.game.current.banners.darkestSecret.holder.toSet
    case LegacyGoal.GrandScepter => scepter(ready)
    case goal => leaders(ready, ChronicleTasks.seated(ready).map(player =>
      player -> score(ready, catalog, goal, legacy.suit, player)))

  def score(ready: ReadyGame, catalog: ExecutableCatalog, goal: LegacyGoal,
      suit: Suit, player: PlayerId): Int = goal match
    case LegacyGoal.MostSites => ruled(ready, player).size
    case LegacyGoal.MostOfSuit => ... // see below
    case LegacyGoal.HighestRelic => ... // best held relic's printed value
    case _ => 0

  /** The highest positive score's players, unless an Exile is among them. */
  private def leaders(ready: ReadyGame, scores: Vector[(PlayerId, Int)])
      : Set[PlayerId] =
    val best = scores.map(_._2).maxOption.getOrElse(0)
    val tied = scores.collect { case (p, s) if best > 0 && s == best => p }
    if tied.size > 1 && tied.exists(!Imperials.isImperial(ready, _)) then
      Set.empty
    else tied.toSet
```

- `ruled(ready, player)`: the sites in play `ChronicleTasks.rules` says the
  player's side rules (the Empire's for an Imperial, their own for an
  Exile).
- `MostOfSuit`: over `ruled` sites, the site's denizen row counting a
  `DenizenState` whose `catalog.denizen(id)` has `suit` and an
  `EdificeState` whose `catalog.edifice(id)` has `suit`; plus the player's
  `advisers` that are `DenizenState`s of `suit`.
- `HighestRelic`: the maximum `catalog.relic(id).map(_.value)` over the
  player's relics, `TheGrandScepterCard.value` for the Grand Scepter, 0 for
  none.
- `scepter`: the seated player holding `TheGrandScepterCard.id`; if no
  player holds it and no site holds it, `ChronicleTasks.winner(ready)`
  (ruling 7); otherwise `Set.empty` (R22).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.LegacyGoalsSuite"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/LegacyGoals.scala \
  src/test/scala/distantlands/gameplay/chronicle/LegacyGoalsSuite.scala
git commit -m "feat(chronicle): read who meets a legacy's goal

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: The Chronicle names every legacy

**Files:**
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (`identifiesCard`, its doc comment)
- Test: `src/test/scala/distantlands/application/LegacyIdentitySuite.scala`

**Interfaces:**
- Produces: in `Phase.Chronicle`, `identifiesCard` names a lineage's
  legacy to every viewer, dormant or not (R18).

- [ ] **Step 1: Write the failing test**

```scala
  test("in the Chronicle Phase every viewer identifies a dormant legacy"):
    val chronicle = ready.updateCurrent(current => current.copy(turn =
      current.turn.copy(phase = Phase.Chronicle)))
    assert(viewers.forall(presentation.identifiesAt(chronicle, _,
      ArbiterCard.id)))
```

(Import `distantlands.model.Phase`.)

- [ ] **Step 2: Run the test to verify it fails**

Run: `./sbtw "testOnly distantlands.application.LegacyIdentitySuite"`
Expected: FAIL: p2 and the public viewer do not identify Arbiter.

- [ ] **Step 3: Implement**

In the `CardContainer.Lineage(lineage, LineageCardArea.Legacies)` arm, add
`ready.game.current.turn.phase == Phase.Chronicle ||` before the owner
check, and say why in the comment: Resolve Legacies reveals the dormant
faces to check goals, and after it the only dormant legacies are newly
dormant ones, which were active. Extend the doc comment's legacy bullet.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.*"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/application/GamePresentationProjector.scala \
  src/test/scala/distantlands/application/LegacyIdentitySuite.scala
git commit -m "feat(presentation): name every legacy in the Chronicle Phase

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: Sun step 2, Flip Legacies

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/ResolveLegacies.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSunProcedure.scala`
  (`steps`, `noteKeys`, doc comment)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
  (the `ChronicleSun` entry: each new key's `LogKind`)
- Create: `src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt` (rewritten)

**Interfaces:**
- Consumes: Task 1's `LegacyGoals.meeting`.
- Produces: `ResolveLegacies.flip(catalog): Vector[Operation]`, the
  children of the `ChronicleFlipLegacies` window;
  `ResolveLegacies.publicDecisionId(legacy: LegacyId)` =
  `s"chronicle.sun.public-legacy.${legacy.value}"`, owned by the outgoing
  Chancellor, a choose-one over the tied Imperials as `Player` options;
  note keys `activated` ("{0} activated {1}"), `discarded` ("{0} discarded
  {1}"), `tookPublic` ("{0} took {1}"), `returnedPublic` ("{0} returned to
  the legacy deck") and one `altered` key per foundation ("{0} set
  Foundation II to Movers and Shakers", the face name read from
  `FoundationCards.face(number, FoundationFace.Altered).name`, as
  `ShapeFoundation` reads the normal face's).

- [ ] **Step 1: Write the failing tests**

Create `ResolveLegaciesSuite` with `ChronicleSunSuite`'s helpers
(`sunStarted`, `answer`, `sunWithTasksGiven`, `sunEnded`); if they are
needed by both suites, move them to `ChronicleWalk` instead of copying
them. Tests:

- A dormant legacy whose goal its player meets turns active, and one whose
  goal they miss returns to the deck and leaves play.
- A public legacy one player alone meets goes active to them; one nobody
  meets returns to the deck; the row is empty after the step.
- A public legacy tied between the Chancellor and a Citizen parks on
  `publicDecisionId` owned by the Chancellor, offering exactly the tied
  players; the answer gives it to the chosen player, active.
- A public legacy tied with an Exile goes to nobody.
- An active altering legacy (`WealthyCard`) alters Foundation II with
  itself as a source; held by a boxed lineage (baseline only), it alters
  too (ruling 8); a face that already lists it is left alone, so a second
  Chronicle with the same legacy writes no second `FoundationAltered`.
- A lineage in the box keeps its legacies and faces unchanged.
- The lines, in order: activations and discards in turn order, then each
  public legacy's, then the foundation lines.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ResolveLegaciesSuite"`
Expected: FAIL to compile: `Not found: ResolveLegacies`.

- [ ] **Step 3: Implement**

`ResolveLegacies.flip(catalog)` returns three parts, each a `Branch` read
when it is reached (R25):

1. **Dormant legacies.** For each seated player in turn order (R21), each
   dormant legacy of their lineage: `Flip(id, Location.Legacies(lineage),
   FaceUp)` when `LegacyGoals.meeting` holds the player, else
   `Move(Piece.Card(id), Legacies(lineage), Deck(CardDeck.Legacy))`. One
   `BuildOps(required = true)` carries them all, then one `Note` per legacy
   (`activated` or `discarded`, args `Player`, `Card`).
2. **Public legacies.** For each legacy in `commonCards.publicLegacies`, in
   row order, a `Branch` reading `meeting`:
   - one player: `Move(PublicLegacies, Legacies(their lineage),
     resultingOrientation = Some(FaceUp))` and `tookPublic`;
   - several (all Imperial, since `meeting` is empty on an unfriendly tie):
     `Decide(publicDecisionId(id), chancellor, ChooseOne(tied as Player
     options, heading = Some("Choose who takes the public legacy"),
     confirmVerb = Some("Give")))`, then the same move and line for the
     chosen player (`ChronicleTasks.chosenPlayer`); with no Chancellor
     seated, `ChronicleTasks.failing(InvalidEventOrder(...))` (open
     question 9);
   - nobody: `Move(PublicLegacies, Deck(CardDeck.Legacy))` and
     `returnedPublic` (arg `Card`).
3. **Alterations.** Every active legacy of every lineage
   `ChronicleRecord.campaignFields(state).lineages` lists (seated live,
   boxed from the baseline) whose catalog `alters` is `Some(number)` and
   whose number's face does not list it among its alteration sources:
   `FoundationAltered(number, id)`, Foundation I to VI, then one
   `altered(number)` line per operation (arg `Card`).

In `ChronicleSunProcedure.steps`, the `ChronicleFlipLegacies` window takes
`ResolveLegacies.flip(catalog)` (pass `catalog` into `steps`), and
`noteKeys` adds `ResolveLegacies.noteKeys` (declared on the Sun's note id,
as `ImperialMaps` declares Setup's). Update the object's doc comment:
steps 2-4 resolve legacies; step 5 is an empty window (ruling 3).

In `ProcedureLog`'s `ChronicleSun` entry, every step 2 key is
`LogKind.Delta`, as `crowned` is: the Chancellor's tie-break is told by its
`tookPublic` line like any other award.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.*"`
Expected: PASS except `DeclaredNoteKeysSuite` (run it too).
Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
and read the diff: exactly the new `procedure.chronicle-sun` keys.

Run: `./sbtw test`
Expected: PASS. A golden log whose script plays the Sun with legacies
held would change; none does today (`git status --short src/test/resources`
shows only the snapshot).

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle \
  src/main/scala/distantlands/application/gamelog/ProcedureLog.scala \
  src/test/scala src/test/resources/notes
git commit -m "feat(chronicle): flip, discard and award legacies in the Sun

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Sun step 3, Exiles' legacies go dormant

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ResolveLegacies.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSunProcedure.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala`

**Interfaces:**
- Produces: `ResolveLegacies.dormancy(catalog)`, the children of
  `ChronicleExileDormancy`; note key `wentDormant` ("{0}'s {1} went
  dormant").

- [ ] **Step 1: Write the failing tests**

- A seated Exile's active legacy whose goal they miss flips dormant; one
  they meet stays active.
- An Imperial's unmet active legacy stays active.
- No foundation changes (an altering legacy gone dormant leaves its face
  and source).
- The newly-dormant invariant (ruling 2): after the Sun, every dormant
  legacy on a seated lineage was active when the Chronicle began.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: the Exile's legacy stays active.

- [ ] **Step 3: Implement**

A `Branch`: for each seated player whose role is `Exile`, in turn order,
each active legacy of their lineage not met by them:
`Flip(id, Legacies(lineage), FaceDown)`, in one `BuildOps(required =
true)`, then one `wentDormant` line each (`LogKind.Delta`).

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.*"`; then the
snapshot rewrite as in Task 3.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(chronicle): turn an Exile's unmet legacies dormant

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: Sun step 4, the cap

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ResolveLegacies.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSunProcedure.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala`

**Interfaces:**
- Produces: `ResolveLegacies.cap(player: PlayerId): Operation`, a
  `Branch` that asks nothing at three or fewer active legacies;
  `ResolveLegacies.capDecisionId(player)` =
  `s"chronicle.sun.legacy-cap.${player.value}"`, a `ChooseMany` with
  `min = max = n - 3` over the player's active legacies as
  `DecisionOption.Legacy`, `confirm = CountedConfirm("Discard", "legacy",
  "legacies")`; the chosen ones move to the deck with one `capped` line
  each ("{0} discarded {1}", the same words as step 2's `discarded`, but a
  key of its own so that it posts as `LogKind.Decision`). `ResolveLegacies.capAll` runs `cap` for each
  seated player in turn order (R21).

- [ ] **Step 1: Write the failing tests**

- A player with five active legacies is asked to discard exactly two, and
  the two chosen leave play; a player with three is asked nothing.
- Two players over the cap are asked in turn order from the round starter.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: nobody is asked.

- [ ] **Step 3: Implement** as the interface says; the
  `ChronicleLegacyCap` window takes `ResolveLegacies.capAll`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.*"`.

- [ ] **Step 5: Commit**

```bash
git commit -am "feat(chronicle): cap each player at three active legacies

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Newly dormant legacies become active again

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleCrown.scala`
  (`empireBreaks`, doc comment)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSunProcedure.scala`
  (the Crown window: the reactivation line and the cap)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/OfferCitizenship.scala`
  (`joins`, `answer`, doc comment)
- Test: `src/test/scala/distantlands/gameplay/chronicle/ResolveLegaciesSuite.scala`,
  `src/test/scala/distantlands/gameplay/chronicle/OfferCitizenshipSuite.scala`

**Interfaces:**
- Produces: when an Exile wins, Crown Chancellor flips every dormant
  legacy of the winner's lineage faceup (by ruling 2 they are all newly
  dormant), writes `reactivated` ("{0} reactivated {1}", one per legacy),
  then runs `ResolveLegacies.cap(winner)` (R19). An Exile who
  accepts Citizenship does the same in their answer's branch (open
  question 3).

- [ ] **Step 1: Write the failing tests**

- An Exile winner holding a newly dormant legacy (an unmet active one at
  the Sun's start) ends the Sun with it active; a Citizen winner's stays
  dormant (no such legacy exists after step 3 for an Imperial).
- An Exile who accepts Citizenship ends the Throne's first step with
  their newly dormant legacies active; one who declines keeps them
  dormant.
- A reactivation that leaves four active legacies asks the cap decision.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: the legacies stay dormant.

- [ ] **Step 3: Implement**

In `ChronicleCrown.empireBreaks`, before `RoleChanged(player.lineage,
Role.Chancellor)`, add a `Flip(id, Location.Legacies(player.lineage),
FaceUp)` for each dormant legacy of the winner's lineage, and drop "they
arrive with the Legacies slice" from the doc comment. In the Crown window,
after `crowned`, a `Note` per legacy the previous state held dormant and
the new state holds active, and `ResolveLegacies.cap(winner)`.

In `OfferCitizenship.joins`, the same flips before `RoleChanged`; in
`answer`'s accept branch, the `reactivated` lines and
`ResolveLegacies.cap(exile)` after `accepted`.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw test`; rewrite the note-keys snapshot as in Task 3. Golden
logs that end a game where an Exile wins holding no legacy do not change.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle src/test
git commit -m "feat(chronicle): reactivate newly dormant legacies on joining the Empire

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 7: Docs and the gate

**Files:**
- Modify: `docs/rules/chronicle.md` (the Sun, steps 2-5: a "Digital" note
  each, as Shape Foundation has)
- Modify: `docs/rules/implementation-traceability.md` (the "Sun task" row)
- Modify: `docs/rules/ambiguities.md` (the Legacies rulings: the rulings
  this plan's open questions receive)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ShapeFoundation.scala`
  (doc comment: "Before Legacies no face has such a source" no longer
  holds)
- Modify: `docs/superpowers/specs/2026-11/2026-11-13-legacies-design.md`
  (status line)

- [ ] **Step 1: Docs**

- `chronicle.md`: under steps 2-5, the digital reading: who is asked, the
  lines, the tie-break owner, the cap's order, step 5 an empty window
  (ruling 3), and links to the Legacies rulings.
- Traceability "Sun task" row: steps 2-5 move from the gaps to the
  evidence (`LegacyGoalsSuite`, `ResolveLegaciesSuite`); the gap keeps the
  legacy powers.
- `ambiguities.md`: append R18-R26 to the Legacies rulings, numbered on
  from 11, each with its R number.
- Design status: "Slices 1, 2 and 3 landed (plans: ...)".

- [ ] **Step 2: Run the verification gate**

Run: `./sbtw clean coverage test coverageReport` (statements at or above
92%, branches at or above 84.4%), `./sbtw "frontend/test"`,
`python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`; `git status --short
src/test/resources` is clean.

- [ ] **Step 3: Commit**

```bash
git add docs src/main/scala/distantlands/gameplay/chronicle/ShapeFoundation.scala
git commit -m "docs(legacies): record slice 3

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
