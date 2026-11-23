# Catalog Batch 13, Slice 6: Chronicle Cards Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stone Baths and Ruined Baths (E04) change Resolve Legacies, and
Ruined Temple (E10 ruined) takes a pledge at Setup or When Explored that
rules the bandit sites and doubles the legacy points of a pledged winner.
With them `catalog/holding/` is empty.

**Architecture:** The Baths are rules that Sun steps 2 and 3 read
(`BathsRules.exempt`), registered as walker powers that hook no window, as
Steadfast is, so the card counts as implemented. Step 2 records the
legacies Stone Baths keeps dormant, and the reactivation that Crown
Chancellor and Offer Citizenship run skips them. Ruined Temple's pledge is
a SETUP / WHEN EXPLORED face shaped like the Doors: a shared base,
extracted from `DoorsRule`, finds the placer and offers the steps to
Setup's ordered pawn window. A burn records the pledge on the game. At the
win, a step at the end of `ChronicleWinnerProcedure` gives the pledged
final winner the bandit sites, and a pledged winner's legacy bonus doubles
wherever it is scored.

**Why the kept legacies are recorded.** Legacies ruling 2 says that after
Sun step 3 every dormant legacy on a seated lineage is newly dormant, so
nothing records which ones are. Stone Baths breaks that: a legacy it keeps
was dormant before the Chronicle. An Exile at the Baths may also have
newly dormant legacies from step 3, so nothing in the state at Crown
Chancellor tells the two apart. Step 2 therefore records the kept ones,
and only when the Baths keep something, so no other Chronicle's journal
changes.

**Why the win step is at the end of the procedure.** Royal Line's choice
and a fulfilled Conspirator prediction both change the winner inside
`ChronicleWinnerProcedure`. The pledge belongs to the final winner, so the
step runs after `ConspiratorReveal`. That is still before the Sun, so Sun
step 2's legacy goals see the sites the winner now rules (ruling 18).

**Why the Temple's offer stands while it resolves.** Slice 4's ordered
window gathers the offers again after every resolution, and a power whose
offer drops out while its own steps run loses its place. The Temple offers
itself to a player with a secret, so the burn would withdraw it. It
offers itself to a player who has pledged too, which the burn records in
the same batch.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Catalog batch 13 design](../../specs/2026-11/2026-11-14-catalog-batch-13-design.md),
rulings 4, 11 and 14 to 19, and section "Slice 6: Chronicle cards". Rulings
14, 18 and 19 were widened by the owner on 2026-11-15 while this plan was
written, and the spec records them.

## Global Constraints

- Stone Baths (ruling 14): in Sun step 2, a seated player whose pawn is at
  its site does not discard their unmet dormant legacies. A met one still
  activates. A kept one is not newly dormant: Crown Chancellor and Offer
  Citizenship leave it dormant.
- Ruined Baths (ruling 15): in Sun step 3, a seated Exile whose pawn is at
  its site keeps every active legacy. Steadfast and the cap are unaffected.
- The Baths are read at the Sun, when the edifice and pawns are still on
  the map. A pawn off the map is at no site (ruling 16).
- Ruined Temple's pledge (ruling 17) is "you may", for the placer at Setup
  and the explorer at an Explore (ruling 11). A player with no secret is
  not asked. Several placers at Setup may each pledge. The burn takes a
  facedown secret when the player has one, else a faceup one, with no
  question: a facedown secret is spent, so it is never worth more.
- Ruined Temple at the win (ruling 18): the final winner, if pledged, takes
  every bandit site. Its bandits go to the bandit supply, and it takes one
  of the winner's warbands (Imperial for an Imperial winner), the bank
  topped up when short. Crown Chancellor then treats those sites as the
  winner's.
- Ruined Temple's double (ruling 19): a pledged winner's legacy bonus
  doubles at Score Lineage Points and at Chronicler. The 3 base points
  never double. Gifts come from the doubled total. Iron Hand's scoring is
  unchanged: its holder is an Exile at the Throne, so never the winner.
- Setup decision ids name the placer, `setup.<slug>.<step>.<player>`, which
  Setup's log narrates by prefix. Explore ids are fixed,
  `power.<slug>.<step>`, listed in `narratedDecisions`.
- Lines (spec, "Lines", in the Game Log's "Source: text" form):
  - "Ruined Temple: p2 burned a secret."; a declined pledge writes nothing
  - "p2 took rule of 3 bandit sites with Ruined Temple", under the final
    winner step; nothing when there is no bandit site
  - "p1 scored 9 lineage points (3 base, 6 from Beast, doubled by Ruined
    Temple)" at Score Lineage Points
  - "p1 scored 4 lineage points (4 from Beast, doubled by Ruined Temple)" at
    Chronicler
  - the Baths write no line.
