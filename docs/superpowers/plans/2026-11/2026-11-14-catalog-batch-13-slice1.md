# Catalog Batch 13, Slice 1: Reliquary Raid Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reliquary Raid (card 220) runs: WHEN PLAYED, its player takes a
random relic from the Reliquary, facedown, and the card leaves
`catalog/holding/`.

**Architecture:** The draw already exists. `ReliquaryDraws.steps` shuffles
the Reliquary through the walker, takes its first relic facedown to the
player's play area, and writes "The Reliquary was empty." when the
Reliquary is empty or set aside by Setup With No Empire. Bewitch, Ancient
Pact and Royal Ambitions use it through `CitizenshipWhenPlayed`. Reliquary
Raid is a new `WhenPlayedPower` whose effect is that draw plus one line,
"Took a relic from the Reliquary.", written only when the Reliquary had a
relic to take. The card object moves beside its power, and
`HoldingDiscordDenizens.scala`, whose only card it is, is deleted.

**Tech Stack:** Scala 3.9.0, munit, `./sbtw` (sbt wrapper), compiler flags
`-deprecation -feature -unchecked -Wunused:imports,privates,locals,implicits,nowarn -Werror`.

**Spec:** [Catalog batch 13 design](../../specs/2026-11/2026-11-14-catalog-batch-13-design.md),
ruling 7 and section "Slice 1: Reliquary Raid".

## Global Constraints

- Gameplay never reads card text: `scripts/check-architecture.py` forbids
  `rulesText` and `.text` under `gameplay/`.
- A card sits beside its power once a power of it is implemented, and in
  `catalog/holding/` exactly while none is (`CardCatalogSuite`).
- A `WhenPlayedPower`'s `effect` is a pure function of `ctx`; anything that
  reads state an earlier operation changes goes inside a `Branch` or
  `BuildOps`.
