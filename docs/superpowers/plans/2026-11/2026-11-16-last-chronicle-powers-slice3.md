# Last Chronicle Powers, Slice 3: Blasted Pastures Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Blasted Pastures (E07 ruined) buries every Nomad card on the map
at the start of the Throne unless the Throne holder rules its site. With
it, `src/test/resources/powers/unimplemented.txt` is empty.

**Architecture:** `BlastedPastures` is a `ContributingPower` that appends
a step to `ChronicleStartOfThrone`. Its priority of 2 folds it after
Reformer and Hiding Place. The step is a `Branch` that reads the board
when reached. If E07's ruined face is on the map and the Throne holder
does not rule its site (`ChronicleTasks.rules`), it writes one line, then
one required batch buries every Nomad card at a site with `Bury.standard`.
Two small log fixes make the buried and listed edifices read as the face
they lay on.

**Why the line comes before the batch.** `NoteLines` judges a note's cards
at the states before the note plus the state just after it, and names each
card by its label in the last of them (`LogWords.seen`,
`GamePresentationProjector.cardLabel`). The edifice deck holds bare ids,
so a buried edifice has no side, and `cardLabel` falls back to the intact
face. A line written after the batch would read "Wild Pastures buried
Wild Pastures, …". Written first, every card still lies on the map with
its side. A covering note cannot be used: covering needs the note after
the step it restates (`LogJournal.covered`), so the generic "Buried" detail
lines stay, as Shape Empire keeps its generic discard lines beside its
own.

**Why two log fixes.** Both are needed for this power's lines and both
are pre-existing gaps that no earlier power reached:

- The generic "Buried {card}" detail line names the card by the state
  after its bury (`DetailLines`, `card(id)`), so a buried ruined edifice
  reads as its intact face. It now uses `asItLay`, as the set-aside
  ruined edifice's line already does.
- A card list (more than five cards, catalog batch 2, N5) shows each named
  card through `presentation.cardDetails(id, None, false)`, which has no
  edifice case: an edifice shows a bare id. It now shows an edifice's face
  from its last state on the map, through `edificeCardDetails`.

**Why the priority.** Transforms of one phase fold by
`ContributingPower.sortKey`: priority, then the source's `stableKey`, then
the id. `game:edifice.e07.ruined` sorts before both
`game:edifice.e26.ruined` (Hiding Place, priority 1) and
`game:legacy.reformer` (priority 0). Ruling 6 orders Blasted Pastures
last, so its priority is 2.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Last Chronicle powers design](../../specs/2026-11/2026-11-16-last-chronicle-powers-design.md),
rulings 5 and 6, and section "Slice 3: Blasted Pastures".

## Global Constraints

- Ruling 5: at the start of the Throne, unless the Throne holder rules
  Blasted Pastures' site, every Nomad card at a site is buried: denizens
  and edifices of either face, locked cards and Blasted Pastures itself.
  "Rules" is `ChronicleTasks.rules(state, holder, site)`, so after Crown
  Chancellor the Empire's sites count as the winner's.
- Ruling 5: denizens go to the bottom of the world deck and edifices to
  the edifice deck, through `Bury.standard`. Favor on them returns to the
  Nomad bank; secrets go to the Throne holder, facedown (owner,
  2026-11-16).
- Ruling 5: advisers and relics are not on the map and stay. Rival is not
  asked. Ancient City's Enduring and Hall of Bandits do not keep a card
  from a bury.
- Ruling 5: one line, "Blasted Pastures buried {cards}", every viewer.
  Up to five cards inline; more are a card list. No Nomad card buried
  writes no line.
- Ruling 6: at the start of the Throne the relics are hidden, then
  Reformer, then Hiding Place, then Blasted Pastures.
- No new operation, no new decision, no wire change.
- Mechanics are never inferred from rules text or handler-name fragments
  (`docs/architecture/all-exile-round-endings.md`).
- Docs, comments and commits are plain English prose, lines under 80
  columns in Scala. Commits end with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Stage files by name. Never `git add -A`, `-u`, `.`, `--all`, never
  `git commit -a`, never `git stash`.

## Files

- Create: `src/main/scala/distantlands/gameplay/chronicle/BlastedPastures.scala`.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`.
  `noteKeys` adds `BlastedPastures.noteKeys`; doc comment.
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`.
  Registers `BlastedPastures`.
- Modify: `src/main/scala/distantlands/gameplay/powers/economy/WildPastures.scala`.
  Doc comment: the ruined face is implemented.
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`. Doc
  comment of `ChronicleStartOfThrone`.
- Modify: `src/main/scala/distantlands/application/gamelog/DetailLines.scala`.
  The generic "Buried" line names the card as it lay.
- Modify: `src/main/scala/distantlands/application/gamelog/LogWords.scala`.
  A card list shows an edifice's face.
- Modify: `src/test/resources/powers/unimplemented.txt` (empty),
  `src/test/resources/notes/declared-keys.txt`.
- Create: `src/test/scala/distantlands/gameplay/chronicle/BlastedPasturesSuite.scala`,
  `src/test/resources/gamelog/throne-blasted-pastures.actor.log`,
  `src/test/resources/gamelog/throne-blasted-pastures.other.log` (written
  by the golden suite).
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`,
  `src/test/scala/distantlands/application/gamelog/ChronicleEdificeLogSuite.scala`
  (created by slice 2).
