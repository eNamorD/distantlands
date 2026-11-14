# Legacy In-Play Powers, Slice 3: Triggers and Role Implementation Plan

> Executed inline by the implementer.

**Goal:** Three legacy powers act on events of a game. The Needle's holder
may flip a banner they have just taken. Arbiter's holder modifies the
Stable Regime end die before it is rolled. Iron Hand goes to the legacy
deck the moment its holder becomes Imperial.

**Architecture:** A new core operation, `FlipBanner`, turns a banner to
its other face beside `BoardControlOperations.moveBanner`; flipping the
Darkest Secret away from Festival takes the Festival marker off. The
Needle is an `ExecutedTrigger`: after a walker batch moves a banner into
its holder's play area, it asks them "Keep this side" or "Flip it". The
end die is rolled outside the walker (`StateBasedEvaluation.endRound`,
called by `TurnBoundary.finishRound` from the driver), so Arbiter's step
is a triggered walker procedure the driver starts at the round's end,
before the roll, as it starts Quest to Distant Lands' first Wake. Its
answer is journaled as a new operation, `SetEndDieModifier`, onto a new
`CurrentGameState.endDieModifier`; `endRound` reads it into the
`EndDieRolled` it writes, which gains a `modifier` field, and the event's
replay check requires the two to agree and compares `face + modifier`
with the printed thresholds. A saved `EndDieRolled` without the field
decodes with modifier 0 and replays as before. Iron Hand's discard is one
more operation in `RoleChanges.becomeImperial`, right after
`RoleChanged`, and its line a `Note` in `Citizenship.become`, which every
in-game road to Citizenship takes.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy in-play powers design](../../specs/2026-11/2026-11-14-legacy-in-play-powers-design.md),
rulings 1-2 and 20-22 and section "Slice 3: triggers and role".
[Implementation rulings](../../specs/2026-11/2026-11-14-legacy-in-play-powers-implementation-rulings.md).
Card text in `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
(The Needle L05, Arbiter L23) and
`src/main/scala/distantlands/gameplay/chronicle/legacies/IronHand.scala`
(L01). The rules: Empire endings (`docs/rules/rules-reference.md#empire-endings`),
the banners (`#core-state-and-terminology`), Role changes (`#role-changes`);
the [Legacy Chronicle powers rulings](../../../rules/ambiguities.md#legacy-chronicle-powers-rulings)
item on Iron Hand's discard (Chronicle powers ruling 18).

## Prerequisite

Slice 2 has landed (through `5eab649b2`) and its review fixes are in.
Check before Task 1:

```bash
grep -q "object TheNeedleCard" src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala &&
grep -q "object ArbiterCard" src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala &&
grep -q "case object KeeperOfOrder" src/main/scala/distantlands/gameplay/powers/campaign/KeeperOfOrder.scala &&
echo ready
```

Expected: `ready`.

## Open questions

Raised for the owner's rulings before execution. Each names the plan's
default, which the tasks below are written to.

- **Q1. Where does Arbiter's choice wait for the roll?** The step is a
  walk; the roll comes after the walk ends, in the driver. (a) A new
  journaled operation, `SetEndDieModifier(amount)`, writes it to a new
  `CurrentGameState.endDieModifier: Int = 0`, which `endRound` reads and
  `EndDieRolled`'s replay clears. (b) A marker dice pool
  (`ModifyDicePool(PoolKey("end-die.arbiter"), n)`), with no new
  operation kind or field, but read from walker scratch the walk's end
  may clear. *Default: (a)*: a fact of the game's state, journaled and
  replayed like every other, which the end die's replay can check.
  Tasks 3 and 4.
- **Q2. The lines the spec does not word.** *Default:* The Needle writes
  "p2 flipped the People's Favor with The Needle" (the new face shows on
  the banner, so the line does not name it); keeping the side writes
  nothing. The end die's line becomes "The end die rolled 4, modified by
  +2 to 6. Stable Regime needed 6." when modified, and is unchanged
  otherwise. Arbiter's Decline writes nothing. Iron Hand's line names the
  role reached: "p3 discarded Iron Hand on becoming a Citizen", or "... on
  becoming the Chancellor" in a game with no Empire. Tasks 2, 3, 4 and 5.
- **Q3. Option order.** *Default:* the plainest answer first, as the
  Chronicle powers ask (Chronicle powers R6): The Needle "Keep this side",
  then "Flip it"; Arbiter "Decline", then −1, +1, +2, +3. Every existing
  test and golden that answers by default keeps its result. Tasks 2 and 4.
- **Q4. Several banners in one batch.** A raid or a deal may hand The
  Needle's holder both banners at once. *Default:* one question per
  banner, People's Favor first, each with its own id
  (`power.the-needle.<banner key>`). Task 2.

## Plan choices

- `EndDieRolled` encodes `"modifier"` only when it is not 0, and decodes
  an absent one as 0, so every saved roll and every event snapshot reads
  and writes exactly as before.
- The Arbiter step is asked whenever the end die will be rolled
  (`EmpireEndings.rollsEndDie`) and a seated player holds Arbiter active,
  whatever the modifier could change: its shape never depends on the die.
- Iron Hand's discard is of an active Iron Hand only (ruling 1), and
  writes the same `Move` the Chronicle's discard writes (Chronicle powers
  ruling 18).
- The Needle watches `Move`s of `Piece.Banner` into its holder's play area
  from anywhere else, found in the batch's executed operations through
  `Operation.flatten`, so a `Take`'s inner move counts. Its holder answers,
  on or off turn; a later trigger's question reads its own answer with
  `PowerAnswers.last`.
- The Arbiter card and step live in `gameplay/phases/rest` beside
  `TurnBoundary`; The Needle in `gameplay/powers/banner` beside the banner
  powers. Each registers a `ContributingPower` in `WalkerPowerCatalog`
  (Arbiter's hooks no window, as The Ear's Chronicle half), so the cards
  leave holding.

