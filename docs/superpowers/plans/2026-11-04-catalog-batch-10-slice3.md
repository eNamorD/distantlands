# Catalog Batch 10 Slice 3: Round Die Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the round die as a third die kind and implement Shattered Portal (E14 ruined) on it.

**Architecture:**
- `DiceKind.Round` and `enum RoundDieFace extends DieFace`, faces 1 to 6, each scoring its value. Every exhaustive match on `DiceKind` gains a `Round` arm: the walker's live kind check, the reroll kind, the placeholder dice, the production port adapter, the kind codec, `RerollChoice.word` and Jinx's line.
- One family check, `RollOutcome.oneFamily`, replaces the two private copies (`RollOperations.sameFamily` and the recorded-roll check in `WalkerRolls`), so a round roll is one family like an attack or defense roll.
- Jinx's text names only attack and defense dice. Its line becomes an `Option`, `None` for a round die, and both its offer and its use filter on it, so no reroll is offered after a round roll.
- `CampaignDicePort.rollRound` has a uniform random default in the trait, as `shuffle` does, so the nine test ports need no edit.
- Shattered Portal is a `PaidAction` beside Stone Portal: `ModifyDicePool` of 1, a `RollStep` of the round die, then a `BuildOps` that moves the pawn `score` sites along `MapState.hinterward`, wrapping, with `PawnMoves.relocate`. Its `refusal` disables it, with a hover reason, when the user's pawn is not at its site.
- **Deviation from the spec:** the spec lists `RolledFaces` among the places the new kind reaches. It is left unchanged: its `case _ => None` already maps a round face to nothing, so the generic "Rolled" detail line is skipped (`LogWords.dice` returns `None`) and the decision wire's roll outcome leaves it out. No decision follows a round roll, and the Portal's own line states the number. Adding round faces to `protocol.projection.DieFace` would also need glyphs in the client's `DieFaceChip`, which nothing would show. Task 1 updates the `RolledFaces` doc to say so.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read "Goal and scope", "Readings" (Shattered Portal), "Slice 3: Round die", "Testing" and "Out of scope". Card text: `~/projects/cards/content/card-data/oath/en-US/oath-newfoundations.yml` (`OATH-E14b-NF`).

This is the third of 14 slice plans, written against `0.2.0-alpha` at `c849c8c5`.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-3` on branch `batch-10-slice-3`, created from local `0.2.0-alpha`. Never work in the main checkout.
  - An agent worktree made by the Agent tool starts from `origin/main`, which is far behind. Reset it onto local `0.2.0-alpha` before the first edit.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree. Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports, unused values and non-exhaustive matches fail the build. Adding `DiceKind.Round` breaks every exhaustive match at once; Task 1 lists them all.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write` (`src/test/resources/notes/declared-keys.txt`) and `UNIMPLEMENTED_SNAPSHOT=write` (`src/test/resources/powers/unimplemented.txt`), then read the diff. Commit only the lines the task explains.
- **Log lines:** Shattered Portal's line is the spec's, "Rolled 4; {Blue} placed at {site}.", parallel with Stone Portal's "{Blue} placed at {site}." (the movement relics' wording).
- **Hover reasons:** show an unavailable option disabled with a reason (memory: "Prefer hover reasons").
- **Wire:** a new die kind key, `round`, and six face keys, `round-1` to `round-6`. Old journals need not decode.
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").

---

### Task 1: The round die

**Files:**
- Modify: `src/main/scala/oathdigital/model/DiceSpec.scala:3-4`
- Modify: `src/main/scala/oathdigital/model/ActionValues.scala:12-21` (add `RoundDieFace` after `DefenseDieFace`)
- Modify: `src/main/scala/oathdigital/model/DicePoolState.scala:43-52` (`RollOutcome.derive`, new `oneFamily`)
- Modify: `src/main/scala/oathdigital/model/DieFace.scala:7` (doc)
- Modify: `src/main/scala/oathdigital/gameplay/operations/RollOperations.scala:31, 59-60, 72-74`
- Modify: `src/main/scala/oathdigital/gameplay/walker/WalkerRolls.scala:3-4, 29-35, 50-52`
- Modify: `src/main/scala/oathdigital/gameplay/walker/WalkerRerolls.scala:3-4, 22-24`
- Modify: `src/main/scala/oathdigital/gameplay/walker/WalkerDice.scala:3-4, 40-43`
- Modify: `src/main/scala/oathdigital/application/GameRandomPorts.scala`
- Modify: `src/main/scala/oathdigital/application/RolledFaces.scala:16-19` (doc only)
- Modify: `src/main/scala/oathdigital/serialization/WalkerValueCodec.scala:57-65, 254-264`
- Modify: `src/main/scala/oathdigital/gameplay/actions/dice/RerollChoice.scala:120-122`
- Modify: `src/main/scala/oathdigital/gameplay/powers/dice/Jinx.scala:48-50, 69-85`
- Modify: `src/test/scala/oathdigital/gameplay/WalkerDiceFixture.scala` (every stub)
- Modify: `src/test/scala/oathdigital/gameplay/AutomaticRollSuite.scala:18-22`
- Modify: `src/test/scala/oathdigital/testkit/ActPowerHarness.scala:53` (add `roundDie` after `attackDice`)
- Test: `src/test/scala/oathdigital/gameplay/walker/WalkerRollsSuite.scala`
- Test: `src/test/scala/oathdigital/gameplay/walker/WalkerDiceContract.scala`
- Test: `src/test/scala/oathdigital/application/CampaignDicePortContract.scala`
- Test: `src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala`