- Modify (docs): `docs/rules/ambiguities.md`, `docs/rules/chronicle.md`,
  `docs/rules/rules-reference.md`, `docs/rules/implementation-traceability.md`,
  `docs/ROADMAP.md`,
  `docs/superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md`.

## Running things

- This plan starts from slice 2. The branch `last-chronicle-slice3` was
  cut from slice 2's plan commits, before slice 2 ran. Before Task 1,
  slice 2 must be merged into 0.2.0-alpha; then rebase:
  `git rebase 0.2.0-alpha`. Only the spec commit and this plan's commit
  should replay. Check that `HidingPlace.scala`, `RelicKeep.scala` and
  `ChronicleEdificeLogSuite.scala` exist before starting.
- The worktree needs `.tooling` and `node_modules` symlinked from the main
  checkout before `./sbtw`. They are already linked.
- One suite: `./sbtw "testOnly distantlands.gameplay.chronicle.BlastedPasturesSuite"`.
- Whole suite: `./sbtw test` takes about 7 minutes. Run it in the
  background.
- Unimplemented snapshot: `UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.UnimplementedPowersSuite"`.
- Declared-keys snapshot: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`.
- Log goldens: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`.
- Gates: `python3 scripts/check-architecture.py` and
  `python3 scripts/check-markdown-links.py`.

---