- Saved games need not replay across this slice (the "no old replay
  support" rule), but every new operation round-trips on the wire.
- No production Scala file exceeds 800 lines (`check-architecture.py`).
- Docs, comments and commit messages are written in plain English.

## Files

- Modify: `src/main/scala/distantlands/model/GameState.scala`. Two fields
  on `CurrentGameState`: `templePledges` and `keptDormant`.
- Modify: `src/main/scala/distantlands/model/TurnOperation.scala`. `PledgeTemple`.
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala`. `LegaciesKeptDormant`.
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`,
  `TurnStateOperations.scala`, `ChronicleStateOperations.scala`. Apply the two.
- Modify: `src/main/scala/distantlands/serialization/TurnOperationCodec.scala`,
  `ChronicleOperationCodec.scala`. Encode and decode the two.
- Create: `src/main/scala/distantlands/gameplay/chronicle/Baths.scala`.
  `StoneBathsCard`, moved from holding, `BathsRules`, and the two
  registered powers `StoneBaths` and `RuinedBaths`.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ResolveLegacies.scala`.
  Steps 2 and 3 skip the exempt players, step 2 records the kept legacies,
  and the reactivation skips them.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSunProcedure.scala`.
  A comment that ruling 2 no longer makes true.
- Delete: `src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala`,
  the last file in `catalog/holding/`.
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala`.
  Imports `StoneBathsCard` from its new place.
- Create: `src/main/scala/distantlands/gameplay/powers/setup/PlacedPawnEdificeRule.scala`.
  The base the Doors and Ruined Temple share.
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/AmberDoorsRules.scala`.
  `DoorsRule` extends the new base.
- Create: `src/main/scala/distantlands/gameplay/powers/setup/RuinedTempleRules.scala`.
  `RuinedTemple`, the pledge.
- Create: `src/main/scala/distantlands/gameplay/chronicle/RuinedTempleWin.scala`.
  The win step, `doubles` and the card's name.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWinnerProcedure.scala`.
  Runs the win step last and declares its line.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/LineageScoring.scala`.
  `scoredBonus` and the doubled line.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/Chronicler.scala`.
  Scores `scoredBonus` and writes the doubled line.
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`.
  Registers `StoneBaths`, `RuinedBaths` and `RuinedTemple`.
- Modify: `src/test/resources/powers/unimplemented.txt`. Drops three lines.
- Modify: `src/test/resources/notes/declared-keys.txt`. Rewritten by the
  snapshot.
- Modify: `src/test/scala/distantlands/testkit/Table.scala`.
  `pledgedAtRuinedTemple`.
- Modify: `src/test/scala/distantlands/gameplay/setup/PawnWindowOffers.scala`.
  `offer`, the optional offer `steps` reads.
- Create: `src/test/scala/distantlands/gameplay/TempleAndBathsOperationsSuite.scala`.
- Modify: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`,
  `ChronicleOperationWireSuite.scala`.
- Create: `src/test/scala/distantlands/gameplay/chronicle/BathsSuite.scala`.
- Create: `src/test/scala/distantlands/gameplay/powers/setup/RuinedTempleSuite.scala`.
- Create: `src/test/scala/distantlands/gameplay/chronicle/RuinedTempleWinSuite.scala`.
- Modify: `src/test/scala/distantlands/gameplay/chronicle/LineageScoringSuite.scala`,
  `src/test/scala/distantlands/gameplay/chronicle/legacies/ChroniclerSuite.scala`.
- Modify docs: `docs/rules/ambiguities.md`, `docs/rules/rules-reference.md`,
  `docs/rules/implementation-traceability.md`, `docs/catalog/README.md`,
  `docs/ROADMAP.md`, the spec's status line.

## Running things

- One suite: `./sbtw "testOnly distantlands.gameplay.chronicle.BathsSuite"`.
- Every test: `./sbtw test` (about 7 minutes; run it in the background).
- Architecture: `python3 scripts/check-architecture.py`.
- Links: `python3 scripts/check-markdown-links.py`.
- The note keys snapshot: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`.
  Find the suite's package with `grep -rl "NOTE_KEYS_SNAPSHOT" src/test`
  if that name does not match.

---

### Task 1: The pledge and the kept legacies as state

**Files:**
- Modify: `src/main/scala/distantlands/model/GameState.scala` (the end of `CurrentGameState`'s fields, after `legacyHands`)
- Modify: `src/main/scala/distantlands/model/TurnOperation.scala` (after `ResolvePrediction`)
- Modify: `src/main/scala/distantlands/model/ChronicleOperation.scala` (after `LegaciesScored`)
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/TurnStateOperations.scala`
- Modify: `src/main/scala/distantlands/gameplay/operations/ChronicleStateOperations.scala`
- Modify: `src/main/scala/distantlands/serialization/TurnOperationCodec.scala`
- Modify: `src/main/scala/distantlands/serialization/ChronicleOperationCodec.scala`
- Test: `src/test/scala/distantlands/gameplay/TempleAndBathsOperationsSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
- Test: `src/test/scala/distantlands/serialization/ChronicleOperationWireSuite.scala`

**Interfaces:**
- Produces: `CurrentGameState.templePledges: Set[PlayerId]` and
  `CurrentGameState.keptDormant: Set[LegacyId]`, both default empty.
- Produces: `final case class PledgeTemple(player: PlayerId) extends TurnOperation, LeafOperation`.
- Produces: `final case class LegaciesKeptDormant(legacies: Vector[LegacyId]) extends ChronicleOperation, LeafOperation`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/TempleAndBathsOperationsSuite.scala`:

```scala
package distantlands.gameplay

import distantlands.gameplay.operations.{OperationPipeline, OperationPolicy}
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Ruined Temple's pledge store and the legacies Stone Baths keeps dormant
  * (catalog batch 13, rulings 14 and 17): `PledgeTemple` records a
  * player's pledge for the game, and `LegaciesKeptDormant` records the
  * kept legacies, so no reactivation takes them for newly dormant. */
class TempleAndBathsOperationsSuite extends munit.FunSuite:
  private def applied(ready: ReadyGame, ops: CoreOperation*)
      : Either[?, ReadyGame] =
    OperationPipeline.run(ready, ops.toVector, OperationPolicy.Permissive,
      Vector.empty)(Right(_)).map(_.state)

  /** The quiet three-player table. */
  private val table: ReadyGame = Table.start.ready

  test("a pledge records its player, once"):
    val pledged = accepted(applied(table, PledgeTemple(p1)))
    assertEquals(pledged.game.current.templePledges, Set(p1))
    assert(applied(pledged, PledgeTemple(p1)).isLeft)

  test("a player who is not seated cannot pledge"):
    assert(applied(table, PledgeTemple(PlayerId("p9"))).isLeft)

  test("kept legacies are recorded, each once"):
    val kept = accepted(applied(table,
      LegaciesKeptDormant(Vector(SeerCard.id))))
    assertEquals(kept.game.current.keptDormant, Set(SeerCard.id))
    assert(applied(kept, LegaciesKeptDormant(Vector(SeerCard.id))).isLeft)
    assert(applied(table,
      LegaciesKeptDormant(Vector(SeerCard.id, SeerCard.id))).isLeft)
```

In `WalkerOperationWireSuite.scala`, add two entries to the list of
operations that round-trip, right after
`ResolvePrediction(player, fulfilled = false),`:

```scala
    PledgeTemple(player),
    LegaciesKeptDormant(Vector(legacy)),
```

In `ChronicleOperationWireSuite.scala`, add after the "scored legacies"
test:

```scala
  test("legacies kept dormant name each legacy, and an empty list decodes " +
      "to a typed WireError"):
    val encoded = encodedStreamThatRoundTrips("legacies-kept-dormant",
      Vector(recordedOperation(LegaciesKeptDormant(Vector(LegacyId("L02"),
        LegacyId("L22"))))))
    assertEquals(ujson.read(encoded)(0)("payload")("step")("ops")(0),
      ujson.Obj("kind" -> "legacies-kept-dormant",
        "legacies" -> ujson.Arr("L02", "L22")))
    val injected = ujson.read(encoded).arr
    injected(0)("payload")("step")("ops")(0)("legacies") = ujson.Arr()
    assertEquals(GameEventWire.decodeStream(ujson.write(injected)),
      Left(WireError.InvalidValue("$[0].payload.step.ops[0].legacies",
        "keeps at least one legacy")))
```

Read the "scored legacies" test just above it first: if its last
assertion compares a different `WireError` shape, copy that shape.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.TempleAndBathsOperationsSuite"`
Expected: compile errors, `Not found: PledgeTemple` and
`Not found: LegaciesKeptDormant`.

- [ ] **Step 3: Add the state fields**

In `GameState.scala`, `legacyHands` is the last field of
`CurrentGameState` and has no trailing comma. Replace:

```scala
    legacyHands: Map[PlayerId, Vector[LegacyId]] = Map.empty
):
```

with:

```scala
    legacyHands: Map[PlayerId, Vector[LegacyId]] = Map.empty,
    // The players who burned a secret at Ruined Temple this game (catalog
    // batch 13, ruling 17), read at the win and by the scoring that
    // doubles. Public, as the burn was made openly. Not walker scratch: it
    // lasts the game, and a new game starts without it. Never projected.
    templePledges: Set[PlayerId] = Set.empty,
    // The dormant legacies Stone Baths kept at Sun step 2 (ruling 14):
    // not newly dormant, so Crown Chancellor and Offer Citizenship leave
    // them dormant. Public. Never projected.
    keptDormant: Set[LegacyId] = Set.empty
):
```

- [ ] **Step 4: Add the operations**

In `TurnOperation.scala`, after `ResolvePrediction`:

```scala
/** Records `player`'s Ruined Temple pledge in
  * `CurrentGameState.templePledges` (catalog batch 13, ruling 17). A
  * seated player pledges once a game. */
final case class PledgeTemple(player: PlayerId) extends TurnOperation,
    LeafOperation
```

In `ChronicleOperation.scala`, after `LegaciesScored`:

```scala
/** Records the dormant legacies Stone Baths kept at Sun step 2 (catalog
  * batch 13, ruling 14) in `CurrentGameState.keptDormant`. They are not
  * newly dormant, so no reactivation turns them active. A legacy already
  * recorded is a doubled operation. */
final case class LegaciesKeptDormant(legacies: Vector[LegacyId])
    extends ChronicleOperation, LeafOperation:
  require(legacies.nonEmpty, "a keep records at least one legacy")
```

- [ ] **Step 5: Apply them**

In `TurnStateOperations.scala`, after `resolvePrediction`:

```scala
  def pledgeTemple(ready: ReadyGame, player: PlayerId)
      : Either[OperationError, ReadyGame] =
    val current = ready.game.current
    if current.player(player).isEmpty then Left(InvalidDescription(
      s"${player.value} must be seated to pledge"))
    else if current.templePledges(player) then Left(InvalidDescription(
      s"${player.value} has already pledged at Ruined Temple"))
    else Right(ready.updateCurrent(_.copy(
      templePledges = current.templePledges + player)))
```

Check how `writePrediction` names `InvalidDescription` in that file (an
import of `OperationError.InvalidDescription` or a qualified name) and do
the same.

In `ChronicleStateOperations.scala`, after `legaciesScored`:

```scala
  def legaciesKeptDormant(ready: ReadyGame, legacies: Vector[LegacyId])
      : Either[OperationError, ReadyGame] =
    val kept = ready.game.current.keptDormant
    legacies.find(kept) match
      case Some(legacy) => Left(InvalidDescription(
        s"${legacy.value} is already kept dormant"))
      case None if legacies.distinct.size != legacies.size =>
        Left(InvalidDescription("a keep names each legacy once"))
      case None => Right(ready.updateCurrent(current => current.copy(
        keptDormant = current.keptDormant ++ legacies)))
```

In `LeafApplication.scala`, after the `ResolvePrediction` case:

```scala
    case PledgeTemple(player) =>
      TurnStateOperations.pledgeTemple(ready, player)
```

and after the `LegaciesScored` case:

```scala
    case LegaciesKeptDormant(legacies) =>
      ChronicleStateOperations.legaciesKeptDormant(ready, legacies)
```

- [ ] **Step 6: Encode and decode them**

In `TurnOperationCodec.scala`, in the encoder after the
`ResolvePrediction` case:

```scala
      case PledgeTemple(player) => ujson.Obj("kind" -> "pledge-temple",
        "player" -> player.value)
```

and in the decoder after the `"resolve-prediction"` case:

```scala
    case "pledge-temple" => Right(PledgeTemple(PlayerId(value("player").str)))
```

In `ChronicleOperationCodec.scala`, in the encoder after the
`LegaciesScored` case:

```scala
      case LegaciesKeptDormant(legacies) => ujson.Obj(
        "kind" -> "legacies-kept-dormant",
        "legacies" -> legacies.map(legacy => ujson.Str(legacy.value)))
```

and in the decoder after the `"legacies-scored"` case:

```scala
    case "legacies-kept-dormant" =>
      val legacies = value("legacies").arr.toVector.map(id => LegacyId(id.str))
      Either.cond(legacies.nonEmpty, LegaciesKeptDormant(legacies),
        InvalidValue(s"$path.legacies", "keeps at least one legacy"))
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.TempleAndBathsOperationsSuite distantlands.serialization.WalkerOperationWireSuite distantlands.serialization.ChronicleOperationWireSuite"`
Expected: PASS. A compile error naming a non-exhaustive match on
`TurnOperation` or `ChronicleOperation` elsewhere means another file lists
every operation: add the new case there beside `WritePrediction` or
`LegaciesScored`.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/model src/main/scala/distantlands/gameplay/operations src/main/scala/distantlands/serialization src/test/scala/distantlands/gameplay/TempleAndBathsOperationsSuite.scala src/test/scala/distantlands/serialization
git commit -m "$(cat <<'EOF'
feat(model): record Ruined Temple pledges and Stone Baths' kept legacies

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Stone Baths and Ruined Baths

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/Baths.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ResolveLegacies.scala` (`dormancy`, `reactivation`, `dormant`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSunProcedure.scala` (the Crown comment)
- Delete: `src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala`
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala:4`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala` (the edifice list and its doc comment)
- Modify: `src/test/resources/powers/unimplemented.txt`
- Test: `src/test/scala/distantlands/gameplay/chronicle/BathsSuite.scala`

**Interfaces:**
- Consumes: `LegaciesKeptDormant`, `CurrentGameState.keptDormant` (Task 1).
- Produces: `object StoneBathsCard` in package `distantlands.gameplay.chronicle`;
  `BathsRules.exempt(ready: ReadyGame, side: EdificeSide): Set[PlayerId]`;
  `case object StoneBaths`, `case object RuinedBaths`, both `ContributingPower`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/chronicle/BathsSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.OathRules
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import distantlands.gameplay.chronicle.legacies.IronHandCard
import distantlands.gameplay.powers.banner.TheNeedleCard
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.setup.legacies.SeerCard
import distantlands.model._
import distantlands.testkit.{RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Stone Baths and Ruined Baths (E04), CHRONICLE (SUN): a player whose
  * pawn is at the site keeps their dormant legacies at Sun step 2 (Stone
  * Baths), or, as an Exile, their active ones at step 3 (Ruined Baths)
  * (catalog batch 13, rulings 14 to 16). Every game here is p1's win, in
  * the Chronicle Phase with Start Chronicle answered, the Baths at Broken
  * Peaks. Seer's goal is the Darkest Secret, which nobody holds unless a
  * test gives it; The Needle's is met only by a player who rules a site,
  * and p1 rules none. */
class BathsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog))
  private def won: Table = ChronicleWalk.chronicleWonByP1

  private def sun(table: Table)(using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleSun)

  /** `side` of the Baths at Broken Peaks, `bathing`'s pawn there and every
    * other pawn at Ancient City. */
  private def bathsWith(table: Table, side: EdificeSide,
      bathing: PlayerId*): Table =
    Vector(p1, p2, p3).foldLeft(table.edifice(StoneBathsCard, side,
      at = BrokenPeaksSite))((t, player) => t.pawn(player,
        if bathing.contains(player) then BrokenPeaksSite else AncientCitySite))

  private def legaciesOf(ready: ReadyGame, player: PlayerId)
      : Vector[LegacyState] =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies

  test("both faces of the Baths are implemented"):
    assert(PowerImplementationStatus.implemented(
      StoneBathsCard.intact.power.id))
    assert(PowerImplementationStatus.implemented(
      StoneBathsCard.ruined.power.id))

  test("at Stone Baths an unmet dormant legacy stays dormant, and a met " +
      "one still activates"):
    val run = sun(bathsWith(won.darkestSecret(Some(p2), 1)
      .legacy(p2, SeerCard, active = false)
      .legacy(p2, IronHandCard, active = false), EdificeSide.Intact, p2))
    assertEquals(legaciesOf(run.ready, p2), Vector(
      LegacyState(SeerCard.id, active = true),
      LegacyState(IronHandCard.id, active = false)))
    assert(!run.ready.game.current.commonCards.legacyDeck
      .contains(IronHandCard.id))
    assertEquals(run.ready.game.current.keptDormant, Set(IronHandCard.id))

  test("a player away from Stone Baths still discards an unmet dormant " +
      "legacy, and nothing is recorded"):
    val run = sun(bathsWith(won.legacy(p3, SeerCard, active = false),
      EdificeSide.Intact, p2))
    assertEquals(legaciesOf(run.ready, p3), Vector.empty)
    assert(run.ready.game.current.commonCards.legacyDeck.contains(SeerCard.id))
    assert(!RecordedOps.all(run.events).exists(
      _.isInstanceOf[LegaciesKeptDormant]))

  test("a pawn off the map is at no site (ruling 16)"):
    val offMap = bathsWith(won.legacy(p2, SeerCard, active = false),
      EdificeSide.Intact, p2).update(_.updateCurrent(current => current.copy(
        players = current.players.map(player =>
          if player.player == p2 then player.copy(pawnSite = None)
          else player))))
    assertEquals(legaciesOf(sun(offMap).ready, p2), Vector.empty)

  test("an Exile winner at Stone Baths reactivates only the newly dormant " +
      "legacy at Crown Chancellor"):
    // The Needle goes dormant in step 3; Seer was kept in step 2.
    val run = sun(bathsWith(won.legacy(p1, TheNeedleCard)
      .legacy(p1, SeerCard, active = false), EdificeSide.Intact, p1))
    assertEquals(legaciesOf(run.ready, p1), Vector(
      LegacyState(TheNeedleCard.id, active = true),
      LegacyState(SeerCard.id, active = false)))

  test("at Ruined Baths an Exile keeps an unmet active legacy active, and " +
      "an Exile elsewhere does not"):
    val run = sun(bathsWith(won.legacy(p2, IronHandCard)
      .legacy(p3, SeerCard), EdificeSide.Ruined, p2))
    assertEquals(legaciesOf(run.ready, p2),
      Vector(LegacyState(IronHandCard.id, active = true)))
    assertEquals(legaciesOf(run.ready, p3),
      Vector(LegacyState(SeerCard.id, active = false)))

  test("Ruined Baths does not keep a dormant legacy at step 2"):
    val run = sun(bathsWith(won.legacy(p2, SeerCard, active = false),
      EdificeSide.Ruined, p2))
    assertEquals(legaciesOf(run.ready, p2), Vector.empty)
