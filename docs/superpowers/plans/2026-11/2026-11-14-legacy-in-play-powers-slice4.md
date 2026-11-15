# Legacy In-Play Powers, Slice 4: Activation and Prediction Implementation Plan

> Executed inline by the implementer.

**Goal:** The last two in-play legacies. The Mouth's holder burns 2 favor
to activate a dormant legacy of their own, or take one from the Public
Ambitions row, whose goal they meet. Conspirator's holder secretly writes
down, at their first Wake, the round the game will end in and who will
win; a correct prediction wins the game instead, even over a Successor.

**Architecture:** The Mouth is a legacy ACTION, a `PaidAction` beside
Golden Key. Its tree asks for the legacy, then activates it in one
batch. It flips an own legacy faceup with `Flip`, or moves a public one
into the lineage faceup with `Move`, as Resolve Legacies does. The same
batch alters the foundation the legacy names with `FoundationAltered`.
Then the tree caps the holder at three active legacies with the public
`ResolveLegacies.cap`. Conspirator's prediction is a forced Wake power
gated to round 1. Its two answers are a `ChooseAmount` for the round and
a `ChooseOne` of players. They are journaled as a new `WritePrediction`
operation onto a new `CurrentGameState.predictions`. The power writes a
public line that names nothing and a line only the holder reads. The
winner question is narrated, so `DetailLines` never posts its answer, and
an amount answer is never posted. At game end the driver starts a new
triggered step, `ConspiratorReveal`, before the Chronicle opens. The step
reveals the prediction in a public line, then journals `ResolvePrediction`.
That operation clears the store, and when the round and the result's
winner (already after the Successor substitution) both match while the
holder still holds Conspirator active, it makes the holder the winner
under a new `VictoryKind.Conspirator`.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy in-play powers design](../../specs/2026-11/2026-11-14-legacy-in-play-powers-design.md),
rulings 1-2 and 23-27 and section "Slice 4: activation and prediction".
[Implementation rulings](../../specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md).
Card text is in `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
(Conspirator L26, The Mouth L34). Resolve Legacies
(`gameplay/chronicle/ResolveLegacies.scala`) and
`gameplay/chronicle/LegacyGoals.scala` define activation and goals. The
Chronicle rulings on legacy activation are in
`docs/rules/ambiguities.md#legacy-chronicle-powers-rulings`.

## Prerequisite

Slice 3 has landed (through `8a6942b8c`). Check before Task 1:

```bash
grep -q "object TheMouthCard" src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala &&
grep -q "object ConspiratorCard" src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala &&
grep -q "object ArbiterStep" src/main/scala/distantlands/gameplay/phases/rest/Arbiter.scala &&
echo ready
```

Expected: `ready`.

## Rulings

This plan's open questions were ruled on 2026-11-14 as implementation
rulings R24-R30 ([rulings](../../specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md)).
Each took the recommended option but R25:

- **R24** (Task 4, re-ruled by the owner after the slice landed): the
  prediction is compared at the Sun's first window, right after Royal
  Line's step, against the winner it leaves. The first ruling, comparing
  it before the Chronicle, is superseded; see Deviations.
- **R25** (Task 1): The Mouth may activate Iron Hand for an Imperial
  holder; the same batch then discards it, with Iron Hand's discard line.
- **R26** (Task 3): the holder sees the prediction through the holder-only
  line alone.
- **R27** (Tasks 1-4): the lines as worded in the open questions below.
- **R28** (Task 3): the round is an amount; a test shows that neither
  `DetailLines` nor any other viewer's projection or log exposes the
  amount or the winner before the game ends.
- **R29** (Task 4): a void prediction reveals with the same line.
- **R30** (Task 1): with nothing to activate, The Mouth shows unavailable
  with "No legacy whose goal you meet".

For the weak spots: if no arrangement reaches round 5 cleanly, the
golden's table starts at a later round with a prediction stored by the
operation (a deviation); The Mouth's alteration test may use Arbiter,
which names Foundation VI.

Every table is a named fixture with concrete sites and pieces in the test
itself.

## Plan choices