## Global Constraints

- Scala 3 with `-Werror`; every Scala file at or under 800 lines;
  `model` imports nothing from `gameplay`; `gameplay/actions` never imports
  `gameplay/powers`.
- A new test names cards by their card objects, and builds its table as a
  named fixture in the test.
- Only seated players' active legacies act (ruling 1).
- No `Branch`'s children or ids depend on state the walk itself changes;
  every new `Decide` has a resume test; no two decisions share an id in
  one walk.
- Old saved streams still decode and replay: `FlipBanner` and
  `SetEndDieModifier` are new wire kinds; `EndDieRolled` without
  `modifier` decodes as 0; `TriggeredProcedureRef.Arbiter` is a new key.
- TDD, one commit per task; coverage at or above 92% statements and
  84.4% branches; a rewritten snapshot or golden is reviewed line by line
  and committed in its task; regenerate the power snapshots with
  `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write`.
- A card that leaves holding updates the test imports that name it
  (`ResolveLegaciesSuite`, `OfferCitizenshipSuite`, `IronHandSuite`,
  `LineageScoringSuite`, `ChroniclerSuite`, `ChronicleLogScripts`, and the
  `ArbiterCard` users: `LegacyIdentitySuite`, `RuleSourceIndexSuite`,
  `RestSuite`, `GameStartRulesSuite`, `LegacyDealSuite`,
  `ChronicleRecordSuite`, `CardIndexSuite`, `ChronicleJsonCodecSuite`,
  `ChronicleTtsCodecSuite`, `LegacyCardDataSuite`).
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; explicit
  pathspecs; `git log -3` after each commit.

## The gate

```bash
./sbtw clean coverage test coverageReport
./sbtw "frontend/test"
python3 scripts/check-architecture.py
python3 scripts/check-markdown-links.py
git status --short src/test/resources
```

Expected: every suite passes; coverage at or above 92% statements and
84.4% branches; both scripts pass; the last command prints nothing.

---

### Task 1: `FlipBanner`, the banner flip

**Files:**
- Modify: `src/main/scala/distantlands/model/TurnOperation.scala`
  (`FlipBanner`)
- Modify: `src/main/scala/distantlands/gameplay/operations/BoardControlOperations.scala`
  (`flipBanner`)
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`
- Modify: `src/main/scala/distantlands/serialization/TurnOperationCodec.scala`
  (`"flip-banner"`)
- Test: `src/test/scala/distantlands/gameplay/operations/FlipBannerSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
  (`everyOperation`, `familyOf`)

**Interfaces:**
- Produces: `final case class FlipBanner(banner: Banner) extends
  TurnOperation, LeafOperation`;
  `BoardControlOperations.flipBanner(ready: ReadyGame, banner: Banner):
  ReadyGame`.

- [ ] **Step 1: Write the failing test**

```scala
package distantlands.gameplay.operations

import distantlands.model._
import distantlands.testkit.{Look, Table}
import distantlands.testkit.Table.p2

/** A banner turned to its other face (Legacy in-play powers ruling 20). */
class FlipBannerSuite extends munit.FunSuite:
  private val executor = new OperationExecutor()
  private def flipped(table: Table, banner: Banner): ReadyGame =
    executor.execute(table.ready, FlipBanner(banner))
      .getOrElse(fail("the flip must apply"))

  /** p2 holds the People's Favor on the Mob with 2 favor, and the Darkest
    * Secret on the Wandering Flame with 1 secret. */
  private val held: Table = Table.start
    .peoplesFavor(Some(p2), 2, face = PeoplesFavorFace.Mob)
    .darkestSecret(Some(p2), 1, face = DarkestSecretFace.WanderingFlame)

  test("the People's Favor turns between the Mob and the Grand Council, " +
      "its holder and favor kept"):
    val once = flipped(held, Banner.PeoplesFavor)
    assertEquals(once.game.current.banners.peoplesFavor,
      PeoplesFavorState(PeoplesFavorFace.GrandCouncil, Some(p2), 2))
    assertEquals(flipped(Table.from(once), Banner.PeoplesFavor)
      .game.current.banners.peoplesFavor.active, PeoplesFavorFace.Mob)

  test("flipping to the Festival places no marker"):
    val festival = flipped(held, Banner.DarkestSecret)
    assertEquals(festival.game.current.banners.darkestSecret,
      DarkestSecretState(DarkestSecretFace.Festival, Some(p2), 1, None))

  test("flipping away from the Festival takes its marker off"):
    val site = Look(held.ready).pawn(p2)
    val marked = held.darkestSecret(Some(p2), 1,
      face = DarkestSecretFace.Festival).festival(site)
    assertEquals(flipped(marked, Banner.DarkestSecret).game.current
      .banners.darkestSecret, DarkestSecretState(
        DarkestSecretFace.WanderingFlame, Some(p2), 1, None))
```

Read `Table.peoplesFavor`, `Table.darkestSecret` and `Table.festival` for
their face parameters first; use their names. In
`WalkerOperationWireSuite.everyOperation` add
`FlipBanner(Banner.PeoplesFavor)` and `FlipBanner(Banner.DarkestSecret)`
after the festival markers.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.operations.FlipBannerSuite distantlands.serialization.WalkerOperationWireSuite"`
Expected: compile error, `FlipBanner` is not found.

- [ ] **Step 3: Implement**

`TurnOperation.scala`, after `SetFestivalMarker`:

```scala
/** Turns `banner` to its other face (Legacy in-play powers ruling 20):
  * the People's Favor between the Mob and the Grand Council, the Darkest
  * Secret between the Wandering Flame and the Festival. Its holder and
  * tokens stay. Flipping to the Festival places no marker; flipping away
  * from it takes the marker off. */
final case class FlipBanner(banner: Banner) extends TurnOperation,
    LeafOperation
```

`BoardControlOperations.flipBanner`:

```scala
  /** `banner` on its other face; the Festival marker comes off with the
    * Festival face (Legacy in-play powers ruling 20). */
  def flipBanner(ready: ReadyGame, banner: Banner): ReadyGame =
    ready.updateCurrent(current => current.copy(banners = banner match
      case Banner.PeoplesFavor =>
        val held = current.banners.peoplesFavor
        current.banners.copy(peoplesFavor = held.copy(active =
          held.active match
            case PeoplesFavorFace.Mob => PeoplesFavorFace.GrandCouncil
            case PeoplesFavorFace.GrandCouncil => PeoplesFavorFace.Mob))
      case Banner.DarkestSecret =>
        val held = current.banners.darkestSecret
        current.banners.copy(darkestSecret = held.active match
          case DarkestSecretFace.WanderingFlame =>
            held.copy(active = DarkestSecretFace.Festival)
          case DarkestSecretFace.Festival =>
            held.copy(active = DarkestSecretFace.WanderingFlame,
              festival = None))))
```

`LeafApplication`: `case FlipBanner(banner) =>
Right(BoardControlOperations.flipBanner(ready, banner))`.
`TurnOperationCodec`: encode `ujson.Obj("kind" -> "flip-banner",
"bannerKey" -> banner.key)`; decode `"flip-banner"` with
`Banner.fromKey(value("bannerKey").str)` to `FlipBanner`, an unknown key
an `InvalidValue`. The wire suite's `familyOf` puts it in the turn
family, where `SetFestivalMarker` is.

- [ ] **Step 4: Run it and see it pass**

Run: `./sbtw "testOnly distantlands.gameplay.operations.* distantlands.serialization.*"`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add src/test/scala/distantlands/gameplay/operations/FlipBannerSuite.scala
git commit -m "feat(operations): FlipBanner turns a banner to its other face

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/model/TurnOperation.scala \
  src/main/scala/distantlands/gameplay/operations/BoardControlOperations.scala \
  src/main/scala/distantlands/gameplay/operations/LeafApplication.scala \
  src/main/scala/distantlands/serialization/TurnOperationCodec.scala \
  src/test/scala/distantlands/gameplay/operations/FlipBannerSuite.scala \
  src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala
git log -3
```

### Task 2: The Needle

Written to Q2, Q3 and Q4.

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/banner/TheNeedle.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `TheNeedleCard`), and the test imports of `TheNeedleCard`
- Test: `src/test/scala/distantlands/gameplay/powers/banner/TheNeedleSuite.scala`

**Interfaces:**
- Produces: `TheNeedleCard` (L05); `case object TheNeedle extends
  ExecutedTrigger` with `decisionId(banner: Banner): String`
  (`power.the-needle.<banner key>`), `keep` and `flip` buttons, and the
  `flipped: NoteKey` line.

- [ ] **Step 1: Write the failing test**

Read `ChallengeSuite` (or the suite that takes a banner by challenge) and
`RaidSuite`/`CampaignFixture` for how each takes a banner, then write,
each table a named fixture with its sites and pieces:

```scala
/** The Needle (L05): after its holder takes a banner, they may flip it
  * (Legacy in-play powers ruling 20). */
class TheNeedleSuite extends munit.FunSuite:
  /** p1 holds The Needle active and challenges p2, who holds the People's
    * Favor on the Mob with 2 favor; the challenge wins (`ChallengeFixture`'s
    * winning dice). */
  private def challengeForPeoplesFavor: Table = ...

  /** p1 holds The Needle active and raids p2 at Ancient City, taking the
    * Darkest Secret on the Festival, its marker at Broken Peaks. */
  private def raidForDarkestSecret: Table = ...

  test("after a challenge takes the People's Favor, its holder is asked, " +
      "and flipping turns it to the Grand Council"):
    // awaits TheNeedle.decisionId(Banner.PeoplesFavor), owner p1, options
    // Vector(keep, flip) labelled "Keep this side", "Flip it"; flip, then
    // the banner is p1's on the Grand Council with its 2 favor; one line
    // "p1 flipped the People's Favor with L05".

  test("keeping the side changes nothing and writes no line"):

  test("after a raid takes the Darkest Secret from the Festival, flipping " +
      "takes the Festival marker off"):

  test("taking a banner on the Wandering Flame and flipping it places no " +
      "Festival marker"):

  test("a banner moving to someone else, or to a dormant Needle's holder, " +
      "asks nothing (ruling 1)"):

  test("a banner placed at game start is not taken: Setup asks nothing"):
    // LaterGameFixture's start with p1 holding The Needle and the
    // People's Favor dealt at start: Setup's walk parks on no Needle
    // question.

  test("a walk resumed at The Needle's question flips once"):
    // Replay the events to the park, answer flip on the replayed state.
```

Each `...` is the table the comment states, built in the test.

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.banner.TheNeedleSuite"`
Expected: compile error, `TheNeedleCard` is not in `gameplay.powers.banner`.

- [ ] **Step 3: Implement**

`TheNeedle.scala` (card object as printed, moved from holding):