### Task 1: Blasted Pastures

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/BlastedPastures.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/economy/WildPastures.scala`
- Modify: `src/main/scala/distantlands/model/PowerWindow.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`,
  `src/test/resources/notes/declared-keys.txt`
- Test: `src/test/scala/distantlands/gameplay/chronicle/BlastedPasturesSuite.scala`

**Interfaces:**
- Consumes: `HidingPlace.decisionId(player: PlayerId): String` (slice 2),
  `OfferCitizenship.offerDecisionId`, `ChronicleTasks.rules`,
  `ChronicleTasks.forHolder`, `Bury.standard`.
- Produces: `case object BlastedPastures extends ContributingPower` with
  `id = WildPasturesCard.ruined.power.id`, `priority = 2`,
  `buried: NoteKey` (key `blasted-pastures-buried`, args: the card, then
  `NoteArg.Cards`) and `noteKeys: Vector[NoteKey]`. Task 2 reads its
  lines.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/chronicle/BlastedPasturesSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.{EldersCard, GiantsPathCard}
import distantlands.gameplay.powers.economy.{CupOfPlentyCard,
  HallowedSpringCard, VillageIdiotCard, WildPasturesCard}
import distantlands.gameplay.powers.setup.GreatForgeCard
import distantlands.gameplay.powers.whenplayed.{ThreateningRoarCard,
  TwinBrotherCard}
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.gameplay.walker.LogNoted
import distantlands.model._
import distantlands.testkit.{Outcomes, Table}
import distantlands.testkit.Table.{p1, p2}

/** Blasted Pastures (E07 ruined): "CHRONICLE (START OF THRONE): Bury all
  * Nomad cards on the map unless the winner rules this card." (Last
  * Chronicle powers design, rulings 5 and 6). p1, the Chancellor, won and
  * holds the Throne; Blasted Pastures stands at Broken Peaks. Each test
  * reads the table where Offer Citizenship parks, after the start of the
  * Throne and before Shape Empire discards anything. */
class BlastedPasturesSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  private def pasturesAtBrokenPeaks: Table =
    ChronicleWalk.throneHeldByChancellorP1
      .edifice(WildPasturesCard, EdificeSide.Ruined, at = BrokenPeaksSite)

  /** `table`'s Throne walked to Offer Citizenship's question: the game
    * there and the events that led to it. */
  private def atOffer(table: Table)(using munit.Location)
      : (ReadyGame, Vector[OathEvent]) =
    val walked = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(rules,
      table, TriggeredProcedureRef.ChronicleSun),
      OfferCitizenship.offerDecisionId)
    assertEquals(ChronicleWalk.parked(walked.state).map(_.decisionId),
      Some(OfferCitizenship.offerDecisionId))
    (Outcomes.ready(walked.state), walked.events)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  /** The arguments of each Blasted Pastures line among `events`. */
  private def buriedLines(events: Vector[OathEvent]): Vector[Vector[NoteArg]] =
    events.collect {
      case LogNoted(_, note, _) if note.key == BlastedPastures.buried.name =>
        note.args
    }

  test("Blasted Pastures is implemented"):
    assert(PowerImplementationStatus.implemented(
      WildPasturesCard.ruined.power.id))
    assertEquals(BlastedPastures.id, WildPasturesCard.ruined.power.id)

  test("unless the Throne holder rules it, every Nomad card on the map is " +
      "buried: itself, both edifice faces, locked cards, and cards at the " +
      "holder's own and Enduring sites (ruling 5)"):
    // p2 rules Broken Peaks. The Empire, so p1, rules Ancient City, which
    // is also the Enduring site. Great Forge's intact face is locked.
    val table = pasturesAtBrokenPeaks.warbandsAt(BrokenPeaksSite, p2, 1)
      .warbandsAt(AncientCitySite, p1, 1)
      .edifice(GiantsPathCard, EdificeSide.Ruined, at = AncientCitySite)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .edifice(GreatForgeCard, EdificeSide.Intact, at = BuriedGiantSite)
      .denizen(EldersCard, at = DeepWoodsSite)
      .denizen(VillageIdiotCard, at = DeepWoodsSite)
      .adviser(p2, TwinBrotherCard)
    val (ready, events) = atOffer(table)
    val edifices = Set(WildPasturesCard.id, GiantsPathCard.id,
      GreatForgeCard.id)
    val nomads: Set[CardId] = edifices ++ Set(EldersCard.id)
    nomads.foreach(card => assert(!onMap(ready, card), card))
    val common = ready.game.current.commonCards
    assertEquals(common.worldDeck.lastOption.map(_.value),
      Some(EldersCard.id.value))
    assertEquals(common.edificeDeck.takeRight(3).toSet, edifices)
    assert(onMap(ready, ThreateningRoarCard.id))
    assert(onMap(ready, VillageIdiotCard.id))
    assert(ready.game.current.player(p2).get.advisers
      .exists(_.id == TwinBrotherCard.id))
    val lines = buriedLines(events)
    assertEquals(lines.map(_.head),
      Vector[NoteArg](NoteArg.Card(WildPasturesCard.id)))
    assertEquals(lines.flatMap(_.lift(1)).collect {
      case NoteArg.Cards(ids) => ids.toSet }, Vector(nomads))

  test("favor on a buried card returns to the Nomad bank and its secrets " +
      "go to the Throne holder, facedown (ruling 5)"):
    val table = pasturesAtBrokenPeaks.denizen(EldersCard, at = DeepWoodsSite)
      .tokens(EldersCard, favor = 2, secrets = 1)
    val before = table.ready
    val (ready, _) = atOffer(table)
    assert(!onMap(ready, EldersCard.id))
    assertEquals(ready.banks.favor(Suit.Nomad),
      before.banks.favor(Suit.Nomad) + 2)
    def hidden(game: ReadyGame) =
      game.game.current.player(p1).get.board.faceDownSecrets
    assertEquals(hidden(ready), hidden(before) + 1)

  test("the Throne holder ruling its site keeps every card; after Crown " +
      "Chancellor the Empire's sites are theirs (ruling 5)"):
    // p1 is the Chancellor, so their warband at Broken Peaks is Imperial.
    val table = pasturesAtBrokenPeaks.warbandsAt(BrokenPeaksSite, p1, 1)
      .denizen(EldersCard, at = DeepWoodsSite)
    assert(ChronicleTasks.rules(table.ready, p1, BrokenPeaksSite.id))
    val (ready, events) = atOffer(table)
    assert(onMap(ready, WildPasturesCard.id))
    assert(onMap(ready, EldersCard.id))
    assertEquals(buriedLines(events), Vector.empty)

  test("a site another player, bandits or nobody rules still buries"):
    val withElders = pasturesAtBrokenPeaks
      .denizen(EldersCard, at = DeepWoodsSite)
    Vector(withElders.warbandsAt(BrokenPeaksSite, p2, 1),
      withElders.bandits(BrokenPeaksSite, 1), withElders).foreach { table =>
      val (ready, events) = atOffer(table)
      assert(!onMap(ready, WildPasturesCard.id))
      assert(!onMap(ready, EldersCard.id))
      assertEquals(buriedLines(events).size, 1)
    }

  test("the intact Wild Pastures buries nothing"):
    val table = ChronicleWalk.throneHeldByChancellorP1
      .edifice(WildPasturesCard, EdificeSide.Intact, at = BrokenPeaksSite)
      .denizen(EldersCard, at = DeepWoodsSite)
    val (ready, events) = atOffer(table)
    assert(onMap(ready, WildPasturesCard.id))
    assert(onMap(ready, EldersCard.id))
    assertEquals(buriedLines(events), Vector.empty)

  test("Blasted Pastures buries after Hiding Place asks (ruling 6)"):
    val table = pasturesAtBrokenPeaks
      .edifice(HallowedSpringCard, EdificeSide.Ruined, at = DeepWoodsSite)
      .warbandsAt(DeepWoodsSite, p2, 1).relic(p2, CupOfPlentyCard)
    val parked = ChronicleWalk.walkedTo(rules, ChronicleWalk.started(rules,
      table, TriggeredProcedureRef.ChronicleSun), HidingPlace.decisionId(p2))
    assertEquals(ChronicleWalk.parked(parked.state).map(_.decisionId),
      Some(HidingPlace.decisionId(p2)))
    assert(onMap(Outcomes.ready(parked.state), WildPasturesCard.id))
    val (ready, _) = atOffer(table)
    assert(!onMap(ready, WildPasturesCard.id))

  test("the whole Throne replays with the bury"):
    val table = pasturesAtBrokenPeaks.denizen(EldersCard, at = DeepWoodsSite)
    // `through` replays its events from the table and fails on a refusal.
    val run = ChronicleWalk.through(rules, table,
      TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)
    assert(!onMap(run.ready, WildPasturesCard.id))
    assert(run.ready.game.current.commonCards.worldDeck
      .exists(_.value == EldersCard.id.value))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.BlastedPasturesSuite"`