```

Before running, check two fixture facts and adjust the test, not the
code, if either is false: Iron Hand's goal is unmet for p2 in `won` (the
first `ResolveLegaciesSuite` test shows it unmet for p3, and its goal
counts ruled sites), and `Legacies` order on a lineage is the order
`Table.legacy` added them.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.BathsSuite"`
Expected: compile error, `Not found: StoneBathsCard` in package
`distantlands.gameplay.chronicle`.

- [ ] **Step 3: Create `Baths.scala` and delete the holding file**

Create `src/main/scala/distantlands/gameplay/chronicle/Baths.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.{Edifice, EdificeFace, Locked, PrintedPower}
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

object StoneBathsCard extends Edifice(EdificeId("E04"), Suit.Discord):
  object intact extends EdificeFace("Stone Baths") with Locked:
    val power = PrintedPower(PowerId("edifice.e04.intact"),
      persistent = true, cost = Cost.free,
      text = "CHRONICLE (SUN): Players with a pawn at this site do not " +
        "discard their dormant legacies.")
    val powers: Vector[PrintedPower] = Vector(power)
  object ruined extends EdificeFace("Ruined Baths"):
    val power = PrintedPower(PowerId("edifice.e04.ruined"),
      persistent = true, cost = Cost.free,
      text = "CHRONICLE (SUN): Exiles at this site don't need to meet the " +
        "goals of their active legacies to keep them active.")
    val powers: Vector[PrintedPower] = Vector(power)

/** Who the Baths exempt (catalog batch 13, rulings 14 to 16). The Sun is
  * the Chronicle's first task, so the edifice and the pawns are still on
  * the map when Resolve Legacies reads them, and a pawn off the map is at
  * no site. */
object BathsRules:
  /** The seated players whose pawns stand at the site where E04 shows
    * `side`: anyone at Stone Baths (Sun step 2), only Exiles at Ruined
    * Baths (Sun step 3). Nobody when that face is not on the map. */
  def exempt(ready: ReadyGame, side: EdificeSide): Set[PlayerId] =
    val current = ready.game.current
    current.edificeSite(StoneBathsCard.id, side).fold(Set.empty)(site =>
      current.players.collect {
        case player if player.pawnSite.contains(site) &&
            (side == EdificeSide.Intact ||
              Imperials.role(ready, player.player).contains(Role.Exile)) =>
          player.player
      }.toSet)

/** Stone Baths (E04 intact): Sun step 2 reads it
  * (`ResolveLegacies.flip`). It hooks no window; it is registered so the
  * card counts as implemented, as Steadfast is. */
case object StoneBaths extends ContributingPower:
  val id: PowerId = StoneBathsCard.intact.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** Ruined Baths (E04 ruined): Sun step 3 reads it
  * (`ResolveLegacies.dormancy`). It hooks no window; it is registered so
  * the card counts as implemented. */
case object RuinedBaths extends ContributingPower:
  val id: PowerId = StoneBathsCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

`Imperials` is the object `ResolveLegacies.dormancy` already calls; if
it is not in `distantlands.model`, import it from where
`ResolveLegacies.scala` does.

Delete the holding file, now empty of cards:

```bash
git rm src/main/scala/distantlands/catalog/holding/HoldingEdifices.scala
```

In `NewFoundations.scala`, replace line 4,
`import distantlands.catalog.holding._`, with:

```scala
import distantlands.gameplay.chronicle.StoneBathsCard
```

In `WalkerPowerCatalog.scala`, in the edifice list, change
`ProvingGrounds, EmptyGrounds, LostDoors, AmberDoors, HallOfMockery,` so
the Baths follow the Doors, and rewrap the list to stay under 80 columns:

```scala
      Vector(Dazzle, GreatMarket, BanditMarket, GreatForge, BrokenForge,
        ProvingGrounds, EmptyGrounds, LostDoors, AmberDoors, StoneBaths,
        RuinedBaths, HallOfMockery, LostGuardians, RuinedSchool, BrokenHorn,
        BrokenClock, FallenSpire, TakeWealthLimit, TakeWealthPawnGate,
        LightFingers, Beloved, TheNeedle, Arbiter, ConspiracyWhenPlayed) ++
```

Import them with `import distantlands.gameplay.chronicle.{RuinedBaths, StoneBaths}`.
In the doc comment above `object WalkerPowerCatalog`, after the sentence
"Steadfast hooks none, as Sun step 3 reads it.", add:
"The Baths hook none either, as Sun steps 2 and 3 read them."

In `src/test/resources/powers/unimplemented.txt`, delete the lines
`edifice.e04.intact` and `edifice.e04.ruined`.

- [ ] **Step 4: Change Sun steps 2 and 3 and the reactivation**

In `ResolveLegacies.scala`, replace `dormant`:

```scala
  /** Step 2's dormant legacies: met ones turn active, unmet ones go to the
    * bottom of the deck, except a Stone Baths player's, which stay dormant
    * and are recorded as kept (catalog batch 13, ruling 14), so the
    * reactivations below leave them dormant. The Baths write no line. */
  private def dormant(catalog: ExecutableCatalog): Operation =
    Branch((state, _) =>
      val bathed = BathsRules.exempt(state, EdificeSide.Intact)
      val resolved = for
        player <- LegacyGoals.turnOrder(state)
        legacy <- legaciesOf(state, player) if !legacy.active
      yield (player, legacy.id, meets(catalog, state, legacy.id, player))
      val (kept, settled) = resolved.partition((player, _, met) =>
        !met && bathed(player))
      if resolved.isEmpty then Vector.empty
      else BuildOps((live, _) => Right(settled.map { (player, id, met) =>
          val at = PositionedLocation(Location.Legacies(lineageOf(live, player)))
          if met then Flip(id, at.location, Orientation.FaceUp)
          else Move(Piece.Card(id), at,
            PositionedLocation.legacyDeckBottom)
        } ++ Option.when(kept.nonEmpty)(
          LegaciesKeptDormant(kept.map(_._2))).toVector),
        required = true) +:
        settled.map((player, id, met) => line(if met then activated
          else discarded, NoteArg.Player(player), NoteArg.Card(id))))
