# Legacy Chronicle Powers, Slice 2: Throne Denizens Implementation Plan

> Executed inline by the implementer.

**Goal:** Four legacies act on the Throne's denizens. Rival's holder keeps
any of the denizens Shape Empire would discard. Great Architect's Imperial
holder performs Build Edifice in place of the Throne holder and may build
any available edifice, not only the matching one. Revolutionary's holder,
after Build Edifice, discards any of the denizens at Imperial sites.
Reformer's holder, at the start of the Throne and only if they won, moves
and swaps denizens between sites they rule, ignoring locks, until Done.
The four cards count as implemented.

**Architecture:** The seam of slice 1 carries on. Changed steps ask
`ChronicleLegacyRules` when they are reached: Shape Empire reads
`protector`, Build Edifice reads `builder` and `anyEdifice`. Added steps
are `Transform`s on two new, empty Throne windows that append one `Branch`
of constant shape: Revolutionary after Build Edifice, Reformer at the
Throne's start, its passes a `Repeat` read when each pass is reached, as
Call for Help's are. The cards and powers live in
`gameplay/chronicle/legacies`, registered by `LegacyChroniclePowers.all`;
their lines are Throne procedure lines on the Throne's note id, listed in
`LegacyChroniclePowers.throneNoteKeys`.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Chronicle powers design](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md),
rulings 1-4 and 9-12, and section "Slice 2: Throne denizens".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-implementation-rulings.md)
R1-R6. Card text in `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
(Great Architect L07, Revolutionary L15, Rival L17, Reformer L35).
`docs/rules/chronicle.md`, the Throne, steps 1-3; Chronicle rulings 6, 9,
11, 12, 14 and 16 in `docs/rules/ambiguities.md`. Slice 1's plan
([slice 1](2026-11-13-legacy-chronicle-powers-slice1.md)) for the seam.

## Prerequisite

Slice 1 has landed (through `fea52c52e`). Check before Task 1:

```bash
grep -q "object ChronicleLegacyRules" src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala &&
grep -q "object LegacyChroniclePowers" src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala &&
grep -q "def through" src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala &&
echo ready
```

Expected: `ready`.

## Rulings

The plan's open questions were ruled on 2026-11-13 as implementation
rulings R7-R14 ([rulings](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-implementation-rulings.md));
R15 accepts that inserted windows shift a walk saved mid-Chronicle.

- **R7** (Tasks 1 and 5): Rival leaves holding with its whole text and
  counts as implemented; the ROADMAP setup item names its SETUP half. The
  design's slice 2 sentence is corrected.
- **R8** (Task 1): Rival protects every card Shape Empire would discard,
  ruined edifices included.
- **R9** (Task 3, low confidence): Revolutionary discards the denizens and
  ruined edifices at Imperial sites; intact edifices stay.
- **R10** (Task 4, low confidence): Reformer moves and swaps every card in
  a site's row, edifices included, intact and ruined. An edifice moves
  between sites by the existing `Move` (a site card keeps its state), so no
  new move kind is needed.
- **R11** (Task 4): a pass is two choose-ones: a card at a ruled site, or
  Done (first), offering only cards with a destination; then another ruled
  site with room (a move) or a card at another ruled site (a swap). A swap
  is two `Move`s, each card landing in the other's site's last slot.
- **R12** (Tasks 1, 3 and 4): the lines and window names as proposed:
  "p1 protected 2 denizens with Rival", "p2 discarded 3 denizens with
  Revolutionary", "p1 moved X to Y with Reformer", "p1 swapped X and Y
  with Reformer", decision lines, "1 denizen" for one; nothing for an empty
  choice; no Great Architect line. `ChronicleStartOfThrone`
  (`chronicle.throne.start-of-throne`) and `ChronicleAfterBuildEdifice`
  (`chronicle.throne.after-build-edifice`).
- **R13** (Tasks 1-4): the decision shapes as proposed. Rival and
  Revolutionary: one choose-many of zero to all, asked when there is a
  card; ids `chronicle.throne.legacy.rival.<player>`,
  `chronicle.throne.legacy.revolutionary.<player>`. Reformer:
  `chronicle.throne.legacy.reformer.<player>` and its `.to` destination,
  reused each pass and read from the latest answers. Great Architect keeps
  Build Edifice's per-suit decisions, owned by the builder. All narrated by
  the prefix `chronicle.throne.legacy.`. Reformer gets a resume test that
  parks mid-pass after a completed move and resumes to the same question.
- **R14** (Tasks 2 and 3): a burial returns favor to the buried card's own
  suit bank; a ruined edifice Revolutionary discards after Build Edifice
  stays set aside, and a test checks the stored Chronicle still passes game
  start's card accounting.

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, private members or locals, no
  non-exhaustive matches.
- Every Scala file stays at or under 800 lines
  (`scripts/check-architecture.py`).
- `gameplay` never reads a catalog `.text` or `rulesText`; it may read a
  card's `name`, `suit`, `goal`, `alters`.
- A new test names cards by their card objects.
- Every power reads its holder and role when its window is reached
  (rulings 1 and 2); a `Transform` appends a node of constant shape.
- No two decisions share an id: each new id is
  `chronicle.throne.legacy.<power>.<player>` (ruling 3); a `Repeat`'s
  passes reuse theirs and read the latest answer.
- Choosing several cards is one choose-many (ruling 4).
- Locks do not bind in the Chronicle Phase (Chronicle ruling 16), so
  Reformer's moves need no lock lifting.
- Nothing in the Throne is hidden; the Chronicle shows every legacy
  (Legacies R18).
- TDD, one commit per task; coverage at or above 92% statements and 84.4%
  branches; rewritten snapshots reviewed and committed in their task;
  `git status --short src/test/resources` otherwise clean.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`. Stage only
  this plan's files; run `git log -3` after each commit.

---

