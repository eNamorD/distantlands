# Legacy Chronicle Powers, Slice 3: Relics and the Next Game Implementation Plan

> Executed inline by the implementer.

**Goal:** Four legacies act on relics, advisers and newcomers, some into
the next game. Heirloom's holder, before Scatter Relics, secretly keeps one
relic, which starts the next game with them facedown. The Ear's holder
chooses which scattered relic goes in each empty slot. Oaths of Loyalty's
holder keeps up to three faceup advisers as the next game's facedown
starting advisers, and every other starting adviser they would gain is
buried. The Bright Beacon's holder, resolving the Beacon, may take six
newcomers from the Dispossessed instead of the suited box. The four cards
count as implemented (R7 for The Ear, whose ACTION half stays uncovered).

**Architecture:** The seam of slices 1 and 2 carries on. Heirloom is an
added step: a `Transform` on a new, empty window before Scatter Relics,
journaling a new `ChronicleOperation`, `StartingRelicKept`, which moves the
relic into the lineage's `startingRelics`. The others change steps, each
asking `ChronicleLegacyRules` when reached: Scatter Relics reads
`scatterer`, Gather Denizens reads `keepsAdvisers`, the three starting
adviser keeps read `buriesStartingAdvisers`, `SetupDeal.of` reads the same
rule from the stored Chronicle, and Introduce Newcomers reads
`beaconFromDispossessed`. Hidden cards are shown to the holder alone by
`Peek`, which records knowledge, and two new `identifiesCard` cases (set
aside relics, the Dispossessed) read it.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Chronicle powers design](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md),
rulings 1-4 and 13-17, and section "Slice 3: relics and the next game".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-implementation-rulings.md)
R1-R16 (R7: a card with an unbuilt half leaves holding whole). Card text in
`src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (The Ear
L19, Heirloom L20, The Bright Beacon L24, Oaths of Loyalty L25).
`docs/rules/chronicle.md`, the Throne steps 4-5, the World steps 2-3 and
the Beacon steps 1-2; Chronicle rulings 15 and 16; Legacies R2 (a boxed
lineage's starting relics). Slice 2's plan for the seam.

## Prerequisite

Slice 2 has landed (through `31c681d8a`). Check before Task 1:

```bash
grep -q "object Reformer" src/main/scala/distantlands/gameplay/chronicle/legacies/Reformer.scala &&
grep -q "def builder" src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala &&
grep -q "ChronicleStartOfThrone" src/main/scala/distantlands/model/PowerWindow.scala &&
echo ready
```

Expected: `ready`.

## Open questions

These change behaviour, data, saved formats or the public shape of the
code and wait for the orchestrator's rulings (R17 onward). The tasks are
written to each recommendation.

1. **Heirloom held by the Throne holder.** Heirloom's step comes before
   Scatter Relics, as the card and ruling 13 say, so after Stock Reliquary,
   which has already put the Throne holder's relics, but the Grand
   Scepter, into the Reliquary. A Throne holder's Heirloom then keeps
   nothing. Options: (a) as the card says: the window sits between Stock
   Reliquary and Scatter Relics; (b) Heirloom's step before Stock
   Reliquary, so the winner may keep one too. Recommendation: (a), the
   printed timing; the winner already scores and takes the Throne.
2. **Heirloom's choice.** The card says "secretly choose one of your
   relics", with no "may" (ruling 3's decline is for "you may"). Options:
   (a) a choose-one over the holder's relics but the Grand Scepter, no
   Decline; a single relic is kept without a question, as Legacies R9
   keeps a single drawn legacy; none asks nothing; (b) always asked, with
   Decline. Recommendation: (a).
3. **Heirloom's lines.** All held relics were revealed in Sun step 1, so
   every viewer identified the relic at the Throne's start, and a card
   argument would name it to all. `LogNote.readers` can show a note to a
   set of players but cannot hide it from one player and show it to
   spectators. Options: (a) one line for every viewer, "p2 kept a relic
   with Heirloom" (no card argument), and one line for the holder alone,
   "p2 kept Cup of Plenty with Heirloom"; the holder reads both; (b) the
   naming line for the holder and "kept a relic" for every other seated
   player, so spectators read neither; (c) a new note feature that hides a
   note from named players. Recommendation: (a), which keeps every viewer's
   log complete with the private-line mechanism catalog batch 12 already
   uses.
4. **How the kept relic is journaled.** Ruling 13 asks for a new
   `ChronicleOperation`. Proposed: `StartingRelicKept(player: PlayerId,
   relic: RelicId)`, wire kind `"starting-relic-kept"` with fields
   `"player"` and `"relic"`: the relic leaves the player's relics and joins
   their lineage's `startingRelics`, outside play and outside the card
   index (Legacies R2), as game start then hands it back facedown. It
   refuses the Grand Scepter, a relic the player does not hold, and a relic
   with tokens; the walk first returns the relic's secrets to its holder,
   facedown, as `Discard.Relic` does. Recommendation: as proposed.
5. **The Ear's choice.** Options for each empty slot, taken one at a time
   in the step's Hinterward order (ruling 14) and read when reached:
   (a) a choose-one over the relics still set aside, no Decline (the card
   has no "may"); the last relic for a slot is placed without a question
   when only one remains; one decision id,
   `chronicle.throne.legacy.the-ear.<player>`, reused per slot and read
   from the latest answer, as Reformer's are (R13); each relic placed is
   peeked for the holder at its site, so they still know it there;
   (b) one partition of all the relics among the slots. Recommendation:
   (a), ruling 14's "slot by slot". The step's heading names the slot's
   site. Line: one, "p2 chose where the relics went with The Ear", after
   the last placement (a decision line); the generic move lines show
   facedown relics to everyone else.
6. **How hidden cards reach their holder alone.** The Ear's relics lie in
   the set-aside pile; the Beacon's in the Dispossessed and the newly
   Dispossessed; `identifiesCard` names none of them to anyone
   (`case _ => false`). Options: (a) the step journals a `Peek` of each
   card for the holder (which records `knowledge.heldRelics` for a relic
   and `knowledge.advisers` for a denizen), and `identifiesCard` gains
   `SetAsideRelics` and `Dispossessed`/`NewlyDispossessed` cases that read
   that knowledge; nobody else's projection changes, and replay derives
   the same knowledge; (b) presentation rules keyed on the legacy holder
   and the step, as the Reliquary's Setup case is. Recommendation: (a):
   knowledge follows journaled facts, and a decision is projected to its
   owner alone anyway.
7. **Oaths of Loyalty in Gather Denizens.** Proposed: before the gathering,
   a `ChooseMany(0, min(3, n))` over the holder's faceup denizen advisers,
   asked when they have one; id
   `chronicle.world.legacy.oaths-of-loyalty.<player>`; each chosen
   adviser's tokens return as Gather Denizens returns every adviser's, and
   the card moves to `Location.StartingAdvisers(lineage)` facedown, as
   Protect and Dispossess's keep does; the rest gather as before. Line:
   "p2 kept Village Idiot and Map Library with Oaths of Loyalty" (the
   cards were faceup, so every viewer names them), a decision line.
   Recommendation: as proposed.
8. **Oaths' burial of other starting advisers** (ruling 16). Options:
   (a) the holder still answers the keep choice in Protect and Dispossess,
   Welcome a Stranger and the next Setup's hand of three, and the kept
   card is buried at the bottom of the world deck instead (the
   "would gain" is the choice); the line becomes "p2 buried Fae Merchant
   with Oaths of Loyalty", the card argument naming it only to viewers who
   identify it, as the keep line does today; (b) no keep question for the
   holder in the Chronicle steps, where all three go to the world deck
   bottom anyway, and the question only in Setup, where the other two go
   to a discard pile. Recommendation: (a), one rule in three places.
   Under Teeming World, `SetupDeal.of` deals an Oaths holder no draw at
   all (ruling 16), read from the stored Chronicle's active legacies.
9. **The Bright Beacon's choices.** Proposed: when the Beacon holder holds
   The Bright Beacon active, a first choose-one, "Suited box" (first) or
   "The Dispossessed", id
   `chronicle.beacon.legacy.the-bright-beacon.<player>`; the box runs the
   ordinary step. The Dispossessed: a `Peek` of each card for the holder,
   then a `ChooseMany(6, 6)` over the older Dispossessed then the newly
   Dispossessed, denizens only (Welcome a Stranger keeps a denizen), id
   `...<player>.cards`; when exactly six remain they are taken without a
   question (a choose-many of all its options is ill-formed). The six move
   to the holder's temporary hand and Welcome a Stranger runs on them.
   Line: "p3 took six newcomers from the Dispossessed with The Bright
   Beacon" (a decision line). Recommendation: as proposed.
10. **The new window.** `PowerWindow.ChronicleBeforeScatterRelics`
    (`chronicle.throne.before-scatter-relics`), between Stock Reliquary
    and Scatter Relics. Recommendation: as proposed.
11. **The golden log "into the next game's setup".** Each game is its own
    event stream, so one golden cannot span the boundary. Options: (a) two
    goldens: the Chronicle where Oaths and Heirloom act, and the next
    game's Setup started from that Chronicle's journaled record;
    (b) one golden of the Chronicle only. Recommendation: (a).

## Global Constraints

- Scala 3 with `-Werror`; every Scala file at or under 800 lines.
- `model` imports nothing from `gameplay`; `gameplay` never reads a
  catalog `.text` or `rulesText`.
- A new test names cards by their card objects.
- Every power reads its holder and role when its window is reached; a
  `Transform` appends a node of constant shape; a `Repeat`'s passes reuse
  their ids and read the latest answer.
- No two decisions share an id; each new id is
  `chronicle.<task>.legacy.<power>.<player>`, a suffix after it for a
  second question of the same power.
- Hidden information: Heirloom's relic, The Ear's examined relics and the
  Beacon's Dispossessed reach their holder alone; each task has a test
  that another seat and the public view do not name them.
- Old saved streams still decode: `StartingRelicKept` is a new wire kind.
- R15: inserted windows and wrapped steps shift a walk parked
  mid-Chronicle by an earlier build.
- TDD, one commit per task; coverage at or above 92% statements and 84.4%
  branches; snapshots and goldens reviewed and committed in their task;
  `git status --short src/test/resources` otherwise clean.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; stage only
  this plan's files; `git log -3` after each commit.

---

### Task 1: Heirloom

**Files:**
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala`
  (`StartingRelicKept`)
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`,
  `ChronicleStateOperations.scala` (`startingRelicKept`)
- Modify: `src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala`
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
  (`ChronicleBeforeScatterRelics`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWindows.scala`,
  `ChronicleThroneProcedure.scala` (the empty window)
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/Heirloom.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `HeirloomCard`)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Test: `src/test/scala/distantlands/gameplay/operations/ChronicleStateOperationsSuite.scala`,
  `src/test/scala/distantlands/serialization/ChronicleOperationWireSuite.scala`,
  `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`,
  `src/test/scala/distantlands/gameplay/chronicle/ChronicleWindowsSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/HeirloomSuite.scala`
