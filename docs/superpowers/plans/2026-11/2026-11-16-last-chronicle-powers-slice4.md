# Last Chronicle Powers, Slice 4: Retire the Machinery — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the code that sorts, filters and marks unimplemented cards, now that every printed power runs.

**Architecture:** Four removals across the stack, each ending green:
1. The deal shuffles both decks uniformly.
2. Teeming World deals each card on top of its pile (ruling 7).
3. The card projection and the frontend lose the `implemented` flag and the badge.
4. `PowerImplementationStatus` goes, with the snapshot, the holding rule and every test that asked it.

A last task updates the docs and runs the full gates. No new operation, line or decision.

**Tech Stack:** Scala 3.9.0, munit, Scala.js frontend, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [docs/superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md](../../specs/2026-11/2026-11-16-last-chronicle-powers-design.md), section "Slice 4: retire the machinery" and ruling 7.

## Global Constraints

- Work in the worktree `.worktrees/last-chronicle-slice4` on branch `last-chronicle-slice4`. Never commit on `0.2.0-alpha` or `main`.
- Before Task 1, slice 3 must be merged into `0.2.0-alpha`. Then run `git rebase 0.2.0-alpha` in the worktree. Before that rebase, the branch sits on an unfinished slice 3.
- Before the first `./sbtw`, link the main checkout's tooling into the worktree:
  `ln -s /Users/roman/projects/distantlands/.tooling .tooling` and
  `ln -s /Users/roman/projects/distantlands/node_modules node_modules`. Never commit either link.
- Stage files by name. The hook blocks `git add -A`, `git add -u`, `git add .`, `git add --all`, `git commit -a` and `git stash`. Stage a deletion with `git rm <path>` or `git add <deleted path>`.
- Every commit message ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Push nothing.
- Code, comments, docs and commit messages are in plain prose. Wrap Scala and Markdown prose at 80 columns, as the files around the change are wrapped.
- `-Werror` is on: remove every import a removal leaves unused.
- Protected (do not change): the registration of every power in `WalkerPowerCatalog` and `PhasePowerCatalog`, inert ones included (owner, 2026-11-16); `CatalogHandlerInventory` and its suite; `OperationRestrictions.printedRules` and its contents; `ChronicleRandomPort` and the `ChronicleShuffles` stubs; every golden except one whose diff Task 2 explains.
- Claim evidence in reports: tag each claim *gate* (a command passed), *read* (seen in code) or *UNKNOWN*.

## Re-inventory before each task

The inventory below was read at `d100a4a5c` (slice 3, Task 1). Slice 3's Tasks 2 and 3 land before this plan runs, so each task begins by running its grep at HEAD. Treat what that grep shows as the list. A hit that is missing from the task's table is handled by the task's rule. Report each such hit.

## File map

| Task | Delete | Modify |
|---|---|---|
| 1 | `src/main/scala/distantlands/application/ImplementedCardCatalog.scala`, `src/test/scala/distantlands/application/ImplementedCardCatalogSuite.scala`, `src/test/scala/distantlands/application/ChronicleShuffleSuite.scala` | `ChronicleShuffle.scala`, `FirstGameChronicleGenerator.scala`, `GeneratedFirstGamePlanFactory.scala` (all in `src/main/scala/distantlands/application/`); tests `ChronicleRandomPortContract.scala`, `FirstGameChronicleGeneratorSuite.scala`, `GeneratedFirstGamePlanFactorySuite.scala`, `gameplay/setup/FirstGameSetupMaterializerSuite.scala` |
| 2 | — | `src/main/scala/distantlands/gameplay/setup/SetupDeal.scala`; tests `gameplay/setup/SetupDealSuite.scala`, `gameplay/setup/AlteredFoundationSetupSuite.scala`, and any Teeming World test the full run shows failing on pile order; `docs/rules/ambiguities.md`, `docs/architecture/game-setup.md` |
| 3 | `src/test/scala/distantlands/application/GamePresentationProjectorImplementedSuite.scala` | `shared/.../projection/WorldProjectionDtos.scala`, `shared/.../projection/WorldProjectionCodec.scala`, `shared/src/test/.../ProjectionProtocolSuite.scala`, `src/main/.../application/GamePresentationProjector.scala`, `frontend/.../CardFace.scala`, `frontend/.../CardInspectionOverlay.scala`, `frontend/src/test/.../CardFaceSuite.scala`, `frontend/styles.css`, `DESIGN.md`, `.impeccable/design.json` |
| 4 | `src/main/scala/distantlands/gameplay/powers/PowerImplementationStatus.scala`, `src/test/scala/distantlands/gameplay/powers/PowerImplementationStatusSuite.scala`, `src/test/scala/distantlands/gameplay/powers/UnimplementedPowersSuite.scala`, `src/test/resources/powers/unimplemented.txt` | `gameplay/cards/HomelandsAndPlains.scala`, `gameplay/operations/OperationRestrictions.scala`, `gameplay/operations/PrintedRule.scala`, the inert powers' doc comments, `gameplay/cards/CardCatalogSuite.scala`, `gameplay/OperationRestrictionsSuite.scala`, every suite that asserts `PowerImplementationStatus.implemented` |
| 5 | — | `docs/architecture/rule-resolution.md`, `docs/architecture/codebase-structure.md`, `docs/architecture/gameplay-modules.md`, `docs/architecture/all-exile-round-endings.md`, `docs/catalog/README.md`, `docs/rules/ambiguities.md`, `docs/ROADMAP.md`, the spec |

---

### Task 1: Uniform deal

`ShufflePolicy.implementedFirst` puts implemented cards on top. Every card is implemented, so `random.shuffle` gives the same result. Under the deterministic stubs `reversing` and `unshuffled`, the orders the generator and the factory produce do not change: `shuffle(all) ++ shuffle(empty)` equals `shuffle(all)`. Each Homeland still gets the lowest-id edifice of its suit, because every edifice is now fully implemented.