Expected: compilation fails with "Not found: BlastedPastures".

- [ ] **Step 3: Write the power**

Create `src/main/scala/distantlands/gameplay/chronicle/BlastedPastures.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.actions.ProcedureNotes
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower,
  Transform}
import distantlands.gameplay.powers.economy.WildPasturesCard
import distantlands.model._

/** Blasted Pastures (E07 ruined): "CHRONICLE (START OF THRONE): Bury all
  * Nomad cards on the map unless the winner rules this card." (Last
  * Chronicle powers design, rulings 5 and 6). Its step is at the start of
  * the Throne, after Reformer's and Hiding Place's. It reads the board
  * when reached. While the ruined face is on the map and the Throne holder
  * does not rule its site, by Shape Empire's reading
  * (`ChronicleTasks.rules`), every Nomad card at a site in play is buried:
  * Blasted Pastures itself, locked cards and intact edifices included.
  * Favor on a card returns to the Nomad bank and its secrets go to the
  * Throne holder, facedown (`Bury.standard`). Advisers and relics are not
  * on the map, and a bury is not a discard, so Rival, Enduring and Hall of
  * Bandits keep nothing from it.
  *
  * Its one line, "Blasted Pastures buried {cards}", comes before the
  * burials: the log names a card by the state just after its note, and a
  * buried edifice has no side in the edifice deck. */
case object BlastedPastures extends ContributingPower:
  val id: PowerId = WildPasturesCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  /** After Hiding Place, whose priority is 1 (ruling 6). Transforms of one
    * phase fold by priority, then by source key, and this power's key
    * sorts before Hiding Place's and Reformer's. */
  override def priority: Int = 2
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map(
    PowerWindow.ChronicleStartOfThrone -> Vector(Transform(
      (ctx, ops) => ops :+ step(ctx.catalog))))

  private val noteId = ProcedureNotes.id(TriggeredProcedureRef.ChronicleThrone)
  private val notes = NoteSet(noteId)
  /** "Blasted Pastures buried Elders and Giant's Rampage". */
  val buried: NoteKey = notes.key("blasted-pastures-buried", Vector(
    NotePart.Arg(0), NotePart.Text(" buried "), NotePart.Arg(1)))
  /** The Throne lines this power writes, on the Throne's note id. */
  def noteKeys: Vector[NoteKey] = notes.declared

  def step(catalog: ExecutableCatalog): Operation = Branch((state, _) =>
    val current = state.game.current
    current.edificeSite(WildPasturesCard.id, EdificeSide.Ruined)
      .filter(current.map.inPlay.contains).toVector.flatMap(site =>
        ChronicleTasks.forHolder(state, ChronicleTask.Throne)(holder =>
          if ChronicleTasks.rules(state, holder, site) then Vector.empty
          else burying(burials(catalog, state, holder)))))

  /** The line, then one batch burying every card of `found`. */
  private def burying(found: Vector[(CardId, Vector[CoreOperation])])
      : Vector[Operation] =
    if found.isEmpty then Vector.empty
    else Vector(
      Note(noteId, _ => Some(buried.sourceless(
        NoteArg.Card(WildPasturesCard.id), NoteArg.Cards(found.map(_._1))))),
      BuildOps((_, _) => Right(found.flatMap(_._2)), required = true))

  /** Each Nomad card at a site in play, in map order, with its burial. */
  private def burials(catalog: ExecutableCatalog, state: ReadyGame,
      holder: PlayerId): Vector[(CardId, Vector[CoreOperation])] =
    val current = state.game.current
    current.map.inPlay.flatMap(site =>
      current.map.sites.get(site).toVector.flatMap(_.denizens).flatMap(card =>
        catalog.suitOf(card.id).filter(_ == Suit.Nomad).map(suit =>
          (card.id: CardId) -> Bury.standard(buryable(card),
            PositionedLocation(Location.Site(site)), Some(suit),
            card.tokens.favor, card.tokens.secrets, holder))))

  private def buryable(card: SiteDenizenState): BuryableCard = card match
    case held: DenizenState => BuryableCard.Denizen(held.id)
    case held: EdificeState => BuryableCard.Edifice(held.id)
```

- [ ] **Step 4: Register it and declare its line**

In `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`,
change the import that slice 2 left,