**Interfaces:**
- Produces: `DiceKind.Round`.
- Produces: `enum RoundDieFace(val value: Int) extends DieFace` with cases `One` to `Six`; `RoundDieFace.score(faces: Vector[RoundDieFace]): Int`.
- Produces: `RollOutcome.oneFamily(faces: Vector[DieFace]): Boolean`.
- Produces: `CampaignDicePort.rollRound(count: Int): Vector[RoundDieFace]`, with a uniform random default.
- Produces: `WalkerDiceFixture.dice(attack, defense, round: Vector[RoundDieFace] = Vector.empty)` and `ActPowerHarness.roundDie(face: RoundDieFace): WalkerDice`.

- [ ] **Step 1: Write the failing roll-outcome tests**

In `WalkerRollsSuite.scala`, add beside the other pools:

```scala
  private val roundPool = PoolKey("shattered-portal")
  private val roundRoll = Roll(roundPool, DiceSpec(DiceKind.Round))
```

After the test "a defense roll scores shields and has no skulls":

```scala
  test("a round roll scores the number it shows and has no skulls"):
    val faces: Vector[DieFace] = Vector(RoundDieFace.Four)
    val state = withPools(roundPool -> 1)
    assertEquals(WalkerRolls.outcomeFor(roundRoll, state, faces),
      Right(RollOutcome(roundPool, 1, faces, skulls = 0, score = 4)))
    assertEquals(WalkerRolls.outcomeFor(roundRoll, state,
      Vector(DefenseDieFace.Blank)), Left(OathViolation.InvalidEventOrder(
        s"round roll for pool $roundPool received a non-round die face")))

  test("a recorded round roll is one family; round and defense faces mixed " +
      "are not"):
    val state = withPools(roundPool -> 2)
    val faces: Vector[DieFace] = Vector(RoundDieFace.Six, RoundDieFace.One)
    assertEquals(WalkerRolls.outcomeForRecorded(state, roundPool, faces),
      Right(RollOutcome(roundPool, 2, faces, skulls = 0, score = 7)))
    assertEquals(WalkerRolls.outcomeForRecorded(state, roundPool,
      Vector[DieFace](RoundDieFace.Six, DefenseDieFace.Blank)),
      Left(OathViolation.InvalidEventOrder(
        "recorded roll mixes faces of different dice")))
```

In the test "a recorded roll needs its pool, its count and one family of faces", change the expected message `"recorded roll mixes attack and defense faces"` to `"recorded roll mixes faces of different dice"`.

- [ ] **Step 2: Write the failing dice-source contract tests**

In `WalkerDiceContract.scala`, change the import to:

```scala
import oathdigital.model.{AttackDieFace, DefenseDieFace, DiceKind, DieFace,
  RoundDieFace}
```

In `WalkerDiceRollContract`, after the defense test inside `rollers.foreach`:

```scala
    test(s"${subject.name}: a pool of n round dice rolls n round faces"):
      subject.poolSizes.foreach { count =>
        val faces: Vector[DieFace] =
          accepted(subject.dice.roll(DiceKind.Round, count))
        assertEquals(faces.size, count, s"pool of $count")
        assertEquals(faces.collect { case face: RoundDieFace => face }, faces,
          s"pool of $count")
      }
```

In `CampaignWalkerDiceSuite`'s test "random campaign dice show every face of each die over many rolls", add:

```scala
    val round = accepted(production.roll(DiceKind.Round, 600)).toSet
    assertEquals(round, RoundDieFace.values.toSet[DieFace])
```

