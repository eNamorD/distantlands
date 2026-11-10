# Catalog Batch 10 Slice 7: Trade Suit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a Trade read a suit picked for one adviser, and implement Acting Troupe (36) on that pick.

**Architecture:**
- `EffectiveSuit` already reads Spiteful Mirror's pick from the walk's answers (`mirrored`). It learns a second pick the same way: the answer to Acting Troupe's suit question.
- The card moves from holding into `EffectiveSuit.scala`, beside `SpitefulMirrorCard`, because `EffectiveSuit` lives below `powers` and reads the card's question. The card also holds the question's id and its two buttons.
- `EffectiveSuit.count` takes the walk's answers. A faceup Troupe counts as the answered suit instead of Arcane. `MusterSource.matching` passes the answers, so every reader of a Trade's matching advisers sees the pick: the Trade's own gain and the Cup of Plenty (through `SuitSupplyWaiver`). Vow of Beastkin passes its answers too, since the parameter is required, but it restricts only a Muster, where the Troupe is never picked.
- **Acting Troupe** is a free `SelectedModifier` for Trade, offered while the Troupe is the user's faceup adviser. Its `selectionQuestions` ask Beast or Order and write the line, as `ActingSite` asks its site. It hooks no window: the answer is the whole effect.

**Tech Stack:** Scala 3, munit, sbt via `./sbtw`.

**Spec:** `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`. Read these sections:
- "Goal and scope"
- "Catalog corrections" (Acting Troupe carries the Trade icon)
- "Readings" (Acting Troupe)
- "Slice 7: Trade suit"
- "Testing"

Card text (`HoldingArcaneDenizens.scala`): "Act as if Acting Troupe is a [suit-beast] or [suit-order] card instead." Adviser-only, Arcane, free.