### Task 1: Rival

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/Rival.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
  (`all`, `throneNoteKeys`)
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `RivalCard`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
  (`protector`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpire.scala`
  (`discards`, `discardOf`, doc comment)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`
  (`noteKeys`)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
  (`chronicleThrone`)
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ChronicleWalk.scala`
  (`through` takes `after`)
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/RivalSuite.scala`
- Modify (imports): every test importing `RivalCard` from
  `distantlands.catalog.holding` (`ResolveLegaciesSuite`,
  `OfferCitizenshipSuite`, `LegacyIdentitySuite`, `GatekeeperSuite`,
  `SteadfastSuite`, `LegacyCardDataSuite` and any other `grep -rln
  RivalCard src/test` lists)
- Modify: `src/test/resources/powers/unimplemented.txt`,
  `src/test/resources/notes/declared-keys.txt` (rewritten)

**Interfaces:**
- Produces: `RivalCard` (text unchanged) and `Rival`, a `ContributingPower`
  hooking no window; `ChronicleLegacyRules.protector(state: ReadyGame):
  Option[PlayerId]`; `Rival.decisionId(player)` =
  `s"chronicle.throne.legacy.rival.${player.value}"`; `Rival.protectedWith:
  NoteKey` ("{0} protected {1} {denizen/denizens} with {2}");
  `ShapeEmpire.discardOf(catalog, site, region, card, actor):
  Option[Discard.Denizen | Discard.RuinedEdifice]`, the destination of a
  Shape Empire discard, which Task 3 reuses;
  `LegacyChroniclePowers.throneNoteKeys: Vector[NoteKey]`;
  `ChronicleWalk.through(rules, table, procedure, answers, after:
  TriggeredProcedureRef = ChronicleBegin)`, the procedure whose end the
  driver starts from (`ChronicleSun` starts the Throne on a
  `throneHeldByChancellorP1` table).

- [ ] **Step 1: Write the failing tests**

In `ChronicleWalk.through`, add the parameter `after:
TriggeredProcedureRef = TriggeredProcedureRef.ChronicleBegin` and pass it
to `rules.next` in place of `ChronicleBegin`.

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}

/** Rival (L17), its Throne half: "In Shape Empire, you may prevent any
  * number of denizens from being discarded." (ruling 9). p1, the
  * Chancellor, holds the Throne and the Empire rules Ancient City; two
  * denizens and a ruined edifice lie at sites it does not rule. */
class RivalSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def outside: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1)
    .denizen(ThreateningRoarCard, at = BrokenPeaksSite)
    .denizen(FaeMerchantCard, at = BuriedGiantSite)
    .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = AncientCitySite)

  private def throne(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def keeping(refs: DecisionOptionRef*)
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Rival.decisionId(p2) =>
      DecisionAnswer.ChooseManyAnswer(refs.toVector)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.throneNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone),
      keys, run.events).filter(said => keys.exists(_.name == said.key))
      .map(_.text)

  test("Rival is implemented"):
    assert(PowerImplementationStatus.implemented(RivalCard.power.id))

  test("the holder is offered every card Shape Empire would discard"):
    // Ancient City is Enduring, so its ruined edifice stays anyway.
    val run = throne(outside.legacy(p2, RivalCard))
    val asked = run.asked.find(_.decisionId == Rival.decisionId(p2)).get
    val query = asked.query.asInstanceOf[DecisionQuery.ChooseMany]
    assertEquals((asked.owner, query.min, query.max), (p2, 0, 2))
    assertEquals(query.options.map(_.ref).toSet, Set[DecisionOptionRef](
      DecisionOptionRef.Denizen(ThreateningRoarCard.id),
      DecisionOptionRef.Denizen(FaeMerchantCard.id)))

  test("the kept denizens stay and the rest are discarded as before " +
      "(ruling 9)"):
    val run = throne(outside.legacy(p2, RivalCard),
      keeping(DecisionOptionRef.Denizen(FaeMerchantCard.id)))
    assert(onMap(run.ready, FaeMerchantCard.id))
    assert(!onMap(run.ready, ThreateningRoarCard.id))
    assertEquals(lines(run), Vector(
      s"p2 protected 1 denizen with ${RivalCard.id.value}"))

  test("keeping none discards everything and writes no line"):
    val run = throne(outside.legacy(p2, RivalCard))
    assert(!onMap(run.ready, FaeMerchantCard.id))
    assertEquals(lines(run), Vector.empty)

  test("a ruined edifice outside the Empire may be kept"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = BuriedGiantSite)
      .legacy(p2, RivalCard)
    val run = throne(table,
      keeping(DecisionOptionRef.Edifice(SchoolOfVinesCard.id)))
    assert(onMap(run.ready, SchoolOfVinesCard.id))

  test("nothing to discard asks nothing, and a dormant Rival asks nothing " +
      "(ruling 1)"):
    val empire = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
    assert(!throne(empire.legacy(p2, RivalCard)).asked.exists(
      _.decisionId == Rival.decisionId(p2)))
    assert(!throne(outside.legacy(p2, RivalCard, active = false)).asked
      .exists(_.decisionId == Rival.decisionId(p2)))

  test("a walk resumed at Rival's question keeps what was chosen, after " +
      "the warband removal changed the map"):
    // Two Imperial warbands: Shape Empire first asks p1 to remove some.
    val table = outside.warbandsAt(BrokenPeaksSite, p1, 1)
      .legacy(p2, RivalCard)
    val removing: PartialFunction[Decide, DecisionAnswer] =
      case decide if decide.decisionId ==
          distantlands.gameplay.chronicle.ShapeEmpire.removeDecisionId =>
        DecisionAnswer.DistributeAnswer(Vector(
          DistributeAmount(DecisionOptionRef.Site(BrokenPeaksSite.id), 1),
          DistributeAmount(DecisionOptionRef.Site(AncientCitySite.id), 0)))
    val run = throne(table, removing.orElse(
      keeping(DecisionOptionRef.Denizen(ThreateningRoarCard.id))))
    assert(onMap(run.ready, ThreateningRoarCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.RivalSuite"`
Expected: FAIL to compile: `Not found: RivalCard` in the new package.

- [ ] **Step 3: Implement**

`Rival.scala`: move `RivalCard` verbatim (both halves' text; the SETUP
half stays uncovered, R7), then:

```scala
/** Rival (legacy L17), its Throne half: "In Shape Empire, you may prevent
  * any number of denizens from being discarded." Shape Empire asks
  * `ChronicleLegacyRules.protector` before its discard (Legacy Chronicle
  * powers design, ruling 9): the holder keeps any of the cards it would
  * discard, in one choose-many. Its SETUP half waits for the setup batch.
  * This power hooks no window; it is registered so the card counts as
  * implemented. Line: "p1 protected 2 denizens with Rival". */
case object Rival extends ContributingPower:
  val id: PowerId = RivalCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.legacy.rival.${player.value}"

  private val notes = NoteSet(ProcedureNotes.id(
    TriggeredProcedureRef.ChronicleThrone))
  val protectedWith: NoteKey = notes.key("rival-protected", Vector(
    NotePart.Arg(0), NotePart.Text(" protected "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Plural(1, "denizen", "denizens"),
    NotePart.Text(" with "), NotePart.Arg(2)))
  /** The Throne lines this power writes, on the Throne's note id. */
  def throneNoteKeys: Vector[NoteKey] = notes.declared
```

`ChronicleLegacyRules.protector(state) = holder(state, RivalCard.id)`.

`ShapeEmpire`: rename the private `discard` to a public `discardOf` (same
body). In `discards`, after computing `cards`, branch on
`ChronicleLegacyRules.protector(state)`:

```scala
    if cards.isEmpty then Vector.empty
    else ChronicleLegacyRules.protector(state) match
      case None => discarding(cards)
      case Some(rival) => Vector(
        Decide(Rival.decisionId(rival), rival, DecisionQuery.ChooseMany(0,
          cards.size, cards.map((_, card, _) => option(card)),
          heading = Some("Rival: you may keep any of these cards from " +
            "being discarded"),
          confirm = Some(CountedConfirm("Keep", "card", "cards",
            Some("Keep none"))))),
        Branch((_, pending) =>
          val kept = PowerAnswers.many(pending, Rival.decisionId(rival))
            .getOrElse(Vector.empty)
          val keptCards = cards.filter((_, card, _) => kept.contains(ref(card)))
          discarding(cards.filterNot(keptCards.contains)) ++
            Option.when(keptCards.nonEmpty)(Note(noteId, _ => Some(
              Rival.protectedWith.sourceless(NoteArg.Player(rival),
                NoteArg.Number(keptCards.size),
                NoteArg.Card(RivalCard.id))))).toVector))
```

where `cards` keeps each card's id as a `CardId`, `ref(card)` is
`DecisionOptionRef.Denizen` for a `DenizenId` and `DecisionOptionRef.Edifice`
for an `EdificeId`, `option` the matching `DecisionOption`, and
`discarding(cards)` is the existing body (one required `BuildOps`, then
one `discarded` line per card), empty when `cards` is. The Branch reads
only the `cards` its outer Branch computed from the state at the question,
so a walk resumed there finds the same options. Drop "they arrive with
legacy powers through this step's window" from the doc comment and name
Rival there.

`LegacyChroniclePowers`: `all :+ Rival`; `throneNoteKeys =
Rival.throneNoteKeys`. `ChronicleThroneProcedure.noteKeys` adds
`LegacyChroniclePowers.throneNoteKeys`. `ProcedureLog.chronicleThrone`:
`Rival.protectedWith -> LogKind.Decision` and the prefix
`"chronicle.throne.legacy."`.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.gameplay.cards.CardCatalogSuite distantlands.catalog.* distantlands.application.*"`
Expected: PASS except the snapshot suites.
Run: `UNIMPLEMENTED_SNAPSHOT=write NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
and read the diff: `legacy.rival` removed; `procedure.chronicle-throne
rival-protected` added.
Run: `./sbtw test`. Expected: PASS; only the two snapshots in
`git status --short src/test/resources`.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/application/gamelog/ProcedureLog.scala \
  src/test/scala src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt
git commit -m "feat(legacies): Rival keeps denizens from Shape Empire's discard

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: Great Architect

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/GreatArchitect.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `GreatArchitectCard`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
  (`builder`, `anyEdifice`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/BuildEdifice.scala`
  (the builder, the candidates, the bury's suit, doc comment)
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/GreatArchitectSuite.scala`
- Modify (imports): tests importing `GreatArchitectCard` from holding
  (`ResolveLegaciesSuite`, `LegacyCardDataSuite`)
- Modify: `src/test/resources/powers/unimplemented.txt` (rewritten)

**Interfaces:**
- Produces: `GreatArchitectCard`; `GreatArchitect` (power, no window);
  `ChronicleLegacyRules.imperialRoles: Set[Role]` (Chancellor, Citizen);
  `ChronicleLegacyRules.builder(state): Either[OathViolation, PlayerId]`,
  the Imperial Great Architect holder, else the Throne holder (ruling 10);
  `ChronicleLegacyRules.anyEdifice(state): Boolean`, whether an Imperial
  holds Great Architect in effect.

- [ ] **Step 1: Write the failing tests**

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.chronicle.{BuildEdifice, ChronicleWalk}
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Great Architect (L07): "If you are an Imperial: you perform Build
  * Edifice and may build any edifice (not just a matching one)." (ruling
  * 10). p1, the Chancellor, holds the Throne; the Empire rules Ancient
  * City, which holds Threatening Roar. */
class GreatArchitectSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val roar = ThreateningRoarCard.id
  private val roarSuit = catalog.denizen(roar).get.suit
  /** A suit other than the denizen's, whose edifice it may build. */
  private val other = Suit.all.find(_ != roarSuit).get

  private def empire: Table = ChronicleWalk.throneHeldByChancellorP1
    .imperial(p1, p2).warbandsAt(AncientCitySite, p1, 1)
    .denizen(ThreateningRoarCard, at = AncientCitySite)

  private def throne(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def building(suit: Suit): PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == BuildEdifice.decisionId(suit) =>
      DecisionAnswer.ChooseManyAnswer(Vector(DecisionOptionRef.Denizen(roar)))

  private def builds(run: ChronicleWalk.Run): Vector[Decide] =
    run.asked.filter(_.decisionId.startsWith("chronicle.build-edifice."))

  private def edificesAt(ready: ReadyGame): Vector[EdificeId] =
    ready.game.current.map.sites(AncientCitySite.id).denizens.collect {
      case EdificeState(id, _, _) => id }

  test("Great Architect is implemented"):
    assert(PowerImplementationStatus.implemented(GreatArchitectCard.power.id))

  test("a Citizen holder performs Build Edifice in place of the Chancellor " +
      "and builds an off-suit edifice (ruling 10)"):
    val run = throne(empire.legacy(p2, GreatArchitectCard), building(other))
    assert(builds(run).nonEmpty && builds(run).forall(_.owner == p2))
    assertEquals(edificesAt(run.ready).flatMap(id =>
      catalog.edifice(id).map(_.suit)), Vector(other))
    assert(!run.ready.game.current.map.sites(AncientCitySite.id).denizens
      .exists(_.id == roar))

  test("every available edifice is offered the denizen, whatever its suit"):
    val run = throne(empire.legacy(p2, GreatArchitectCard))
    assertEquals(builds(run).size,
      BuildEdifice.available(catalog, empire.ready.game.current).size)
    assert(builds(run).forall(_.query.asInstanceOf[DecisionQuery.ChooseMany]
      .options.map(_.ref) == Vector(DecisionOptionRef.Denizen(roar))))

  test("an off-suit burial returns the buried card's favor to its own " +
      "suit's bank (R14)"):
    val table = empire.tokens(ThreateningRoarCard, favor = 2)
      .bankFavor(roarSuit, 0).bankFavor(other, 0)
      .legacy(p2, GreatArchitectCard)
    val run = throne(table, building(other))
    assertEquals((run.ready.banks.favor(roarSuit),
      run.ready.banks.favor(other)), (2, 0))

  test("held by the Chancellor, only the matching rule changes"):
    val run = throne(empire.legacy(p1, GreatArchitectCard), building(other))
    assert(builds(run).forall(_.owner == p1))
    assertEquals(edificesAt(run.ready).flatMap(id =>
      catalog.edifice(id).map(_.suit)), Vector(other))

  test("an Exile's Great Architect does nothing (ruling 2)"):
    val run = throne(empire.legacy(p3, GreatArchitectCard))
    assert(builds(run).forall(_.owner == p1))
    assertEquals(builds(run).map(_.decisionId),
      Vector(BuildEdifice.decisionId(roarSuit)))

  test("a walk resumed at a later suit's question after an off-suit build " +
      "finds the denizen gone and the new edifice offered"):
    // The first suit's build buries the only denizen; each later suit,
    // asked of a walk rebuilt from the journal, offers the edifice just
    // built in its place, as an edifice may be buried.
    val first = BuildEdifice.available(catalog, empire.ready.game.current)
      .head._1
    val run = throne(empire.legacy(p2, GreatArchitectCard), building(first))
    val built = edificesAt(run.ready)
    assertEquals(built.size, 1)
    val later = builds(run).drop(1)
    assert(later.nonEmpty)
    assert(later.forall(_.query.asInstanceOf[DecisionQuery.ChooseMany]
      .options.map(_.ref) == Vector(DecisionOptionRef.Edifice(built.head))))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.GreatArchitectSuite"`
Expected: FAIL to compile: `Not found: GreatArchitectCard` in the new
package.

- [ ] **Step 3: Implement**

`GreatArchitect.scala`: move `GreatArchitectCard` verbatim, then a
`case object GreatArchitect extends ContributingPower` hooking no window,
its doc comment quoting the card and naming `builder` and `anyEdifice`
(Iron Hand's "even if" is slice 4).

`ChronicleLegacyRules`:

```scala
  /** The Imperial roles, for a power that says "if you are an Imperial". */
  val imperialRoles: Set[Role] = Role.values.filter(_.isImperial).toSet

  /** Who performs Build Edifice: an Imperial holding Great Architect, else
    * the Throne holder (ruling 10). */
  def builder(state: ReadyGame): Either[OathViolation, PlayerId] =
    holder(state, GreatArchitectCard.id, imperialRoles)
      .fold(ChronicleTasks.holder(state, ChronicleTask.Throne))(Right(_))

  /** Whether the builder may build any available edifice (ruling 10). */
  def anyEdifice(state: ReadyGame): Boolean =
    holder(state, GreatArchitectCard.id, imperialRoles).nonEmpty
```

`BuildEdifice.steps`: each suit's `Branch` asks `builder(state)` in place
of `ChronicleTasks.forHolder(state, ChronicleTask.Throne)` (a `Left`
becomes `ChronicleTasks.failing`). `candidates` keeps a card of any suit
when `anyEdifice(state)`. `bury` takes the buried card's own suit
(`catalog.suitOf(card.id)`) for its favor's bank, not the edifice's, which
was the same suit until now. Doc comment: the builder and the any-edifice
rule.

`LegacyChroniclePowers.all :+ GreatArchitect`.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshot**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.* distantlands.gameplay.cards.CardCatalogSuite distantlands.catalog.*"`
Expected: PASS except the snapshot; rewrite it as in Task 1 (diff:
`legacy.great-architect` removed). Run `./sbtw test`: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala src/test/resources/powers/unimplemented.txt
git commit -m "feat(legacies): Great Architect builds any edifice for the Empire

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: Revolutionary

**Files:**
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
  (`ChronicleAfterBuildEdifice`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWindows.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`
  (the empty window; doc comment)
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/Revolutionary.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `RevolutionaryCard`)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ChronicleWindowsSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/RevolutionarySuite.scala`
- Modify (imports): `LegacyCardDataSuite`
- Modify: the two snapshots (rewritten)

**Interfaces:**
- Consumes: Task 1's `ShapeEmpire.discardOf`.
- Produces: `PowerWindow.ChronicleAfterBuildEdifice`, in
  `ChronicleWindows.throne` after `ChronicleBuildEdifice`;
  `RevolutionaryCard`; `Revolutionary` (power);
  `Revolutionary.decisionId(player)` =
  `s"chronicle.throne.legacy.revolutionary.${player.value}"`;
  `Revolutionary.discardedWith: NoteKey` ("{0} discarded {1}
  {denizen/denizens} with {2}").

- [ ] **Step 1: Write the failing tests**

In `ChronicleWindowsSuite`, the Throne's keys gain
`"chronicle.throne.after-build-edifice"` after
`"chronicle.throne.build-edifice"`.

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.ChronicleAccounting
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p3}

/** Revolutionary (L15): "After Build Edifice, you may discard any number
  * of denizens at Imperial sites." (ruling 11). p1, the Chancellor, holds
  * the Throne; the Empire rules Ancient City and Broken Peaks; p3, an
  * Exile, holds Revolutionary. */
class RevolutionarySuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def empire: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BrokenPeaksSite, p1, 1)
    .denizen(ThreateningRoarCard, at = AncientCitySite)
    .denizen(FaeMerchantCard, at = BrokenPeaksSite)
    .edifice(GreatMarketCard, EdificeSide.Intact, at = AncientCitySite)
    .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = AncientCitySite)
    .legacy(p3, RevolutionaryCard)

  private def throne(table: Table,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      answers, after = TriggeredProcedureRef.ChronicleSun)

  private def discarding(refs: DecisionOptionRef*)
      : PartialFunction[Decide, DecisionAnswer] =
    case decide if decide.decisionId == Revolutionary.decisionId(p3) =>
      DecisionAnswer.ChooseManyAnswer(refs.toVector)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.throneNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone),
      keys, run.events).filter(said => keys.exists(_.name == said.key))
      .map(_.text)

  test("Revolutionary is implemented"):
    assert(PowerImplementationStatus.implemented(RevolutionaryCard.power.id))

  test("the holder is offered the denizens and ruined edifices at Imperial " +
      "sites, after Build Edifice"):
    val run = throne(empire)
    val asked = run.asked.find(_.decisionId == Revolutionary.decisionId(p3)).get
    assertEquals(asked.owner, p3)
    assertEquals(asked.query.asInstanceOf[DecisionQuery.ChooseMany].options
      .map(_.ref).toSet, Set[DecisionOptionRef](
        DecisionOptionRef.Denizen(ThreateningRoarCard.id),
        DecisionOptionRef.Denizen(FaeMerchantCard.id),
        DecisionOptionRef.Edifice(SchoolOfVinesCard.id)))
    val order = run.asked.map(_.decisionId)
    assert(order.lastIndexWhere(_.startsWith("chronicle.build-edifice.")) <
      order.indexOf(Revolutionary.decisionId(p3)))

  test("choosing none discards nothing and writes no line"):
    val run = throne(empire)
    assert(onMap(run.ready, ThreateningRoarCard.id) &&
      onMap(run.ready, FaeMerchantCard.id))
    assertEquals(lines(run), Vector.empty)

  test("several chosen are discarded where Shape Empire sends them " +
      "(ruling 11)"):
    val run = throne(empire, discarding(
      DecisionOptionRef.Denizen(ThreateningRoarCard.id),
      DecisionOptionRef.Denizen(FaeMerchantCard.id),
      DecisionOptionRef.Edifice(SchoolOfVinesCard.id)))
    Vector(ThreateningRoarCard.id, FaeMerchantCard.id, SchoolOfVinesCard.id)
      .foreach(card => assert(!onMap(run.ready, card), card))
    assert(onMap(run.ready, GreatMarketCard.id))
    val discards = run.ready.game.current.commonCards.regionalDiscards
      .values.flatten.toSet
    assert(discards.contains(ThreateningRoarCard.id) &&
      discards.contains(FaeMerchantCard.id))
    assertEquals(lines(run), Vector(
      s"p3 discarded 3 denizens with ${RevolutionaryCard.id.value}"))

  test("a ruined edifice discarded after Build Edifice leaves a stored " +
      "Chronicle that passes game start's card accounting (R14)"):
    // The whole Chronicle, from p1's win: p1 rules Ancient City, so the
    // crowned Empire holds its ruined edifice; p3 holds the People's Favor,
    // so keeps Revolutionary active through the Sun.
    val table = ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 1)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = AncientCitySite)
      .peoplesFavor(Some(p3), 1).legacy(p3, RevolutionaryCard)
    val run = ChronicleWalk.through(rules, table,
      TriggeredProcedureRef.ChronicleFinish,
      discarding(DecisionOptionRef.Edifice(SchoolOfVinesCard.id)))
    assert(!onMap(run.ready, SchoolOfVinesCard.id))
    val record = run.ready.game.campaign.evolved.get
    assertEquals(ChronicleAccounting.knownAndUnique(catalog, record),
      Right(()))
    assertEquals(ChronicleAccounting.conserves(
      run.ready.game.campaign.baseline, record), Right(()))

  test("a dormant Revolutionary asks nothing (ruling 1)"):
    val run = throne(empire.update(_.updateCampaign(campaign =>
      campaign.copy(lineages = campaign.lineages.view.mapValues(lineage =>
        lineage.copy(legacies = lineage.legacies.map(_.copy(active = false))))
        .toMap))))
    assert(!run.asked.exists(_.decisionId == Revolutionary.decisionId(p3)))

  test("a walk resumed at Revolutionary's question after a build applies " +
      "the answer"):
    // Build Edifice first buries Threatening Roar for its suit's edifice.
    val suit = catalog.denizen(ThreateningRoarCard.id).get.suit
    val building: PartialFunction[Decide, DecisionAnswer] =
      case decide if decide.decisionId ==
          distantlands.gameplay.chronicle.BuildEdifice.decisionId(suit) =>
        DecisionAnswer.ChooseManyAnswer(Vector(
          DecisionOptionRef.Denizen(ThreateningRoarCard.id)))
    val run = throne(empire, building.orElse(
      discarding(DecisionOptionRef.Denizen(FaeMerchantCard.id))))
    assert(!onMap(run.ready, FaeMerchantCard.id))
    val asked = run.asked.find(_.decisionId == Revolutionary.decisionId(p3)).get
    assert(!asked.query.asInstanceOf[DecisionQuery.ChooseMany].options
      .exists(_.ref == DecisionOptionRef.Denizen(ThreateningRoarCard.id)))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.RevolutionarySuite distantlands.gameplay.chronicle.ChronicleWindowsSuite"`