**Files:** see the file map, row 1.

**Interfaces:**
- Produces: `FirstGameChronicleGenerator.generate(catalog: ExecutableCatalog, name: String, random: ChronicleRandomPort): Either[ChronicleGeneratorFailure, Chronicle]`; `ChronicleGeneratorFailure.TooFewSuitDenizens(suit: Suit, denizens: Int)`; `new GeneratedFirstGamePlanFactory(catalog: ExecutableCatalog, random: ChronicleRandomPort = ChronicleRandomPort.random)`.
- The trait `ShufflePolicy` and the object `ImplementedCardCatalog` no longer exist.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'ShufflePolicy|implementedFirst|ImplementedCardCatalog|TooFewSuitDenizens|FirstGameChronicleGenerator\.generate|GeneratedFirstGamePlanFactory\(' -- src shared frontend
```
Expected: the hits in the files of row 1, plus `ServerRuntime.scala` and three server suites that construct `GeneratedFirstGamePlanFactory(catalog)` or `(catalog, unshuffled)`. Those constructor calls compile unchanged.

- [ ] **Step 2: Write the new generator tests**

In `src/test/scala/distantlands/application/FirstGameChronicleGeneratorSuite.scala`, make these edits:
- Delete the import of `PowerImplementationStatus`, and the import of `GreatForgeCard` once the test that uses it is gone.
- Delete the fields `status` and `policy`.
- Make `generated` call `FirstGameChronicleGenerator.generate(catalog, "Test Chronicle", random)`.

Replace the relic-deck test with:

```scala
  test("the relic deck has every ordinary relic exactly once"):
    val chronicle = generated
    val ordinary = catalog.relics.filter(_.id != TheGrandScepterCard.id)
      .map(r => distantlands.model.RelicId(r.id.value))
    assertEquals(chronicle.relicDeck.toSet, ordinary.toSet)
    assertEquals(chronicle.relicDeck.size, ordinary.size)
```

Delete the test `"implemented means what the UI marker reads: a walker-only power counts"`. Replace `"every Homeland carries an implemented edifice when its suit has one"` with:

```scala
  test("every Homeland carries the lowest-id edifice of its suit"):
    val chronicle = generated
    chronicle.atlasBox.flatMap(_.items).collect { case id: EdificeId => id }
      .foreach { id =>
        val suit = suitOf(id)
        assertEquals(id.value,
          catalog.edifices.filter(_.suit == suit).map(_.id.value).min)
      }

  test("a suit with fewer than 12 denizens is refused, with its count"):
    val hearth = catalog.denizens.filter(_.suit == Suit.Hearth)
    val short = catalog.copy(denizens =
      catalog.denizens.filterNot(_.suit == Suit.Hearth) ++ hearth.take(11))
    assertEquals(
      FirstGameChronicleGenerator.generate(short, "Short", random).left.toOption,
      Some(ChronicleGeneratorFailure.TooFewSuitDenizens(Suit.Hearth, 11)))
```

- [ ] **Step 3: Rewrite the factory's first test**

In `src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala`, delete the import of `PowerImplementationStatus`. Replace the test `"implemented relics are still dealt first after setup: site relic slots, then the relic deck"` with:

```scala
  test("site relic slots, then the relic deck, are dealt in the plan's " +
      "relic order"):
    val plan = accepted(factory.build(config, ChronicleChoice.New("Test Chronicle")))
    val current = accepted(
      GameStartRules.evolve(catalog, plan.chronicle, plan.orders)).game.current
    val dealtRelics = current.map.inPlay.flatMap(site =>
      current.map.sites(site).relics.map(_.id)) ++ current.commonCards.relicDeck
    assertEquals(dealtRelics, plan.orders.relicOrder)
```

Remove `present` from the `Unwrap` import if nothing else in the file uses it.

- [ ] **Step 4: Delete the policy's suites and fix the materializer doc**

```bash
git rm src/test/scala/distantlands/application/ChronicleShuffleSuite.scala \
  src/test/scala/distantlands/application/ImplementedCardCatalogSuite.scala
```

In `src/test/scala/distantlands/application/ChronicleRandomPortContract.scala`, delete the doc comment and the class `ShufflePolicySuite`, from `/** What every \`ShufflePolicy\` promises` to the end of the file.

In `src/test/scala/distantlands/gameplay/setup/FirstGameSetupMaterializerSuite.scala`, replace the class doc comment with:

```scala
/** `relicOrder` fills the site relic slots from its top, and the rest, in
  * the same order, is the drawable deck: a slot takes the card a draw from
  * the deck would have taken.
  */
```

- [ ] **Step 5: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.application.FirstGameChronicleGeneratorSuite distantlands.application.GeneratedFirstGamePlanFactorySuite"`
Expected: compilation fails. `generate` takes 5 arguments, and `TooFewSuitDenizens` takes 3.

- [ ] **Step 6: Remove the policy and the filter from main**

```bash
git rm src/main/scala/distantlands/application/ImplementedCardCatalog.scala
```

In `src/main/scala/distantlands/application/ChronicleShuffle.scala`, delete everything from the `/**` doc above `trait ShufflePolicy:` to the end of the file. The file keeps `ChronicleRandomPort` and its companion. Leave the file name as it is.

In `src/main/scala/distantlands/application/FirstGameChronicleGenerator.scala`, replace from the `TooFewSuitDenizens` line through the end of `denizenPools` with:

```scala
  final case class TooFewSuitDenizens(suit: Suit, denizens: Int)
      extends ChronicleGeneratorFailure
  final case class InvariantViolated(detail: String) extends ChronicleGeneratorFailure
  final case class InvalidName(reason: String) extends ChronicleGeneratorFailure

/**
 * Produces a random first-game Chronicle (2026-09-21 Chronicle design, "The
 * first-game generator"): all 24 sites shuffled into the atlas box, each
 * Homeland carrying the lowest-id edifice of its suit; a 60-denizen world
 * deck, 10 random denizens per suit; 12 dispossessed denizens, 2 per suit,
 * drawn from what the 60 left behind; and the full ordinary relic deck.
 * `random` shuffles both decks. Self-validates the counts before returning.
 */
object FirstGameChronicleGenerator:
  import ChronicleGeneratorFailure._

  def generate(catalog: ExecutableCatalog, name: String,
      random: ChronicleRandomPort)
      : Either[ChronicleGeneratorFailure, Chronicle] =
    for
      _ <- Chronicle.nameProblem(name).map(InvalidName(_)).toLeft(())
      atlas <- atlasBox(catalog, random)
      pools <- denizenPools(catalog, random)
      (worldPool, dispossessedPool) = pools
      relicPool = catalog.relics.filter(_.id != TheGrandScepterCard.id)
        .map(_.id)
      chronicle = Chronicle(
        name,
        atlas,
        worldDeck = random.shuffle(worldPool),
        relicDeck = random.shuffle(relicPool),
        era = EraState(20, Vector.empty),
        oathkeeperGoal = OathkeeperGoal.Supremacy,
        dispossessed = dispossessedPool)
      _ <- validate(catalog, chronicle)
    yield chronicle

  private def atlasBox(catalog: ExecutableCatalog, random: ChronicleRandomPort)
      : Either[ChronicleGeneratorFailure, Vector[StoredSite]] =
    val sites = catalog.sites.map(_.id)
    if sites.size != 24 then Left(WrongSiteCount(sites.size))
    else Right(random.shuffle(sites).map { siteId =>
      catalog.site(siteId).get.homeland match {
        case None => StoredSite(siteId)
        case Some(suit) =>
          StoredSite(siteId, Vector(edificeForHomeland(catalog, suit)))
      }
    })

  /** The lowest-id edifice of `suit`. */
  private def edificeForHomeland(catalog: ExecutableCatalog, suit: Suit)
      : EdificeId =
    EdificeId(catalog.edifices.filter(_.suit == suit).map(_.id.value).min)

  private val PerSuit = 10
  private val DispossessedPerSuit = 2

  /** Per suit: 10 random denizens go into the world deck, and 2 of the
    * denizens left out are dispossessed. */
  private def denizenPools(catalog: ExecutableCatalog,
      random: ChronicleRandomPort)
      : Either[ChronicleGeneratorFailure, (Vector[DenizenId], Vector[DenizenId])] =
    Suit.all.foldLeft[Either[ChronicleGeneratorFailure,
        (Vector[DenizenId], Vector[DenizenId])]](Right(Vector.empty -> Vector.empty)):
      (acc, suit) =>
      acc.flatMap { case (worldPool, dispossessedPool) =>
        val suited = catalog.denizens.filter(_.suit == suit)
          .map(d => DenizenId(d.id.value))
        if suited.size < PerSuit + DispossessedPerSuit then
          Left(TooFewSuitDenizens(suit, suited.size))
        else
          val world = random.shuffle(suited).take(PerSuit)
          val left = random.shuffle(suited.filterNot(world.contains))
          Right((worldPool ++ world,
            dispossessedPool ++ left.take(DispossessedPerSuit)))
      }
```

`validate` and the `WrongSiteCount` case stay as they are. Remove `PowerId` from the imports if the compiler flags it (the file imports `distantlands.model._`).

In `src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala`:
- Delete the `PowerImplementationStatus` import.
- Make the constructor `(catalog: ExecutableCatalog, random: ChronicleRandomPort = ChronicleRandomPort.random)`.
- Delete `val implemented = ...`.
- Call `FirstGameChronicleGenerator.generate(catalog, name, random)`.
- Make the two deck orders in `dealOrder` `random.shuffle(chronicle.worldDeck)` and `random.shuffle(chronicle.relicDeck)`.

In the class doc, replace "Both decks are shuffled into the deal by\n * `policy`, and the derived legacy deck by `random`, since" with "Both decks and the derived legacy deck are shuffled into the deal by\n * `random`, since".

- [ ] **Step 7: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.application.* distantlands.gameplay.setup.* distantlands.server.*"`
Expected: PASS. A failure here means an order changed. Stop and report it: by the argument at the top of this task, no order should change.

- [ ] **Step 8: Confirm nothing is left**

Run: `git grep -n -E 'ShufflePolicy|implementedFirst|ImplementedCardCatalog' -- src shared frontend`
Expected: no output.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/distantlands/application/ChronicleShuffle.scala \
  src/main/scala/distantlands/application/FirstGameChronicleGenerator.scala \
  src/main/scala/distantlands/application/GeneratedFirstGamePlanFactory.scala \
  src/test/scala/distantlands/application/ChronicleRandomPortContract.scala \
  src/test/scala/distantlands/application/FirstGameChronicleGeneratorSuite.scala \
  src/test/scala/distantlands/application/GeneratedFirstGamePlanFactorySuite.scala \
  src/test/scala/distantlands/gameplay/setup/FirstGameSetupMaterializerSuite.scala
git commit -m "$(cat <<'EOF'
refactor(setup): deal both decks from a uniform shuffle

Every printed power runs, so the implemented-first policy and the
implemented filter of the first-game generator no longer change any deal.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

The `git rm` calls in Steps 4 and 6 already staged the deletions.

---

### Task 2: Teeming World deals each card on top (ruling 7)

Today `SetupDeal.discards` reverses each pile, so the first card dealt is its top. Ruling 7 deals each card on top of its pile: a pile is stored top last, so its stored order is the order it was dealt.

**Files:** see the file map, row 2.

**Interfaces:**
- Consumes: nothing from Task 1.
- Produces: `SetupDeal.discards(stack)` keeps its signature. Under Teeming World each pile is in deal order, top last.

- [ ] **Step 1: Re-inventory**

