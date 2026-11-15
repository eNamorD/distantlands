# Legacy In-Play Powers, Slice 1: Seam and ACTIONs Implementation Plan

> Executed inline by the implementer.

**Goal:** Five legacy ACTIONs are used from the Act menu with the legacy
named as their source. Ruthless buries a card at its holder's site.
Peacemaker returns a banner and leaves one warband on every site and
board. Golden Key takes a relic from the Reliquary, and its Imperial
holder may always peek at the Reliquary, which the shared bank now shows.
High Priest moves secrets from the Darkest Secret to its holder's board.
The Ear peeks at the relics at any site. Each writes one line naming the
legacy.

**Architecture:** The seam is small, because the source index and the
access rule already know legacies (`RuleSourceIndex` lists
`RuleSourceRef.Legacy`, `PowerAccess.accessible` accepts an active one of
the actor's lineage). What falls through to `None` today is the mapping
from a rule source to a menu option and back: `PhasePowerProcedure`'s
`sourceRef` and `sourceOf`, `PowerSourceRef.of`, and
`DecisionOptionRef.ofCard` and `cardOf`. A legacy source maps to
`PowerSourceRef.Card(legacyId)` and `DecisionOptionRef.Legacy(legacyId)`,
so the existing cost path pays a legacy's burnt cost as it pays a card's.
Each ACTION is a `PaidAction` in a new package, `gameplay/powers/legacies`,
beside its card object (the "card sits beside its power" rule of
`CardCatalogSuite`), registered by `LegacyActionPowers.all` in
`PhasePowerCatalog`. The Ear's card stays in `gameplay/chronicle/legacies`;
its ACTION is a second power object with the same printed id, as Hidden
Passages is both a phase power and a contributing power. The Reliquary is
a new projection field the shared bank draws, its faces gated by a new
clause of `GamePresentationProjector.identifiesCard`.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), Scala.js
frontend in jsdom, compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy in-play powers design](../../specs/2026-11/2026-11-14-legacy-in-play-powers-design.md),
rulings 1-13 and section "Slice 1: seam and ACTIONs".
[Implementation rulings](../../specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md).
Card text in `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
(Peacemaker L04, Golden Key L12, High Priest L21, Ruthless L29, The Mouth
L34) and `src/main/scala/distantlands/gameplay/chronicle/legacies/TheEar.scala`
(The Ear L19). The rules: Minor actions, "use an Action power"
(`docs/rules/rules-reference.md#minor-actions`); access to the play area,
legacies included (`#core-state-and-terminology`); the Reliquary (NF,
"The Reliquary": the Chancellor may "peek at the Reliquary pile and take
any one relic from it").

## Prerequisite

The Legacy Setup powers batch has landed, and the files this plan shares
with it are committed. Check before Task 1:

```bash
git status --short src/main/scala/distantlands/model/Decisions.scala \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/application/GamePresentationProjector.scala \
  frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/styles.css docs/rules
grep -q "object RuthlessCard" src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala &&
grep -q "case RuleSourceRef.Legacy(lineage, _) =>" src/main/scala/distantlands/gameplay/PowerAccess.scala &&
echo ready
```

Expected: no file listed, then `ready`.

## Files the Setup batch also touches

These files changed in the Legacy Setup powers batch, which is finishing
in this checkout. Re-read each before editing it; the line numbers below
are from 2026-11-14.

- `src/main/scala/distantlands/model/Decisions.scala` (Task 1; the batch
  added `ChooseAmounts`).
- `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (Tasks 2, 3, 4, 5 and 7; the batch moves its cards out).
- `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
  (Task 3; the batch adds imports).
- `src/main/scala/distantlands/application/GamePresentationProjector.scala`
  (Task 6; the batch added Matchmaker's and Seer's clauses).
- `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala`
  and `frontend/styles.css` (Task 6).
- `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
  (Task 6).
- `src/test/scala/distantlands/model/DecisionOptionRefSuite.scala` (Task 2).
- `src/test/scala/distantlands/application/gamelog/GoldenScripts.scala`
  (Task 9).
- `docs/rules/ambiguities.md`, `docs/rules/implementation-traceability.md`,
  `docs/rules/rules-reference.md` (Task 9).

The Setup batch's uncommitted files on 2026-11-14
(`GeneratedFirstGamePlanFactory`, `EmpireSetupRules`, `GameStartRules`
and the setup test suites) are not touched by this plan.

## Rulings

The plan's open questions were ruled on 2026-11-14 as implementation
rulings R1-R5 ([rulings](../../specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md)):

- **R1** (Tasks 5 and 6): "If you are an Imperial" gates both Golden
  Key's peek and its ACTION; an Exile holder's menu leaves the ACTION out.
- **R2** (Task 8): The Ear's "not identified" means missing from the
  holder's recorded card knowledge (`knowledge.siteRelics`), so their own
  pawn's site counts until they peek there.
- **R3** (Tasks 7 and 8): the lines are "p2 moved 3 secrets from the
  Darkest Secret with High Priest" and "p2 peeked at the relics at Deep
  Woods with The Ear"; The Ear's never names the relics.
- **R4** (Tasks 3, 4, 5, 7 and 8): an ACTION with nothing to do is shown
  unavailable with its reason, as card ACTIONs are ("No denizen at your
  site", "You hold no banner", "The Reliquary is empty", "You do not hold
  the Darkest Secret", "The Darkest Secret holds no secrets", "No site
  holds a relic you have not seen"). Only Golden Key for an Exile holder
  is left out (`PhasePower.listed`), a role gate under R1.
- **R5** (Tasks 5 and 6): the shared bank and Golden Key read
  `reliquaryInPlay`, so a set-aside Reliquary shows nothing and gives
  nothing; spec ruling 9 does not override Foundations ruling 3.

## Plan choices

Mechanical choices made here, not open questions:

- A legacy ACTION is a phase power option in the menu's Power band, as a
  card's ACTION is: "use an Action power" is the minor action the rules
  name (ruling 3).
- Decision ids follow the phase powers': `power.ruthless.denizen`,
  `power.peacemaker.banner`, `power.golden-key.relic`,
  `power.high-priest.secrets`, `power.the-ear.site`. None repeats an
  existing id (The Ear's Chronicle question is
  `chronicle.throne.legacy.the-ear.<player>`).
- Every power's line is a sourceless `used` note, so it reads as ruling
  13 writes it, with no "Ruthless: " prefix, and replaces "Used Ruthless".
  Each lists its decision in `narratedDecisions`, so no "Chose ..." line
  repeats it.
- Ruthless, Golden Key, High Priest and The Ear always ask, even with one
  option, as Giant's Rampage and the Setup Reliquary pick do. Peacemaker
  asks only when its holder holds both banners (ruling 8).
- Peacemaker's removals are `Kill`s, as Ballot Box's "remove" is: a kill
  returns warbands to their bank, bandits to the bandit supply.
- "Your site" is the holder's pawn site.
- Every locked denizen in the catalog is adviser-only, so the locked card
  Ruthless buries in its test is an intact edifice, which is locked.

## Deviations from the spec

- Spec ruling 6 names `PhasePowerProcedure.payable` among the places that
  gain a legacy case. It needs none: a legacy source is
  `PowerSourceRef.Card(legacyId)`, which `payable` and `assemble` already
  pay from, and a burnt cost places nothing on the card. Task 2 proves it
  with a test.
- The Ear's ACTION is a second power object, `TheEarAction`, in
  `gameplay/powers/legacies`, with The Ear's printed id; its card and its
  Chronicle half stay in `gameplay/chronicle/legacies/TheEar.scala`.
- `Decisions.scala` is split by moving `DecisionOptionRef` and its
  companion to `model/DecisionOptionRef.scala` (Task 1).

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file, production or test, stays at or under 800 lines
  (`scripts/check-architecture.py`).
- `model` imports nothing from `gameplay`; `gameplay` never reads a
  catalog `.text` or `rulesText`; `gameplay/actions` never imports
  `gameplay/powers`.
- A new test names cards by their card objects (`RuthlessCard`).
- Only seated players' active legacies act (ruling 1); "if you are an
  Imperial" is read when the power is used, through
  `ChronicleLegacyRules.holder` with `ChronicleLegacyRules.imperialRoles`
  (ruling 2).
- No `Branch`'s children or ids depend on state the walk itself changes
  before the branch's question; every new `Decide` has a resume test, and
  no two decisions share an id.
- The Reliquary's faces are named only to a viewer `identifiesCard`
  allows; a hidden relic's id never reaches another viewer's projection.
- Old saved streams still decode: no wire format of a journaled event
  changes. The projection gains one field, `reliquary`, which a payload
  without it decodes to empty.
- TDD: write the failing test, run it and see it fail, implement, run it
  and see it pass, commit. One commit per task. Task 1 is a pure move and
  is verified by the existing suites.
- Statement coverage stays at or above 92% and branch coverage at or above
  84.4%.
- A rewritten snapshot or golden file is reviewed line by line and
  committed in the task that changes it; every changed line must be one the
  task explains. `git status --short src/test/resources` is otherwise clean.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Stage only
  this plan's files and commit with an explicit pathspec
  (`git commit -- <paths>`); run `git log -3` after each commit.

## The gate

Run after the last task, and before any task whose commit touches more
than its own suite. Run the two sbt commands in the background.

```bash
./sbtw clean coverage test coverageReport
./sbtw "frontend/test"
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git status --short src/test/resources
```

Expected: every suite passes; coverage at or above 92% statements and
84.4% branches; both scripts pass; the last command prints nothing. Then
start the server (`.claude/launch.json`, `oath-server`), open a later game
whose Reliquary holds relics, and read the shared bank's text.

---

### Task 1: Split `Decisions.scala`

**Files:**
- Create: `src/main/scala/distantlands/model/DecisionOptionRef.scala`
- Modify: `src/main/scala/distantlands/model/Decisions.scala:1-237`

**Interfaces:**
- Produces: `DecisionOptionRef` and its companion, unchanged, in their own
  file. Every caller compiles untouched (same package).

- [ ] **Step 1: Confirm the block's bounds**

Run: `sed -n '26,27p;236,238p' src/main/scala/distantlands/model/Decisions.scala`
Expected: line 27 opens `/** Stable identity of one selectable thing in
a decision.`, line 236 is `        case _ => None)` closing `facingOf`, and
line 238 opens the `OptionGroup` doc comment. If the Setup batch moved
them, use the lines that hold these three markers below.

- [ ] **Step 2: Move the block**

```bash
f=src/main/scala/distantlands/model/Decisions.scala
t=src/main/scala/distantlands/model/DecisionOptionRef.scala
{ printf 'package distantlands.model\n\n'
  printf '%s\n' \
    '// The reference half of the walker decision contract: the persisted' \
    '// identity of one selectable thing. It lives beside `Decisions.scala`,' \
    '// which holds the options, queries and answers built on it; the two' \
    '// were one file until it reached the 800-line limit.' ''
  sed -n '27,236p' "$f"; } > "$t"
sed -i '' '27,237d' "$f"
```

In `Decisions.scala`'s header comment, after the paragraph ending "Do not
\"restore\" the split.", add:

```scala
//
// The reference vocabulary, `DecisionOptionRef`, is in
// `DecisionOptionRef.scala` beside this file, split out at the 800-line
// limit; it is the same contract.
```

- [ ] **Step 3: Verify**

Run: `./sbtw "testOnly distantlands.model.*"` and
`python3 scripts/check-architecture.py`
Expected: all pass; `wc -l` shows `Decisions.scala` near 530 lines and
`DecisionOptionRef.scala` near 220.

- [ ] **Step 4: Commit**

```bash
git add src/main/scala/distantlands/model/DecisionOptionRef.scala
git commit -m "refactor(model): move DecisionOptionRef to its own file

Decisions.scala was near the 800-line limit (Legacy in-play powers
ruling 6). The reference vocabulary moves unchanged.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/model/DecisionOptionRef.scala \
  src/main/scala/distantlands/model/Decisions.scala
git log -3
```

### Task 2: The legacy source seam and the printed costs

**Files:**
- Modify: `src/main/scala/distantlands/model/DecisionOptionRef.scala`
  (`ofCard`, `cardOf`)
- Modify: `src/main/scala/distantlands/model/GameState.scala:141-149`
  (`PowerSourceRef.of`)
- Modify: `src/main/scala/distantlands/gameplay/phases/PhasePowerProcedure.scala:196-220`
  (`sourceRef`, `sourceOf`)
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (Ruthless, Golden Key and The Mouth costs)
- Create: `src/test/scala/distantlands/gameplay/powers/legacies/LegacyActionFixture.scala`
- Test: `src/test/scala/distantlands/model/DecisionOptionRefSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/PhasePowerSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Produces:
  - `DecisionOptionRef.ofCard(id: LegacyId) == Some(DecisionOptionRef.Legacy(id))`;
    `DecisionOptionRef.cardOf(DecisionOptionRef.Legacy(id)) == Some(id)`.
  - `PowerSourceRef.of(DecisionOptionRef.Legacy(id)) == Some(PowerSourceRef.Card(id))`.
  - A phase power whose id a held active legacy prints is offered with the
    source `DecisionOptionRef.Legacy(id)` and pays from
    `PowerSourceRef.Card(id)`.
  - `LegacyActionFixture.boxed(ready: ReadyGame, player: PlayerId): ReadyGame`.

- [ ] **Step 1: Write the fixture and the failing tests**

`LegacyActionFixture.scala`:

```scala
package distantlands.gameplay.powers.legacies

import distantlands.model._

/** Tables for the legacies' in-play powers (Legacy in-play powers design). */
object LegacyActionFixture:
  /** The lineage nobody is seated with in [[boxed]]. */
  val boxedLineage: LineageId = LineageId("boxed")

  /** `ready` with `player`'s legacies moved to a lineage nobody is seated
    * with, so they are a boxed lineage's and never act (ruling 1). */
  def boxed(ready: ReadyGame, player: PlayerId): ReadyGame =
    val lineage = ready.game.current.player(player)
      .getOrElse(throw AssertionError(s"$player is not seated")).lineage
    ready.updateCampaign(campaign =>
      val held = campaign.lineages(lineage)
      campaign.copy(lineages = campaign.lineages
        .updated(lineage, held.copy(legacies = Vector.empty))
        .updated(boxedLineage, held.copy(id = boxedLineage,
          previousPlayer = None))))
```

In `DecisionOptionRefSuite`, after the legacy reference test:

```scala
  test("a legacy names itself as a card option and as a power source, and " +
      "back (in-play powers ruling 6)"):
    assertEquals(DecisionOptionRef.ofCard(SeerCard.id), Some(seer))
    assertEquals(DecisionOptionRef.cardOf(seer), Some(SeerCard.id))
    assertEquals(PowerSourceRef.of(seer),
      Some(PowerSourceRef.Card(SeerCard.id)))
```

In `PhasePowerSuite`, add the imports
`distantlands.catalog.holding.{GoldenKeyCard, RuthlessCard, TheMouthCard}`
and `distantlands.gameplay.powers.legacies.LegacyActionFixture`, then:

```scala
  private val ruthless = DecisionOptionRef.Legacy(RuthlessCard.id)

  /** p1's lineage holds Ruthless, active unless `active` is false, with 3
    * favor, in p1's Act. */
  private def holdingRuthless(active: Boolean = true)(
      using munit.Location): ReadyGame =
    Table.start.legacy(p1, RuthlessCard, active).favor(p1, 3)
      .turn(p1, Phase.Act).ready

  test("an active legacy is a source of its power for its seated holder " +
      "only (in-play powers rulings 1 and 6)"):
    val power = TestPower(RuthlessCard.power.id, PowerTiming.Act)
    def menu(ready: ReadyGame) = PhasePowerHarness.menuPowers(catalog, ready,
      p1, PhasePowers(Vector(power)), WalkerPowers.empty).map(_.ref)
    assertEquals(menu(holdingRuthless()), Vector(ruthless))
    assertEquals(menu(holdingRuthless(active = false)), Vector.empty)
    assertEquals(menu(Table.start.legacy(p2, RuthlessCard)
      .turn(p1, Phase.Act).ready), Vector.empty)
    assertEquals(menu(LegacyActionFixture.boxed(holdingRuthless(), p1)),
      Vector.empty)

  test("a legacy ACTION pays its burnt cost from its holder's board, and " +
      "its use replays (in-play powers rulings 3 and 6)"):
    val choice = "test-legacy-choice"
    val power = TestPower(RuthlessCard.power.id, PowerTiming.Act,
      player => Decide(choice, player, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(DecisionOptionRef.Button("go"), "Go")))),
      cost = RuthlessCard.power.cost)
    val start = Ready(holdingRuthless())
    val parked = accepted(rules(power).startWalker(MenuPlay.withoutMenu(start),
      ActionRef.UsePower(power.id), p1, Vector(ruthless)))
    assertEquals(Look(Outcomes.ready(parked.state)).favor(p1), 1)
    val done = accepted(rules(power).resolveWalker(parked.state, p1, choice,
      DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Button("go"))))
    assertEquals((parked.events ++ done.events)
      .foldLeft[Either[OathViolation, OathState]](Right(start))(
        (state, event) => state.flatMap(rules(power).evolve(_, event))),
      Right(done.state))
    // The parked walk carries the legacy as its start selection; it
    // survives the wire as a legacy.
    Outcomes.assertWireRoundTrips(parked.events ++ done.events)

  test("Golden Key and The Mouth print their burnt costs (ruling 6)"):
    assertEquals(GoldenKeyCard.power.cost, Cost(favorBurnt = 1,
      secretBurnt = 1))
    assertEquals(TheMouthCard.power.cost, Cost(favorBurnt = 2))