Expected: FAIL to compile: `Not found: RevolutionaryCard` in the new
package.

- [ ] **Step 3: Implement**

`PowerWindow`, after `ChronicleBuildEdifice`:

```scala
  /** After Build Edifice: Revolutionary's step (Legacy Chronicle powers
    * design, ruling 11). Empty with no power. */
  case object ChronicleAfterBuildEdifice extends OtherWindow:
    val key = "chronicle.throne.after-build-edifice"
```

`ChronicleWindows.throne` and `ChronicleThroneProcedure.steps` gain the
empty window after Build Edifice.

`Revolutionary.scala`: move `RevolutionaryCard` verbatim, then:

```scala
/** Revolutionary (legacy L15): "After Build Edifice, you may discard any
  * number of denizens at Imperial sites." Its step is the window after
  * Build Edifice (Legacy Chronicle powers design, ruling 11). The holder,
  * read when the step is reached, chooses any of the denizens and ruined
  * edifices at sites the Empire rules, in one choose-many; each goes where
  * Shape Empire sends it (`ShapeEmpire.discardOf`). Intact edifices stay,
  * as in Shape Empire. Line: "p2 discarded 3 denizens with
  * Revolutionary". */
case object Revolutionary extends ContributingPower:
  val id: PowerId = RevolutionaryCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleAfterBuildEdifice -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  def decisionId(player: PlayerId): String =
    s"chronicle.throne.legacy.revolutionary.${player.value}"

  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val discardedWith: NoteKey = notes.key("revolutionary-discarded", Vector(
    NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Plural(1, "denizen", "denizens"),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def throneNoteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, RevolutionaryCard.id).toVector.flatMap(
      holder => discardable(catalog, state, holder) match
        case Vector() => Vector.empty
        case cards => Vector(
          Decide(decisionId(holder), holder, DecisionQuery.ChooseMany(0,
            cards.size, cards.map((_, option, _) => option),
            heading = Some("Revolutionary: you may discard any denizens " +
              "at Imperial sites"),
            confirm = Some(CountedConfirm("Discard", "card", "cards",
              Some("Discard none"))))),
          Branch((_, pending) =>
            val chosen = PowerAnswers.many(pending, decisionId(holder))
              .getOrElse(Vector.empty)
            val discards = cards.filter((_, option, _) =>
              chosen.contains(option.ref)).map(_._3)
            if discards.isEmpty then Vector.empty
            else Vector(BuildOps((_, _) => Right(discards), required = true),
              Note(noteId, _ => Some(discardedWith.sourceless(
                NoteArg.Player(holder), NoteArg.Number(discards.size),
                NoteArg.Card(RevolutionaryCard.id)))))))))

  /** Each card at a site the Empire rules that Shape Empire could discard:
    * its site, its option and its discard, in map then row order. */
  private def discardable(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId): Vector[(SiteId, DecisionOption, CoreOperation)] =
    val current = state.game.current
    for
      site <- current.map.inPlay
        if current.map.sites.get(site).exists(at => SiteRule.ruler(at.forces,
          current.players).contains(SiteRuler.Empire))
      region <- current.map.regionOf(site).toVector
      card <- current.map.sites(site).denizens
      discard <- ShapeEmpire.discardOf(catalog, site, region, card, holder)
        .toVector
    yield (site, option(card), discard)
```