```scala
import distantlands.gameplay.chronicle.{AncientCityEnduring, HallOfBandits,
  HidingPlace, RuinedBaths, StoneBaths}
```

to

```scala
import distantlands.gameplay.chronicle.{AncientCityEnduring,
  BlastedPastures, HallOfBandits, HidingPlace, RuinedBaths, StoneBaths}
```

and insert `BlastedPastures` after `HidingPlace` in the contributions
list, re-wrapping under 80 columns. (The list's order does not order the
folds; `sortKey` does.) In the doc comment, after the sentence slice 2
added, "Hiding Place adds a step at the start of the Throne, after
Reformer's.", add:

```scala
  * Blasted Pastures adds one after Hiding Place's.
```

In `src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala`,
append `++ BlastedPastures.noteKeys` to the end of the `noteKeys`
expression, and in the doc comment replace "then holds Reformer's and
Hiding Place's steps;" with "then holds Reformer's, Hiding Place's and
Blasted Pastures' steps;".

- [ ] **Step 5: Drop the snapshot line and update the comments**

Run: `UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.UnimplementedPowersSuite"`
Then `git diff src/test/resources/powers/unimplemented.txt`.
Expected: the only change is the removed line `edifice.e07.ruined`; the
file is now a single newline. Any other change: stop and report.

In `src/main/scala/distantlands/gameplay/powers/economy/WildPastures.scala`,
replace

```scala
  * The ruined face, Blasted Pastures, is a Chronicle step and is not
  * implemented.
```

with

```scala
  * The ruined face, Blasted Pastures, is a Throne step:
  * `BlastedPastures` in `gameplay/chronicle`.
```

In `src/main/scala/distantlands/model/PowerWindow.scala`, change the
`ChronicleStartOfThrone` doc comment that slice 2 wrote to:

```scala
  /** The start of the Throne, before Offer Citizenship: the held relics
    * are hidden (Last Chronicle powers design, ruling 3), then Reformer's
    * step (Legacy Chronicle powers design, ruling 12), then Hiding
    * Place's and Blasted Pastures' (Last Chronicle powers design, rulings
    * 4 to 6). */
```

- [ ] **Step 6: Rewrite the declared-keys snapshot**

Run: `NOTE_KEYS_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Then `git diff src/test/resources/notes/declared-keys.txt`.
Expected: one added line for `blasted-pastures-buried` under the Throne's
note id. Any other change: stop and report.

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.BlastedPasturesSuite distantlands.gameplay.chronicle.HidingPlaceSuite distantlands.gameplay.chronicle.legacies.ReformerSuite distantlands.gameplay.chronicle.legacies.GreatArchitectSuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.powers.DeclaredNoteKeysSuite"`
Expected: PASS. `GreatArchitectSuite`'s `lonePastures` table puts Blasted
Pastures at Ancient City, which the Empire rules, so nothing is buried
there. If "buries after Hiding Place asks" fails with the bury first, the
priority is not ordering the folds: stop and report rather than
reordering the list.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/BlastedPastures.scala \
  src/main/scala/distantlands/gameplay/chronicle/ChronicleThroneProcedure.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/main/scala/distantlands/gameplay/powers/economy/WildPastures.scala \
  src/main/scala/distantlands/model/PowerWindow.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt \
  src/test/scala/distantlands/gameplay/chronicle/BlastedPasturesSuite.scala
git commit -F - <<'EOF'
feat(chronicle): Blasted Pastures buries the Nomad cards on the map

At the start of the Throne, after Hiding Place, every Nomad card at a site
is buried unless the Throne holder rules Blasted Pastures' site. Its
secrets go to the Throne holder. The line is written before the burials,
while the cards still lie on the map.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 2: Blasted Pastures' lines, and edifices named by their face

**Files:**
- Modify: `src/main/scala/distantlands/application/gamelog/DetailLines.scala`
- Modify: `src/main/scala/distantlands/application/gamelog/LogWords.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/ChronicleEdificeLogSuite.scala`
- Create: `src/test/resources/gamelog/throne-blasted-pastures.actor.log`,
  `src/test/resources/gamelog/throne-blasted-pastures.other.log`

**Interfaces:**
- Consumes: `BlastedPastures` (Task 1).
- Produces: `ChronicleLogScripts.throneBlastedPastures: Script`, named
  `throne-blasted-pastures` and registered for goldens, and
  `ChronicleLogScripts.throneBlastedPasturesListed: Script`, not
  registered.

- [ ] **Step 1: Write the scripts**

In `src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala`,
add after `throneHidingPlace` (slice 2):