Run: `git grep -n -i -E 'first card dealt|dealt to a pile|first dealt|each later (card|one) beneath' -- src docs ':!docs/superpowers'`
Expected: `SetupDeal.scala`, `SetupDealSuite.scala`, `AlteredFoundationSetupSuite.scala`, `docs/rules/ambiguities.md` (Foundations item 9) and `docs/architecture/game-setup.md`.

- [ ] **Step 2: Re-pin the deal tests**

In `src/test/scala/distantlands/gameplay/setup/SetupDealSuite.scala`, rename the test `"Teeming World deals the rest of the stack to the Cradle, the Provinces and the Hinterland in turn, the first card dealt to a pile on top and each later one beneath, and leaves the world deck no denizen"` to:

```scala
  test("Teeming World deals the rest of the stack to the Cradle, the " +
      "Provinces and the Hinterland in turn, each card on top of its pile, " +
      "and leaves the world deck no denizen"):
```

In its body, change the comment and the expected piles to:

```scala
    // 9 drawn of 19 leaves 10: the Cradle takes the odd card. A pile is
    // stored top last, in the order it was dealt.
    val nineteen = stack.take(19)
    assertEquals(deal.discards(nineteen), Map[Region, Vector[WorldCardId]](
      Region.Cradle -> Vector(9, 12, 15, 18).map(nineteen),
      Region.Provinces -> Vector(10, 13, 16).map(nineteen),
      Region.Hinterland -> Vector(11, 14, 17).map(nineteen)))
```

In the next test, the one about a rest two cards over a multiple of three, change the expected piles to:

```scala
      Region.Cradle -> Vector(9, 12, 15, 18).map(twenty),
      Region.Provinces -> Vector(10, 13, 16, 19).map(twenty),
      Region.Hinterland -> Vector(11, 14, 17).map(twenty)))
```

In `src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala`, in the test `"Teeming World deals the rest of the stack to the Cradle, the Provinces and the Hinterland discard piles in turn, with no pile seeded"`, replace `everyThird` and the two closing assertions with:

```scala
    /** Every third card of the rest from `from`, in the order dealt: a pile
      * is stored top last, and each card dealt goes on top. */
    def everyThird(from: Int): Vector[WorldCardId] =
      rest.indices.filter(_ % 3 == from).map(rest).toVector
```

and

```scala
    // The first three cards dealt lie at the piles' bottoms, and the last
    // three top them.
    assertEquals(Region.all.map(region =>
      ready.game.current.commonCards.discard(region).head), rest.take(3))
    assertEquals(Region.all.map(region =>
      ready.game.current.commonCards.discard(region).last), rest.takeRight(3))
```

- [ ] **Step 3: Run the tests to see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.setup.SetupDealSuite distantlands.gameplay.setup.AlteredFoundationSetupSuite"`
Expected: FAIL on the three re-pinned tests. Each pile comes out reversed.

- [ ] **Step 4: Deal each card on top**

In `src/main/scala/distantlands/gameplay/setup/SetupDeal.scala`, replace the doc and the Teeming branch of `discards` with:

```scala
  /** The discard piles the deal leaves, each in stored order, top last.
    * Teeming World deals the rest of the stack to the Cradle, the Provinces
    * and the Hinterland in turn, each card on top of its pile, so the last
    * card dealt to a pile is its top (Last Chronicle powers ruling 7). */
  def discards(stack: Vector[DenizenId]): Map[Region, Vector[WorldCardId]] =
    val piles = Region.all.zipWithIndex
    if teeming then
      val rest = stack.drop(drawn).zipWithIndex
      piles.map((region, pile) => region -> rest.collect {
        case (card, dealt) if dealt % piles.size == pile => card
      }).toMap
```

The `else` branch stays as it is.

- [ ] **Step 5: Run the tests to see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.setup.SetupDealSuite distantlands.gameplay.setup.AlteredFoundationSetupSuite"`
Expected: PASS.

- [ ] **Step 6: Find every other test that reads a Teeming pile's order**

Run in the background: `./sbtw test`, then `./sbtw "frontend/test"`.
Expected: everything passes, or only tests that read a Teeming World discard pile's top or order fail. These suites use Teeming World and may fail: `OathsTeemingBurialSuite`, `QuestFirstWakeSuite`, `VisionPacketsSuite`, `TheMouthSuite`, `OathsSetupSuite`, `GameStartRulesSuite`, `SeerSuite`, `SecretSocietySuite`, `QuestSetupSuite`, `GamePresentationProjectorAdviserRedactionSuite`, `GameEventFactsWireSuite`.

For each failure:
- Read the assertion.
- Re-pin it only if the expected value is a pile card or pile order that the deal now reverses.
- Prefer deriving the expectation from the deal (`.last` of the pile) over a new literal.

A Search of a Teeming pile now draws what was its bottom. A log golden that names that card changes. Rewrite such a golden with `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`, but only after reading `git diff` on it. The diff must change only card names drawn from or lying in a Teeming pile.

Stop and report any other kind of failure. List every re-pinned test and golden in the task report.

- [ ] **Step 7: Record the ruling**

In `docs/rules/ambiguities.md`, Foundations rulings item 9, replace

```markdown
and the first card dealt to a pile is its top (decided in the slice 2 review).
```

with

```markdown
and each card dealt goes on top of its pile (decided in the slice 2 review as the first card on top; re-ruled 2026-11-16, [Last Chronicle powers rulings](#last-chronicle-powers-rulings), item 7).
```

In the "Last Chronicle powers rulings" section, after item 6, add:

```markdown
7. **Teeming World deals each card on top of its pile.** The rest of the stack is dealt one card at a time to the Cradle, the Provinces and the Hinterland in turn, each card on top of its pile, so the last card dealt to a pile is its top. This re-rules [Foundations rulings](#foundations-rulings) item 9, whose first-card-on-top reading kept the alpha shuffle's unimplemented denizens off the pile tops. The shuffle is now uniform (owner, 2026-11-16).
```

