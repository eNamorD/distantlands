# Last Chronicle Powers, Slice 1: Shape Empire Protections Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ancient City's Enduring power becomes a registered power that
Shape Empire reads by its id, and Hall of Bandits (E16 ruined) keeps
ruined edifices at bandit sites out of Shape Empire's discard.

**Architecture:** Both are rules Shape Empire reads, registered as walker
powers that hook no window, as the Baths are, so each card counts as
implemented. A new `ShapeEmpireExemptions` object holds the two powers and
the two checks. `ShapeEmpire.discards` calls the checks before it builds
its list of cards, so Iron Hand's Shape Empire (the same `discards`) and
Rival's question (whose options are that list) inherit both. The
handler-name suffix check `.endsWith(".enduring")` goes.

**Why one file for both.** The two powers share one job, keeping cards out
of one discard, and each is a few lines. The card objects stay where they
are: `AncientCitySite` in `gameplay/cards/PoweredSites.scala`,
`HallOfMinistersCard` in `gameplay/operations/HallOfMinisters.scala`.

**Why Revolutionary needs nothing.** Revolutionary calls
`ShapeEmpire.discardOf`, not `discards`, so the exemptions never reach it
(ruling 1). `RevolutionarySuite`'s "the holder is offered the denizens and
ruined edifices at Imperial sites, after Build Edifice" already offers
Threatening Roar and School of Vines at Ancient City; it must stay green.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Last Chronicle powers design](../../specs/2026-11/2026-11-16-last-chronicle-powers-design.md),
rulings 1 and 2, and section "Slice 1: Shape Empire protections".

## Global Constraints

- Ancient City (ruling 1): no card at its site is discarded in Shape
  Empire, whoever performs the step, Iron Hand's Exile included.
  Revolutionary still discards there. The behavior is today's; only its
  source changes, from a handler-name suffix to a registered power.
- Hall of Bandits (ruling 2): while its ruined face is on the map, a ruined
  edifice at a site bandits rule is not discarded in the Chronicle. It
  keeps itself when bandits rule its site. Denizens and intact edifices at
  bandit sites are handled as before. It writes no line.
- "Bandits rule" is `SiteRule.rulingPlayer(ready, site).contains(SiteRuler.Bandits)`,
  the reading `BanditCrownRule.banditSites` uses.
- No new operation, decision, note key or wire change.
- Mechanics are never inferred from rules text or handler-name fragments
  (`docs/architecture/all-exile-round-endings.md`).
- Docs, comments and commits are plain English prose. Commits end with
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- Stage files by name. Never `git add -A`, `-u`, `.`, `--all`, never
  `git commit -a`, never `git stash`.

## Files

- Create: `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptions.scala`.
  `AncientCityEnduring`, `HallOfBandits`, and `ShapeEmpireExemptions`
  with `enduring` and `banditRuin`.
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpire.scala`.
  `discards` reads the two checks; the private `enduring` goes.
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`.
  Registers both powers and names them in its doc comment.
- Modify: `src/test/resources/powers/unimplemented.txt`. Drops two lines.
- Create: `src/test/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptionsSuite.scala`.
- Modify docs: `docs/rules/ambiguities.md`, `docs/rules/chronicle.md`,
  `docs/rules/rules-reference.md`, `docs/rules/implementation-traceability.md`,
  `docs/ROADMAP.md`, the spec's status line.

## Running things

- Work in `.worktrees/last-chronicle-slice1` on branch
  `last-chronicle-slice1`, which this plan's commit starts. Its `.tooling`
  and `node_modules` are symlinks to the main checkout's; recreate them if
  missing, before `./sbtw`.
- One suite: `./sbtw "testOnly distantlands.gameplay.chronicle.ShapeEmpireExemptionsSuite"`.
- Several: `./sbtw "testOnly distantlands.gameplay.chronicle.ShapeEmpireSuite distantlands.gameplay.chronicle.legacies.RivalSuite distantlands.gameplay.chronicle.legacies.RevolutionarySuite distantlands.gameplay.chronicle.legacies.IronHandSuite distantlands.gameplay.powers.UnimplementedPowersSuite distantlands.gameplay.cards.CardCatalogSuite"`.
- Every test: `./sbtw test` (about 7 minutes; run it in the background).
- Architecture: `python3 scripts/check-architecture.py`.
- Links: `python3 scripts/check-markdown-links.py`.

