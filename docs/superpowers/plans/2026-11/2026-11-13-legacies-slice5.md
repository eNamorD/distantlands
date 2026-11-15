# Legacies, Slice 5: UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Players see legacies. Each player area has a legacy strip: each
legacy as a small card with its name, goal and suit seal; an active one
faceup; a dormant one showing its face marked "Dormant" to its owner and a
legacy back to everyone else (in the Chronicle Phase, to everyone, R18).
Clicking one opens the card inspection with its rules text, goal and suit.
While the Public Ambitions row holds any legacy it shows faceup near the
map. The Setup keep decision, the cap and the Chancellor's tie-break draw
their legacies as cards, with hover and selection working on
`legacy:<id>` keys. The Chronicle page lists each lineage's legacies by
name, active or dormant. The running app is checked by hand.

**Architecture:** The projection carries what the board needs:
`CardDetailsProjection` gains the legacy's goal, `PlayerBoardProjection`
gains `legacies`, and `GameProjection` gains `publicLegacies`; each is
built through `identifiesCard`, so the dormant back follows ruling 1 and
R18 with no new rule. The Chronicle summary gains each lineage's legacies.
The frontend draws the strip and the row with `CardFace`, learns the
`legacy` card kind (R27), and the inspection shows the goal.

**Tech Stack:** Scala 3.9.0 (JVM and Scala.js), munit, `./sbtw`; the
frontend suites run with `./sbtw "frontend/test"`. The running app:
`./sbtw frontend/fastLinkJS`, then
`./sbtw 'runMain distantlands.server.DistantLandsServer --database-path var/distantlands'`
and `http://127.0.0.1:8080/?mode=server` (README, "Local server UI").

**Spec:** [Legacies design](../../specs/2026-11/2026-11-13-legacies-design.md),
section "Slice 5: UI", rulings 1 and 3.
[Implementation rulings](../../specs/2026-11/2026-11-13-legacies-implementation-rulings.md):
R4, R7, R10, R11, R18 and R27.

## Prerequisite

Slices 1 to 4 have landed (through `4c8363506`). Check before Task 1:

```bash
grep -q "def bonus" src/main/scala/distantlands/gameplay/chronicle/LineageScoring.scala &&
grep -q '"legacy" => Some(Legacy' src/main/scala/distantlands/model/Decisions.scala &&
echo ready
```

Expected: `ready`.

## Rulings

The plan's open questions were ruled on 2026-11-13 as implementation
rulings R34-R40 ([rulings](../../specs/2026-11/2026-11-13-legacies-implementation-rulings.md)),
each as the plan recommended: goals in words, icons deferred (R34); the
goal's key and printed words, `LegacyGoal.all` deleted (R35); a dormant
legacy's face with a "Dormant" tag for its owner, and for everyone in the
Chronicle Phase (R36); a "Legacies" row of its own (R37); the "Public
Ambitions" row above the regions (R38); a lineage list on the Chronicle
page (R39); the local `.claude/launch.json` corrected, not committed
(R40).

## Global Constraints

- Scala 3 with `-Werror`, on the JVM and in Scala.js.
- Every Scala file stays at or under 800 lines
  (`scripts/check-architecture.py`). `WorldBoardRenderer.scala` is at 425;
  if the strip and the row would take it past 600, they go in a
  `LegacyStrip.scala` of their own.
- `gameplay` never reads a catalog `.text` or `rulesText`; the presentation
  layer may.
- A new projection field is optional or defaulted in its codec, so an older
  snapshot still decodes (as `foundations` is).