```scala
/** The Needle (legacy L05): "After you take a banner in any way, you may
  * flip it to its other side" (Legacy in-play powers design, ruling 20).
  *
  * An `ExecutedTrigger`: after a walker batch moves a banner into its
  * holder's play area from anywhere else (a challenge, a raid, Favored
  * Son, Wizard's Conclave, the Conspiracy, The Standard Bearer at Setup),
  * its holder is asked, on or off turn, "Keep this side" or "Flip it",
  * once per banner, the People's Favor first (Q3, Q4). Flipping is a
  * `FlipBanner`: its tokens stay, flipping to the Festival places no
  * marker and flipping away from it takes the marker off. A banner placed
  * at game start is not taken: game start is no walk.
  *
  * Line (Q2): "p2 flipped the People's Favor with The Needle"; keeping
  * the side writes nothing.
  */
case object TheNeedle extends ExecutedTrigger:
  val id: PowerId = TheNeedleCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  def decisionId(banner: Banner): String = s"power.the-needle.${banner.key}"
  val keep: DecisionOptionRef.Button = DecisionOptionRef.Button("keep")
  val flip: DecisionOptionRef.Button = DecisionOptionRef.Button("flip")

  private val notes = NoteSet(id)
  /** "{p2} flipped the {People's Favor} with {The Needle}". */
  val flipped: NoteKey = notes.key("flipped", Vector(NotePart.Arg(0),
    NotePart.Text(" flipped the "), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  override def noteKeys: Vector[NoteKey] = notes.declared
  override def narratedDecisions: Set[String] =
    Banner.all.map(decisionId).toSet

  /** The banners `batch` moved into `holder`'s play area from elsewhere,
    * in banner order. */
  private def taken(batch: ExecutedBatch, holder: PlayerId): Vector[Banner] =
    val moved = batch.executed.flatMap(Operation.flatten).collect {
      case Move(Piece.Banner(banner), PositionedLocation(from, _, _),
          PositionedLocation(Location.PlayArea(to), _, _), _, _)
          if to == holder && from != Location.PlayArea(holder) => banner
    }.toSet
    Banner.all.filter(moved)

  def onExecuted(batch: ExecutedBatch): Option[Operation] =
    for
      holder <- ChronicleLegacyRules.holder(batch.now, TheNeedleCard.id)
      banners = taken(batch, holder)
      if banners.nonEmpty
    yield Sequence(banners.flatMap(banner => Vector[Operation](
      Decide(decisionId(banner), holder, DecisionQuery.ChooseOne(Vector(
        DecisionOption.Button(keep, "Keep this side"),
        DecisionOption.Button(flip, "Flip it")),
        heading = Some(s"The Needle: flip the ${BannerRules.displayName(
          banner)}?"))),
      BuildOps((_, pending) => Right(
        PowerAnswers.last(pending.answered, decisionId(banner))
          .filter(_ == flip).map(_ => FlipBanner(banner): CoreOperation)
          .toVector), required = true),
      Note(id, states => Option.when(PowerAnswers.last(states.answered,
        decisionId(banner)).contains(flip))(flipped.sourceless(
          NoteArg.Player(holder), NoteArg.Banner(banner),
          NoteArg.Card(TheNeedleCard.id)))))))
```

Check `Operation.flatten`'s signature (it takes an `Operation`) and
`BannerRules.displayName`; the imports are `catalog.{Legacy, LegacyGoal,
PrintedPower}`, `gameplay.actions.BannerRules`,
`gameplay.chronicle.ChronicleLegacyRules`,
`gameplay.powerresolver.{Contribution, ExecutedBatch, ExecutedTrigger}`,
`gameplay.powers.PowerAnswers`, `model._`. Register `TheNeedle` in
`WalkerPowerCatalog.contributing` beside the legacy modifiers. Remove
`TheNeedleCard` from holding and move its test imports.

- [ ] **Step 4: Run it and see it pass**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.* distantlands.application.* distantlands.catalog.* distantlands.serialization.*"`
Expected: all pass; `declared-keys.txt` gains The Needle's line and
`unimplemented.txt` loses `legacy.the-needle`; every golden unchanged.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/banner/TheNeedle.scala \
  src/test/scala/distantlands/gameplay/powers/banner/TheNeedleSuite.scala
git commit -m "feat(legacies): The Needle's holder may flip a banner they take

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/powers/banner/TheNeedle.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/powers/banner/TheNeedleSuite.scala \
  <each test file whose TheNeedleCard import moved> \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt
git log -3
```

### Task 3: The end die's modifier

Written to Q1 (a) and Q2.

**Files:**
- Modify: `src/main/scala/distantlands/model/GameState.scala`
  (`CurrentGameState.endDieModifier: Int = 0`)
- Modify: `src/main/scala/distantlands/model/TurnOperation.scala`
  (`SetEndDieModifier`)
- Modify: `src/main/scala/distantlands/model/GameEventProtocol.scala`
  (`EndDieRolled.modifier: Int = 0`)
- Modify: `src/main/scala/distantlands/gameplay/operations/LeafApplication.scala`
  and `TurnStateOperations.scala`
- Modify: `src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala`
  (`endRound`, `evolve` of `EndDieRolled`)
- Modify: `src/main/scala/distantlands/gameplay/EmpireEndings.scala`
  (`stableRegime` takes the total; its doc)
- Modify: `src/main/scala/distantlands/serialization/EndingEventCodec.scala`
  and `TurnOperationCodec.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala`
  (`endDie`)
- Test: `src/test/scala/distantlands/gameplay/EmpireRoundEndSuite.scala`
- Test: `src/test/scala/distantlands/serialization/GameEventFactsWireSuite.scala`
- Test: `src/test/scala/distantlands/serialization/WalkerOperationWireSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogHeadlineSuite.scala`

**Interfaces:**
- Produces: `SetEndDieModifier(amount: Int) extends TurnOperation,
  LeafOperation`; `CurrentGameState.endDieModifier`;
  `EndDieRolled(round, face, result, modifier = 0)`;
  `EmpireEndings.stableRegime(ready, total: Int)`.

- [ ] **Step 1: Write the failing tests**

`EmpireRoundEndSuite`, on its `roundEnd(round)` table (p1 the
Chancellor, p2 a Citizen holding the title, p3 an Exile, at Round End):

```scala
  /** `roundEnd(round)` with `modifier` waiting for the roll. */
  private def modified(round: Int, modifier: Int): ReadyGame =
    roundEnd(round).updateCurrent(_.copy(endDieModifier = modifier))

  test("a modified round-five 4 is a 6, and the Chancellor wins " +
      "(Legacy in-play powers ruling 21)"):
    val won = GameResult(p1, VictoryKind.StableRegime)
    val events = ended(modified(5, 2), RoundEndChoices.endDie(4))
    assertEquals(events, Vector(EndDieRolled(5, 4, Some(won), 2)))
    val after = Outcomes.ready(accepted(replayed(modified(5, 2), events)))
    assertEquals(after.game.current.result, Some(won))
    assertEquals(after.game.current.endDieModifier, 0)

  test("a modified round-seven 3 is a 2, and the round goes on"):
    assertEquals(ended(modified(7, -1), RoundEndChoices.endDie(3)),
      Vector(EndDieRolled(7, 3, None, -1), RoundEnded(7, Some(8))))

  test("replay refuses a modifier other than the one waiting"):
    assert(replayed(modified(5, 2), Vector(EndDieRolled(5, 4,
      Some(GameResult(p1, VictoryKind.StableRegime)), 1))).isLeft)
    assert(replayed(roundEnd(5), Vector(EndDieRolled(5, 4,
      Some(GameResult(p1, VictoryKind.StableRegime)), 2))).isLeft)