---

### Task 1: Ancient City's Enduring as a registered power

**Files:**
- Create: `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptions.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpire.scala` (`enduring`, `discards`)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala` (imports, the edifice list, the doc comment)
- Modify: `src/test/resources/powers/unimplemented.txt`
- Test: `src/test/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptionsSuite.scala`

**Interfaces:**
- Produces: `case object AncientCityEnduring extends ContributingPower` and
  `ShapeEmpireExemptions.enduring(catalog: ExecutableCatalog, site: SiteId): Boolean`,
  both in package `distantlands.gameplay.chronicle`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptionsSuite.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.gameplay.{OathRules, WalkerDiceFixture}
import distantlands.gameplay.cards.{AncientCitySite, BrokenPeaksSite,
  BuriedGiantSite, DeepWoodsSite}
import distantlands.gameplay.chronicle.legacies.IronHandCard
import distantlands.gameplay.powers.{PowerImplementationStatus,
  WalkerPowerCatalog}
import distantlands.gameplay.powers.action.FaeMerchantCard
import distantlands.gameplay.powers.whenplayed.ThreateningRoarCard
import distantlands.gameplay.setup.FirstGameSetupFixture.catalog
import distantlands.model._
import distantlands.testkit.Table
import distantlands.testkit.Table.{p1, p3}

/** The cards Shape Empire keeps on the map besides the Throne holder's:
  * Ancient City's (Enduring) and, while Hall of Bandits is on the map, the
  * ruined edifices at bandit sites (Last Chronicle powers design, rulings 1
  * and 2). p1 is the Chancellor holding the Throne; the Empire rules the
  * sites each test gives an Imperial warband. */
class ShapeEmpireExemptionsSuite extends munit.FunSuite:
  private val rules = new OathRules(catalog,
    walkerPowerCatalog = WalkerPowerCatalog.default(catalog),
    walkerDice = WalkerDiceFixture.reversingShuffle)

  /** The Throne walked to its end on `table`, every choice answered
    * plainly. */
  private def throne(table: Table)(using munit.Location): ChronicleWalk.Run =
    ChronicleWalk.through(rules, table, TriggeredProcedureRef.ChronicleThrone,
      after = TriggeredProcedureRef.ChronicleSun)

  private def onMap(ready: ReadyGame, card: CardId): Boolean =
    ready.game.current.map.sites.values.exists(_.denizens.exists(_.id == card))

  test("Ancient City's Enduring is implemented"):
    assert(PowerImplementationStatus.implemented(AncientCitySite.enduring))
    assertEquals(AncientCityEnduring.id, AncientCitySite.enduring)

  test("a card at Ancient City stays when Iron Hand's Exile shapes the " +
      "Empire in the Chancellor's place (ruling 1)"):
    // p3 holds Iron Hand and rules Broken Peaks; the Empire rules Deep
    // Woods. Ancient City is nobody's, so only Enduring keeps its card.
    val table = ChronicleWalk.throneHeldByChancellorP1
      .warbandsAt(DeepWoodsSite, p1, 1).warbandsAt(BrokenPeaksSite, p3, 1)
      .denizen(ThreateningRoarCard, at = AncientCitySite)
      .denizen(FaeMerchantCard, at = BuriedGiantSite)
      .legacy(p3, IronHandCard)
    val run = throne(table)
    assert(onMap(run.ready, ThreateningRoarCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ShapeEmpireExemptionsSuite"`
Expected: compilation fails with "Not found: AncientCityEnduring".

- [ ] **Step 3: Write the power and the check**

Create `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptions.scala`:

```scala
package distantlands.gameplay.chronicle

import distantlands.catalog.ExecutableCatalog
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powerresolver.{Contribution, ContributingPower}
import distantlands.model._

/** The cards Shape Empire keeps on the map although the Throne holder does
  * not rule their site (Last Chronicle powers design, rulings 1 and 2).
  * Shape Empire reads these checks before it lists its discards, so Iron
  * Hand's Shape Empire and Rival's question see the same list.
  * Revolutionary's discard comes after Build Edifice and reads neither. */
object ShapeEmpireExemptions:
  /** Cards at an Enduring site are not discarded in Shape Empire (CR
    * p. 37). Ancient City is the only one: its catalog handlers carry
    * [[AncientCityEnduring]]. */
  def enduring(catalog: ExecutableCatalog, site: SiteId): Boolean =
    catalog.site(site).exists(_.handlers.contains(AncientCityEnduring.id))

/** Ancient City's Enduring: Shape Empire reads it
  * (`ShapeEmpireExemptions.enduring`). It hooks no window; it is
  * registered so the power counts as implemented, as the Baths are. */
case object AncientCityEnduring extends ContributingPower:
  val id: PowerId = AncientCitySite.enduring
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

- [ ] **Step 4: Make Shape Empire read it**

In `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpire.scala`,
delete the private `enduring` and its doc comment:

```scala
  /** Cards at Enduring sites are not discarded in Shape Empire (CR p. 37).
    * Ancient City is one; its handler names the keyword. */
  private def enduring(catalog: ExecutableCatalog, site: SiteId): Boolean =
    catalog.site(site).exists(_.handlers.exists(_.value.endsWith(".enduring")))

```

and in `discards` replace

```scala
      ChronicleTasks.rules(state, holder, site) || enduring(catalog, site))
```

with

```scala
      ChronicleTasks.rules(state, holder, site) ||
        ShapeEmpireExemptions.enduring(catalog, site))
```

- [ ] **Step 5: Register the power**

In `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`,
change the import

```scala
import distantlands.gameplay.chronicle.{RuinedBaths, StoneBaths}
```

to

```scala
import distantlands.gameplay.chronicle.{AncientCityEnduring, RuinedBaths,
  StoneBaths}
```

In the contributions list, insert `AncientCityEnduring` after `RuinedBaths`,
re-wrapping the lines to stay under 80 columns:

```scala
        ProvingGrounds, EmptyGrounds, LostDoors, AmberDoors, StoneBaths,
        RuinedBaths, AncientCityEnduring, RuinedTemple, HallOfMockery,
        LostGuardians, RuinedSchool,
```

(Keep the rest of that list unchanged. If the next existing line already
begins with `BrokenHorn`, the result reads `LostGuardians, RuinedSchool,`
then `BrokenHorn, ...` as before.)

In the doc comment, replace

```scala
  * reads it. The Baths hook none either, as Sun steps 2 and 3 read them.
```

with

```scala
  * reads it. The Baths hook none either, as Sun steps 2 and 3 read them,
  * nor does Ancient City's Enduring, which Shape Empire reads.
