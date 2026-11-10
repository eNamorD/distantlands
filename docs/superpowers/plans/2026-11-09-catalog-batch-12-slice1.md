# Catalog Batch 12, Slice 1: Promises Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let every Negotiation author add written promises to their terms,
with the binding kind present but closed, and write one Game Log line per
promise when the deal is agreed.

**Architecture:** `NegotiationTerms` gains `promises` and `bindingPromises`
(text, at most 5 of each, each at most 160 characters), checked by one
model function, `NegotiationPromise.problem`. `NegotiationBounds` gains
`bindingPromises: Boolean`, false everywhere in this slice; the answer check
refuses a binding promise while it is false. A promise counts as deal
substance. The journal codec, the command wire, the intent mapper, the deal
projection and the deal panel carry the text. The log gets
`NoteArg.Promise(text)` and two Negotiation note keys, posted last in the
settlement.

**Tech Stack:** Scala 3, Scala.js (frontend), munit, `./sbtw` (sbt
wrapper), `-Werror`.

**Spec:** [Catalog batch 12 design](../specs/2026-11-09-catalog-batch-12-design.md),
sections "Rulings" (10 to 13), "The deal model" and "Slice 1: Promises".

## Global Constraints

- Scala 3 with `-Werror`: no unused imports, no non-exhaustive matches.
- Every file stays under 800 lines (`scripts/check-architecture.py`).
- `gameplay.actions` must not import `gameplay.powers`
  (`scripts/check-architecture.py`).
- Statement coverage stays at or above 92%
  (`./sbtw clean coverage test coverageReport`).
- Wire changes need not keep old journals decodable: new fields are
  required on decode.
- Promise limits (ruling 10): at most 5 of each kind per author, each at
  most 160 characters, never blank, no surrounding spaces, no control
  character, no promise twice.
- The engine enforces no promise (ruling 12). A promise alone is substance
  (ruling 13).
- Promise text is rendered as plain text only: `textContent` and
  `createTextNode` in the client, `LogSpan.Text` in the log. Never
  `innerHTML`.
- The package is `distantlands` (renamed from `oathdigital` at `d13ab38b3`
  and its siblings). Every path below is at `a582390b4`.
- Shell is zsh: never write a bare `===` or `=word`, and quote globs.
- Never `git add -A`; add files by name. Push nothing.
- Commit trailer: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Setup

Work in a worktree off `0.2.0-alpha`, never in the main checkout. Symlink
the main checkout's `.tooling` and `node_modules` into the worktree before
running `./sbtw`:

```bash
ln -s /Users/roman/projects/oathdigital/.tooling .tooling
ln -s /Users/roman/projects/oathdigital/node_modules node_modules
```

## Plan-time findings

1. **Terms are built in three places.** The model constructor
   (`model/NegotiationTerms.scala:28-39`), the journal decoder
   (`serialization/NegotiationTermsCodec.scala:36-62`, which turns a
   failed `require` into `InvalidValue` through its `NonFatal` catch), and
   the intent mapper (`application/GameIntentMapper.scala:52`, which has no
   catch). The mapper must check the limits itself before it builds terms,
   so a bad promise from a client is a `GameIntentMappingFailure`, not an
   exception.
2. **The answer check is `DecisionQueries.acceptsTerms`**
   (`gameplay/walker/DecisionQueries.scala:350-375`). It reads the author's
   `NegotiationBounds`. The binding-promise refusal goes there.
3. **Substance is `DealState.hasSubstance`**
   (`gameplay/actions/negotiation/NegotiationDeal.scala:10-13`). A promise
   counts, so a promise-only deal may be accepted and settles. Its
   settlement batch (`NegotiationDeal.settle`) is then empty; the walk runs
   an empty `BuildOps` already when a deal holds only Citizenship.
4. **The settlement's lines are `Note`s** built in
   `NegotiationProcedure.tree` (`NegotiationProcedure.scala:96-143`):
   opening, then `BuildOps` plus disclosure, transfer and Citizenship-offer
   notes, then each Citizen's role change and line. The promise notes go
   after all of that.