In `WalkerDiceFixtureSuite`, give the "exact faces" subject round faces too:

```scala
    DiceSubject("exact faces", WalkerDiceFixture.dice(
      attack = Vector(AttackDieFace.OneSword, AttackDieFace.HollowSword),
      defense = Vector(DefenseDieFace.Doubler, DefenseDieFace.Blank),
      round = Vector(RoundDieFace.Two, RoundDieFace.Five)),
      poolSizes = Vector(2)))
```

In `CampaignDicePortContract.scala`, change the import to `import oathdigital.model.{AttackDieFace, DefenseDieFace, RoundDieFace}`, and inside `rollers.foreach`, after the defense test:

```scala
    test(s"$name: a pool of n round dice rolls n faces"):
      Vector(0, 1, 2, 5).foreach { count =>
        assertEquals(port.rollRound(count).size, count, s"pool of $count")
      }
```

In `RandomCampaignDicePortSuite`'s "every face" test, add:

```scala
    assertEquals(CampaignDicePort.random.rollRound(600).toSet,
      RoundDieFace.values.toSet)
```

- [ ] **Step 3: Write the failing wire tests**

In `WalkerOperationWireSuite.scala`, after the test "a walker roll payload round-trips attack and defense faces":

```scala
  test("a walker roll payload round-trips round faces, and a round Roll " +
      "keeps its die"):
    val events = Vector[OathEvent](
      WalkerStepRecorded("1", RollPayload(PoolKey("shattered-portal"),
        Vector(RoundDieFace.One, RoundDieFace.Six), automatic = true),
        Vector.empty),
      recordedOperation(Roll(PoolKey("shattered-portal"),
        DiceSpec(DiceKind.Round))))
    val encoded = encodedStreamThatRoundTrips("walker-round", events)
    val written = ujson.read(encoded).arr
    assertEquals(written(0)("payload")("step")("faces"),
      ujson.Arr("round-1", "round-6"))
    assertEquals(written(1)("payload")("step")("ops")(0)("die").str, "round")
    assert(GameEventWire.decodeStream(encoded.replace("round-6", "round-7"))
      .isLeft)
```

- [ ] **Step 4: Run the tests to see them fail**

Run: `./sbtw "testOnly oathdigital.gameplay.walker.WalkerRollsSuite"`
Expected: compile errors, `RoundDieFace` and `DiceKind.Round` not found.

- [ ] **Step 5: Add the die kind and its faces**

`DiceSpec.scala`:

```scala
/** Family of a rolled die: the two Campaign dice, and the round die, a d6
  * numbered 1 to 6 (Shattered Portal rolls it). */
enum DiceKind { case Defense, Attack, Round }
```

`ActionValues.scala`, after `object DefenseDieFace`:

```scala
/** The round die's faces, a d6 numbered 1 to 6. Each scores its number. */
enum RoundDieFace(val value: Int) extends DieFace:
  case One extends RoundDieFace(1)
  case Two extends RoundDieFace(2)
  case Three extends RoundDieFace(3)
  case Four extends RoundDieFace(4)
  case Five extends RoundDieFace(5)
  case Six extends RoundDieFace(6)
object RoundDieFace:
  def score(faces: Vector[RoundDieFace]): Int = faces.map(_.value).sum
```

`DicePoolState.scala`, replace `RollOutcome.derive` and add `oneFamily`:

```scala
object RollOutcome:
  /** Skulls and score of `faces`, all of one family: attack faces score
    * swords and count skulls, round faces score their numbers, defense
    * faces score shields, a Doubler applying across every face. */
  def derive(faces: Vector[DieFace]): (Int, Int) =
    val attack = faces.collect { case face: AttackDieFace => face }
    val round = faces.collect { case face: RoundDieFace => face }
    if attack.nonEmpty then
      (AttackDieFace.skulls(attack), AttackDieFace.score(attack))
    else if round.nonEmpty then (0, RoundDieFace.score(round))
    else (0, DefenseDieFace.score(faces.collect {
      case face: DefenseDieFace => face
    }))

  /** Whether every face of `faces` is on one die: all attack, all defense or
    * all round. No faces is one family. */
  def oneFamily(faces: Vector[DieFace]): Boolean =
    faces.forall(_.isInstanceOf[AttackDieFace]) ||
      faces.forall(_.isInstanceOf[DefenseDieFace]) ||
      faces.forall(_.isInstanceOf[RoundDieFace])
```