```

`GameEventFactsWireSuite`:

```scala
  test("an end die without a modifier, as saved before Arbiter, decodes " +
      "with modifier 0, and a modified one round-trips"):
    val old = """{"round":6,"face":5,"winnerPlayerId":null,""" +
      """"victoryKind":null}"""
    // Decode `old` through the ending codec as the suite's other payload
    // tests do, expecting EndDieRolled(6, 5, None, 0); then round-trip
    // EndDieRolled(5, 4, Some(GameResult(...)), 2), and check an
    // unmodified roll encodes no "modifier" key.
```

`WalkerOperationWireSuite.everyOperation`: `SetEndDieModifier(2)`,
`SetEndDieModifier(-1)`.

`GameLogHeadlineSuite`:

```scala
  test("a modified end die posts its face, the modifier and the total"):
    val script = recordedEventsBoard
    assertEquals(text(formatAppended(script,
      Vector(OathEvent.EndDieRolled(5, 4, None, 2))).last),
      "The end die rolled 4, modified by +2 to 6. Stable Regime needed 6.")
```

- [ ] **Step 2: Run them and see them fail**

Run: `./sbtw "testOnly distantlands.gameplay.EmpireRoundEndSuite distantlands.serialization.* distantlands.application.gamelog.GameLogHeadlineSuite"`
Expected: compile errors, `endDieModifier` and the fourth field of
`EndDieRolled` do not exist.

- [ ] **Step 3: Implement**

- `CurrentGameState`: `endDieModifier: Int = 0`, documented "Arbiter's
  modifier, chosen before the end die and read by the roll, which
  clears it (Legacy in-play powers ruling 21; Q1)."
- `SetEndDieModifier(amount: Int)` in `TurnOperation.scala`, with
  `require(amount >= -1 && amount <= 3)`; `TurnStateOperations` sets the
  field; `LeafApplication` routes it; `TurnOperationCodec` spells it
  `"set-end-die-modifier"` with `"amount"`, in the turn family.
- `EndDieRolled`: `modifier: Int = 0` last, its doc naming Arbiter.
- `EmpireEndings.stableRegime(ready, total)`: `total` replaces `face`;
  its doc says the total is the face plus Arbiter's modifier, read as "at
  least" the printed roll.
- `StateBasedEvaluation.endRound`: `val modifier =
  current.endDieModifier; val result = EmpireEndings.stableRegime(ready,
  face + modifier); EndDieRolled(round, face, result, modifier)`.
- Its `evolve` of `EndDieRolled`: refuse `recorded.modifier !=
  current.endDieModifier` ("end die modifier mismatch"); expect
  `stableRegime(ready, recorded.face + recorded.modifier)`; on success set
  `result` and `endDieModifier = 0`.
- `EndingEventCodec`: encode `"modifier"` only when not 0; decode it with
  `payload.obj.get("modifier")`, absent as 0.
- `GameLogFormatter`: the `EndDieRolled` case passes `modifier`;
  `endDie(round, face, modifier)` writes "The end die rolled 4, modified
  by +2 to 6." when `modifier != 0` (a minus sign for −1), then the
  needed clause as now.

- [ ] **Step 4: Run them and see them pass**

Run: `./sbtw "testOnly distantlands.gameplay.* distantlands.serialization.* distantlands.application.*"`
Expected: all pass; no golden or snapshot changes.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(empire): the end die carries a modifier, chosen before the roll and checked on replay

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/model \
  src/main/scala/distantlands/gameplay/operations \
  src/main/scala/distantlands/gameplay/StateBasedEvaluation.scala \
  src/main/scala/distantlands/gameplay/EmpireEndings.scala \
  src/main/scala/distantlands/serialization \
  src/main/scala/distantlands/application/gamelog/GameLogFormatter.scala \
  src/test/scala/distantlands/gameplay/EmpireRoundEndSuite.scala \
  src/test/scala/distantlands/serialization \
  src/test/scala/distantlands/application/gamelog/GameLogHeadlineSuite.scala
git log -3
```

### Task 4: Arbiter's step

Written to Q1 (a), Q2 and Q3.

**Files:**
- Create: `src/main/scala/distantlands/gameplay/phases/rest/Arbiter.scala`
  (card, power, procedure)
- Modify: `src/main/scala/distantlands/model/ProcedureRef.scala`
  (`TriggeredProcedureRef.Arbiter`, key `"round-end-arbiter"`, in `all`)
- Modify: `src/main/scala/distantlands/gameplay/OathDriver.scala`
  (`turnStart`)
- Modify: `src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala`
  (remove `ArbiterCard`), and the test imports of `ArbiterCard`
- Test: `src/test/scala/distantlands/gameplay/phases/rest/ArbiterSuite.scala`
- Test: `src/test/scala/distantlands/model/EnumWireKeySuite.scala`,
  `ProcedureRefSuite.scala` (the new key)