In `docs/architecture/game-setup.md`, replace

```markdown
Cradle, Provinces and Hinterland discard piles, the first card dealt to a
pile its top and each later card beneath; and the world deck is the five
```

with

```markdown
Cradle, Provinces and Hinterland discard piles, each card dealt going on
top of its pile; and the world deck is the five
```

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/setup/SetupDeal.scala \
  src/test/scala/distantlands/gameplay/setup/SetupDealSuite.scala \
  src/test/scala/distantlands/gameplay/setup/AlteredFoundationSetupSuite.scala \
  docs/rules/ambiguities.md docs/architecture/game-setup.md
git commit -m "$(cat <<'EOF'
feat(setup): Teeming World deals each card on top of its pile

Last Chronicle powers ruling 7 re-rules Foundations ruling 9: with a
uniform shuffle, nothing needs the first card dealt on top.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

Add to the same `git add` every test and golden that Step 6 re-pinned, by name.

---

### Task 3: No implemented flag, no badge

**Files:** see the file map, row 3.

**Interfaces:**
- Consumes: nothing from Tasks 1 and 2.
- Produces: `CardDetailsProjection` has no `implemented` field. Its wire form has no `"implemented"` key, and the decoder refuses one (`exact`). `GamePresentationProjector` no longer reads `PowerImplementationStatus`, which leaves Task 4 free to delete it.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -i -w -E 'implemented|unimplemented' -- shared frontend DESIGN.md .impeccable src/main/scala/distantlands/application/GamePresentationProjector.scala src/test/scala/distantlands/application
```
Expected: the hits listed in Steps 2 to 7. `MapViewport.scala:6` ("Pointer capture is implemented by supported browsers") is unrelated; leave it.

- [ ] **Step 2: Remove the flag from the protocol**

- In `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala`, delete the line `implemented: Boolean = true,`.
- In `shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala`:
  - In `encodeCardFields`, delete the pair `"implemented" -> value.implemented,`. `"hidden" -> value.hidden,` ends its line.
  - In `decodeCard`, delete `"implemented", ` from the `exact` set.
  - Delete the line `implemented <- boolOr(value, "implemented", path, true)`.
  - Delete `implemented,` from the `yield CardDetailsProjection(...)` argument list.
- In `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`, delete the test `"a card defaults to implemented and round-trips implemented = false"` (its five lines).

- [ ] **Step 3: Remove the flag from the projector**

In `src/main/scala/distantlands/application/GamePresentationProjector.scala`:
- Delete the comment and the two members from `// A card is implemented only once every power it declares is implemented` through the end of `cardImplemented`.
- Delete `PowerImplementationStatus` from the `gameplay.powers` import. `PrintsPowers` stays, because `powerFaces` uses it.
- Delete the argument `implemented = ...` from the four `CardDetailsProjection` calls: in `edificeCardDetails`, and for the denizen, relic and legacy cases of `cardDetails`. Where the next argument followed on the same line, keep it, as in `hidden = hidden, powers = powerFaces(d))`.

```bash
git rm src/test/scala/distantlands/application/GamePresentationProjectorImplementedSuite.scala
```

- [ ] **Step 4: Remove the badge from the frontend**

In `frontend/src/main/scala/distantlands/frontend/CardFace.scala`:
- In `render`, delete the class element `if card.implemented then "" else "card-face-unimplemented"` and the comma before it, so the vector ends with `if dormantLegacy(card) then "card-face-dormant" else "")`.
- Replace the `aria-label` block with `node.setAttribute("aria-label", card.name)`.
- In the doc that lists the priority order, change "defense at its right end), live tokens, the power marks, unimplemented." to "defense at its right end), live tokens and the power marks.".
- In the face's parts, delete the last element `Option.when(!card.implemented)(text("span", "card-unimplemented-badge", "Unimplemented"))`. The vector ends `Option.when(marks.childNodes.length > 0)(marks)).flatten`.

In `frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala`, delete the two lines `if !card.implemented then` and `panel.appendChild(text("p", "card-unimplemented-badge", "Unimplemented"))`.

In `frontend/src/test/scala/distantlands/frontend/CardFaceSuite.scala`:
- In `"printed parts carry the printed mark and live state does not"`, delete `, implemented = false` from the `CardDetails(...)` call. Change the selector `".card-tokens.printed, " + ".card-unimplemented-badge.printed"` to `".card-tokens.printed"`.
- Delete the test `"an unimplemented card is marked by class, accessible name and badge"`.

- [ ] **Step 5: Remove the badge's styles**

In `frontend/styles.css`:
- Delete the token line `--unimplemented: #a15c4a;`.
- Delete the rule `.card-face-unimplemented { border-style: dashed; border-color: var(--unimplemented); }`.
- In the comment above `.card-face { --marks-bottom: ...`, replace "An Unimplemented band or a Replace badge lifts\n   the marks, and the band with them." with "A Replace badge lifts the marks, and the\n   band with them.".
- Delete the rule `.card-face:has(> .card-unimplemented-badge) {\n  --marks-bottom: calc(max(0.62em, 0.7rem) * 1.35 + 0.3em); }`.
- In the comment above `.card-power-marks`, replace "lifted above the unimplemented band or a Replace badge when either is\n   there." with "lifted above a Replace badge when one is there.".
- Delete the comment "A band across the card's foot, out to the dashed border, ..." and the two rules under it: `.card-unimplemented-badge { ... }` and `.card-face:has(> .card-unimplemented-badge) { padding-bottom: ... }`.
- In the next comment, replace "A face holding tokens keeps the band's room free, the Unimplemented\n   band's with it: what it prints stops above the band," with "A face holding tokens keeps the token band's room free: what it\n   prints stops above the band,".
- Replace

```css
.map-compact .card-face .card-defense, .map-compact .card-face .card-power-marks,
.map-compact .card-face .card-unimplemented-badge { display: none; }
```