```

In `WalkerOperationWireSuite`, after "every CoreOperation variant
round-trips through the walker codec" (import
`distantlands.testkit.Outcomes.assertWireRoundTrips`):

```scala
  test("a legacy's power source and a denizen's with the same id stay " +
      "apart through the codec and replay (in-play powers ruling 6)"):
    val id = "L29"
    val sources = Vector[CardId](LegacyId(id), DenizenId(id))
      .map(PowerSourceRef.Card(_))
    assertNotEquals(sources(0), sources(1))
    val uses = sources.map(source => recordedOperation(RecordPowerUse(
      PowerUseRef(PowerTiming.Wake, source, PowerId("legacy.ruthless")))))
    assertWireRoundTrips(uses)
    val encoded = encodedStreamThatRoundTrips("legacy-source", uses)
    assertEquals(ujson.read(encoded).arr.map(
      _("payload")("step")("ops")(0)("cardKind").str).toVector,
      Vector("legacy", "denizen"))
```

`assertWireRoundTrips` compares each decoded event with the original, so
a legacy decoded as a denizen of the same id fails it: `LegacyId("L29")`
and `DenizenId("L29")` are unequal. Phase menu starts carry the source as
`DecisionOptionRef.Legacy`, whose wire kind is `legacy`
(`DecisionOptionRefSuite`), and the PhasePowerSuite test above round-trips
a parked legacy use.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.model.DecisionOptionRefSuite distantlands.gameplay.PhasePowerSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: the `ofCard` assertion fails (`None`); the menu test fails
(`Vector()` for the active holder); the cost test fails (favor 3, not 1);
the printed-costs test fails (`Cost.free`). The wire test already passes
(the codec spells a card's kind), and stays as the regression guard the
seam relies on.

- [ ] **Step 3: Implement**

`DecisionOptionRef.scala`, `ofCard` and `cardOf`:

```scala
  /** The option that names `card` by itself: a denizen, relic, edifice or
    * legacy. A vision has none, so a consent keyed by a card cannot name
    * one. */
  def ofCard(card: CardId): Option[DecisionOptionRef] = card match
    case id: DenizenId => Some(Denizen(id))
    case id: RelicId => Some(Relic(id))
    case id: EdificeId => Some(Edifice(id))
    case id: LegacyId => Some(Legacy(id))
    case _ => None

  /** The card `ref` names by itself: the inverse of `ofCard`. */
  def cardOf(ref: DecisionOptionRef): Option[CardId] = ref match
    case Denizen(id) => Some(id)
    case Relic(id) => Some(id)
    case Edifice(id) => Some(id)
    case Legacy(id) => Some(id)
    case _ => None
```

`GameState.scala`, `PowerSourceRef.of`: add
`case DecisionOptionRef.Legacy(id) => Some(Card(id))` after the edifice
case.

`PhasePowerProcedure.scala`: in `sourceRef`, before `case _ => None`:

```scala
    // A legacy in its holder's play area (Legacy in-play powers ruling 6):
    // access has already kept only the actor's active ones.
    case RuleSourceRef.Legacy(_, id) =>
      Some(PowerSourceRef.Card(id) -> DecisionOptionRef.Legacy(id))
```

and in `sourceOf`, before `case other`:
`case DecisionOptionRef.Legacy(id) => Right(PowerSourceRef.Card(id))`.

`HoldingLegacies.scala`: Ruthless's `cost = Cost(favorBurnt = 2)`, Golden
Key's `cost = Cost(favorBurnt = 1, secretBurnt = 1)`, The Mouth's
`cost = Cost(favorBurnt = 2)`.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.model.* distantlands.gameplay.PhasePowerSuite distantlands.gameplay.PhaseMenuSuite distantlands.gameplay.powers.* distantlands.serialization.*"`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/gameplay/powers/legacies/LegacyActionFixture.scala
git commit -m "feat(legacies): a held active legacy is a phase power source