**Interfaces:**
- Produces: `ArbiterCard` (L23); `case object Arbiter extends
  ContributingPower` (no window); `object ArbiterStep` with
  `decisionId(player: PlayerId): String` (`round-end.legacy.arbiter.<player>`),
  `due(ready: ReadyGame): Option[PlayerId]`, `build`, `noteKeys`, and the
  buttons `decline`, `by(amount: Int)` for −1, +1, +2, +3.

- [ ] **Step 1: Write the failing test**

Through the driver, on the service's round-end port, with named tables
(read `RestSuite`'s round-end tests and `EmpireRoundEndSuite` for the
Round End table, and `RoundEndChoices.endDie`):

```scala
/** Arbiter (L23): before the end die is rolled, its holder may modify it
  * by −1 to +3 (Legacy in-play powers ruling 21). */
class ArbiterSuite extends munit.FunSuite:
  /** Round 5's last Rest: p1 the Chancellor, p2 a Citizen holding the
    * title, p3 an Exile holding Arbiter active, in p3's Rest, the last
    * seat; the end die will roll `face`. */
  private def lastRestOfRound(round: Int, face: Int): ... = ...

  test("before the roll the holder is asked: Decline, −1, +1, +2, +3, " +
      "public"):
    // After p3 finishes Rest, the walk parks on
    // ArbiterStep.decisionId(p3), owner p3, options in that order; no
    // EndDieRolled is journaled yet.

  test("+2 turns a round-five 4 into the Chancellor's win, and both lines " +
      "are written"):
    // Answer by(2): SetEndDieModifier(2) is journaled, then
    // EndDieRolled(5, 4, Some(win), 2); lines "p3 modified the end die by
    // +2 with L23" and the roll's.

  test("−1 turns a round-seven 3 into a loss, and round 8 begins"):

  test("Decline rolls the plain die and writes no Arbiter line"):

  test("no step when the end die is not rolled: rounds 1-4, round 8, an " +
      "Exile Oathkeeper"):

  test("a dormant Arbiter, or none, asks nothing (ruling 1)"):

  test("a walk resumed at Arbiter's question rolls once, with the chosen " +
      "modifier, and the whole journal replays"):
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.phases.rest.ArbiterSuite"`
Expected: compile error, `ArbiterCard` is not in `gameplay.phases.rest`.

- [ ] **Step 3: Implement**

`Arbiter.scala` (card object as printed, moved from holding):

```scala
/** Arbiter (legacy L23): "End of round: Before the end die is rolled, you
  * may choose to modify the roll by −1 to +3" (Legacy in-play powers
  * design, ruling 21). Hooks no window: the driver starts `ArbiterStep`
  * before the roll. Registered so the card counts as implemented. */
case object Arbiter extends ContributingPower:
  val id: PowerId = ArbiterCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

/** Arbiter's step, a triggered walker procedure the driver starts at a
  * round's end when the end die will be rolled and a seated player holds
  * Arbiter active, before `TurnBoundary.finishRound` rolls it.
  *
  * {{{
  * Sequence(
  *   Decide(round-end.legacy.arbiter.<holder>, holder,
  *     ChooseOne(Decline, −1, +1, +2, +3)),              // public (Q3)
  *   BuildOps(SetEndDieModifier(n) unless Decline),
  *   Note("p2 modified the end die by +2 with Arbiter")) // not on Decline
  * }}}
  *
  * The question is asked whatever the die could change, so its shape
  * reads only who holds Arbiter, which the walk does not change; `build`
  * is also `rebuild`. The roll reads the modifier
  * (`CurrentGameState.endDieModifier`, Q1) and clears it.
  */
object ArbiterStep:
  def decisionId(player: PlayerId): String =
    s"round-end.legacy.arbiter.${player.value}"
  val decline: DecisionOptionRef.Button = DecisionOptionRef.Button("decline")
  val Amounts: Vector[Int] = Vector(-1, 1, 2, 3)
  def by(amount: Int): DecisionOptionRef.Button =
    DecisionOptionRef.Button(s"modify-$amount")

  private val procedure = TriggeredProcedureRef.Arbiter
  private val noteId = ProcedureNotes.id(procedure)
  private val notes = NoteSet(noteId)
  /** "{p2} modified the end die by +{2} with {Arbiter}". */
  val raised: NoteKey = notes.key("raised", Vector(NotePart.Arg(0),
    NotePart.Text(" modified the end die by +"), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  /** "{p2} modified the end die by −{1} with {Arbiter}". */
  val lowered: NoteKey = notes.key("lowered", Vector(NotePart.Arg(0),
    NotePart.Text(" modified the end die by −"), NotePart.Arg(1),
    NotePart.Text(" with "), NotePart.Arg(2)))
  def noteKeys: Vector[NoteKey] = notes.declared

  /** The holder asked now: the end die will be rolled and a seated player
    * holds Arbiter active. */
  def due(ready: ReadyGame): Option[PlayerId] =
    Option.when(ready.game.current.turn.phase == Phase.RoundEnd &&
      EmpireEndings.rollsEndDie(ready))(()).flatMap(_ =>
      ChronicleLegacyRules.holder(ready, ArbiterCard.id))

  def build(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      args: Vector[DecisionOptionRef]): Either[OathViolation, Operation] =
    for
      _ <- Either.cond(args.isEmpty, (), OathViolation.InvalidEventOrder(
        "Arbiter's step selects nothing"))
      holder <- due(state).toRight(OathViolation.InvalidEventOrder(
        "no Arbiter's step is due"))
    yield
      val id = decisionId(holder)
      def chosen(answered: Vector[Answered]): Option[Int] =
        PowerAnswers.last(answered, id).flatMap(ref =>
          Amounts.find(by(_) == ref))
      Sequence(Vector(
        Decide(id, holder, DecisionQuery.ChooseOne(
          DecisionOption.Button(decline, "Decline") +: Amounts.map(amount =>
            DecisionOption.Button(by(amount),
              if amount > 0 then s"+$amount" else s"−${-amount}")),
          heading = Some("Arbiter: modify the end die before it is " +
            "rolled?"))),
        BuildOps((_, pending) => Right(chosen(pending.answered).map(amount =>
          SetEndDieModifier(amount): CoreOperation).toVector),
          required = true),
        Note(noteId, states => chosen(states.answered).map(amount =>
          (if amount > 0 then raised else lowered).sourceless(
            NoteArg.Player(holder), NoteArg.Number(amount.abs),
            NoteArg.Card(ArbiterCard.id))))))
```