```scala
  /** The game of [[chronicleStarted]] with Blasted Pastures at Broken
    * Peaks, which bandits rule, Elders at Deep Woods and Giant's Rampage at
    * Ancient City: at the start of the Throne all three are buried, named
    * inline (Last Chronicle powers, slice 3). */
  def throneBlastedPastures(using munit.Location): Script =
    blastedPastures("throne-blasted-pastures", table => table
      .denizen(EldersCard, at = DeepWoodsSite)
      .edifice(GiantsPathCard, EdificeSide.Ruined, at = AncientCitySite))

  /** The same, with six Nomad cards, so the line lists them as a card
    * list. */
  def throneBlastedPasturesListed(using munit.Location): Script =
    blastedPastures("throne-blasted-pastures-listed", table => table
      .denizen(EldersCard, at = DeepWoodsSite)
      .denizen(GreatHerdCard, at = DeepWoodsSite)
      .edifice(GiantsPathCard, EdificeSide.Ruined, at = AncientCitySite)
      .denizen(RivalKhanCard, at = AncientCitySite)
      .edifice(GreatForgeCard, EdificeSide.Intact, at = BuriedGiantSite))

  /** [[chronicleStarted]]'s table with Blasted Pastures at Broken Peaks and
    * the Nomad cards `nomads` places. */
  private def blastedPastures(name: String, nomads: Table => Table)(
      using munit.Location): Script =
    val (service, acting) = atTable(name, nomads(Table.start
      .pawn(p1, AncientCitySite).pawn(p2, BrokenPeaksSite)
      .pawn(p3, BuriedGiantSite).oathkeeper(Some(p1), TitleSide.Usurper)
      .edifice(WildPasturesCard, EdificeSide.Ruined, at = BrokenPeaksSite))
      .turn(p3, Phase.Act).banditsRefilled)
    acting.after(MenuPlay.start(p3, PhaseTransitionRef.BeginRest))
    Script(name, service, p1)
```

Add the imports this needs, merging into the existing import lines:
`EldersCard` and `GiantsPathCard` from
`distantlands.gameplay.powers.action`, `GreatHerdCard` from
`distantlands.gameplay.powers.whenplayed` (beside `ThreateningRoarCard`),
`RivalKhanCard` from `distantlands.gameplay.powers.campaign`,
`GreatForgeCard` from `distantlands.gameplay.powers.setup`, and
`WildPasturesCard` to the existing `distantlands.gameplay.powers.economy`
import. Add `"throne-blasted-pastures" -> (() => throneBlastedPastures),`
to `named` after `"throne-hiding-place"`. Do not register the listed
script: its line is checked by the suite, and one golden covers the
power.

If either script stops before the Chronicle because one of these cards
asks something during p3's Rest or p1's Wake, replace that card with
another site-capable Nomad denizen with no Rest or Wake power and report
the swap.

- [ ] **Step 2: Write the failing tests**

In `src/test/scala/distantlands/application/gamelog/ChronicleEdificeLogSuite.scala`,
add these imports beside the ones slice 2 wrote:

```scala
import distantlands.gameplay.powers.action.{EldersCard, GiantsPathCard}
import distantlands.gameplay.powers.campaign.RivalKhanCard
import distantlands.gameplay.powers.economy.WildPasturesCard
import distantlands.gameplay.powers.setup.GreatForgeCard
import distantlands.gameplay.powers.whenplayed.GreatHerdCard
import distantlands.protocol.projection.LogSpan
```

(merge `WildPasturesCard` into the existing `economy` import), and add
these tests:

```scala
  test("Blasted Pastures names what it buried inline, each by the face it " +
      "lay on, to every viewer (ruling 5)"):
    val script = ChronicleLogScripts.throneBlastedPastures
    val name = WildPasturesCard.ruined.name
    Vector(Some(p1), Some(p2), None).foreach { viewer =>
      val lines = format(script, viewer).map(text)
      val buried = lines.filter(_.startsWith(s"$name buried "))
      assertEquals(buried.size, 1, lines)
      Vector(name, EldersCard.name, GiantsPathCard.ruined.name).foreach(card =>
        assert(buried.head.contains(card), buried.head))
      // The generic detail line names the edifice as it lay, too.
      assert(lines.contains(s"Buried $name"), lines)
      Vector(WildPasturesCard.intact.name, GiantsPathCard.intact.name)
        .foreach(intact => assert(!lines.exists(_.contains(intact)), lines))
    }

  test("Blasted Pastures lists six cards with their faces (ruling 5)"):
    val script = ChronicleLogScripts.throneBlastedPasturesListed
    val name = WildPasturesCard.ruined.name
    val entry = format(script, Some(p2))
      .find(entry => text(entry).startsWith(s"$name buried ")).get
    assertEquals(text(entry), s"$name buried 6 cards")
    val faces = entry.spans.collect { case LogSpan.Cards(cards) => cards }
      .flatten
    assertEquals(faces.map(_.name).toSet, Set(name, EldersCard.name,
      GreatHerdCard.name, GiantsPathCard.ruined.name, RivalKhanCard.name,
      GreatForgeCard.intact.name))
    assert(faces.forall(!_.hidden), faces)
```

- [ ] **Step 3: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.application.gamelog.ChronicleEdificeLogSuite"`
Expected: the inline test fails on a "Buried Wild Pastures" line (the
generic line names the buried edifice by its intact face), and the
listed test fails on the faces' names (an edifice shows a bare id). The
Hiding Place test stays green.