A legacy maps to its card option and power source, so the menu offers its
ACTION and pays its burnt cost as a card's. Ruthless, Golden Key and The
Mouth print their burnt costs (Legacy in-play powers ruling 6).

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/model/DecisionOptionRef.scala \
  src/main/scala/distantlands/model/GameState.scala \
  src/main/scala/distantlands/gameplay/phases/PhasePowerProcedure.scala \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/LegacyActionFixture.scala \
  src/test/scala/distantlands/model/DecisionOptionRefSuite.scala \
  src/test/scala/distantlands/gameplay/PhasePowerSuite.scala \
  src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git log -3
```

### Task 3: Ruthless

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/Ruthless.scala`
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/LegacyActionPowers.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `RuthlessCard`)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`
  (import `distantlands.gameplay.powers.legacies._`)
- Modify: `src/test/scala/distantlands/gameplay/PhasePowerSuite.scala`
  (import `RuthlessCard` from `gameplay.powers.legacies`)
- Test: `src/test/scala/distantlands/gameplay/powers/legacies/RuthlessSuite.scala`

**Interfaces:**
- Consumes: Task 2's seam; `LegacyActionFixture.boxed`.
- Produces:
  - `RuthlessCard` (L29) in `distantlands.gameplay.powers.legacies`.
  - `case object Ruthless extends PaidAction(RuthlessCard.power)` with
    `decisionId: String = "power.ruthless.denizen"`, `buried: NoteKey`,
    `targets(ready: ReadyGame, player: PlayerId): Vector[(SiteId, SiteDenizenState)]`.
  - `LegacyActionPowers.all: Vector[PhasePower]`, appended to
    `PhasePowerCatalog.default`.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.powers.legacies

import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.economy.DefameCard
import distantlands.gameplay.powers.setup.HallOfDebateCard
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** Ruthless (L29): burn 2 favor to bury a denizen at your site (Legacy
  * in-play powers rulings 1, 6, 7 and 13). */
class RuthlessSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val source = DecisionOptionRef.Legacy(RuthlessCard.id)
  private val defame = DecisionOptionRef.Denizen(DefameCard.id)
  private val hall = DecisionOptionRef.Edifice(HallOfDebateCard.id)

  /** p1's Act at Ancient City holding Ruthless active with `favor`, Defame
    * there with 1 favor on it and Hall of Debate intact beside it. */
  private def table(favor: Int = 2): Table =
    Table.start.turn(actor, Phase.Act).pawn(actor, AncientCitySite)
      .legacy(actor, RuthlessCard).favor(actor, favor)
      .denizen(DefameCard, AncientCitySite).tokens(DefameCard, favor = 1)
      .edifice(HallOfDebateCard, EdificeSide.Intact, AncientCitySite)

  private def offered(ready: ReadyGame): Boolean =
    usableNow(ready).exists(_.power.id == Ruthless.id)

  test("it is offered while the holder's site holds a denizen, and burns " +
      "2 favor"):
    val ready = table().ready
    assert(offered(ready))
    val t = accepted(use(ready, Ruthless, source))
    assert(awaits(t, Ruthless.decisionId))
    assertEquals(PhasePowerHarness.offered(t, actor), Some(Vector(
      "denizen" -> DefameCard.id.value, "edifice" -> HallOfDebateCard.id.value)))
    assertEquals(Look(after(t)).favor(actor), 0)

  test("it buries the chosen denizen, returning its favor, and writes one " +
      "line"):
    val ready = table().ready
    val done = useAndAnswer(ready, Ruthless, source, Ruthless.decisionId,
      defame)
    val now = Look(after(done.answered))
    assert(!now.cardsAt(AncientCitySite).contains(DefameCard.id))
    assertEquals(now.bankFavor(DefameCard.suit),
      Look(ready).bankFavor(DefameCard.suit) + 1)
    assertEquals(replayed(ready, done.events), Right(done.answered.state))
    assertEquals(NoteText.said(Ruthless, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${actor.value} buried " +
        s"${DefameCard.id.value} with ${RuthlessCard.id.value}",
        covers = true)))

  test("it buries a locked edifice (ruling 7)"):
    val done = useAndAnswer(table().ready, Ruthless, source,
      Ruthless.decisionId, hall)
    assert(!Look(after(done.answered)).cardsAt(AncientCitySite)
      .contains(HallOfDebateCard.id))

  test("it is not offered at a site with no denizen"):
    val bare = Table.start.turn(actor, Phase.Act).pawn(actor, AncientCitySite)
      .legacy(actor, RuthlessCard).favor(actor, 2).ready
    assert(!offered(bare))
    assertEquals(use(bare, Ruthless, source).left.toOption,
      Some(OathViolation.StartUnavailable("No denizen at your site")))

  test("it is not offered without 2 favor"):
    assert(!offered(table(favor = 1).ready))

  test("a dormant Ruthless and a boxed lineage's offer nothing (ruling 1)"):
    val dormant = Table.start.turn(actor, Phase.Act)
      .pawn(actor, AncientCitySite).legacy(actor, RuthlessCard, active = false)
      .favor(actor, 2).denizen(DefameCard, AncientCitySite).ready
    assert(!offered(dormant))
    assert(!offered(LegacyActionFixture.boxed(table().ready, actor)))

  test("a walk resumed at Ruthless's question buries the chosen card once"):
    val ready = table().ready
    val started = accepted(use(ready, Ruthless, source))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      Ruthless.decisionId, pick(defame)))
    val now = Look(after(done))
    assertEquals(now.cardsAt(AncientCitySite), Vector(HallOfDebateCard.id))
    assertEquals(now.favor(actor), 0)
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.RuthlessSuite"`
Expected: compile error, `Ruthless` is not found.

- [ ] **Step 3: Implement**

`Ruthless.scala`:

```scala
package distantlands.gameplay.powers.legacies

import distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
  PrintedPower}
import distantlands.gameplay.NoteSupport
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.PaidAction
import distantlands.model._

object RuthlessCard extends Legacy(LegacyId("L29"), "Ruthless",
    LegacyGoal.HighestRelic, Suit.Order):
  val power = PrintedPower(PowerId("legacy.ruthless"),
    persistent = false, cost = Cost(favorBurnt = 2),
    text = "**ACTION:** Burn [favor-burnt] [favor-burnt] to bury a " +
      "denizen at your site.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Ruthless (legacy L29), ACTION: burn 2 favor, then bury a denizen at the
  * holder's site (Legacy in-play powers design, ruling 7).
  *
  * Any card in the site's denizen row may be buried: a denizen, or an
  * edifice on either face, locked or not, since a bury ignores locks (CR:
  * "You can bury locked cards, including edifices"). Its tokens return as
  * `Bury.standard` returns them. The question is always asked; the use is
  * refused while the site holds no such card. The bury is required, so a
  * card a restriction keeps (Ancient Bloodline) is not offered.
  *
  * Line: "p1 buried Herbalist with Ruthless" (ruling 13), covering the
  * generic Buried line.
  */
case object Ruthless extends PaidAction(RuthlessCard.power):
  val decisionId: String = "power.ruthless.denizen"
  private val notes = NoteSet(id)
  /** "{p1} buried {card} with {Ruthless}". */
  val buried: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" buried "), NotePart.Arg(1), NotePart.Text(" with "),
    NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The cards in the denizen row of `player`'s site, with the site. */
  def targets(ready: ReadyGame, player: PlayerId)
      : Vector[(SiteId, SiteDenizenState)] =
    val current = ready.game.current
    current.pawnSite(player).toVector.flatMap(site =>
      current.map.sites.get(site).toVector.flatMap(_.denizens)
        .map(site -> _))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(targets(ready, player).isEmpty)("No denizen at your site")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseOne(targets(live, player).map((_, card) =>
        option(card)), heading = Some("Ruthless: bury which card at your " +
          "site?"))))),
    BuildOps((live, pending) => bury(catalog, live, player, pending),
      required = true),
    Note(id, line(_, player), covers = true))))

  private def refOf(card: SiteDenizenState): DecisionOptionRef = card match
    case held: DenizenState => DecisionOptionRef.Denizen(held.id)
    case held: EdificeState => DecisionOptionRef.Edifice(held.id)

  private def option(card: SiteDenizenState): DecisionOption = card match
    case held: DenizenState =>
      DecisionOption.Denizen(DecisionOptionRef.Denizen(held.id))
    case held: EdificeState =>
      DecisionOption.Edifice(DecisionOptionRef.Edifice(held.id))

  private def bury(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    for
      ref <- PowerAnswers.one(pending, decisionId)
        .toRight(PowerAnswers.missing(decisionId))
      found <- targets(ready, player).find((_, card) => refOf(card) == ref)
        .toRight(OathViolation.InvalidEventOrder(
          s"${ref.wireId} is not a card at the holder's site"))
      (site, card) = found
      (buryable, tokens) = card match
        case held: DenizenState => (BuryableCard.Denizen(held.id), held.tokens)
        case held: EdificeState => (BuryableCard.Edifice(held.id), held.tokens)
      suit = catalog.suitOf(buryable.id)
      _ <- Either.cond(tokens.favor == 0 || suit.isDefined, (),
        OathViolation.InvalidEventOrder(
          s"no suit is known for ${buryable.id.value}"))
    yield Bury.standard(buryable, PositionedLocation(Location.Site(site)),
      suit, tokens.favor, tokens.secrets, player)

  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    NoteSupport.answer(states, decisionId).flatMap(DecisionOptionRef.cardOf)
      .map(card => buried.sourceless(NoteArg.Player(player),
        NoteArg.Card(card), NoteArg.Card(RuthlessCard.id)))
```

`LegacyActionPowers.scala`:

```scala
package distantlands.gameplay.powers.legacies

import distantlands.gameplay.powerresolver.PhasePower

/** The legacies' ACTION powers (Legacy in-play powers design, slice 1),
  * registered by `PhasePowerCatalog`. Each is offered with its legacy as
  * its source (ruling 3). */
object LegacyActionPowers:
  val all: Vector[PhasePower] = Vector(Ruthless)
```

`PhasePowerCatalog.default`: import
`distantlands.gameplay.powers.legacies.LegacyActionPowers` and add
`LegacyActionPowers.all ++` before `RiverSitePower.all`.