`ProcedureRef.scala`: `case Arbiter extends
TriggeredProcedureRef("round-end-arbiter")`, added to `all`.
`WalkerProcedureRegistry`: `TriggeredProcedureRef.Arbiter ->
Entry.withoutStartGates(ArbiterStep.build)`. `ProcedureNotes`: the
`Arbiter` case gives `ArbiterStep.noteKeys`. `ProcedureLog`: an entry
with no tag and `raised`/`lowered` as `LogKind.Decision`, as
`QuestFirstWake`'s is. `WalkerPowerCatalog.contributing`: `Arbiter`.

`OathDriver.turnStart`:

```scala
    // Arbiter's step has run: roll the end die, then the Wake (Legacy
    // in-play powers ruling 21).
    case TriggeredProcedureRef.Arbiter => roundEnded(transition)
    case PhaseTransitionRef.FinishRest | TriggeredProcedureRef.Setup =>
      transition.state match
        case Ready(ready) if ready.game.current.turn.phase ==
            Phase.RoundEnd && ArbiterStep.due(ready).nonEmpty =>
          startTriggered(transition, TriggeredProcedureRef.Arbiter)
        case _ => roundEnded(transition)
```

with `roundEnded` the old body: `finishRound` at Round End, then
`enterWake` unless a result is set. The class doc's step 2 names
Arbiter's step before the round's end.

- [ ] **Step 4: Run it and see it pass**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.* distantlands.application.* distantlands.model.* distantlands.catalog.* distantlands.serialization.*"`
Expected: all pass; `declared-keys.txt` gains the two lines under
`procedure.round-end-arbiter`, `unimplemented.txt` loses `legacy.arbiter`.

- [ ] **Step 5: Commit**

```bash
git add src/main/scala/distantlands/gameplay/phases/rest/Arbiter.scala \
  src/test/scala/distantlands/gameplay/phases/rest/ArbiterSuite.scala
git commit -m "feat(legacies): Arbiter's holder modifies the end die before it is rolled

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/phases/rest/Arbiter.scala \
  src/main/scala/distantlands/model/ProcedureRef.scala \
  src/main/scala/distantlands/gameplay/OathDriver.scala \
  src/main/scala/distantlands/gameplay/walker/WalkerProcedureRegistry.scala \
  src/main/scala/distantlands/gameplay/actions/ProcedureNotes.scala \
  src/main/scala/distantlands/application/gamelog/ProcedureLog.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala \
  src/test/scala/distantlands/gameplay/phases/rest/ArbiterSuite.scala \
  src/test/scala/distantlands/model/EnumWireKeySuite.scala \
  src/test/scala/distantlands/model/ProcedureRefSuite.scala \
  <each test file whose ArbiterCard import moved> \
  src/test/resources/notes/declared-keys.txt \
  src/test/resources/powers/unimplemented.txt
git log -3
```

### Task 5: Iron Hand's discard on becoming Imperial

Written to Q2.

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/roles/RoleChanges.scala`
  (`becomeImperial`)
- Modify: `src/main/scala/distantlands/gameplay/roles/Citizenship.scala`
  (`become`'s line)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/legacies/IronHand.scala`
  (its own notes and doc)
- Test: `src/test/scala/distantlands/gameplay/chronicle/legacies/IronHandSuite.scala`
  or a new `IronHandCitizenshipSuite` there

**Interfaces:**
- Produces: `IronHand.discardedOnBecoming(role: Role): NoteKey`;
  `IronHand.inGameDiscard(ready: ReadyGame, lineage: LineageId):
  Vector[CoreOperation]`.

- [ ] **Step 1: Write the failing test**

```scala
/** Iron Hand's discard when its holder becomes Imperial during a game
  * (Legacy in-play powers ruling 22; Chronicle powers ruling 18). */
class IronHandCitizenshipSuite extends munit.FunSuite:
  /** GrandScepterActionSuite's table: p2 the Chancellor; p1, an Exile in
    * their Act at Broken Peaks, holds the Grand Scepter and 2 board
    * warbands, and Iron Hand active. */
  private def scepterHolderWithIronHand: Table = ...

  /** NegotiationCitizenshipSuite's table: p1 the Chancellor at Broken
    * Peaks with the Grand Scepter, beside p2 and p3, Exiles with 2 board
    * warbands, p2 holding Iron Hand active. */
  private def dealWithIronHandHolder: Table = ...

  test("becoming a Citizen by the Grand Scepter's offer discards it, " +
      "right after the role changes"):
    // Iron Hand is in the legacy deck, not in p1's lineage; the batch
    // holds RoleChanged(l1, Citizen) and then the Move to
    // Deck(CardDeck.Legacy); one line "p1 discarded L01 on becoming a
    // Citizen".

  test("becoming a Citizen by a deal discards it"):

  test("with no Empire, becoming the Chancellor discards it, and the line " +
      "says so"):

  test("a dormant Iron Hand stays (ruling 1)"):
```

- [ ] **Step 2: Run it and see it fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.IronHandCitizenshipSuite"`
Expected: Iron Hand is still in p1's lineage.

- [ ] **Step 3: Implement**