```

- [ ] **Step 6: Drop the snapshot line**

In `src/test/resources/powers/unimplemented.txt`, delete the line
`site.ancient-city.enduring`.

- [ ] **Step 7: Run the tests to verify they pass**

Run the "Several" command from Running things.
Expected: all PASS, `ShapeEmpireSuite`'s "cards at an Enduring site stay"
included, and `CardCatalogSuite` accepts the registered site power. If
`CardCatalogSuite` reports `site.ancient-city.enduring` as registered but
not printed, stop and report: site handlers are expected to count as
printed, as the River powers' do.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptions.scala \
  src/main/scala/distantlands/gameplay/chronicle/ShapeEmpire.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptionsSuite.scala
git commit -F - <<'EOF'
feat(chronicle): register Ancient City's Enduring and read it by id

Shape Empire found Enduring sites by a handler name ending in
".enduring". It now reads a registered power, AncientCityEnduring, so the
card counts as implemented and no mechanic hangs on a name fragment.

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

---

### Task 2: Hall of Bandits

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptions.scala`
- Modify: `src/main/scala/distantlands/gameplay/chronicle/ShapeEmpire.scala` (`discards`, the object's doc comment)
- Modify: `src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`
- Test: `src/test/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptionsSuite.scala`

**Interfaces:**
- Consumes: `ShapeEmpireExemptions` (Task 1).
- Produces: `case object HallOfBandits extends ContributingPower` and
  `ShapeEmpireExemptions.banditRuin(ready: ReadyGame, site: SiteId, card: SiteDenizenState): Boolean`.

- [ ] **Step 1: Write the failing tests**

Add these imports to `ShapeEmpireExemptionsSuite.scala` (merge them into the
existing import lines, keeping each group sorted as the file has them):

```scala
import distantlands.gameplay.actions.challenge.SchoolOfVinesCard
import distantlands.gameplay.chronicle.legacies.{IronHandCard, Rival,
  RivalCard}
import distantlands.gameplay.operations.HallOfMinistersCard
```

Append to the suite:

```scala
  private def empireAt(sites: distantlands.catalog.Site*): Table =
    sites.foldLeft(ChronicleWalk.throneHeldByChancellorP1)((table, site) =>
      table.warbandsAt(site, p1, 1))

  test("Hall of Bandits is implemented"):
    assert(PowerImplementationStatus.implemented(
      HallOfMinistersCard.ruined.power.id))
    assertEquals(HallOfBandits.id, HallOfMinistersCard.ruined.power.id)

  test("with Hall of Bandits on the map, the ruined edifices at bandit " +
      "sites stay, the Hall included (ruling 2)"):
    val table = empireAt(BrokenPeaksSite)
      .bandits(BuriedGiantSite, 2).bandits(DeepWoodsSite, 1)
      .edifice(HallOfMinistersCard, EdificeSide.Ruined, at = BuriedGiantSite)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = DeepWoodsSite)
    val run = throne(table)
    assert(onMap(run.ready, HallOfMinistersCard.id))
    assert(onMap(run.ready, SchoolOfVinesCard.id))

  test("Hall of Bandits keeps no ruin at a site bandits do not rule, nor " +
      "a denizen at a bandit site"):
    // Deep Woods has no warbands, so nobody rules it.
    val table = empireAt(BrokenPeaksSite)
      .bandits(BuriedGiantSite, 2)
      .edifice(HallOfMinistersCard, EdificeSide.Ruined, at = BuriedGiantSite)
      .denizen(FaeMerchantCard, at = BuriedGiantSite)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = DeepWoodsSite)
    val run = throne(table)
    assert(onMap(run.ready, HallOfMinistersCard.id))
    assert(!onMap(run.ready, FaeMerchantCard.id))
    assert(!onMap(run.ready, SchoolOfVinesCard.id))

  test("the Hall of Ministers, its intact face, keeps no ruin at a bandit " +
      "site"):
    val table = empireAt(BrokenPeaksSite)
      .edifice(HallOfMinistersCard, EdificeSide.Intact, at = BrokenPeaksSite)
      .bandits(BuriedGiantSite, 2)
      .edifice(SchoolOfVinesCard, EdificeSide.Ruined, at = BuriedGiantSite)
    val run = throne(table)
    assert(onMap(run.ready, HallOfMinistersCard.id))
    assert(!onMap(run.ready, SchoolOfVinesCard.id))

  test("a ruin Hall of Bandits keeps is not among Rival's options"):
    val table = empireAt(BrokenPeaksSite)
      .bandits(BuriedGiantSite, 2)
      .edifice(HallOfMinistersCard, EdificeSide.Ruined, at = BuriedGiantSite)
      .denizen(FaeMerchantCard, at = BuriedGiantSite)
      .legacy(p3, RivalCard)
    val run = throne(table)
    val asked = run.asked.find(_.decisionId == Rival.decisionId(p3)).get
    assertEquals(asked.query.asInstanceOf[DecisionQuery.ChooseMany].options
      .map(_.ref), Vector[DecisionOptionRef](
        DecisionOptionRef.Denizen(FaeMerchantCard.id)))
```

If `Table.bandits` or `Table.edifice` takes a site object of a different
type than `BuriedGiantSite`, follow `ShapeEmpireSuite`, which calls
`.bandits(AncientCitySite, 2)` and
`.edifice(GreatMarketCard, EdificeSide.Intact, at = BrokenPeaksSite)`.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.chronicle.ShapeEmpireExemptionsSuite"`
Expected: compilation fails with "Not found: HallOfBandits".

- [ ] **Step 3: Write the power and the check**

In `ShapeEmpireExemptions.scala`, add the import

```scala
import distantlands.gameplay.operations.HallOfMinistersCard
```

add to `object ShapeEmpireExemptions`:

```scala
  /** A ruined edifice at a site bandits rule, while Hall of Bandits (E16
    * ruined) is on the map, is not discarded in the Chronicle (ruling 2).
    * Shape Empire is the only Chronicle discard that reaches a bandit
    * site. The Hall keeps itself when bandits rule its site. */
  def banditRuin(ready: ReadyGame, site: SiteId, card: SiteDenizenState)
      : Boolean = card match
    case EdificeState(_, EdificeSide.Ruined, _) =>
      ready.game.current.edificeSite(HallOfMinistersCard.id,
        EdificeSide.Ruined).nonEmpty &&
        SiteRule.rulingPlayer(ready, site).contains(SiteRuler.Bandits)
    case _ => false
```

and after `AncientCityEnduring`:

```scala
/** Hall of Bandits (E16 ruined): Shape Empire reads it
  * (`ShapeEmpireExemptions.banditRuin`). It hooks no window; it is
  * registered so the face counts as implemented. */
case object HallOfBandits extends ContributingPower:
  val id: PowerId = HallOfMinistersCard.ruined.power.id
  def source: RuleSourceRef = RuleSourceRef.GameRule(id.value)
  def contributions: Map[PowerWindow, Vector[Contribution]] = Map.empty
```

Update the object's doc comment to name both rulings' cards in its first
sentence, as written in Task 1 (it already does).

- [ ] **Step 4: Make Shape Empire read it**

In `ShapeEmpire.discards`, replace

```scala
        current.map.sites(site).denizens.flatMap(card =>
          discardOf(catalog, site, region, card, holder)
            .map(op => (site, card, op)))))
```

with

```scala
        current.map.sites(site).denizens
          .filterNot(ShapeEmpireExemptions.banditRuin(state, site, _))
          .flatMap(card => discardOf(catalog, site, region, card, holder)
            .map(op => (site, card, op)))))
```

In the `object ShapeEmpire` doc comment, replace

```scala
  * is discarded, except intact edifices and cards at Enduring sites. Iron
```

with

```scala
  * is discarded, except intact edifices, cards at Enduring sites and,
  * while Hall of Bandits is on the map, ruined edifices at bandit sites
  * (`ShapeEmpireExemptions`). Iron
```

and in the `discards` doc comment replace "The cards at sites the holder
does not rule, but Enduring ones, are" with "The cards at sites the holder
does not rule, but those `ShapeEmpireExemptions` keeps, are".

- [ ] **Step 5: Register the power**

In `WalkerPowerCatalog.scala`, change the import to

```scala
import distantlands.gameplay.chronicle.{AncientCityEnduring, HallOfBandits,
  RuinedBaths, StoneBaths}
```

insert `HallOfBandits` after `AncientCityEnduring` in the contributions
list (re-wrap under 80 columns), and change the doc comment sentence from
Task 1 to

```scala
  * reads it. The Baths hook none either, as Sun steps 2 and 3 read them,
  * nor do Ancient City's Enduring and Hall of Bandits, which Shape Empire
  * reads.
```

- [ ] **Step 6: Drop the snapshot line**

In `src/test/resources/powers/unimplemented.txt`, delete the line
`edifice.e16.ruined`.

- [ ] **Step 7: Run the tests to verify they pass**

Run the "Several" command from Running things.
Expected: all PASS. `RivalSuite` and `IronHandSuite` place no ruined
Hall of Bandits, so they are unchanged; a failure there means the filter
reached a card it should not.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptions.scala \
  src/main/scala/distantlands/gameplay/chronicle/ShapeEmpire.scala \
  src/main/scala/distantlands/gameplay/powers/WalkerPowerCatalog.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/scala/distantlands/gameplay/chronicle/ShapeEmpireExemptionsSuite.scala
git commit -F - <<'EOF'
feat(chronicle): Hall of Bandits keeps ruins at bandit sites

While Hall of Bandits is on the map, Shape Empire does not discard a
ruined edifice at a site bandits rule, the Hall included. Iron Hand's
Shape Empire and Rival's options read the same list.

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

- [ ] **Step 1: The rulings**

In `docs/rules/ambiguities.md`, insert before `## Source relationship cautions`:

```markdown
## Last Chronicle powers rulings

Rulings for the last four printed powers ([Last Chronicle powers design](../superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md)), taken with the owner on 2026-11-16. Items are the design's rulings; each slice adds its own.

1. **Ancient City is Enduring.** No card at its site is discarded in Shape Empire, whoever performs the step, Iron Hand's Exile included. The protection is Shape Empire's only: Revolutionary's discard comes after Build Edifice and still discards cards at Ancient City.
2. **Hall of Bandits.** While its ruined face is on the map, a ruined edifice at a site bandits rule is not discarded in the Chronicle. Shape Empire is the only Chronicle discard that reaches a bandit site. Hall of Bandits keeps itself when bandits rule its site. A bury is not a discard. It writes no line.

```

In item 1 of "Component-data dependencies", replace "Four printed powers
still have no executable behavior. They are listed in
`src/test/resources/powers/unimplemented.txt`: the ruined faces Blasted
Pastures (E07), Hall of Bandits (E16) and Hiding Place (E26), and the
Ancient City's enduring power." with "Two printed powers still have no
executable behavior. They are listed in
`src/test/resources/powers/unimplemented.txt`: the ruined faces Blasted
Pastures (E07) and Hiding Place (E26)." In item 5, replace "the four
powers item 1 lists" with "the two powers item 1 lists".

- [ ] **Step 2: The Chronicle rules**

In `docs/rules/chronicle.md`, step 2 (Shape Empire) of the Throne, add a
bullet after the "Digital: while a seated Exile holds Iron Hand active"
bullet:

```markdown
   - Digital: Ancient City is the Enduring site. While Hall of Bandits (E16 ruined) is on the map, a ruined edifice at a site bandits rule is not discarded either, the Hall included; Rival is not offered it. Neither writes a line ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), items 1 and 2).
```

- [ ] **Step 3: The rules reference and traceability**

In `docs/rules/rules-reference.md`, after the **Ruined Temple.** bullet, add:

```markdown
- **Ancient City and Hall of Bandits.** Ancient City is Enduring: cards at its site are not discarded in Shape Empire. CHRONICLE: Hall of Bandits keeps ruined edifices ruled by bandits from being discarded. Digital: both hold for Iron Hand's Shape Empire, and Revolutionary still discards at Ancient City ([Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), items 1 and 2).
```

In `docs/rules/implementation-traceability.md`, in the Throne task row,
replace

```markdown
Shape Empire with Enduring sites ([rulings 6, 11, 14](ambiguities.md#chronicle-rulings))
```

with

```markdown
Shape Empire with Ancient City's Enduring and Hall of Bandits ([rulings 6, 11, 14](ambiguities.md#chronicle-rulings); [Last Chronicle powers rulings](ambiguities.md#last-chronicle-powers-rulings), items 1 and 2; `ShapeEmpireExemptionsSuite`)
```

- [ ] **Step 4: The roadmap and the spec status**

In `docs/ROADMAP.md`, replace the **The last Chronicle powers.** item with:

```markdown
- [ ] **The last Chronicle powers.** Two printed powers still do nothing:
  Blasted Pastures (E07 ruined) buries the Nomad cards on the map at the
  start of the Throne unless the winner rules it, and Hiding Place (E26
  ruined) lets its ruler keep a relic into the next game
  ([Last Chronicle powers design](superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md),
  slices 2 and 3). With them
  `src/test/resources/powers/unimplemented.txt` is empty, which unblocks
  **Retire the unimplemented-card machinery across the stack**.
```

In the **Retire the unimplemented-card machinery** item, replace "Four
powers remain" with "Two powers remain".

In the spec, change the status line "Each slice is planned when the one
before it lands." to "Each slice is planned when the one before it lands.
Slice 1 landed 2026-11-16."

- [ ] **Step 5: The full gate**

Run in the background: `./sbtw test`.
Expected: every test passes.
Run: `python3 scripts/check-architecture.py` and
`python3 scripts/check-markdown-links.py`.
Expected: both pass.

- [ ] **Step 6: Commit**

```bash
git add docs/rules/ambiguities.md docs/rules/chronicle.md \
  docs/rules/rules-reference.md docs/rules/implementation-traceability.md \
  docs/ROADMAP.md \
  docs/superpowers/specs/2026-11/2026-11-16-last-chronicle-powers-design.md
git commit -F - <<'EOF'
docs(chronicle): record Ancient City and Hall of Bandits and close slice 1

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
```

## Self-review

- Spec coverage: ruling 1 (Tasks 1 and 3, Revolutionary covered by the
  existing `RevolutionarySuite` test), ruling 2 (Tasks 2 and 3), the
  slice's registration and suffix removal (Task 1), the filter before
  Rival's options (Task 2), the snapshot lines (Tasks 1 and 2), the docs
  the spec lists for this slice (Task 3). No note key changes, so the
  declared-keys snapshot is untouched; no wire change, so no round trip.
- Names: `ShapeEmpireExemptions.enduring`, `ShapeEmpireExemptions.banditRuin`,
  `AncientCityEnduring`, `HallOfBandits`, used the same way in every task.