Remove `RuthlessCard` from `HoldingLegacies.scala`. In `NewFoundations.scala`
add `import distantlands.gameplay.powers.legacies._`. In `PhasePowerSuite`,
import `RuthlessCard` from `distantlands.gameplay.powers.legacies`.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.* distantlands.gameplay.PhasePowerSuite distantlands.gameplay.cards.* distantlands.gameplay.powers.*CatalogSuite distantlands.gameplay.powers.PowerImplementationStatusSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: all pass. `CardCatalogSuite` finds Ruthless implemented and out
of holding.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/legacies \
  src/test/scala/distantlands/gameplay/powers/legacies/RuthlessSuite.scala
git commit -m "feat(legacies): Ruthless buries a card at its holder's site

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/legacies \
  src/main/scala/distantlands/gameplay/powers/PhasePowerCatalog.scala \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/test/scala/distantlands/gameplay/PhasePowerSuite.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/RuthlessSuite.scala
git log -3
```

### Task 4: Peacemaker

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/Peacemaker.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/legacies/LegacyActionPowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `PeacemakerCard`)
- Test: `src/test/scala/distantlands/gameplay/powers/legacies/PeacemakerSuite.scala`

**Interfaces:**
- Consumes: `BannerToSharedBank.move` and `.moved`
  (`gameplay/powers/setup/BannerSetupRules.scala`), `BannerRules.holder`.
- Produces: `PeacemakerCard` (L04);
  `case object Peacemaker extends PaidAction(PeacemakerCard.power)` with
  `decisionId = "power.peacemaker.banner"`, `returned: NoteKey`,
  `banners(ready: ReadyGame, player: PlayerId): Vector[Banner]`.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.powers.legacies

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** Peacemaker (L04): return a banner you hold to the shared bank, then
  * every site and board keeps one warband (Legacy in-play powers rulings
  * 8 and 13). */
class PeacemakerSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val p2 = Table.p2
  private val source = DecisionOptionRef.Legacy(PeacemakerCard.id)

  /** p1 holds Peacemaker and the People's Favor with 2 favor; p2 is the
    * Chancellor; p1 holds Ancient City with 2 warbands, p2 Broken Peaks
    * with 3 purple ones, and 3 bandits hold Buried Giant. Every board has
    * 3 warbands. */
  private def table: Table =
    Table.start.imperial(p2).turn(actor, Phase.Act)
      .legacy(actor, PeacemakerCard).peoplesFavor(Some(actor), 2)
      .warbandsAt(AncientCitySite, actor, 2)
      .warbandsAt(BrokenPeaksSite, p2, 3).bandits(BuriedGiantSite, 3)

  test("it returns the banner with its favor and leaves one warband on " +
      "every site and board, the holder's, the Empire's and the bandits'"):
    val ready = table.ready
    val done = accepted(use(ready, Peacemaker, source))
    val now = Look(after(done))
    assertEquals(now.peoplesFavorHolder, None)
    assertEquals(now.favorOnPeoplesFavor, 2)
    Vector(AncientCitySite, BrokenPeaksSite, BuriedGiantSite).foreach(site =>
      assertEquals(now.forces(site).total, 1, site.toString))
    after(done).game.current.players.foreach(player =>
      assertEquals(player.board.warbands, 1, player.player.toString))
    assertEquals(replayed(ready, done.events), Right(done.state))
    assertEquals(NoteText.said(Peacemaker, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${actor.value} returned the People's " +
        s"Favor with ${PeacemakerCard.id.value}", covers = false)))

  test("holding both banners, the holder chooses which; the other stays"):
    val ready = table.darkestSecret(Some(actor), 1).ready
    val done = useAndAnswer(ready, Peacemaker, source, Peacemaker.decisionId,
      DecisionOptionRef.Banner(Banner.DarkestSecret))
    val now = Look(after(done.answered))
    assertEquals(now.darkestSecretHolder, None)
    assertEquals(now.secretsOnDarkestSecret, 1)
    assertEquals(now.peoplesFavorHolder, Some(actor))

  test("a walk resumed at Peacemaker's question returns the chosen banner"):
    val ready = table.darkestSecret(Some(actor), 1).ready
    val started = accepted(use(ready, Peacemaker, source))
    assert(awaits(started, Peacemaker.decisionId))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      Peacemaker.decisionId, pick(DecisionOptionRef.Banner(Banner.DarkestSecret))))
    assertEquals(Look(after(done)).darkestSecretHolder, None)
    assertEquals(Look(after(done)).peoplesFavorHolder, Some(actor))

  test("it is not offered while the holder holds no banner"):
    val bare = Table.start.turn(actor, Phase.Act)
      .legacy(actor, PeacemakerCard).ready
    assert(!usableNow(bare).exists(_.power.id == Peacemaker.id))
    assertEquals(use(bare, Peacemaker, source).left.toOption,
      Some(OathViolation.StartUnavailable("You hold no banner")))
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.PeacemakerSuite"`
Expected: compile error, `Peacemaker` is not found.

- [ ] **Step 3: Implement**

`Peacemaker.scala` (the card object as printed, moved from holding, with
`cost = Cost.free`):

```scala
/** Peacemaker (legacy L04), ACTION: return a banner the holder holds to
  * the shared bank, its favor or secrets left on it, then remove every
  * warband but the last from each site and each player board, the
  * holder's included (Legacy in-play powers design, ruling 8).
  *
  * Holding both banners, the holder chooses which; holding one, nothing is
  * asked. The use is refused while they hold none. The return is required:
  * the removals follow only once the banner has gone ("If you do"). Each
  * removal is a `Kill`, as Ballot Box's "remove" is: Imperial and Exile
  * warbands return to their bank, bandits to the bandit supply. Warbands
  * on cards are neither at a site nor on a board and stay.
  *
  * Line: "p2 returned the People's Favor with Peacemaker" (ruling 13),
  * read from the return's step.
  */
case object Peacemaker extends PaidAction(PeacemakerCard.power):
  val decisionId: String = "power.peacemaker.banner"
  private val notes = NoteSet(id)
  /** "{p2} returned the {People's Favor} with {Peacemaker}". */
  val returned: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" returned the "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The banners `player` holds, in banner order. */
  def banners(ready: ReadyGame, player: PlayerId): Vector[Banner] =
    Banner.all.filter(banner =>
      BannerRules.holder(ready.game.current, banner).contains(player))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(banners(ready, player).isEmpty)("You hold no banner")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) =>
      val held = banners(live, player)
      Option.when(held.size > 1)(Decide(decisionId, player,
        DecisionQuery.ChooseOne(held.map(banner =>
          DecisionOption.Banner(DecisionOptionRef.Banner(banner))),
          heading = Some("Peacemaker: return which banner to the shared " +
            "bank?")))).toVector),
    BuildOps((live, pending) => chosen(live, player, pending).map(banner =>
      BannerToSharedBank.move(live, banner).toVector), required = true),
    Note(id, line(_, player)),
    BuildOps((live, _) => Right(thinned(live))))))

  /** The banner to return: the only one held, or the one chosen. */
  private def chosen(ready: ReadyGame, player: PlayerId,
      pending: PendingTree): Either[OathViolation, Banner] =
    banners(ready, player) match
      case Vector(only) => Right(only)
      case held => PowerAnswers.one(pending, decisionId).collect {
        case DecisionOptionRef.Banner(banner) if held.contains(banner) =>
          banner }.toRight(PowerAnswers.missing(decisionId))

  /** Every warband but one at each site in play and on each board. */
  private def thinned(ready: ReadyGame): Vector[CoreOperation] =
    val current = ready.game.current
    val atSites: Vector[CoreOperation] = current.map.inPlay.flatMap(site =>
      current.map.sites.get(site).map(_.forces).collect {
        case SiteForces.Occupied(kind, count) if count > 1 =>
          Kill(Piece.Warbands(kind, count - 1),
            PositionedLocation(Location.Site(site)))
      })
    val onBoards: Vector[CoreOperation] = current.players.flatMap(held =>
      PlayerForceKind.of(ready, held).filter(_ => held.board.warbands > 1)
        .map(kind => Kill(Piece.Warbands(kind, held.board.warbands - 1),
          PositionedLocation(Location.PlayArea(held.player)))))
    atSites ++ onBoards

  /** The banner the step before left the holder for the shared bank. */
  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    for
      (before, after) <- states.previous
      banner <- Banner.all.find(BannerToSharedBank.moved(before, after, _))
    yield returned.sourceless(NoteArg.Player(player), NoteArg.Banner(banner),
      NoteArg.Card(PeacemakerCard.id))
```

Imports: `distantlands.catalog.{ExecutableCatalog, Legacy, LegacyGoal,
PrintedPower}`, `distantlands.gameplay.actions.BannerRules`,
`distantlands.gameplay.powerresolver.QueuedStarts`,
`distantlands.gameplay.powers.PowerAnswers`,
`distantlands.gameplay.powers.action.PaidAction`,
`distantlands.gameplay.powers.setup.BannerToSharedBank`,
`distantlands.model._`.