with

```css
.map-compact .card-face .card-defense,
.map-compact .card-face .card-power-marks { display: none; }
```

- [ ] **Step 6: Remove the badge from the design record**

In `DESIGN.md`:
- Delete the token line `  unimplemented: "#a15c4a"`.
- In **States**, delete "`unimplemented` for a card the engine does not run yet;". Rejoin the line.
- Delete the whole **Unimplemented badge** bullet (5 lines).
- In **Cards**, **Border**, delete "dashed Unimplemented for a card the engine\n  does not run,".
- In **Face**, replace "lifted above the unimplemented band or a Replace badge" with "lifted above a Replace badge".
- In **Compact**, replace "Defense, the power\n  marks and the unimplemented badge are hidden" with "Defense and the power\n  marks are hidden".

In `.impeccable/design.json`, delete the `"unimplemented": { ... }` entry, and keep the JSON valid. Check with:
`python3 -c "import json;json.load(open('.impeccable/design.json'))"`

- [ ] **Step 7: Confirm and run**

Run: `git grep -n -i 'unimplemented' -- frontend shared DESIGN.md .impeccable`
Expected: no output.

Run: `./sbtw "testOnly distantlands.protocol.* distantlands.application.* distantlands.server.*"` and `./sbtw "frontend/test"`
Expected: PASS. The stylesheet suites under `distantlands.server` read `styles.css`, so they run here.

- [ ] **Step 8: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/WorldProjectionDtos.scala \
  shared/src/main/scala/distantlands/protocol/projection/WorldProjectionCodec.scala \
  shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala \
  src/main/scala/distantlands/application/GamePresentationProjector.scala \
  frontend/src/main/scala/distantlands/frontend/CardFace.scala \
  frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala \
  frontend/src/test/scala/distantlands/frontend/CardFaceSuite.scala \
  frontend/styles.css DESIGN.md .impeccable/design.json
git commit -m "$(cat <<'EOF'
refactor(ui): drop the implemented flag and the unimplemented badge

Every printed power runs, so no card face is marked.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: No implementation status

**Files:** see the file map, row 4.

**Interfaces:**
- Consumes: Tasks 1 and 3 removed the status's production readers. Only `HomelandsAndPlains`, docs and tests still name it.
- Produces: no `PowerImplementationStatus`, no `HomelandsAndPlains` object, no `unimplemented.txt`, no `UNIMPLEMENTED_SNAPSHOT`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -E 'PowerImplementationStatus|HomelandsAndPlains\b|UNIMPLEMENTED_SNAPSHOT|unimplemented\.txt|catalog\.holding' -- src frontend shared
git grep -n -i -E 'counts? as implemented|reads? as implemented|so the (card|face|power) counts|implemented status|is not implemented' -- src/main
```
Expected, first grep: `PowerImplementationStatus.scala`, its suite, `UnimplementedPowersSuite`, `CardCatalogSuite`, `HomelandsAndPlains.scala`, `OperationRestrictions.scala`, and about 40 suites that assert `PowerImplementationStatus.implemented(...)`. Slices 2 and 3 add `HidingPlaceSuite` and `BlastedPasturesSuite` to them. `PoweredSites.scala` names the file `HomelandsAndPlains.scala`, which stays.

Expected, second grep: the doc comments in the table of Step 6, plus `PrintedRule.scala`.

- [ ] **Step 2: Delete the status, its suites and the snapshot**

```bash
git rm src/main/scala/distantlands/gameplay/powers/PowerImplementationStatus.scala \
  src/test/scala/distantlands/gameplay/powers/PowerImplementationStatusSuite.scala \
  src/test/scala/distantlands/gameplay/powers/UnimplementedPowersSuite.scala \
  src/test/resources/powers/unimplemented.txt
```

`PowerImplementationStatusSuite` held one test that pins `OperationRestrictions.printedRules` itself. Move it into `src/test/scala/distantlands/gameplay/OperationRestrictionsSuite.scala`, and merge these imports with the file's own:

```scala
import distantlands.gameplay.operations.{AncientBloodline, GrandScepter,
  GrandScepterNegotiation, HallOfMinisters, OperationRestrictions,
  StillBoilingLake}
```

```scala
  test("the printed rules are Ancient Bloodline, the Hall of Ministers, the " +
      "Grand Scepter's restriction and negotiation term and Still Boiling " +
      "Lake"):
    assertEquals(OperationRestrictions.printedRules.map(_.id),
      Vector(AncientBloodline.id, HallOfMinisters.id, GrandScepter.id,
        GrandScepterNegotiation.id, StillBoilingLake.id))