**Ruling this plan takes** (owner, 2026-11-05; it goes into the spec's "Rulings" in Task 2):
- **Acting Troupe and Pledge to Discord.** A picked Troupe counts as the picked suit for that Trade, even beside a faceup Pledge to Discord. Unpicked, or outside the Trade, Pledge makes it Discord as usual.

**Readings this plan takes** (each also goes into the spec's "Readings" in Task 2):
- **Every reader of the Trade's matching advisers sees the pick.** With the Cup of Plenty, a Troupe acting as a Beast card no longer matches an Arcane card, so a Trade with an Arcane card is free of Supply.
- **The Troupe as the Trade's own card.** Beloved (Legacy L33) lets a Trade draw on another player's adviser, which may be the Troupe. Its suit is then read as usual (Arcane, or Discord under its holder's Pledge), since only the Troupe's holder can pick it. So `EffectiveSuit.traded` is unchanged.
- **Line wording.** Two keys, so the article reads right: "Acting Troupe acted as a Beast card." and "Acting Troupe acted as an Order card."

This is the seventh of 14 slice plans, written against `0.2.0-alpha` at `73d7d094`.

## Global Constraints

- **Worktree:**
  - Work in `.worktrees/batch-10-slice-7` on branch `batch-10-slice-7`, created from local `0.2.0-alpha`. Never work in the main checkout.
  - An agent worktree made by the Agent tool starts from `origin/main`, which is far behind. Reset it onto local `0.2.0-alpha` before the first edit.
  - Before running `./sbtw`, symlink the main checkout's `.tooling` and `node_modules` into the worktree. Remove the symlinks before `git worktree remove --force`.
- **Git:**
  - Never run `git add -A`; add files by name.
  - Push nothing.
  - Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.
- **Build:** the compiler runs with `-Werror`, so unused imports and unused values fail the build.
- **Gates before the last commit:**
  - `./sbtw clean coverage test coverageReport`, with statement coverage of at least 92%
  - `python3 scripts/check-architecture.py`, which caps files at 800 lines and enforces the layer import rules
  - `python3 scripts/check-markdown-links.py`
- **Snapshots:** regenerate with `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`, then read the diff. Commit only the lines the task explains. The files are `src/test/resources/notes/declared-keys.txt` and `src/test/resources/powers/unimplemented.txt`.
- **Log lines:** a line names only non-actors (memory: "Log lines omit actor"). The Troupe's line names the card, not its user.
- **Test helpers:** a helper's name states what it builds (memory: "Descriptive helper names").
- **Modifier icon:** the Troupe's printed power gains `modifier = Some(ModifierMark.Action(MajorActionType.Trade))` (spec, "Catalog corrections").

---

### Task 1: `EffectiveSuit` reads the Troupe's pick

**Files:**
- Modify: `src/main/scala/distantlands/gameplay/EffectiveSuit.scala`
- Modify: `src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala` (remove `ActingTroupeCard`)
- Modify: `src/main/scala/distantlands/gameplay/cards/NewFoundations.scala` (import)
- Modify: `src/main/scala/distantlands/gameplay/actions/economy/MusterSource.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/economy/EconomyTree.scala:132`
- Modify: `src/main/scala/distantlands/gameplay/powers/economy/SuitSupplyWaiver.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/economy/CupOfPlenty.scala`, `AnimalPlaymates.scala`, `Birdsong.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/economy/VowOfBeastkin.scala:61`
- Test: `src/test/scala/distantlands/gameplay/EffectiveSuitSuite.scala`
- Modify: `src/test/scala/distantlands/gameplay/MusterProcedureSuite.scala:164-171` (the three `matching` calls)

**Interfaces:**
- Produces:
  - `object ActingTroupeCard` in package `distantlands.gameplay`, with `power`, `powers`, `suitDecision: String = "acting-troupe.suit"`, `suits: Vector[Suit] = Vector(Suit.Beast, Suit.Order)` and `button(suit: Suit): DecisionOptionRef.Button`.
  - `EffectiveSuit.troupe(answered: Vector[Answered]): Option[Suit]`.
  - `EffectiveSuit.count(catalog, ready, player, suit, answered: Vector[Answered] = Vector.empty): Int`.
  - `MusterSource.matching(catalog, state, actor, suit, answered: Vector[Answered]): Int`, the answers now required.
  - `SuitSupplyWaiver.effects(window, decisionId)(free: (ExecutableCatalog, ReadyGame, PlayerId, Vector[Answered], Suit) => Boolean)`.

- [ ] **Step 1: Write the failing tests**

In `EffectiveSuitSuite.scala`, add `private val troupe = ActingTroupeCard.id` beside the other card vals, and a helper after them:

```scala
  /** The walk's answers once Acting Troupe is picked for a Trade and acts
    * as `suit`. */
  private def troupeActingAs(suit: Suit): Vector[Answered] = Vector(
    Answered(ModifierPicks.decisionId(PowerWindow.TradeModifierSelection),
      ChooseOneAnswer(DecisionOptionRef.Denizen(troupe)), p1),
    Answered(ActingTroupeCard.suitDecision,
      ChooseOneAnswer(ActingTroupeCard.button(suit)), p1))
```

Then add these tests after "the traded suit is Discord once Spiteful Mirror is picked":

```scala
  test("a picked Acting Troupe counts as the answered suit instead of " +
      "Arcane, and unpicked as Arcane"):
    val ready = Table.start.adviser(p1, troupe).adviser(p1, wrestlers).ready
    val beast = troupeActingAs(Suit.Beast)
    assertEquals(EffectiveSuit.troupe(beast), Some(Suit.Beast))
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Beast, beast), 1)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Arcane, beast),
      0)
    // Wrestlers is Order and counts either way.
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Order,
      troupeActingAs(Suit.Order)), 2)
    assertEquals(EffectiveSuit.troupe(Vector.empty), None)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Arcane), 1)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Beast), 0)

  test("a facedown Acting Troupe counts for no suit, picked or not"):
    val ready = Table.start.adviser(p1, troupe, facedown = true).ready
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Beast,
      troupeActingAs(Suit.Beast)), 0)

  test("a picked Acting Troupe beats Pledge to Discord for the Trade"):
    // Owner ruling, 2026-11-05.
    val ready = Table.start.adviser(p1, pledge).adviser(p1, troupe).ready
    val order = troupeActingAs(Suit.Order)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Order, order), 1)
    // Only Pledge itself is Discord now.
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Discord, order),
      1)
    assertEquals(EffectiveSuit.count(catalog, ready, p1, Suit.Discord), 2)

```

Update the suite's doc comment to end: "..., and a picked Acting Troupe counts as the suit its user named (catalog batch 10, slice 7)."

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.EffectiveSuitSuite"`
Expected: compile error, `ActingTroupeCard` not found in `distantlands.gameplay`.

- [ ] **Step 3: Move the card**

Delete `ActingTroupeCard` (lines 25-30) from `HoldingArcaneDenizens.scala`. Add it to `EffectiveSuit.scala` after `SpitefulMirrorCard`:

```scala
object ActingTroupeCard extends Denizen(DenizenId("36"), "Acting Troupe", Suit.Arcane) with AdviserOnly:
  val power = PrintedPower(PowerId("denizen.acting-troupe"),
    persistent = false, cost = Cost.free,
    modifier = Some(ModifierMark.Action(MajorActionType.Trade)),
    text = "Act as if Acting Troupe is a [suit-beast] or [suit-order] " +
      "card instead.")
  val powers: Vector[PrintedPower] = Vector(power)
  /** The question its pick asks: which suit it acts as. */
  val suitDecision: String = "acting-troupe.suit"
  /** The suits it may act as, in the order the question offers them. */
  val suits: Vector[Suit] = Vector(Suit.Beast, Suit.Order)
  /** The question's answer that names `suit`. */
  def button(suit: Suit): DecisionOptionRef.Button =
    DecisionOptionRef.Button(suit.key)
```

Add `ModifierMark` to the `distantlands.catalog` import of `EffectiveSuit.scala`. In `NewFoundations.scala`, change the `distantlands.gameplay` import to:

```scala
import distantlands.gameplay.{ActingTroupeCard, MarriageCard,
  PledgeToDiscordCard, SpitefulMirrorCard}
```

If another holding file or test imports `catalog.holding.ActingTroupeCard`, point it at `distantlands.gameplay`. Run `grep -rn "ActingTroupeCard" src` to check.

- [ ] **Step 4: Read the pick in `EffectiveSuit`**

In the object's doc comment, replace the last two sentences ("It holds the cards its rules read: ...") with:

```scala
  * operation pipeline reads `pledged` too. It holds the cards its rules
  * read: Marriage and Pledge to Discord, whose registered powers add
  * nothing, and Spiteful Mirror and Acting Troupe, whose picks it reads.
  * A picked Troupe counts as the suit its question was answered with,
  * beside a faceup Pledge too (catalog batch 10, slice 7).
```

Replace `count` with:

```scala
  /** `player`'s faceup advisers of `suit`, each by its weight. `answered`
    * is the walk's answers, so a picked Acting Troupe counts as its
    * answered suit ([[troupe]]); a reader outside a Trade passes none. */
  def count(catalog: ExecutableCatalog, ready: ReadyGame, player: PlayerId,
      suit: Suit, answered: Vector[Answered] = Vector.empty): Int =
    ready.game.current.players.find(_.player == player).toVector
      .flatMap(_.advisers).collect {
        case DenizenState(id, Orientation.FaceUp, _)
            if acted(catalog, ready, answered, id).contains(suit) =>
          weight(id)
      }.sum
```

After `mirrored`, add:

```scala
  /** The suit a picked Acting Troupe acts as: the walk's answer to its
    * question, which is asked only once it is picked. */
  def troupe(answered: Vector[Answered]): Option[Suit] =
    answered.reverseIterator.collectFirst {
      case Answered(ActingTroupeCard.suitDecision,
          DecisionAnswer.ChooseOneAnswer(DecisionOptionRef.Button(key)), _) =>
        key
    }.flatMap(key => ActingTroupeCard.suits.find(_.key == key))
```

Leave `traded` as it is: only the Troupe's holder can pick it, so a Trade drawing on the Troupe never carries its pick. Add the private helper before `cardOf`:

```scala
  /** `card`'s suit for the walk with `answered`: a picked Acting Troupe's
    * answered suit, otherwise [[of]]. */
  private def acted(catalog: ExecutableCatalog, ready: ReadyGame,
      answered: Vector[Answered], card: CardId): Option[Suit] =
    if card == ActingTroupeCard.id then
      troupe(answered).orElse(of(catalog, ready, card))
    else of(catalog, ready, card)
```

If the stable-identifier pattern `ActingTroupeCard.suitDecision` does not compile, bind it first (`val decision = ActingTroupeCard.suitDecision`) and match with a backquoted `` `decision` ``.

- [ ] **Step 5: Pass the answers to every Trade reader**

In `MusterSource.scala`, replace `matching` and its doc comment:

```scala
  /** The actor's faceup advisers of `suit`, a Marriage counting as two
    * (`EffectiveSuit`): how many warbands, favor or secrets a Muster or Trade
    * adds to its base yield. `answered` is the walk's answers, so a picked
    * Acting Troupe counts as its picked suit.
    */
  def matching(catalog: ExecutableCatalog, state: ReadyGame, actor: PlayerId,
      suit: Suit, answered: Vector[Answered]): Int =
    EffectiveSuit.count(catalog, state, actor, suit, answered)
```

In `EconomyTree.afterSource`, change the yield's call to `MusterSource.matching(catalog, ready, actor, source.suit, answered)`.

In `VowOfBeastkin.refused`, change the check to `MusterSource.matching(ctx.catalog, ctx.state, ctx.actor, suit, ctx.answered) == 0`.

In `SuitSupplyWaiver.scala`, the `free` function gains the answers, before the suit, in both `effects` and `unlessFree`:

```scala
      free: (ExecutableCatalog, ReadyGame, PlayerId, Vector[Answered], Suit)
        => Boolean)
```

and `unlessFree` calls it as `.exists(free(catalog, ready, pay.player, pending.answered, _))`. Add to the doc comment, after the sentence ending "A card whose suit cannot be read pays.": "`free` also receives the walk's answers, for a rule that counts the actor's advisers as the action reads them."

Update the three lambdas:
- `CupOfPlenty`: `(catalog, ready, actor, answered, suit) => MusterSource.matching(catalog, ready, actor, suit, answered) == 0`
- `AnimalPlaymates`: `(_, _, _, _, suit) => suit == Suit.Beast`
- `Birdsong`: `(_, _, _, _, suit) => Birdsong.Suits(suit)`

In `CupOfPlenty`'s doc comment, add: "A picked Acting Troupe counts as its picked suit."

In `MusterProcedureSuite.scala`, the three `MusterSource.matching` calls in the two "matching counts ..." tests gain a last argument, `Vector.empty`: those tests pick no modifier.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.EffectiveSuitSuite distantlands.gameplay.powers.economy.*"`
Expected: PASS. The economy suites pin that the threading changes nothing without a Troupe.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/gameplay/EffectiveSuit.scala \
  src/main/scala/distantlands/catalog/holding/HoldingArcaneDenizens.scala \
  src/main/scala/distantlands/gameplay/cards/NewFoundations.scala \
  src/main/scala/distantlands/gameplay/actions/economy/MusterSource.scala \
  src/main/scala/distantlands/gameplay/actions/economy/EconomyTree.scala \
  src/main/scala/distantlands/gameplay/powers/economy/SuitSupplyWaiver.scala \
  src/main/scala/distantlands/gameplay/powers/economy/CupOfPlenty.scala \
  src/main/scala/distantlands/gameplay/powers/economy/AnimalPlaymates.scala \
  src/main/scala/distantlands/gameplay/powers/economy/Birdsong.scala \
  src/main/scala/distantlands/gameplay/powers/economy/VowOfBeastkin.scala \
  src/test/scala/distantlands/gameplay/EffectiveSuitSuite.scala \
  src/test/scala/distantlands/gameplay/MusterProcedureSuite.scala
git commit -m "$(cat <<'EOF'
feat(gameplay): a Trade counts a picked Acting Troupe as its answered suit

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

---

### Task 2: Acting Troupe, docs and gates

**Files:**
- Create: `src/main/scala/distantlands/gameplay/powers/economy/ActingTroupe.scala`
- Modify: `src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala`
- Modify: `src/test/resources/powers/unimplemented.txt`, `src/test/resources/notes/declared-keys.txt` (regenerated)
- Test: `src/test/scala/distantlands/gameplay/powers/economy/ActingTroupeSuite.scala`
- Modify: `docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md`, `docs/ROADMAP.md`

**Interfaces:**
- Consumes: `ActingTroupeCard.suitDecision`, `suits`, `button`; `EffectiveSuit.troupe`.
- Produces: `case object ActingTroupe extends SelectedModifier`, with note keys `actedBeast` and `actedOrder`.

- [ ] **Step 1: Write the failing tests**

Create `ActingTroupeSuite.scala`:

```scala
package distantlands.gameplay.powers.economy

import distantlands.gameplay.ActingTroupeCard
import distantlands.gameplay.actions.economy.{MusterProcedure, TradeProcedure}
import distantlands.gameplay.cards.AncientCitySite
import distantlands.gameplay.powerresolver.ModifierPicks
import distantlands.gameplay.powers.NoteText
import distantlands.gameplay.powers.action.AlchemistCard
import distantlands.gameplay.powers.campaign.WrestlersCard
import distantlands.gameplay.powers.search.ErrandBoyCard
import distantlands.model._
import distantlands.model.DecisionAnswer.ChooseOneAnswer
import distantlands.model.OathState.Ready
import distantlands.testkit.{ActPowerHarness, Look, MenuPlay, Outcomes,
  PhasePowerHarness, PowerRules, Table}
import distantlands.testkit.Outcomes.assertWireRoundTrips
import distantlands.testkit.Table.p1
import distantlands.testkit.Unwrap.accepted

/** Acting Troupe (card 36), a selected Trade modifier: picked, it asks
  * Beast or Order, and the Trade counts it as that suit (catalog batch 10,
  * slice 7). */
class ActingTroupeSuite extends munit.FunSuite:
  private val troupe = ActingTroupeCard.id
  private val power = ActingTroupe
  private val home = AncientCitySite.id
  /** A plain Beast denizen. */
  private val errandBoy = ErrandBoyCard.id
  /** A plain Order denizen. */
  private val wrestlers = WrestlersCard.id
  /** A plain Arcane denizen. */
  private val alchemist = AlchemistCard.id
  private val modifiers =
    ModifierPicks.decisionId(PowerWindow.TradeModifierSelection)
  private val rules = PowerRules.withWalkerPowers
  private val parked = PowerRules.parkedUnderWalkerPowers

  /** p1 at Ancient City holding Acting Troupe faceup, with `card` at the
    * site, 2 favor, 1 faceup secret, 7 Supply, and 3 favor in the Beast,
    * Order and Arcane banks. */
  private def troupeBeside(card: DenizenId): Table = Table.start
    .pawn(p1, at = home).adviser(p1, troupe).denizen(card, at = home)
    .favor(p1, 2).secrets(p1, 1).supply(p1, 7)
    .bankFavor(Suit.Beast, 3).bankFavor(Suit.Order, 3)
    .bankFavor(Suit.Arcane, 3)

  private def tradeBegun(ready: ReadyGame, resource: String = "favor")
      : OathTransition = accepted(MenuPlay.started(rules, Ready(ready), p1,
    ActionRef.Trade, Vector(DecisionOptionRef.Button(resource))))

  /** `from`, then p1's answer `ref` at `decisionId`; the events run from
    * the action's start. */
  private def answeredWith(from: OathTransition, decisionId: String,
      ref: DecisionOptionRef): OathTransition =
    val next = accepted(rules.resolveWalker(from.state, p1, decisionId,
      ChooseOneAnswer(ref)))
    next.copy(events = from.events ++ next.events)

  /** `from` with the Troupe picked and acting as `suit`. The Troupe is the
    * only modifier, so the Trade then starts by itself. */
  private def troupeActingAs(from: OathTransition, suit: Suit)
      : OathTransition =
    answeredWith(answeredWith(from, modifiers,
      DecisionOptionRef.Denizen(troupe)), ActingTroupeCard.suitDecision,
      ActingTroupeCard.button(suit))

  /** `from` with "Start" answered and no modifier picked. */
  private def startedUnpicked(from: OathTransition): OathTransition =
    answeredWith(from, modifiers, ModifierPicks.start)

  private def tradedWith(from: OathTransition, card: DenizenId)
      : OathTransition =
    answeredWith(from, TradeProcedure.decisionId,
      DecisionOptionRef.Denizen(card))

  private def said(events: Vector[OathEvent]): Vector[NoteText.Said] =
    NoteText.said(power.id, power.noteKeys, events)

  test("picked, it asks Beast or Order"):
    val asked = answeredWith(tradeBegun(troupeBeside(errandBoy).ready),
      modifiers, DecisionOptionRef.Denizen(troupe))
    assertEquals(PhasePowerHarness.offered(asked, p1), Some(Vector(
      "button" -> "beast", "button" -> "order")))

  test("acting as a Beast card, it adds 1 favor to a Trade with a Beast " +
      "card, and the line says so"):
    val ready = troupeBeside(errandBoy).ready
    val done = tradedWith(troupeActingAs(tradeBegun(ready), Suit.Beast),
      errandBoy)
    // 1, and 1 for the Troupe.
    assertEquals(Look(done.state).favor(p1), 2 + 2)
    assertEquals(Look(done.state).bankFavor(Suit.Beast), 1)
    assertEquals(said(done.events), Vector(NoteText.Said("acted-beast",
      s"${troupe.value} acted as a Beast card.", covers = false)))
    assertEquals(ActPowerHarness.replayed(rules, ready, done.events),
      Outcomes.ready(done.state))
    assertWireRoundTrips(done.events)

  test("acting as an Order card, it gains a secret in a Trade for secrets " +
      "with an Order card, and the line says so"):
    val ready = troupeBeside(wrestlers).ready
    val done = tradedWith(troupeActingAs(tradeBegun(ready, "secret"),
      Suit.Order), wrestlers)
    // 1 faceup secret kept, 1 gained for the Troupe.
    assertEquals(Look(done.state).faceUpSecrets(p1), 2)
    assertEquals(said(done.events), Vector(NoteText.Said("acted-order",
      s"${troupe.value} acted as an Order card.", covers = false)))
    val unpicked = tradedWith(startedUnpicked(tradeBegun(ready, "secret")),
      wrestlers)
    assertEquals(Look(unpicked.state).faceUpSecrets(p1), 1)

  test("unpicked, it counts as Arcane"):
    val ready = troupeBeside(alchemist).ready
    val arcane = tradedWith(startedUnpicked(tradeBegun(ready)), alchemist)
    assertEquals(Look(arcane.state).favor(p1), 2 + 2)
    assertEquals(said(arcane.events), Vector.empty)
    val beastReady = troupeBeside(errandBoy).ready
    val beast = tradedWith(startedUnpicked(tradeBegun(beastReady)), errandBoy)
    assertEquals(Look(beast.state).favor(p1), 2 + 1)

  test("acting as a Beast card, it no longer counts as Arcane"):
    val ready = troupeBeside(alchemist).ready
    val done = tradedWith(troupeActingAs(tradeBegun(ready), Suit.Beast),
      alchemist)
    assertEquals(Look(done.state).favor(p1), 2 + 1)

  test("with the Cup of Plenty, a Troupe acting as a Beast card no longer " +
      "matches an Arcane card, so the Trade costs no Supply"):
    val ready = troupeBeside(alchemist).relic(p1, CupOfPlentyCard.id).ready
    val cupPicked = answeredWith(
      troupeActingAs(tradeBegun(ready), Suit.Beast), modifiers,
      DecisionOptionRef.Relic(CupOfPlentyCard.id))
    assertEquals(Look(tradedWith(cupPicked, alchemist).state).supply(p1), 7)
    val unpicked = answeredWith(answeredWith(tradeBegun(ready), modifiers,
      DecisionOptionRef.Relic(CupOfPlentyCard.id)), modifiers,
      ModifierPicks.start)
    assertEquals(Look(tradedWith(unpicked, alchemist).state).supply(p1),
      7 - 1)

  test("a facedown Troupe is not offered, so the Trade asks for its card " +
      "at once"):
    val ready = Table.start.pawn(p1, at = home)
      .adviser(p1, troupe, facedown = true).denizen(errandBoy, at = home)
      .secrets(p1, 1).ready
    parked.assertParked(tradeBegun(ready).state, ActionRef.Trade,
      TradeProcedure.decisionId, p1)

  test("it is no Muster modifier, and a Muster counts it as Arcane"):
    val ready = troupeBeside(alchemist).ready
    val begun = accepted(MenuPlay.started(rules, Ready(ready), p1,
      ActionRef.Muster, Vector.empty))
    parked.assertParked(begun.state, ActionRef.Muster,
      MusterProcedure.decisionId, p1)
    val done = answeredWith(begun, MusterProcedure.decisionId,
      DecisionOptionRef.Denizen(alchemist))
    // 1, and 1 for the Troupe.
    assertEquals(Look(done.state).boardWarbands(p1),
      Look(ready).boardWarbands(p1) + 2)
```

Notes for the implementer:
- The Cup test picks the Troupe first. After its question, the modifier decision asks again with the Cup still to pick, so the Trade does not start by itself until the Cup is picked too. If the walk instead parks again after the Cup, answer `ModifierPicks.start` before `tradedWith`.
- If `PhasePowerHarness.offered` names a button option's kind other than `"button"`, use the kind it reports. The ids must be `"beast"` and `"order"`.
- If a Trade with no modifier available still parks on the modifier decision, the facedown test fails on `assertParked`. Then the facedown Troupe is offered by mistake: check `selectable`.

- [ ] **Step 2: Run them to verify they fail**

Run: `./sbtw "testOnly distantlands.gameplay.powers.economy.ActingTroupeSuite"`
Expected: compile error, `ActingTroupe` not found.

- [ ] **Step 3: Write the power**

Create `ActingTroupe.scala`:

```scala
package distantlands.gameplay.powers.economy

import distantlands.catalog.{Denizen, ExecutableCatalog}
import distantlands.gameplay.{ActingTroupeCard, NoteSupport, PowerAccess}
import distantlands.gameplay.powerresolver.Contribution
import distantlands.gameplay.powers.SelectedModifier
import distantlands.model._

/** Acting Troupe (card 36, adviser-only), a free selected Trade modifier:
  * "Act as if Acting Troupe is a [suit-beast] or [suit-order] card
  * instead." (catalog batch 10, slice 7).
  *
  * It is offered while the Troupe is its user's faceup adviser. Picked, it
  * asks Beast or Order, in the same pass of the modifier decision, as
  * `ActingSite` asks its site. The answer is the whole effect:
  * `EffectiveSuit` reads it, so every reader of the Trade's matching
  * advisers counts the Troupe as that suit, beside a faceup Pledge to
  * Discord too (owner ruling, 2026-11-05). It hooks no window.
  *
  * Its line follows the answer: "Acting Troupe acted as a Beast card." or
  * "... as an Order card."
  */
case object ActingTroupe extends SelectedModifier:
  val id: PowerId = ActingTroupeCard.power.id
  private val notes = NoteSet(id)
  override def noteKeys: Vector[NoteKey] = notes.declared
  /** "{Acting Troupe} acted as a Beast card." */
  val actedBeast: NoteKey = notes.key("acted-beast", Vector(
    NotePart.Arg(0), NotePart.Text(" acted as a Beast card.")))
  /** "{Acting Troupe} acted as an Order card." */
  val actedOrder: NoteKey = notes.key("acted-order", Vector(
    NotePart.Arg(0), NotePart.Text(" acted as an Order card.")))

  val card: Denizen = ActingTroupeCard
  def actions: Set[MajorActionType] = Set(MajorActionType.Trade)
  def effects: Map[PowerWindow, Vector[Contribution]] = Map.empty
  override def narratedDecisions: Set[String] =
    Set(ActingTroupeCard.suitDecision)

  /** Only as its user's faceup adviser, never from a site. */
  override def selectable(ready: ReadyGame, actor: PlayerId): Boolean =
    PowerAccess.holdsFaceupAdviser(ready, actor, ActingTroupeCard.id)

  /** The suit question, then the line. */
  override def selectionQuestions(catalog: ExecutableCatalog,
      ready: ReadyGame, actor: PlayerId): Vector[Operation] = Vector(
    Decide(ActingTroupeCard.suitDecision, actor, DecisionQuery.ChooseOne(
      ActingTroupeCard.suits.map(suit =>
        DecisionOption.Button(ActingTroupeCard.button(suit), suit.toString)),
      heading = Some("Acting Troupe: act as which suit?"))),
    Note(id, actedNote))

  private def actedNote(states: NoteStates): Option[LogNote] =
    val troupe = NoteArg.Card(cardId)
    NoteSupport.answer(states, ActingTroupeCard.suitDecision).flatMap {
      case ref if ref == ActingTroupeCard.button(Suit.Beast) =>
        Some(actedBeast(PowerSourceRef.Card(cardId), troupe))
      case ref if ref == ActingTroupeCard.button(Suit.Order) =>
        Some(actedOrder(PowerSourceRef.Card(cardId), troupe))
      case _ => None
    }
```

In `ActionModifiers.scala`, add `ActingTroupe` to the `powers.economy` import (alphabetical, first) and to `all` after `SpitefulMirror`:

```scala
      SpitefulMirror, ActingTroupe, MovingMarket, AwaitedReturn,
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.gameplay.powers.economy.*"`
Expected: PASS.

- [ ] **Step 5: Regenerate the snapshots**

Run: `NOTE_KEYS_SNAPSHOT=write UNIMPLEMENTED_SNAPSHOT=write ./sbtw "testOnly distantlands.gameplay.powers.*"`

Then `git diff src/test/resources`. Expected:
- `unimplemented.txt` loses `denizen.acting-troupe`, and nothing else.
- `declared-keys.txt` gains the two `denizen.acting-troupe` keys, `acted-beast` and `acted-order`, and nothing else.

- [ ] **Step 6: Update the spec and the roadmap**

In the spec:
- Change the status line to "**Status:** designed 2026-11-03. Slices 1 to 7 landed."
- After ruling 13, add:

```markdown
14. **Acting Troupe and Pledge to Discord** (owner, 2026-11-05). A picked
    Troupe counts as the picked suit for that Trade, even beside a faceup
    Pledge to Discord. Unpicked, or outside the Trade, Pledge makes it
    Discord as usual.
```

- Replace the Readings bullet "**Acting Troupe:** the user picks Beast or Order each Trade." with:

```markdown
- **Acting Troupe:** the user picks Beast or Order each Trade. Every
  reader of the Trade's matching advisers sees the pick, so with the Cup
  of Plenty a Troupe acting as a Beast card no longer matches an Arcane
  card. Beloved lets a Trade draw on another player's adviser, which may
  be the Troupe. Its suit is then read as usual (Arcane, or Discord under
  its holder's Pledge), since only the Troupe's holder can pick it.
```

- Replace the "Slice 7: Trade suit" body above "Tests:" with:

```markdown
**Acting Troupe.** A Trade modifier, a `SelectedModifier`, usable while the
Troupe is the user's faceup adviser. Its `selectionQuestions` ask Beast or
Order once, at the Trade's start. The card moves beside
`SpitefulMirrorCard`, since `EffectiveSuit` reads its question.
`EffectiveSuit.count` receives `answered`, as Spiteful Mirror's pick is
read (`EffectiveSuit.mirrored`), so a picked Troupe counts as the chosen
suit when Trade counts matching advisers. `MusterSource.matching`
and `SuitSupplyWaiver` pass the answers on, which reaches the Cup of
Plenty. Lines: "Acting Troupe acted as a Beast card." and "Acting Troupe
acted as an Order card."
```

In `docs/ROADMAP.md`, in the "**Unchanged from batch 8:**" bullet, replace "Master of Disguise and Acting Troupe (acting as another card)" with "Master of Disguise (acting as another player's advisers)". Re-wrap the bullet to 80 columns as the file does. Leave the rest of the bullet alone: its other cards are later slices' work.

- [ ] **Step 7: Run the gates**

Run, in the background: `./sbtw clean coverage test coverageReport`
Expected: every test passes; statement coverage at least 92%.

Run: `python3 scripts/check-architecture.py && python3 scripts/check-markdown-links.py`
Expected: both pass.

- [ ] **Step 8: Commit**

```bash
git add src/main/scala/distantlands/gameplay/powers/economy/ActingTroupe.scala \
  src/main/scala/distantlands/gameplay/powers/ActionModifiers.scala \
  src/test/resources/powers/unimplemented.txt \
  src/test/resources/notes/declared-keys.txt \
  src/test/scala/distantlands/gameplay/powers/economy/ActingTroupeSuite.scala \
  docs/superpowers/specs/2026-11-03-catalog-batch-10-design.md \
  docs/ROADMAP.md
git commit -m "$(cat <<'EOF'
feat(powers): Acting Troupe acts as a Beast or Order card for a Trade

Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>
EOF
)"
```

**Review checkpoint:** a review subagent reads the Task 1 and Task 2 diffs against this plan and the spec.