- [ ] **Step 4: Name a buried card as it lay**

In `src/main/scala/distantlands/application/gamelog/DetailLines.scala`,
replace

```scala
      case Bury(buried, _, _) =>
        line(LogSpan.Text("Buried ") +: words.one(card(buried.id)))
```

with

```scala
      // A buried card is named as it lay: an edifice in the deck has no
      // side, and a ruined one would read as its intact face.
      case Bury(buried, _, _) =>
        line(LogSpan.Text("Buried ") +: words.one(asItLay(buried.id)))
```

- [ ] **Step 5: Show an edifice's face in a card list**

In `src/main/scala/distantlands/application/gamelog/LogWords.scala`, in
`listed`, pass the states to `face`:

```scala
    else Vector(LogSpan.Cards(ids.zip(words).map((id, word) =>
      face(id, word, states))))
```

and replace `face` with:

```scala
  /** A card as a card list shows it: its face when named, else its back. A
    * named edifice shows the side it lies on in the last of `states` that
    * holds it on the map. */
  private def face(id: CardId, word: CardWord, states: Vector[ReadyGame])
      : distantlands.protocol.projection.CardDetailsProjection = word match
    case CardWord.Named(_) => id match
      case edifice: EdificeId => states.reverseIterator.flatMap(state =>
          state.game.cardIndex.toOption.flatMap(_.stateOf(edifice)))
        .collectFirst { case lying: EdificeState =>
          presentation.edificeCardDetails(lying) }
        .getOrElse(presentation.cardDetails(id, None, hidden = false))
      case other => presentation.cardDetails(other, None, hidden = false)
    case CardWord.Back(_) => presentation.hiddenCard(id match
      case _: RelicId => "relic"
      case other => presentation.cardKind(other))
```

Keep the existing `CardWord.Back` branch exactly as it was if it differs
from the one shown. `edificeCardDetails` is `private[application]`, which
`gamelog` reaches.

- [ ] **Step 6: Run the log tests and write the goldens**

Run: `./sbtw "testOnly distantlands.application.gamelog.ChronicleEdificeLogSuite"`
Expected: PASS.

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
Then `git status --short src/test/resources/gamelog/`.
Expected: only the two new `throne-blasted-pastures` files. Read both:
"Blasted Pastures buried …" names the three cards, followed by the three
"Buried" lines, and no line names "Wild Pastures" or "Giant's Path". If
an existing golden changed, it must be a "Buried" line that now names a
ruined edifice by its ruined face: keep it and report it. Any other
change: stop and report.

- [ ] **Step 7: Run the log suites**

Run: `./sbtw "testOnly distantlands.application.gamelog.*"`
Expected: PASS, the properties suite included.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/application/gamelog/DetailLines.scala \
  src/main/scala/distantlands/application/gamelog/LogWords.scala \
  src/test/scala/distantlands/application/gamelog/ChronicleLogScripts.scala \
  src/test/scala/distantlands/application/gamelog/ChronicleEdificeLogSuite.scala \
  src/test/resources/gamelog/throne-blasted-pastures.actor.log \
  src/test/resources/gamelog/throne-blasted-pastures.other.log
git commit -F - <<'EOF'
feat(gamelog): name buried and listed edifices by their face

The generic "Buried" line names a card as it lay, and a card list shows
an edifice's face from its last state on the map. Blasted Pastures' line
and golden.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 3: Docs and the full gate

**Files:**
- Modify: `docs/rules/ambiguities.md`, `docs/rules/chronicle.md`,
  `docs/rules/rules-reference.md`, `docs/rules/implementation-traceability.md`,
  `docs/ROADMAP.md`,
  `docs/superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md`

Every replacement below quotes the text slice 2 left. If a quoted text is
not found, read the surrounding lines, make the equivalent edit and
report it.

- [ ] **Step 1: The rulings**

In `docs/rules/ambiguities.md`, append to the "## Last Chronicle powers
rulings" section, after item 4:

```markdown
5. **Blasted Pastures.** At the start of the Throne, unless the Throne holder rules its site, every Nomad card at a site is buried: denizens and edifices of either face, locked cards and Blasted Pastures itself. "Rules" is Shape Empire's reading, so after Crown Chancellor the Empire's sites count as the winner's. Denizens go to the bottom of the world deck and edifices to the edifice deck. Favor on them returns to the Nomad bank, and secrets go to the Throne holder, facedown (owner, 2026-11-16). Advisers and relics are not on the map and stay. A bury is not a discard, so Rival is not asked, and neither Enduring nor Hall of Bandits keeps a card. One line, "Blasted Pastures buried Elders and Giant's Rampage", or a card list for more than five cards; with no Nomad card buried, no line.
6. **Start of Throne order.** The relics are hidden, then Reformer acts, then Hiding Place, then Blasted Pastures. Reformer moves cards only among sites the winner rules, so it cannot change who rules Hiding Place or Blasted Pastures, and the order changes no outcome.
```