`IronHand`: `inGameDiscard(ready, lineage)` is the Chronicle discard's
`Move(Piece.Card(IronHandCard.id), Legacies(lineage) → Deck(Legacy))`
when that lineage holds it active, else nothing; a `NoteSet(id)` with two
keys, "{0} discarded {1} on becoming a Citizen" and "{0} discarded {1}
on becoming the Chancellor", which `discardedOnBecoming(role)` picks,
declared in `noteKeys`; its doc's "Its discard during a game is out of scope" now
names `RoleChanges.becomeImperial`.

`RoleChanges.becomeImperial`: `Vector(RoleChanged(state.lineage, role))
++ IronHand.inGameDiscard(ready, state.lineage) ++ gained ...`.

`Citizenship.become`: after the change's `BuildOps`,
`Note(IronHand.id, ironHandLine(actor))`, reading the step before: Iron
Hand left `actor`'s lineage, and the role it reached picks the key.

- [ ] **Step 4: Run it and see it pass**

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.* distantlands.application.*"`
Expected: all pass; `declared-keys.txt` gains Iron Hand's two lines;
the `deal-citizen` and `scepter-citizen` goldens unchanged (no Iron
Hand there).

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): Iron Hand is discarded when its holder becomes Imperial during a game

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>" -- \
  src/main/scala/distantlands/gameplay/roles/RoleChanges.scala \
  src/main/scala/distantlands/gameplay/roles/Citizenship.scala \
  src/main/scala/distantlands/gameplay/chronicle/legacies/IronHand.scala \
  src/test/scala/distantlands/gameplay/chronicle/legacies/IronHandCitizenshipSuite.scala \
  src/test/resources/notes/declared-keys.txt
git log -3
```

### Task 6: The golden log, the docs and the gate

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/LegacyActionLogScripts.scala`
  (a `legacy-triggers` script)
- Modify: `src/test/scala/distantlands/application/gamelog/LogScriptSupport.scala`
  (`atTable` takes a `RoundEndRandomPort`, default random)
- Create: the `legacy-triggers` golden pair
- Modify: `docs/rules/ambiguities.md`, `implementation-traceability.md`,
  `rules-reference.md`, the design's status line

- [ ] **Step 1: The script**

`legacyTriggers`, on `Table.start` with the round set to 5 (as
`EmpireRoundEndSuite.roundEnd` sets it) and the service's round-end port
rolling 4 (`RoundEndChoices.endDie(4)`, passed through `atTable`):

- p1 is the Chancellor and holds the Oathkeeper title; p2 is a Citizen
  holding Arbiter active; p3 is an Exile at Broken Peaks in their Act, the
  last seat of the round, holding the Grand Scepter, Iron Hand active and
  2 board warbands.
- p3 uses the Grand Scepter's Action (`MenuPlay.start(p3,
  ActionRef.UsePower(GrandScepterAction.id), Vector(scepter))`), becomes a
  Citizen and discards Iron Hand; the Action ends their Act and Rest runs
  on to the round's end.
- Arbiter's question parks for p2, answered `ArbiterStep.by(2)`
  (`withAnswers`); the end die rolls 4, modified to 6, and the Chancellor
  wins.

The Needle's line stays in `TheNeedleSuite`: taking a banner needs a
challenge or a raid with dice, which this turn does not hold.

- [ ] **Step 2: Generate and review the golden**

`GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite -- *legacy-triggers*"`,
then the gamelog suites. Expected lines, for both seats: "p3 discarded
Iron Hand on becoming a Citizen", "p2 modified the end die by +2 with
Arbiter", "The end die rolled 4, modified by +2 to 6. Stable Regime
needed 6." and the Chancellor's victory. Every other golden unchanged.

- [ ] **Step 3: Docs**

- `ambiguities.md`: the design's rulings 20-22 and slice 3's
  implementation rulings, numbered on, the section's opening sentence
  updated.
- `implementation-traceability.md`: a row "Legacy trigger and role
  powers" after "Legacy modifier powers"; the "Rounds 5-8, Stable Regime"
  row names Arbiter's step and the modifier on `EndDieRolled`; the Throne
  row's Iron Hand note names the in-game discard.
- `rules-reference.md`: "Digital:" notes under Empire endings (Arbiter),
  Role changes (Iron Hand) and the banners (The Needle).
- The design's status line: slice 3 has landed.

- [ ] **Step 4: Run the gate** (top of this plan).

- [ ] **Step 5: Commit** the script, the goldens, `LogScriptSupport` and
  the docs, with explicit pathspecs.

## Self-review notes

- Spec rulings: 20 Tasks 1 and 2; 21 Tasks 3 and 4; 22 Task 5. Rulings
  1-2: the dormant cases of Tasks 2, 4 and 5.
- The slice 3 test list: The Needle after a challenge and after a raid,
  the Festival marker removed on flipping away and not placed on flipping
  to it, no question at game start (Tasks 1 and 2); Arbiter's round-five
  4 into a win with +2 and round-seven 3 into a loss with −1, no step
  when the die is not rolled, replay rejecting a changed modifier (Tasks
  3 and 4); Iron Hand discarded by the Grand Scepter's offer and by a
  deal (Task 5). A golden log (Task 6). The owner's addition: an old
  saved roll without the modifier decodes and replays (Task 3).
- Names across tasks: `FlipBanner`, `BoardControlOperations.flipBanner`,
  `TheNeedle.decisionId`, `SetEndDieModifier`,
  `CurrentGameState.endDieModifier`, `EndDieRolled.modifier`,
  `EmpireEndings.stableRegime(ready, total)`, `ArbiterStep.due`,
  `.decisionId`, `.by`, `TriggeredProcedureRef.Arbiter`,
  `IronHand.inGameDiscard`, `.discardedOnBecoming`.
- Weak spot, to settle at execution: The Needle's, Arbiter's and Iron
  Hand's tables are named and described with their sites and pieces, but
  not yet written as code; their harnesses are the challenge, raid,
  round-end, Grand Scepter and deal suites each task names. Each becomes
  a named fixture in the test itself, as slice 2's did.