with `option` the `Denizen` or `Edifice` option of the card (Task 1's
helper, made package-visible in `ShapeEmpire` if it lives there).

`LegacyChroniclePowers`: `all :+ Revolutionary`; `throneNoteKeys ++
Revolutionary.throneNoteKeys`. `ProcedureLog.chronicleThrone`:
`Revolutionary.discardedWith -> LogKind.Decision`.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

As in Task 1 (diffs: `legacy.revolutionary` removed;
`revolutionary-discarded` added). Run `./sbtw test`: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/PowerWindow.scala \
  src/main/scala/distantlands/gameplay/chronicle \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/application/gamelog/ProcedureLog.scala \
  src/test/scala src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt
git commit -m "feat(legacies): Revolutionary discards denizens at Imperial sites

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: Reformer

**Files:**
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
  (`ChronicleStartOfThrone`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWindows.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/Reformer.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/LegacyChroniclePowers.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `ReformerCard`)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Modify: `src/test/scala/distantlands/gameplay/chronicle/ChronicleWindowsSuite.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/ReformerSuite.scala`
- Modify (imports): `LegacyCardDataSuite`
- Modify: the two snapshots (rewritten)

**Interfaces:**
- Produces: `PowerWindow.ChronicleStartOfThrone`, first in
  `ChronicleWindows.throne`; `ReformerCard`; `Reformer` (power);
  `Reformer.pickId(player)` =
  `s"chronicle.throne.legacy.reformer.${player.value}"`,
  `Reformer.destinationId(player)` = `pickId(player) + ".to"`,
  `Reformer.done` (`DecisionOptionRef.Button("done")`); notes
  `Reformer.moved` ("{0} moved {1} to {2} with {3}") and
  `Reformer.swapped` ("{0} swapped {1} and {2} with {3}").