`DieFace.scala` doc: change "([[DefenseDieFace]], [[AttackDieFace]], any later family)" to "([[DefenseDieFace]], [[AttackDieFace]], [[RoundDieFace]])".

- [ ] **Step 6: Use the one family check**

`RollOperations.scala`:
- Line 31: `rolled) => invalid("mixes faces of different dice")`, with the guard calling `RollOutcome.oneFamily(indices.map(outcome.faces) ++ rolled)` in place of `sameFamily(...)`.
- Lines 59-60: `else if !RollOutcome.oneFamily(all) then Left(InvalidDescription(s"setting dice of ${pool.value} mixes faces of different dice"))`.
- Delete the private `sameFamily`.

`WalkerRolls.scala`:
- Import `RoundDieFace` too.
- In `outcomeFor`, add an arm to the kind match:

```scala
        case DiceKind.Round => Either.cond(
          faces.forall(_.isInstanceOf[RoundDieFace]), (), invalid(
            s"round roll for pool $pool received a non-round die face"))
```

- In `outcomeForRecorded`, replace the two `isInstanceOf` checks:

```scala
    _ <- Either.cond(RollOutcome.oneFamily(faces), (), invalid(
      "recorded roll mixes faces of different dice"))
```

  and update its doc's "one family" sentence if it names only attack and defense.

`WalkerRerolls.scala`: import `RoundDieFace` too, and name every family:

```scala
        val kind = face match
          case _: AttackDieFace => DiceKind.Attack
          case _: RoundDieFace => DiceKind.Round
          case _ => DiceKind.Defense
```

(The last arm stays a wildcard: `DieFace` is open, and an unknown face fails later when the reroll applies.)

- [ ] **Step 7: Roll it from every dice source**

`WalkerDice.scala` placeholder: import `RoundDieFace`; add `case DiceKind.Round => RoundDieFace.One: DieFace`.

`GameRandomPorts.scala`:

```scala
trait CampaignDicePort:
  def rollAttack(count: Int): Vector[AttackDieFace]
  def rollDefense(count: Int): Vector[DefenseDieFace]
  /** `count` round dice, each uniform over 1 to 6. A test port may fix
    * them. */
  def rollRound(count: Int): Vector[RoundDieFace] =
    CampaignDicePort.uniformRound(count)
```

In `walkerDice`, add `case DiceKind.Round => port.rollRound(count)`. In `object CampaignDicePort`, beside `uniform`:

```scala
  private val roundRoller = new scala.util.Random()
  private def uniformRound(count: Int): Vector[RoundDieFace] =
    Vector.fill(count)(RoundDieFace.values(roundRoller.nextInt(6)))
```

`RolledFaces.scala` doc: after "maps to nothing rather than to a guess", add: "The round die is such a family: Shattered Portal's own line states its number, no decision follows its roll, and the client draws no round face."

- [ ] **Step 8: Name the kind on the wire and in words**

`WalkerValueCodec.scala`:

```scala
  protected final def encodeDiceKind(kind: DiceKind): String = kind match
    case DiceKind.Defense => "defense"
    case DiceKind.Attack => "attack"
    case DiceKind.Round => "round"
```

with `case "round" => Right(DiceKind.Round)` in `decodeDiceKind`. Face codec:

```scala
  protected final def encodeDieFace(face: DieFace): ujson.Value = face match
    case face: DefenseDieFace => ujson.Str(encodeDefenseFace(face))
    case face: AttackDieFace => ujson.Str(encodeAttackFace(face))
    case face: RoundDieFace => ujson.Str(roundFaceKey(face))
    case other => throw new IllegalArgumentException(
      s"unsupported walker die face $other")

  protected final def decodeDieFace(value: String, path: String)
      : Either[WireError, DieFace] = value match
    case "hollow-sword" | "one-sword" | "two-swords-skull" =>
      decodeAttackFace(value, path)
    case round if round.startsWith("round-") =>
      RoundDieFace.values.find(face => roundFaceKey(face) == round)
        .toRight(InvalidValue(path, s"unknown round die face '$round'"))
    case _ => decodeDefenseFace(value, path)

  /** A round face's journal key: `round-1` to `round-6`. */
  private def roundFaceKey(face: RoundDieFace): String = s"round-${face.value}"
```

`RerollChoice.word`: add `case DiceKind.Round => "round"`.

`Jinx.scala`: the line becomes optional, and the offer and the use read it:

```scala
  /** Its line for `rolled`; none for the round die, which Jinx's text does
    * not name, so Jinx offers nothing after a round roll. */
  private def line(rolled: DiceThrow): Option[NoteKey] = rolled.die match
    case DiceKind.Attack => Some(rerolledAttack)
    case DiceKind.Defense => Some(rerolledDefense)
    case DiceKind.Round => None

  /** The throw at hand, when its roller rules Jinx and Jinx can reroll it. */
  private def jinxable(ctx: PowerCtx): Option[DiceThrow] =
    RerollChoice.throwOf(ctx).filter(rolled => line(rolled).nonEmpty &&
      Jinx.rules(ctx.state, rolled.roller))
```

In `use`, replace `RerollChoice.throwOf(ctx).filter(rolled => Jinx.rules(ctx.state, rolled.roller))` with `jinxable(ctx)`. In `offer`, do the same, and write the note as `Note(id, _ => line(rolled).map(_(PowerSourceRef.Card(JinxCard.id))))`. Update the class doc: "It offers itself at every attack or defense roll step whose roller rules it".

- [ ] **Step 9: Give the test stubs the round die**

`WalkerDiceFixture.scala`: in `blanks`, `raid` and `shields`, add `case DiceKind.Round => RoundDieFace.One: DieFace` to the match. In `swordsCyclingDefense`, `swordsSingleThenDoubleShields` and `skullThenSwords`, add `case DiceKind.Round => Vector.fill(count)(RoundDieFace.One: DieFace)`. Each stub's doc gains "every round die a 1". Give `dice` a round parameter:

```scala
  /** Stub of WalkerDice: dice that return exactly these faces, and fail
    * loudly on a wrong count. */
  def dice(attack: Vector[AttackDieFace] = Vector.empty,
      defense: Vector[DefenseDieFace] = Vector.empty,
      round: Vector[RoundDieFace] = Vector.empty): WalkerDice =
    (kind, count) => kind match
      case DiceKind.Attack => Either.cond(attack.size == count, attack,
        OathViolation.InvalidEventOrder(
          s"test dice: ${attack.size} attack faces for a pool of $count"))
      case DiceKind.Defense => Either.cond(defense.size == count, defense,
        OathViolation.InvalidEventOrder(
          s"test dice: ${defense.size} defense faces for a pool of $count"))
      case DiceKind.Round => Either.cond(round.size == count, round,
        OathViolation.InvalidEventOrder(
          s"test dice: ${round.size} round faces for a pool of $count"))
```

`AutomaticRollSuite.scala` `fixed`: add `case DiceKind.Round => Right(Vector.fill(count)(RoundDieFace.One))` and "round always One" to its doc.

`ActPowerHarness.scala`, after `attackDice`:

```scala
  /** Stub of WalkerDice: one round die showing `face`; any other pool
    * fails. */
  def roundDie(face: RoundDieFace): WalkerDice =
    WalkerDiceFixture.dice(round = Vector(face))
```

- [ ] **Step 10: Run the tests to see them pass**

Run: `./sbtw "testOnly oathdigital.gameplay.walker.* oathdigital.application.*DicePort* oathdigital.serialization.WalkerOperationWireSuite oathdigital.gameplay.AutomaticRollSuite oathdigital.gameplay.powers.action.GamblingHallSuite"`
Expected: PASS. Gambling Hall's Jinx test shows Jinx still rerolls defense dice.

- [ ] **Step 11: Commit**