`LegacyActionPowers.all = Vector(Ruthless, Peacemaker)`. Remove
`PeacemakerCard` from `HoldingLegacies.scala`.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.* distantlands.gameplay.cards.*"`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/legacies/Peacemaker.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/PeacemakerSuite.scala
git commit -m "feat(legacies): Peacemaker returns a banner and leaves one warband everywhere

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/legacies \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/PeacemakerSuite.scala
git log -3
```

### Task 5: Golden Key's ACTION

Written to R1 and R5.

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/GoldenKey.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/legacies/LegacyActionPowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `GoldenKeyCard`)
- Modify: `src/test/scala/distantlands/gameplay/PhasePowerSuite.scala`
  (import `GoldenKeyCard` from `gameplay.powers.legacies`)
- Test: `src/test/scala/distantlands/gameplay/powers/legacies/GoldenKeySuite.scala`

**Interfaces:**
- Consumes: `ChronicleLegacyRules.holder`, `.imperialRoles`.
- Produces: `GoldenKeyCard` (L12);
  `case object GoldenKey extends PaidAction(GoldenKeyCard.power)` with
  `decisionId = "power.golden-key.relic"`, `took: NoteKey`, and
  `peeker(ready: ReadyGame): Option[PlayerId]`, the seated Imperial
  holding Golden Key active, which Task 6 reads.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.powers.legacies

import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** Golden Key (L12): an Imperial holder burns a secret and a favor to take
  * a relic from the Reliquary (Legacy in-play powers rulings 2, 10 and
  * 13). */
class GoldenKeySuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val source = DecisionOptionRef.Legacy(GoldenKeyCard.id)

  /** p1, the Chancellor, in Act holding Golden Key active with 1 favor and
    * 1 secret, and 2 relics in the Reliquary. */
  private def table: Table =
    Table.start.imperial(actor).turn(actor, Phase.Act)
      .legacy(actor, GoldenKeyCard).favor(actor, 1).secrets(actor, faceUp = 1)
      .reliquaryFromDeck(2)

  private def offered(ready: ReadyGame): Boolean =
    usableNow(ready).exists(_.power.id == GoldenKey.id)

  test("an Imperial holder takes the chosen relic facedown, paying a " +
      "favor and a secret"):
    val ready = table.ready
    val reliquary = ready.game.campaign.reliquaryInPlay
    assert(offered(ready))
    val chosen = reliquary.last
    val done = useAndAnswer(ready, GoldenKey, source, GoldenKey.decisionId,
      DecisionOptionRef.Relic(chosen))
    val now = after(done.answered)
    assertEquals(now.game.campaign.reliquary, reliquary.init)
    assertEquals(now.game.current.player(actor).map(_.relics.filter(
      _.id == chosen).map(_.orientation)), Some(Vector(Orientation.FaceDown)))
    assertEquals((Look(now).favor(actor), Look(now).faceUpSecrets(actor)),
      (0, 0))
    assertEquals(replayed(ready, done.events), Right(done.answered.state))
    assertEquals(NoteText.said(GoldenKey, done.events), Vector(
      NoteText.Said(NoteKey.Used, s"${actor.value} took a relic from the " +
        s"Reliquary with ${GoldenKeyCard.id.value}", covers = false)))

  test("a walk resumed at Golden Key's question takes the chosen relic"):
    val ready = table.ready
    val chosen = ready.game.campaign.reliquaryInPlay.head
    val started = accepted(use(ready, GoldenKey, source))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      GoldenKey.decisionId, pick(DecisionOptionRef.Relic(chosen))))
    assert(Look(after(done)).relics(actor).contains(chosen))

  test("it is not offered while the Reliquary is empty"):
    val empty = Table.start.imperial(actor).turn(actor, Phase.Act)
      .legacy(actor, GoldenKeyCard).favor(actor, 1)
      .secrets(actor, faceUp = 1).ready
    assert(!offered(empty))
    assertEquals(use(empty, GoldenKey, source).left.toOption,
      Some(OathViolation.StartUnavailable("The Reliquary is empty")))

  test("an Exile holder's menu leaves it out, and a use is refused (R1)"):
    val exile = Table.start.turn(actor, Phase.Act)
      .legacy(actor, GoldenKeyCard).favor(actor, 1)
      .secrets(actor, faceUp = 1).reliquaryFromDeck(2).ready
    assert(!offered(exile))
    assert(use(exile, GoldenKey, source).isLeft)
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.GoldenKeySuite"`
Expected: compile error, `GoldenKey` is not found.

- [ ] **Step 3: Implement**

`GoldenKey.scala` (the card object as printed, moved from holding, with
`cost = Cost(favorBurnt = 1, secretBurnt = 1)`):

```scala
/** Golden Key (legacy L12): "If you are an Imperial: you may always peek
  * at the Reliquary. ACTION: Burn a secret and a favor to take a relic
  * from the Reliquary" (Legacy in-play powers design, rulings 2 and 10).
  *
  * Both halves need its holder to be an Imperial when they are used (R1):
  * the peek is a clause of `GamePresentationProjector.identifiesCard`,
  * which reads [[peeker]], and an Exile holder's menu leaves the ACTION
  * out. The ACTION takes any relic in the Reliquary the holder chooses,
  * facedown to their play area, as the Chancellor takes one at Setup. It
  * is not a Recover. The use is refused while the Reliquary in play is
  * empty, set aside included (R5).
  *
  * Line: "p3 took a relic from the Reliquary with Golden Key" (ruling
  * 13), which never names the relic.
  */
case object GoldenKey extends PaidAction(GoldenKeyCard.power):
  val decisionId: String = "power.golden-key.relic"
  private val notes = NoteSet(id)
  /** "{p3} took a relic from the Reliquary with {Golden Key}". */
  val took: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" took a relic from the Reliquary with "),
    NotePart.Arg(1)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The seated Imperial holding Golden Key active, read now (ruling 2). */
  def peeker(ready: ReadyGame): Option[PlayerId] =
    ChronicleLegacyRules.holder(ready, GoldenKeyCard.id,
      ChronicleLegacyRules.imperialRoles)

  override def listed(ready: ReadyGame, player: PlayerId): Boolean =
    peeker(ready).contains(player)

  override def usable(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef): Boolean =
    peeker(ready).contains(player)

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(ready.game.campaign.reliquaryInPlay.isEmpty)(
      "The Reliquary is empty")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseOne(live.game.campaign.reliquaryInPlay.map(relic =>
        DecisionOption.Relic(DecisionOptionRef.Relic(relic))),
        heading = Some("Golden Key: take a relic from the Reliquary"),
        confirmVerb = Some("Take"))))),
    BuildOps((live, pending) => take(live, player, pending), required = true),
    Note(id, _ => Some(took.sourceless(NoteArg.Player(player),
      NoteArg.Card(GoldenKeyCard.id)))))))

  private def take(ready: ReadyGame, player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.one(pending, decisionId).collect {
      case DecisionOptionRef.Relic(relic)
          if ready.game.campaign.reliquaryInPlay.contains(relic) =>
        Vector[CoreOperation](Move(Piece.Card(relic),
          PositionedLocation(Location.Reliquary),
          PositionedLocation(Location.PlayArea(player)),
          resultingOrientation = Some(Orientation.FaceDown)))
    }.toRight(PowerAnswers.missing(decisionId))
```

Imports: as Peacemaker's, less `BannerRules` and `BannerToSharedBank`,
plus `distantlands.gameplay.chronicle.ChronicleLegacyRules`.

`LegacyActionPowers.all = Vector(Ruthless, Peacemaker, GoldenKey)`.
Remove `GoldenKeyCard` from `HoldingLegacies.scala`; update
`PhasePowerSuite`'s import.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.* distantlands.gameplay.PhasePowerSuite distantlands.gameplay.cards.*"`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/legacies/GoldenKey.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/GoldenKeySuite.scala
git commit -m "feat(legacies): Golden Key's Imperial holder takes a relic from the Reliquary

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/legacies \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/PhasePowerSuite.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/GoldenKeySuite.scala
git log -3
```

### Task 6: The Reliquary in the shared bank and Golden Key's peek

Written to R1 and R5.

**Files:**
- Modify: `src/main/scala/distantlands/application/GamePresentationProjector.scala:321-322,406-410`
  (the doc bullet and the `CardContainer.Reliquary` clause; add `reliquary`)
- Modify: `src/main/scala/distantlands/application/GameProjection.scala:131-136`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala:512-546`
  (`sharedBank`)
- Modify: `frontend/styles.css` (`.shared-reliquary`)
- Test: `src/test/scala/distantlands/application/ReliquaryViewSuite.scala`
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/ReliquaryBankSuite.scala`

**Interfaces:**
- Consumes: `GoldenKey.peeker`.
- Produces:
  - `GameProjection.reliquary: Vector[CardDetailsProjection] = Vector.empty`,
    wire field `"reliquary"`, absent decoding to empty.
  - `GamePresentationProjector.reliquary(ready: ReadyGame, viewer: Option[PlayerId]): Vector[CardDetailsProjection]`:
    one entry per relic in `reliquaryInPlay`, its face where
    `identifiesCard` allows, else `hiddenCard("relic")`.
  - The shared bank's `.shared-reliquary` block, absent while the
    projection's Reliquary is empty.

- [ ] **Step 1: Write the failing tests**

`ReliquaryViewSuite.scala`:

```scala
package distantlands.application

import distantlands.gameplay.powers.legacies.GoldenKeyCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.protocol.projection.GameProjectionCodec
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** The Reliquary in the shared bank, facedown, and Golden Key's peek
  * (Legacy in-play powers rulings 9 and 10). */
class ReliquaryViewSuite extends munit.FunSuite:
  private val projector = new GameProjector(catalog)
  private val presentation = new GamePresentationProjector(catalog)
  private def loaded(ready: ReadyGame) = LoadedGame(OathState.Ready(ready), 1L)
  private def view(ready: ReadyGame, viewer: Option[PlayerId]) =
    viewer.fold(projector.projectPublic("g", loaded(ready)))(player =>
      projector.project("g", loaded(ready), player))

  /** p1, the Chancellor, holds Golden Key active in Act, and the Reliquary
    * holds two relics. */
  private val keyed = Table.start.imperial(p1).turn(p1, Phase.Act)
    .legacy(p1, GoldenKeyCard).reliquaryFromDeck(2).ready
  private val relics = keyed.game.campaign.reliquaryInPlay

  test("every viewer sees a non-empty Reliquary in the shared bank, " +
      "facedown, and only Golden Key's Imperial holder sees its faces"):
    assertEquals(view(keyed, Some(p1)).reliquary.map(card =>
      (card.cardId, card.hidden)), relics.map(id => (id.value, false)))
    Vector(Some(p2), None).foreach { viewer =>
      val shown = view(keyed, viewer)
      assertEquals(shown.reliquary.map(_.hidden), Vector(true, true))
      val wire = GameProjectionCodec.encode(shown)
      relics.foreach(id => assert(!wire.contains(s"\"${id.value}\""),
        s"$viewer sees ${id.value}"))
    }

  test("the peek is gone once its holder becomes an Exile (ruling 2)"):
    val exiled = keyed.updateCampaign(campaign => campaign.copy(lineages =
      campaign.lineages.view.mapValues(lineage => lineage.copy(
        role = Role.Exile)).toMap))
    assertEquals(view(exiled, Some(p1)).reliquary.map(_.hidden),
      Vector(true, true))

  test("an empty or set-aside Reliquary is not shown (ruling 9; R5)"):
    val empty = Table.start.turn(p1, Phase.Act).ready
    assertEquals(view(empty, None).reliquary, Vector.empty)
    val setAside = Table.from(keyed).reliquarySetAside.ready
    assertEquals(view(setAside, Some(p1)).reliquary, Vector.empty)

  test("Golden Key's holder reads a Reliquary option as its relic"):
    val rows = new DecisionOptionRows(catalog, presentation)
    val option = DecisionOption.Relic(DecisionOptionRef.Relic(relics.head))
    val index = keyed.game.cardIndex.toOption
    assert(rows.project(keyed, Some(p1), index, option)
      .flatMap(_.card).exists(!_.hidden))
    assertEquals(rows.project(keyed, Some(p2), index, option)
      .flatMap(_.card).map(_.cardId), None)