- A line a power writes from a key another object owns is declared with
  `notes.borrow(...)` (`ReliquaryDraws.notes`' contract).
- Lines a When Played power writes name no player: they post under the
  card's play ("Became a Citizen.", "Took the People's Favor.").
- Docs, comments and commit messages are written in plain English.
  Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Files

- Create: `src/main/scala/distantlands/gameplay/powers/whenplayed/ReliquaryRaid.scala`
  (the card object and its power)
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala`
  (register it)
- Delete: `src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala`
- Create: `src/test/scala/distantlands/gameplay/powers/whenplayed/ReliquaryRaidSuite.scala`
- Modify: `docs/rules/ambiguities.md`, `docs/rules/rules-reference.md`,
  `docs/superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md`

`NewFoundations.scala` needs no edit: it already lists `ReliquaryRaidCard`
and imports both `distantlands.catalog.holding._` and
`distantlands.gameplay.powers.whenplayed._`, so the object resolves from
its new package.

---

### Task 1: Reliquary Raid takes a random relic

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/whenplayed/ReliquaryRaid.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala`
- Delete: `src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala`
- Test: `src/test/scala/distantlands/gameplay/powers/whenplayed/ReliquaryRaidSuite.scala`

**Interfaces:**
- Consumes: `ReliquaryDraws.steps(power: PowerId, player: PlayerId, source: PowerSourceRef): Vector[Operation]`,
  `ReliquaryDraws.empty: NoteKey`, `WhenPlayedPower`,
  `WhenPlayedHarness` (`registered`, `walkShuffling`, `hook`, `said`,
  `p1AtHomeSite`, `assertJournalRebuilds`), `WalkerDiceFixture.reversingShuffle`,
  the `Table` builders `reliquaryFromDeck(n)` and `reliquarySetAside`.
- Produces: `ReliquaryRaidCard` (`DenizenId("220")`, `power.id ==
  PowerId("denizen.reliquary-raid")`), `case object ReliquaryRaid extends
  WhenPlayedPower` with `took: NoteKey` (key `"took"`).

- [ ] **Step 1: Write the failing suite**

Create `src/test/scala/distantlands/gameplay/powers/whenplayed/ReliquaryRaidSuite.scala`:

```scala
package distantlands.gameplay.powers.whenplayed

import distantlands.gameplay.WalkerDiceFixture
import distantlands.gameplay.powers.{NoteText, PowerImplementationStatus}
import distantlands.model._
import distantlands.testkit.Look
import distantlands.testkit.Table.p1

/** Reliquary Raid (card 220), WHEN PLAYED: take a random relic from the
  * Reliquary (catalog batch 13, ruling 7). */
class ReliquaryRaidSuite extends munit.FunSuite:
  import WhenPlayedHarness._

  private val raid = registered[ReliquaryRaid.type]

  private def line(key: String, text: String): NoteText.Said =
    NoteText.Said(key, text, covers = false)

  private def played(ready: ReadyGame): Walked =
    walkShuffling(ready, raid, hook(ReliquaryRaidCard.id),
      WalkerDiceFixture.reversingShuffle)

  test("the player takes a random Reliquary relic facedown"):
    val start = p1AtHomeSite.turn(p1, Phase.Act).reliquaryFromDeck(3).ready
    val stocked = start.game.campaign.reliquary
    val walked = played(start)
    assertEquals(walked.asked, Vector.empty)
    // The shuffle reverses the Reliquary, so its last relic comes first.
    val taken = Look(walked.end).player(p1).relics
      .filter(relic => stocked.contains(relic.id))
    assertEquals(taken.map(_.id), Vector(stocked.last))
    assertEquals(taken.map(_.orientation), Vector(Orientation.FaceDown))
    assertEquals(walked.end.game.campaign.reliquary.size, 2)
    assertEquals(said(raid, walked.events),
      Vector(line("took", "Took a relic from the Reliquary.")))
    assertJournalRebuilds(start, walked.events, walked.end)

  test("an empty Reliquary takes nothing and says so"):
    val start = p1AtHomeSite.turn(p1, Phase.Act).ready
    assertEquals(start.game.campaign.reliquary, Vector.empty)
    val walked = played(start)
    assertEquals(said(raid, walked.events),
      Vector(line("reliquary-empty", "The Reliquary was empty.")))
    assertJournalRebuilds(start, walked.events, walked.end)

  test("a Reliquary Setup With No Empire set aside gives no relic"):
    val start = p1AtHomeSite.turn(p1, Phase.Act).reliquaryFromDeck(2)
      .reliquarySetAside.ready
    val stocked = start.game.campaign.reliquary
    assertEquals(stocked.size, 2)
    val walked = played(start)
    assertEquals(walked.end.game.campaign.reliquary, stocked)
    assert(!Look(walked.end).player(p1).relics.exists(relic =>
      stocked.contains(relic.id)))
    assertEquals(said(raid, walked.events),
      Vector(line("reliquary-empty", "The Reliquary was empty.")))
    assertJournalRebuilds(start, walked.events, walked.end)

  test("its power counts as implemented"):
    assert(PowerImplementationStatus.implemented(ReliquaryRaidCard.power.id))
```

- [ ] **Step 2: Run it to verify it fails**

Run: `./sbtw "testOnly distantlands.gameplay.powers.whenplayed.ReliquaryRaidSuite"`
Expected: compile FAIL, "Not found: ReliquaryRaid" (the card object still
lives in `distantlands.catalog.holding`, which the suite does not import,
and the power does not exist).

- [ ] **Step 3: Move the card and write the power**

Delete `src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala`
(its only object is `ReliquaryRaidCard`).

Create `src/main/scala/distantlands/gameplay/powers/whenplayed/ReliquaryRaid.scala`:

```scala
package distantlands.gameplay.powers.whenplayed

import distantlands.catalog.{AdviserOnly, Denizen, Locked, PrintedPower}
import distantlands.gameplay.powerresolver.PowerCtx
import distantlands.gameplay.powers.ReliquaryDraws
import distantlands.model._

object ReliquaryRaidCard extends Denizen(DenizenId("220"), "Reliquary Raid", Suit.Discord) with Locked with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.reliquary-raid"),
    persistent = false, cost = Cost.free,
    text = "**WHEN PLAYED,** take a random relic from the Reliquary.")
  val powers: Vector[PrintedPower] = Vector(power)

/** Reliquary Raid (card 220, adviser-only, locked), WHEN PLAYED: take a
  * random relic from the Reliquary (catalog batch 13, ruling 7).
  *
  * The draw is `ReliquaryDraws`': the Reliquary is shuffled through the
  * walker and its first relic goes facedown to the player's play area. An
  * empty Reliquary, or one Setup With No Empire set aside, takes nothing
  * and writes "The Reliquary was empty." (ruling R22; Foundations ruling
  * 3). Whether there is a relic to take is read when the card resolves, so
  * "Took a relic from the Reliquary." is written only when one was taken.
  * The line never names the relic: it is facedown.
  */
case object ReliquaryRaid extends WhenPlayedPower:
  val id: PowerId = ReliquaryRaidCard.power.id
  val cardId: DenizenId = ReliquaryRaidCard.id
  private val notes = NoteSet(id)
  /** "Took a relic from the Reliquary." */
  val took: NoteKey = notes.key("took",
    Vector(NotePart.Text("Took a relic from the Reliquary.")))
  notes.borrow(ReliquaryDraws.empty)
  override def noteKeys: Vector[NoteKey] = notes.declared

  private def card: PowerSourceRef = PowerSourceRef.Card(cardId)

  def effect(ctx: PowerCtx): Vector[Operation] =
    Vector(Branch((live, _) =>
      val stocked = live.game.campaign.reliquaryInPlay.nonEmpty
      ReliquaryDraws.steps(id, ctx.actor, card) ++
        (if stocked then Vector(Note(id, _ => Some(took(card))))
          else Vector.empty)))
```

In `WhenPlayedPowers.scala`, add `ReliquaryRaid` to the end of `all`:

```scala
      Pilgrimage, AutumnWind, FavoredSon, SearchParty, FalseProphet,
      TheGathering, ReliquaryRaid)
```

- [ ] **Step 4: Run the suite and the catalog suite**

Run: `./sbtw "testOnly distantlands.gameplay.powers.whenplayed.ReliquaryRaidSuite distantlands.gameplay.cards.CardCatalogSuite"`
Expected: PASS. `CardCatalogSuite`'s "a card leaves the holding files once
a power of it is implemented" now finds Reliquary Raid outside
`catalog.holding` with its power implemented.

If `notes.borrow(...)` at object level is rejected by `-Wunused` or the
`NoteSet` API (it returns a value), copy the form `CitizenshipWhenPlayed`
uses, `if takesRelic then notes.borrow(ReliquaryDraws.empty)`, as a bare
statement; no other change.

- [ ] **Step 5: Run the full test suite**

Run: `./sbtw test`
Expected: PASS. Watch for any golden or count that lists implemented or
unimplemented cards: Reliquary Raid moving to implemented is the only
intended change. If one moves, update the expected value and say why in
the commit message.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/whenplayed/ReliquaryRaid.scala \
  src/main/scala/distantlands/gameplay/powers/whenplayed/WhenPlayedPowers.scala \
  src/main/scala/distantlands/catalog/holding/HoldingDiscordDenizens.scala \
  src/test/scala/distantlands/gameplay/powers/whenplayed/ReliquaryRaidSuite.scala
git commit -m "feat(catalog): Reliquary Raid takes a random relic from the Reliquary

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

### Task 2: Record the ruling and close the slice

**Files:**
- Modify: `docs/rules/ambiguities.md` (new section after "Legacy in-play
  powers rulings", before "Source relationship cautions")
- Modify: `docs/rules/rules-reference.md` (the "become a Citizen" bullet
  that names the Reliquary, near line 170)
- Modify: `docs/superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md`
  (status line)

**Interfaces:**
- Consumes: Task 1's power and its lines.
- Produces: the "Catalog batch 13 rulings" section, which slices 2 to 6
  extend with their rulings as items 2 onward.

- [ ] **Step 1: Open the rulings section**

In `docs/rules/ambiguities.md`, insert before `## Source relationship cautions`:

```markdown
## Catalog batch 13 rulings

Rulings for the cards that act on legacies ([Catalog batch 13 design](../superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md)). Items are the design's rulings, taken with the owner on 2026-11-14, added as each slice lands: item 1 is ruling 7.

1. **Reliquary Raid** takes a random relic from the Reliquary, facedown, as Bewitch, Ancient Pact and Royal Ambitions do. An empty Reliquary takes nothing and writes "The Reliquary was empty." (R22), and so does a Reliquary Setup With No Empire set aside ([Foundations rulings](#foundations-rulings), item 3). Otherwise the line is "Took a relic from the Reliquary.", which never names the relic.

```

- [ ] **Step 2: Add the Digital note**

In `docs/rules/rules-reference.md`, find the bullet beginning
`- **"Become a Citizen" denizens.**`. Add this bullet directly after it:

```markdown
- **Reliquary Raid.** WHEN PLAYED, take a random relic from the Reliquary, facedown. Digital: an empty Reliquary, or one Setup With No Empire set aside, gives nothing ([Catalog batch 13 rulings](ambiguities.md#catalog-batch-13-rulings), item 1).
```

- [ ] **Step 3: Mark the slice landed in the spec**

In the spec's `**Status:**` paragraph, append the sentence:
`Slice 1 landed 2026-11-14.` (keep the rest of the paragraph).

- [ ] **Step 4: Check the links**

Run: `python3 scripts/check-markdown-links.py`
Expected: "Markdown link check passed".

- [ ] **Step 5: Commit**

```bash
git add docs/rules/ambiguities.md docs/rules/rules-reference.md \
  docs/superpowers/specs/2026-11/2026-11-14-catalog-batch-13-design.md
git commit -m "docs(catalog): record Reliquary Raid's ruling and close batch 13 slice 1

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```