```bash
git add src/main/scala/oathdigital/model/DiceSpec.scala \
  src/main/scala/oathdigital/model/ActionValues.scala \
  src/main/scala/oathdigital/model/DicePoolState.scala \
  src/main/scala/oathdigital/model/DieFace.scala \
  src/main/scala/oathdigital/gameplay/operations/RollOperations.scala \
  src/main/scala/oathdigital/gameplay/walker/WalkerRolls.scala \
  src/main/scala/oathdigital/gameplay/walker/WalkerRerolls.scala \
  src/main/scala/oathdigital/gameplay/walker/WalkerDice.scala \
  src/main/scala/oathdigital/application/GameRandomPorts.scala \
  src/main/scala/oathdigital/application/RolledFaces.scala \
  src/main/scala/oathdigital/serialization/WalkerValueCodec.scala \
  src/main/scala/oathdigital/gameplay/actions/dice/RerollChoice.scala \
  src/main/scala/oathdigital/gameplay/powers/dice/Jinx.scala \
  src/test/scala/oathdigital/gameplay/WalkerDiceFixture.scala \
  src/test/scala/oathdigital/gameplay/AutomaticRollSuite.scala \
  src/test/scala/oathdigital/testkit/ActPowerHarness.scala \
  src/test/scala/oathdigital/gameplay/walker/WalkerRollsSuite.scala \
  src/test/scala/oathdigital/gameplay/walker/WalkerDiceContract.scala \
  src/test/scala/oathdigital/application/CampaignDicePortContract.scala \
  src/test/scala/oathdigital/serialization/WalkerOperationWireSuite.scala
git commit -m "feat(dice): add the round die, a d6 that scores its number

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

**Checkpoint:** a review subagent reads Task 1's diff against this task and the spec before Task 2 starts.

---

### Task 2: Shattered Portal

**Files:**
- Create: `src/main/scala/oathdigital/gameplay/powers/action/ShatteredPortal.scala`
- Modify: `src/main/scala/oathdigital/gameplay/powers/action/StonePortal.scala:25-34` (doc)
- Modify: `src/main/scala/oathdigital/gameplay/powers/action/OtherActionPowers.scala:19`
- Create: `src/test/scala/oathdigital/gameplay/powers/action/ShatteredPortalSuite.scala`
- Modify: `src/test/resources/notes/declared-keys.txt`, `src/test/resources/powers/unimplemented.txt` (snapshots)
- Modify: `docs/ROADMAP.md:69`, `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md:3`

**Interfaces:**
- Consumes: `DiceKind.Round`, `RoundDieFace`, `ActPowerHarness.roundDie`, `ActPowerHarness.rulesWithPrintedRestrictions` (Task 1).
- Produces: `case object ShatteredPortal extends PaidAction(StonePortalCard.ruined.power)` with `pool: PoolKey`, `NotHere: String`, `used: NoteKey` and `destination(ready: ReadyGame, from: SiteId, steps: Int): Option[SiteId]`.

- [ ] **Step 1: Write the failing suite**

Create `ShatteredPortalSuite.scala`:

```scala
package oathdigital.gameplay.powers.action

import oathdigital.gameplay.cards.{AncientCitySite, BrokenPeaksSite}
import oathdigital.gameplay.phases.PhaseMenu
import oathdigital.gameplay.powers.{NoteText, PhasePowerCatalog, PowerFixture}
import oathdigital.gameplay.powers.dice.JinxCard
import oathdigital.gameplay.setup.FirstGameSetupFixture.catalog
import oathdigital.gameplay.walker.WalkerPowers
import oathdigital.model._
import oathdigital.testkit.{Look, MenuPlay, Table}
import oathdigital.testkit.ActPowerHarness
import oathdigital.testkit.Outcomes.{assertWireRoundTrips, ready}
import oathdigital.testkit.Unwrap.accepted

/** Shattered Portal (catalog batch 10, slice 3): with the pawn at its site,
  * roll the round die and place the pawn that many sites Hinterward,
  * wrapping from the bottom Hinterland site to the top Cradle site. */