- [ ] **Step 1: Write the failing tests**

In `ChronicleWindowsSuite`, the Throne's keys start with
`"chronicle.throne.start-of-throne"`.

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{MarriageCard, OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.ChronicleWalk
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.setup.GreatMarketCard
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2}
import distantlands.testkit.Unwrap.accepted

/** Reformer (L35): "If you won, you may move and swap denizens at sites you
  * rule, ignoring the locked restriction." A step at the start of the
  * Throne (ruling 12). p1 won and is the Chancellor; the Empire rules
  * Ancient City (room for three) and Broken Peaks (room for one). */
class ReformerSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private val ancientCity = AncientCitySite.id
  private val brokenPeaks = BrokenPeaksSite.id

  private def empire: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BrokenPeaksSite, p1, 1)
    .denizen(ThreateningRoarCard, at = AncientCitySite)

  /** The Throne's start, every pass answered in turn from `passes`, a
    * pick and, unless it is Done, a destination; Done once they run out. */
  private def reformed(table: Table,
      passes: Vector[(DecisionOptionRef, DecisionOptionRef)])(
      using munit.Location): ChronicleWalk.Run =
    var picks = passes.map(_._1)
    var destinations = passes.map(_._2)
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == Reformer.pickId(p1) =>
        val next = picks.headOption.getOrElse(Reformer.done)
        picks = picks.drop(1)
        DecisionAnswer.ChooseOneAnswer(next)
      case decide if decide.decisionId == Reformer.destinationId(p1) =>
        val next = destinations.head
        destinations = destinations.drop(1)
        DecisionAnswer.ChooseOneAnswer(next)
    }, after = TriggeredProcedureRef.ChronicleSun)

  private def at(ready: ReadyGame, site: SiteId): Vector[CardId] =
    ready.game.current.map.sites(site).denizens.map(_.id)

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    val keys = LegacyChroniclePowers.throneNoteKeys
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone),
      keys, run.events).filter(said => keys.exists(_.name == said.key))
      .map(_.text)

  private def denizen(card: distantlands.catalog.Denizen) =
    DecisionOptionRef.Denizen(card.id)

  test("Reformer is implemented"):
    assert(PowerImplementationStatus.implemented(ReformerCard.power.id))

  test("the winner moves a locked denizen to another site they rule with " +
      "room (ruling 12; Chronicle ruling 16)"):
    val table = empire.denizen(MarriageCard, at = AncientCitySite)
      .legacy(p1, ReformerCard)
    val run = reformed(table, Vector(denizen(MarriageCard) ->
      DecisionOptionRef.Site(brokenPeaks)))
    assertEquals(at(run.ready, brokenPeaks), Vector(MarriageCard.id))
    assertEquals(lines(run), Vector(s"p1 moved ${MarriageCard.id.value} to " +
      s"${brokenPeaks.value} with ${ReformerCard.id.value}"))

  test("the winner swaps two denizens between sites they rule"):
    val table = empire.denizen(FaeMerchantCard, at = BrokenPeaksSite)
      .legacy(p1, ReformerCard)
    val run = reformed(table, Vector(denizen(ThreateningRoarCard) ->
      denizen(FaeMerchantCard)))
    assertEquals(at(run.ready, brokenPeaks), Vector(ThreateningRoarCard.id))
    assert(at(run.ready, ancientCity).contains(FaeMerchantCard.id))
    assertEquals(lines(run), Vector(s"p1 swapped " +
      s"${ThreateningRoarCard.id.value} and ${FaeMerchantCard.id.value} " +
      s"with ${ReformerCard.id.value}"))

  test("a full site is no destination for a move, only for a swap"):
    val table = empire.denizen(FaeMerchantCard, at = BrokenPeaksSite)
      .legacy(p1, ReformerCard)
    val run = reformed(table, Vector(denizen(ThreateningRoarCard) ->
      denizen(FaeMerchantCard)))
    val offered = run.asked.find(_.decisionId == Reformer.destinationId(p1))
      .get.query.asInstanceOf[DecisionQuery.ChooseOne].options.map(_.ref)
    assertEquals(offered, Vector(denizen(FaeMerchantCard)))

  test("the holder repeats until Done, each pass read when reached"):
    val table = empire.legacy(p1, ReformerCard)
    val run = reformed(table, Vector(
      denizen(ThreateningRoarCard) -> DecisionOptionRef.Site(brokenPeaks),
      denizen(ThreateningRoarCard) -> DecisionOptionRef.Site(ancientCity)))
    assertEquals(at(run.ready, ancientCity), Vector(ThreateningRoarCard.id))
    assertEquals(run.asked.count(_.decisionId == Reformer.pickId(p1)), 3)
    assertEquals(lines(run).size, 2)

  test("Done first changes nothing and writes no line"):
    val run = reformed(empire.legacy(p1, ReformerCard), Vector.empty)
    assertEquals(at(run.ready, ancientCity), Vector(ThreateningRoarCard.id))
    assertEquals(lines(run), Vector.empty)
    assertEquals(run.asked.head.decisionId, Reformer.pickId(p1))

  test("an intact edifice moves too (R10)"):
    val table = empire.edifice(GreatMarketCard, EdificeSide.Intact,
      at = AncientCitySite).legacy(p1, ReformerCard)
    val run = reformed(table, Vector(
      DecisionOptionRef.Edifice(GreatMarketCard.id) ->
        DecisionOptionRef.Site(brokenPeaks)))
    assertEquals(at(run.ready, brokenPeaks), Vector(GreatMarketCard.id))
    assertEquals(run.ready.game.current.map.sites(brokenPeaks).denizens,
      Vector(EdificeState(GreatMarketCard.id, EdificeSide.Intact,
        Tokens.empty)))

  test("a walk parked mid-pass after a completed move resumes to the same " +
      "question (R13)"):
    // Pass 1 moves Threatening Roar to Broken Peaks; pass 2 picks it again
    // and parks on its destination. Rebuilt from the journal, the walk
    // parks on the same question with the same options.
    val table = empire.legacy(p1, ReformerCard)
    val started = accepted(rules.next(OathTransition(table.state,
      Vector.empty), TriggeredProcedureRef.ChronicleSun))
    def answer(step: OathTransition, id: String, ref: DecisionOptionRef) =
      val next = accepted(rules.resolveWalker(step.state, p1, id,
        DecisionAnswer.ChooseOneAnswer(ref)))
      OathTransition(next.state, step.events ++ next.events)
    val roar = denizen(ThreateningRoarCard)
    val parked = Vector(
      (Reformer.pickId(p1), roar),
      (Reformer.destinationId(p1), DecisionOptionRef.Site(brokenPeaks)),
      (Reformer.pickId(p1), roar))
      .foldLeft(started)((step, pair) => answer(step, pair._1, pair._2))
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.decisionId, Reformer.destinationId(p1))
    val replayed = accepted(parked.events.foldLeft[
      Either[OathViolation, OathState]](Right(table.state))((state, event) =>
        state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
    assertEquals(decide.query.asInstanceOf[DecisionQuery.ChooseOne].options
      .map(_.ref), Vector(DecisionOptionRef.Site(ancientCity)))

  test("a holder who lost is not asked"):
    val run = reformed(empire.legacy(p2, ReformerCard), Vector.empty)
    assert(!run.asked.exists(_.decisionId.startsWith(
      "chronicle.throne.legacy.reformer.")))

  test("with nothing to move or swap the winner is not asked"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .legacy(p1, ReformerCard)
    assert(!reformed(table, Vector.empty).asked.exists(
      _.decisionId == Reformer.pickId(p1)))
```

(Every answer is given to a walk rebuilt from the journal, so the passes
test resuming after each move. `MarriageCard` is a locked denizen.)

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.ReformerSuite distantlands.gameplay.chronicle.ChronicleWindowsSuite"`
Expected: FAIL to compile: `Not found: ReformerCard` in the new package.

- [ ] **Step 3: Implement**

`PowerWindow`, first among the Throne's:

```scala
  /** The start of the Throne, before Offer Citizenship: Reformer's step
    * (Legacy Chronicle powers design, ruling 12). Empty with no power. */
  case object ChronicleStartOfThrone extends OtherWindow:
    val key = "chronicle.throne.start-of-throne"
```

`ChronicleWindows.throne` and `ChronicleThroneProcedure.steps` start with
the empty window.

`Reformer.scala`: move `ReformerCard` verbatim, then:

```scala
/** Reformer (legacy L35): "If you won, you may move and swap denizens at
  * sites you rule, ignoring the locked restriction." Its step is the window
  * at the start of the Throne (Legacy Chronicle powers design, ruling 12),
  * only when the holder is the game's winner. Each pass is one move or
  * swap: the holder picks a card at a site they rule, edifices included
  * (R10), or Done; then its destination, another site they rule with room
  * (a move) or a card at another site they rule (a swap). Passes repeat
  * until Done or until no card has a destination; each is read when reached, under the same
  * two ids, from the latest answers, as Call for Help's passes are. Locks
  * do not bind in the Chronicle (Chronicle ruling 16). A swap is two moves,
  * so each card takes the last slot of the other's site. Lines: "p1 moved
  * Village Idiot to Broken Peaks with Reformer", "p1 swapped Village Idiot
  * and Map Library with Reformer". */
case object Reformer extends ContributingPower:
  val id: PowerId = ReformerCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleStartOfThrone -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  def pickId(player: PlayerId): String =
    s"chronicle.throne.legacy.reformer.${player.value}"
  def destinationId(player: PlayerId): String = pickId(player) + ".to"
  val done: DecisionOptionRef.Button = DecisionOptionRef.Button("done")

  private val noteId =
    ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  val moved: NoteKey = notes.key("reformer-moved", Vector(NotePart.Arg(0),
    NotePart.Text(" moved "), NotePart.Arg(1), NotePart.Text(" to "),
    NotePart.Arg(2), NotePart.Text(" with "), NotePart.Arg(3)))
  val swapped: NoteKey = notes.key("reformer-swapped", Vector(
    NotePart.Arg(0), NotePart.Text(" swapped "), NotePart.Arg(1),
    NotePart.Text(" and "), NotePart.Arg(2), NotePart.Text(" with "),
    NotePart.Arg(3)))
  def throneNoteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    ChronicleLegacyRules.holder(state, ReformerCard.id)
      .filter(holder => ChronicleTasks.winner(state).contains(holder))
      .toVector.map(holder => Repeat((live, pending) =>
          !finished(pending, holder) && movable(catalog, live, holder).nonEmpty,
        Sequence(Vector(
          Branch((live, _) => Vector(Decide(pickId(holder), holder,
            DecisionQuery.ChooseOne(DecisionOption.Button(done, "Done") +:
              movable(catalog, live, holder),
              heading = Some("Reformer: you may move or swap a denizen at " +
                "sites you rule"))))),
          Branch((live, pending) => picked(pending, holder).toVector.flatMap(
            card => Option.when(owed(pending, holder))(Decide(
              destinationId(holder), holder, DecisionQuery.ChooseOne(
                destinations(catalog, live, holder, card),
                heading = Some("Reformer: move it to a site, or swap it " +
                  "with a denizen"))))).toVector),
          Branch((live, pending) => applied(live, holder, pending)))))))
```

with these private helpers, each reading state or answers when reached:

- `ruled(state, holder)`: the sites in play `ChronicleTasks.rules` says
  the holder rules.
- `rowOf(state, site)`: every card in the site's row, denizens and
  edifices, intact or ruined (R10), as its `DecisionOption.Denizen` or
  `DecisionOption.Edifice`.
- `destinations(catalog, state, holder, card)`: for each other ruled site,
  `DecisionOption.Site` when `SiteRoom.hasRoom`, then the option of each
  card there; sites in map order.
- `movable(catalog, state, holder)`: the cards at ruled sites with at
  least one destination.
- `finished(pending, holder)`: `PowerAnswers.last(pending.answered,
  pickId(holder)).contains(done)`.
- `picked(pending, holder)`: the latest pick, when it is a card (a
  `Denizen` or `Edifice` ref).
- `owed(pending, holder)`: the count of card picks exceeds the count of
  destination answers, so a pass asks its destination once.
- `applied(state, holder, pending)`: when the pass's pick and destination
  are both answered and the pick count equals the destination count, the
  `Move` (a site) or two `Move`s (a card), one required `BuildOps`, and
  the `moved` or `swapped` line; otherwise nothing. The pass's own answers
  are the latest of each id, so an earlier pass is never applied twice.

`LegacyChroniclePowers`: `all :+ Reformer`; `throneNoteKeys ++
Reformer.throneNoteKeys`. `ProcedureLog.chronicleThrone`: both keys
`LogKind.Decision`.

- [ ] **Step 4: Run the tests to verify they pass, and rewrite the snapshots**

As in Task 1 (diffs: `legacy.reformer` removed; `reformer-moved` and
`reformer-swapped` added). Run `./sbtw test`: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/model/PowerWindow.scala \
  src/main/scala/distantlands/gameplay/chronicle \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/main/scala/distantlands/application/gamelog/ProcedureLog.scala \
  src/test/scala src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt
git commit -m "feat(legacies): Reformer moves and swaps the winner's denizens

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: Docs and the gate

**Files:**
- Modify: `docs/rules/chronicle.md` (the Throne, steps 1-3 and after 3)
- Modify: `docs/rules/ambiguities.md` (the "Legacy Chronicle powers
  rulings" section)
- Modify: `docs/rules/implementation-traceability.md` (the "Throne task"
  row)
- Modify: `docs/ROADMAP.md` (the setup item names Rival's SETUP half, R7)
- Modify: `docs/superpowers/specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md`
  (status line)

- [ ] **Step 1: Docs**

- `chronicle.md`: a "Digital:" note before step 1 for Reformer (who is
  asked, a pass, the lines, ruling 12); on step 2 for Rival (ruling 9); on
  step 3 for Great Architect (the builder, any available edifice, ruling
  10); after step 3 for Revolutionary (ruling 11). Each links the
  "Legacy Chronicle powers rulings".
- `ambiguities.md`: the design's rulings 9-12 as items 15-18 and this
  slice's implementation rulings after them, each with its R number; the
  section's opening sentence names the new ranges.
- Traceability "Throne task" row: Rival, Great Architect, Revolutionary and
  Reformer join the evidence (`RivalSuite`, `GreatArchitectSuite`,
  `RevolutionarySuite`, `ReformerSuite`).
- Design status: "Slices 1 and 2 landed (plans: ...)".

UI check: the decisions use option kinds the app shows already (denizens,
edifices and sites on the board, a Done button). Reaching a Throne with
these legacies held in the running app needs a finished game holding
them, which no fixture offers; the report says so.

- [ ] **Step 2: Run the verification gate**

In the background: `./sbtw clean coverage test coverageReport`
(statements at or above 92%, branches at or above 84.4%); then
`./sbtw "frontend/test"`, `python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`; `git status --short
src/test/resources` is clean.

- [ ] **Step 3: Commit**

```bash
git add docs/rules/chronicle.md docs/rules/ambiguities.md \
  docs/rules/implementation-traceability.md docs/ROADMAP.md \
  docs/superpowers/specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md
git commit -m "docs(legacies): record the Throne's legacy powers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

## Self-Review

**Spec coverage** (design, "Slice 2: Throne denizens"):
- The Reformer and Revolutionary windows: Tasks 4 and 3, listed in
  `ChronicleWindowsSuite`.
- Shape Empire reads `protector`: Task 1 (ruling 9).
- Build Edifice reads `builder` and `anyEdifice`: Task 2 (ruling 10).
- Rival's Throne half, Great Architect, Revolutionary and Reformer leave
  holding: Tasks 1-4 (Rival with its whole text, R7).
- Tests the spec lists: Rival keeping some denizens (Task 1); Great
  Architect as a Citizen building an off-suit edifice in place of the
  Chancellor, and as the Chancellor (Task 2); Revolutionary choosing none
  and several (Task 3); Reformer moving a locked denizen, swapping two and
  refusing a full site, and not asked when its holder lost (Task 4);
  resume tests for each decision (each suite's last test, and every answer
  through `ChronicleWalk.through`).
- General rulings: 1 (dormant and boxed holders do nothing), 2 (Great
  Architect's Imperial condition read when Build Edifice is reached), 3
  (each decision its own id, declining or Done always possible), 4 (Rival's
  and Revolutionary's picks are one choose-many each).
- Docs: Task 5.

**Placeholder scan:** no step defers its content.

**Type consistency:** `ChronicleLegacyRules.protector(ReadyGame):
Option[PlayerId]`, `builder(ReadyGame): Either[OathViolation, PlayerId]`,
`anyEdifice(ReadyGame): Boolean`, `imperialRoles: Set[Role]` in Tasks 1
and 2; `ShapeEmpire.discardOf` in Tasks 1 and 3;
`LegacyChroniclePowers.throneNoteKeys` in Tasks 1-4;
`ChronicleWalk.through(..., after)` in Tasks 1-4; decision ids
`Rival.decisionId`, `Revolutionary.decisionId`, `Reformer.pickId`,
`Reformer.destinationId` as their interfaces define.