In item 1 of "Component-data dependencies", replace "One printed power
still has no executable behavior. It is listed in
`src/test/resources/powers/unimplemented.txt`: the ruined face Blasted
Pastures (E07)." with "Every printed power has executable behavior, so
`src/test/resources/powers/unimplemented.txt` is empty." In item 5,
replace "but banner ribbons and the power item 1 lists still require
typed executable handlers" with "but banner ribbons still require typed
executable handlers".

- [ ] **Step 2: The Chronicle rules**

In `docs/rules/chronicle.md`, section "### 2. The Throne", after the
Hiding Place bullet slice 2 added, insert:

```markdown
- Digital: then, while Blasted Pastures (E07 ruined) is on the map and the Throne holder does not rule its site (after Crown Chancellor the Empire's sites are theirs), every Nomad card at a site is buried, Blasted Pastures, locked cards and intact edifices included: denizens to the bottom of the world deck, edifices to the edifice deck. Favor on them returns to the Nomad bank and secrets go to the Throne holder. Advisers and relics stay. One line names the cards, "Blasted Pastures buried Elders and Giant's Rampage" ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), items 5 and 6).
```

- [ ] **Step 3: The rules reference and traceability**

In `docs/rules/rules-reference.md`, after the **Hiding Place.** bullet,
add:

```markdown
- **Blasted Pastures.** CHRONICLE (START OF THRONE): bury all Nomad cards on the map unless the winner rules this card. Digital: after Reformer and Hiding Place; the winner rules it when the Throne holder's side rules its site, the Empire's after Crown Chancellor; edifices of either face and locked cards are buried, and their secrets go to the Throne holder ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), items 5 and 6).
```

In `docs/rules/implementation-traceability.md`, in the Throne task row,
replace "The held relics turn facedown at the start of the Throne;" with:

```markdown
The held relics turn facedown at the start of the Throne, and Blasted Pastures buries the Nomad cards on the map unless the winner rules it ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), items 5 and 6; `BlastedPasturesSuite`, the `throne-blasted-pastures` golden);
```

- [ ] **Step 4: The roadmap and the spec status**

In `docs/ROADMAP.md`, delete the whole **The last Chronicle powers.**
item, from its first line,

```markdown
- [ ] **The last Chronicle powers.** One printed power still does
```

through its last line, "the stack**." The next item, **Eras.**, follows
the item before it directly.

In the **Retire the unimplemented-card machinery** item, replace

```markdown
One power remains in
  `src/test/resources/powers/unimplemented.txt` (**The last Chronicle
  powers**, under Chronicle steps). Once the snapshot is empty, remove the machinery that sorts, filters and marks
  unimplemented cards:
```

with

```markdown
The
  [last Chronicle powers](superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md)
  emptied `src/test/resources/powers/unimplemented.txt`. Remove the
  machinery that sorts, filters and marks unimplemented cards (slice 4):
```

In the spec, change "Slice 2 landed 2026-11-16." to "Slice 2 landed
2026-11-16. Slice 3 landed 2026-11-16." Use the merge date instead if
the slice merges later.

- [ ] **Step 5: The full gate**

Run in the background: `./sbtw test`.
Expected: every test passes.
Run: `python3 scripts/check-architecture.py` and
`python3 scripts/check-markdown-links.py`.
Expected: both pass, with no missing anchor.

- [ ] **Step 6: Commit**

```bash
git add docs/rules/ambiguities.md docs/rules/chronicle.md \
  docs/rules/rules-reference.md docs/rules/implementation-traceability.md \
  docs/ROADMAP.md \
  docs/superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md
git commit -F - <<'EOF'
docs(chronicle): record Blasted Pastures and close slice 3

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

## Self-review

- Spec coverage:
  - ruling 5, the Throne holder's rule by `ChronicleTasks.rules`, the
    Empire's sites after Crown Chancellor, another player, bandits and
    nobody: Task 1;
  - ruling 5, what is buried (denizens, both edifice faces, locked cards,
    itself, cards at the holder's own and Enduring sites) and what stays
    (other suits, advisers): Task 1;
  - ruling 5, favor to the Nomad bank and secrets to the Throne holder:
    Task 1;
  - ruling 5, the line inline and as a card list, every viewer, no line
    with nothing buried: Tasks 1 and 2;
  - ruling 6, Blasted Pastures after Hiding Place: Task 1 (priority and
    its test); Reformer before Hiding Place is slice 2's test;
  - the snapshot line, the declared-keys rewrite, the replay round trip
    (`ChronicleWalk.through` replays its events), and the docs the spec
    lists for this slice: Tasks 1 and 3. The power adds no decision, so
    no resumed-walk test is needed.
- Not covered here: relics at sites have no suit and are never Nomad;
  visions are not site cards. Slice 4 retires the machinery the empty
  snapshot leaves idle.
- Names, used the same way in every task:
  - `BlastedPastures.buried`, `BlastedPastures.noteKeys`,
    `BlastedPastures.priority`;
  - `ChronicleLogScripts.throneBlastedPastures` and
    `ChronicleLogScripts.throneBlastedPasturesListed`.