- Modify (imports): tests importing `HeirloomCard` from holding
  (`LineageScoringSuite`, `LegacyCardDataSuite`, and any `grep -rln`
  lists)
- Modify: the two snapshots (rewritten)

**Interfaces:**
- Produces: `StartingRelicKept(player: PlayerId, relic: RelicId)`;
  `PowerWindow.ChronicleBeforeScatterRelics`, after
  `ChronicleStockReliquary` in `ChronicleWindows.throne`; `HeirloomCard`;
  `Heirloom` (power); `Heirloom.decisionId(player)` =
  `s"chronicle.throne.legacy.heirloom.${player.value}"`; notes
  `Heirloom.keptPublic` ("{0} kept a relic with {1}") and
  `Heirloom.keptPrivate` ("{0} kept {1} with {2}", read by the holder).

- [ ] **Step 1: Write the failing tests**

`ChronicleStateOperationsSuite`:

```scala
  test("a kept relic leaves its holder for the lineage's starting relics"):
    val ready = Table.start.relic(p1, CupOfPlentyCard).ready
    val after = accepted(run(ready, StartingRelicKept(p1, CupOfPlentyCard.id)))
    assert(!after.game.current.player(p1).get.relics
      .exists(_.id == CupOfPlentyCard.id))
    assertEquals(after.game.campaign.lineages(lineageOf(ready, p1))
      .startingRelics, Vector(CupOfPlentyCard.id))
    assert(after.game.cardIndex.toOption.get.get(CupOfPlentyCard.id).isEmpty)

  test("the Grand Scepter, a relic not held and a relic with tokens are " +
      "not kept"):
    val ready = Table.start.relic(p1, CupOfPlentyCard).ready
    rejected(run(ready, StartingRelicKept(p2, CupOfPlentyCard.id)))
    rejected(run(Table.start.relic(p1, TheGrandScepterCard).ready,
      StartingRelicKept(p1, TheGrandScepterCard.id)))
    rejected(run(Table.start.relic(p1, CupOfPlentyCard)
      .tokens(CupOfPlentyCard, secrets = 1).ready,
      StartingRelicKept(p1, CupOfPlentyCard.id)))
```