```

A relic option its viewer may not identify projects no row, as a
dormant legacy's does in `LegacyIdentitySuite`.

`ProjectionProtocolSuite`, after the legacies round-trip test:

```scala
  test("the Reliquary round-trips, and a payload without it decodes to " +
      "none"):
    val relic = known.copy(cardId = "R19", cardKind = "relic")
    val held = projection.copy(reliquary =
      Vector(relic, hidden.copy(cardKind = "relic")))
    assertRoundTrips(held)
    val raw = ujson.read(GameProjectionCodec.encode(held))
    raw.obj.remove("reliquary")
    assertEquals(GameProjectionCodec.decode(ujson.write(raw))
      .map(_.reliquary), Right(Vector.empty))
```

`ReliquaryBankSuite.scala`:

```scala
package distantlands.frontend

import distantlands.frontend.Dom.{all, firstMatch, one}

/** The Reliquary in the shared bank (Legacy in-play powers ruling 9). */
class ReliquaryBankSuite extends munit.FunSuite:
  private val back = CardDetails("hidden", "relic", "Facedown relic",
    orientation = Some("face-down"), hidden = true)
  private val known = CardDetails("R19", "relic", "Book of Records",
    orientation = Some("face-down"))
  private def bank(relics: CardDetails*) =
    val world = WorldBoardRenderer.world(TestProjections.actingProjection()
      .copy(reliquary = relics.toVector), BoardOptions.none)
    one(world, ".table-edge > .shared-bank")

  test("the shared bank shows a non-empty Reliquary, its relics facedown"):
    val reliquary = one(bank(back, back), ".shared-reliquary")
    assertEquals(reliquary.getAttribute("aria-label"), "Reliquary")
    assertEquals(all(reliquary, ".card-back-letter").map(_.textContent),
      Vector("R", "R"))

  test("a relic its viewer may peek at is drawn knowable, by its name"):
    val reliquary = one(bank(known), ".shared-reliquary")
    assertEquals(all(reliquary, ".card-face-knowable").size, 1)
    assert(reliquary.textContent.contains("Book of Records"))

  test("an empty Reliquary is not shown"):
    assertEquals(firstMatch(bank(), ".shared-reliquary"), None)
```

`CardFace.render` draws a hidden card's back letter as
`.card-back-letter`, as the legacy chip does.

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.application.ReliquaryViewSuite" "sharedJVM/testOnly distantlands.protocol.ProjectionProtocolSuite" "frontend/testOnly distantlands.frontend.ReliquaryBankSuite"`
(use the project names `build.sbt` gives the shared and frontend
modules). Expected: compile errors, `reliquary` is not a member of
`GameProjection`.

- [ ] **Step 3: Implement**

`GameProjectionDto.scala`, after `publicLegacies`:

```scala
    /** The Reliquary in play, facedown, one entry per relic: a face only
      * where its viewer may peek (Legacy in-play powers rulings 9 and 10);
      * empty while it holds none or is set aside. */
    ,reliquary: Vector[CardDetailsProjection] = Vector.empty
```

`GameProjectionCodec.scala`: add `"reliquary"` to the field set after
`"publicLegacies"`; encode
`"reliquary" -> encoded(value.reliquary)(WorldProjectionCodec.encodeCard)`;
decode as `publicLegacies` is decoded, defaulting to empty, and pass
`reliquary` last to `GameProjection(...)`.

`GamePresentationProjector.scala`: the doc bullet becomes "The Reliquary
is named to the Chancellor in Setup, where they pick one of its relics
(Empire ruling R29), and to Golden Key's Imperial holder whenever they
look (Legacy in-play powers ruling 10)." The clause:

```scala
      // The Chancellor chooses a Reliquary relic in Setup (CR p. 8, Empire
      // ruling R29); Golden Key's Imperial holder may always peek at it
      // (Legacy in-play powers rulings 2 and 10). Nobody else sees it.
      case CardContainer.Reliquary =>
        viewer.exists(player =>
          (ready.game.current.turn.phase == Phase.Setup &&
            Imperials.chancellor(ready).contains(player)) ||
            GoldenKey.peeker(ready).contains(player))
```

and, after `publicLegacies`:

```scala
  /** The Reliquary in play as `viewer` may see it (Legacy in-play powers
    * rulings 9 and 10): facedown, a face where `identifiesCard` names the
    * relic, a relic back elsewhere. */
  def reliquary(ready: ReadyGame, viewer: Option[PlayerId])
      : Vector[CardDetailsProjection] =
    ready.game.campaign.reliquaryInPlay.map(id =>
      if identifiesCard(ready, viewer, id, Some(Orientation.FaceDown),
          CardContainer.Reliquary) then
        cardDetails(id, Some(Orientation.FaceDown), hidden = false)
      else hiddenCard("relic"))
```

Import `distantlands.gameplay.powers.legacies.GoldenKey`.

`GameProjection.scala`, in the `.copy(...)` beside `publicLegacies`:
`reliquary = presentation.reliquary(context.ready, context.viewer),`.

`WorldBoardRenderer.sharedBank`, after the banners and before the
foundations:

```scala
    // The Reliquary, facedown, while it holds a relic (Legacy in-play
    // powers ruling 9): a face only where its viewer may peek. Each relic
    // is keyed by its card when known, so Golden Key's question marks it.
    if value.reliquary.nonEmpty then
      val reliquary = element("div", "shared-reliquary")
      reliquary.setAttribute("aria-label", "Reliquary")
      reliquary.appendChild(text("h4", "shared-reliquary-label", "Reliquary"))
      value.reliquary.foreach(card => reliquary.appendChild(options.card(
        CardFace.render(card), card,
        Option.unless(card.hidden)(BoardOptions.cardKey(card)).toVector)))
      section.appendChild(reliquary)
```

`styles.css`, beside `.shared-banners`: a `.shared-reliquary` row that
wraps its cards with the banners' gap, and a `.shared-reliquary-label`
in the bank's label style. Copy the two declarations `.shared-banners`
and `.public-legacies-label` use; add no new colour.

- [ ] **Step 4: Run them and see them pass**

Run the three suites of Step 2, then `./sbtw "frontend/test"` and
`./sbtw "testOnly distantlands.application.*"`.
Expected: all pass; no existing projection snapshot changes, since an
empty Reliquary encodes as an empty array only where the codec writes
empty arrays (check `git status --short src/test/resources`; a changed
snapshot is reviewed line by line and committed here).

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/application/ReliquaryViewSuite.scala \
  frontend/src/test/scala/distantlands/frontend/ReliquaryBankSuite.scala
git commit -m "feat(presentation): show the Reliquary in the shared bank, its faces to Golden Key's holder

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/application/GamePresentationProjector.scala \
  src/main/scala/distantlands/application/GameProjection.scala \
  shared/src/main/scala/distantlands/protocol/projection/GameProjectionDto.scala \
  shared/src/main/scala/distantlands/protocol/projection/GameProjectionCodec.scala \
  frontend/src/main/scala/distantlands/frontend/WorldBoardRenderer.scala \
  frontend/styles.css \
  src/test/scala/distantlands/application/ReliquaryViewSuite.scala \
  shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala \
  frontend/src/test/scala/distantlands/frontend/ReliquaryBankSuite.scala
git log -3
```

### Task 7: High Priest's ACTION

Written to R3 and R4.

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/HighPriest.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/legacies/LegacyActionPowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `HighPriestCard`)
- Test: `src/test/scala/distantlands/gameplay/powers/legacies/HighPriestSuite.scala`

**Interfaces:**
- Produces: `HighPriestCard` (L21);
  `case object HighPriest extends PaidAction(HighPriestCard.power)` with
  `decisionId = "power.high-priest.secrets"`, `moved: NoteKey`. Its
  WHEN NEGOTIATING half is slice 2's (ruling 19); this object's doc says
  so.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.powers.legacies

import distantlands.gameplay.powers.NoteText
import distantlands.model._
import distantlands.testkit.{Look, PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** High Priest (L21), ACTION: move secrets from the Darkest Secret to your
  * board (Legacy in-play powers rulings 11 and 13). */
class HighPriestSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val source = DecisionOptionRef.Legacy(HighPriestCard.id)
  private def table(secrets: Int = 3): Table =
    Table.start.turn(actor, Phase.Act).legacy(actor, HighPriestCard)
      .secrets(actor, faceUp = 0).darkestSecret(Some(actor), secrets)
  private def offered(ready: ReadyGame): Boolean =
    usableNow(ready).exists(_.power.id == HighPriest.id)
  private def amount(n: Int) = DecisionAnswer.ChooseAmountAnswer(n)

  test("the holder chooses from 1 to the secrets on the Darkest Secret, " +
      "and that many move to their board"):
    val ready = table().ready
    assert(offered(ready))
    val started = accepted(use(ready, HighPriest, source))
    assert(awaits(started, HighPriest.decisionId))
    val done = accepted(answer(started, actor, HighPriest.decisionId,
      amount(2)))
    val now = Look(after(done))
    assertEquals((now.secretsOnDarkestSecret, now.faceUpSecrets(actor)),
      (1, 2))
    assertEquals(now.darkestSecretHolder, Some(actor))
    assertEquals(replayed(ready, started.events ++ done.events),
      Right(done.state))
    assertEquals(NoteText.said(HighPriest, started.events ++ done.events),
      Vector(NoteText.Said(NoteKey.Used, s"${actor.value} moved 2 secrets " +
        s"from the Darkest Secret with ${HighPriestCard.id.value}",
        covers = false)))

  test("an amount past the secrets on the banner is refused"):
    val started = accepted(use(table().ready, HighPriest, source))
    assert(answer(started, actor, HighPriest.decisionId, amount(4)).isLeft)

  test("a walk resumed at High Priest's question moves the chosen amount"):
    val ready = table().ready
    val started = accepted(use(ready, HighPriest, source))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      HighPriest.decisionId, amount(3)))
    assertEquals(Look(after(done)).secretsOnDarkestSecret, 0)

  test("it is not offered without the Darkest Secret, or with none on it"):
    val without = Table.start.turn(actor, Phase.Act)
      .legacy(actor, HighPriestCard).darkestSecret(Some(Table.p2), 2).ready
    assert(!offered(without))
    assertEquals(use(without, HighPriest, source).left.toOption,
      Some(OathViolation.StartUnavailable(
        "You do not hold the Darkest Secret")))
    val bare = table(secrets = 0).ready
    assert(!offered(bare))
    assertEquals(use(bare, HighPriest, source).left.toOption,
      Some(OathViolation.StartUnavailable(
        "The Darkest Secret holds no secrets")))
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.HighPriestSuite"`
Expected: compile error, `HighPriest` is not found.

- [ ] **Step 3: Implement**

`HighPriest.scala` (the card object as printed, moved from holding):