```

In `dormancy`, read Ruined Baths at the step's start and skip its
Exiles. Replace:

```scala
    Branch((state, _) =>
      val lapsed = for
        player <- LegacyGoals.turnOrder(state)
          if Imperials.role(state, player).contains(Role.Exile)
```

with:

```scala
    Branch((state, _) =>
      val bathed = BathsRules.exempt(state, EdificeSide.Ruined)
      val lapsed = for
        player <- LegacyGoals.turnOrder(state)
          if Imperials.role(state, player).contains(Role.Exile) &&
            !bathed(player)
```

and add to `dormancy`'s doc comment: "A seated Exile at Ruined Baths keeps
every active legacy (catalog batch 13, ruling 15)."

In `reactivation`, replace:

```scala
      val dormant = legaciesOf(state, player).filterNot(_.active).map(_.id)
```

with:

```scala
      val dormant = legaciesOf(state, player).filterNot(_.active).map(_.id)
        .filterNot(state.game.current.keptDormant)
```

and in its doc comment replace "Every dormant legacy on a seated lineage
after Sun step 3 is newly dormant (ruling 2)." with "Every dormant legacy
on a seated lineage after Sun step 3 is newly dormant (ruling 2) but the
ones Stone Baths kept, which `keptDormant` records (catalog batch 13,
ruling 14)."

In `ChronicleSunProcedure.scala`, in the Crown comment, replace:

```scala
        // An Imperial winner holds no dormant legacy after step 3 (ruling
        // 2), so this asks nothing of them.
```

with:

```scala
        // An Imperial winner holds no newly dormant legacy after step 3
        // (ruling 2), so this asks nothing of them; a legacy Stone Baths
        // kept is not newly dormant (catalog batch 13, ruling 14).
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.BathsSuite distantlands.gameplay.chronicle.ResolveLegaciesSuite distantlands.gameplay.chronicle.OfferCitizenshipSuite distantlands.gameplay.cards.CardCatalogSuite distantlands.gameplay.powers.PowerImplementationStatusSuite"`
Expected: PASS. If `PowerImplementationStatusSuite` does not exist, find
the suite that reads `unimplemented.txt` with
`grep -rl "unimplemented.txt" src/test` and run that one instead.

- [ ] **Step 6: Commit**

```bash
git add -A src/main/scala/distantlands/gameplay/chronicle src/main/scala/distantlands/catalog src/main/scala/distantlands/gameplay/cards/NewFoundations.scala src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala src/test/resources/powers/unimplemented.txt src/test/scala/distantlands/gameplay/chronicle/BathsSuite.scala
git commit -m "$(cat <<'EOF'
feat(edifices): Stone Baths and Ruined Baths keep legacies at the Sun

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 3: A base for edifices that act on the placer

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/setup/PlacedPawnEdificeRule.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/setup/AmberDoorsRules.scala` (`DoorsRule`)

**Interfaces:**
- Produces: `abstract class PlacedPawnEdificeRule extends ContributingPower` with
  `def edifice: EdificeId`, `protected def side: EdificeSide`,
  `protected def slug: String`,
  `protected final def decision(step: String, placer: Option[PlayerId]): String`,
  `protected final def card: PowerSourceRef`,
  `protected def offers(ready: ReadyGame, actor: PlayerId): Boolean` (default `true`),
  `protected def steps(catalog: ExecutableCatalog, actor: PlayerId, placer: Option[PlayerId]): Vector[Operation]`.
- Behaviour of the Doors is unchanged: `LostDoorsSuite`, `AmberDoorsSuite`
  and `DoorsPowerOrderSuite` guard it.

- [ ] **Step 1: Create the base**

Create `src/main/scala/distantlands/gameplay/powers/setup/PlacedPawnEdificeRule.scala`:

```scala
package distantlands.gameplay.powers.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  OfferResolution, PowerCtx, Transform}
import distantlands.model._

/** A SETUP / WHEN EXPLORED edifice face for "you" who placed your pawn at
  * its site (catalog batch 13, rulings 11 and 17), shaped as `ForgeRule`
  * is: the placer in Setup's pawn window, the explorer after an Explore
  * (`EdificeSetupSupport.pawnPlacement`).
  *
  * Setup is one walk in which several players may place at the site, so
  * there a face's decision ids name the placer, under `setup.`, which
  * Setup's log narrates. An Explore has one explorer, so there the ids are
  * fixed and each face narrates them itself.
  *
  * Setup's pawn window orders its powers and gathers the offers again
  * after each one resolves (ruling 4), so `offers` must stay true for a
  * player while their own steps run.
  */
abstract class PlacedPawnEdificeRule extends ContributingPower:
  def edifice: EdificeId
  protected def side: EdificeSide
  /** The face's name in its decision ids. */
  protected def slug: String

  final def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  protected final def card: PowerSourceRef = PowerSourceRef.Card(edifice)

  /** The id of this face's `step` question: at Setup, for `placer`; at an
    * Explore, `None`. */
  protected final def decision(step: String,
      placer: Option[PlayerId]): String = placer match
    case Some(player) => s"setup.$slug.$step.${player.value}"
    case None => s"power.$slug.$step"

  /** Whether the face offers itself to `actor`, who placed their pawn at
    * its site. Every face does but one that needs something of them. */
  protected def offers(ready: ReadyGame, actor: PlayerId): Boolean = true

  private def at(ctx: PowerCtx): Option[(PlayerId, SiteId)] =
    for
      edificeSite <- ctx.state.game.current.edificeSite(edifice, side)
      placement <- EdificeSetupSupport.pawnPlacement(ctx)
      if placement._2 == edificeSite && offers(ctx.state, placement._1)
    yield placement

  override def applicable(ctx: PowerCtx): Boolean = at(ctx).isDefined

  /** The face's steps for `actor`, asking under `placer`'s ids. */
  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation]

  /** Setup's pawn window orders its powers, so there the steps are offered
    * to the placer (ruling 4). An Explore folds them in. */
  final def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.SetupPawnPlaced -> Vector(OfferResolution(ctx =>
      at(ctx).map((actor, _) => ResolutionOffer(
        EdificeSetupSupport.option(edifice),
        steps(ctx.catalog, actor, Some(actor)))))),
    PowerWindow.WhenExplored -> Vector(Transform((ctx, ops) =>
      at(ctx).fold(ops)((actor, _) =>
        ops ++ steps(ctx.catalog, actor, None)))))
```

- [ ] **Step 2: Make `DoorsRule` extend it**

In `AmberDoorsRules.scala`, replace the whole `DoorsRule` class (from
`sealed abstract class DoorsRule extends ContributingPower:` to the end of
its `contributions`) with:

```scala
sealed abstract class DoorsRule extends PlacedPawnEdificeRule:
  final val edifice: EdificeId = AmberDoorsCard.id

  /** `actor`'s lineage in `ready`. */
  protected final def lineageOf(ready: ReadyGame, actor: PlayerId)
      : Either[OathViolation, LineageId] =
    ChronicleTasks.lineageOf(ready, actor).toRight(
      OathViolation.InvalidEventOrder(s"${actor.value} has no lineage"))

  /** The legacies `actor`'s lineage holds in `ready`, in lineage order. */
  protected final def held(ready: ReadyGame, actor: PlayerId)
      : Vector[LegacyState] =
    ChronicleTasks.lineageOf(ready, actor).toVector.flatMap(lineage =>
      ready.game.campaign.lineages.get(lineage).toVector.flatMap(_.legacies))
```

Keep the doc comment above it, and shorten it to what the class still
says: "E05, both faces (catalog batch 13, rulings 11 to 13), a
`PlacedPawnEdificeRule`. Each question sits in its own `Branch` after the
draw's, so a walk resumed at it selects the same shape: it lists a hand or
a lineage that nothing changed since the park." Then drop the imports the
compiler reports unused (`Contribution`, `ContributingPower`,
`OfferResolution`, `PowerCtx`, `Transform` are the likely ones).

- [ ] **Step 3: Run the Doors suites to verify nothing changed**

Run: `./sbtw "testOnly distantlands.gameplay.powers.setup.LostDoorsSuite distantlands.gameplay.powers.setup.AmberDoorsSuite distantlands.gameplay.setup.DoorsPowerOrderSuite"`
Expected: PASS, 20 tests.

- [ ] **Step 4: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/setup
git commit -m "$(cat <<'EOF'
refactor(edifices): share the placed-pawn edifice base the Doors use

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 4: Ruined Temple's pledge

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/setup/RuinedTempleRules.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Modify: `src/test/scala/distantlands/testkit/Table.scala`
- Modify: `src/test/scala/distantlands/gameplay/setup/PawnWindowOffers.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/setup/RuinedTempleSuite.scala`

**Interfaces:**
- Consumes: `PlacedPawnEdificeRule` (Task 3), `PledgeTemple` and
  `CurrentGameState.templePledges` (Task 1).
- Produces: `case object RuinedTemple` with
  `def pledgeDecisionId(placer: Option[PlayerId]): String`,
  `val keep, burn: DecisionOptionRef.Button`, `val burned: NoteKey`,
  `def secretsOf(ready: ReadyGame, actor: PlayerId): Int`.
- Produces (test kit): `Table.pledgedAtRuinedTemple(player: PlayerId): Table`;
  `PawnWindowOffers.offer(catalog, power, ready, placer, site): Option[ResolutionOffer]`.

- [ ] **Step 1: Add the test helpers**

In `Table.scala`, after `trackedPawn`:

```scala
  /** `player` burned a secret at Ruined Temple this game. */
  def pledgedAtRuinedTemple(player: PlayerId): Table =
    update(_.updateCurrent(current => current.copy(
      templePledges = current.templePledges + player)))
```

In `PawnWindowOffers.scala`, split `steps` so a suite can see that a power
offers nothing. Replace the body of `object PawnWindowOffers` with:

```scala
  /** `power`'s offer in `ready`, once `placer` has placed their pawn at
    * `site`, if it makes one. */
  def offer(catalog: ExecutableCatalog, power: ContributingPower,
      ready: ReadyGame, placer: PlayerId, site: SiteId)
      : Option[ResolutionOffer] =
    val placed = Vector(Answered(SetupProcedure.pawnDecisionId(placer),
      ChooseOneAnswer(DecisionOptionRef.Site(site)), placer))
    val ctx = PowerCtx(catalog, ready, placer, power.source,
      PowerWindow.SetupPawnPlaced, Vector.empty,
      OrderedWindow(placer, Vector.empty), answered = placed)
    power.contributions(PowerWindow.SetupPawnPlaced)
      .collectFirst { case resolution: OfferResolution =>
        resolution.offer(ctx) }.flatten

  /** `power`'s offered steps in `ready`, once `placer` has placed their
    * pawn at `site`. Fails the test when the power offers nothing. */
  def steps(catalog: ExecutableCatalog, power: ContributingPower,
      ready: ReadyGame, placer: PlayerId, site: SiteId)(
      using munit.Location): Vector[Operation] =
    present(offer(catalog, power, ready, placer, site),
      s"${power.id.value}'s offer").steps
```

- [ ] **Step 2: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/powers/setup/RuinedTempleSuite.scala`:

```scala
package distantlands.gameplay.powers.setup

import distantlands.gameplay.OathRules
import distantlands.gameplay.actions.explore.ExploreProcedure
import distantlands.gameplay.cards.{AncientCitySite, BuriedGiantSite,
  DeepWoodsSite}
import distantlands.gameplay.powers.{NoteText, WalkerPowerCatalog}
import distantlands.gameplay.powers.action.TempleOfAncestorsCard
import distantlands.gameplay.setup.{FirstGameSetupFixture, PawnWindowOffers,
  SetupProcedure, SetupWalkDriver}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.{WalkerCompleted, WalkerDice,
  WalkerPowers}
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{Answers, PhasePowerHarness, Table}
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Ruined Temple (E10 ruined), SETUP / WHEN EXPLORED: "If you place your
  * pawn here, you may burn a secret. If you do and then win the game,
  * score double the legacy points and take rule of all bandit sites."
  * The pledge (catalog batch 13, ruling 17); `RuinedTempleWinSuite` holds
  * the win. */
class RuinedTempleSuite extends munit.FunSuite:
  private val powers = WalkerPowers.selected(
    WalkerPowerCatalog.default(catalog), Vector.empty)

  /** Ruined Temple at Ancient City in p1's game. */
  private def templeAtAncientCity: Table =
    Table.start.edifice(TempleOfAncestorsCard.id, EdificeSide.Ruined,
      at = AncientCitySite)

  /** Ruined Temple's Setup steps for p1, who placed their pawn at Ancient
    * City, walked with `answers`. */
  private def walkedAtSetup(table: Table,
      answers: Answers = PartialFunction.empty): SetupWalkDriver.SetupWalked =
    val ready = table.ready
    SetupWalkDriver.driveWithEvents(ready, Sequence(PawnWindowOffers.steps(
      catalog, RuinedTemple, ready, p1, AncientCitySite.id)),
      TriggeredProcedureRef.Setup, powers, answers = answers)

  private val setupPledge = RuinedTemple.pledgeDecisionId(Some(p1))

  private val burning: Answers =
    case park if park.decisionId.startsWith("setup.ruined-temple.pledge.") ||
        park.decisionId == RuinedTemple.pledgeDecisionId(None) =>
      ChooseOneAnswer(RuinedTemple.burn)

  private def boardOf(ready: ReadyGame, player: PlayerId): PlayerBoard =
    ready.game.current.player(player).get.board

  private def lines(events: Vector[OathEvent]): Vector[String] =
    NoteText.said(RuinedTemple.id, RuinedTemple.noteKeys, events).map(_.text)

  test("the placer may keep their secrets or burn one, Keep first"):
    val run = walkedAtSetup(templeAtAncientCity.secrets(p1, faceUp = 1))
    val asked = run.asked.filter(_.decisionId == setupPledge)
    assertEquals(asked.map(_.owner), Vector(p1))
    asked.head.query match
      case one: DecisionQuery.ChooseOne =>
        assertEquals(one.options.map(_.ref),
          Vector(RuinedTemple.keep, RuinedTemple.burn))
      case other => fail(s"expected a choose-one, got $other")

  test("keeping writes nothing and pledges nothing"):
    val run = walkedAtSetup(templeAtAncientCity.secrets(p1, faceUp = 1))
    assertEquals(run.ready.game.current.templePledges, Set.empty[PlayerId])
    assertEquals(boardOf(run.ready, p1).faceUpSecrets, 1)
    assertEquals(lines(run.events), Vector.empty)

  test("burning takes a facedown secret first, pledges, and says so"):
    val run = walkedAtSetup(templeAtAncientCity
      .secrets(p1, faceUp = 1, faceDown = 1), burning)
    assertEquals(run.ready.game.current.templePledges, Set(p1))
    assertEquals((boardOf(run.ready, p1).faceUpSecrets,
      boardOf(run.ready, p1).faceDownSecrets), (1, 0))
    assertEquals(lines(run.events), Vector("p1 burned a secret."))

  test("with only faceup secrets, burning takes a faceup one"):
    val run = walkedAtSetup(templeAtAncientCity.secrets(p1, faceUp = 2),
      burning)
    assertEquals(boardOf(run.ready, p1).faceUpSecrets, 1)

  test("a player with no secret is offered nothing"):
    val ready = templeAtAncientCity.secrets(p1, faceUp = 0).ready
    assertEquals(PawnWindowOffers.offer(catalog, RuinedTemple, ready, p1,
      AncientCitySite.id), None)

  test("a player who has pledged is still offered it, so the offer stands " +
      "after the burn"):
    val ready = templeAtAncientCity.secrets(p1, faceUp = 0)
      .pledgedAtRuinedTemple(p1).ready
    assert(PawnWindowOffers.offer(catalog, RuinedTemple, ready, p1,
      AncientCitySite.id).isDefined)

  test("every player who places their pawn at the Temple in Setup may " +
      "pledge"):
    // The generic driver places every pawn at Ancient City, the first site.
    val fresh = Table.from(FirstGameSetupFixture.freshReady)
      .edifice(TempleOfAncestorsCard.id, EdificeSide.Ruined,
        at = AncientCitySite)
    val seated = fresh.ready.game.current.players.map(_.player)
    val staged = seated.foldLeft(fresh)(_.secrets(_, faceUp = 1)).ready
    val tree = accepted(SetupProcedure.build(catalog, staged,
      staged.game.current.turn.activePlayer, Vector.empty))
    val walked = SetupWalkDriver.driveWithEvents(staged, tree,
      TriggeredProcedureRef.Setup, powers, answers = burning)
    assertEquals(walked.ready.game.current.templePledges, seated.toSet)
    // Pawns are placed from the first player, not in seat order.
    assertEquals(walked.asked.map(_.decisionId).filter(
      _.startsWith("setup.ruined-temple.pledge.")).toSet,
      seated.map(player => RuinedTemple.pledgeDecisionId(Some(player))).toSet)

  // ---- When explored ----

  /** Every defense die two shields, so an Explore succeeds on its first
    * roll, and every shuffle reverses its pile (as `LostDoorsSuite`). */
  private val shieldsAndReversedShuffles: WalkerDice = new WalkerDice:
    def roll(kind: DiceKind, count: Int)
        : Either[OathViolation, Vector[DieFace]] =
      Right(Vector.fill(count)(DefenseDieFace.TwoShields))
    override def shuffle(count: Int): Either[OathViolation, Vector[Int]] =
      Right((0 until count).reverse.toVector)

  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = shieldsAndReversedShuffles)

  /** p1 explores into Deep Woods, where the Temple of Ancestors is stored,
    * and the explored site's edifice comes ruined. */
  private def exploring(table: Table): Table = table
    .foundation(FoundationNumber.I, FoundationFace.Altered)
    .mapCutIntoAtlas(3).pawn(p1, BuriedGiantSite).banditsRefilled
    .storedInAtlas(DeepWoodsSite, TempleOfAncestorsCard.id)

  test("when explored, the explorer may burn a secret under the Explore's " +
      "fixed id"):
    val rolled = accepted(rules.startWalker(Ready(
      exploring(Table.start.secrets(p1, faceUp = 1)).ready),
      ActionRef.Explore, p1))
    val placed = accepted(rules.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    // The walk parks at the pledge: resolving it under this id is the
    // proof, since the walker refuses an answer to any other question.
    val done = accepted(rules.resolveWalker(placed.state, p1,
      RuinedTemple.pledgeDecisionId(None), ChooseOneAnswer(RuinedTemple.burn)))
    assert(done.events.contains(WalkerCompleted(ActionRef.Explore)),
      done.events)
    val ready = PhasePowerHarness.after(done)
    assertEquals(ready.game.current.templePledges, Set(p1))
    assertEquals(lines(rolled.events ++ placed.events ++ done.events),
      Vector("p1 burned a secret."))

  test("when explored, an explorer with no secret is asked nothing"):
    val rolled = accepted(rules.startWalker(Ready(
      exploring(Table.start.secrets(p1, faceUp = 0)).ready),
      ActionRef.Explore, p1))
    val placed = accepted(rules.resolveWalker(rolled.state, p1,
      ExploreProcedure.endDecisionId,
      ChooseOneAnswer(ExploreProcedure.endOption(AtlasEnd.Recent))))
    assert(placed.events.contains(WalkerCompleted(ActionRef.Explore)),
      placed.events)
```

The board type is the one `PlayerState.board` returns; if it is not
named `PlayerBoard`, use that name in `boardOf`.

- [ ] **Step 3: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.setup.RuinedTempleSuite"`
Expected: compile error, `Not found: RuinedTemple`.

- [ ] **Step 4: Write `RuinedTemple`**

Create `src/main/scala/distantlands/gameplay/powers/setup/RuinedTempleRules.scala`:

```scala
package distantlands.gameplay.powers.setup

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.powers.PowerAnswers
import distantlands.gameplay.powers.action.TempleOfAncestorsCard
import distantlands.model._

/** Ruined Temple (E10 ruined), SETUP / WHEN EXPLORED: "If you place your
  * pawn here, you may burn a secret. If you do and then win the game,
  * score double the legacy points and take rule of all bandit sites."
  * (catalog batch 13, rulings 11 and 17). A placer with a secret is
  * asked, Keep first; on Burn, one secret burns, facedown first, since a
  * spent secret is never worth more, and `PledgeTemple` records the
  * pledge for the game. A player with no secret is offered nothing, and a
  * player who pledged is still offered it, so the offer stands while the
  * burn resolves (ruling 4). The win and the double read the pledge
  * (`RuinedTempleWin`). Line: "p2 burned a secret."; a kept secret writes
  * nothing. */
case object RuinedTemple extends PlacedPawnEdificeRule:
  val id: PowerId = TempleOfAncestorsCard.ruined.power.id
  def edifice: EdificeId = TempleOfAncestorsCard.id
  protected def side: EdificeSide = EdificeSide.Ruined
  protected def slug: String = "ruined-temple"

  def pledgeDecisionId(placer: Option[PlayerId]): String =
    decision("pledge", placer)
  val keep: DecisionOptionRef.Button = DecisionOptionRef.Button("keep")
  val burn: DecisionOptionRef.Button = DecisionOptionRef.Button("burn")

  private val notes = NoteSet(id)
  /** "p2 burned a secret." */
  val burned: NoteKey = notes.key(NoteKey.Used, Vector(NotePart.Arg(0),
    NotePart.Text(" burned a secret.")))
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** The burn's line tells the answer. */
  override def narratedDecisions: Set[String] = Set(pledgeDecisionId(None))

  /** The secrets on `actor`'s board, faceup and facedown. */
  def secretsOf(ready: ReadyGame, actor: PlayerId): Int =
    ready.game.current.player(actor).fold(0)(player =>
      player.board.faceUpSecrets + player.board.faceDownSecrets)

  override protected def offers(ready: ReadyGame, actor: PlayerId): Boolean =
    secretsOf(ready, actor) > 0 || ready.game.current.templePledges(actor)

  protected def steps(catalog: ExecutableCatalog, actor: PlayerId,
      placer: Option[PlayerId]): Vector[Operation] =
    val ask = pledgeDecisionId(placer)
    Vector(Branch((live, _) =>
      if secretsOf(live, actor) == 0 then Vector.empty
      else Vector(
        Decide(ask, actor, DecisionQuery.ChooseOne(Vector(
          DecisionOption.Button(keep, "Keep your secrets"),
          DecisionOption.Button(burn, "Burn a secret")),
          heading = Some("Ruined Temple: you may burn a secret; if you " +
            "then win, score double the legacy points and take rule of " +
            "all bandit sites"))),
        Branch((_, pending) =>
          if !PowerAnswers.one(pending, ask).contains(burn) then Vector.empty
          else Vector(
            BuildOps((ready, _) => Right(burnOne(ready, actor) :+
              PledgeTemple(actor)), required = true),
            Note(id, _ => Some(burned(card, NoteArg.Player(actor)))))))))

  /** One of `actor`'s secrets, facedown when they have one. */
  private def burnOne(ready: ReadyGame, actor: PlayerId)
      : Vector[CoreOperation] =
    val facedown = ready.game.current.player(actor)
      .exists(_.board.faceDownSecrets > 0)
    val side = if facedown then SecretSide.FaceDown else SecretSide.FaceUp
    Vector(Burn.secrets(1, PositionedLocation(Location.PlayArea(actor),
      side = Some(side)), Some(actor)))
```

Register it: in `WalkerPowerCatalog.scala`'s edifice list put
`RuinedTemple` right after `RuinedBaths`, rewrapping to stay under 80
columns. It is in package `distantlands.gameplay.powers.setup`, which the
file already imports for the Doors; check the import line and add it
there.

In `src/test/resources/powers/unimplemented.txt`, delete the line
`edifice.e10.ruined`.

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.setup.RuinedTempleSuite distantlands.gameplay.powers.setup.LostDoorsSuite distantlands.gameplay.setup.DoorsPowerOrderSuite distantlands.gameplay.cards.CardCatalogSuite"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers src/test/resources/powers/unimplemented.txt src/test/scala/distantlands/testkit/Table.scala src/test/scala/distantlands/gameplay/setup/PawnWindowOffers.scala src/test/scala/distantlands/gameplay/powers/setup/RuinedTempleSuite.scala
git commit -m "$(cat <<'EOF'
feat(edifices): Ruined Temple takes a pledge for a burnt secret

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 5: Ruined Temple at the win

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/RuinedTempleWin.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleWinnerProcedure.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/RuinedTempleWinSuite.scala`

**Interfaces:**
- Consumes: `CurrentGameState.templePledges` (Task 1),
  `Table.pledgedAtRuinedTemple` (Task 4).
- Produces: `RuinedTempleWin.name: String`,
  `RuinedTempleWin.doubles(ready: ReadyGame, player: PlayerId): Boolean`,
  `RuinedTempleWin.step: Operation`, `RuinedTempleWin.tookRule: NoteKey`,
  `RuinedTempleWin.noteKeys: Vector[NoteKey]`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/chronicle/RuinedTempleWinSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{BrokenPeaksSite, BuriedGiantSite}
import distantlands.gameplay.chronicle.legacies.{RoyalLine, RoyalLineCard}
import distantlands.gameplay.powers.WalkerPowerCatalog
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.{RecordedOps, Table}
import distantlands.testkit.Table.{p1, p2, p3}

/** Ruined Temple at the win (catalog batch 13, ruling 18): the pledged
  * final winner takes every bandit site, once Royal Line and Conspirator
  * have settled who won and before the Sun. Every game here ended with
  * bandits at Broken Peaks and Buried Giant, among any others. */
class RuinedTempleWinSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** `table` at the game's end, before the Chronicle, with bandits at two
    * known sites. */
  private def ended(table: Table): Table = table
    .bandits(BrokenPeaksSite, 2).bandits(BuriedGiantSite, 1)
    .turn(p1, Phase.RoundEnd)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, ended(table), procedure, answers,
      after = PhaseTransitionRef.FinishRest)

  /** The sites holding bandits in `table` once it has ended. */
  private def banditSites(table: Table): Vector[SiteId] =
    val current = ended(table).ready.game.current
    current.map.inPlay.filter(site =>
      current.map.sites(site).forces.count(ForceKind.Bandit) > 0)

  private def forcesAt(ready: ReadyGame, sites: Vector[SiteId])
      : Vector[SiteForces] =
    sites.map(ready.game.current.map.sites(_).forces)

  private def kindOf(ready: ReadyGame, player: PlayerId): ForceKind =
    PlayerForceKind.of(ready, ready.game.current.player(player).get).get

  private def lines(run: ChronicleWalk.Run): Vector[String] =
    NoteText.said(ProcedureNotes.id(TriggeredProcedureRef.ChronicleWinner),
      RuinedTempleWin.noteKeys, run.events).map(_.text)

  private def won: Table = ChronicleWalk.chronicleWonByP1

  test("a pledged Exile winner takes every bandit site with one warband"):
    val table = won.pledgedAtRuinedTemple(p1)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner)
    assertEquals(forcesAt(run.ready, sites), sites.map(_ =>
      SiteForces.Occupied(kindOf(run.ready, p1), 1)))
    assertEquals(lines(run),
      Vector(s"p1 took rule of ${sites.size} bandit sites with Ruined Temple"))

  test("Crown Chancellor then turns those sites Imperial for an Exile " +
      "winner"):
    val table = won.pledgedAtRuinedTemple(p1)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleSun)
    assertEquals(forcesAt(run.ready, sites), sites.map(_ =>
      SiteForces.Occupied(ForceKind.Imperial, 1)))

  test("a pledged Imperial winner takes them with Imperial warbands"):
    val table = won.imperial(p1).pledgedAtRuinedTemple(p1)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner)
    assertEquals(forcesAt(run.ready, sites), sites.map(_ =>
      SiteForces.Occupied(ForceKind.Imperial, 1)))

  test("a winner who did not pledge takes nothing, even when another " +
      "player pledged"):
    val table = won.pledgedAtRuinedTemple(p2)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner)
    assert(forcesAt(run.ready, sites).forall(_.count(ForceKind.Bandit) > 0))
    assertEquals(lines(run), Vector.empty)
    assert(!RuinedTempleWin.doubles(run.ready, p2))

  test("a winner whose bank runs short has it topped up"):
    val table = won.pledgedAtRuinedTemple(p1).warbandsInBank(p1, 1)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner)
    assertEquals(RecordedOps.all(run.events).collect {
      case WarbandsAdded(kind, count) => kind -> count },
      Vector(kindOf(run.ready, p1) -> (sites.size - 1)))

  test("the final winner Royal Line names is the one who takes the sites"):
    val table = won.imperial(p1, p2, p3)
      .update(_.updateCurrent(_.copy(result =
        Some(GameResult(p1, VictoryKind.StableRegime)))))
      .legacy(p3, RoyalLineCard).pledgedAtRuinedTemple(p2)
    val sites = banditSites(table)
    val run = through(table, TriggeredProcedureRef.ChronicleWinner, {
      case decide if decide.decisionId == RoyalLine.decisionId(p3) =>
        DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Player(p2)) })
    assertEquals(forcesAt(run.ready, sites), sites.map(_ =>
      SiteForces.Occupied(ForceKind.Imperial, 1)))
    assertEquals(lines(run),
      Vector(s"p2 took rule of ${sites.size} bandit sites with Ruined Temple"))
```

If `ChronicleWalk.through` never sees `WalkerCompleted(ChronicleWinner)`
because the final winner step completes under another procedure ref, walk
`through` to `TriggeredProcedureRef.ChronicleSun` in the first test
instead, and keep the line assertion.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.RuinedTempleWinSuite"`
Expected: compile error, `Not found: RuinedTempleWin`.

- [ ] **Step 3: Write `RuinedTempleWin`**

Create `src/main/scala/distantlands/gameplay/chronicle/RuinedTempleWin.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powers.PlayerFacts
import distantlands.gameplay.powers.action.TempleOfAncestorsCard
import distantlands.model._

/** Ruined Temple's pledge at the win (catalog batch 13, rulings 18 and
  * 19). The final winner is the one Royal Line and Conspirator leave, so
  * `step` is the last step of `ChronicleWinnerProcedure`, before the Sun:
  * Sun step 2's legacy goals then see the sites the winner rules. A
  * pledged winner's bandit sites each lose their bandits to the bandit
  * supply and take one of the winner's warbands, Imperial for an Imperial
  * winner, the bank topped up when short. Crown Chancellor and Shape
  * Empire then treat them as the winner's. Line: "p2 took rule of 3
  * bandit sites with Ruined Temple"; nothing with no bandit site. */
object RuinedTempleWin:
  /** The face's printed name, which the lines name. */
  val name: String = TempleOfAncestorsCard.ruined.name

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleWinner)
  private val notes = NoteSet(noteId)
  val tookRule: NoteKey = notes.key("ruined-temple-took-rule", Vector(
    NotePart.Arg(0), NotePart.Text(" took rule of "), NotePart.Arg(1),
    NotePart.Text(" "), NotePart.Plural(1, "bandit site", "bandit sites"),
    NotePart.Text(s" with $name")))
  /** The lines `step` writes, on the final winner step's note id. */
  def noteKeys: Vector[NoteKey] = notes.declared

  /** Whether `player` won the game and pledged at Ruined Temple. */
  def doubles(ready: ReadyGame, player: PlayerId): Boolean =
    val current = ready.game.current
    current.templePledges(player) && current.result.exists(_.winner == player)

  /** The pledged winner's bandit sites, read when reached. */
  val step: Operation = Branch((state, _) =>
    state.game.current.result.map(_.winner).filter(doubles(state, _))
      .toVector.flatMap(winner =>
        val sites = banditSites(state)
        if sites.isEmpty then Vector.empty
        else PlayerFacts.forceKind(state, winner).fold(
          violation => Vector(ChronicleTasks.failing(violation)),
          kind => Vector(
            BuildOps((live, _) => Right(ruled(live, kind, sites)),
              required = true),
            Note(noteId, _ => Some(tookRule.sourceless(
              NoteArg.Player(winner), NoteArg.Number(sites.size))))))))

  /** Every site in play that holds bandits, with how many. */
  private def banditSites(ready: ReadyGame): Vector[(SiteId, Int)] =
    val current = ready.game.current
    current.map.inPlay.flatMap(site => current.map.sites.get(site)
      .map(_.forces.count(ForceKind.Bandit)).filter(_ > 0).map(site -> _))

  private def ruled(ready: ReadyGame, kind: ForceKind,
      sites: Vector[(SiteId, Int)]): Vector[CoreOperation] =
    def at(site: SiteId) = PositionedLocation(Location.Site(site))
    val short = sites.size - PlayerFacts.banked(ready, kind)
    Option.when[CoreOperation](short > 0)(WarbandsAdded(kind, short))
      .toVector ++ sites.flatMap((site, bandits) => Vector(
        Kill(Piece.Warbands(ForceKind.Bandit, bandits), at(site)),
        Move(Piece.Warbands(kind, 1),
          PositionedLocation(Location.WarbandBank(kind)), at(site))))
```

- [ ] **Step 4: Run the step last in the procedure**

In `ChronicleWinnerProcedure.scala`, replace:

```scala
  def noteKeys: Vector[NoteKey] = RoyalLine.winnerNoteKeys ++
    ConspiratorReveal.noteKeys
```

with:

```scala
  def noteKeys: Vector[NoteKey] = RoyalLine.winnerNoteKeys ++
    ConspiratorReveal.noteKeys ++ RuinedTempleWin.noteKeys
```

and replace:

```scala
    yield Sequence(ChronicleWindows.step(PowerWindow.ChronicleFinalWinner,
      Vector.empty) +: ConspiratorReveal.steps)
```

with:

```scala
    yield Sequence((ChronicleWindows.step(PowerWindow.ChronicleFinalWinner,
      Vector.empty) +: ConspiratorReveal.steps) :+ RuinedTempleWin.step)
```

Add to the object's doc comment, after the Conspirator sentence: "Then
Ruined Temple's pledged winner takes the bandit sites
(`RuinedTempleWin`; catalog batch 13, ruling 18)."

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.RuinedTempleWinSuite distantlands.gameplay.chronicle.legacies.RoyalLineSuite distantlands.gameplay.chronicle.legacies.ConspiratorRevealSuite"`
Expected: PASS. Drop `ConspiratorRevealSuite` from the command if
`grep -rl "class ConspiratorRevealSuite" src/test` finds nothing, and run
the suite that `grep -rl "ConspiratorReveal" src/test` finds. If the walk
refuses the `Kill` or the `Move` as an operation the final winner step
may not record, find the policy with
`grep -rn "ChronicleWinner" src/main/scala/distantlands/gameplay/operations`
and stop to ask: do not widen a policy without the owner.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle src/test/scala/distantlands/gameplay/chronicle/RuinedTempleWinSuite.scala
git commit -m "$(cat <<'EOF'
feat(chronicle): a pledged winner takes rule of every bandit site

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 6: The doubled legacy points

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/LineageScoring.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/Chronicler.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/LineageScoringSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/chronicle/legacies/ChroniclerSuite.scala`

**Interfaces:**
- Consumes: `RuinedTempleWin.doubles`, `RuinedTempleWin.name` (Task 5).
- Produces: `LineageScoring.scoredBonus(ready: ReadyGame, catalog: ExecutableCatalog, player: PlayerId): Vector[(Suit, Int)]`,
  `LineageScoring.scoredDoubled: NoteKey`, `Chronicler.scoredDoubled: NoteKey`.
- `LineageScoring.bonus` is unchanged; Iron Hand's scoring keeps reading it.

- [ ] **Step 1: Write the failing tests**

In `LineageScoringSuite.scala`, after the test "after a gift the lines
name it and the points kept (R28)":

```scala
  test("a winner who pledged at Ruined Temple scores double the legacy " +
      "bonus, never the base, and gives from the doubled total"):
    // The Needle over the Empire's 3 Beast cards, doubled: 3 + 6.
    val at = atGiftsOn(empireWithCards.legacy(p1, TheNeedleCard)
      .pledgedAtRuinedTemple(p1))
    assertEquals(giftCap(at), 4)
    val kept = giftingOn(at, 0, 0)
    assertEquals(scoreOf(Outcomes.ready(kept.state), p1) -
      scoreOf(Outcomes.ready(at.state), p1), 9)
    assertEquals(scoreLines(kept.events), Vector("p1 scored 9 lineage " +
      "points (3 base, 6 from Beast, doubled by Ruined Temple)"))

  test("a pledged winner with no bonus scores the base alone"):
    val at = atGiftsOn(empireWithCards.pledgedAtRuinedTemple(p1))
    assertEquals(scoreLines(giftingOn(at, 0, 0).events),
      Vector("p1 scored 3 lineage points"))
```

In `ChroniclerSuite.scala`, after the test "Score Lineage Points then
scores only the base for the winner's legacies Chronicler scored":

```scala
  test("a winner who pledged at Ruined Temple scores double at Chronicler " +
      "(catalog batch 13, ruling 19)"):
    val run = through(chronicled.pledgedAtRuinedTemple(p1),
      TriggeredProcedureRef.ChronicleSun)
    assertEquals(scoreOf(run.ready, p1), 4)
    assertEquals(sunLines(run), Vector(
      s"p2 declared ${ChroniclerCard.id.value}",
      "p1 scored 4 lineage points (4 from Beast, doubled by Ruined Temple)"))

  test("a pledged winner's base is not doubled after Chronicler"):
    val run = through(chronicled.pledgedAtRuinedTemple(p1),
      TriggeredProcedureRef.ChronicleThrone)
    assertEquals(scoreOf(run.ready, p1), 4 + LineageScoring.basePoints)

  test("a pledged player who did not win scores no double"):
    val run = through(chronicled.legacy(p2, TheNeedleCard)
      .pledgedAtRuinedTemple(p2), TriggeredProcedureRef.ChronicleSun)
    assertEquals(scoreOf(run.ready, p2), 2)
```

`chronicled` gives p1 The Needle already; `TheNeedleCard` is one card, so
the last test moves it to p2. If `Table.legacy` refuses a card another
lineage holds, build that table from `empire.legacy(p2, ChroniclerCard)
.legacy(p2, TheNeedleCard)` instead, and expect the same 2.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.LineageScoringSuite distantlands.gameplay.chronicle.legacies.ChroniclerSuite"`
Expected: FAIL: the pledged winner scores 6 and 2, with the undoubled
lines.

- [ ] **Step 3: Double the scored bonus**

In `LineageScoring.scala`, after `bonus`:

```scala
  /** `player`'s legacy bonus as they score it, at Score Lineage Points or
    * at Chronicler: `bonus`, each share doubled for a winner who pledged
    * at Ruined Temple (catalog batch 13, ruling 19). Iron Hand's holder is
    * never the winner, so its scoring reads `bonus`. */
  def scoredBonus(ready: ReadyGame, catalog: ExecutableCatalog,
      player: PlayerId): Vector[(Suit, Int)] =
    val shares = bonus(ready, catalog, player)
    if RuinedTempleWin.doubles(ready, player) then
      shares.map((suit, points) => suit -> 2 * points)
    else shares
```

After `scoredWithBonus`, add:

```scala
  /** "p1 scored 9 lineage points (3 base, 6 from Beast, doubled by Ruined
    * Temple)". */
  val scoredDoubled: NoteKey = notes.key("scored-doubled", Vector(
    NotePart.Arg(0), NotePart.Text(" scored "), NotePart.Arg(1),
    NotePart.Text(" ("), NotePart.Arg(2), NotePart.Text(" base, "),
    NotePart.Arg(3),
    NotePart.Text(s", doubled by ${RuinedTempleWin.name})")))
```

In `steps`, replace `score(state, winner, bonus(state, catalog, winner))`
with `score(state, winner, scoredBonus(state, catalog, winner))`.

In `score`, replace the `earned` note:

```scala
    val earned = Note(noteId, _ => Some(
      if shares.isEmpty then
        scored.sourceless(NoteArg.Player(winner), points(total))
      else scoredWithBonus.sourceless(NoteArg.Player(winner), points(total),
        NoteArg.Number(basePoints), NoteArg.SuitShares(
          shares.map((suit, share) => share -> suit)))))
```

with:

```scala
    val withBonus =
      if RuinedTempleWin.doubles(state, winner) then scoredDoubled
      else scoredWithBonus
    val earned = Note(noteId, _ => Some(
      if shares.isEmpty then
        scored.sourceless(NoteArg.Player(winner), points(total))
      else withBonus.sourceless(NoteArg.Player(winner), points(total),
        NoteArg.Number(basePoints), NoteArg.SuitShares(
          shares.map((suit, share) => share -> suit)))))
```

In `Chronicler.scala`, after `scored`:

```scala
  /** "p1 scored 4 lineage points (4 from Beast, doubled by Ruined
    * Temple)", a pledged winner's share (catalog batch 13, ruling 19). */
  val scoredDoubled: NoteKey = notes.key("chronicler-scored-doubled", Vector(
    NotePart.Arg(0), NotePart.Text(" scored "), NotePart.Arg(1),
    NotePart.Text(" ("), NotePart.Arg(2),
    NotePart.Text(s", doubled by ${RuinedTempleWin.name})")))
```

In `scoring`, replace `LineageScoring.bonus(state, catalog, player)` with
`LineageScoring.scoredBonus(state, catalog, player)`, and in the line
replace `scored.sourceless(` with:

```scala
          (if RuinedTempleWin.doubles(state, player) then scoredDoubled
            else scored).sourceless(
```

Import `RuinedTempleWin` in `Chronicler.scala` beside its other
`distantlands.gameplay.chronicle` imports. Update both doc comments: add
to `LineageScoring`'s object comment "A winner who pledged at Ruined
Temple scores double the bonus (`scoredBonus`)." and to Chronicler's
"A winner who pledged at Ruined Temple scores double (catalog batch 13,
ruling 19)."

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.LineageScoringSuite distantlands.gameplay.chronicle.legacies.ChroniclerSuite distantlands.gameplay.chronicle.legacies.IronHandSuite"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle src/test/scala/distantlands/gameplay/chronicle
git commit -m "$(cat <<'EOF'
feat(chronicle): a pledged winner scores double the legacy points

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 7: Note keys, the full gate and the docs

**Files:**
- Modify: `src/test/resources/notes/declared-keys.txt`
- Modify: `docs/rules/ambiguities.md`, `docs/rules/rules-reference.md`,
  `docs/rules/implementation-traceability.md`, `docs/catalog/README.md`,
  `docs/ROADMAP.md`, `docs/superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md`

- [ ] **Step 1: Rewrite the note keys snapshot**

Run the snapshot write (see "Running things"), then
`git diff src/test/resources/notes/declared-keys.txt`.
Expected: added lines only, for `edifice.e10.ruined` (`used`), the final
winner's `ruined-temple-took-rule`, the Throne's `scored-doubled` and the
Sun's `chronicler-scored-doubled`. No line removed.

- [ ] **Step 2: Run every test, the architecture check and the link check**

Run: `./sbtw test` in the background, then
`python3 scripts/check-architecture.py`.
Expected: every test passes; the architecture check passes. A golden that
changes is a finding to read, not to rewrite: neither the Setup nor the
Explore goldens place E04 or E10, and the first-game generator still picks
E01 and E06, so no golden should change.

- [ ] **Step 3: Record the rulings**

In `docs/rules/ambiguities.md`, "Catalog batch 13 rulings":

1. Extend the intro's last clause, "items 11 to 13 are rulings 11 to 13.",
   to: "items 11 to 13 are rulings 11 to 13; items 14 to 19 are rulings 14
   to 19, with rulings 14, 18 and 19 widened by the owner on 2026-11-15;
   item 20 is an implementation reading."
2. Append:

```markdown
14. **Stone Baths.** In Sun step 2, a seated player whose pawn is at its site does not discard their dormant legacies: one whose goal they meet still activates, and one they miss stays dormant on their lineage. A kept legacy is not newly dormant, so Crown Chancellor and Offer Citizenship leave it dormant; the Chronicle records it (`keptDormant`), which amends [Legacies rulings](#legacies-rulings), item 2. The Baths write no line.
15. **Ruined Baths.** In Sun step 3, a seated Exile whose pawn is at its site keeps every active legacy, whether or not they meet its goal. Steadfast and the cap are unaffected.
16. **The Baths are read at the Sun,** while the edifice and the pawns are still on the map. A pawn off the map, as Quest to Distant Lands leaves one before its first Wake, is at no site.
17. **Ruined Temple's pledge.** At Setup, every player who places their pawn at its site may burn a secret, and when it is explored, the explorer may. Each who burns pledges for the game. A player with no secret is not asked. The burn takes a facedown secret when the player has one, since a spent secret is never worth more. Line: "Ruined Temple: p2 burned a secret."
18. **Ruined Temple at the win.** Once Royal Line and Conspirator have settled the final winner, and before the Sun, a pledged winner takes every bandit site: its bandits go to the bandit supply, and it takes one of the winner's warbands, Imperial for an Imperial winner, the bank topped up when short. Sun step 2's legacy goals then see those sites, and Crown Chancellor, Shape Empire and the rest treat them as the winner's. Line: "p2 took rule of 3 bandit sites with Ruined Temple".
19. **Ruined Temple's double.** A pledged winner's legacy bonus doubles wherever it is scored, at Score Lineage Points and at Chronicler, so a declaration cannot strip the double. The 3 base points are not doubled, and gifts are spent from the doubled total. Lines: "p1 scored 9 lineage points (3 base, 6 from Beast, doubled by Ruined Temple)", and at Chronicler "p1 scored 4 lineage points (4 from Beast, doubled by Ruined Temple)".
20. **The Temple's offer** goes to a placer who has a secret or has pledged, so it stands while its own burn resolves in the ordered pawn window (item 9).
```

3. In "Legacies rulings", item 2 ("**Newly dormant is dormant.**"),
   replace "no flag records it (design, ruling 2)." with:

```markdown
no flag records it (design, ruling 2), but for the legacies Stone Baths keeps dormant, which are not newly dormant ([Catalog batch 13 rulings](#catalog-batch-13-rulings), item 14).
```

- [ ] **Step 4: Update the other docs**

`docs/rules/rules-reference.md`, after the "Amber Doors and Lost Doors"
bullet:

```markdown
- **Stone Baths and Ruined Baths.** CHRONICLE (SUN): players with a pawn at Stone Baths do not discard their dormant legacies; Exiles at Ruined Baths keep their active legacies without meeting their goals. Digital: a legacy Stone Baths keeps is not newly dormant, so winning or joining the Empire does not reactivate it ([Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), items 14 to 16).
- **Ruined Temple.** SETUP / WHEN EXPLORED, the player who placed their pawn there may burn a secret; if they then win, they score double the legacy points and take rule of all bandit sites. Digital: the final winner takes the sites before the Sun, and Chronicler's scoring doubles too ([Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), items 17 to 20).
```

`docs/rules/implementation-traceability.md`:

- the Explore row: delete the sentence "Ruined Temple waits for the
  ROADMAP's **Cards that act on legacies**." and, in its evidence cell,
  after the Doors' entry, add "Ruined Temple's pledge
  (`gameplay/powers/setup/RuinedTempleRules.scala`; `RuinedTempleSuite`;
  items 17 and 20)".
- the Sun task row: add to its evidence cell:

```markdown
Stone Baths and Ruined Baths (`gameplay/chronicle/Baths.scala`; `BathsSuite`; [Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), items 14 to 16)
```
- the Throne task row: add "Ruined Temple's bandit sites and double
  (`gameplay/chronicle/RuinedTempleWin.scala`; `RuinedTempleWinSuite`,
  `LineageScoringSuite`, `ChroniclerSuite`; items 18 and 19)".

`docs/catalog/README.md`: replace "Cards that are not yet implemented live
in `src/main/scala/distantlands/catalog/holding/`; an implemented card
sits beside its power, except the 24 sites" with "Every card sits beside
its power, except the 24 sites". Replace "and that a card sits in
`catalog/holding/` exactly while `PowerImplementationStatus` reports none
of its powers implemented." with "and that a card sits in a
`catalog/holding` package exactly while `PowerImplementationStatus`
reports none of its powers implemented, which keeps an unimplemented card
apart if one is ever added."

`docs/ROADMAP.md`: delete the whole **Cards that act on legacies** item
(seven lines, from `- [ ] **Cards that act on legacies.**` to "landed in
its slice 2."). Leave no note in its place.

The spec: in its status paragraph, after "Slice 5 landed 2026-11-15.", add
"Slice 6 landed 2026-11-15; with it `catalog/holding/` is empty."

- [ ] **Step 5: Run the link check**

Run: `python3 scripts/check-markdown-links.py`
Expected: passes.

- [ ] **Step 6: Commit**

```bash
git add src/test/resources/notes/declared-keys.txt docs
git commit -m "$(cat <<'EOF'
docs(catalog): record the Baths and Ruined Temple and close batch 13

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

## Self-review

- Spec coverage: ruling 14 (Task 2, kept legacies Task 1), 15 and 16
  (Task 2), 17 (Task 4), 18 (Task 5), 19 (Task 6); "Lines" (Tasks 4 to
  6); "Testing" names `BathsSuite` and `RuinedTempleSuite`, here with the
  win in `RuinedTempleWinSuite`; `CardCatalogSuite` with holding empty
  (Task 2 deletes the last file); "Docs" (Task 7), including the ROADMAP
  item's deletion.
- The spec's approach had the win step at the start of
  `ChronicleWinnerProcedure` and the double inside `LineageScoring.bonus`;
  the owner's rulings of 2026-11-15 moved both, and the spec now says so.
- Types: `PledgeTemple`, `LegaciesKeptDormant`, `templePledges`,
  `keptDormant`, `BathsRules.exempt`, `PlacedPawnEdificeRule`,
  `RuinedTemple.pledgeDecisionId`, `RuinedTempleWin.doubles` and
  `LineageScoring.scoredBonus` are named the same in every task.