(If `Table.tokens` sets only site cards, set the relic's tokens through
`update` on the player's `relics`.)

`ChronicleOperationWireSuite`: `StartingRelicKept(PlayerId("red"),
RelicId("R07"))` round-trips as `{"kind": "starting-relic-kept",
"player": "red", "relic": "R07"}`. `WalkerOperationWireSuite.everyOperation`
gains `StartingRelicKept(PlayerId("red"), RelicId("R07"))`.
`ChronicleWindowsSuite`: the Throne's keys gain
`"chronicle.throne.before-scatter-relics"` after
`"chronicle.throne.stock-reliquary"`.

`HeirloomSuite` (package `distantlands.gameplay.chronicle.legacies`), on
`ChronicleWalk.throneHeldByChancellorP1` (p1 holds the Throne; p2 an
Exile), Throne runs through `ChronicleWalk.through(..., after =
ChronicleSun)` with `WalkerDiceFixture.reversingShuffle`:

```scala
  test("Heirloom is implemented"):
    assert(PowerImplementationStatus.implemented(HeirloomCard.power.id))

  test("the holder keeps one relic of their choice, never the Grand " +
      "Scepter, before Scatter Relics (ruling 13)"):
    val table = throne.relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .relic(p2, TheGrandScepterCard).legacy(p2, HeirloomCard)
    val run = through(table, keeping(GrandMaskCard.id))
    val asked = run.asked.find(_.decisionId == Heirloom.decisionId(p2)).get
    assertEquals(asked.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref).toSet, Set[DecisionOptionRef](
        DecisionOptionRef.Relic(CupOfPlentyCard.id),
        DecisionOptionRef.Relic(GrandMaskCard.id)))
    assertEquals(lineage(run.ready, p2).startingRelics,
      Vector(GrandMaskCard.id))
    // Scatter Relics took the rest; the Scepter stays with p2.
    assertEquals(run.ready.game.current.player(p2).get.relics.map(_.id),
      Vector(TheGrandScepterCard.id))

  test("a single relic is kept without a question, and none asks nothing " +
      "(R17 recommendation)"):
    val one = through(throne.relic(p2, CupOfPlentyCard)
      .legacy(p2, HeirloomCard), PartialFunction.empty)
    assert(!one.asked.exists(_.decisionId == Heirloom.decisionId(p2)))
    assertEquals(lineage(one.ready, p2).startingRelics,
      Vector(CupOfPlentyCard.id))
    val none = through(throne.legacy(p2, HeirloomCard), PartialFunction.empty)
    assertEquals(lineage(none.ready, p2).startingRelics, Vector.empty)

  test("a Throne holder's relics are already in the Reliquary, so their " +
      "Heirloom keeps nothing (open question 1)"):
    val run = through(throne.relic(p1, CupOfPlentyCard)
      .legacy(p1, HeirloomCard), PartialFunction.empty)
    assertEquals(lineage(run.ready, p1).startingRelics, Vector.empty)

  test("a kept relic's secrets return to its holder first"):
    // Set the relic's secrets through `update` on p2's relics.
    ...
```

For the last test, set `RelicState(CupOfPlentyCard.id, FaceUp,
Tokens(0, 2))` in p2's relics with `update`, and assert p2 holds 2 more
facedown secrets after the Throne and the startingRelics hold the cup.

The lines and their redaction, through `NoteLines` in a gamelog test
(`ChronicleLegacyLogSuite`), on a script in `ChronicleLogScripts`
(`throneHeirloom`) where p2 holds Heirloom and two relics and keeps Cup of
Plenty:

```scala
  test("Heirloom's relic is named to its holder alone (ruling 13)"):
    val script = ChronicleLogScripts.throneHeirloom
    val public = s"${p2.value} kept a relic with ${HeirloomCard.name}"
    val named = s"${p2.value} kept ${CupOfPlentyCard.name} with " +
      HeirloomCard.name
    assertEquals(format(script, Some(p2)).map(text).filter(line =>
      line == public || line == named), Vector(public, named))
    Vector(Some(p1), Some(p3), None).foreach(viewer =>
      assertEquals(format(script, viewer).map(text).filter(line =>
        line == public || line == named), Vector(public), viewer))
```

And the next game, in `HeirloomSuite`:

```scala
  test("the kept relic starts the next game with its holder, facedown"):
    val run = ChronicleWalk.through(rules, ChronicleWalk.chronicleWonByP1
      .relic(p2, CupOfPlentyCard).relic(p2, GrandMaskCard)
      .darkestSecret(Some(p2), 1).legacy(p2, HeirloomCard),
      TriggeredProcedureRef.ChronicleFinish, keeping(CupOfPlentyCard.id))
    val record = run.ready.game.campaign.evolved.get
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, record),
      Right(()))
    val next = accepted(GameStartRules.evolve(catalog, record,
      LaterGameFixture.orders(record, p1)))
    val seat = next.game.current.players.find(player =>
      player.lineage == lineage(run.ready, p2).id).get
    assert(seat.relics.contains(RelicState(CupOfPlentyCard.id,
      Orientation.FaceDown, Tokens.empty)))
```

(p2 holds the Darkest Secret so Heirloom, a Darkest Secret legacy, stays
active through the Sun.)

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.HeirloomSuite distantlands.gameplay.operations.ChronicleStateOperationsSuite distantlands.serialization.ChronicleOperationWireSuite"`
Expected: FAIL to compile: `Not found: StartingRelicKept`.

- [ ] **Step 3: Implement**

The operation (model), its application and its codec as `LegaciesScored`'s
were in slice 1:

```scala
/** Keeps a held relic for the next game (Heirloom; Legacy Chronicle powers
  * design, ruling 13): it leaves the player's relics for their lineage's
  * `startingRelics`, outside play and the card index (Legacies R2), and
  * game start hands it back facedown. The Grand Scepter, a relic the
  * player does not hold and a relic with tokens are refused. */
final case class StartingRelicKept(player: PlayerId, relic: RelicId)
    extends ChronicleOperation, LeafOperation
```

`ChronicleStateOperations.startingRelicKept(ready, player, relic)` checks
the three refusals (`TheGrandScepterCard.id`, the player's `relics`, an
empty `tokens`), then removes the `RelicState` and appends the id to the
lineage's `startingRelics`.

`Heirloom.scala`: move `HeirloomCard` verbatim, then:

```scala
/** Heirloom (legacy L20): "Before Scatter Relics, secretly choose one of
  * your relics to keep facedown. You will start the next game with it."
  * Its step is the window before Scatter Relics (Legacy Chronicle powers
  * design, ruling 13), after Stock Reliquary. The holder, read when the
  * step is reached, chooses one of their relics but the Grand Scepter; a
  * single one is kept without a question. Its secrets return to the
  * holder facedown, then `StartingRelicKept` keeps it. Lines: "p2 kept a
  * relic with Heirloom" for every viewer, and "p2 kept Cup of Plenty with
  * Heirloom" for the holder alone. */
case object Heirloom extends ContributingPower:
  val id: PowerId = HeirloomCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleBeforeScatterRelics -> Vector(Transform(
      (_, ops) => ops :+ step)))

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.legacy.heirloom.${player.value}"

  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val keptPublic: NoteKey = notes.key("heirloom-kept", Vector(
    NotePart.Arg(0), NotePart.Text(" kept a relic with "), NotePart.Arg(1)))
  val keptPrivate: NoteKey = notes.key("heirloom-kept-named", Vector(
    NotePart.Arg(0), NotePart.Text(" kept "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def throneNoteKeys: Vector[NoteKey] = notes.declared

  val step: Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, HeirloomCard.id).toVector.flatMap(
      holder => keepable(state, holder) match
        case Vector() => Vector.empty
        case Vector(relic) => kept(holder, relic.id)
        case relics => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseOne(
            relics.map(relic =>
              DecisionOption.Relic(DecisionOptionRef.Relic(relic.id))),
            heading = Some("Heirloom: keep a relic for the next game"),
            confirmVerb = Some("Keep"))),
          Branch((_, pending) => PowerAnswers.one(pending, decisionId(holder))
            .collect { case DecisionOptionRef.Relic(relic) => relic }
            .toVector.flatMap(kept(holder, _))))))

  private def keepable(state: ReadyGame, holder: PlayerId): Vector[RelicState] =
    state.game.current.player(holder).toVector.flatMap(_.relics)
      .filterNot(_.id == TheGrandScepterCard.id)

  /** The relic's secrets back to `holder`, facedown, then the keep and its
    * two lines. */
  private def kept(holder: PlayerId, relic: RelicId): Vector[Operation] =
    Vector(BuildOps((live, _) => Right(returned(live, holder, relic) :+
        StartingRelicKept(holder, relic)), required = true),
      Note(noteId, _ => Some(keptPublic.sourceless(NoteArg.Player(holder),
        NoteArg.Card(HeirloomCard.id)))),
      Note(noteId, _ => Some(keptPrivate.sourceless(NoteArg.Player(holder),
        NoteArg.Card(relic), NoteArg.Card(HeirloomCard.id))
        .readBy(Vector(holder)))))
```

with `returned` the `Move(Piece.Secrets(n), OnCard(relic), PlayArea(holder))`
and `FlipSecrets(holder, n, FaceUp, FaceDown)` pair Gather Denizens uses
for an adviser's secrets, empty when the relic holds none.

The window, `LegacyChroniclePowers` (`all`, `throneNoteKeys`) and
`ProcedureLog` (both keys `LogKind.Decision`) as in slice 2.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

Run the chronicle, catalog, operations, serialization and gamelog suites;
rewrite the unimplemented and note-keys snapshots (diffs:
`legacy.heirloom` removed; `heirloom-kept` and `heirloom-kept-named`
added). Run `./sbtw test`: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala src/test/scala src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt
git commit -m "feat(legacies): Heirloom keeps a relic for the next game

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

(`git add src/main/scala src/test/scala` stages only this task's files;
check `git status --short` first and add file by file if another
session's changes are present.)

---

### Task 2: The Ear

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/TheEar.scala`
- Modify: `LegacyChroniclePowers.scala`, `HoldingLegacies.scala` (remove
  `TheEarCard`), `ProcedureLog.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
  (`scatterer`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ReliquaryAndRelics.scala`
  (`scatter`, doc comment)
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (`identifiesCard`: `SetAsideRelics`)
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/TheEarSuite.scala`
- Test: `src/test/scala/distantlands/application/LegacyIdentitySuite.scala`
  (or a new `HiddenChronicleCardsSuite` if that suite nears its limit)
- Modify: the two snapshots

**Interfaces:**
- Produces: `TheEarCard` (whole text, R7); `TheEar` (power, no window);
  `ChronicleLegacyRules.scatterer(state): Option[PlayerId]`;
  `TheEar.decisionId(player)` =
  `s"chronicle.throne.legacy.the-ear.${player.value}"`; note
  `TheEar.placed` ("{0} chose where the relics went with {1}");
  `ReliquaryAndRelics.emptySlots(catalog, state): Vector[SiteId]`, one
  entry per empty relic slot in Hinterward order (the existing `deal`'s
  `slots`, made a named helper).

- [ ] **Step 1: Write the failing tests**

`TheEarSuite`, on `throneHeldByChancellorP1` with p2 holding The Ear and
two relics, and the Empire at Ancient City (no relic slots): the relics
set aside are p2's two plus any the Reliquary discards; the empty slots
are those of Broken Peaks and Buried Giant (two each), in map order.

```scala
  test("The Ear is implemented"):
    assert(PowerImplementationStatus.implemented(TheEarCard.power.id))

  test("the holder chooses the relic for each empty slot in Hinterward " +
      "order, and the rest return to the deck (ruling 14)"):
    val run = through(earTable, placing(GrandMaskCard.id, CupOfPlentyCard.id))
    val asked = run.asked.filter(_.decisionId == TheEar.decisionId(p2))
    assert(asked.nonEmpty && asked.forall(_.owner == p2))
    val slots = ReliquaryAndRelics.emptySlots(catalog, earTable.ready)
    assertEquals(relicsAt(run.ready, slots.head).head, GrandMaskCard.id)
    assert(run.ready.game.current.setAsideRelics.isEmpty)

  test("each choice offers only the relics still set aside"):
    val run = through(earTable, placing(GrandMaskCard.id, CupOfPlentyCard.id))
    val second = run.asked.filter(_.decisionId == TheEar.decisionId(p2))(1)
    assert(!second.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .exists(_.ref == DecisionOptionRef.Relic(GrandMaskCard.id)))

  test("a single relic left is placed without a question"):
    // Two relics set aside and two empty slots: one question.
    ...

  test("without The Ear the deal is the shuffled one, as before"):
    // The same table with The Ear dormant: no question, the slots filled.
    ...

  test("a walk resumed at the second slot's question after the first " +
      "placement finds the same options (R13)"):
    // Park on the second question, rebuild from the replayed journal,
    // and compare, as ReformerSuite's mid-pass test does.
    ...
```

Write the three sketched tests in full when writing the suite, each from
`earTable` with the named answers; the resume test follows
`ReformerSuite`'s "parked mid-pass" test line for line.

Visibility, in the application tests:

```scala
  test("a set-aside relic is named to a viewer who peeked it, and to no " +
      "one else (open question 6)"):
    val ready = Table.start.update(r => r.updateCurrent(c =>
      c.copy(setAsideRelics = Vector(GrandMaskCard.id)))) ...
```

Build the state with Grand Mask set aside (through `Table` or a
`Move` from a player's relics by `RawOperationBatch`), apply
`Peek(p2, GrandMaskCard.id, Location.SetAsideRelics)`, and assert
`identifiesAt(ready, Some(p2), GrandMaskCard.id)` and not for `Some(p1)`
or `None`; and that the parked The Ear decision is not projected to p1 or
the public view (`GameProjector.project` for those viewers shows no
`walkerDecision` with The Ear's id).

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `Not found: TheEarCard` in the new package.

- [ ] **Step 3: Implement**

`ChronicleLegacyRules.scatterer(state) = holder(state, TheEarCard.id)`.

`ReliquaryAndRelics.scatter`: after the existing discard and `Shuffle`,
a `Branch` reading `scatterer`: with no holder, the existing `deal`;
with one, a `BuildOps` of one `Peek(holder, relic,
Location.SetAsideRelics)` per set-aside relic, then

```scala
      Repeat((live, _) => live.game.current.setAsideRelics.nonEmpty &&
          emptySlots(catalog, live).nonEmpty,
        Sequence(Vector(
          Branch((live, _) => TheEar.ask(catalog, live, holder)),
          Branch((live, pending) => TheEar.place(catalog, live, holder,
            pending))))),
      Note(... TheEar.placed ...),
      BuildOps((live, _) => Right(deal(catalog, live)), required = true)
```

where `TheEar.ask` asks the choose-one over the set-aside relics for the
first empty slot, heading `s"The Ear: choose the relic for ${siteName}"`,
or nothing when one relic remains; and `TheEar.place` moves the latest
answer (or the only relic) facedown to that slot's site and peeks it there
for the holder (`Peek(holder, relic, Location.Site(site))`), once per pass
(a pass places when the count of placements made, read from the site
relics, is below the count of answers plus forced placements; follow
Reformer's `owed` pattern, counting the decision's answers). The final
`deal` returns what is left to the deck, since every slot is full.

`identifiesCard` gains:

```scala
      // A set-aside relic a player peeked: The Ear's holder examines the
      // scattered relics (Legacy Chronicle powers design, ruling 14).
      case CardContainer.SetAsideRelics =>
        viewer.exists(player => ready.knowledge.heldRelics
          .getOrElse(player, Vector.empty).contains(id))
```

`TheEar.scala`: move `TheEarCard` verbatim (whole text, R7), then the
power with no window, its ids, its note, `ask` and `place`.
`LegacyChroniclePowers`, `ProcedureLog` (`placed`, `LogKind.Decision`) as
before.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

As in Task 1 (diffs: `legacy.the-ear` removed; `the-ear-placed` added).
`./sbtw test`: PASS; no golden changes (no script holds The Ear).

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): The Ear chooses where the scattered relics go

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: Oaths of Loyalty in the Chronicle

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/OathsOfLoyalty.scala`
- Modify: `LegacyChroniclePowers.scala`, `HoldingLegacies.scala` (remove
  `OathsOfLoyaltyCard`), `ProcedureLog.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
  (`keepsAdvisers`, `buriesStartingAdvisers`, `oathsHolds(lineage)`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/GatherDenizens.scala`
  (the keep, then the gathering without the kept ones)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/StartingAdviserChoice.scala`
  (the kept card buried for an Oaths holder)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWorldProcedure.scala`
  (`noteKeys`)
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/OathsOfLoyaltySuite.scala`
- Modify (imports): tests importing `OathsOfLoyaltyCard` from holding
  (`LegacyGoalsSuite`, `LegacyCardDataSuite`, ...)
- Modify: the two snapshots

**Interfaces:**
- Produces: `OathsOfLoyaltyCard`; `OathsOfLoyalty` (power, no window);
  `ChronicleLegacyRules.keepsAdvisers(state): Option[PlayerId]`,
  `buriesStartingAdvisers(state, player): Boolean`, and
  `oathsHolds(lineage: LineageState): Boolean` (an active Oaths in a
  stored lineage, for Task 4's `SetupDeal`);
  `OathsOfLoyalty.keepDecisionId(player)` =
  `s"chronicle.world.legacy.oaths-of-loyalty.${player.value}"`; notes
  `OathsOfLoyalty.keptAdvisers` ("{0} kept {1} with {2}", the cards as
  `NoteArg.Cards`) and `OathsOfLoyalty.buried` ("{0} buried {1} with {2}"),
  each a note of the procedure it is written in (`buriedNotes(noteId)` per
  procedure: the World, the Beacon and, in Task 4, Setup).

- [ ] **Step 1: Write the failing tests**

`OathsOfLoyaltySuite`, on `throneHeldByChancellorP1` walked through the
World (`ChronicleWalk.through(..., ChronicleWorld, after =
ChronicleSun)`), p2 holding Oaths and five faceup advisers:

```scala
  test("Oaths of Loyalty is implemented"):
    assert(PowerImplementationStatus.implemented(
      OathsOfLoyaltyCard.power.id))

  test("the holder keeps three of five faceup advisers as facedown " +
      "starting advisers, and the rest are gathered (ruling 15)"):
    val run = world(fiveAdvisers, keeping(VillageIdiotCard, FaeMerchantCard,
      ThreateningRoarCard))
    val query = asked(run).query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((query.min, query.max, query.options.size), (0, 3, 5))
    assertEquals(lineage(run.ready, p2).startingAdvisers.map(_.id).toSet,
      Set[CardId](VillageIdiotCard.id, FaeMerchantCard.id,
        ThreateningRoarCard.id))
    assert(lineage(run.ready, p2).startingAdvisers.forall {
      case DenizenState(_, orientation, tokens) =>
        orientation == Orientation.FaceDown && tokens.isEmpty
      case _ => false })

  test("a kept adviser's tokens return to their banks first"):
    ...

  test("facedown advisers are not offered; none faceup asks nothing"):
    ...

  test("the holder's Protect and Dispossess keep is buried at the world " +
      "deck bottom (ruling 16)"):
    // p2 holds the World task too; the plainest keep is the first card.
    ...

  test("the holder's Welcome a Stranger keep is buried (ruling 16)"):
    // p2 holds the Beacon task; walk through the Beacon.
    ...

  test("a dormant Oaths keeps nothing and buries nothing (ruling 1)"):
    ...

  test("a walk resumed at the keep question finds the same five advisers"):
    ...
```

Write each sketched test in full when writing the suite: the card objects
are `VillageIdiotCard`, `FaeMerchantCard`, `ThreateningRoarCard`,
`MapLibraryCard` and `DowntroddenCard` (site or either-place denizens),
the burial asserted by the card at `worldDeck.last` and the line
`s"p2 buried ${card.id.value} with ${OathsOfLoyaltyCard.id.value}"`.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `Not found: OathsOfLoyaltyCard` in the new
package.

- [ ] **Step 3: Implement**

`GatherDenizens.steps` becomes:

```scala
  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Branch((state, _) => OathsOfLoyalty.keep(catalog, state)),
    BuildOps((state, pending) => Right(gatherAdvisers(catalog, state,
      OathsOfLoyalty.kept(state, pending))), required = true),
    ...the rest unchanged)
```

where `OathsOfLoyalty.keep` reads `keepsAdvisers(state)` and asks the
choose-many over the holder's faceup `DenizenState` advisers, then, in a
`Branch` reading the answer, returns each chosen adviser's tokens (the
same favor and secret moves `gatherAdvisers` writes, made a shared
`GatherDenizens.returnTokens(catalog, player, card)`) and moves it to
`Location.StartingAdvisers(lineage)` with `resultingOrientation =
Some(FaceDown)`, with the `keptAdvisers` line; `OathsOfLoyalty.kept(state,
pending)` is the chosen ids, which `gatherAdvisers` then skips (by then
they have already left the player, so the skip is a guard).

`StartingAdviserChoice.steps` takes the state when its Branch builds it:
for a holder `buriesStartingAdvisers`, the `keepKey` placement moves to
the world deck bottom (`Bury`'s destination, `StackPosition.Bottom`,
facedown) and the line is `OathsOfLoyalty.buried` under the caller's note
id; the question is unchanged (open question 8).

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

As in Task 1. `./sbtw test`: PASS.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): Oaths of Loyalty keeps advisers for the next game

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: Oaths of Loyalty in the next game's Setup

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupDeal.scala`
  (`of`: an Oaths holder draws nothing under Teeming World)
- Modify: `src/main/scala/distantlands/gameplay/setup/SetupProcedure.scala`
  (`chooseAdviser`, the kept-adviser line: buried for an Oaths holder)
- Test: `src/test/scala/distantlands/gameplay/setup/SetupDealSuite.scala`,
  and a new `src/test/scala/distantlands/gameplay/setup/OathsSetupSuite.scala`

**Interfaces:**
- Consumes: Task 3's `ChronicleLegacyRules.oathsHolds`,
  `buriesStartingAdvisers` and `OathsOfLoyalty.buried`.

- [ ] **Step 1: Write the failing tests**

```scala
  test("under Teeming World an Oaths holder draws no starting adviser " +
      "(ruling 16)"):
    val record = LaterGameFixture.chronicle(LineageId("l1"), LineageId("l2"))
    val altered = record.copy(foundations = record.foundations.updated(
      FoundationNumber.IV, FoundationState(FoundationFace.Altered, Set.empty)),
      lineages = record.lineages.map(lineage =>
        if lineage.id == LineageId("l1") then lineage.copy(legacies =
          Vector(LegacyState(OathsOfLoyaltyCard.id, active = true)))
        else lineage))
    val deal = SetupDeal.of(altered, participants(altered))
    assertEquals(deal.draws.find(_._1 == p1).map(_._2), Some(0))
```

(Use the fixture's own field names for the Chronicle's foundations and the
participants; adapt if the record stores foundation faces differently.)

`OathsSetupSuite`: a later game from `LaterGameFixture`, Ties of
Fellowship, l1 (p1) holding Oaths active and no starting adviser: p1 draws
a hand of three; the Setup adviser question is asked as usual; the kept
card goes to the world deck bottom and p1 holds no adviser; the line is
"p1 buried {card} with Oaths of Loyalty"; a dormant Oaths keeps as before.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: p1 draws three under Teeming World; the kept card is an
adviser.

- [ ] **Step 3: Implement**

`SetupDeal.of`: under `FoundationFace.Altered`, a participant whose stored
lineage `ChronicleLegacyRules.oathsHolds` draws 0. `SetupProcedure`: the
adviser choice reads `buriesStartingAdvisers(ready, player)` when its
`BuildOps` and `Note` run; for the holder the chosen card moves to the
world deck bottom facedown and the line is `OathsOfLoyalty.buried` under
Setup's note id (declare it in Setup's note keys).

- [ ] **Step 4: Run the tests to verify they pass**

Run `./sbtw test`: PASS. The later-game goldens hold no Oaths and do not
change.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): Oaths of Loyalty buries the next game's starting advisers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: The Bright Beacon

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/TheBrightBeacon.scala`
- Modify: `LegacyChroniclePowers.scala`, `HoldingLegacies.scala` (remove
  `TheBrightBeaconCard`), `ProcedureLog.scala`
- Modify: `ChronicleLegacyRules.scala` (`beaconFromDispossessed`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/IntroduceNewcomers.scala`
  (the source question, then the ordinary step or the Dispossessed)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleBeaconProcedure.scala`
  (`noteKeys`)
- Modify: `GamePresentationProjector.scala` (`identifiesCard`:
  `Dispossessed`, `NewlyDispossessed`)
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/TheBrightBeaconSuite.scala`
- Modify: the visibility suite of Task 2; the two snapshots

**Interfaces:**
- Produces: `TheBrightBeaconCard`; `TheBrightBeacon` (power, no window);
  `ChronicleLegacyRules.beaconFromDispossessed(state): Option[PlayerId]`,
  the Beacon holder when they hold The Bright Beacon in effect;
  `TheBrightBeacon.sourceId(player)` =
  `s"chronicle.beacon.legacy.the-bright-beacon.${player.value}"`,
  `TheBrightBeacon.cardsId(player)` = `sourceId(player) + ".cards"`,
  `TheBrightBeacon.box` and `TheBrightBeacon.dispossessed` (buttons); note
  `TheBrightBeacon.took` ("{0} took six newcomers from the Dispossessed
  with {1}").

- [ ] **Step 1: Write the failing tests**

`TheBrightBeaconSuite`, on the table `ChronicleWalk.beaconWalkedTo` uses
(p3 holds the Beacon), with p3 holding The Bright Beacon and the older
Dispossessed seeded by `Table.dispossessed`:

```scala
  test("The Bright Beacon is implemented"):
    assert(PowerImplementationStatus.implemented(
      TheBrightBeaconCard.power.id))

  test("the holder first chooses the source, the box first, before seeing " +
      "the Dispossessed (ruling 17)"):
    ...

  test("the box runs the ordinary step"):
    ...

  test("from the Dispossessed the holder chooses exactly six of all of " +
      "them, the newly Dispossessed included, and Welcome a Stranger runs " +
      "on those six"):
    ...

  test("exactly six Dispossessed are taken without a question"):
    ...

  test("a holder not resolving the Beacon is not asked"):
    ...

  test("the Dispossessed are named to the holder after the source choice, " +
      "and to no other viewer (open question 6)"):
    ...

  test("a walk resumed at the six-card question finds the same cards"):
    ...
```

Write each sketched test in full when writing the suite.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL to compile: `Not found: TheBrightBeaconCard` in the new
package.

- [ ] **Step 3: Implement**

`IntroduceNewcomers.steps(catalog)` becomes:

```scala
  def steps(catalog: ExecutableCatalog): Vector[Operation] = Vector(
    Branch((state, _) => TheBrightBeacon.askSource(state)),
    Branch((state, pending) =>
      if TheBrightBeacon.fromDispossessed(state, pending) then
        TheBrightBeacon.fromDispossessedSteps(state)
      else ordinary(catalog)))
```

where `ordinary(catalog)` is the existing body, and the answer read from
`pending` keeps the branch's shape on resume. `fromDispossessedSteps`:
one `BuildOps` of `Peek(holder, card, location)` for each Dispossessed
denizen (`Location.Dispossessed`, then `Location.NewlyDispossessed`);
then the choose-many of six, or all six without a question; then the
moves into `Location.Hand(holder)` and the `took` line. (Check that a move
out of `NewlyDispossessed` is supported by `CardMovementOperations`; if a
removal arm is missing, add it with a test, as the Stars' own moves read
that pile.)

`identifiesCard` gains:

```scala
      // A Dispossessed card a player peeked: The Bright Beacon's holder
      // examines them (Legacy Chronicle powers design, ruling 17).
      case CardContainer.Dispossessed | CardContainer.NewlyDispossessed =>
        viewer.exists(player => ready.knowledge.advisers
          .getOrElse(player, Vector.empty).contains(id))
```

(Use the `CardContainer` names the index defines for the two piles.)

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

As in Task 1. `./sbtw test`: PASS.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): The Bright Beacon takes newcomers from the Dispossessed

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: Golden logs for Oaths and Heirloom

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`
  (`chronicleOathsHeirloom`, `throneHeirloom` from Task 1 if not shared)
- Modify: `src/test/scala/distantlands/application/gamelog/LaterGameLogScripts.scala`
  (`laterGameAfterOaths`, the next game's Setup from that Chronicle's
  record)
- Create: the four golden files (`chronicle-oaths-heirloom.*.log`,
  `later-game-after-oaths.*.log`)

- [ ] **Step 1: Write the scripts**

`chronicleOathsHeirloom`: the game of `throneOffered`, with p2 holding
Oaths of Loyalty (and the Darkest Secret to keep it active) and two faceup
advisers, and Heirloom and two relics; p2 keeps both advisers with Oaths
and Cup of Plenty with Heirloom. `laterGameAfterOaths`: the next game,
started from the record that script's stream journals, under Ties of
Fellowship, with every Setup park answered as the situation does by
default.

- [ ] **Step 2: Write and review the goldens**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
Read all four files line by line: the Heirloom lines (the public one for
the other seat; both for the actor if the actor is p2), the Oaths keep
line naming both advisers, p2 starting the next game with two facedown
advisers and a facedown relic, drawing no hand. No existing golden
changes.

- [ ] **Step 3: Commit**

```bash
git commit -m "test(gamelog): goldens for an Oaths and Heirloom Chronicle and the next Setup

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 7: Docs and the gate

**Files:**
- Modify: `docs/rules/chronicle.md` (Throne steps 4-5, World steps 2-3,
  Beacon steps 1-2), `docs/rules/ambiguities.md` (design rulings 13-17 and
  this slice's implementation rulings), `docs/rules/implementation-traceability.md`
  (Throne, World and Beacon rows; the later-game row for Oaths and the
  kept relic), `docs/ROADMAP.md` (the in-play item names The Ear's ACTION
  half, R7), the design's status line.

- [ ] **Step 1: Docs**, as slice 2's Task 5.

- [ ] **Step 2: Run the verification gate**, as slice 2's Task 5, in the
  background. UI check: The Ear's relic picks, Heirloom's pick and the
  Beacon's six-card pick are board or panel picks of kinds the app shows;
  reaching them in the running app needs a finished game holding these
  legacies, which no fixture offers.

- [ ] **Step 3: Commit**

```bash
git commit -m "docs(legacies): record the relic, adviser and newcomer legacy powers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

## Self-Review

**Spec coverage** (design, "Slice 3: relics and the next game"):
- The Heirloom window: Task 1; Scatter Relics reads `scatterer`: Task 2;
  Gather Denizens, Protect and Dispossess and Welcome a Stranger read
  Oaths' queries: Task 3; Setup's adviser pick and Teeming World: Task 4;
  Introduce Newcomers reads `beaconFromDispossessed`: Task 5.
- Heirloom, The Ear's Chronicle half, Oaths of Loyalty and The Bright
  Beacon leave holding: Tasks 1, 2, 3 and 5 (R7 for The Ear).
- Tests the spec lists: Heirloom's relic reaching the next game's start
  facedown, and its redaction (Task 1); The Ear placing chosen relics and
  the rest returning (Task 2); Oaths keeping three of five, burying a
  Protect keep (Task 3), the next Setup burying the hand pick and Teeming
  World dealing nothing (Task 4); The Bright Beacon from the box and from
  the Dispossessed with Welcome a Stranger on the six (Task 5); the golden
  logs (Task 6, open question 11). Resume tests for each new question.
- Hidden choices: Heirloom's relic (Task 1), The Ear's relics (Task 2),
  the Dispossessed (Task 5), each with a viewer test.
- Journal: `StartingRelicKept` (Task 1), replayed by `LeafApplication` and
  covered by both codec suites; Oaths' keeps use `StartingAdvisers`, as
  Protect and Dispossess's do.
- Docs: Task 7.

**Placeholder scan:** several tests are sketched with their scenario, card
objects and assertions named, to be written in full with the suite; each
says so. These suites depend on answers to open questions 2, 5, 8 and 9,
whose rulings fix the questions' exact shapes, so their code is written
after the rulings, in the same session, before execution starts. The
sketched implementation of The Ear's `place` names its pattern
(Reformer's `owed`).

**Type consistency:** `StartingRelicKept(PlayerId, RelicId)`;
`ChronicleLegacyRules.scatterer`, `keepsAdvisers`, `beaconFromDispossessed`
(`ReadyGame => Option[PlayerId]`), `buriesStartingAdvisers(ReadyGame,
PlayerId): Boolean`, `oathsHolds(LineageState): Boolean`; decision ids as
each task's interface defines; `ReliquaryAndRelics.emptySlots` in Task 2.