```scala
/** High Priest (legacy L21), its ACTION: "If you have the Darkest Secret,
  * you may move any number of secrets from the Darkest Secret to your
  * player board" (Legacy in-play powers design, ruling 11).
  *
  * The use is refused unless the holder holds the Darkest Secret with a
  * secret on it. The holder chooses an amount from 1 to the secrets on
  * it, always asked, and that many move to their board faceup with a
  * `Take`, so a restriction on taking applies. Its WHEN NEGOTIATING half
  * is slice 2's (ruling 19).
  *
  * Line: "p2 moved 3 secrets from the Darkest Secret with High Priest"
  * (R3).
  */
case object HighPriest extends PaidAction(HighPriestCard.power):
  val decisionId: String = "power.high-priest.secrets"
  private val notes = NoteSet(id)
  /** "{p2} moved {3 secrets} from the Darkest Secret with {High Priest}". */
  val moved: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" moved "), NotePart.Arg(1),
    NotePart.Text(" from the Darkest Secret with "), NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  private def holds(ready: ReadyGame, player: PlayerId): Boolean =
    BannerRules.holder(ready.game.current, Banner.DarkestSecret)
      .contains(player)

  private def secrets(ready: ReadyGame): Int =
    ready.game.current.banners.darkestSecret.secrets

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    if !holds(ready, player) then Some("You do not hold the Darkest Secret")
    else Option.when(secrets(ready) == 0)(
      "The Darkest Secret holds no secrets")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseAmount(1, secrets(live),
        Some("High Priest: move how many secrets to your board?"),
        "Move secrets")))),
    BuildOps((live, pending) => move(live, player, pending), required = true),
    Note(id, line(_, player)))))

  private def move(ready: ReadyGame, player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    PowerAnswers.amount(pending, decisionId)
      .filter(amount => amount >= 1 && amount <= secrets(ready))
      .map(amount => Vector[CoreOperation](Take(Piece.Secrets(amount),
        player, Location.OnBanner(Banner.DarkestSecret),
        Location.PlayArea(player))))
      .toRight(PowerAnswers.missing(decisionId))

  /** The secrets the step before took from the banner. */
  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    for
      (before, after) <- states.previous
      count = secrets(before) - secrets(after)
      if count > 0
    yield moved.sourceless(NoteArg.Player(player),
      NoteArg.Amount(count, NoteUnit.Secret), NoteArg.Card(HighPriestCard.id))
```

Imports as Peacemaker's, less `BannerToSharedBank`.

`LegacyActionPowers.all = Vector(Ruthless, Peacemaker, GoldenKey,
HighPriest)`. Remove `HighPriestCard` from `HoldingLegacies.scala`.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.* distantlands.gameplay.cards.*"`
Expected: all pass. The amount past 4 is refused by the walker's
`ChooseAmount` bounds.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/legacies/HighPriest.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/HighPriestSuite.scala
git commit -m "feat(legacies): High Priest moves secrets from the Darkest Secret to its holder

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/legacies \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/HighPriestSuite.scala
git log -3
```

### Task 8: The Ear's ACTION

Written to R2, R3 and R4.

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/legacies/TheEarAction.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/legacies/LegacyActionPowers.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/TheEar.scala:20-32`
  (doc: the ACTION half is `TheEarAction`)
- Test: `src/test/scala/distantlands/gameplay/powers/legacies/TheEarActionSuite.scala`

**Interfaces:**
- Consumes: `TheEarCard` (`gameplay/chronicle/legacies`).
- Produces: `case object TheEarAction extends PaidAction(TheEarCard.power)`
  with `decisionId = "power.the-ear.site"`, `peeked: NoteKey`,
  `sites(ready: ReadyGame, player: PlayerId): Vector[SiteId]`.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.powers.legacies

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.TheEarCard
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.action.BrassHorseCard
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.economy.CupOfPlentyCard
import distantlands.model._
import distantlands.testkit.{PhasePowerHarness, Table}
import distantlands.testkit.Unwrap.accepted

/** The Ear (L19), ACTION: peek at the relics at any site (Legacy in-play
  * powers rulings 12 and 13). */
class TheEarActionSuite extends munit.FunSuite:
  import PhasePowerHarness._

  private val actor = Table.p1
  private val source = DecisionOptionRef.Legacy(TheEarCard.id)
  private val peaks = DecisionOptionRef.Site(BrokenPeaksSite.id)

  /** p1 at Ancient City holds The Ear active; Broken Peaks holds Sticky
    * Fire and Brass Horse, Ancient City holds Cup of Plenty. */
  private def table: Table =
    Table.start.turn(actor, Phase.Act).pawn(actor, AncientCitySite)
      .legacy(actor, TheEarCard).relicAt(StickyFireCard, BrokenPeaksSite)
      .relicAt(BrassHorseCard, BrokenPeaksSite)
      .relicAt(CupOfPlentyCard, AncientCitySite)
  private def offered(ready: ReadyGame): Boolean =
    usableNow(ready).exists(_.power.id == TheEarAction.id)
  private def known(ready: ReadyGame, site: SiteId): Vector[RelicId] =
    ready.knowledge.siteRelics.getOrElse(actor, Map.empty)
      .getOrElse(site, Vector.empty)

  test("it is free, offers every site with a relic the holder has not " +
      "seen, and peeks at all the chosen site's relics (R2)"):
    val ready = table.ready
    assert(offered(ready))
    val started = accepted(use(ready, TheEarAction, source))
    assertEquals(PhasePowerHarness.offered(started, actor).map(_.toSet),
      Some(Set("site" -> AncientCitySite.id.value,
        "site" -> BrokenPeaksSite.id.value)))
    val done = accepted(answer(started, actor, TheEarAction.decisionId,
      pick(peaks)))
    assertEquals(known(after(done), BrokenPeaksSite.id).toSet,
      Set(StickyFireCard.id, BrassHorseCard.id))
    assertEquals(after(done).game.current.player(actor).map(_.board.favor),
      ready.game.current.player(actor).map(_.board.favor))
    assertEquals(NoteText.said(TheEarAction, started.events ++ done.events),
      Vector(NoteText.Said(NoteKey.Used, s"${actor.value} peeked at the " +
        s"relics at ${BrokenPeaksSite.id.value} with " +
        s"${TheEarCard.id.value}", covers = true)))

  test("the holder keeps knowing them after the action, so a second use " +
      "offers only the site still unseen"):
    val peeked = after(accepted(answer(accepted(use(table.ready,
      TheEarAction, source)), actor, TheEarAction.decisionId, pick(peaks))))
    val again = accepted(use(peeked, TheEarAction, source))
    assertEquals(PhasePowerHarness.offered(again, actor),
      Some(Vector("site" -> AncientCitySite.id.value)))

  test("a site whose relics the holder knows is not offered, and with " +
      "none left it is not offered at all"):
    val seen = table.knowsRelicAt(actor, StickyFireCard, BrokenPeaksSite)
      .knowsRelicAt(actor, BrassHorseCard, BrokenPeaksSite)
    val started = accepted(use(seen.ready, TheEarAction, source))
    assertEquals(PhasePowerHarness.offered(started, actor),
      Some(Vector("site" -> AncientCitySite.id.value)))
    val all = seen.knowsRelicAt(actor, CupOfPlentyCard, AncientCitySite).ready
    assert(!offered(all))
    assertEquals(use(all, TheEarAction, source).left.toOption,
      Some(OathViolation.StartUnavailable(
        "No site holds a relic you have not seen")))

  test("a walk resumed at The Ear's question peeks at the chosen site"):
    val ready = table.ready
    val started = accepted(use(ready, TheEarAction, source))
    val resumed = accepted(replayed(ready, started.events))
    val done = accepted(answer(OathTransition(resumed, Vector.empty), actor,
      TheEarAction.decisionId, pick(peaks)))
    assertEquals(known(after(done), BrokenPeaksSite.id).size, 2)
```

A relic that leaves the site carries its peekers' knowledge with it
(`CardKnowledgeMoves.carry`), which its own suite covers; this suite
does not repeat it.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.TheEarActionSuite"`
Expected: compile error, `TheEarAction` is not found.

- [ ] **Step 3: Implement**

`TheEarAction.scala`:

```scala
package distantlands.gameplay.powers.legacies

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.chronicle.legacies.TheEarCard
import distantlands.gameplay.powerresolver.QueuedStarts
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.PaidAction
import distantlands.model._

/** The Ear (legacy L19), its ACTION half: "You may peek at the relics at
  * any site" (Legacy in-play powers design, ruling 12). Its card and its
  * Chronicle half are `TheEarCard` and `TheEar`.
  *
  * Free. The holder picks a site in play with a relic missing from their
  * recorded knowledge (`knowledge.siteRelics`), their own site included
  * until they peek it (R2), and peeks at all its relics. They know them
  * while they stay at that site, as with the minor peek; a relic that
  * moves carries its knowledge as `CardKnowledgeMoves` carries it. The
  * use is refused while no site qualifies.
  *
  * Line: "p2 peeked at the relics at Deep Woods with The Ear" (R3),
  * public, never naming the relics, and covering the peeks' lines.
  */
case object TheEarAction extends PaidAction(TheEarCard.power):
  val decisionId: String = "power.the-ear.site"
  private val notes = NoteSet(id)
  /** "{p2} peeked at the relics at {site} with {The Ear}". */
  val peeked: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" peeked at the relics at "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] = Set(decisionId)

  /** The sites in play holding a relic `player` has not peeked, in map
    * order. */
  def sites(ready: ReadyGame, player: PlayerId): Vector[SiteId] =
    val map = ready.game.current.map
    val known = ready.knowledge.siteRelics.getOrElse(player, Map.empty)
    map.inPlay.filter(site => map.sites.get(site).exists(_.relics.exists(
      relic => !known.getOrElse(site, Vector.empty).contains(relic.id))))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] =
    Option.when(sites(ready, player).isEmpty)(
      "No site holds a relic you have not seen")

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef)
      : Either[OathViolation, Operation] = Right(Sequence(Vector[Operation](
    Branch((live, _) => Vector(Decide(decisionId, player,
      DecisionQuery.ChooseOne(sites(live, player).map(site =>
        DecisionOption.Site(DecisionOptionRef.Site(site))),
        heading = Some("The Ear: peek at the relics at which site?"))))),
    BuildOps((live, pending) => peeks(live, player, pending),
      required = true),
    Note(id, line(_, player), covers = true))))

  private def chosen(ready: ReadyGame, player: PlayerId,
      answer: Option[DecisionOptionRef]): Option[SiteId] =
    answer.collect { case DecisionOptionRef.Site(site)
      if sites(ready, player).contains(site) => site }

  private def peeks(ready: ReadyGame, player: PlayerId, pending: PendingTree)
      : Either[OathViolation, Vector[CoreOperation]] =
    chosen(ready, player, PowerAnswers.one(pending, decisionId))
      .toRight(PowerAnswers.missing(decisionId)).map(site =>
        ready.game.current.map.sites.get(site).toVector.flatMap(_.relics)
          .map(relic => Peek(player, relic.id, Location.Site(site))))

  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    PowerAnswers.last(states.answered, decisionId).collect {
      case DecisionOptionRef.Site(site) => peeked.sourceless(
        NoteArg.Player(player), NoteArg.Site(site),
        NoteArg.Card(TheEarCard.id))
    }
```