- **The Mouth's targets** (ruling 23) are the holder's own dormant
  legacies, then the Public Ambitions row (`commonCards.publicLegacies`),
  each kept when `LegacyGoals.meeting(ready, catalog, card)` contains the
  holder. The Mouth itself is never a target, since its holder's Mouth is
  active. With no target the ACTION is shown unavailable with the reason
  "No legacy whose goal you meet" (R4's pattern, through `refusal`).
- **One tree shape.** The question's options are the live targets plus
  the legacy already answered, so a walk that parks later (at the cap)
  and is rebuilt finds its answer still offered. This is the lesson of
  R20. The activation and the foundation alteration are one `BuildOps`
  batch. Its line reads the step it just ran, and no park can come
  between them, because nothing triggers on a legacy or a foundation
  operation. The cap comes last and reads live state when reached, as it
  does in the Sun.
- **The foundation alteration** happens only when the legacy names a
  foundation (`catalog.legacy(id).flatMap(_.alters)`) and the foundation
  does not already list it as a source. The guard is the one
  `ResolveLegacies.alterations` uses, and `foundationAltered` refuses a
  repeated source. It changes `campaign.foundations` only: the banners
  keep their faces, as in the Sun.
- **Iron Hand** (R25): when the activated legacy is Iron Hand and the
  holder is Imperial, the activation batch ends with
  `IronHand.inGameDiscard`, and Iron Hand's line ("p1 discarded Iron Hand")
  follows; it then counts for no cap.
- **The cap** reuses `ResolveLegacies.cap(holder, TheMouth.capDecisionId(holder),
  TheMouth.id, TheMouth.capped)` with The Mouth's own decision id and
  line, as `OfferCitizenship` does. It flips no foundation back
  (ruling 24).
- **Conspirator's prediction** is a forced Wake `PhasePower`, usable when
  three things hold: the round is 1, the player holds Conspirator active
  (`ChronicleLegacyRules.holder`), and the player has no prediction
  stored. Every seat's first Wake is in round 1, so a Conspirator gained
  later, by The Mouth, is never asked (ruling 25). The prediction is
  stored only after both answers, since a forced power must not change
  what is due before its last decision (`ForcedWakeProcedure`'s doc). A
  Quest to Distant Lands holder's first Wake places the pawn first,
  because the driver runs `QuestFirstWake` before the Wake checks and the
  forced powers. That ordering needs no driver change.
- **Secrecy** (ruling 26):
  - The round is a `ChooseAmount`, which `DetailLines` never posts.
  - The winner question is in `narratedDecisions`, so no "chose" line
    names it.
  - The walk's decision and its answered options are projected only to
    the awaited holder. The public "waiting" projection shows only the
    heading, which names neither choice.
  - The power writes "p2 wrote down a prediction with Conspirator" for
    everyone, and "p2 predicted p3 wins in round 6" read only by p2
    (`LogNote.readBy`, Heirloom's pattern). A posted line never changes
    (prefix stability), so the reveal is a third line at the end.
  - The stored prediction is never projected. The dev events endpoint
    stays the privileged, unredacted view it is.
- **Resolution** (ruling 27) is a triggered procedure,
  `TriggeredProcedureRef.ConspiratorReveal` ("game-end-conspirator").
  - `OathDriver.openChronicle` starts it before `ChronicleBegin` when a
    result is set, the phase is not Chronicle, no walk is pending, and a
    prediction is stored.
  - It has no `Decide`. It writes the reveal line, then, when the
    prediction holds, the victory line, then journals
    `ResolvePrediction(holder, fulfilled)`.
  - At that point `tracks.round` is the round the game ended in, and
    `result.winner` is already after the Successor substitution
    (`EmpireEndings.chancellorWins`). A holder who no longer holds
    Conspirator active is void, and the prediction is still revealed.
  - `ResolvePrediction` removes the store, so the driver then opens the
    Chronicle for the new winner. Royal Line acts only on an Empire win,
    so a Conspirator win switches it off.
- **The new victory kind** `Conspirator` ("conspirator") is posted by the
  reveal step's own line: "p2 won with Conspirator". It is not posted by
  an event's victory line, since no event sets it. `GameLogFormatter.wonBy`
  and the frontend's `VictoryWords` gain its words.
- The Mouth's card moves to `gameplay/powers/legacies/TheMouth.scala` and
  Conspirator's to `gameplay/powers/legacies/Conspirator.scala`.
  `HoldingLegacies.scala` is then empty and is deleted, and the
  `catalog.holding._` imports go.

## Deviations from the plan

- Slice 3's review fixes landed first, each its own commit: the
  Challenge's line reads the holder at walk time; The Needle's tests for a
  banner dealt at game start, its pinned options, both banners in one
  raid and an off-turn holder; Iron Hand's resume test; the decode
  refusals.
- The Mouth's alteration test uses Beloved (Foundation IV): Arbiter names
  no foundation. Its lines are `used` and `used.public`, since every
  non-forced phase power declares a used line (`LogNoteCatalogSuite`).
  Iron Hand's mid-game discard line is a new `discarded` key on Iron
  Hand's own notes. Its relic goal in the tests is met with Sticky Fire,
  since the Grand Scepter's printed value is 0.
- Only `HoldingLegacies.scala` is deleted; the holding package keeps its
  other cards, so `NewFoundations` keeps its import.
- The reveal line names Conspirator in words: a Conspirator gone to the
  deck, or dormant, is a card no other viewer may identify, and the line
  names it either way. The victory line keeps the card.
- "Round 2 asks nothing" stands for a Conspirator The Mouth activates
  after round 1: the gate is the round. The secrecy suite checks every
  other viewer's projection and log through the service; the holder's
  answered options are not separately checked.
- The `legacy-prediction` golden reaches round 5 by an `AdvanceRound`
  arrangement in p3's Act, as planned, and lands with Task 4, since
  `GameLogPropertiesSuite` needs a script completing `game-end-conspirator`.
  The holder's private line shows in neither golden seat (p1, p2).
- `ConspiratorReveal` lives in `gameplay/chronicle/legacies`, beside
  Royal Line, not in `gameplay/powers/legacies`: `ProcedureNotes`, in
  `gameplay/actions`, names it and may not import `gameplay/powers`.
- The coverage build's size limit also caught `insertCard`, whose
  `CurrentGameState` copies grew with `predictions`; its current-game piles
  moved to a helper.
- From the whole-feature review: the cap's decision id is the player-free
  `power.the-mouth.cap`, not `power.the-mouth.cap.<player>`, since a
  power narrates its decisions by exact id and only the walk's actor
  answers it; narrated, it writes no "Chose" line.
- `GameLogFormatter.wonBy` gains its `Conspirator` words, which no event
  posts: the reveal step writes the victory line.

- After the slice landed the owner re-ruled R24: the comparison moved from
  a triggered step before the Chronicle (`TriggeredProcedureRef.ConspiratorReveal`,
  "game-end-conspirator", now withdrawn; no released stream used it) to
  the Sun's steps, right after the `ChronicleStartOfSun` window where
  Royal Line acts. Its lines are now the Sun's.

## Global Constraints

- Scala 3 with `-Werror`. Every Scala file stays at or under 800 lines.
  `model` imports nothing from `gameplay`, and `gameplay/actions` never
  imports `gameplay/powers`.
- A new test names cards by their card objects and builds its table as a
  named fixture in the test.
- Only seated players' active legacies act (ruling 1).
- No `Branch`'s children or ids depend on state the walk itself changes.
  Every new `Decide` has a resume test, and no two decisions share an id
  in one walk.
- Old saved streams still decode and replay. `WritePrediction` and
  `ResolvePrediction` are new wire kinds, `TriggeredProcedureRef.ConspiratorReveal`
  is a new key, and `VictoryKind.Conspirator` is a new key.
- A prediction never reaches another viewer before the game ends: not in
  a projection, a log line, a decision's heading, or a "chose" line.
- TDD, one commit per task. Coverage stays at or above 92% statements and
  84.4% branches. A rewritten snapshot or golden is reviewed line by line
  and committed in its task. Regenerate the snapshots with
  `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write`. Keep
  `ProcedureLog.rules` under the coverage tool's size limit by adding new
  cases as named vals, as slice 3's fix did.
- A card that leaves holding updates the code that names it:
  - `NewFoundations`, through its `catalog.holding._` import
  - `PhasePowerSuite` (`TheMouthCard`)
  - `LegacyCardDataSuite` (both cards)
- Commit messages follow Conventional Commits and end with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Use explicit
  pathspecs, and run `git log -3` after each commit.

## The gate

```bash
./sbtw clean coverage test coverageReport
./sbtw "frontend/test"
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git status --short src/test/resources
```

Expected: every suite passes, coverage is at or above 92% statements and
84.4% branches, both scripts pass, and the last command prints nothing.
The Mouth's question and Conspirator's two questions are checked in the
running app, for the holder and for another seat.

---

### Task 1: The Mouth

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/TheMouth.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/legacies/LegacyActionPowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (remove `TheMouthCard`)
- Modify: `src/test/scala/distantlands/gameplay/PhasePowerSuite.scala`, `src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala` (imports)
- Test: `src/test/scala/distantlands/gameplay/powers/legacies/TheMouthSuite.scala`
- Snapshots: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt`

**Interfaces:**
- Consumes: `ResolveLegacies.cap(player, decisionId, notesOf, cappedKey)`,
  `LegacyGoals.meeting(ready, catalog, legacy): Set[PlayerId]`,
  `Flip`, `Move`, `FoundationAltered`.
- Produces:
  - `TheMouthCard` (L34, moved unchanged)
  - `case object TheMouth extends PaidAction(TheMouthCard.power)`, with:
    - `decisionId = "power.the-mouth.legacy"`
    - `capDecisionId(player): String = s"power.the-mouth.cap.${player.value}"`
    - `targets(catalog, ready, player): Vector[LegacyId]`
    - note keys `activated`, `tookPublic`, `altered`, `capped`

- [ ] **Step 1: Write the failing tests**

`TheMouthSuite`, on named tables:

```scala
/** The Mouth (L34): "ACTION: Burn 2 favor to activate a dormant legacy if
  * you meet its goal. You can even activate legacies in the Public
  * Ambitions foundation." (Legacy in-play powers rulings 23-24.) */
class TheMouthSuite extends munit.FunSuite:
  import PhasePowerHarness._
  private val source = DecisionOptionRef.Legacy(TheMouthCard.id)

  /** p1, the Chancellor in their Act at Ancient City with 2 favor, holds
    * The Mouth active, Ruthless dormant and the Darkest Secret, which
    * meets Arbiter's goal; Arbiter is p1's other dormant legacy. Herbalist
    * stands at Ancient City for Ruthless. */
  private def mouthHolder: Table = Table.start.imperial(p1)
    .turn(p1, Phase.Act).pawn(p1, at = AncientCitySite).favor(p1, 2)
    .legacy(p1, TheMouthCard).legacy(p1, ArbiterCard, active = false)
    .legacy(p1, RuthlessCard, active = false)
    .darkestSecret(Some(p1), secrets = 1)
    .denizen(HerbalistCard, AncientCitySite)
```

The tests:

```scala
  test("it offers the holder's dormant legacies whose goal they meet, and " +
      "the Public Ambitions legacies, and pays 2 burnt favor")
    // Ruthless's goal is checked as LegacyGoals.meeting reads it. The
    // Public Ambitions row holds Golden Key (goal met by p1) and Seer
    // (not met); offered: Arbiter (own, Darkest Secret goal) and Golden
    // Key, not Seer and not The Mouth. p1's favor drops by 2 to the bank
    // that burnt favor goes to.

  test("an own dormant legacy flips faceup and is in effect at once: " +
      "the activated Ruthless's ACTION is usable that turn")

  test("a Public Ambitions legacy moves faceup into the lineage, and the " +
      "row no longer holds it")

  test("a legacy that alters a foundation alters it now, and the line " +
      "says so")
    // The table's Public Ambitions holds a legacy with `alters` (one of
    // LegacyCardDataSuite's six MostOfSuit legacies); after activation
    // campaign.foundations(n).alterationSources contains it.

  test("with four active legacies the holder discards to three, choosing, " +
      "and no foundation flips back")

  test("with no legacy whose goal they meet it is shown unavailable with " +
      "its reason (R4)")

  test("a dormant Mouth offers nothing (ruling 1)")

  test("an Imperial holder may activate Iron Hand, which the same batch " +
      "discards, with Iron Hand's line (R25)")

  test("a walk resumed at the cap keeps the chosen legacy offered and the " +
      "activation's line, and the journal replays")
    // Parks on TheMouth.capDecisionId(p1); Journal.evolved from the
    // events, then the cap answered: one Flip, one activated line.

  test("the lines name the legacy: \"p1 activated Arbiter with The Mouth\", " +
      "\"p1 took Golden Key from the Public Ambitions with The Mouth\"")
```

Each test body uses `PhasePowerHarness.use(ready, TheMouth, source)`,
`answer(..., TheMouth.decisionId, ChooseOneAnswer(DecisionOptionRef.Legacy(id)))`,
`after`, `replayed` and `NoteText.said(TheMouth, events)`, as
`GoldenKeySuite` does.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.TheMouthSuite"`
Expected: a compile error, because `TheMouth` does not exist.

- [ ] **Step 3: Implement**

```scala
case object TheMouth extends PaidAction(TheMouthCard.power):
  val decisionId: String = "power.the-mouth.legacy"
  def capDecisionId(player: PlayerId): String =
    s"power.the-mouth.cap.${player.value}"
  private val notes = NoteSet(id)
  /** "{p1} activated {Arbiter} with {The Mouth}". */
  val activated: NoteKey = notes.key("activated", ...)
  /** "{p1} took {Golden Key} from the Public Ambitions with {The Mouth}". */
  val tookPublic: NoteKey = notes.key("took-public", ...)
  /** "{Arbiter} set Foundation {VI} to {Festival}". */
  val altered: NoteKey = notes.key("altered", ...)
  /** "{p1} discarded {Seer} with {The Mouth}". */
  val capped: NoteKey = notes.key("capped", ...)
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The holder's dormant legacies, then the Public Ambitions row, whose
    * goal `player` meets now (ruling 23). */
  def targets(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId): Vector[LegacyId] = ...

  override def refusal(...): Option[String] =
    Option.when(targets(catalog, ready, player).isEmpty)(
      "No legacy whose goal you meet")

  def build(catalog, ready, player, source) = Right(Sequence(Vector(
    // The answered legacy stays offered after it is activated, so a walk
    // rebuilt at the cap finds it (R20's lesson).
    Branch((live, pending) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseOne(offered(catalog, live, player, pending)
        .map(id => DecisionOption.Legacy(DecisionOptionRef.Legacy(id))),
        heading = Some("The Mouth: activate a legacy whose goal you meet"),
        confirmVerb = Some("Activate"))))),
    BuildOps((live, pending) => activation(catalog, live, player, pending),
      required = true),
    Note(id, states => activationNote(states, player)),
    Note(id, states => alterationNote(states)),
    ResolveLegacies.cap(player, capDecisionId(player), id, capped))))
```

`activation` returns either `Flip(id, Location.Legacies(lineage),
Orientation.FaceUp)` for an own legacy or `Move(Piece.Card(id),
PositionedLocation(Location.PublicLegacies),
PositionedLocation(Location.Legacies(lineage)), resultingOrientation =
Some(Orientation.FaceUp))` for a public one. It adds `FoundationAltered(n, id)`
when `catalog.legacy(id).flatMap(_.alters)` is `Some(n)` and
`campaign.foundations.get(n)` does not already list `id`. The answered
legacy must be among the live targets, else it fails with
`PowerAnswers.missing(decisionId)`. The two notes read `states.previous`:
the legacy that became active, and the foundation whose
`alterationSources` grew.

Move `TheMouthCard` into the file, register `TheMouth` in
`LegacyActionPowers.all`, and update the two test imports.

- [ ] **Step 4: Run them and see them pass**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.* distantlands.catalog.* distantlands.application.*"`
Expected: everything passes. `declared-keys.txt` gains The Mouth's four
lines, and `unimplemented.txt` loses `legacy.the-mouth`.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/legacies/TheMouth.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/TheMouthSuite.scala
git commit -m "feat(legacies): The Mouth activates a dormant or public legacy whose goal its holder meets

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/legacies \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/TheMouthSuite.scala \
  src/test/scala/distantlands/gameplay/PhasePowerSuite.scala \
  src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt
git log -3
```

### Task 2: The prediction store and the Conspirator victory kind

**Files:**
- Modify: `src/main/scala/distantlands/model/GameState.scala`
  (`final case class Prediction(round: Int, winner: PlayerId)`,
  `CurrentGameState.predictions: Map[PlayerId, Prediction] = Map.empty`,
  `VictoryKind.Conspirator extends VictoryKind("conspirator")`)
- Modify: `src/main/scala/distantlands/model/TurnOperation.scala`
  (`WritePrediction`, `ResolvePrediction`)
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`,
  `TurnStateOperations.scala`
- Modify: `src/main/scala/distantlands/serialization/TurnOperationCodec.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala` (`wonBy`)
- Modify: `frontend/src/main/scala/distantlands/frontend/VictoryWords.scala`
- Test: `src/test/scala/distantlands/gameplay/PredictionOperationsSuite.scala`,
  `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`,
  the frontend's VictoryWords test if one exists, else
  `frontend/src/test/scala/distantlands/frontend/VictoryWordsSuite.scala`

**Interfaces:**
- Produces:
  - `WritePrediction(holder: PlayerId, round: Int, winner: PlayerId) extends TurnOperation, LeafOperation`.
    It requires a round from 1 to 8 and refuses a holder with a stored
    prediction or a winner who is not seated.
  - `ResolvePrediction(holder: PlayerId, fulfilled: Boolean) extends TurnOperation, LeafOperation`.
    It removes the holder's prediction. When `fulfilled`, it sets
    `result = Some(GameResult(holder, VictoryKind.Conspirator))` and
    refuses a game with no result.
  - Wire kinds `"write-prediction"` (`holder`, `round`, `winner`) and
    `"resolve-prediction"` (`holder`, `fulfilled`).

- [ ] **Step 1: Write the failing tests.** `PredictionOperationsSuite`, on
  `Table.start` with p1 seated:
  - writing stores the prediction;
  - a second write by the same holder is refused;
  - round 0 or 9 is refused, and so is an unseated winner;
  - a fulfilled resolution with a result set makes p1 the winner by
    `Conspirator` and empties the store;
  - an unfulfilled one only empties it;
  - a fulfilled one with no result is refused.

  `WalkerOperationWireSuite.everyOperation` gains
  `WritePrediction(p1, 6, p2)`, `ResolvePrediction(p1, true)` and
  `ResolvePrediction(p1, false)`. The `GameLogHeadlineSuite`-style test
  "a Conspirator result reads 'p2 won with Conspirator'" checks
  `wonBy`'s words through a victory line built for the kind. The frontend
  check is `VictoryWords.of(Some("conspirator")) == "Conspirator's
  prediction"`.
- [ ] **Step 2: Run them and see them fail** (compile errors).
- [ ] **Step 3: Implement** as above. `TurnStateOperations.writePrediction`
  and `resolvePrediction` return `Either[OperationError, ReadyGame]` with
  `InvalidDescription` refusals, as `trackPawn` does.
- [ ] **Step 4: Run** `./sbtw "testOnly distantlands.gameplay.* distantlands.serialization.* distantlands.application.*"`
  and `./sbtw "frontend/test"`. Everything passes, with no golden change.
- [ ] **Step 5: Commit** `feat(legacies): a journaled prediction store and the Conspirator victory kind`.

### Task 3: Conspirator's prediction at the first Wake

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/Conspirator.scala`
  (`ConspiratorCard`, moved, and `case object ConspiratorPrediction extends PhasePower`)
- Modify: `src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala`
  (registered beside `Hunger`)
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (deleted, now empty), `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
  and `src/test/scala/distantlands/catalog/LegacyCardDataSuite.scala` (imports)
- Test: `src/test/scala/distantlands/gameplay/powers/legacies/ConspiratorPredictionSuite.scala`,
  `src/test/scala/distantlands/application/ConspiratorSecrecySuite.scala`

**Interfaces:**
- Produces:
  - `ConspiratorPrediction.roundDecisionId = "power.conspirator.round"`
  - `ConspiratorPrediction.winnerDecisionId = "power.conspirator.winner"`
  - note keys `wrote` (public) and `predicted` (read by the holder)

- [ ] **Step 1: Write the failing tests.**

  `ConspiratorPredictionSuite`, driven through the rules from the end of
  Setup or from `Table.start` at round 1 in p2's Wake:
  - "p2, holding Conspirator active, is asked at their round-1 Wake for a
    round from 1 to 8, then a winner among the seated players, themselves
    included". The first question is a `ChooseAmount(1, 8, ...)`, the
    second a `ChooseOne` of three players.
  - "after both answers `predictions(p2)` holds them, and the Wake goes
    on".
  - "a walk resumed between the questions, and after them, stores one
    prediction" (`Journal.evolved` at each park).
  - "round 2's Wake asks nothing: a Conspirator activated after round 1
    by The Mouth never asks" (ruling 25). The Mouth activates p2's
    dormant Conspirator in round 1's Act; round 2's Wake parks on no
    Conspirator question.
  - "a dormant Conspirator asks nothing" (ruling 1).
  - "a Quest to Distant Lands holder places their pawn, then writes"
    (`QuestFirstWakeSuite`'s Chronicle with Quest and Conspirator both
    active: the placement parks first, then the round).

  `ConspiratorSecrecySuite`, through the service:
  - "another seat's projection shows only whom it waits on, and its
    heading names no choice". `projectPublic` and p1's `project` give
    `walkerDecision == None` and a `walkerWaiting` with p2.
  - "every other viewer's log reads only 'p2 wrote down a prediction with
    Conspirator'; p2's also reads 'p2 predicted p3 wins in round 6'". Use
    the formatter with `Some(p1)` and `Some(p2)`, and check that no "chose"
    line exists for either decision.
  - "the holder's own projection shows their answered options while the
    second question waits".
- [ ] **Step 2: Run them and see them fail.**
- [ ] **Step 3: Implement.**

```scala
case object ConspiratorPrediction extends PhasePower:
  val id: PowerId = ConspiratorCard.power.id
  def timing: PowerTiming = PowerTiming.Wake
  override def forced: Boolean = true
  override def narratedDecisions: Set[String] =
    Set(roundDecisionId, winnerDecisionId)

  /** Round 1, Conspirator held active, no prediction yet (rulings 1, 25).
    * The store is written after the last question, so this stays true
    * while the questions wait. */
  def usable(catalog, ready, player, source): Boolean =
    ready.game.current.tracks.round == 1 &&
      ChronicleLegacyRules.holder(ready, ConspiratorCard.id).contains(player) &&
      !ready.game.current.predictions.contains(player)

  def build(catalog, ready, player, source) = Right(Sequence(Vector(
    Decide(roundDecisionId, player, DecisionQuery.ChooseAmount(1, 8,
      Some("Conspirator: secretly write down the round the game ends in"),
      "Write down")),
    Decide(winnerDecisionId, player, DecisionQuery.ChooseOne(
      ChronicleTasks.playerOptions(seated(ready)),
      heading = Some("Conspirator: secretly write down who will win"),
      confirmVerb = Some("Write down"))),
    BuildOps((_, pending) => for
      round <- PowerAnswers.amount(pending, roundDecisionId)
        .toRight(PowerAnswers.missing(roundDecisionId))
      winner <- ChronicleTasks.chosenPlayer(pending, winnerDecisionId)
    yield Vector(WritePrediction(player, round, winner)), required = true),
    Note(id, _ => Some(wrote.sourceless(NoteArg.Player(player),
      NoteArg.Card(ConspiratorCard.id)))),
    Note(id, states => states.now.game.current.predictions.get(player)
      .map(p => predicted.sourceless(NoteArg.Player(player),
        NoteArg.Player(p.winner), NoteArg.Number(p.round))
        .readBy(Vector(player)))))))
```

  The seated players are read from `ready` when the tree is built, a set
  the walk does not change. `ForcedWakeProcedure` wraps the tree in an
  `AutomaticUse` with the legacy card. Move `ConspiratorCard`, delete the
  empty `HoldingLegacies.scala`, and drop the `catalog.holding._` imports.
  The `ProcedureLog` ForcedWake entry stays `Silent`. The power's notes
  post through `NoteWordings`, as Hunger's do.
- [ ] **Step 4: Run** `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.* distantlands.application.* distantlands.catalog.*"`.
  The snapshots gain Conspirator's two lines and lose `legacy.conspirator`.
  The `GameLogPropertiesSuite` "every decision a script asks is told" test
  passes, since both ids are narrated.
- [ ] **Step 5: Commit** `feat(legacies): Conspirator's holder secretly writes down a prediction at their first Wake`.

### Task 4: The prediction at the game's end

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/ConspiratorReveal.scala`
  (`object ConspiratorReveal` with `due`, `build`, `noteKeys`)
- Modify: `src/main/scala/distantlands/model/ProcedureRef.scala`
  (`TriggeredProcedureRef.ConspiratorReveal`, key `"game-end-conspirator"`, in `all`)
- Modify: `src/main/scala/distantlands/gameplay/OathDriver.scala` (`openChronicle`)
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala`,
  `gameplay/actions/ProcedureNotes.scala`, `application/gamelog/ProcedureLog.scala`
  (a named val: `revealed` as a Decision line, `won` as a Victory line)
- Test: `src/test/scala/distantlands/gameplay/powers/legacies/ConspiratorRevealSuite.scala`,
  `src/test/scala/distantlands/model/EnumWireKeySuite.scala`, `ProcedureRefSuite.scala`,
  `GameLogPropertiesSuite`'s noting list

**Interfaces:**
- Produces:
  - `ConspiratorReveal.due(ready): Option[PlayerId]`: the holder of the
    stored prediction, when a result is set and the phase is not
    Chronicle.
  - `ConspiratorReveal.fulfilled(ready, holder): Boolean`: the round
    matches `tracks.round`, the winner matches `result.winner`, and the
    holder still holds Conspirator active.
  - Lines:
    - `revealed`: "p2 had written down p3 to win in round 6 with
      Conspirator"
    - `won`: "p2 won with Conspirator"

- [ ] **Step 1: Write the failing tests**, through the service on the
  round-end port as `ArbiterSuite` does:
  - "a correct prediction beats a Successor win" (ruling 27). In round 5,
    p1 is the Chancellor and holds the Oathkeeper title of Devotion and
    the Darkest Secret. p2, a Citizen, holds the Grand Scepter, the
    Devotion Successor. p3, an Exile in the round's last Act, holds
    Conspirator active and a stored prediction (p2, round 5). The end die
    rolls 6: the event's result is p2's Successor win, and then the
    result becomes `GameResult(p3, Conspirator)`. The log reads the
    roll, "p2 won as Successor to the Chancellor", the reveal, then "p3
    won with Conspirator". The Start Chronicle decision parks for p3.
  - "a wrong round changes nothing, and the prediction is still revealed".
  - "a wrong winner changes nothing".
  - "a Conspirator lost to The Mouth's cap is void" (ruling 27). p3 is
    arranged as no longer holding Conspirator, with the prediction
    stored: revealed, result unchanged.
  - "the game's end replays, and the store is empty after it".
  - "with no prediction the Chronicle opens as before" (any existing
    end-of-game test unchanged).
- [ ] **Step 2: Run them and see them fail.**
- [ ] **Step 3: Implement.** `build` is
  `Sequence(Note(revealed), Note(won when fulfilled), BuildOps(ResolvePrediction(holder, fulfilled), required = true))`,
  all computed from `state` at build. The step has no question, so its
  shape never meets a resume. `OathDriver.openChronicle`:

```scala
  private def openChronicle(transition: OathTransition) =
    transition.state match
      case Ready(ready) if ConspiratorReveal.due(ready).nonEmpty &&
          ChronicleSequence.opening(ready.game.current).nonEmpty =>
        startTriggered(transition, TriggeredProcedureRef.ConspiratorReveal)
      case Ready(ready) =>
        ChronicleSequence.opening(ready.game.current)
          .fold(Right(transition))(startTriggered(transition, _))
      case _ => Right(transition)
```

  When the step ends, the driver's next pass finds the store empty and
  opens the Chronicle for the result now set.
- [ ] **Step 4: Run** `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.* distantlands.application.* distantlands.model.*"`.
  Everything passes. `GameLogPropertiesSuite`'s procedure coverage waits
  for Task 5's script, so commit Task 4 with Task 5's golden script
  added, as slice 3's Task 4 did with `legacy-triggers`.
- [ ] **Step 5: Commit** `feat(legacies): a correct Conspirator prediction wins the game instead, revealed at its end`.

### Task 5: The golden log, the docs and the gate

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/LegacyActionLogScripts.scala`
  (a `legacy-prediction` script)
- Create: the `legacy-prediction` golden pair
- Modify: `docs/rules/ambiguities.md`, `implementation-traceability.md`,
  `rules-reference.md`, the design's status line

- [ ] **Step 1: The script.** `legacyPrediction` starts on `Table.start` in
  round 1 with the round-end port rolling 6:
  - p1, the Chancellor, holds the Oathkeeper title of Devotion and the
    Darkest Secret.
  - p2, a Citizen, holds the Grand Scepter.
  - p3, an Exile at Broken Peaks, is in their Wake as the round's last
    seat. They hold Conspirator and The Mouth active, The Standard Bearer
    dormant, the People's Favor (which meets The Standard Bearer's goal)
    and 2 favor.
  - p3's forced Wake writes round 5 and p2 (`withAnswers`).
  - In p3's Act, The Mouth activates The Standard Bearer. That is a SETUP
    power, so it does nothing now.
  - An arrangement (`Step.Arrange(Vector.fill(4)(AdvanceRound))`) moves
    the marker to round 5.
  - p3 begins Rest. The round ends, the end die rolls 6, and p2 wins as
    Successor. Then the reveal runs, and p3 wins with Conspirator.

  Expected lines for every other seat:
  - "p3 wrote down a prediction with Conspirator"
  - "p3 activated The Standard Bearer with The Mouth"
  - the roll and p2's Successor win
  - "p3 had written down p2 to win in round 5 with Conspirator"
  - "p3 won with Conspirator"

  p3's log also reads "p3 predicted p2 wins in round 5". The Chronicle
  runs on by default answers, as `legacy-triggers` does.
- [ ] **Step 2: Generate and review the golden**:
  `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite -- *legacy-prediction*"`.
  Every other golden is unchanged.
- [ ] **Step 3: Docs.**
  - `ambiguities.md`: the design's rulings 23-27 and this slice's
    implementation rulings (R22 and R23 onward), numbered on.
  - `implementation-traceability.md`: a row "Legacy activation and
    prediction powers", and the Throne row's Royal Line note on a
    Conspirator win.
  - `rules-reference.md`: "Digital:" notes under Minor actions (The
    Mouth), Wake (Conspirator's first Wake) and Empire endings (the
    override).
  - The design's status line: slice 4 has landed.
- [ ] **Step 4: Run the gate** (top of this plan), and check in the
  running app The Mouth's question and Conspirator's two questions, as
  the holder and as another seat.
- [ ] **Step 5: Commit** the script, the goldens and the docs, with
  explicit pathspecs.

## Self-review notes

- Spec rulings:
  - Ruling 23: Task 1's targets and refusal.
  - Ruling 24: Task 1's activation, alteration and cap.
  - Ruling 25: Task 3's gate and the Quest order.
  - Ruling 26: Task 3's secrecy.
  - Ruling 27: Tasks 2 and 4.
  - Rulings 1-2: the dormant cases.
- The slice 4 test list:
  - The Mouth activating its holder's dormant legacy and a Public
    Ambitions legacy, the activated power usable that turn, a foundation
    altered, the cap discard, and not offered when no goal is met
    (Task 1).
  - Conspirator's prediction redacted for every other viewer and in the
    log (Task 3).
  - A correct prediction beating a Successor win, a wrong round or winner
    changing nothing, and a Conspirator lost to the cap voiding it
    (Task 4).
  - A Quest holder placing first, and a Conspirator gained by The Mouth
    never asking (Task 3).
  - A resume test for each decision (Tasks 1 and 3).
  - A golden log through the game's end (Task 5).
- Names across tasks: `TheMouth.decisionId`, `.capDecisionId`,
  `.targets`; `Prediction`, `CurrentGameState.predictions`,
  `WritePrediction`, `ResolvePrediction`, `VictoryKind.Conspirator`;
  `ConspiratorPrediction.roundDecisionId`, `.winnerDecisionId`;
  `ConspiratorReveal.due`, `.fulfilled`;
  `TriggeredProcedureRef.ConspiratorReveal`.
- Weak spots, to settle at execution:
  - Whether an `AdvanceRound` arrangement moves the round in one Act
    without other bookkeeping. If not, the golden script starts at round
    5 and keeps the round-1 prediction in its own suite.
  - Which MostOfSuit legacy's goal Task 1's alteration test can meet on
    a plain table.

## Open questions

1. **Royal Line and the "final winner."** Royal Line names a winner in
   the Sun, after the game's end.
   - (a) Compare the prediction at the game's end, before the Chronicle,
     with the result after Successor. Royal Line then acts only on an
     Empire win, so a Conspirator win stays.
   - (b) Compare after Royal Line's choice.

   Recommend (a): the card predicts who wins the game, and Royal Line is
   a CHRONICLE power, acting after the game ended.
2. **The Mouth and Iron Hand.** An Imperial holder may meet Iron Hand's
   goal, but the card says "If you are Imperial, discard Iron Hand".
   - (a) Iron Hand is no target for an Imperial holder: the burn would
     only discard it.
   - (b) Activate it, then discard it in the same batch with Iron Hand's
     line.
   - (c) Activate it, and leave it to the Chronicle's checkpoints.

   Recommend (a).
3. **How the holder sees the prediction.**
   - (a) The private log line only.
   - (b) Also a holder-only note on the Conspirator card in the
     projection, as the Tracker's pawn is shown (a frontend change).

   Recommend (a), since the log keeps the line for the whole game.
4. **The lines' wording.**
   - "p2 wrote down a prediction with Conspirator" (the design's)
   - "p2 predicted p3 wins in round 6" (holder only)
   - "p2 had written down p3 to win in round 6 with Conspirator" (the
     reveal)
   - "p2 won with Conspirator" (the victory)
   - The frontend's winner banner reads "Conspirator's prediction".
   - The Mouth's lines are "p1 activated Arbiter with The Mouth", "p1 took
     Golden Key from the Public Ambitions with The Mouth", "Arbiter set
     Foundation VI to Festival" and "p1 discarded Seer with The Mouth".

   Recommend these.
5. **The round as an amount.**
   - (a) A `ChooseAmount` from 1 to 8, which no log line ever names.
   - (b) Eight buttons, each narrated.

   Recommend (a).
6. **A void prediction.** The design reveals the prediction "either way".
   - (a) The same reveal line, with no word that it was void.
   - (b) Add "Conspirator was no longer held".

   Recommend (a).
7. **The Mouth with nothing to activate.**
   - (a) The ACTION is shown unavailable with "No legacy whose goal you
     meet", as R4 shows every legacy ACTION.
   - (b) It is left out of the menu.

   Recommend (a). The design's "not offered" is read as R4 reads it.