```

- [ ] **Step 3: Delete the holding rule and the Homelands list**

In `src/test/scala/distantlands/gameplay/cards/CardCatalogSuite.scala`:
- Delete the whole test `"a card leaves the holding files once a power of it is implemented, and sits there until then"`.
- Delete `PowerImplementationStatus` from the `gameplay.powers` import.

In `src/main/scala/distantlands/gameplay/cards/HomelandsAndPlains.scala`, replace the doc comment and `object HomelandsAndPlains` (from `/** The eleven sites` through the line `val handlers: Set[PowerId] = ...`) with a plain comment:

```scala
// The eleven sites whose power no power object runs. The six Homelands'
// `homeland-<suit>` handlers are card play's Homeland rule,
// `CardPlayRules.homeland`, which writes its line as
// `RuleNotes.homelandDiscard`; the five Plains' `plains` handlers print
// "This site has no power".
```

The site objects below it stay. Remove `Site` from the `distantlands.catalog` import if nothing else in the file uses it.

- [ ] **Step 4: Fix the printed-rule docs**

In `src/main/scala/distantlands/gameplay/operations/OperationRestrictions.scala`, in the doc of `printedRules`, replace "the one list `printed`, `PowerImplementationStatus`,\n    * `ConsentPowers` and `RuleNotes` read." with "the one list `printed`, `ConsentPowers` and\n    * `RuleNotes` read.".

In `src/main/scala/distantlands/gameplay/operations/PrintedRule.scala`, replace "reads that list: the implemented\n  * status, the consent grid and the Game Log's notes." with "reads that list: the consent grid\n  * and the Game Log's notes.".

- [ ] **Step 5: Remove the status from every power suite**

Run: `git grep -l 'PowerImplementationStatus' -- src/test`
For each file:
1. Delete each `assert(PowerImplementationStatus.implemented(...))`, which may span two lines.
2. If the test then has no assertion left, delete the test. Examples: `"Iron Hand is implemented"`, `"both faces of the Baths are implemented"`, `"the ruined face counts as implemented"`.
3. If the test keeps other assertions, keep it. When its name speaks of being implemented or of leaving holding, rename it to say what it still checks. Examples:
   - `"Ancient City's Enduring is implemented"` keeps `assertEquals(AncientCityEnduring.id, AncientCitySite.enduring)`. Rename it `"Ancient City's Enduring is registered under its printed id"`.
   - `"Hall of Bandits is implemented"` becomes `"Hall of Bandits is registered under its printed id"`.
   - `"the card has left holding and its power is registered"` (QuestSetupSuite) becomes `"Quest to Distant Lands is registered among the setup legacies"`.
   - In AncientBloodlineSuite, the test keeps its `printed.exists` check. Rename it if its name says implemented.
4. Remove `PowerImplementationStatus` from the file's `gameplay.powers` import. Delete the import line if nothing else is left in it.

Leave `EconomyWalkerSuite`'s `"an unimplemented optional Economy power does not block a base Trade"` alone: it tests a power its test catalog never registers, not the status.

- [ ] **Step 6: Reword the inert powers' doc comments**

Each comment in this table says the power is registered so that its card counts as implemented. It stays registered (owner, 2026-11-16). The comment drops that reason and keeps what reads the power. The rules:
- Delete the clause or sentence that gives the reason. Its forms include "; it is registered so the card counts as implemented", "It is registered so the card counts as implemented.", "Registered so the card counts as implemented." and "it is registered so the power counts as implemented, as the Baths are".
- Keep "It hooks no window" or "This power hooks no window" and end it with a period.
- Where the sentence goes on, keep the rest as its own sentence. In `DesecratedGround`, "It is registered so the card counts as implemented, and it declares the kept line the card play writes." becomes "It declares the kept line the card play writes."
- Rewrap the comment to 80 columns.

| File | Object |
|---|---|
| `gameplay/chronicle/Baths.scala` | `StoneBaths`, `RuinedBaths` |
| `gameplay/chronicle/ShapeEmpireExemptions.scala` | `AncientCityEnduring`, `HallOfBandits` |
| `gameplay/chronicle/legacies/GreatArchitect.scala` | `GreatArchitect` |
| `gameplay/chronicle/legacies/OathsOfLoyalty.scala` | `OathsOfLoyalty` |
| `gameplay/chronicle/legacies/Rival.scala` | `Rival` |
| `gameplay/chronicle/legacies/Steadfast.scala` | `Steadfast` |
| `gameplay/chronicle/legacies/TheBrightBeacon.scala` | `TheBrightBeacon` |
| `gameplay/phases/rest/Arbiter.scala` | `Arbiter` |
| `gameplay/powers/action/NegotiationAdviserPowers.scala` | both powers |
| `gameplay/powers/action/NegotiationReachPowers.scala` | all three powers |
| `gameplay/powers/action/NegotiationSitePowers.scala` | its power |
| `gameplay/powers/banner/YewStaff.scala` | `YewStaff` |
| `gameplay/powers/cardplay/DesecratedGround.scala` | its power |
| `gameplay/powers/economy/Marriage.scala` | `Marriage` |
| `gameplay/powers/economy/PledgeToDiscord.scala` | `PledgeToDiscord` |
| `gameplay/powers/poweruse/BanditCrown.scala` | `BanditCrown` |
| `gameplay/powers/poweruse/GrandMask.scala` | `GrandMask` |
| `gameplay/setup/legacies/CircleOfSwords.scala` | `CircleOfSwords` |
| `gameplay/setup/legacies/ScepterBearer.scala` | `ScepterBearer` |

All paths are under `src/main/scala/distantlands/`. The grep in Step 1 is the full list; it may also show `HidingPlace` or `BlastedPastures` if slices 2 and 3 wrote such a comment.

Three more comments say something stale about implementation:
- `gameplay/operations/GrandScepter.scala`, `GrandScepterNegotiation`: replace "Listed among the printed rules so that the\n  * power reads as implemented." with "It is listed among the printed rules,\n  * which the consent grid and the Game Log's notes read.".
- `gameplay/powers/action/TempleOfAncestors.scala`: replace "The ruined face is a Chronicle step and is not implemented." with "The ruined face is a Chronicle step, `RuinedTempleWin`.".
- `gameplay/powers/setup/RuinedSiteSetupRules.scala`, `RuinedSchool`: replace "The School's\n  * intact face is not implemented (catalog batch 9, \"Out of scope\")." with "The School's\n  * intact face is a Challenge modifier, `SchoolOfVines`.".

- [ ] **Step 7: Confirm and run**

Run the two greps of Step 1 again.
Expected: the first prints only `PoweredSites.scala`'s mention of the file `HomelandsAndPlains.scala`. The second prints nothing. `LegacyChroniclePowers` and `LegacySetupPowers` ("whose ... powers are implemented") stay; they are true.

Run in the background: `./sbtw test`
Expected: PASS.

- [ ] **Step 8: Commit**

Stage every file this task changed, by name. List them with `git status --short`. The `git rm` in Step 2 already staged the deletions.

```bash
git commit -m "$(cat <<'EOF'
refactor(powers): retire the implementation status and its snapshot

Every printed power is accounted for, so no check of it remains (owner,
2026-11-16). The powers that hook no window stay registered; their docs
say what reads them.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Docs and the full gate

**Files:** see the file map, row 5.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -i -E 'PowerImplementationStatus|ImplementedCardCatalog|ShufflePolicy|unimplemented|holding rule|catalog/holding' -- docs README.md ':!docs/superpowers'
```
Expected: the hits in Steps 2 to 5, plus historical records to leave alone:
- `docs/rules/ambiguities.md` item 29 (High Priest's history) and the new ruling 7;
- `docs/testing/audits/walker-follow-ups-audit.md` (a dated audit);
- `docs/rules/implementation-traceability.md`, whose **Unimplemented** status names a rule with no executable behavior, not this machinery.

- [ ] **Step 2: Power resolution**

In `docs/architecture/rule-resolution.md`, replace the whole section from `## Unimplemented powers` up to `## Replay` with:

```markdown
## Unregistered powers

A power no catalog covers contributes nothing: the walker gathers only
registered powers, so an unregistered automatic power has no effect and an
unregistered player-selected power is not offered. It never fails or blocks
a command, and nothing records it. Every printed power is registered in
`WalkerPowerCatalog` or `PhasePowerCatalog`, enforced by
`OperationRestrictions.printedRules`, or is a site handler that a game rule
runs (a Homeland's, which card play's Homeland rule runs) or that prints no
power (a Plains'). The reviewed stub registries that once produced an
`IgnoredRulesRecorded` diagnostic were retired on 2026-10-23
([decision](../superpowers/specs/2026-10/2026-10-23-reviewed-catalog-retirement-decision.md)).
```

In "Adding a power" in the same file, delete step 5 (`Drop its line from src/test/resources/powers/unimplemented.txt.`) and renumber step 6 to 5.

- [ ] **Step 3: Architecture notes**

- `docs/architecture/codebase-structure.md`, step 4 of adding a power: delete the line "Registration marks the handler implemented (`PowerImplementationStatus`)."
- `docs/architecture/gameplay-modules.md`: replace "Unimplemented powers are ignored; they\nnever fail or block a command, and nothing records them." with "A power no catalog registers is ignored; it\nnever fails or blocks a command, and nothing records it."
- `docs/architecture/all-exile-round-endings.md`: replace "An applicable unimplemented Rest\nhandler is ignored" with "An applicable unregistered Rest\nhandler is ignored".

- [ ] **Step 4: Catalog and rules**

In `docs/catalog/README.md`, replace

```markdown
the Forge requirements of three-slot sites, that every registered card power
is printed in the catalog, and that a card sits in a `catalog/holding`
package exactly while `PowerImplementationStatus` reports none of its powers
implemented. No card does today, so the package does not exist; the rule
keeps an unimplemented card apart if one is ever added.
```

with

```markdown
the Forge requirements of three-slot sites, and that every registered card
power is printed in the catalog.
```

In `docs/rules/ambiguities.md`, Component-data dependencies item 1, replace "Every printed power has executable behavior, so `src/test/resources/powers/unimplemented.txt` is empty." with "Every printed power has executable behavior." Item 5 needs no change after slice 3.

- [ ] **Step 5: Roadmap and spec status**

In `docs/ROADMAP.md`, delete the whole **Retire the unimplemented-card machinery across the stack** item: from its first line,

```markdown
- [ ] **Retire the unimplemented-card machinery across the stack.** Every
```

through `  every printed power is wired in.` and the blank line after it. **Reasons on the remaining options.** then follows the item before it directly. Run `git grep -n 'Retire the unimplemented' -- docs/ROADMAP.md`. Expected: no output. Slice 3 already deleted the other item that named it.

In the spec, append " Slice 4 landed 2026-11-16." to the status line, after slice 3's sentence. Use the merge date instead if it differs.

- [ ] **Step 6: The full gate**

Run in the background: `./sbtw test`, then `./sbtw "frontend/test"`.
Expected: every test passes.
Run: `python3 scripts/check-architecture.py` and `python3 scripts/check-markdown-links.py`.
Expected: both pass.

Run:
```bash
git grep -n -E 'PowerImplementationStatus|ImplementedCardCatalog|ShufflePolicy|UnimplementedPowersSuite|card-unimplemented-badge' -- ':!docs/superpowers'
```
Expected: no output.

- [ ] **Step 7: Commit**

```bash
git add docs/architecture/rule-resolution.md docs/architecture/codebase-structure.md \
  docs/architecture/gameplay-modules.md docs/architecture/all-exile-round-endings.md \
  docs/catalog/README.md docs/rules/ambiguities.md docs/ROADMAP.md \
  docs/superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md
git commit -m "$(cat <<'EOF'
docs: retire the unimplemented-card machinery and close the last Chronicle powers

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

Merge into `0.2.0-alpha` with `--no-ff` only when the owner asks.

---

## Self-review

- Spec coverage:
  - The implemented-first deal: Task 1. That covers `ShufflePolicy`, `ImplementedCardCatalog`, the generator's filler, and `SetupDeal`'s pile order through ruling 7 in Task 2.
  - The `implemented` flag and its codec, and the frontend class, accessible-name suffix and badge: Task 3.
  - `UnimplementedPowersSuite`, `unimplemented.txt`, the holding rule and `PowerImplementationStatus` itself: Task 4.
  - The inert powers kept with reworded docs: Task 4, Step 6.
  - The docs the spec lists: Tasks 2, 3 and 5.
  - Spec Testing, slice 4: the full gates and the absence greps in Tasks 3 and 5.
- Behavior change: only ruling 7, the order of a Teeming World pile. Every other task removes code that no longer changes an outcome. The argument is in Task 1, and Task 1 Step 7 tests it.
- Names used across tasks: `FirstGameChronicleGenerator.generate(catalog, name, random)`, `TooFewSuitDenizens(suit, denizens)` and `GeneratedFirstGamePlanFactory(catalog, random)`.