5. **Every viewer gets the same Game Log entries**
   (`GameLogPropertiesSuite`, "every viewer receives the same entries with
   the same keys"). This slice keeps that: promise lines are public. Private
   lines (`LogNote.readers`) are slice 5's.
6. **Two exhaustive matches over `NoteArg`:** `NoteLines.argument`
   (`application/gamelog/NoteLines.scala:132-158`) and the test helper
   `NoteText.plain` (`src/test/scala/distantlands/gameplay/powers/NoteText.scala:45-64`).
   The codec's encoder (`serialization/WalkerEventCodec.scala:126-156`) is
   a third. All three need the new case.
7. **The deal panel builds its own inputs**
   (`frontend/.../NegotiationDealPanel.scala:150-209`). The existing panel
   test counts `.negotiation-item` rows and expects 2; the promise editor
   adds no row while the viewer has no promise, so it stays 2.
8. **Shared sources** (`shared/src/main` and `shared/src/test`) build in the
   root project, so `./sbtw "testOnly distantlands.protocol.*"` runs their
   suites.

## File map

| File | Change |
|---|---|
| `src/main/scala/distantlands/model/NegotiationTerms.scala` | `NegotiationPromise`, promise fields, `bindingPromises` bound |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala` | promises are substance |
| `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala` | refuse a binding promise the bounds close |
| `src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala` | journal fields |
| `shared/src/main/scala/distantlands/protocol/CommandIntents.scala` | wire fields |
| `shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala` | wire codec |
| `src/main/scala/distantlands/application/GameIntentMapper.scala` | trim and check promises |
| `src/main/scala/distantlands/model/LogNotes.scala` | `NoteArg.Promise` |
| `src/main/scala/distantlands/serialization/WalkerEventCodec.scala` | `promise` note arg |
| `src/main/scala/distantlands/application/gamelog/NoteLines.scala` | quoted span |
| `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala` | two note keys, promise notes |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala` | promise projection, binding flag |
| `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala` | projection codec |
| `src/main/scala/distantlands/application/NegotiationDealProjector.scala` | project promises and flag |
| `frontend/src/main/scala/distantlands/frontend/NegotiationPromiseEditor.scala` | new: one kind's promise fields |
| `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala` | summary lines, editors, Save |
| `frontend/src/main/scala/distantlands/frontend/GameClient.scala` | input fields |
| `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala` | pass promises to the wire |

---

### Task 1: Promise terms in the model and the answer check

**Files:**
- Modify: `src/main/scala/distantlands/model/NegotiationTerms.scala`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala:10-13`
- Modify: `src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala:350-375`
- Create: `src/test/scala/distantlands/model/NegotiationPromiseSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala`
- Test: `src/test/scala/distantlands/gameplay/walker/DecisionQuerySuite.scala`

**Interfaces:**
- Produces: `NegotiationPromise.MaxLength: Int = 160`,
  `NegotiationPromise.MaxPerKind: Int = 5`,
  `NegotiationPromise.problem(promises: Vector[String]): Option[String]`;
  `NegotiationTerms(transfers, disclosures, citizenship, promises:
  Vector[String] = Vector.empty, bindingPromises: Vector[String] =
  Vector.empty)`; `NegotiationBounds(..., citizenshipRefused,
  bindingPromises: Boolean = false)`.

- [ ] **Step 1: Write the failing tests**

Create `src/test/scala/distantlands/model/NegotiationPromiseSuite.scala`:

```scala
package distantlands.model

/** The limits on a Negotiation author's promises (catalog batch 12,
  * ruling 10). */
class NegotiationPromiseSuite extends munit.FunSuite:
  private val longest = "x" * NegotiationPromise.MaxLength

  test("promises inside every limit pass"):
    assertEquals(NegotiationPromise.problem(Vector.empty), None)
    assertEquals(NegotiationPromise.problem(Vector("I will not attack you.",
      longest)), None)
    assertEquals(NegotiationPromise.problem(Vector.tabulate(
      NegotiationPromise.MaxPerKind)(i => s"Promise $i")), None)

  test("each broken limit is named"):
    assertEquals(NegotiationPromise.problem(Vector.tabulate(
      NegotiationPromise.MaxPerKind + 1)(i => s"Promise $i")),
      Some("more than 5 promises"))
    assertEquals(NegotiationPromise.problem(Vector("Same", "Same")),
      Some("the same promise twice"))
    assertEquals(NegotiationPromise.problem(Vector("  ")),
      Some("a blank promise"))
    assertEquals(NegotiationPromise.problem(Vector(" padded")),
      Some("a promise with surrounding spaces"))
    assertEquals(NegotiationPromise.problem(Vector("two\nlines")),
      Some("a promise with a control character"))
    assertEquals(NegotiationPromise.problem(Vector(longest + "x")),
      Some("a promise over 160 characters"))

  test("length counts characters, not UTF-16 units"):
    // Each emoji is one character and two UTF-16 units.
    val emoji = "😀" * NegotiationPromise.MaxLength
    assertEquals(NegotiationPromise.problem(Vector(emoji)), None)

  test("terms refuse promises that break a limit, of either kind"):
    intercept[IllegalArgumentException](NegotiationTerms(promises =
      Vector("")))
    intercept[IllegalArgumentException](NegotiationTerms(bindingPromises =
      Vector(longest + "x")))
```

Add to `NegotiationDealSuite` (after "the deal is agreed only when every
participant accepted terms with substance"):

```scala
  test("a promise alone is substance, and editing it clears every acceptance"):
    val promise = NegotiationTerms(promises = Vector("I will not attack you."))
    val agreed = fold(said(p1, ProposeTerms(promise)), said(p2, AcceptDeal),
      said(p1, AcceptDeal))
    assert(agreed.hasSubstance && agreed.agreed)
    val edited = fold(said(p1, ProposeTerms(promise)), said(p2, AcceptDeal),
      said(p1, ProposeTerms(NegotiationTerms(promises =
        Vector("I will not attack you this round.")))))
    assertEquals(edited.accepted, Set.empty[PlayerId])
    val binding = fold(said(p1, ProposeTerms(NegotiationTerms(
      bindingPromises = Vector("I will give you 1 favor next turn.")))))
    assert(binding.hasSubstance)
```

Add to `DecisionQuerySuite` (after "proposed terms must sit inside the
author's bounds"):

```scala
  test("a binding promise needs bounds that open them; a plain one does not"):
    val plain = NegotiationTerms(promises = Vector("I will not attack you."))
    val binding = NegotiationTerms(bindingPromises =
      Vector("I will not attack you."))
    assertEquals(acceptsBy(red, DecisionAnswer.ProposeTerms(plain)), Right(()))
    assertEquals(acceptsBy(red, DecisionAnswer.ProposeTerms(binding)),
      invalid("decision recover.choice offers a binding promise it cannot"))
    val open = deal(acceptors = Set(red, blue)).copy(bounds = Map(
      red -> redBounds.copy(bindingPromises = true), blue -> blueBounds))
    assertEquals(acceptsBy(red, DecisionAnswer.ProposeTerms(binding), open),
      Right(()))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.model.NegotiationPromiseSuite distantlands.gameplay.NegotiationDealSuite distantlands.gameplay.walker.DecisionQuerySuite"`
Expected: compile errors: `NegotiationPromise` not found, no `promises`
parameter, no `bindingPromises` on `NegotiationBounds`.

- [ ] **Step 3: Implement the model**

In `src/main/scala/distantlands/model/NegotiationTerms.scala`, add before
`NegotiationTerms`:

```scala
/** The limits on the text an author promises (catalog batch 12, ruling
  * 10): at most `MaxPerKind` of each kind, each at most `MaxLength`
  * characters, none blank, padded, multi-line or repeated. The engine
  * enforces no promise (ruling 12). */
object NegotiationPromise:
  val MaxLength: Int = 160
  val MaxPerKind: Int = 5

  /** Why `promises` breaks a limit, or `None`. Length counts code points,
    * so an emoji is one character. */
  def problem(promises: Vector[String]): Option[String] =
    if promises.size > MaxPerKind then Some(s"more than $MaxPerKind promises")
    else if promises.distinct.size != promises.size then
      Some("the same promise twice")
    else promises.collectFirst {
      case text if text.isBlank => "a blank promise"
      case text if text.strip != text => "a promise with surrounding spaces"
      case text if text.exists(_.isControl) =>
        "a promise with a control character"
      case text if text.codePointCount(0, text.length) > MaxLength =>
        s"a promise over $MaxLength characters"
    }
```

Change `NegotiationTerms` and its doc comment to:

```scala
/** One author's terms. `citizenship` names the Exiles the author, the Grand
  * Scepter's holder, offers Citizenship (CR p. 34). `promises` and
  * `bindingPromises` are what the author writes they will do; the Game Log
  * records them and the engine enforces none (catalog batch 12, rulings 10
  * to 12). */
final case class NegotiationTerms(
    transfers: Vector[NegotiationTransfer] = Vector.empty,
    disclosures: Vector[NegotiationDisclosure] = Vector.empty,
    citizenship: Vector[PlayerId] = Vector.empty,
    promises: Vector[String] = Vector.empty,
    bindingPromises: Vector[String] = Vector.empty
):
  require(transfers.map(_.recipient).distinct.size == transfers.size,
    "Negotiation transfers must have one row per recipient")
  require(disclosures.distinct.size == disclosures.size,
    "Negotiation disclosures must be distinct")
  require(citizenship.distinct.size == citizenship.size,
    "Negotiation Citizenship offers must be distinct")
  require(NegotiationPromise.problem(promises).isEmpty,
    s"Negotiation promises hold ${NegotiationPromise.problem(promises)
      .getOrElse("")}")
  require(NegotiationPromise.problem(bindingPromises).isEmpty,
    s"Negotiation binding promises hold ${NegotiationPromise
      .problem(bindingPromises).getOrElse("")}")
```

Add the last field to `NegotiationBounds`, and one sentence to its doc
comment ("`bindingPromises` is whether the author may add binding promises:
false until The Tribunal opens them, catalog batch 12 slice 2."):

```scala
    citizenshipRefused: Option[String] = None,
    bindingPromises: Boolean = false
```

- [ ] **Step 4: Count promises as substance**

In `NegotiationDeal.scala`, `DealState.hasSubstance` becomes:

```scala
  def hasSubstance: Boolean = terms.values.exists(t =>
    t.transfers.exists(x => x.favor > 0 || x.relics.nonEmpty) ||
      t.disclosures.nonEmpty || t.citizenship.nonEmpty ||
      t.promises.nonEmpty || t.bindingPromises.nonEmpty)
```

- [ ] **Step 5: Refuse a closed binding promise**

In `DecisionQueries.acceptsTerms`, add as the last generator of the `for`:

```scala
          _ <- require(terms.bindingPromises.isEmpty || own.bindingPromises,
            decisionId, "offers a binding promise it cannot")
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.model.NegotiationPromiseSuite distantlands.gameplay.NegotiationDealSuite distantlands.gameplay.walker.DecisionQuerySuite distantlands.gameplay.NegotiationProcedureSuite distantlands.gameplay.NegotiationCitizenshipSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/model/NegotiationTerms.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationDeal.scala src/main/scala/distantlands/gameplay/walker/DecisionQueries.scala src/test/scala/distantlands/model/NegotiationPromiseSuite.scala src/test/scala/distantlands/gameplay/NegotiationDealSuite.scala src/test/scala/distantlands/gameplay/walker/DecisionQuerySuite.scala
git commit -m "feat(negotiation): an author's terms hold written promises" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 2: Promises on the journal and the command wire

**Files:**
- Modify: `src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandIntents.scala:47-51`
- Modify: `shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala:7-31`
- Modify: `src/main/scala/distantlands/application/GameIntentMapper.scala:52`
- Test: `src/test/scala/distantlands/serialization/WalkerDecisionWireSuite.scala`
- Test: `shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala`
- Test: `src/test/scala/distantlands/application/GameIntentMapperNegotiationSuite.scala`

**Interfaces:**
- Consumes: Task 1's `NegotiationTerms(..., promises, bindingPromises)` and
  `NegotiationPromise.problem`.
- Produces: wire `distantlands.protocol.NegotiationTerms(transfers,
  disclosures, citizenshipPlayerIds, promises: Vector[String] =
  Vector.empty, bindingPromises: Vector[String] = Vector.empty)`; JSON
  fields `"promises"` and `"bindingPromises"` (arrays of strings), required
  on both the journal and the command wire.

- [ ] **Step 1: Write the failing tests**

In `WalkerDecisionWireSuite`, "walker choice steps round trip the
negotiation answers": add the two promise vectors to `terms`, after
`Vector(blue)`:

```scala
      Vector(blue), Vector("I will not attack you."),
      Vector("I will give you 1 favor next turn."))
```

Add to `WalkerDecisionWireSuite`:

```scala
  test("journalled terms with a promise over the limit are refused"):
    val red = PlayerId("red")
    val event: OathEvent = WalkerStepRecorded("0.0.0", ChoicePayload(
      "negotiation.deal", DecisionAnswer.ProposeTerms(NegotiationTerms(
        promises = Vector("short"))), red), Vector.empty)
    val encoded = encodedStream("promise-limit", Vector(event))
    val broken = encoded.replace("\"short\"", "\"" + "x" * 161 + "\"")
    assert(broken != encoded, "the promise text must be in the stream")
    assert(GameEventWire.decodeStream(broken).isLeft, broken)
```

`encodedStream` is already imported from `WalkerWireEvents`.

In `CommandProtocolSuite`, change the `ProposeTermsWire` command in the
round-trip list to carry promises:

```scala
    ResolveWalker("negotiation.deal", DecisionAnswerWire.ProposeTermsWire(
      NegotiationTerms(Vector(NegotiationTransfer("blue", 2, Vector("r1"))),
        Vector(NegotiationDisclosure("blue",
          NegotiationInformation.HeldRelic("red", "r2"))), Vector("blue"),
        Vector("I will not attack you."), Vector("I will not attack you.")))),
```

Add to `GameIntentMapperNegotiationSuite`:

```scala
  test("promises are trimmed, and a promise over a limit is refused"):
    def terms(promises: Vector[String], binding: Vector[String] = Vector.empty) =
      Wire.ProposeTermsWire(distantlands.protocol.NegotiationTerms(
        Vector.empty, Vector.empty, Vector.empty, promises, binding))
    assertEquals(bound(terms(Vector("  I will not attack you.  "),
      Vector(" Binding. "))), Right(PlayerIntent.ResolveWalker(TreeDecision(
      "negotiation.deal", DecisionAnswer.ProposeTerms(NegotiationTerms(
        promises = Vector("I will not attack you."),
        bindingPromises = Vector("Binding."))))))
    assertEquals(bound(terms(Vector("x" * 161))), Left(
      GameIntentMappingFailure("$.intent.terms.promises",
        "a promise over 160 characters")))
    assertEquals(bound(terms(Vector.empty, Vector("   "))), Left(
      GameIntentMappingFailure("$.intent.terms.bindingPromises",
        "a blank promise")))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.serialization.WalkerDecisionWireSuite distantlands.protocol.CommandProtocolSuite distantlands.application.GameIntentMapperNegotiationSuite"`
Expected: compile error on the wire `NegotiationTerms`'s extra arguments.

- [ ] **Step 3: Add the wire fields**

In `CommandIntents.scala`:

```scala
final case class NegotiationTerms(
    transfers: Vector[NegotiationTransfer],
    disclosures: Vector[NegotiationDisclosure],
    citizenshipPlayerIds: Vector[String] = Vector.empty,
    promises: Vector[String] = Vector.empty,
    bindingPromises: Vector[String] = Vector.empty
)
```

In `CommandNestedCodecs.encodeNegotiation`, add after
`"citizenshipPlayerIds"`:

```scala
    "promises" -> ujson.Arr.from(value.promises.map(ujson.Str(_))),
    "bindingPromises" -> ujson.Arr.from(
      value.bindingPromises.map(ujson.Str(_))))
```

(moving the closing parenthesis from the `citizenshipPlayerIds` entry). In
`decodeNegotiation`, widen the `exact` set to
`Set("transfers", "disclosures", "citizenshipPlayerIds", "promises",
"bindingPromises")`, and add before `yield`:

```scala
    promiseValue <- field(root, "promises", path)
    promises <- strings(promiseValue, s"$path.promises")
    bindingValue <- field(root, "bindingPromises", path)
    bindingPromises <- strings(bindingValue, s"$path.bindingPromises")
  yield NegotiationTerms(transfers, disclosures, citizenship, promises,
    bindingPromises) }
```

The wire checks only shape. The limits are the mapper's (Step 5).

- [ ] **Step 4: Add the journal fields**

In `NegotiationTermsCodec.encode`, add after `"citizenship"`:

```scala
    "promises" -> ujson.Arr.from(terms.promises.map(ujson.Str(_))),
    "bindingPromises" -> ujson.Arr.from(terms.bindingPromises.map(ujson.Str(_))))
```

and in `decode` the `yield` becomes:

```scala
    yield NegotiationTerms(transfers, disclosures,
      value("citizenship").arr.toVector.map(v => PlayerId(v.str)),
      value("promises").arr.toVector.map(_.str),
      value("bindingPromises").arr.toVector.map(_.str))
```

A promise over a limit fails the model's `require`, which the existing
`NonFatal` catch turns into `InvalidValue`.

- [ ] **Step 5: Trim and check in the mapper**

In `GameIntentMapper.scala`, replace the one-line `negotiation` with:

```scala
  private def negotiation(value: distantlands.protocol.NegotiationTerms)
      : Result[distantlands.model.NegotiationTerms] = for
    ds <- traverse(value.disclosures)(disclosure)
    promises <- promised(value.promises, "$.intent.terms.promises")
    binding <- promised(value.bindingPromises,
      "$.intent.terms.bindingPromises")
  yield distantlands.model.NegotiationTerms(value.transfers.map(v =>
    distantlands.model.NegotiationTransfer(PlayerId(v.recipientPlayerId),
      v.favor, v.relicIds.map(RelicId.apply))), ds,
    value.citizenshipPlayerIds.map(PlayerId.apply), promises, binding)

  /** The client's promises, trimmed, or why they break a limit (catalog
    * batch 12, ruling 10). */
  private def promised(values: Vector[String], path: String)
      : Result[Vector[String]] =
    val trimmed = values.map(_.strip)
    NegotiationPromise.problem(trimmed).map(problem =>
      GameIntentMappingFailure(path, problem)).toLeft(trimmed)
```

`NegotiationPromise` comes in with the file's existing
`distantlands.model._` import; add it to the import if the file names model
types one by one.

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.serialization.* distantlands.protocol.* distantlands.application.GameIntentMapperNegotiationSuite distantlands.application.GameApplicationServiceReopenSuite"`
Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add src/main/scala/distantlands/serialization/NegotiationTermsCodec.scala shared/src/main/scala/distantlands/protocol/CommandIntents.scala shared/src/main/scala/distantlands/protocol/CommandNestedCodecs.scala src/main/scala/distantlands/application/GameIntentMapper.scala src/test/scala/distantlands/serialization/WalkerDecisionWireSuite.scala shared/src/test/scala/distantlands/protocol/CommandProtocolSuite.scala src/test/scala/distantlands/application/GameIntentMapperNegotiationSuite.scala
git commit -m "feat(negotiation): promises travel on the journal and the command wire" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 3: A Game Log line per promise

**Files:**
- Modify: `src/main/scala/distantlands/model/LogNotes.scala` (beside `NoteArg.Players`)
- Modify: `src/main/scala/distantlands/serialization/WalkerEventCodec.scala:126-156` and `:190-241`
- Modify: `src/main/scala/distantlands/application/gamelog/NoteLines.scala:132-158`
- Modify: `src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala`
- Modify: `src/test/scala/distantlands/gameplay/powers/NoteText.scala:45-64`
- Modify: `src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala`
- Modify: `src/test/scala/distantlands/application/gamelog/LogScripts.scala:45-46` and `:64-66`
- Create: `src/test/resources/gamelog/negotiation-promised.actor.log`, `src/test/resources/gamelog/negotiation-promised.other.log` (written by the golden suite)
- Test: `src/test/scala/distantlands/serialization/LogNoteWireSuite.scala`
- Test: `src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala`

**Interfaces:**
- Consumes: Task 1's `NegotiationTerms.promises` and `.bindingPromises`.
- Produces: `NoteArg.Promise(text: String)`, wire kind `"promise"` with
  field `"text"`; `NegotiationProcedure.promised` (key `"promised"`) and
  `NegotiationProcedure.promisedBinding` (key `"promised-binding"`), both
  `{author} … {others}: {promise}`; the script
  `NegotiationLogScripts.negotiationPromised`.

- [ ] **Step 1: Write the failing tests**

Add to `LogNoteWireSuite`:

```scala
  test("a log note's promise argument round trips its text"):
    val event = noteEvent(PowerSourceRef.Card(DenizenId("denizen")),
      NoteArg.Promise("I will not attack you, “ever”."))
    val encoded = accepted(GameEventWire.encodeEvent("notes", 0, event))
    assertEquals(GameEventWire.decode(encoded).map(_.event), Right(event))
```

Add to `NegotiationLogScripts`, after `negotiationAgreed`:

```scala
  /** A deal that holds nothing but the actor's promise: it may be accepted
    * and settles with the promise's line (catalog batch 12, ruling 13). */
  def negotiationPromised(using munit.Location): Script =
    val (service, parked, actor, partner) = negotiating("negotiation-promised")
    parked
      .parkedAfter(deal(actor, ProposeTerms(NegotiationTerms(promises =
        Vector("I will not attack you this round.")))))
      .parkedAfter(deal(partner, AcceptDeal))
      .after(deal(actor, AcceptDeal))
    Script("negotiation-promised", service, actor)
```

In `LogScripts`, add `negotiationPromised` to the
`NegotiationLogScripts` export, and to the named scripts after
`"negotiation-disclosed"`:

```scala
    "negotiation-promised" -> (() => negotiationPromised),
```

Add to `GameLogExchangeSuite`:

```scala
  test("an agreed promise is written once, quoted, naming its author and the others"):
    val script = negotiationPromised
    val expected = s"${playerLabel(script.actor)} promised " +
      s"${playerLabel(partner)}: “I will not attack you this round.”"
    Vector(None, Some(script.actor), Some(partner), Some(bystander))
      .foreach { viewer =>
        val all = texts(format(script, viewer).filter(_.depth == 1))
        assertEquals(all.count(_ == expected), 1, s"$viewer: $all")
      }
    assertEquals(lines(script).last, expected, lines(script))
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.serialization.LogNoteWireSuite distantlands.application.gamelog.GameLogExchangeSuite"`
Expected: compile error: `NoteArg.Promise` not found.

- [ ] **Step 3: Add the argument**

In `LogNotes.scala`, after `final case class Players(...)`:

```scala
  /** What a Negotiation author wrote they will do, as they wrote it
    * (catalog batch 12, ruling 12). The one argument that is text rather
    * than a reference: it names no card, so the log's knowledge rule has
    * nothing to judge. The Game Log quotes it as plain text. */
  final case class Promise(text: String) extends NoteArg
```

In `WalkerEventCodec.encodeNoteArg`, after the `NoteArg.Players` case:

```scala
    case NoteArg.Promise(text) => ujson.Obj("kind" -> "promise", "text" -> text)
```

and in `decodeNoteArg`, after the `"players"` case:

```scala
    case "promise" => Right(NoteArg.Promise(value("text").str))
```

In `NoteLines.argument`, after the `NoteArg.Players` case:

```scala
    case NoteArg.Promise(text) => Vector(Text(s"“$text”"))
```

In the test helper `NoteText.plain`, after the `NoteArg.Players` case:

```scala
    case NoteArg.Promise(text) => s"“$text”"
```

- [ ] **Step 4: Write the promise notes**

In `NegotiationProcedure`, declare after `becameChancellor`:

```scala
  /** "p1 promised p2 and p3: “…”" (catalog batch 12, ruling 12). */
  val promised: NoteKey = notes.key("promised", Vector(
    NotePart.Arg(0), NotePart.Text(" promised "), NotePart.Arg(1),
    NotePart.Text(": "), NotePart.Arg(2)))
  /** "p1 made a binding promise to p2: “…”" (ruling 11). */
  val promisedBinding: NoteKey = notes.key("promised-binding", Vector(
    NotePart.Arg(0), NotePart.Text(" made a binding promise to "),
    NotePart.Arg(1), NotePart.Text(": "), NotePart.Arg(2)))
```

In `tree`, after the `citizenship` value and before the final line, add:

```scala
        // Each promise's line, last: the deal's pieces have all moved and
        // its roles changed by now. The engine enforces none of them.
        val promises: Vector[Operation] =
          if !deal.agreed then Vector.empty
          else promisedLines(deal).map(note =>
            Note(ProcedureNotes.id(ActionRef.Negotiation), _ => Some(note)))
        opening ++ settlement ++ citizenship ++ promises
```

replacing `opening ++ settlement ++ citizenship`. Add the builder after
`offered`:

```scala
  /** One line per promise, by author in participant order, the plain
    * promises before the binding ones. Each names the other participants. */
  private def promisedLines(deal: DealState): Vector[LogNote] =
    deal.participants.flatMap { author =>
      val others = NoteArg.Players(deal.participants.filter(_ != author))
      val terms = deal.terms(author)
      terms.promises.map(text => promised.sourceless(NoteArg.Player(author),
        others, NoteArg.Promise(text))) ++
        terms.bindingPromises.map(text => promisedBinding.sourceless(
          NoteArg.Player(author), others, NoteArg.Promise(text)))
    }
```

Add `promises` to the procedure's layout comment at the top of the object:
`Branch(agreed -> BuildOps(settle), notes, Citizenship, promise notes)`.

- [ ] **Step 5: Run the tests, then write and review the golden files**

Run: `./sbtw "testOnly distantlands.serialization.LogNoteWireSuite distantlands.application.gamelog.GameLogExchangeSuite distantlands.application.gamelog.GameLogPropertiesSuite distantlands.gameplay.powers.DeclaredNotesSuite"`
Expected: PASS.

Run: `GAMELOG_GOLDEN=write ./sbtw "testOnly distantlands.application.gamelog.GameLogGoldenSuite"`
then `git status --short src/test/resources/gamelog`. Expected: only the
two new `negotiation-promised.*.log` files. Read both. Each must end with
the turn's "Negotiated with [player:p2|p2]" line followed by
"[player:p1|p1] promised [player:p2|p2]: “I will not attack you this
round.”", and hold no transfer line. If any other golden changed, stop and
find out why before going on.

Run: `./sbtw "testOnly distantlands.application.gamelog.*"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/model/LogNotes.scala src/main/scala/distantlands/serialization/WalkerEventCodec.scala src/main/scala/distantlands/application/gamelog/NoteLines.scala src/main/scala/distantlands/gameplay/actions/negotiation/NegotiationProcedure.scala src/test/scala/distantlands/gameplay/powers/NoteText.scala src/test/scala/distantlands/application/gamelog/NegotiationLogScripts.scala src/test/scala/distantlands/application/gamelog/LogScripts.scala src/test/scala/distantlands/serialization/LogNoteWireSuite.scala src/test/scala/distantlands/application/gamelog/GameLogExchangeSuite.scala src/test/resources/gamelog/negotiation-promised.actor.log src/test/resources/gamelog/negotiation-promised.other.log
git commit -m "feat(gamelog): an agreed deal writes one line per promise" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 4: Promises in the deal projection

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala:344-377`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala:404-456`
- Modify: `src/main/scala/distantlands/application/NegotiationDealProjector.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/frontend.scala:73-76` (type alias)
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala:286-305`
- Test: `src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala`

**Interfaces:**
- Consumes: Task 1's terms fields and `NegotiationBounds.bindingPromises`.
- Produces: `NegotiationPromiseProjection(authorPlayerId: String, text:
  String, binding: Boolean)`; `NegotiationDealProjection(...,
  citizenship, promises: Vector[NegotiationPromiseProjection] =
  Vector.empty)`; `NegotiationEditingProjection(..., citizenshipRefused,
  bindingPromisesOpen: Boolean = false)`; frontend alias
  `NegotiationPromiseState`.

- [ ] **Step 1: Write the failing tests**

In `ProjectionProtocolSuite`, "a negotiate query and a waiting deal round
trip with and without editing": give `editing`
`bindingPromisesOpen = true`, and append to `deal`'s arguments:

```scala
      Some(editing), Vector(NegotiationCitizenshipProjection("red", "blue")),
      Vector(NegotiationPromiseProjection("red", "I will not attack you.",
        binding = false), NegotiationPromiseProjection("blue",
        "I will give you 1 favor.", binding = true)))
```

and add `NegotiationPromiseProjection` to the suite's projection imports.

Add to `NegotiationDealProjectionSuite`:

```scala
  test("every viewer reads every promise; the binding box stays closed"):
    val terms = NegotiationTerms(promises = Vector("I will not attack you."))
    val state = parkedDeal(gathered, Vector(p2), Some(terms))
    Vector(p1, p2, p3).foreach { viewer =>
      assertEquals(deal(view(state, viewer)).promises, Vector(
        NegotiationPromiseProjection(p1.value, "I will not attack you.",
          binding = false)), viewer.value)
    }
    val public = projector.projectPublic("negotiation", LoadedGame(state, 30))
    assertEquals(deal(public).promises.map(_.text),
      Vector("I will not attack you."))
    val editing = deal(view(state, p2)).editing.getOrElse(fail("editing"))
    assert(!editing.bindingPromisesOpen)
```

and add `NegotiationPromiseProjection` to the suite's
`distantlands.protocol.projection` import.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "testOnly distantlands.protocol.ProjectionProtocolSuite distantlands.application.NegotiationDealProjectionSuite"`
Expected: compile error: `NegotiationPromiseProjection` not found.

- [ ] **Step 3: Add the DTOs**

In `ActionProjectionDtos.scala`, after `NegotiationCitizenshipProjection`:

```scala
/** One promise an author wrote into the deal, as they wrote it. `binding`
  * is whether it is a binding promise (catalog batch 12, rulings 10 and
  * 11). */
final case class NegotiationPromiseProjection(authorPlayerId: String,
    text: String, binding: Boolean)
```

Add `bindingPromisesOpen: Boolean = false` as the last field of
`NegotiationEditingProjection`, with the comment line "Whether the viewer
may add binding promises (The Tribunal)." Add
`promises: Vector[NegotiationPromiseProjection] = Vector.empty` as the last
field of `NegotiationDealProjection`, and add "and every promise" to its
doc comment's list of what everyone sees.

- [ ] **Step 4: Add the codec fields**

In `ActionProjectionCodec.encodeDeal`, add `"bindingPromisesOpen" ->
editing.bindingPromisesOpen` inside the editing object, and after
`"citizenship"`:

```scala
    "promises" -> encoded(value.promises)(promise => ujson.Obj(
      "authorPlayerId" -> promise.authorPlayerId, "text" -> promise.text,
      "binding" -> promise.binding)))
```

In `decodeDeal`, add `"promises"` to the deal's `exact` set and
`"bindingPromisesOpen"` to the editing's; read
`open <- bool(row, "bindingPromisesOpen", child)` in the editing block and
pass it as the editing projection's last argument; and before the final
`yield`:

```scala
    promiseRaws <- array(value, "promises", path)
    promises <- traverse(promiseRaws, s"$path.promises") { (raw, child) => for
      row <- obj(raw, child)
      _ <- exact(row, Set("authorPlayerId", "text", "binding"), child)
      author <- string(row, "authorPlayerId", child)
      text <- string(row, "text", child)
      binding <- bool(row, "binding", child)
    yield NegotiationPromiseProjection(author, text, binding) }
  yield NegotiationDealProjection(participants, accepted, transfers, disclosures,
    editing, offers, promises)
```

- [ ] **Step 5: Project them**

In `NegotiationDealProjector.project`, before the returned projection:

```scala
    val promises = query.participants.flatMap { author =>
      val terms = query.terms(author)
      terms.promises.map(NegotiationPromiseProjection(author.value, _,
        binding = false)) ++ terms.bindingPromises.map(
        NegotiationPromiseProjection(author.value, _, binding = true))
    }
```

and pass `promises` as the projection's last argument. In `editing`, pass
`bindingPromisesOpen = own.bindingPromises` as the last argument. Add to
the class comment: "Everyone reads every promise."

In `frontend.scala`, beside the other Negotiation aliases:

```scala
type NegotiationPromiseState = protocol.projection.NegotiationPromiseProjection
val NegotiationPromiseState = protocol.projection.NegotiationPromiseProjection
```

- [ ] **Step 6: Run the tests to verify they pass**

Run: `./sbtw "testOnly distantlands.protocol.* distantlands.application.NegotiationDealProjectionSuite distantlands.application.WalkerDecisionProjectionSuite"`
Expected: PASS. (If `WalkerDecisionProjectionSuite` does not exist, drop
it from the command.)

- [ ] **Step 7: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/ActionProjectionDtos.scala shared/src/main/scala/distantlands/protocol/projection/ActionProjectionCodec.scala src/main/scala/distantlands/application/NegotiationDealProjector.scala frontend/src/main/scala/distantlands/frontend/frontend.scala shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala src/test/scala/distantlands/application/NegotiationDealProjectionSuite.scala
git commit -m "feat(negotiation): every viewer is shown the deal's promises" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 5: The promise editor in the deal panel

**Files:**
- Create: `frontend/src/main/scala/distantlands/frontend/NegotiationPromiseEditor.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/GameClient.scala:66-68`
- Modify: `frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala:350-367`
- Test: `frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala`

**Interfaces:**
- Consumes: Task 4's `NegotiationPromiseState`, `deal.promises`,
  `editing.bindingPromisesOpen`; Task 2's wire `NegotiationTerms(...,
  promises, bindingPromises)`.
- Produces: `NegotiationPromiseEditor(heading: String, noun: String,
  initial: Vector[String], panel: dom.Element, sectionClass: String)` with
  `values: Vector[String]`; `NegotiationTermsInput(..., citizenshipPlayerIds,
  promises: Vector[String] = Vector.empty, bindingPromises: Vector[String]
  = Vector.empty)`.

- [ ] **Step 1: Write the failing tests**

Add to `NegotiationDealPanelSuite`:

```scala
  /** Red's deal with one promise from each player: red's plain one and
    * blue's binding one. */
  private def dealWithPromises(bindingOpen: Boolean) =
    val base = dealWhereBlueHasAccepted()
    base.copy(promises = Vector(
        NegotiationPromiseState("red", "I will not attack you.", binding = false),
        NegotiationPromiseState("blue", "I will give you 1 favor.", binding = true)),
      editing = base.editing.map(_.copy(bindingPromisesOpen = bindingOpen)))

  test("every viewer reads each promise as written"):
    val panel = render(spectator(dealWithPromises(bindingOpen = false)),
      new RecordingControls())
    assertEquals(Dom.all(panel, ".negotiation-promise").map(_.textContent),
      Vector("red promises: “I will not attack you.”",
        "blue makes a binding promise: “I will give you 1 favor.”"))

  test("the owner edits their promises, and Save sends them trimmed"):
    val ui = new RecordingControls()
    val panel = render(owner(dealWithPromises(bindingOpen = false)), ui)
    // Red's own promise is restored into its field; blue's is not.
    val restored = input(panel, ".negotiation-promise-item input[type=text]")
    assertEquals(restored.value, "I will not attack you.")
    assertEquals(restored.maxLength, 160)
    button(panel, ".negotiation-add-promise").click()
    val fields = Dom.all(panel, ".negotiation-promise-item input[type=text]")
    assertEquals(fields.length, 2)
    as[dom.html.Input](fields(1)).value = "  Also this.  "
    // No binding section while the bounds close it.
    assertEquals(Dom.all(panel, ".negotiation-binding-promises").length, 0)
    button(panel, ".negotiation-save").click()
    val sent = ui.submitted.collect {
      case Intent.ResolveWalker(_, DecisionAnswerWire.ProposeTermsWire(terms)) =>
        terms }
    assertEquals(sent.map(_.promises),
      Vector(Vector("I will not attack you.", "Also this.")))
    assertEquals(sent.map(_.bindingPromises), Vector(Vector.empty[String]))

  test("a removed or blank promise is not sent, and Add stops at five"):
    val ui = new RecordingControls()
    val panel = render(owner(dealWithPromises(bindingOpen = false)), ui)
    button(panel, ".negotiation-remove-promise").click()
    val add = button(panel, ".negotiation-add-promise")
    (1 to 5).foreach(_ => add.click())
    assert(add.disabled)
    assertEquals(Dom.all(panel,
      ".negotiation-promise-item input[type=text]").length, 5)
    button(panel, ".negotiation-save").click()
    val sent = ui.submitted.collect {
      case Intent.ResolveWalker(_, DecisionAnswerWire.ProposeTermsWire(terms)) =>
        terms.promises }
    assertEquals(sent, Vector(Vector.empty[String]))

  test("binding promises get their own section when the bounds open it"):
    val ui = new RecordingControls()
    val panel = render(owner(dealWithPromises(bindingOpen = true)), ui)
    val binding = Dom.one(panel, ".negotiation-binding-promises")
    as[dom.html.Button](Dom.one(binding, ".negotiation-add-promise")).click()
    as[dom.html.Input](Dom.one(binding, "input[type=text]")).value =
      "I will not attack you next round."
    button(panel, ".negotiation-save").click()
    val sent = ui.submitted.collect {
      case Intent.ResolveWalker(_, DecisionAnswerWire.ProposeTermsWire(terms)) =>
        terms.bindingPromises }
    assertEquals(sent, Vector(Vector("I will not attack you next round.")))
```

If `Dom.all` returns a JS collection rather than a `Vector`, convert with
`.toVector` where these tests compare it, as the suite's existing tests do.

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./sbtw "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite"`
Expected: FAIL: no `.negotiation-promise` lines and no promise editor.

- [ ] **Step 3: Write the editor**

Create `frontend/src/main/scala/distantlands/frontend/NegotiationPromiseEditor.scala`:

```scala
package distantlands.frontend

import org.scalajs.dom

/** The editor of one author's promises of one kind (catalog batch 12,
  * slice 1): a one-line text field per promise, with its character count
  * and a Remove button, and an Add button while fewer than `MaxPerKind`
  * stand. `values` leaves blank fields out and trims the rest; the server
  * refuses what breaks the other limits. The text is only ever a field's
  * value, never markup.
  */
private[frontend] final class NegotiationPromiseEditor(heading: String,
    noun: String, initial: Vector[String], panel: dom.Element,
    sectionClass: String):
  import NegotiationPromiseEditor.{MaxLength, MaxPerKind}
  import ServerUiSupport.{button, text}

  private val section = text("div", sectionClass, "")
  private val list = text("div", "negotiation-promise-list", "")
  private val fields =
    scala.collection.mutable.ArrayBuffer.empty[dom.html.Input]
  private val adder = button(s"Add $noun", "negotiation-add-promise")

  section.appendChild(text("h3", "", heading))
  section.appendChild(list)
  section.appendChild(adder)
  panel.appendChild(section)
  adder.onclick = _ => add("")
  initial.foreach(add)
  refresh()

  def values: Vector[String] =
    fields.toVector.map(_.value.strip).filter(_.nonEmpty)

  private def add(value: String): Unit =
    val row = text("div", "negotiation-item negotiation-promise-item", "")
    val field =
      dom.document.createElement("input").asInstanceOf[dom.html.Input]
    field.`type` = "text"
    field.maxLength = MaxLength
    field.value = value
    val count = text("span", "negotiation-promise-count", "")
    val counted = () => count.textContent = s"${field.value.length}/$MaxLength"
    field.oninput = _ => counted()
    counted()
    val remove = button("Remove", "negotiation-remove-promise")
    remove.onclick = _ =>
      fields -= field
      list.removeChild(row)
      refresh()
    row.appendChild(field)
    row.appendChild(count)
    row.appendChild(remove)
    list.appendChild(row)
    fields += field
    refresh()

  /** Numbers the fields' labels and closes Add at the limit. */
  private def refresh(): Unit =
    adder.disabled = fields.size >= MaxPerKind
    fields.zipWithIndex.foreach((field, index) =>
      field.setAttribute("aria-label", s"$noun ${index + 1}"))

private[frontend] object NegotiationPromiseEditor:
  /** The server's limits (`NegotiationPromise` in the model). */
  val MaxLength: Int = 160
  val MaxPerKind: Int = 5
```

- [ ] **Step 4: Wire it into the panel and the input**

In `GameClient.scala`:

```scala
final case class NegotiationTermsInput(transfers: Vector[NegotiationTransferInput],
    disclosures: Vector[NegotiationDisclosureInput],
    citizenshipPlayerIds: Vector[String] = Vector.empty,
    promises: Vector[String] = Vector.empty,
    bindingPromises: Vector[String] = Vector.empty)
```

In `ServerUiSupport.protocolNegotiationTerms`, the last argument
`value.citizenshipPlayerIds)` becomes:

```scala
      value.citizenshipPlayerIds, value.promises, value.bindingPromises)
```

In `NegotiationDealPanel.summary`, after the Citizenship lines:

```scala
    deal.promises.foreach(promise => panel.appendChild(text("p",
      "negotiation-promise", s"${promise.authorPlayerId} " +
        s"${if promise.binding then "makes a binding promise" else "promises"}" +
        s": “${promise.text}”")))
```

In `editor`, after the per-recipient loop and before `val save`:

```scala
    def own(binding: Boolean): Vector[String] = deal.promises.collect {
      case promise if promise.authorPlayerId == me &&
          promise.binding == binding => promise.text }
    val promises = new NegotiationPromiseEditor("Your promises", "Promise",
      own(binding = false), panel, "negotiation-promises")
    val binding = Option.when(editing.bindingPromisesOpen)(
      new NegotiationPromiseEditor("Your binding promises",
        "Binding promise", own(binding = true), panel,
        "negotiation-binding-promises"))
```

and in `save.onclick`, the `NegotiationTermsInput(...)` gets two more
arguments after the Citizenship vector:

```scala
          case (recipient, check) if check.checked => recipient }.toVector,
        promises.values, binding.fold(Vector.empty[String])(_.values))
```

Update the panel's class comment: "An owner also writes their promises,
and binding promises when the bounds open them."

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./sbtw "frontend/testOnly distantlands.frontend.NegotiationDealPanelSuite distantlands.frontend.ParkedDecisionSuite distantlands.frontend.WalkerSelectionDraftSuite"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/NegotiationPromiseEditor.scala frontend/src/main/scala/distantlands/frontend/NegotiationDealPanel.scala frontend/src/main/scala/distantlands/frontend/GameClient.scala frontend/src/main/scala/distantlands/frontend/ServerUiSupport.scala frontend/src/test/scala/distantlands/frontend/NegotiationDealPanelSuite.scala
git commit -m "feat(frontend): write promises into a deal" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

---

### Task 6: Full verification and the spec's status

**Files:**
- Modify: `docs/superpowers/specs/2026-11-09-catalog-batch-12-design.md:3`

- [ ] **Step 1: Run every check**

Run: `./sbtw clean coverage test coverageReport`
Expected: PASS, statement coverage at or above 92%.

Run: `./sbtw "frontend/test"`
Expected: PASS.

Run: `python3 scripts/check-architecture.py && python3 scripts/check-markdown-links.py`
Expected: both pass.

- [ ] **Step 2: Check the panel in the browser**

Start a scratch server on port 8081 against a scratch database (never the
main `var/` database), open a game, take a Negotiation, add a promise as
one seat and accept as both, and confirm the panel shows the promise and
the Game Log shows its quoted line. Screenshot both.

- [ ] **Step 3: Mark the slice in the spec**

Change the spec's status line to:

```markdown
**Status:** designed 2026-11-09. Slice 1 landed.
```

- [ ] **Step 4: Commit**

```bash
git add docs/superpowers/specs/2026-11-09-catalog-batch-12-design.md
git commit -m "docs(catalog): close catalog batch 12 slice 1, promises" -m "Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>"
```

## Known limits

- The binding-promise section never opens in this slice: no power sets
  `NegotiationBounds.bindingPromises` until slice 2 (The Tribunal).
- Promises are public. Whispering Stone's private promise lines and
  `LogNote.readers` are slice 5's.
