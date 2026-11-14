# Legacy Chronicle Powers, Slice 4: Iron Hand Implementation Plan

> Executed inline by the implementer.

**Goal:** Iron Hand's Chronicle half acts. An Imperial holding Iron Hand
discards it at three checkpoints: the start of the Sun, after Crown
Chancellor and after Offer Citizenship. While a seated Exile holds it
active, that Exile performs Shape Empire and Build Edifice in the Empire's
place, over the sites they rule; an Imperial Great Architect still builds.
In Score Lineage Points, after the winner, the holder scores the legacy
bonus at the sites they rule. In the Stars, the Empire's sites are archived
as rival sites and the holder's are stored in their place, Cradlemost
first. The card counts as implemented (R7: its SETUP half stays
uncovered).

**Architecture:** The seam carries on. `ChronicleLegacyRules.ironHand`
names the seated Exile holding Iron Hand active, read when each step is
reached. Changed steps ask it: Shape Empire's actor (`shaper`), Build
Edifice's builders, Score Lineage Points' second scorer and
`ChronicleExtraction.layout`'s stored sites. The discard checkpoints are
one `IronHand.discard` node placed in the three steps, before each step's
reactivation cap. The lines are procedure lines on the Sun's, the Throne's
and the Stars' note ids.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Legacy Chronicle powers design](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-design.md),
rulings 1-4 and 18-21, and section "Slice 4: Iron Hand".
[Implementation rulings](../../specs/2026-11/2026-11-13-legacy-chronicle-powers-implementation-rulings.md)
R1-R28 (R7: a card with an unbuilt half leaves holding whole; R2: slice 4
decides whether Iron Hand's scoring records). Card text in
`src/main/scala/distantlands/catalog/holding/HoldingLegacies.scala` (Iron
Hand L01). `docs/rules/chronicle.md`, the Sun steps 1 and 6, the Throne
steps 1-3 and 6, the Stars steps 2 and 4; Chronicle rulings 11, 17 and 18.

## Prerequisite

Slice 3 has landed (through `19929f5af`). Check before Task 1:

```bash
grep -q "object TheBrightBeacon" src/main/scala/distantlands/gameplay/chronicle/legacies/TheBrightBeacon.scala &&
grep -q "def builder" src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala &&
grep -q "def layout" src/main/scala/distantlands/gameplay/chronicle/ChronicleExtraction.scala &&
echo ready
```

Expected: `ready`.

## Open questions

These change behaviour, data or the public shape of the code and wait for
the orchestrator's rulings (R29 onward). The tasks are written to each
recommendation.

1. **A dormant Iron Hand at a checkpoint.** The card prints "If you are
   Imperial, discard Iron Hand." Ruling 1 says a dormant legacy has no
   power. Options: (a) a checkpoint discards only an active Iron Hand; a
   dormant one an Imperial holds stays until Flip Legacies activates it and
   the next checkpoint (after Crown Chancellor) finds it; (b) any Iron Hand
   an Imperial holds, active or dormant. Recommendation: (a), ruling 1.
2. **Where the checkpoints sit.** Ruling 18 names the start of the Sun,
   after Crown Chancellor and after Offer Citizenship. The Crown and Offer
   Citizenship steps end with a reactivation and then the three-legacy
   cap (Legacies R19, R20). Options: (a) the discard runs after the
   reactivation and before the cap, so a player is never asked to discard
   a legacy over the cap that Iron Hand's own discard would have spared;
   placed by the steps themselves (a changed step, as Steadfast's), the
   Sun's as the first node of Reveal Relics' window; (b) `Transform`s
   appended to the Crown and Offer Citizenship windows and prepended to
   Reveal Relics', after each cap, as the design's Approach groups the
   checkpoints with the added steps. Recommendation: (a).
3. **What the holder's Shape Empire discards.** The step discards "the
   denizens you do not rule". Options: (a) literally: every denizen and
   ruined edifice at a site the holder does not rule, the Empire's sites
   included, but intact edifices and Enduring sites (the holder's sites
   are the ones stored, ruling 21, so the rest are archived as rivals);
   (b) only cards at sites neither the holder nor the Empire rules.
   Recommendation: (a).
4. **An Imperial Great Architect beside Iron Hand.** Ruling 19: "an
   Imperial Great Architect still builds"; ruling 19 also has the holder
   perform Build Edifice. Options: (a) both build, sharing the available
   edifices: for each suit, the Great Architect is offered the suit's
   available edifice first over the Empire's sites (any card buried, ruling
   10), then, if it is still available, the Iron Hand holder over theirs
   (matching cards only). The holder's question takes its own id,
   `chronicle.build-edifice.<suit>.iron-hand`, so no two decisions share
   one; (b) only the Great Architect builds. Recommendation: (a).
5. **Does Iron Hand's scoring record its legacies?** R2 left it to this
   slice. Options: (a) one `LegaciesScored` of every legacy the holder
   holds, as Chronicler records, though no later scoring reads it; (b) no
   record. Recommendation: (a), so "a legacy scores once per game" is a
   journaled fact wherever a legacy scores outside Score Lineage Points'
   winner.
6. **The lines.** Proposed: "p2 discarded Iron Hand" at each checkpoint (a
   delta line, the spec's wording); "p3 scored 2 lineage points with Iron
   Hand (2 from Beast)" after the winner's lines (a delta line), none when
   the bonus is 0; in the Stars, "Buried Giant stays on the map with Iron
   Hand" for each of the holder's sites, in place of "... stays in the
   Empire" (a delta line). Shape Empire's and Build Edifice's existing
   lines name their actor already. Recommendation: as proposed.
7. **The next game until the setup batch.** Ruling 21 stores the holder's
   sites in the Chronicle's `world`, which game start restores as the
   Empire's sites, with a purple warband each (`LaterGameSetup`). Iron
   Hand's SETUP half ("you place one warband on all sites coming from the
   Atlas until the Empire divider") is out of scope, so until the setup
   batch the stored sites start the next game as the Empire's. Options:
   (a) accept it, name it on the ROADMAP's setup item, and test that the
   record passes game start; (b) build the SETUP half's placement now.
   Recommendation: (a).
8. **Test fixtures that used Iron Hand as a stand-in.** Iron Hand is the
   first `MostSites` legacy, and 11 test files use it as a plain legacy.
   Where its Chronicle half now acts (an Imperial holding it in the Sun,
   or an active Exile holder reaching the Throne), the outcome changes.
   Proposed: in each such test, Iron Hand is swapped for a `MostSites`
   legacy with no Chronicle half that the test does not already hold
   (Peacemaker, The Needle or Circle of Swords), keeping the test's intent;
   tests about Iron Hand's goal keep it. Recommendation: as proposed, a
   mechanical change, listed in the report.

## Global Constraints

- Scala 3 with `-Werror`; every Scala file at or under 800 lines.
- `gameplay` never reads a catalog `.text` or `rulesText`.
- A new test names cards by their card objects.
- Every power reads its holder and role when its step is reached
  (rulings 1 and 2); the Crown and Offer Citizenship change roles, so each
  checkpoint reads them then.
- No two decisions share an id (ruling 3): the holder's Build Edifice
  question is `chronicle.build-edifice.<suit>.iron-hand` when a Great
  Architect also builds, and the plain `chronicle.build-edifice.<suit>`
  otherwise.
- R15: placed nodes shift a walk parked mid-Chronicle by an earlier build.
- TDD, one commit per task, test results read before every commit;
  coverage at or above 92% statements and 84.4% branches; snapshots and
  goldens reviewed and committed in their task; `git status --short
  src/test/resources` otherwise clean.
- Commit messages: Conventional Commits, ending with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`; stage only
  this plan's files; `git log -3` after each commit.

---

### Task 1: The card, `ironHand` and the discard checkpoints

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/legacies/IronHand.scala`
- Modify: `LegacyChroniclePowers.scala` (`all`, `sunNoteKeys`,
  `throneNoteKeys`), `HoldingLegacies.scala` (remove `IronHandCard`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
  (`ironHand`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleSunProcedure.scala`
  (the Sun start and the Crown checkpoints)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/OfferCitizenship.scala`
  (the checkpoint before the cap)
- Modify: `src/main/scala/distantlands/application/gamelog/ProcedureLog.scala`
- Create: `src/test/scala/distantlands/gameplay/chronicle/legacies/IronHandSuite.scala`
- Modify (imports and stand-ins): every test that imports `IronHandCard`
  from holding (`grep -rln IronHandCard src/test`)
- Modify: the two snapshots

**Interfaces:**
- Produces: `IronHandCard` (whole text, R7); `IronHand` (power, no
  window); `ChronicleLegacyRules.ironHand(state): Option[PlayerId]`, the
  seated Exile holding Iron Hand active; `IronHand.discard(procedure:
  TriggeredProcedureRef): Operation`, a `Branch` that returns an active
  Iron Hand an Imperial holds to the legacy deck with its line;
  `IronHand.discarded(procedure): NoteKey` ("{0} discarded {1}") for the
  Sun and the Throne.

- [ ] **Step 1: Write the failing tests**

```scala
package distantlands.gameplay.chronicle.legacies

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.chronicle.{BuildEdifice, ChronicleWalk,
  OfferCitizenship, ShapeEmpire}
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p2, p3}

/** Iron Hand (L01), its Chronicle half: "If you are Imperial, discard Iron
  * Hand. ... Score your legacies in addition to the winner, and perform
  * Shape Empire and Build Edifice (Throne). Your sites are stored in place
  * of the Empire (Stars)." (rulings 18-21). */
class IronHandSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def through(table: Table, procedure: TriggeredProcedureRef,
      answers: PartialFunction[Decide, DecisionAnswer] = PartialFunction.empty,
      after: TriggeredProcedureRef = TriggeredProcedureRef.ChronicleBegin)(
      using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, procedure, answers, after)

  private def holds(ready: ReadyGame, player: PlayerId): Boolean =
    ready.game.campaign.lineages(ready.game.current.player(player).get.lineage)
      .legacies.exists(_.id == IronHandCard.id)

  private def lines(run: ChronicleWalk.Run, procedure: TriggeredProcedureRef)
      : Vector[String] =
    NoteText.said(ProcedureNotes.id(procedure),
      IronHand.noteKeys(procedure), run.events).map(_.text)

  private val discardLine = s"p3 discarded ${IronHandCard.id.value}"

  test("Iron Hand is implemented"):
    assert(PowerImplementationStatus.implemented(IronHandCard.power.id))

  test("an Imperial holding Iron Hand active discards it at the start of " +
      "the Sun (ruling 18)"):
    val run = through(ChronicleWalk.chronicleWonByP1.imperial(p2, p3)
      .legacy(p3, IronHandCard), TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p3))
    assertEquals(lines(run, TriggeredProcedureRef.ChronicleSun).take(1),
      Vector(discardLine))

  test("a dormant Iron Hand an Imperial holds stays until Flip Legacies " +
      "activates it, then the Crown checkpoint discards it (R29)"):
    // The Empire rules Ancient City, so the Imperials meet Iron Hand's goal.
    val run = through(ChronicleWalk.chronicleWonByP1.imperial(p2, p3)
      .warbandsAt(AncientCitySite, p2, 1)
      .legacy(p3, IronHandCard, active = false),
      TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p3))
    val said = NoteText.said(ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleSun),
      distantlands.gameplay.chronicle.ChronicleSunProcedure.noteKeys,
      run.events).map(_.text)
    assert(said.indexOf(s"p3 activated ${IronHandCard.id.value}") <
      said.indexOf(discardLine), said)

  test("an Exile winner holding Iron Hand discards it after Crown " +
      "Chancellor (ruling 18)"):
    // p1 rules Ancient City alone, so keeps Iron Hand active in step 3.
    val run = through(ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 1).legacy(p1, IronHandCard),
      TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p1))
    assert(lines(run, TriggeredProcedureRef.ChronicleSun).contains(
      s"p1 discarded ${IronHandCard.id.value}"))

  test("an Exile who becomes a Citizen at Offer Citizenship discards Iron " +
      "Hand, and the Empire shapes itself again (ruling 18)"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(AncientCitySite, p1, 1).warbandsAt(BuriedGiantSite, p3, 1)
      .legacy(p3, IronHandCard)
    val run = through(table, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == OfferCitizenship.offerDecisionId =>
        DecisionAnswer.ChooseManyAnswer(Vector(DecisionOptionRef.Player(p3)))
      case decide if decide.decisionId ==
          OfferCitizenship.answerDecisionId(p3) =>
        DecisionAnswer.ChooseOneAnswer(OfferCitizenship.accept)
    }, after = TriggeredProcedureRef.ChronicleSun)
    assert(!holds(run.ready, p3))
    assert(lines(run, TriggeredProcedureRef.ChronicleThrone)
      .contains(discardLine))

  test("a boxed lineage's Iron Hand does nothing (ruling 1)"):
    val boxed = LineageState(LineageId("lineage:boxed"), None, Role.Citizen,
      Vector(LegacyState(IronHandCard.id, active = true)), Vector.empty)
    val table = ChronicleWalk.chronicleWonByP1.update(_.updateCampaign(
      campaign => campaign.copy(baseline = campaign.baseline.copy(
        lineages = Vector(boxed)))))
    val run = through(table, TriggeredProcedureRef.ChronicleSun)
    assertEquals(lines(run, TriggeredProcedureRef.ChronicleSun), Vector.empty)
```

(Tasks 2-4 add their tests to this suite.)

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.legacies.IronHandSuite"`
Expected: FAIL to compile: `Not found: IronHandCard` in the new package.

- [ ] **Step 3: Implement**

`IronHand.scala`: move `IronHandCard` verbatim (its whole text, R7), then:

```scala
/** Iron Hand (legacy L01), its Chronicle half (Legacy Chronicle powers
  * design, rulings 18-21). An Imperial holding it active discards it at
  * three checkpoints: the start of the Sun, after Crown Chancellor and
  * after Offer Citizenship, each before that step's reactivation cap
  * (R29, R30). While a seated Exile holds it active
  * (`ChronicleLegacyRules.ironHand`), they perform Shape Empire and Build
  * Edifice in the Empire's place, score the legacy bonus after the winner,
  * and their sites are stored in place of the Empire's. Its SETUP half and
  * its discard during a game wait for their batches (R7). This power hooks
  * no window; the steps read it. Line: "p2 discarded Iron Hand". */
case object IronHand extends ContributingPower:
  val id: PowerId = IronHandCard.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty

  private val discarding: Vector[TriggeredProcedureRef] = Vector(
    TriggeredProcedureRef.ChronicleSun, TriggeredProcedureRef.ChronicleThrone)
  private val discardKeys: Map[TriggeredProcedureRef, (NoteSet, NoteKey)] =
    discarding.map(procedure =>
      val notes = NoteSet(ProcedureNotes.id(procedure))
      procedure -> (notes, notes.key("iron-hand-discarded", Vector(
        NotePart.Arg(0), NotePart.Text(" discarded "), NotePart.Arg(1)))))
      .toMap

  def discarded(procedure: TriggeredProcedureRef): NoteKey =
    discardKeys(procedure)._2

  /** The lines this power declares under `procedure`'s note id; Tasks 3
    * and 4 add the Throne's scoring line and the Stars' stored line. */
  def noteKeys(procedure: TriggeredProcedureRef): Vector[NoteKey] =
    discardKeys.get(procedure).toVector.flatMap(_._1.declared)

  /** A checkpoint (ruling 18): an active Iron Hand a seated Imperial holds
    * returns to the legacy deck, read when reached. */
  def discard(procedure: TriggeredProcedureRef): Operation =
    Branch((state, _) =>
      ChronicleLegacyRules.holder(state, IronHandCard.id,
        ChronicleLegacyRules.imperialRoles).toVector.flatMap(holder =>
        ChronicleTasks.lineageOf(state, holder).toVector.flatMap(lineage =>
          Vector(BuildOps((_, _) => Right(Vector(Move(Piece.Card(IronHandCard.id),
              PositionedLocation(Location.Legacies(lineage)),
              PositionedLocation(Location.Deck(CardDeck.Legacy))))),
              required = true),
            Note(ProcedureNotes.id(procedure), _ => Some(discarded(procedure)
              .sourceless(NoteArg.Player(holder),
                NoteArg.Card(IronHandCard.id))))))))
```

`ChronicleLegacyRules.ironHand(state) = holder(state, IronHandCard.id,
Set(Role.Exile))`.

`ChronicleSunProcedure.steps`: the Reveal Relics window's children start
with `IronHand.discard(ChronicleSun)`; the Crown window's tail becomes
`Vector(reactivation, IronHand.discard(ChronicleSun), cap)` in place of
`ResolveLegacies.reactivate(...)` (call `ResolveLegacies.reactivation` and
`ResolveLegacies.cap` directly, as Offer Citizenship already does).
`OfferCitizenship.answers`: `IronHand.discard(ChronicleThrone)` after the
`Simultaneous` node and before the cap `Branch`. The Sun's and the
Throne's `noteKeys` add `IronHand.noteKeys(...)`; `ProcedureLog` gives the
two discard keys `LogKind.Delta`.

- [ ] **Step 4: Run the tests, adapt the stand-ins, rewrite the snapshots**

Run: `./sbtw test`. For each failure where Iron Hand stood in for a plain
legacy and its Chronicle half now acts, swap it in that test for a
`MostSites` legacy with no Chronicle half the test does not already hold
(`PeacemakerCard`, `TheNeedleCard` or `CircleOfSwordsCard`), with the
test's assertions renamed to match; keep Iron Hand where the test is about
its goal (`LegacyGoalsSuite`) or its card data. List every swapped test in
the commit message. Rewrite the unimplemented and note-keys snapshots
(diffs: `legacy.iron-hand` removed; two `iron-hand-discarded` keys added).
Run `./sbtw test` again and read its result: PASS.

- [ ] **Step 5: Commit**

```bash
git add <this task's files>
git commit -m "feat(legacies): an Imperial discards Iron Hand at the Chronicle's checkpoints

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: Iron Hand performs Shape Empire and Build Edifice

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleLegacyRules.scala`
  (`shaper`, `builders`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpire.scala`
  (its actor)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/BuildEdifice.scala`
  (one or two builders per suit; the second's id; candidates per builder)
- Test: `IronHandSuite`

**Interfaces:**
- Produces: `ChronicleLegacyRules.shaper(state): Either[OathViolation,
  PlayerId]`, the Iron Hand holder, else the Throne holder;
  `ChronicleLegacyRules.builders(state): Either[OathViolation,
  Vector[PlayerId]]`: an Imperial Great Architect, then the Iron Hand
  holder, or the Iron Hand holder alone, or the Great Architect alone, or
  the Throne holder (rulings 10 and 19; R32);
  `BuildEdifice.ironHandDecisionId(suit)` =
  `s"chronicle.build-edifice.${suit.key}.iron-hand"`.

- [ ] **Step 1: Write the failing tests**

In `IronHandSuite`:

```scala
  /** p1, the Chancellor, holds the Throne, and the Empire rules Ancient
    * City and Deep Woods, which holds Fae Merchant; p3, an Exile, holds
    * Iron Hand and rules Broken Peaks and Buried Giant, which holds
    * Threatening Roar. */
  private def ironThrone: Table = ChronicleWalk.throneHeldByChancellorP1
    .warbandsAt(AncientCitySite, p1, 1).warbandsAt(DeepWoodsSite, p1, 1)
    .warbandsAt(BrokenPeaksSite, p3, 1).warbandsAt(BuriedGiantSite, p3, 1)
    .denizen(FaeMerchantCard, at = DeepWoodsSite)
    .denizen(ThreateningRoarCard, at = BuriedGiantSite)
    .legacy(p3, IronHandCard)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  test("the holder performs Shape Empire over the sites they rule, and the " +
      "Empire's is skipped (ruling 19; R31)"):
    val run = through(ironThrone, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val removal = run.asked.filter(_.decisionId ==
      ShapeEmpire.removeDecisionId)
    assertEquals(removal.map(_.owner), Vector(p3))
    // The holder rules Buried Giant, so Threatening Roar stays; Deep Woods
    // is the Empire's, so its Fae Merchant is discarded.
    assert(onMap(run.ready, ThreateningRoarCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))

  test("the holder performs Build Edifice over the sites they rule " +
      "(ruling 19)"):
    val suit = catalog.denizen(ThreateningRoarCard.id).get.suit
    val run = through(ironThrone, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val build = run.asked.find(_.decisionId == BuildEdifice.decisionId(suit))
      .get
    assertEquals(build.owner, p3)
    assertEquals(build.query.asInstanceOf[DecisionQuery.ChooseMany].options
      .map(_.ref), Vector(DecisionOptionRef.Denizen(ThreateningRoarCard.id)))

  test("an Imperial Great Architect still builds over the Empire's sites, " +
      "then the holder over theirs, under ids of their own (R32)"):
    // p2 is a Citizen holding Great Architect; Village Idiot, at Enduring
    // Ancient City, survives the holder's Shape Empire.
    val table = ironThrone.imperial(p1, p2)
      .denizen(VillageIdiotCard, at = AncientCitySite)
      .legacy(p2, GreatArchitectCard)
    val suit = catalog.denizen(ThreateningRoarCard.id).get.suit
    val other = Suit.all.find(_ != suit).get
    val run = through(table, TriggeredProcedureRef.ChronicleThrone, {
      case decide if decide.decisionId == BuildEdifice.decisionId(other) =>
        DecisionAnswer.ChooseManyAnswer(Vector(
          DecisionOptionRef.Denizen(VillageIdiotCard.id)))
    }, after = TriggeredProcedureRef.ChronicleSun)
    val builds = run.asked.filter(_.decisionId.startsWith(
      "chronicle.build-edifice."))
    assert(builds.exists(decide => decide.owner == p2 &&
      decide.decisionId == BuildEdifice.decisionId(other)))
    assert(builds.exists(decide => decide.owner == p3 &&
      decide.decisionId == BuildEdifice.ironHandDecisionId(suit)))
    assertEquals(builds.map(_.decisionId).distinct.size, builds.size)
    // Great Architect built the other suit's edifice: the holder is not
    // offered it.
    assert(!builds.exists(_.decisionId ==
      BuildEdifice.ironHandDecisionId(other)))

  test("a walk resumed at the holder's removal question finds the same " +
      "sites"):
    val started = ChronicleWalk.started(rules, ironThrone,
      TriggeredProcedureRef.ChronicleSun)
    val parked = ChronicleWalk.walkedTo(rules, started,
      ShapeEmpire.removeDecisionId)
    val decide = ChronicleWalk.parked(parked.state).get
    assertEquals(decide.owner, p3)
    val replayed = distantlands.testkit.Unwrap.accepted(parked.events
      .foldLeft[Either[OathViolation, OathState]](Right(ironThrone.state))(
        (state, event) => state.flatMap(rules.evolve(_, event))))
    assertEquals(ChronicleWalk.parked(replayed), Some(decide))
```

(Import `GreatArchitectCard` from this package and `VillageIdiotCard` from
`distantlands.gameplay.powers.economy`.)

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: the removal question is p1's.

- [ ] **Step 3: Implement**

`ChronicleLegacyRules`:

```scala
  /** Who performs Shape Empire: Iron Hand's Exile holder, else the Throne
    * holder (ruling 19). */
  def shaper(state: ReadyGame): Either[OathViolation, PlayerId] =
    ironHand(state).fold(ChronicleTasks.holder(state, ChronicleTask.Throne))(
      Right(_))

  /** Who performs Build Edifice, in order: an Imperial Great Architect,
    * then Iron Hand's holder; either alone; else the Throne holder
    * (rulings 10 and 19; R32). */
  def builders(state: ReadyGame): Either[OathViolation, Vector[PlayerId]] =
    holder(state, GreatArchitectCard.id, imperialRoles).toVector ++
      ironHand(state).toVector match
      case Vector() => ChronicleTasks.holder(state, ChronicleTask.Throne)
        .map(Vector(_))
      case some => Right(some)
```

Keep `builder` (the first of `builders`) only if something still reads
it; otherwise delete it.

`ShapeEmpire.steps`: both `Branch`es read `ChronicleLegacyRules.shaper` in
place of `ChronicleTasks.forHolder(state, ChronicleTask.Throne)`.

`BuildEdifice.steps`: two `Branch`es per suit, the first for
`builders(state).head`, the second for `builders(state).lift(1)` (empty
when there is none), each reading `available` when reached, so a suit the
first builder built offers nothing to the second; the second's question
uses `ironHandDecisionId(suit)`. `candidates` takes the builder: any suit
only when the builder is the Great Architect's holder (`anyEdifice` and
that holder), matching cards otherwise. `offer` takes the decision id.

- [ ] **Step 4: Run the tests to verify they pass**

Run `./sbtw test` and read the result: PASS.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): Iron Hand shapes and builds in the Empire's place

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: Iron Hand scores its holder's legacies

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/LineageScoring.scala`
  (`steps`: the holder after the winner)
- Modify: `IronHand.scala` (the Throne scoring line), `ProcedureLog.scala`
- Test: `IronHandSuite`
- Modify: the note-keys snapshot

**Interfaces:**
- Produces: `IronHand.scored: NoteKey` ("{0} scored {1} with {2} ({3})"),
  under the Throne's note id.

- [ ] **Step 1: Write the failing tests**

```scala
  test("the holder scores the legacy bonus at the sites they rule after " +
      "the winner, with no base and no gifts (ruling 20; R33)"):
    // p3 holds The Needle (Beast) beside Iron Hand (Order); Threatening
    // Roar, a Beast denizen, lies at Buried Giant.
    val run = through(ironThrone.legacy(p3, TheNeedleCard),
      TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val lineage = run.ready.game.current.player(p3).get.lineage
    assertEquals(run.ready.game.campaign.era.score(lineage), 1)
    assert(run.ready.game.current.scoredLegacies.contains(TheNeedleCard.id))
    assert(lines(run, TriggeredProcedureRef.ChronicleThrone).contains(
      s"p3 scored 1 lineage point with ${IronHandCard.id.value} " +
        "(1 from Beast)"))

  test("the holder's scoring skips legacies Chronicler scored (ruling 8)"):
    val table = ironThrone.legacy(p3, TheNeedleCard).update(
      _.updateCurrent(_.copy(scoredLegacies = Set(TheNeedleCard.id))))
    val run = through(table, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    val lineage = run.ready.game.current.player(p3).get.lineage
    assertEquals(run.ready.game.campaign.era.score(lineage), 0)
    assert(!lines(run, TriggeredProcedureRef.ChronicleThrone)
      .exists(_.startsWith("p3 scored")))
```

(The amount's rendering in `NoteText` decides "1 lineage point"; adjust
the expected line to what `NoteText.sentence` renders for an `Amount` of
1, as `LineageScoringSuite` reads its own lines.)

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: p3 scores 0.

- [ ] **Step 3: Implement**

`LineageScoring.steps` appends a `Branch` reading `ironHand(state)`: for
the holder, `unscored` and `bonus` as Chronicler reads them; with any
unscored legacy, a `LegaciesScored` of them (R33); with a positive total,
a `LineagePointsScored` and the `IronHand.scored` line (args `Player`,
`Amount`, `Card(IronHandCard.id)`, `SuitShares`). `IronHand.noteKeys(
ChronicleThrone)` gains the key; `ProcedureLog` gives it `LogKind.Delta`.

- [ ] **Step 4: Run the tests; rewrite the note-keys snapshot**

Run `./sbtw test` and read the result: PASS; the snapshot diff is the one
`iron-hand-scored` key.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): Iron Hand scores its holder's legacies after the winner

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: Iron Hand's sites are stored in place of the Empire's

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleExtraction.scala`
  (`layout`)
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleStarsProcedure.scala`
  (the stored line)
- Modify: `IronHand.scala` (the Stars line), `ProcedureLog.scala`
- Test: `IronHandSuite`, `src/test/scala/distantlands/gameplay/chronicle/ChronicleExtractionSuite.scala`
- Modify: the note-keys snapshot

**Interfaces:**
- Produces: `ChronicleExtraction.layout` splits by the Iron Hand holder
  when there is one, the winner otherwise; `IronHand.stored: NoteKey`
  ("{0} stays on the map with {1}"), under the Stars' note id.

- [ ] **Step 1: Write the failing tests**

```scala
  test("the holder's sites are stored in place of the Empire's, Cradlemost " +
      "first, and the Empire's are archived; the record passes game start " +
      "(ruling 21; R35)"):
    // p1 wins and rules Ancient City; p3, an Exile, rules Broken Peaks and
    // Buried Giant, the most sites, so keeps Iron Hand through the Sun.
    val table = ChronicleWalk.chronicleWonByP1
      .warbandsAt(AncientCitySite, p1, 1)
      .warbandsAt(BrokenPeaksSite, p3, 1).warbandsAt(BuriedGiantSite, p3, 1)
      .legacy(p3, IronHandCard)
    val run = through(table, TriggeredProcedureRef.ChronicleFinish)
    val record = run.ready.game.campaign.evolved.get
    val stored = run.ready.game.current.map.hinterward.filter(site =>
      site == BrokenPeaksSite.id || site == BuriedGiantSite.id)
    assertEquals(record.world.map(_.site), stored)
    assert(record.atlasBox.exists(_.site == AncientCitySite.id))
    assertEquals(distantlands.gameplay.setup.ChronicleAccounting
      .knownAndUnique(catalog, record), Right(()))
    assertEquals(distantlands.gameplay.setup.ChronicleAccounting.conserves(
      run.ready.game.campaign.baseline, record), Right(()))
    assert(distantlands.gameplay.setup.GameStartRules.evolve(catalog, record,
      distantlands.gameplay.setup.LaterGameFixture.orders(record, p1))
      .isRight)
    val said = NoteText.said(ProcedureNotes.id(
      TriggeredProcedureRef.ChronicleStars),
      IronHand.noteKeys(TriggeredProcedureRef.ChronicleStars) ++
        distantlands.gameplay.chronicle.ChronicleStarsProcedure.noteKeys,
      run.events).map(_.text)
    assert(said.contains(s"${AncientCitySite.id.value} was archived"), said)
    assert(said.contains(s"${BuriedGiantSite.id.value} stays on the map " +
      s"with ${IronHandCard.id.value}"), said)
```

In `ChronicleExtractionSuite`, a unit test of `layout` on a table where p3
holds Iron Hand active and is an Exile: `layout.empire` is p3's sites in
`hinterward` order and `rivals` the rest, the Empire's included.

- [ ] **Step 2: Run the tests to verify they fail**

Expected: FAIL: the record's `world` is the Empire's.

- [ ] **Step 3: Implement**

`ChronicleExtraction.layout` partitions by `ChronicleLegacyRules.ironHand(
ready).getOrElse(winner)`; its doc comment says so (ruling 21).
`ChronicleStarsProcedure`'s Store step writes `IronHand.stored` (args
`Site`, `Card(IronHandCard.id)`) for each stored site when `ironHand` is
defined, `keptInEmpire` otherwise. `IronHand.noteKeys(ChronicleStars)`
gains the key; the Stars' `noteKeys` and `ProcedureLog` (`LogKind.Delta`)
declare it.

- [ ] **Step 4: Run the tests; rewrite the note-keys snapshot**

Run `./sbtw test` and read the result: PASS.

- [ ] **Step 5: Commit**

```bash
git commit -m "feat(legacies): Iron Hand's sites are stored in place of the Empire's

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: A golden log of an Iron Hand Chronicle

**Files:**
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`
  (`chronicleIronHand`, `named`)
- Create: `src/test/resources/gamelog/chronicle-iron-hand.actor.log`,
  `chronicle-iron-hand.other.log`

- [ ] **Step 1: Write the script**

```scala
  /** The game of [[chronicleStarted]] with Iron Hand: p3 rules Buried
    * Giant and Deep Woods, holds Iron Hand and The Needle, and Threatening
    * Roar lies at Buried Giant. p3 shapes and builds in the Empire's place,
    * scores after p1, and their sites are stored (Legacy Chronicle powers,
    * slice 4). */
  def chronicleIronHand(using munit.Location): Script =
    val (service, acting) = atTable("chronicle-iron-hand", Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .warbandsAt(BuriedGiantSite, p3, 1).warbandsAt(DeepWoodsSite, p3, 1)
      .denizen(ThreateningRoarCard, at = BuriedGiantSite)
      .legacy(p3, IronHandCard).legacy(p3, TheNeedleCard)
      .turn(p3, Phase.Act).banditsRefilled)
    acting.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script("chronicle-iron-hand", service, p1)
```

registered in `named`.

- [ ] **Step 2: Write and review the goldens**

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`,
then `./sbtw "testOnly distantlands.application.gamelog.*"` and read the
result (the visibility property included). Read both files line by line:
Shape Empire's discards and Build Edifice's question are p3's; "p3 scored
1 lineage point with Iron Hand (1 from Beast)" after p1's line; the Stars
archive Ancient City and keep p3's two sites "on the map with Iron Hand".
No existing golden changes.

- [ ] **Step 3: Commit**

```bash
git commit -m "test(gamelog): a golden Chronicle where Iron Hand acts

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: Docs and the gate

**Files:**
- Modify: `docs/rules/chronicle.md` (the Sun steps 1 and 6, the Throne
  steps 1-3 and 6, the Stars steps 2 and 4), `docs/rules/ambiguities.md`
  (design rulings 18-21 and this slice's implementation rulings),
  `docs/rules/implementation-traceability.md` (Sun, Throne and Stars
  rows), `docs/ROADMAP.md` (the Chronicle item drops Iron Hand; the setup
  item names Iron Hand's SETUP half and, R35, that until it lands the
  stored sites start the next game as the Empire's; the in-play item names
  Iron Hand's discard during a game), the design's status line.

- [ ] **Step 1: Docs**, as slice 3's Task 7.

- [ ] **Step 2: Run the verification gate**, in the background, and read
  every result: `./sbtw clean coverage test coverageReport`,
  `./sbtw "frontend/test"`, `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`, `git status --short
  src/test/resources`. UI check: no new decision kind; reaching an Iron
  Hand Chronicle in the running app needs a finished game holding it.

- [ ] **Step 3: Commit**

```bash
git commit -m "docs(legacies): record Iron Hand's Chronicle half

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
git log -3
```

---

## Self-Review

**Spec coverage** (design, "Slice 4: Iron Hand"):
- The discard checkpoints (ruling 18): Task 1.
- Shape Empire and Build Edifice skipped for the Empire and run for the
  holder (ruling 19), an Imperial Great Architect still building: Task 2.
- The holder's scoring after the winner's, skipping scored legacies
  (ruling 20): Task 3.
- Archive Rival Sites and Store keeping the holder's sites (ruling 21):
  Task 4.
- Iron Hand's Chronicle half leaves holding, its SETUP half kept (R7):
  Task 1.
- Tests the spec lists: an Exile holder taking over both steps; an
  Imperial Great Architect still building beside Iron Hand (Task 2); Iron
  Hand discarded when its holder becomes a Citizen at Offer Citizenship
  (Task 1); the holder's scoring skipping legacies Chronicler scored (Task
  3); the stored Chronicle keeping the holder's sites and archiving the
  Empire's, passing game start's card accounting (Task 4); a golden log of
  the whole Chronicle (Task 5). Resume tests for the holder's removal
  question (Task 2).
- Docs: Task 6.

**Placeholder scan:** every test is written out. Task 1's stand-in swaps
are found by running the suite, and each swap is listed in its commit.
Task 3's expected line depends on how `NoteText` renders an amount of 1,
which the step says to read.

**Type consistency:** `ChronicleLegacyRules.ironHand: ReadyGame =>
Option[PlayerId]`, `shaper` and `builders` returning `Either`;
`IronHand.discard(TriggeredProcedureRef): Operation`,
`IronHand.noteKeys(TriggeredProcedureRef)`; `BuildEdifice.ironHandDecisionId`
in Task 2.