`LegacyActionPowers.all = Vector(Ruthless, Peacemaker, GoldenKey,
HighPriest, TheEarAction)`. In `TheEar.scala`'s doc, replace "Its ACTION
half waits for the in-play batch (R7)." with "Its ACTION half is
`TheEarAction` (Legacy in-play powers ruling 12)."

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.legacies.* distantlands.gameplay.chronicle.legacies.TheEarSuite distantlands.gameplay.powers.*CatalogSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite distantlands.gameplay.powers.LogNoteCatalogSuite"`
Expected: all pass. `PowerCatalogUniquenessSuite` passes: The Ear's id is
once in each catalog.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/legacies/TheEarAction.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/TheEarActionSuite.scala
git commit -m "feat(legacies): The Ear's holder peeks at the relics at any site

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/legacies \
  src/main/scala/distantlands/gameplay/chronicle/legacies/TheEar.scala \
  src/test/scala/distantlands/gameplay/powers/legacies/TheEarActionSuite.scala
git log -3
```

### Task 9: The golden log, the docs and the gate

**Files:**
- Create: `src/test/scala/distantlands/application/gamelog/LegacyActionLogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/GoldenScripts.scala`
- Create: `src/test/resources/gamelog/legacy-actions.actor.log`,
  `src/test/resources/gamelog/legacy-actions.other.log` (generated)
- Modify: `docs/rules/ambiguities.md`,
  `docs/rules/implementation-traceability.md`,
  `docs/rules/rules-reference.md`
- Modify: `docs/superpowers/specs/2026-11/2026-11-14-legacy-in-play-powers-design.md`
  (status line; ruling 6's `payable` note)
- Modify: `docs/superpowers/specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md`

**Interfaces:**
- Produces: `LegacyActionLogScripts.named: Vector[(String, () => Script)]`
  with `"legacy-actions"`.

- [ ] **Step 1: Write the script**

```scala
package distantlands.application.gamelog

import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.TheEarCard
import distantlands.gameplay.powers.campaign.StickyFireCard
import distantlands.gameplay.powers.economy.DefameCard
import distantlands.gameplay.powers.legacies.{GoldenKey, GoldenKeyCard,
  HighPriest, HighPriestCard, Peacemaker, PeacemakerCard, Ruthless,
  RuthlessCard, TheEarAction}
import distantlands.model._
import distantlands.testkit.MenuPlay
import distantlands.testkit.Table.p1
import LogScriptSupport.{atTable, tableWithOnePawnPerSite}

/** The legacies' ACTIONs (Legacy in-play powers design, slice 1), one Act
  * of p1, the Chancellor: Ruthless buries Defame at Ancient City, Golden
  * Key takes a relic from the Reliquary, High Priest moves a secret from
  * the Darkest Secret, The Ear peeks at Broken Peaks' relic, and
  * Peacemaker returns the banner it is asked about, each question answered
  * by default. Reached through `GoldenScripts`. */
object LegacyActionLogScripts:
  def legacyActions(using munit.Location): Script =
    val table = tableWithOnePawnPerSite.imperial(p1).turn(p1, Phase.Act)
      .legacy(p1, RuthlessCard).legacy(p1, GoldenKeyCard)
      .legacy(p1, HighPriestCard).legacy(p1, TheEarCard)
      .legacy(p1, PeacemakerCard)
      .favor(p1, 3).secrets(p1, faceUp = 1).reliquaryFromDeck(2)
      .denizen(DefameCard, AncientCitySite)
      .relicAt(StickyFireCard, BrokenPeaksSite)
      .darkestSecret(Some(p1), 2).peoplesFavor(Some(p1), 1)
      .banditsRefilled
    val (service, act) = atTable("legacy-actions", table)
    def use(power: PowerId, card: LegacyId) = MenuPlay.start(p1,
      ActionRef.UsePower(power), Vector(DecisionOptionRef.Legacy(card)))
    act.after(use(Ruthless.id, RuthlessCard.id),
      use(GoldenKey.id, GoldenKeyCard.id),
      use(HighPriest.id, HighPriestCard.id),
      use(TheEarAction.id, TheEarCard.id),
      use(Peacemaker.id, PeacemakerCard.id))
    Script("legacy-actions", service, p1)

  val named: Vector[(String, () => Script)] = Vector(
    "legacy-actions" -> (() => legacyActions))
```

`GoldenScripts.named`: append `++ LegacyActionLogScripts.named`.

- [ ] **Step 2: Generate and review the golden**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite -- *legacy-actions*"`
then `./sbtw "testOnly distantlands.application.gamelog.*"`.

Review both files line by line. Expected, for the actor: one section per
use, each with its one line and no "Chose" or "Used" line: "p1 buried
Defame with Ruthless", "p1 took a relic from the Reliquary with Golden
Key", "p1 moved 1 secret from the Darkest Secret with High Priest", "p1
peeked at the relics at Broken Peaks with The Ear" and "p1 returned the
People's Favor with Peacemaker" (the default answer is the first banner).
The other seat reads the same lines; no line names the Reliquary's relic
or Sticky Fire. Every other golden is unchanged (`git status --short
src/test/resources` lists only the two new files).

- [ ] **Step 3: Docs**

- `docs/rules/ambiguities.md`: a new section "## Legacy in-play powers
  rulings" after "Legacy Setup powers rulings", opening as that section
  does (a link to the design and the implementation rulings, and which
  items are which), then the design's rulings 1-13 as items 1-13, then
  the implementation rulings recorded for slice 1 as the next items.
- `docs/rules/implementation-traceability.md`: a row "Legacy ACTION
  powers" after "Legacy SETUP powers in the player choices", **Tested**,
  citing the design's slice 1 and the Minor actions rule, naming the
  suites (`gameplay/powers/legacies/RuthlessSuite`, `PeacemakerSuite`,
  `GoldenKeySuite`, `HighPriestSuite`, `TheEarActionSuite`,
  `application/ReliquaryViewSuite`, `frontend/ReliquaryBankSuite`, the
  `legacy-actions` golden) and noting that High Priest's WHEN
  NEGOTIATING half and The Ear's Chronicle half are elsewhere. In the
  Throne row, The Ear's note gains "its ACTION half is in the Legacy
  ACTION powers row".
- `docs/rules/rules-reference.md`: under "Minor actions", after "use an
  Action power;": "Digital: a seated player's active legacy is a source
  of its ACTION, named in the Act menu", citing the new section's items
  1 to 5 with a link to `ambiguities.md#legacy-in-play-powers-rulings`
  as the Setup notes cite theirs. Under "Later games" step 2, after the Reliquary pick: "Digital:
  the shared bank shows a non-empty Reliquary facedown, and Golden Key's
  Imperial holder may always peek at it (items 9 and 10)."
- The design: the status line notes slice 1 landed; under ruling 6, a
  sentence that `payable` needed no case, a legacy source being
  `PowerSourceRef.Card`.
- The implementation rulings file: confirm each slice 1 ruling names its
  tasks and files.

- [ ] **Step 4: Run the gate**

Run the gate (top of this plan), the sbt commands in the background.
Then start `oath-server`, reach an Act with a non-empty Reliquary (a
later game's, or the golden script's table through the dev server if it
has a fixture route), and read the shared bank's text: a "Reliquary"
heading with one relic back per relic, and the faces for Golden Key's
Imperial holder.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/application/gamelog/LegacyActionLogScripts.scala \
  src/test/resources/gamelog/legacy-actions.actor.log \
  src/test/resources/gamelog/legacy-actions.other.log
git commit -m "test(gamelog): a golden log of the legacies' ACTIONs, and their docs

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/test/scala/distantlands/application/gamelog/LegacyActionLogScripts.scala \
  src/test/scala/distantlands/application/gamelog/GoldenScripts.scala \
  src/test/resources/gamelog/legacy-actions.actor.log \
  src/test/resources/gamelog/legacy-actions.other.log \
  docs/rules/ambiguities.md docs/rules/implementation-traceability.md \
  docs/rules/rules-reference.md \
  docs/superpowers/specs/2026-11/2026-11-14-legacy-in-play-powers-design.md \
  docs/superpowers/specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md
git log -3
```

## Self-review notes

- Spec rulings 1-2: Task 2 (the seam keeps the seated holder's active
  legacies only; dormant, another player's and boxed tested), Tasks 3 and
  5 (dormant, boxed, Exile), Task 6 (the peek gone on becoming an Exile).
  Ruling 3: Task 2 (burnt cost paid as a card's), the Power band. Ruling
  4: every power here is chosen and carries its source. Ruling 5: slice
  2's (Imperial Tactics); access already treats legacies as the play
  area's. Ruling 6: Tasks 1 and 2. Ruling 7: Task 3. Ruling 8: Task 4.
  Ruling 9: Task 6. Ruling 10: Tasks 5 and 6. Ruling 11: Task 7. Ruling
  12: Task 8. Ruling 13: each power's line test and Task 9's golden.
- The slice 1 test list: offered and not offered (no denizen, no banner,
  empty Reliquary, no secrets on the Darkest Secret, no unidentified
  relic): Tasks 3, 4, 5, 7, 8. Burnt cost paid: Tasks 2, 3, 5. Ruthless
  burying a locked card and an edifice: Task 3 (the intact Hall of Debate
  is both). Peacemaker's warbands with bandits and Imperials: Task 4.
  Golden Key's peek to its Imperial holder only, gone as an Exile: Task 6.
  The Ear's knowledge lasting: Task 8. A dormant and a boxed legacy
  offering nothing: Tasks 2 and 3. Resume tests: Tasks 3, 4, 5, 7, 8. A
  golden log: Task 9.
- Names used across tasks: `LegacyActionFixture.boxed`,
  `LegacyActionPowers.all`, `Ruthless`, `Peacemaker`, `GoldenKey`
  (`.peeker`), `HighPriest`, `TheEarAction`, each `decisionId`,
  `GamePresentationProjector.reliquary`, `GameProjection.reliquary`.
- The Mouth's cost is corrected in Task 2; its card stays in holding until
  slice 4.