- A frontend test renders from a projection (`ServerModeFixtures`,
  `PlayerBoardSuite`'s style) and never names a printed card by looking its
  name up at run time.
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit, then `git log -3` (another session works in
  this checkout; never stage `docs/operations/data-policy.md`).
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4%.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

---

### Task 1: The goal in the card details (R7, R11)

**Files:**
- Modify: `src/main/scala/distantlands/catalog/LegacyGoal.scala`
  (`printed`; delete `all`)
- Modify: `src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala`
  (`LegacyGoal.values`)
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`
  (`CardDetailsProjection.legacyGoal`)
- Modify: `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala`
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (`cardDetails`' legacy arm)
- Test: `src/test/scala/distantlands/application/LegacyIdentitySuite.scala`,
  the projection codec suite (`git grep -ln "relicValue" -- shared/src/test src/test`)

**Interfaces:**
- Produces: `LegacyGoal.printed: String` ("Rule the most sites", "Hold the
  Grand Scepter. If none, win.", ...); `final case class
  LegacyGoalProjection(key: String, text: String)` and
  `CardDetailsProjection.legacyGoal: Option[LegacyGoalProjection] = None`,
  on the wire `"legacyGoal": {"key": ..., "text": ...}`, absent when none.

- [ ] **Step 1: Write the failing tests**

- `LegacyIdentitySuite`: Seer's card details carry
  `Some(LegacyGoalProjection("darkest-secret", "Hold the Darkest Secret"))`.
- The codec suite: a card with a goal round-trips; a card without one
  writes no `legacyGoal` field and an older card without it decodes.
- `LegacyCardDataSuite`: replace `LegacyGoal.all` with
  `LegacyGoal.values.toVector`, and check each goal's `printed` against the
  design's table.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `value legacyGoal is not a member`.

- [ ] **Step 3: Implement** as the interfaces say; the legacy arm of
  `cardDetails` sets `legacyGoal = Some(LegacyGoalProjection(l.goal.key,
  l.goal.printed))`. `MostOfSuit`'s words read "Rule the most cards of
  this suit (even advisers)"; the card's suit field names the suit.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.catalog.* distantlands.application.*"`
and `./sbtw "frontend/test"`.

- [ ] **Step 5: Commit**, then `git log -3 --oneline`.

---

### Task 2: Legacies in the projection

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala`
  (`PlayerBoardProjection.legacies`)
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala`
  (`GameProjection.publicLegacies`)
- Modify: the two codecs (`WorldProjectionCodec.scala`,
  `GameProjectionCodec.scala`)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (`playerBoards`)
- Modify: `src/main/scala/distantlands/application/GameProjection.scala`
  (the public row)
- Test: `src/test/scala/distantlands/application/LegacyIdentitySuite.scala`

**Interfaces:**
- Produces: `PlayerBoardProjection.legacies: Vector[CardDetailsProjection]
  = Vector.empty`, the lineage's legacies in held order: an identified one
  as `cardDetails(id, Some(face), hidden = false)` (face-up when active,
  face-down when dormant), any other as `hiddenCard("legacy")`;
  `GameProjection.publicLegacies: Vector[CardDetailsProjection] =
  Vector.empty`, faceup.

- [ ] **Step 1: Write the failing tests**

On `Table.start.legacy(p1, SeerCard).legacy(p1, ArbiterCard, active =
false).publicLegacies(RivalCard)`, projected through `GameProjector`:
- p1's board lists Seer face-up and Arbiter face-down, both named.
- p2's view of p1's board lists Seer named and a hidden legacy card, whose
  JSON never holds Arbiter's id.
- The public view lists the same as p2.
- In the Chronicle Phase every view names Arbiter (R18).
- Every view's `publicLegacies` is Rival, faceup.
- The codecs: a board and a game with legacies round-trip, and older JSON
  without the fields decodes.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `value legacies is not a member`.

- [ ] **Step 3: Implement** through `identifiesCard` with the legacy's
  container, `CardContainer.Lineage(lineage, LineageCardArea.Legacies)`,
  and `CardContainer.PublicLegacies` for the row.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.* distantlands.server.*"`
and `./sbtw "frontend/test"`.

- [ ] **Step 5: Commit**, then `git log -3 --oneline`.

---

### Task 3: The frontend knows a legacy card (R27)

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/BoardOptions.scala`
  (`cardKind`)
- Modify: `frontend/src/main/scala/distantlands/frontend/CardFace.scala`
  (`backLetter`, a dormant tag, the goal tag)
- Modify: `frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala`
  (`details`: the goal)
- Modify: the stylesheet the card faces use
  (`git grep -ln "card-back-letter" -- frontend src/main/resources`)
- Test: `frontend/src/test/scala/distantlands/frontend/CardFaceSuite.scala`,
  `CardInspectionOverlaySuite.scala`, `BoardOptionsRenderSuite.scala` or
  `CardChoicePanelSuite.scala`

**Interfaces:**
- Produces: `BoardOptions.cardKind` keeps `"legacy"`, so a legacy card's
  key is `legacy:<id>`, the key its decision option has;
  `CardFace.backLetter("legacy")` is `"L"`; a legacy face shows its suit
  glyph, its name and a goal tag (R34); a face-down, identified
  legacy shows its face with a "Dormant" tag (R36), not the
  knowable back; the inspection lists "Goal" with the printed words.

- [ ] **Step 1: Write the failing tests**

- `CardFaceSuite`: a hidden legacy shows "L"; an active legacy shows its
  name and goal tag; an identified dormant one shows its name and
  "Dormant".
- `CardInspectionOverlaySuite`: a legacy's inspection lists Suit and Goal,
  and its rules text.
- The choice panel suite: a choose-one over two legacy options draws both
  as cards; hovering one highlights the elements keyed `legacy:<id>`, and
  choosing it selects that option.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.CardFaceSuite distantlands.frontend.CardInspectionOverlaySuite distantlands.frontend.CardChoicePanelSuite"`
Expected: FAIL: the back reads "D" and the key is `denizen:L22`.

- [ ] **Step 3: Implement**, and the styles for the tags in the stylesheet's
  card-face section.

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "frontend/test"`.

- [ ] **Step 5: Commit**, then `git log -3 --oneline`.

---

### Task 4: The legacy strip and the public row

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
  (`playerBoards`: the strip; `world`: the row), or a new
  `LegacyStrip.scala` beside it if the file would pass 600 lines
- Modify: the stylesheet
- Test: `frontend/src/test/scala/distantlands/frontend/PlayerBoardSuite.scala`,
  a world-pane suite (`git grep -ln "def world\|WorldBoardRenderer.world" -- frontend/src/test`)

**Interfaces:**
- Produces: each player area's "Legacies" row (R37), each card
  drawn by `CardFace.render` and keyed by `BoardOptions.cardKey` when
  identified, so a decision over a legacy highlights it on the board; the
  World pane's "Public Ambitions" row above the regions while
  `publicLegacies` is non-empty (R38).

- [ ] **Step 1: Write the failing tests**

From a projection (the fixtures' style):
- The strip renders p1's two legacies on p1's board, the active one faceup,
  the dormant one with its face and "Dormant" (p1's view), and as an "L"
  back on p2's view.
- No strip renders for a player with no legacy.
- The public row renders its legacies faceup, and is absent when empty.
- Clicking a strip card opens the inspection with its goal.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: no strip.

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "frontend/test"`.

- [ ] **Step 5: Commit**, then `git log -3 --oneline`.

---

### Task 5: The Chronicle page's legacies

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/ChronicleProtocol.scala`
  (`ChronicleScore.legacies`, codec)
- Modify: `src/main/scala/distantlands/application/chronicle/ChronicleEntries.scala`
  (`summary`)
- Modify: `frontend/src/main/scala/distantlands/frontend/ChroniclePage.scala`,
  `ChronicleText.scala`
- Test: the Chronicle entries suite (`git grep -ln "ChronicleEntries.summary" -- src/test`),
  `frontend/src/test/scala/distantlands/frontend/ChroniclePageSuite.scala`,
  `ChronicleTextSuite.scala`

**Interfaces:**
- Produces: `final case class ChronicleLegacy(name: String, active:
  Boolean)` and `ChronicleScore.legacies: Vector[ChronicleLegacy] =
  Vector.empty`, on the wire `"legacies": [{"name": ..., "active": ...}]`,
  absent when empty; the summary reads names from
  `NewFoundations.catalog` (a legacy it does not know keeps its id); the
  page's lineage list (R39), "Red 9 · Seer, Iron Hand
  (dormant)", every face shown (the Chronicle is the owner's own record,
  R4).

- [ ] **Step 1: Write the failing tests**

- Summary: a Chronicle whose lineage holds Seer active and Iron Hand
  dormant summarises both, in held order.
- Codec: the summary round-trips; an entry without `legacies` decodes with
  none.
- `ChronicleTextSuite`: the lineage line's wording, with and without
  legacies.
- `ChroniclePageSuite`: the loaded Chronicle shows the lineage list.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `Not found: ChronicleLegacy`.

- [ ] **Step 3: Implement.**

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.application.*"` and
`./sbtw "frontend/test"`.

- [ ] **Step 5: Commit**, then `git log -3 --oneline`.

---

### Task 6: The running app

No code unless the check finds a fault; a fault gets its own failing test
and fix commit before the check is repeated.

- [ ] **Step 1: Start the app**

`./sbtw frontend/fastLinkJS`, then the server (R40: through
`preview_start` with a corrected `.claude/launch.json`, or the README's
command run in the background), and open
`http://127.0.0.1:8080/?mode=server` in the built-in browser.

- [ ] **Step 2: Walk Setup to the keep decision**

New Chronicle, three players, Create game. As the first player, place the
pawn; the panel asks "Keep a legacy" with two legacy cards. Check, with
screenshots and the page's accessibility tree:
- both options are drawn as legacy cards with name, suit and goal;
- hovering one option highlights it and only it (`data-option-ref` is
  `legacy:<id>`), on the panel and in the player's strip;
- the strip shows both drawn legacies, dormant, with faces, for this
  player; switching seats with the toolbar shows two "L" backs;
- choosing one and confirming keeps it: the strip then shows one dormant
  legacy; the Game Log says "<player> kept a legacy".

- [ ] **Step 3: Inspection**

Click the kept legacy: the overlay shows its name, suit, goal and rules
text; Escape closes it.

- [ ] **Step 4: Record**

Write what was checked, with the screenshots' findings, in the commit
message of the docs task, and stop the server.

---

### Task 7: Docs and the gate

- [ ] **Step 1: Docs**

- `docs/rules/implementation-traceability.md`: the legacy UI's evidence
  (the suites above and the running-app check).
- ROADMAP: a deferred item for the goal icons (R34); the Legacies design's slice 5 no longer pending in the in-play
  batch's text.
- `docs/rules/ambiguities.md`: R34-R39, numbered on from 27.
- The design's status line: "Slices 1 to 5 landed".

- [ ] **Step 2: Run the verification gate**

`./sbtw clean coverage test coverageReport` (statements at or above 92%,
branches at or above 84.4%), `./sbtw "frontend/test"`,
`python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`; `git status --short
src/test/resources` is clean.

- [ ] **Step 3: Commit**, then `git log -3 --oneline`.