class ShatteredPortalSuite extends munit.FunSuite:
  import ActPowerHarness.{replayed, roundDie, rules,
    rulesWithPrintedRestrictions, use}
  import PowerFixture._

  private val portal = StonePortalCard.id
  private val source = DecisionOptionRef.Edifice(portal)

  /** The map's sites from the Cradle out. */
  private val hinterward: Vector[SiteId] =
    Table.from(quietTable).ready.game.current.map.hinterward

  /** p1's Act with 1 faceup secret, the ruined Portal at `portalAt` and p1's
    * pawn at `pawnAt`. */
  private def portalTable(portalAt: SiteId, pawnAt: SiteId): Table =
    Table.from(quietTable).turn(actor, Phase.Act).pawn(actor, pawnAt)
      .edifice(portal, EdificeSide.Ruined, at = portalAt).secrets(actor, 1)

  /** The pawn's site after using the Portal from `hinterward(index)` with
    * the round die showing `face`. */
  private def landingFrom(index: Int, face: RoundDieFace): SiteId =
    val site = hinterward(index)
    val done = accepted(use(rules(roundDie(face)),
      portalTable(site, site).ready, ShatteredPortal.id, source))
    Look(ready(done.state)).pawn(actor)

  test("the map has 8 sites, so a roll of 1 to 6 never lands where it " +
      "started"):
    assertEquals(hinterward.size, 8)

  test("it pays a secret, rolls 4 and places the pawn 4 sites Hinterward, " +
      "with the line, and the journal replays and round trips"):
    val home = hinterward(1)
    val start = portalTable(home, home).ready
    val rules0 = rules(roundDie(RoundDieFace.Four))
    val done = accepted(use(rules0, start, ShatteredPortal.id, source))
    val after = ready(done.state)
    assertEquals(Look(after).pawn(actor), hinterward(5))
    assertEquals(Look(after).tokensOn(portal), Tokens(0, 1))
    assertEquals(after.game.current.walkerPending, None)
    assertEquals(NoteText.said(ShatteredPortal, done.events), Vector(
      NoteText.Said(NoteKey.Used,
        s"Rolled 4; ${actor.value} placed at ${hinterward(5).value}.",
        covers = false)))
    assertEquals(replayed(rules0, start, done.events), after)
    assertWireRoundTrips(done.events)

  test("from the bottom Hinterland site a 1 wraps to the top Cradle site"):
    val top = landingFrom(7, RoundDieFace.One)
    assertEquals(top, hinterward(0))
    assertEquals(Table.from(quietTable).ready.game.current.map.regionOf(top),
      Some(Region.Cradle))

  test("every start and roll lands that many sites on, wrapping"):
    for
      index <- hinterward.indices
      face <- RoundDieFace.values
    do assertEquals(landingFrom(index, face),
      hinterward((index + face.value) % hinterward.size),
      s"from ${hinterward(index).value} rolling ${face.value}")

  test("with the pawn elsewhere at a site the user rules, it is shown " +
      "unavailable with the reason"):
    val home = AncientCitySite.id
    val ruledFromAfar = portalTable(home, BrokenPeaksSite.id)
      .warbandsAt(home, actor, 1).ready
    val option = PhaseMenu.offers(PhaseMenu.Menu.Act, catalog, ruledFromAfar,
      actor, PhasePowerCatalog.default, WalkerPowers.empty)
      .map(offer => MenuPlay.withoutBand(offer.option))
      .find(_.ref == source)
    assertEquals(option.collect {
      case DecisionOption.Unavailable(_, reason) => reason },
      Some(OptionReason.Written(ShatteredPortal.NotHere)))

  test("Jinx offers no reroll of the round die"):
    val home = hinterward(2)
    val start = portalTable(home, home).adviser(actor, JinxCard.id)
      .secrets(actor, 2).ready
    val done = accepted(use(rulesWithPrintedRestrictions(
      roundDie(RoundDieFace.Three)), start, ShatteredPortal.id, source))
    val after = ready(done.state)
    assertEquals(after.game.current.walkerPending, None)
    assertEquals(Look(after).pawn(actor), hinterward(5))
```

`landingFrom` checks 48 cases. If a `Table.edifice` placement at some site is refused (a full site), read `Table.edifice` and stage the Portal the way it allows; do not drop the case.

- [ ] **Step 2: Run the suite to see it fail**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.ShatteredPortalSuite"`
Expected: compile error, `ShatteredPortal` not found.

- [ ] **Step 3: Write the power**

Create `ShatteredPortal.scala`:

```scala
package oathdigital.gameplay.powers.action

import oathdigital.catalog.ExecutableCatalog
import oathdigital.gameplay.PowerAccess
import oathdigital.gameplay.actions.dice.RollStep
import oathdigital.gameplay.powerresolver.QueuedStarts
import oathdigital.gameplay.powers.setup.EdificeSetupSupport
import oathdigital.model._

/** Shattered Portal (E14, ruined), ACTION: with the user's pawn at its site,
  * place 1 secret on it, roll the round die and place the pawn that many
  * sites Hinterward, the bottom Hinterland site wrapping to the top Cradle
  * site (catalog batch 10, slice 3).
  *
  * The roll is a `RollStep` of one round die; Jinx names only attack and
  * defense dice, so no reroll is offered. The placement is a plain `Move`,
  * as Stone Portal's is. The map holds 8 sites, so a roll of 1 to 6 never
  * lands on the starting site. With the pawn elsewhere (the user rules the
  * site), the option shows unavailable with [[NotHere]]. Its line: "Rolled
  * 4; {Blue} placed at {site}."
  */
case object ShatteredPortal extends PaidAction(StonePortalCard.ruined.power):
  val NotHere: String = "Your pawn is not at this site."
  val pool: PoolKey = PoolKey("shattered-portal")

  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "Rolled {4}; {Blue} placed at {site}." */
  val used: NoteKey = notes.key(NoteKey.Used, Vector(
    NotePart.Text("Rolled "), NotePart.Arg(0), NotePart.Text("; "),
    NotePart.Arg(1), NotePart.Text(" placed at "), NotePart.Arg(2),
    NotePart.Text(".")))

  private def portalSite(ready: ReadyGame): Option[SiteId] =
    EdificeSetupSupport.siteOf(ready, StonePortalCard.id, EdificeSide.Ruined)

  private def pawnHere(ready: ReadyGame, player: PlayerId): Boolean =
    portalSite(ready).exists(PowerAccess.pawnSite(ready, player).contains)

  /** The site `steps` sites Hinterward of `from`, wrapping from the last
    * site in play to the first. */
  def destination(ready: ReadyGame, from: SiteId, steps: Int)
      : Option[SiteId] =
    val order = ready.game.current.map.hinterward
    Option(order.indexOf(from)).filter(_ >= 0)
      .map(index => order((index + steps) % order.size))

  override def refusal(catalog: ExecutableCatalog, ready: ReadyGame,
      player: PlayerId, source: DecisionOptionRef, starts: QueuedStarts)
      : Option[String] = Option.when(!pawnHere(ready, player))(NotHere)

  def build(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      source: DecisionOptionRef): Either[OathViolation, Operation] =
    Right(Sequence(Vector[Operation](
      ModifyDicePool(pool, 1),
      RollStep(pool, DiceSpec(DiceKind.Round), Some(player)),
      BuildOps((live, _) => moved(live, player)),
      Note(id, states => line(states, player)))))

  private def moved(ready: ReadyGame, player: PlayerId)
      : Either[OathViolation, Vector[CoreOperation]] = for
    from <- PawnMoves.pawnSite(ready, player)
    to <- destination(ready, from, RollResults.score(ready, pool))
      .toRight(OathViolation.InvalidEventOrder(
        s"${from.value} is not on the map"))
    ops <- PawnMoves.relocate(ready, player, to)
  yield ops

  private def line(states: NoteStates, player: PlayerId): Option[LogNote] =
    for site <- PowerAccess.pawnSite(states.now, player)
    yield used(PowerSourceRef.Card(StonePortalCard.id),
      NoteArg.Number(RollResults.score(states.now, pool)),
      NoteArg.Player(player), NoteArg.Site(site))
```

`PaidAction` needs only the printed power; the engine's access rule already reaches an edifice at the pawn's site or at a site the user rules, as it does for Stone Portal and Great Clock.

- [ ] **Step 4: Register it and update Stone Portal's doc**

In `OtherActionPowers.scala`, add `ShatteredPortal` to the list beside `GreatClock`. In `StonePortal.scala`, replace the doc's last sentence ("The ruined face, Shattered Portal, needs the round die and is not implemented.") with "The ruined face is [[ShatteredPortal]]."

- [ ] **Step 5: Run the suite to see it pass**

Run: `./sbtw "testOnly oathdigital.gameplay.powers.action.ShatteredPortalSuite oathdigital.gameplay.powers.action.StonePortalSuite"`
Expected: PASS. Stone Portal's test "the ruined face offers nothing" still passes: it checks only `StonePortal.id`.

- [ ] **Step 6: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly oathdigital.gameplay.powers.*"`
Then `git diff src/test/resources`. Expected:
- `declared-keys.txt` gains `edifice.e14.ruined used = Rolled {0}; {1} placed at {2}.`
- `unimplemented.txt` drops `edifice.e14.ruined`.

Nothing else changes.

- [ ] **Step 7: Update the docs**

- `docs/ROADMAP.md`: delete the line `- **Round die:** Shattered Portal.` (memory: "Roadmap deletes done work"). If the seam paragraph below the list describes the round die, delete that paragraph too.
- Spec status line: `**Status:** designed 2026-11-03. Slices 1 to 3 landed.`

- [ ] **Step 8: Run the gates**

Run: `./sbtw clean coverage test coverageReport` (background; about 10 minutes)
Expected: all tests pass, statement coverage at least 92%.

Run: `python3 scripts/check-architecture.py && python3 scripts/check-markdown-links.py`
Expected: both pass.

- [ ] **Step 9: Commit**

```bash
git add src/main/scala/oathdigital/gameplay/powers/action/ShatteredPortal.scala \
  src/main/scala/oathdigital/gameplay/powers/action/StonePortal.scala \
  src/main/scala/oathdigital/gameplay/powers/action/OtherActionPowers.scala \
  src/test/scala/oathdigital/gameplay/powers/action/ShatteredPortalSuite.scala \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt \
  docs/ROADMAP.md docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md
git commit -m "feat(powers): Shattered Portal rolls the round die and moves the pawn Hinterward

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

**Checkpoint:** a review subagent reads the whole slice diff against this plan and the spec before the merge.
