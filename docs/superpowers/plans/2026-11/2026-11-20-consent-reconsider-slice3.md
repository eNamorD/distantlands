# Reconsidering a Consent Answer, Slice 3: Requests List Implementation Plan

> Executed inline by the implementer. Steps use checkbox (`- [ ]`) syntax
> for tracking.

**Goal:** The overlay of a power the viewer rules lists, under the
preference rows, the turn's requests about that power which the viewer
answered. Each entry shows the answer and one button that changes it. The
list stays live while the overlay is open, and the granter's prompt says
where to change the answer.

**Architecture:** The server projects the rows:
`ConsentPreferencesProjection` gains `requests`, built in
`ConsentProjector` from the ledger's history. One judgement,
`ConsentChange`, says whether an answer may be changed now and why not;
the desk refuses by it and the projection words it, so the two cannot
drift. On the client `SitePowerLinks.gridOf`, which already builds the
column for every overlay, adds the rows. `ConsentRequestsView` draws the
block, and `CardInspectionOverlay` redraws only that block from each
snapshot, which `TableScreen` hands it through the shell, as it hands
Chronicle Details its snapshot. The rest of the overlay still ignores
polls.

**Tech Stack:** Scala 3.9.0, Scala.js frontend, munit, jsdom
(`TestBrowser`), CSS, `./sbtw` (sbt wrapper), `-Werror`.

**Spec:** [Reconsidering a consent answer](../../specs/2026-11/2026-11-20-consent-reconsider-design.md),
sections "Projection" and "Frontend" ("Requests list", "Live while open",
"Prompt notice") and "Slices", item 3. Rulings so far are in the
[implementation rulings](../../specs/2026-11/2026-11-20-consent-reconsider-design-implementation-rulings.md);
new ones go there from R12. Read `DESIGN.md` ("Buttons", "Hover Note",
"Consent Column", "Do's and Don'ts") and
`/Users/roman/.claude/skills/impeccable/reference/craft-floor.md` before
the view and CSS tasks.

## Global Constraints

- The strings, word for word:
  - the block's heading: "Requests this turn";
  - line 1: "Blue asked to select Deep Woods";
  - the small second line: "Also answers Secret Police";
  - the states: "Refused", "Allowed" and "Allowed, used";
  - the button: "Allow" on a refusal, "Refuse" on a grant;
  - the hover note of a locked entry: "Blue has acted on this consent. It
    unlocks if Blue undoes that.";
  - an automatic power's entry: "Blue's Tyrant: deactivated" or "Blue's
    Tyrant: let work", its button withheld with the hover note "This
    answer cannot be changed.";
  - R8's waiting entry: its button withheld with the hover note "A
    request about this option is waiting for an answer.";
  - the prompt notice: "You can change this answer, and your consent
    preferences, by clicking on the Narrow Pass power." An automatic
    power's prompt keeps "You can adjust your consent preferences by
    clicking on the City Wall power."
- The block stands under the preference rows and is absent when the power
  has no requests this turn. One entry per request, newest first.
- A click sends `ReconsiderConsent` with no confirmation step.
- A locked entry keeps its button, in the Withheld face
  (`aria-disabled="true"`): it takes focus and a tap, both show its
  reason in the hover note, and it sends nothing. The reason is never
  printed beside it.
- Without control the buttons are disabled, as the settings are.
- No accent color in the list: consent amber stays on "ask first", and
  selection blue on the pressed setting. No `transition`, no `animation`.
  Touch targets are 44px through the existing `@media (pointer: coarse)`
  rule. Functional text is never under 11px.
- Live while open: the overlay ignores polls, so a setting just chosen
  stays shown. The requests list is the exception: it is redrawn from
  each projection, so an entry locks in place when the requester acts.
  When the server refuses a change because the entry locked first, the
  redraw shows the locked entry.
- Only the viewer's own answers leave the server (ruling 16). A row never
  names an option the viewer may not identify.
- `requests` is required on the wire inside each `consentPreferences`
  entry, and every key of a row is required. Old clients and snapshots
  need no support.
- R8: an entry with a waiting request about the same option carries the
  `change` value `waiting`.
- Protected (do not change): the preference rows' structure, strings and
  behaviour from slice 1; the desk's refusal reasons and their order from
  slice 2 (`ConsentReconsiderServiceSuite` stays green unchanged); every
  `CardInspection.Request` case and every existing
  `CardInspectionOverlay` method's behaviour apart from drawing the
  column's new block; the prompt's question and answers; every existing
  log line and golden log.
- Work in
  `/Users/roman/projects/distantlands/.worktrees/consent-reconsider` on
  branch `consent-reconsider`. `.tooling` and `node_modules` are symlinks;
  never commit them. Never commit on `0.2.0-alpha` or `main`. Push
  nothing, merge nothing.
- Stage files by name. The hook blocks `git add -A`, `git add -u`,
  `git add .`, `git add --all`, `git commit -a` and `git stash`.
- Commits use Conventional Commits with a scope and end with
  `Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>`. Run
  `git log -3` after each commit and report any commit that is not ours.
- Code, comments, docs and commit messages are plain English prose. Wrap
  Scala and Markdown at 80 columns. Scaladoc cites "reconsider design".
- Test helper names state what they build.
- Never write a bare `===` or `=word` in a shell command. Quote globs.
- Browser check: a scratch server from this worktree on port 8081 against
  a scratch database under the session scratchpad, never `var/distantlands`
  or `var/oathdigital`, never `preview_start`. One bounded pass.
- Claim evidence in reports: tag each claim *gate*, *read* or *UNKNOWN*.

## What the code says (read at `559ac66fd`)

**Every overlay that draws the column gets its rows from one place.**
Five paths open an overlay with the column, and each builds its
`ConsentGrid` through `SitePowerLinks.gridOf(preferences)`:

| Overlay | Opened by | Grid from |
|---|---|---|
| site power | `CardInspection.openPower`, a site's power label | `links.grid(power)` |
| ruled card on a site | `CardInspection.openRuled`, the card's face | `links.cardGrid(card)` |
| ruled card on a player board | the same, an adviser or relic | `links.cardGrid(card)` |
| the banner's Festival face | `CardInspection.openFestival` | `links.bannerGrid(key)` |
| the festival marker | `CardInspection.openFestivalMarker` | `links.festivalGrid` |

Each finds one `ConsentPreferencesState` and hands it to `gridOf`. The
rows ride on that state, so `gridOf` puts them in the grid and
`ConsentGridView.render` draws them: no opener changes, and no overlay
can miss the list.

**The overlay and polls.** `CardInspectionOverlay` draws once and never
reads a projection again. `TableScreen.render` runs on every snapshot and
already hands each one to Chronicle Details
(`shell.showChronicleDetails(projection)`), which redraws through
`PanelContent.replace`, keeping scroll and focus. `PanelContent` finds a
rebuilt control by its tag, label and text, so it loses a button whose
text changes from "Allow" to "Refuse". The requests list follows the
same pattern with its own key: an entry is found again by its requester,
decision and option.

**A refused command.** `TableSession.submitCommand` records a rejection
as the session's failure and repaints. `TableScreen` prints it as the
Actions pane's error line (`.status.error`, `role="alert"`), in the
server's words: for the desk, the `ConsentUnavailable` reason. The
session stays connected and polls on, so the next snapshot redraws the
list. While the overlay is open that line is behind the scrim; a screen
reader announces it.

**The option's label.** The prompt reads its option through
`ConsentProjector.target`: a button's label from the parked decision of
that id, any other option through `DecisionOptionRows.project` for the
viewer, and the raw wire id when neither names it. The log's fallback for
a button whose decision is no longer parked is
`GamePresentationProjector.safeLabel(key)` (`LogWords.label`).
`DecisionOptionRows.project` names a card only when
`identifiesCard` lets this viewer name it, at the current state.

**The Withheld face.** `HoverTip.withhold(control, reason)` sets
`aria-disabled="true"`, stores the reason as the hover note and removes
the click handler. The note shows on a resting mouse, on keyboard focus
and on a tap. Its element is one for the whole document, above the
overlays (`z-index: 400`).

## Rulings

Recorded in the
[implementation rulings](../../specs/2026-11/2026-11-20-consent-reconsider-design-implementation-rulings.md).
Where a task's code below was written before a ruling, the ruling wins;
each such place is named here.

- **R14 (slice 2 review).** The desk now checks the waiting request
  first. `ConsentChange.of` judges in the same order: `Waiting`,
  `Automatic`, `Lapsed`, `Used`, `Open`. Task 2.
- **R16.** `.consent-grid { flex: 1 1 17rem; max-width: 22rem; }`. The
  browser check measures the settings' thirds again at both widths;
  beside the details at desktop width the column must keep its content
  width. Tasks 6 and 8.
- **R17.** A fifth `change` value, `lapsed`, withheld with "You no longer
  rule every power this answer covers." Tasks 2 and 3, as written.
- **R18.** An automatic power's entry: its sentence on line 1, the
  withheld button alone on line 2, reading "Let it work" on a
  deactivation and "Deactivate" on a let-work. Task 3, as written.
- **R19.** An entry whose option the viewer may not identify is not
  listed. In Task 2, `optionLabel` returns an `Option` and `requests`
  drops a row with none; the test "a card the viewer may not identify
  ..." expects no row. No "a hidden card" label exists.
- **R20.** Nothing beyond the redraw; a clicked button stays disabled
  until the next snapshot. Tasks 3 and 4, as written.
- **R21.** `ConsentPromptProjection` gains the required field `adjust`
  in place of the planned `changeable`: `answer` (the new notice),
  `preferences` (the old notice, an automatic power's prompt) or `none`
  (some power of the ask has no granter: no notice element). Tasks 1, 2
  and 5 use `adjust`. This changes four existing prompts, a payer's and
  an Imperial law's, which lose their notice.
- **R22.** Each entry is a `role="group"` whose `aria-label` is its line
  1 text. Task 3.
- **R23.** The list words an option from the current state. Task 2, as
  written.

## Decisions already made in this plan

- The row is `ConsentRequestRowProjection(requesterPlayerId, decisionId,
  optionKind, optionId, label, answer, alsoPowers, use, change)`.
  `answer` is `allowed` or `refused`. `use` is the card's name for an
  automatic power, else `null`. `requests` is the last parameter of
  `ConsentPreferencesProjection` with a Scala default of `Vector.empty`,
  and required on the wire, as slice 1 of Chronicle Details did (its R4).
- The `change` precedence is the desk's order: `automatic`, `lapsed`,
  `used`, `waiting`, `open`. The client treats a value it does not know
  as not changeable, with the note "This answer cannot be changed."
- A refused entry whose option was used (answers of one option lock
  together) reads "Refused", with "Allow" withheld and the locked note.
  "Allowed, used" is the state of a granted, used entry only.
- Without control, an `open` entry's button is disabled; a withheld
  button stays withheld, so its reason can still be read.
- The list is redrawn only when its rows differ from what is drawn, or
  after a click. A redraw on every poll would close an open hover note
  each time.
- The change button has its own class, `consent-request-change`, added to
  the Control-face selector lists. Slice 1's stylesheet tests pin three
  of those lists as strings; Task 6 updates the three strings and nothing
  else in those tests.
- The hover rule's scope becomes `:not(:disabled, [aria-disabled="true"])`
  for the change button, the pattern the Actions pane uses.
- The "Also answers" line is Ink Dim at `max(0.85em, 0.7rem)`: 11.2px at
  360px, on the Eleven-Pixel Floor.
- `ConsentChange` is extracted from `ConsentDesk.reconsider`. The desk's
  behaviour does not change; its suite is the proof.

## Files

- Modify: `shared/src/main/scala/distantlands/protocol/projection/ConsentProjectionDtos.scala`,
  `ConsentProjectionCodec.scala`
- Modify: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`
- Create: `src/main/scala/distantlands/application/ConsentChange.scala`
- Modify: `src/main/scala/distantlands/application/ConsentDesk.scala`,
  `ConsentProjector.scala`
- Modify: `src/test/scala/distantlands/application/ConsentProjectionSuite.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/frontend.scala`
  (aliases), `SitePowerLinks.scala`, `CardInspectionOverlay.scala`,
  `GameTableShell.scala`, `TableScreen.scala`, `ConsentPanels.scala`
- Create: `frontend/src/main/scala/distantlands/frontend/ConsentRequestsView.scala`
- Create: `frontend/src/test/scala/distantlands/frontend/ConsentRequestsViewSuite.scala`,
  `ConsentRequestsLiveSuite.scala`
- Modify: `frontend/src/test/scala/distantlands/frontend/ConsentPromptPanelSuite.scala`
- Modify: `frontend/styles.css`,
  `src/test/scala/distantlands/server/StylesheetConsentGridSuite.scala`
- Modify docs: `DESIGN.md` ("Consent Column"), the spec's status, the
  implementation rulings.

## Running things

- Shared and server suites run in the root project:
  `./sbtw "testOnly distantlands.protocol.ProjectionProtocolSuite distantlands.application.ConsentProjectionSuite"`.
- Frontend suites:
  `./sbtw "frontend/testOnly distantlands.frontend.ConsentRequestsViewSuite distantlands.frontend.ConsentRequestsLiveSuite"`.
- Full gate at slice end, in the background or with a long timeout:
  `./sbtw test`, `./sbtw frontend/test`,
  `python3 scripts/check-architecture.py`,
  `python3 scripts/check-markdown-links.py`.

## Re-inventory before each task

The inventory in this plan was read at `559ac66fd`. Each task begins by
running its grep at HEAD. Treat what the grep shows as the list; handle a
hit missing from the task by the task's rule, and report it.

---

### Task 1: The rows on the wire

**Files:**
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ConsentProjectionDtos.scala`
- Modify: `shared/src/main/scala/distantlands/protocol/projection/ConsentProjectionCodec.scala`
- Test: `shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala`

**Interfaces:**
- Produces, in `distantlands.protocol.projection`:
  `final case class ConsentRequestRowProjection(requesterPlayerId: String, decisionId: String, optionKind: String, optionId: String, label: String, answer: String, alsoPowers: Vector[String], use: Option[String], change: String)`;
  `ConsentPreferencesProjection.requests: Vector[ConsentRequestRowProjection] = Vector.empty`
  (last parameter);
  `ConsentPromptProjection.changeable: Boolean = false` (last parameter).

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -e '"bannerKey"' -e '"consentPrompt":{' -- src/test shared/src/test frontend/src/test
```
Expected: no hand-written JSON of a preferences entry or a prompt in any
suite (the suites build them through the case classes). A hit gains
`"requests":[]` or `"changeable":false` in Step 4.

- [ ] **Step 2: Write the failing tests**

In `ProjectionProtocolSuite`, in the test "the consent parts of a
projection round-trip", replace the first `ConsentPreferencesProjection`

```scala
      consentPreferences = Vector(ConsentPreferencesProjection(
        "site.narrow-pass.pass", "Narrow Pass",
        Vector(ConsentPreferenceRowProjection("p1", "always"))),
```

with

```scala
      consentPreferences = Vector(ConsentPreferencesProjection(
        "site.narrow-pass.pass", "Narrow Pass",
        Vector(ConsentPreferenceRowProjection("p1", "always")),
        requests = Vector(
          ConsentRequestRowProjection("p1", "travel.destination", "site",
            "site:dunes", "Dunes", "refused", Vector("Secret Police"), None,
            "open"),
          ConsentRequestRowProjection("p1", "power-use.tyrant", "button",
            "use", "Use", "allowed", Vector.empty, Some("Tyrant"),
            "automatic"))),
```

and in the same test give the prompt `changeable = true`:

```scala
      consentPrompt = Some(ConsentPromptProjection("p1",
        Vector("Narrow Pass"), "Dunes", use = Some("Tyrant"),
        changeable = true)),
```

Add after that test:

```scala
  test("a preferences entry requires its requests, and a row every key"):
    val whole = ujson.read(GameProjectionCodec.encode(projection.copy(
      consentPreferences = Vector(ConsentPreferencesProjection(
        "site.narrow-pass.pass", "Narrow Pass", Vector.empty,
        requests = Vector(ConsentRequestRowProjection("p1",
          "travel.destination", "site", "site:dunes", "Dunes", "refused",
          Vector.empty, None, "open")))))))
    def refused(edit: ujson.Value => Unit): Boolean =
      val raw = ujson.read(ujson.write(whole))
      edit(raw("consentPreferences")(0))
      GameProjectionCodec.decode(ujson.write(raw)).isLeft
    assert(refused(_.obj.remove("requests")))
    Vector("requesterPlayerId", "decisionId", "optionKind", "optionId",
      "label", "answer", "alsoPowers", "use", "change").foreach(key =>
      assert(refused(_("requests")(0).obj.remove(key)), key))
    assert(refused(_("requests")(0)("extra") = 1))

  test("a prompt requires whether its answer can be changed"):
    val raw = ujson.read(GameProjectionCodec.encode(projection.copy(
      consentPrompt = Some(ConsentPromptProjection("p1",
        Vector("Narrow Pass"), "Dunes")))))
    raw("consentPrompt").obj.remove("changeable")
    assert(GameProjectionCodec.decode(ujson.write(raw)).isLeft)
```

- [ ] **Step 3: Run the suite to verify it fails**

Run: `./sbtw "testOnly distantlands.protocol.ProjectionProtocolSuite"`
Expected: a compile error, `Not found: ConsentRequestRowProjection`.

- [ ] **Step 4: Write the implementation**

In `ConsentProjectionDtos.scala`, give the prompt its last parameter and
doc sentence:

```scala
/** ... `changeable` when the granter may change this answer later in the
  * turn from the power's overlay (reconsider design, "Prompt notice";
  * implementation ruling on Q6). */
final case class ConsentPromptProjection(requesterPlayerId: String,
    powers: Vector[String], target: String, paying: Boolean = false,
    use: Option[String] = None, changeable: Boolean = false)
```

(keep the existing doc text and add the sentence), and replace the
preferences declaration with:

```scala
/** A waivable power the viewer rules, with their setting for each other
  * player: `never`, `case-by-case` or `always`. Only ever the viewer's own
  * (ruling 16). `cardId` names the card that prints it, when a card does, so
  * the card can open the grid (consent slice 3), and `bannerKey` the banner
  * that prints it, so the banner can. `requests` is the turn's requests
  * about this power that the viewer answered, newest first (reconsider
  * design, "Projection"). */
final case class ConsentPreferencesProjection(powerId: String, power: String,
    rows: Vector[ConsentPreferenceRowProjection],
    cardId: Option[String] = None, bannerKey: Option[String] = None,
    requests: Vector[ConsentRequestRowProjection] = Vector.empty)
final case class ConsentPreferenceRowProjection(playerId: String,
    setting: String)

/** One request of this turn that the viewer answered (reconsider design,
  * "Projection"). `requesterPlayerId`, `decisionId`, `optionKind` and
  * `optionId` are what `ReconsiderConsent` sends back. `label` reads the
  * option as the prompt read it. `answer` is `allowed` or `refused`.
  * `alsoPowers` names the viewer's other powers the same ask covered.
  * `use` is the card whose automatic power the request was about.
  * `change` says whether the answer may be changed now: `open`, `used`
  * (the requester acted on it), `automatic`, `waiting` (a request about
  * the same option waits) or `lapsed` (the viewer lost a power the answer
  * covers). */
final case class ConsentRequestRowProjection(requesterPlayerId: String,
    decisionId: String, optionKind: String, optionId: String, label: String,
    answer: String, alsoPowers: Vector[String], use: Option[String],
    change: String)
```

In `ConsentProjectionCodec.scala`:

`encodePrompt` writes the flag always:

```scala
    val row = ujson.Obj(
      "requesterPlayerId" -> value.requesterPlayerId,
      "powers" -> encoded(value.powers)(ujson.Str(_)),
      "target" -> value.target, "changeable" -> value.changeable)
```

`decodePrompt` requires it:

```scala
    _ <- exact(row, Set("requesterPlayerId", "powers", "target", "paying",
      "use", "changeable"), path)
```

with `changeable <- bool(row, "changeable", path)` after `use`, and the
`yield` passing it as the last argument.

`encodePreferences` gains

```scala
      "requests" -> encoded(value.requests)(row => ujson.Obj(
        "requesterPlayerId" -> row.requesterPlayerId,
        "decisionId" -> row.decisionId, "optionKind" -> row.optionKind,
        "optionId" -> row.optionId, "label" -> row.label,
        "answer" -> row.answer,
        "alsoPowers" -> encoded(row.alsoPowers)(ujson.Str(_)),
        "use" -> stringOption(row.use), "change" -> row.change))
```

and `decodePreferences` becomes:

```scala
  def decodePreferences(raw: ujson.Value, path: String)
      : Result[ConsentPreferencesProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, Set("powerId", "power", "rows", "cardId", "bannerKey",
      "requests"), path)
    powerId <- string(row, "powerId", path)
    power <- string(row, "power", path)
    rowRaws <- array(row, "rows", path)
    rows <- traverse(rowRaws, s"$path.rows") { (entryRaw, child) => for
      entry <- obj(entryRaw, child)
      _ <- exact(entry, Set("playerId", "setting"), child)
      player <- string(entry, "playerId", child)
      setting <- string(entry, "setting", child)
    yield ConsentPreferenceRowProjection(player, setting) }
    cardId <- optionalAbsent(row, "cardId", path)(string)
    bannerKey <- optionalAbsent(row, "bannerKey", path)(string)
    requestRaws <- array(row, "requests", path)
    requests <- traverse(requestRaws, s"$path.requests")(decodeRequest)
  yield ConsentPreferencesProjection(powerId, power, rows, cardId, bannerKey,
    requests)

  /** Every key is required; `use` is null when the request was about no
    * automatic power. */
  private def decodeRequest(raw: ujson.Value, path: String)
      : Result[ConsentRequestRowProjection] = for
    row <- obj(raw, path)
    _ <- exact(row, Set("requesterPlayerId", "decisionId", "optionKind",
      "optionId", "label", "answer", "alsoPowers", "use", "change"), path)
    requester <- string(row, "requesterPlayerId", path)
    decisionId <- string(row, "decisionId", path)
    kind <- string(row, "optionKind", path)
    id <- string(row, "optionId", path)
    label <- string(row, "label", path)
    answer <- string(row, "answer", path)
    also <- strings(row, "alsoPowers", path)
    use <- optional(row, "use", path)(string)
    change <- string(row, "change", path)
  yield ConsentRequestRowProjection(requester, decisionId, kind, id, label,
    answer, also, use, change)
```

`array(row, "requests", path)` and `string(row, name, path)` refuse a
missing key, and `optional` requires the key and accepts `null`
(`ProjectionCodecSupport`). If `exact` only refuses unknown keys, the
tests still hold through those readers.

- [ ] **Step 5: Run the protocol suites to verify they pass**

Run:
`./sbtw "testOnly distantlands.protocol.* distantlands.application.ConsentProjectionSuite"`
then `./sbtw "frontend/testOnly distantlands.frontend.HttpGameClientSuite"`.
Expected: all pass. A frontend suite that feeds hand-written snapshot
JSON with a prompt or a preferences entry fails here; it gains the new
keys and is reported.

- [ ] **Step 6: Commit**

```bash
git add shared/src/main/scala/distantlands/protocol/projection/ConsentProjectionDtos.scala \
  shared/src/main/scala/distantlands/protocol/projection/ConsentProjectionCodec.scala \
  shared/src/test/scala/distantlands/protocol/ProjectionProtocolSuite.scala
git commit -m "feat(protocol): carry a power's requests of the turn to its ruler

Each preferences entry gains requests, and the prompt says whether its
answer can be changed later. Every new key is required.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 2: The server projects the rows

**Files:**
- Create: `src/main/scala/distantlands/application/ConsentChange.scala`
- Modify: `src/main/scala/distantlands/application/ConsentDesk.scala`
  (`reconsider`)
- Modify: `src/main/scala/distantlands/application/ConsentProjector.scala`
- Test: `src/test/scala/distantlands/application/ConsentProjectionSuite.scala`

**Interfaces:**
- Consumes: Task 1's DTOs; slice 2's `ConsentState.historyAt`,
  `ConsentUseMarks.locked`.
- Produces: `private[application] enum ConsentChange { Open, Automatic, Lapsed(power: PowerId), Used, Waiting }`
  with `ConsentChange.of(ready: ReadyGame, powers: ConsentPowers, granter: PlayerId, entry: ConsentRequest, ask: ConsentAsk): ConsentChange`
  and `def key: String`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -e "ConsentPromptProjection(" -e "ConsentPreferencesProjection(" -- src/main/scala
```
Expected: both only in `ConsentProjector.scala`.

- [ ] **Step 2: Write the failing tests**

Add to `ConsentProjectionSuite` the imports
`distantlands.gameplay.actions.travel.TravelProcedure` and
`distantlands.gameplay.powers.poweruse.CityWall` (already there), and:

```scala
  private val destination = TravelProcedure.destinationDecisionId

  private def requestsShownTo(viewer: PlayerId, at: GameAccepted)
      : Vector[ConsentRequestRowProjection] =
    view(at, viewer).consentPreferences.flatMap(_.requests)

  private def changing(service: GameApplicationService, at: GameAccepted,
      allow: Boolean)(using munit.Location): GameAccepted =
    accepted(service.handle(gameId, at.nextSequence,
      Commands.reconsiderConsent(p2, p1, destination, dunes, allow)))

  /** `base` projected for p2 with `ledger` as its consent ledger: a
    * position the service cannot be played to. */
  private def shownToP2With(base: ReadyGame, ledger: ConsentState)
      : Vector[ConsentPreferencesProjection] =
    projector.project(gameId, LoadedGame(OathState.Ready(
      base.copy(consent = ledger)), 0L), p2).consentPreferences

  private def answeredBy(asks: Vector[ConsentAsk], target: DecisionOptionRef,
      granted: Boolean, on: ConsentState, base: ReadyGame,
      decisionId: String = destination): ConsentState =
    accepted(accepted(on.requested(ConsentRequest(p1, decisionId, target,
      asks))).answered(p2, granted, ConsentTurn.of(base)))

  test("a power with no requests this turn carries an empty list"):
    val (service, _) = ConsentFixture.service()
    assertEquals(view(startTravel(service), p2).consentPreferences
      .map(_.requests), Vector(Vector.empty))

  test("the ruler's answered request is listed as answered and open to " +
      "change, and nobody else sees it"):
    val (service, _) = ConsentFixture.service()
    val no = answering(service, asking(service), ConsentChoice.No)
    assertEquals(requestsShownTo(p2, no), Vector(ConsentRequestRowProjection(
      "p1", destination, "site", dunes.wireId, "Dunes", "refused",
      Vector.empty, None, "open")))
    assertEquals(requestsShownTo(p1, no), Vector.empty)
    assertEquals(requestsShownTo(p3, no), Vector.empty)
    assertEquals(projector.projectPublic(gameId, no.loaded)
      .consentPreferences, Vector.empty)
    val granted = changing(service, no, allow = true)
    assertEquals(requestsShownTo(p2, granted).map(row =>
      (row.answer, row.change)), Vector(("allowed", "open")))

  test("an entry the requester acted on is used, and open again after " +
      "their undo"):
    val (service, _) = ConsentFixture.service()
    val yes = answering(service, asking(service), ConsentChoice.Yes)
    assertEquals(requestsShownTo(p2, yes).map(row =>
      (row.answer, row.change)), Vector(("allowed", "used")))
    val undone = accepted(service.handle(gameId, yes.nextSequence,
      Commands.undo(p1)))
    assertEquals(requestsShownTo(p2, undone).map(row =>
      (row.answer, row.change)), Vector(("allowed", "open")))

  test("an automatic power's answer is listed as automatic, with its " +
      "card"):
    val (service, _) = ConsentFixture.service(tyrantBesideAWalledCoast)
    val waiting = travelledToTheWalledCoast(service, 0L)
    val no = accepted(service.handle(gameId, waiting.nextSequence,
      Commands.answerConsent(p2, ConsentChoice.No)))
    assertEquals(requestsShownTo(p2, no).map(row =>
      (row.use, row.answer, row.change)),
      Vector((Some("Tyrant"), "refused", "automatic")))

  test("an entry waits while a request about the same option does"):
    val base = passRuledByP2
    val answered = answeredBy(Vector(ConsentAsk(p2,
      Vector(NarrowPassSite.pass))), dunes, granted = true, base.consent,
      base)
    val waiting = answered.copy(pending = Some(ConsentRequest(p1,
      destination, dunes, Vector(ConsentAsk(p3,
        Vector(NarrowPassSite.pass))))))
    assertEquals(shownToP2With(base, waiting).flatMap(_.requests)
      .map(_.change), Vector("waiting"))

  test("the list is newest first, names the other powers an ask covered, " +
      "and marks an answer that covers a power the viewer lost"):
    val police = SecretPoliceCard.power.id
    val both = TravelFixture.on(passRuledByP2)(_.denizen(
      SecretPoliceCard.id, at = TravelFixture.pass))
    val coast = DecisionOptionRef.Site(TravelFixture.coast)
    val first = answeredBy(Vector(ConsentAsk(p2,
      Vector(NarrowPassSite.pass))), dunes, granted = true, both.consent,
      both)
    val second = answeredBy(Vector(ConsentAsk(p2,
      Vector(NarrowPassSite.pass, police))), coast, granted = false, first,
      both)
    val shown = shownToP2With(both, second)
    assertEquals(shown.find(_.powerId == NarrowPassSite.pass.value)
      .toVector.flatMap(_.requests).map(row =>
        (row.optionId, row.alsoPowers, row.change)), Vector(
      (coast.wireId, Vector("Secret Police"), "open"),
      (dunes.wireId, Vector.empty, "open")))
    assertEquals(shown.find(_.powerId == police.value).toVector
      .flatMap(_.requests).map(row => (row.optionId, row.alsoPowers)),
      Vector((coast.wireId, Vector("Narrow Pass"))))
    // Without the Police on the map, p2 rules the Pass alone.
    assertEquals(shownToP2With(passRuledByP2, second).flatMap(_.requests)
      .map(row => (row.optionId, row.change)), Vector(
      (coast.wireId, "lapsed"), (dunes.wireId, "open")))

  test("a button whose decision is no longer parked reads the log's " +
      "fallback label"):
    val base = passRuledByP2
    val answered = answeredBy(Vector(ConsentAsk(p2,
      Vector(NarrowPassSite.pass))), DecisionOptionRef.Button("give-way"),
      granted = true, base.consent, base, decisionId = "not-parked")
    assertEquals(shownToP2With(base, answered).flatMap(_.requests)
      .map(_.label), Vector("Give Way"))

  test("a card the viewer may not identify reads as a hidden card"):
    // p1 holds Tyrant facedown: p2 may not name it.
    val base = TravelFixture.on(passRuledByP2)(_.adviser(p1, TyrantCard,
      facedown = true))
    val answered = answeredBy(Vector(ConsentAsk(p2,
      Vector(NarrowPassSite.pass))),
      DecisionOptionRef.Denizen(TyrantCard.id), granted = true,
      base.consent, base, decisionId = "not-parked")
    assertEquals(shownToP2With(base, answered).flatMap(_.requests)
      .map(_.label), Vector("a hidden card"))

  test("the prompt says its answer can be changed, but not an automatic " +
      "power's"):
    val (service, _) = ConsentFixture.service()
    assertEquals(view(asking(service), p2).consentPrompt.map(_.changeable),
      Some(true))
    val (walled, _) = ConsentFixture.service(tyrantBesideAWalledCoast)
    assertEquals(view(travelledToTheWalledCoast(walled, 0L), p2)
      .consentPrompt.map(_.changeable), Some(false))
```

In the test "the asked ruler sees the prompt; a third player and the
public see neither", the expected prompt becomes
`ConsentPromptProjection("p1", Vector("Narrow Pass"), "Dunes",
changeable = true)`.

- [ ] **Step 3: Run the suite to verify it fails**

Run: `./sbtw "testOnly distantlands.application.ConsentProjectionSuite"`
Expected: the new tests fail: every `requests` is empty, and
`changeable` is false.

- [ ] **Step 4: Write the implementation**

Create `src/main/scala/distantlands/application/ConsentChange.scala`:

```scala
package distantlands.application

import distantlands.gameplay.consent.ConsentUseMarks
import distantlands.gameplay.powers.ConsentPowers
import distantlands.model._

/** Whether a granter may change one answer of the turn now, and if not,
  * why (reconsider design, "Commands" and "Projection"). One judgement
  * serves the desk, which refuses by it, and the projection, which shows
  * the entry's button withheld, so the two cannot disagree. The cases are
  * judged in the desk's order. */
private[application] enum ConsentChange(val key: String):
  /** Nothing stands in the way. */
  case Open extends ConsentChange("open")
  /** The request was about an automatic power; its answer is final. */
  case Automatic extends ConsentChange("automatic")
  /** The granter no longer rules `power`, which the answer covers. This
    * is also every ask nobody rules: a payer's, an Imperial law's. */
  case Lapsed(power: PowerId) extends ConsentChange("lapsed")
  /** The requester chose the option, and that choice stands (ruling
    * 21). */
  case Used extends ConsentChange("used")
  /** A request about the same option waits on an answer (implementation
    * ruling R8). */
  case Waiting extends ConsentChange("waiting")

private[application] object ConsentChange:
  def of(ready: ReadyGame, powers: ConsentPowers, granter: PlayerId,
      entry: ConsentRequest, ask: ConsentAsk): ConsentChange =
    val lost = ask.powers.find(power =>
      !powers.granter(ready, power).contains(granter))
    if entry.use.nonEmpty then Automatic
    else if lost.nonEmpty then Lapsed(lost.get)
    else if ConsentUseMarks.locked(ready, entry.requester,
        entry.decisionId, entry.target) then Used
    else if ready.consent.pending.exists(waiting =>
        waiting.requester == entry.requester &&
          waiting.decisionId == entry.decisionId &&
          waiting.target == entry.target) then Waiting
    else Open
```

In `ConsentDesk.reconsider`, replace the checks between `ask = found._2`
and the `yield` with:

```scala
      judged = ConsentChange.of(ready, powers, granter, entry, ask)
      _ <- (judged match
        case ConsentChange.Automatic => Some(refuse(
          "an automatic power's answer cannot be changed"))
        case ConsentChange.Lapsed(power) => Some(refuse(
          s"${granter.value} no longer rules ${power.value}"))
        case ConsentChange.Used => Some(refuse(
          s"${requester.value} has acted on this consent"))
        case _ => None).toLeft(())
      _ <- Either.cond(!ask.answer.contains(allow), (),
        refuse(s"${granter.value}'s answer is already that"))
      _ <- Either.cond(judged != ConsentChange.Waiting, (),
        refuse("a consent request about that option is waiting"))
```

and drop the `ConsentUseMarks` import if nothing else in the file uses
it.

In `ConsentProjector.scala`:

- `preferences` takes the context and adds the rows:

```scala
  private def preferences(context: ScopedProjectionContext, viewer: PlayerId)
      : Vector[ConsentPreferencesProjection] =
    val ready = context.ready
    powers.ruledBy(ready, viewer).map(power => ConsentPreferencesProjection(
      power.value, names.power(power),
      ready.game.current.players.map(_.player).filter(_ != viewer).map(
        other => ConsentPreferenceRowProjection(other.value,
          ready.consent.preference(viewer, power, other)
            .fold(ConsentSetting.CaseByCase.key)(_.key))),
      powers.source(power).collect { case PowerSourceRef.Card(card) =>
        card.value },
      powers.source(power).collect { case PowerSourceRef.Banner(banner) =>
        banner.key },
      requests(context, viewer, power)))

  /** The turn's requests about `power` that `viewer` answered, newest
    * first (reconsider design, "Projection"). Only the viewer's own
    * answers leave the server. */
  private def requests(context: ScopedProjectionContext, viewer: PlayerId,
      power: PowerId): Vector[ConsentRequestRowProjection] =
    val ready = context.ready
    ready.consent.historyAt(ConsentTurn.of(ready)).reverse.flatMap(entry =>
      entry.asks.find(ask => ask.granter == viewer &&
        ask.answer.nonEmpty && ask.powers.contains(power)).map(ask =>
        ConsentRequestRowProjection(entry.requester.value, entry.decisionId,
          entry.target.kind, entry.target.wireId, optionLabel(context, entry),
          if ask.answer.contains(true) then "allowed" else "refused",
          ask.powers.filterNot(_ == power).map(names.power),
          entry.use.map(used),
          ConsentChange.of(ready, powers, viewer, entry, ask).key)))
```

and the call in `applied` becomes
`context.viewer.toVector.flatMap(preferences(context, _))`.

- The prompt gains its flag. In `applied`, the `ConsentPromptProjection`
  takes as its last argument:

```scala
        changeable = waiting.use.isEmpty && ask.powers.forall(power =>
          powers.granter(ready, power).contains(viewer))
```

- `target` is split so the row and the prompt share the naming and
  differ only in the fallback:

```scala
  /** The option asked about, named as its decision row names it
    * ([[DecisionOptionRows]]), for the prompt. A button has no name of
    * its own, so its label is read from the parked decision the request
    * is about. */
  private def target(context: ScopedProjectionContext,
      request: ConsentRequest): String =
    named(context, request).getOrElse(request.target.wireId)

  /** The option of a settled request, for the requests list. Its decision
    * may be parked no longer, so a button falls back to the log's label
    * for its key. A card this viewer may not identify is not named
    * (reconsider design, "Projection"; implementation ruling on Q4). */
  private def optionLabel(context: ScopedProjectionContext,
      request: ConsentRequest): String =
    named(context, request).getOrElse(request.target match
      case DecisionOptionRef.Button(key) => presentation.safeLabel(key)
      case _: DecisionOptionRef.Denizen | _: DecisionOptionRef.Relic |
          _: DecisionOptionRef.Vision | _: DecisionOptionRef.Legacy =>
        "a hidden card"
      case other => presentation.safeLabel(other.wireId))

  private def named(context: ScopedProjectionContext,
      request: ConsentRequest): Option[String] =
    val ref = request.target
    val option = ref match
      case button: DecisionOptionRef.Button => decisions
        .openDecisions(context.ready)
        .filter(_.decisionId == request.decisionId)
        .flatMap(offered).find(_.ref == button)
      case _ => DecisionOption.forRef(ref)
    option.flatMap(rows.project(context.ready, context.viewer,
        context.ready.game.cardIndex.toOption, _)).map(_.label)
```

- [ ] **Step 5: Run the suites to verify they pass**

Run:
`./sbtw "testOnly distantlands.application.ConsentProjectionSuite distantlands.application.ConsentReconsiderServiceSuite distantlands.application.*Projection*"`
Expected: all pass, `ConsentReconsiderServiceSuite` unchanged.

- [ ] **Step 6: Commit**

```bash
git add src/main/scala/distantlands/application/ConsentChange.scala \
  src/main/scala/distantlands/application/ConsentDesk.scala \
  src/main/scala/distantlands/application/ConsentProjector.scala \
  src/test/scala/distantlands/application/ConsentProjectionSuite.scala
git commit -m "feat(consent): project the turn's requests to the power's ruler

Each power a viewer rules lists the requests of the turn they answered,
newest first, with whether the answer can be changed now. The desk and
the projection read that from one judgement. The prompt says whether its
answer can be changed later.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 3: The requests block

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/frontend.scala`
  (aliases, beside `ConsentPreferenceRowState`)
- Modify: `frontend/src/main/scala/distantlands/frontend/SitePowerLinks.scala`
- Create: `frontend/src/main/scala/distantlands/frontend/ConsentRequestsView.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/ConsentRequestsViewSuite.scala` (create)

**Interfaces:**
- Consumes: Task 1's `ConsentRequestRowProjection`; slice 2's
  `GameIntent.ReconsiderConsent(requesterId, decisionId, target, allow)`.
- Produces, in `distantlands.frontend`:
  `type ConsentRequestRowState = protocol.projection.ConsentRequestRowProjection`
  and its companion `val`;
  `final case class ConsentRequestEntry(requester: String, row: ConsentRequestRowState)`;
  `final case class ConsentRequests(powerId: String, entries: Vector[ConsentRequestEntry], change: Option[(ConsentRequestEntry, Boolean) => Unit])`
  with `ConsentRequests.none`;
  `ConsentGrid(rows, choose, requests: ConsentRequests = ConsentRequests.none)`;
  `SitePowerLinks.requests(powerId: String): Option[ConsentRequests]`;
  `ConsentRequestsView.mount(requests: ConsentRequests): dom.Element`
  (a `div.consent-requests[data-power-id]`, hidden while empty) and
  `ConsentRequestsView.redraw(block: dom.Element, requests: ConsentRequests, scroller: dom.Element, fallback: dom.html.Element): Unit`.

- [ ] **Step 1: Write the failing tests**

Create `frontend/src/test/scala/distantlands/frontend/ConsentRequestsViewSuite.scala`:

```scala
package distantlands.frontend

import distantlands.frontend.Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.{GameIntent => Intent, WalkerStartArgWire}
import org.scalajs.dom

/** The requests block of the consent column (reconsider design,
  * "Frontend", "Requests list"): the turn's requests about a power, as
  * the viewer answered them. Red rules the Narrow Pass; Blue asked. */
class ConsentRequestsViewSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val blue = GamePlayer("blue", "Blue", "Exile", PlayerColor.Blue)
  private val power = "site.narrow-pass.pass"

  private def row(answer: String, change: String,
      also: Vector[String] = Vector.empty, use: Option[String] = None,
      optionId: String = "site:deep-woods", label: String = "Deep Woods")
      : ConsentRequestRowState =
    ConsentRequestRowState("blue", "travel.destination", "site", optionId,
      label, answer, also, use, change)

  private def tableWhereRedAnswered(rows: ConsentRequestRowState*)
      : GameProjection =
    TestProjections.actingProjection(players = Vector(red, blue)).copy(
      consentPreferences = Vector(ConsentPreferencesState(power,
        "Narrow Pass", Vector(ConsentPreferenceRowState("blue",
          "case-by-case")), requests = rows.toVector)))

  /** The block as the column draws it, with what its buttons send. */
  private def blockOf(rows: Vector[ConsentRequestRowState],
      control: Boolean = true): (dom.Element, () => Vector[Intent]) =
    var sent = Vector.empty[Intent]
    val links = SitePowerLinks.of(tableWhereRedAnswered(rows*),
      Option.when(control)(command => sent :+= command))
    val requests = links.requests(power).getOrElse(fail("no requests"))
    (ConsentRequestsView.mount(requests), () => sent)

  private def texts(block: dom.Element, selector: String): Vector[String] =
    all(block, selector).map(_.textContent)

  private def button(block: dom.Element): dom.html.Button =
    one(block, ".consent-request-change").asInstanceOf[dom.html.Button]

  test("a power with no requests draws a hidden, empty block"):
    val (block, _) = blockOf(Vector.empty)
    assert(block.hasAttribute("hidden"))
    assertEquals(block.children.length, 0)
    assertEquals(block.getAttribute("data-power-id"), power)

  test("a refused request reads who asked for what, its state, and an " +
      "Allow button that sends the change"):
    val (block, sent) = blockOf(Vector(row("refused", "open")))
    assert(!block.hasAttribute("hidden"))
    assertEquals(texts(block, ".consent-grid-heading"),
      Vector("Requests this turn"))
    assertEquals(texts(block, ".consent-request-asked"),
      Vector("Blue asked to select Deep Woods"))
    assertEquals(texts(block, ".consent-request-also"), Vector.empty)
    assertEquals(texts(block, ".consent-request-state"), Vector("Refused"))
    assertEquals(button(block).textContent, "Allow")
    button(block).click()
    assertEquals(sent(), Vector(Intent.ReconsiderConsent("blue",
      "travel.destination", WalkerStartArgWire("site", "site:deep-woods"),
      allow = true)))

  test("a granted request reads Allowed and a Refuse button that sends " +
      "the refusal"):
    val (block, sent) = blockOf(Vector(row("allowed", "open")))
    assertEquals(texts(block, ".consent-request-state"), Vector("Allowed"))
    assertEquals(button(block).textContent, "Refuse")
    button(block).click()
    assertEquals(sent().map {
      case Intent.ReconsiderConsent(_, _, _, allow) => allow
      case other => fail(s"unexpected $other")
    }, Vector(false))

  test("a click sends once: the button is disabled until the list is " +
      "redrawn"):
    val (block, sent) = blockOf(Vector(row("refused", "open")))
    button(block).click()
    button(block).click()
    assertEquals(sent().size, 1)
    assert(button(block).disabled)

  test("an ask that covered other powers names them on a second line"):
    val (block, _) = blockOf(Vector(row("refused", "open",
      also = Vector("Secret Police", "Toll Roads"))))
    assertEquals(texts(block, ".consent-request-also"),
      Vector("Also answers Secret Police and Toll Roads"))

  test("a used entry reads Allowed, used; its Refuse is withheld with " +
      "the reason in the hover note, and sends nothing"):
    val (block, sent) = blockOf(Vector(row("allowed", "used")))
    assertEquals(texts(block, ".consent-request-state"),
      Vector("Allowed, used"))
    val refuse = button(block)
    assertEquals(refuse.textContent, "Refuse")
    assertEquals(refuse.getAttribute("aria-disabled"), "true")
    assert(!refuse.disabled)
    assertEquals(refuse.getAttribute("data-tip"), "Blue has acted on this " +
      "consent. It unlocks if Blue undoes that.")
    refuse.click()
    assertEquals(sent(), Vector.empty)
    assertEquals(block.textContent.contains("unlocks"), false)

  test("a refused entry whose option was used keeps Refused, with Allow " +
      "withheld"):
    val (block, _) = blockOf(Vector(row("refused", "used")))
    assertEquals(texts(block, ".consent-request-state"), Vector("Refused"))
    assertEquals(button(block).getAttribute("aria-disabled"), "true")

  test("an entry with a waiting request, and one that covers a lost " +
      "power, are withheld with their own notes"):
    val (waiting, _) = blockOf(Vector(row("allowed", "waiting")))
    assertEquals(button(waiting).getAttribute("data-tip"),
      "A request about this option is waiting for an answer.")
    val (lapsed, _) = blockOf(Vector(row("refused", "lapsed")))
    assertEquals(button(lapsed).getAttribute("data-tip"),
      "You no longer rule every power this answer covers.")
    assertEquals(button(lapsed).getAttribute("aria-disabled"), "true")

  test("an automatic power's entry reads what happened, with the other " +
      "answer's button withheld"):
    val (stopped, sent) = blockOf(Vector(row("refused", "automatic",
      use = Some("Tyrant"), optionId = "use", label = "Use")))
    assertEquals(texts(stopped, ".consent-request-asked"),
      Vector("Blue's Tyrant: deactivated"))
    assertEquals(texts(stopped, ".consent-request-state"), Vector.empty)
    assertEquals(button(stopped).textContent, "Let it work")
    assertEquals(button(stopped).getAttribute("aria-disabled"), "true")
    assertEquals(button(stopped).getAttribute("data-tip"),
      "This answer cannot be changed.")
    button(stopped).click()
    assertEquals(sent(), Vector.empty)
    val (worked, _) = blockOf(Vector(row("allowed", "automatic",
      use = Some("Tyrant"), optionId = "use", label = "Use")))
    assertEquals(texts(worked, ".consent-request-asked"),
      Vector("Blue's Tyrant: let work"))
    assertEquals(button(worked).textContent, "Deactivate")

  test("a change value this client does not know is not changeable"):
    val (block, sent) = blockOf(Vector(row("allowed", "from-the-future")))
    assertEquals(button(block).getAttribute("aria-disabled"), "true")
    button(block).click()
    assertEquals(sent(), Vector.empty)

  test("without control an open entry's button is disabled, and a " +
      "withheld one keeps its note"):
    val (open, _) = blockOf(Vector(row("refused", "open")), control = false)
    assert(button(open).disabled)
    val (used, _) = blockOf(Vector(row("allowed", "used")), control = false)
    assert(!button(used).disabled)
    assertEquals(button(used).getAttribute("aria-disabled"), "true")

  test("entries keep the server's order, and each is keyed by its " +
      "requester, decision and option"):
    val (block, _) = blockOf(Vector(
      row("allowed", "open", optionId = "site:dunes", label = "Dunes"),
      row("refused", "open")))
    assertEquals(texts(block, ".consent-request-asked"), Vector(
      "Blue asked to select Dunes", "Blue asked to select Deep Woods"))
    assertEquals(all(block, ".consent-request").map(
      _.getAttribute("data-request-key")), Vector(
      "blue|travel.destination|site|site:dunes",
      "blue|travel.destination|site|site:deep-woods"))

  test("the column draws the block under the preference rows"):
    val links = SitePowerLinks.of(tableWhereRedAnswered(
      row("refused", "open")), None)
    val grid = links.grid(SitePower("pass", "Pass", None, power))
      .getOrElse(fail("no grid"))
    val column = ConsentGridView.render(grid)
    assertEquals(column.children.toVector.map(_.getAttribute("class")),
      Vector("consent-grid-heading", "consent-grid-row", "consent-requests"))
```

- [ ] **Step 2: Run the suite to verify it fails**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ConsentRequestsViewSuite"`
Expected: a compile error, `Not found: ConsentRequestRowState`.

- [ ] **Step 3: Write the implementation**

In `frontend.scala`, after the `ConsentPreferenceRowState` aliases:

```scala
type ConsentRequestRowState = protocol.projection.ConsentRequestRowProjection
val ConsentRequestRowState = protocol.projection.ConsentRequestRowProjection
```

In `SitePowerLinks.scala`, replace the `ConsentGrid` declaration and its
doc with:

```scala
/** The viewer's consent column for one power they rule (ruling 16): a
  * row per other player and what choosing a setting does, and the turn's
  * requests about the power. Without control (`choose` is `None`) the
  * grid only shows the settings. */
private[frontend] final case class ConsentGrid(rows: Vector[ConsentGridRow],
    choose: Option[(String, String) => Unit],
    requests: ConsentRequests = ConsentRequests.none)
private[frontend] final case class ConsentGridRow(playerId: String,
    name: String, setting: String)

/** One request of the turn that the viewer answered, with the
  * requester's display name. */
private[frontend] final case class ConsentRequestEntry(requester: String,
    row: ConsentRequestRowState)

/** The turn's requests about one power (reconsider design, "Requests
  * list"), newest first. `powerId` finds the power again in a later
  * projection. `change` sends an entry's new answer, and is `None` without
  * control. */
private[frontend] final case class ConsentRequests(powerId: String,
    entries: Vector[ConsentRequestEntry],
    change: Option[(ConsentRequestEntry, Boolean) => Unit])
private[frontend] object ConsentRequests:
  val none: ConsentRequests = ConsentRequests("", Vector.empty, None)
```

(the existing `ConsentGridRow` declaration is the one shown; do not
declare it twice). Add `WalkerStartArgWire` to the file's imports
(`import distantlands.protocol.{GameIntent => GameCommand, WalkerStartArgWire}`),
and in `SitePowerLinks`:

```scala
  /** The turn's requests about the power `powerId`, when the viewer still
    * rules it: what an open overlay redraws its list from. */
  def requests(powerId: String): Option[ConsentRequests] = for
    projection <- value
    preferences <- projection.consentPreferences.find(_.powerId == powerId)
  yield requestsOf(projection, preferences)

  private def requestsOf(projection: GameProjection,
      preferences: ConsentPreferencesState): ConsentRequests =
    ConsentRequests(preferences.powerId, preferences.requests.map(row =>
        ConsentRequestEntry(playerDisplayName(projection,
          row.requesterPlayerId), row)),
      submit.map(send => (entry, allow) => send(GameCommand.ReconsiderConsent(
        entry.row.requesterPlayerId, entry.row.decisionId,
        WalkerStartArgWire(entry.row.optionKind, entry.row.optionId),
        allow))))
```

and `gridOf` passes the requests:

```scala
  private def gridOf(preferences: ConsentPreferencesState)
      : Option[ConsentGrid] =
    value.map(projection => ConsentGrid(preferences.rows.map(row =>
        ConsentGridRow(row.playerId, playerDisplayName(projection,
          row.playerId), row.setting)),
      submit.map(send => (requester, setting) =>
        send(GameCommand.SetConsentPreference(preferences.powerId, requester,
          setting))),
      requestsOf(projection, preferences)))
```

In `ConsentGridView.render`, before `section` is returned:

```scala
    section.appendChild(ConsentRequestsView.mount(grid.requests))
```

Create `frontend/src/main/scala/distantlands/frontend/ConsentRequestsView.scala`:

```scala
package distantlands.frontend

import distantlands.frontend.ServerUiSupport.{button, element, text}
import org.scalajs.dom

/** The requests block of the consent column (reconsider design,
  * "Frontend", "Requests list"): under the preference rows, the turn's
  * requests about the power as the viewer answered them, newest first.
  * Each entry says what was asked and how it stands, and carries one
  * button that changes the answer. A button that cannot change it is
  * withheld, with the reason in its hover note. The block is the one part
  * of the overlay redrawn from each projection. */
private[frontend] object ConsentRequestsView:
  /** The block for `requests`: hidden and empty while there are none. */
  def mount(requests: ConsentRequests): dom.Element =
    val block = element("div", "consent-requests")
    block.setAttribute("data-power-id", requests.powerId)
    fill(block, requests)
    block

  /** `block` redrawn from `requests`, keeping the reader's place: the
    * scroll of `scroller`, and keyboard focus on the same entry's button,
    * whatever it now reads. When that entry is gone, focus goes to
    * `fallback`. It follows `PanelContent.replace`, which finds a rebuilt
    * control by its text and so would lose a button that turns from
    * "Allow" to "Refuse". */
  def redraw(block: dom.Element, requests: ConsentRequests,
      scroller: dom.Element, fallback: dom.html.Element): Unit =
    val focused = Option(dom.document.activeElement).filter(block.contains)
      .flatMap(node => Option(node.closest("[data-request-key]")))
      .map(_.getAttribute("data-request-key"))
    val (left, top) = (scroller.scrollLeft, scroller.scrollTop)
    fill(block, requests)
    focused.foreach { key =>
      val twin = Dom.all(block, "[data-request-key]")
        .find(_.getAttribute("data-request-key") == key)
        .flatMap(entry => Option(entry.querySelector(
          ".consent-request-change")))
      twin.fold(fallback)(_.asInstanceOf[dom.html.Element]).focus()
    }
    scroller.scrollLeft = left
    scroller.scrollTop = top

  private def fill(block: dom.Element, requests: ConsentRequests): Unit =
    while block.firstChild != null do block.removeChild(block.firstChild)
    if requests.entries.isEmpty then block.setAttribute("hidden", "")
    else
      block.removeAttribute("hidden")
      block.appendChild(text("h4", "consent-grid-heading",
        "Requests this turn"))
      val list = element("ul", "consent-request-list")
      requests.entries.foreach(entry =>
        list.appendChild(item(entry, requests.change)))
      block.appendChild(list)

  private def key(row: ConsentRequestRowState): String =
    Vector(row.requesterPlayerId, row.decisionId, row.optionKind,
      row.optionId).mkString("|")

  private def item(entry: ConsentRequestEntry,
      change: Option[(ConsentRequestEntry, Boolean) => Unit]): dom.Element =
    val row = entry.row
    val allowed = row.answer == "allowed"
    val used = row.change == "used"
    val item = element("li", "consent-request")
    item.setAttribute("data-request-key", key(row))
    item.setAttribute("data-change", row.change)
    // "Blue asked to select Deep Woods"; for an automatic power, "Blue's
    // Tyrant: deactivated" or "Blue's Tyrant: let work".
    item.appendChild(text("p", "consent-request-asked", row.use.fold(
      s"${entry.requester} asked to select ${row.label}")(card =>
      s"${entry.requester}'s $card: " +
        (if allowed then "let work" else "deactivated"))))
    if row.alsoPowers.nonEmpty then
      item.appendChild(text("p", "consent-request-also",
        s"Also answers ${ConsentAsking.listed(row.alsoPowers)}"))
    val answer = element("div", "consent-request-answer")
    // An automatic power's first line already says how it stands.
    if row.use.isEmpty then
      answer.appendChild(text("span", "consent-request-state",
        if !allowed then "Refused" else if used then "Allowed, used"
        else "Allowed"))
    val other = button(row.use.fold(if allowed then "Refuse" else "Allow")(
      _ => if allowed then "Deactivate" else "Let it work"),
      "consent-request-change")
    row.change match
      case "open" => change match
        case Some(send) => other.onclick = _ =>
          // One command a click: the next redraw brings the button back.
          other.disabled = true
          send(entry, !allowed)
        case None => other.disabled = true
      case "used" => HoverTip.withhold(other, s"${entry.requester} has " +
        s"acted on this consent. It unlocks if ${entry.requester} undoes " +
        "that.")
      case "waiting" => HoverTip.withhold(other,
        "A request about this option is waiting for an answer.")
      case "lapsed" => HoverTip.withhold(other,
        "You no longer rule every power this answer covers.")
      // An automatic power's answer, and any value a newer server sends.
      case _ => HoverTip.withhold(other, "This answer cannot be changed.")
    answer.appendChild(other)
    item.appendChild(answer)
    item
```

- [ ] **Step 4: Run the consent frontend suites to verify they pass**

Run:
`./sbtw "frontend/testOnly distantlands.frontend.ConsentRequestsViewSuite distantlands.frontend.SitePowerGridSuite distantlands.frontend.BannerConsentGridSuite distantlands.frontend.BoardCardConsentGridSuite distantlands.frontend.ChronicleDetailsOverlaySuite"`
Expected: all pass. Slice 1's test "a preference row names the player
..." reads a row's children, not the column's, so it holds. A suite that
counts the column's children fails here and takes the new block; report
it.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/frontend.scala \
  frontend/src/main/scala/distantlands/frontend/SitePowerLinks.scala \
  frontend/src/main/scala/distantlands/frontend/ConsentRequestsView.scala \
  frontend/src/test/scala/distantlands/frontend/ConsentRequestsViewSuite.scala
git commit -m "feat(frontend): list the turn's requests under the consent rows

Each entry says who asked for what and how it stands, with one button
that changes the answer. A button that cannot change it is withheld,
with the reason in its hover note.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 4: The list stays live while the overlay is open

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/GameTableShell.scala`
- Modify: `frontend/src/main/scala/distantlands/frontend/TableScreen.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/ConsentRequestsLiveSuite.scala` (create)

**Interfaces:**
- Consumes: Task 3's `ConsentRequests`, `SitePowerLinks.requests`,
  `ConsentRequestsView.redraw`.
- Produces: `CardInspectionOverlay.showConsentRequests(links: SitePowerLinks): Unit`;
  `GameTableShell.showConsentRequests(links: SitePowerLinks): Unit`.

- [ ] **Step 1: Re-inventory**

Run:
```bash
git grep -n -e "ConsentGridView.render" -e "showChronicleDetails" -- frontend/src/main
```
Expected: `ConsentGridView.render` at four places in
`CardInspectionOverlay.scala` (`showFestival`, `showFestivalMarker`,
`showPower`, `showRuled`); `showChronicleDetails` in `GameTableShell` and
`TableScreen`. A fifth place that draws the column goes through the new
`drawColumn` too.

- [ ] **Step 2: Write the failing tests**

Create `frontend/src/test/scala/distantlands/frontend/ConsentRequestsLiveSuite.scala`:

```scala
package distantlands.frontend

import distantlands.frontend.Dom.{all, one}
import distantlands.model.PlayerColor
import distantlands.protocol.{GameIntent => Intent}
import org.scalajs.dom

/** The requests list stays live while the overlay is open (reconsider
  * design, "Live while open"): redrawn from each projection, while the
  * rest of the overlay ignores polls. Red rules the Narrow Pass. */
class ConsentRequestsLiveSuite extends munit.FunSuite:
  private val red = GamePlayer("red", "Red", "Exile", PlayerColor.Red)
  private val blue = GamePlayer("blue", "Blue", "Exile", PlayerColor.Blue)
  private val power = "site.narrow-pass.pass"
  private val pass = SitePower("pass", "Pass", Some("Printed text."), power)

  private def row(answer: String, change: String,
      optionId: String = "site:deep-woods", label: String = "Deep Woods")
      : ConsentRequestRowState =
    ConsentRequestRowState("blue", "travel.destination", "site", optionId,
      label, answer, Vector.empty, None, change)

  private def tableWhereRedAnswered(setting: String,
      rows: ConsentRequestRowState*): GameProjection =
    TestProjections.actingProjection(players = Vector(red, blue)).copy(
      consentPreferences = Vector(ConsentPreferencesState(power,
        "Narrow Pass", Vector(ConsentPreferenceRowState("blue", setting)),
        requests = rows.toVector)))

  /** An overlay open on the Pass for `first`, with what was sent. */
  private final class OpenOverlay(first: GameProjection):
    var sent = Vector.empty[Intent]
    val root: dom.Element = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    val overlay = new CardInspectionOverlay(root)
    private val opener = dom.document.createElement("button")
      .asInstanceOf[dom.html.Element]
    dom.document.body.appendChild(opener)
    def links(value: GameProjection): SitePowerLinks =
      SitePowerLinks.of(value, Some(command => sent :+= command))
    overlay.showPower("Pass", Vector("Printed text."),
      links(first).grid(pass), opener)
    def snapshot(value: GameProjection): Unit =
      overlay.showConsentRequests(links(value))
    def change: dom.html.Button =
      one(root, ".consent-request-change").asInstanceOf[dom.html.Button]
    def states: Vector[String] =
      all(root, ".consent-request-state").map(_.textContent)
    def close(): Unit =
      overlay.dispose()
      root.remove()
      opener.remove()

  private def withOverlay(first: GameProjection)(run: OpenOverlay => Unit)
      : Unit =
    val open = new OpenOverlay(first)
    try run(open) finally open.close()

  test("a later projection redraws the list: an entry appears, changes " +
      "and locks in place"):
    withOverlay(tableWhereRedAnswered("case-by-case")) { open =>
      assertEquals(open.states, Vector.empty)
      open.snapshot(tableWhereRedAnswered("case-by-case",
        row("refused", "open")))
      assertEquals(open.states, Vector("Refused"))
      open.snapshot(tableWhereRedAnswered("case-by-case",
        row("allowed", "open")))
      assertEquals((open.states, open.change.textContent),
        (Vector("Allowed"), "Refuse"))
      open.snapshot(tableWhereRedAnswered("case-by-case",
        row("allowed", "used")))
      assertEquals(open.states, Vector("Allowed, used"))
      assertEquals(open.change.getAttribute("aria-disabled"), "true")
    }

  test("the preference rows ignore the projection: a setting just chosen " +
      "stays pressed"):
    withOverlay(tableWhereRedAnswered("case-by-case",
        row("refused", "open"))) { open =>
      val always = all(open.root, ".consent-grid-choice")(2)
        .asInstanceOf[dom.html.Button]
      always.click()
      // The poll still carries the old setting.
      open.snapshot(tableWhereRedAnswered("case-by-case",
        row("allowed", "open")))
      assertEquals(all(open.root, ".consent-grid-choice")
        .map(_.getAttribute("aria-pressed")), Vector("false", "false",
        "true"))
      assertEquals(all(open.root, ".consent-grid-choice")(2), always)
      assertEquals(open.states, Vector("Allowed"))
    }

  test("keyboard focus stays on the entry's button when it turns from " +
      "Allow to Refuse"):
    withOverlay(tableWhereRedAnswered("case-by-case",
        row("allowed", "open", optionId = "site:dunes", label = "Dunes"),
        row("refused", "open"))) { open =>
      val allow = all(open.root, ".consent-request-change")(1)
        .asInstanceOf[dom.html.Button]
      allow.focus()
      allow.click()
      open.snapshot(tableWhereRedAnswered("case-by-case",
        row("allowed", "open", optionId = "site:dunes", label = "Dunes"),
        row("allowed", "open")))
      val focused = dom.document.activeElement
      assertEquals(focused.textContent, "Refuse")
      assertEquals(focused.closest("[data-request-key]")
        .getAttribute("data-request-key"),
        "blue|travel.destination|site|site:deep-woods")
    }

  test("when the focused entry is gone, focus goes to Close"):
    withOverlay(tableWhereRedAnswered("case-by-case",
        row("refused", "open"))) { open =>
      open.change.focus()
      open.snapshot(tableWhereRedAnswered("case-by-case"))
      assertEquals(dom.document.activeElement.textContent, "Close")
      assert(one(open.root, ".consent-requests").hasAttribute("hidden"))
    }

  test("the reader's scroll is kept across a redraw"):
    withOverlay(tableWhereRedAnswered("case-by-case",
        row("refused", "open"))) { open =>
      val body = one(open.root, ".card-overlay-body")
      body.scrollTop = 40
      open.snapshot(tableWhereRedAnswered("case-by-case",
        row("allowed", "open")))
      assertEquals(body.scrollTop, 40.0)
    }

  test("the same rows are not redrawn, so an open hover note survives a " +
      "poll; a click is redrawn at the next snapshot"):
    withOverlay(tableWhereRedAnswered("case-by-case",
        row("refused", "open"))) { open =>
      val before = open.change
      open.snapshot(tableWhereRedAnswered("case-by-case",
        row("refused", "open")))
      assertEquals(open.change, before)
      before.click()
      assert(open.change.disabled)
      // The server refused: the same rows come back, and the button with
      // them.
      open.snapshot(tableWhereRedAnswered("case-by-case",
        row("refused", "open")))
      assert(!open.change.disabled)
      assertEquals(open.sent.size, 1)
    }

  test("a closed overlay, and one showing no column, ignore snapshots"):
    val root = dom.document.createElement("div")
    dom.document.body.appendChild(root)
    val overlay = new CardInspectionOverlay(root)
    val opener = dom.document.createElement("button")
      .asInstanceOf[dom.html.Element]
    dom.document.body.appendChild(opener)
    val links = SitePowerLinks.of(tableWhereRedAnswered("case-by-case",
      row("refused", "open")), None)
    overlay.showConsentRequests(links)
    overlay.showText("Oath", Vector("Rules the most sites"), opener)
    overlay.showConsentRequests(links)
    assertEquals(all(root, ".consent-requests"), Vector.empty)
    overlay.dispose()
    root.remove()
    opener.remove()

  test("the shell hands the overlay each snapshot's requests"):
    val browser = new TestBrowser()
    val shell = new GameTableShell(browser.mount, developmentTools = false,
      logStore = None)
    try
      val origin = dom.document.createElement("button")
        .asInstanceOf[dom.html.Element]
      browser.mount.appendChild(origin)
      val first = tableWhereRedAnswered("case-by-case")
      CardInspection.openPower("Pass", Vector("Printed text."),
        SitePowerLinks.of(first, None).grid(pass), origin)
      shell.showConsentRequests(SitePowerLinks.of(tableWhereRedAnswered(
        "case-by-case", row("refused", "open")), None))
      assertEquals(all(browser.mount, ".consent-request-asked")
        .map(_.textContent), Vector("Blue asked to select Deep Woods"))
    finally
      shell.dispose()
      browser.close()
```

- [ ] **Step 3: Run the suite to verify it fails**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ConsentRequestsLiveSuite"`
Expected: a compile error, `value showConsentRequests is not a member of
CardInspectionOverlay`.

- [ ] **Step 4: Write the implementation**

In `CardInspectionOverlay.scala`:

Extend the class doc's last sentence: "It ignores projection changes
while open ..., with one exception: the consent column's requests list is
redrawn from each projection (`showConsentRequests`; reconsider design,
"Live while open"), so an entry locks in place when the requester acts."

Add the state and the two helpers after `private var opener`:

```scala
  /** The requests the open column shows, to tell a changed list from the
    * same one. `None` while no column is drawn. */
  private var shownRequests = Option.empty[ConsentRequests]
  /** Whether a click since the last redraw waits on the server's word. */
  private var changeSent = false

  /** The consent column for `grid`, with its requests remembered. A click
    * on an entry is noted, so the next snapshot redraws the list even
    * when the server refused and the rows are the same. */
  private def drawColumn(grid: ConsentGrid): Unit =
    val noted = grid.requests.copy(change = grid.requests.change.map(send =>
      (entry, allow) => { changeSent = true; send(entry, allow) }))
    body.appendChild(ConsentGridView.render(grid.copy(requests = noted)))
    shownRequests = Some(noted)

  /** Each snapshot's requests for the power the open column is about
    * (reconsider design, "Live while open"): the list is redrawn when it
    * differs from what is drawn, or after a click. The preference rows
    * are left as the reader set them. A power the viewer no longer rules
    * lists nothing. */
  def showConsentRequests(links: SitePowerLinks): Unit =
    if isOpen then shownRequests.foreach { shown =>
      val next = links.requests(shown.powerId).getOrElse(
        ConsentRequests(shown.powerId, Vector.empty, None))
      if changeSent || next.entries != shown.entries ||
          next.change.isEmpty != shown.change.isEmpty then
        changeSent = false
        val noted = next.copy(change = next.change.map(send =>
          (entry, allow) => { changeSent = true; send(entry, allow) }))
        Option(body.querySelector(".consent-requests")).foreach(block =>
          ConsentRequestsView.redraw(block, noted, body, close))
        shownRequests = Some(noted)
    }
```

Replace each of the four `ConsentGridView.render` uses:

- in `showFestival` and `showFestivalMarker` and `showPower`:
  `grid.foreach(value => body.appendChild(ConsentGridView.render(value)))`
  becomes `grid.foreach(drawColumn)`;
- in `showRuled`: `body.appendChild(ConsentGridView.render(grid))` becomes
  `drawColumn(grid)`.

`clear()` forgets the column:

```scala
  private def clear(): Unit =
    while body.firstChild != null do body.removeChild(body.firstChild)
    shownRequests = None
    changeSent = false
```

`showFestival` and `showPower` call `showText` first, which calls
`clear()`, so the column drawn after it is the one remembered.

In `GameTableShell.scala`, after `showChronicleDetails`:

```scala
  /** Every snapshot's consent requests reach the inspector, so an open
    * consent column's list stays current (reconsider design, "Live while
    * open"). */
  def showConsentRequests(links: SitePowerLinks): Unit =
    inspector.showConsentRequests(links)
```

In `TableScreen.render`, after `shell.showChronicleDetails(projection)`:

```scala
    projection.foreach(value => shell.showConsentRequests(SitePowerLinks.of(
      value, Option.when(view.controlsAvailable)(controls.submitCommand))))
```

- [ ] **Step 5: Run the frontend overlay suites to verify they pass**

Run:
`./sbtw "frontend/testOnly distantlands.frontend.ConsentRequestsLiveSuite distantlands.frontend.ConsentRequestsViewSuite distantlands.frontend.SitePowerGridSuite distantlands.frontend.BannerConsentGridSuite distantlands.frontend.BoardCardConsentGridSuite distantlands.frontend.ShellInspectionSuite distantlands.frontend.ChronicleDetailsOverlaySuite distantlands.frontend.TablePaneOrderSuite"`
Expected: all pass. jsdom lays nothing out; if it does not keep
`scrollTop` on an element with no overflow, the scroll test defines the
property on the body as `ShellInspectionSuite.sized` defines sizes, and
the change is reported.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/CardInspectionOverlay.scala \
  frontend/src/main/scala/distantlands/frontend/GameTableShell.scala \
  frontend/src/main/scala/distantlands/frontend/TableScreen.scala \
  frontend/src/test/scala/distantlands/frontend/ConsentRequestsLiveSuite.scala
git commit -m "feat(frontend): keep the requests list live while the overlay is open

Each snapshot reaches the open overlay, which redraws only the requests
list, and only when it changed or a click waits on the server. Focus
stays on the entry's button and the scroll is kept. The preference rows
still ignore polls.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 5: The prompt notice

**Files:**
- Modify: `frontend/src/main/scala/distantlands/frontend/ConsentPanels.scala`
- Test: `frontend/src/test/scala/distantlands/frontend/ConsentPromptPanelSuite.scala`

**Interfaces:**
- Consumes: Task 1's `ConsentPromptProjection.changeable`.

- [ ] **Step 1: Write the failing tests**

In `ConsentPromptPanelSuite`, give `prompted` its flag:

```scala
    consentPrompt = Some(ConsentPromptState("blue", Vector("Narrow Pass"),
      "Deep Woods", changeable = true)))
```

In the test "the asked ruler reads the question, four answers and where
to change preferences, and no waiting line", the expected notice becomes:

```scala
    assertEquals(panel.querySelector(".consent-notice").textContent,
      "You can change this answer, and your consent preferences, by " +
        "clicking on the Narrow Pass power.")
```

Add:

```scala
  test("a prompt whose answer stays final keeps the old notice: an " +
      "automatic power's, and a payer's"):
    val automatic = pane(deactivating, "red", new RecordingControls())
    assertEquals(automatic.querySelector(".consent-notice").textContent,
      "You can adjust your consent preferences by clicking on the City " +
        "Wall power.")
    val paying = table.copy(
      walkerWaiting = Some(WalkerWaitingProjection(Vector("blue"), None)),
      consentPrompt = Some(ConsentPromptState("blue", Vector("Toll Roads"),
        "Coast", paying = true)))
    assertEquals(pane(paying, "red", new RecordingControls())
      .querySelector(".consent-notice").textContent,
      "You can adjust your consent preferences by clicking on the Toll " +
        "Roads power.")
```

- [ ] **Step 2: Run the suite to verify it fails**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ConsentPromptPanelSuite"`
Expected: one failure, on the changed test: the notice still reads "You
can adjust your consent preferences by clicking on the Narrow Pass
power." The new test passes already; it pins what must not change.

- [ ] **Step 3: Write the implementation**

In `ConsentPanels.scala`, replace the notice paragraph in `render`:

```scala
      section.appendChild(text("p", "consent-notice", notice(asked)))
```

and add:

```scala
  /** Where the granter can come back to this: "You can change this
    * answer, and your consent preferences, by clicking on the Narrow Pass
    * power." (reconsider design, "Prompt notice"). An answer that stays
    * final, an automatic power's among them, keeps the older words. */
  def notice(asked: ConsentPromptState): String =
    val powers = ConsentAsking.listed(asked.powers)
    if asked.changeable then "You can change this answer, and your " +
      s"consent preferences, by clicking on the $powers power."
    else "You can adjust your consent preferences by clicking on the " +
      s"$powers power."
```

- [ ] **Step 4: Run the suite to verify it passes**

Run: `./sbtw "frontend/testOnly distantlands.frontend.ConsentPromptPanelSuite"`
Expected: all pass.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/main/scala/distantlands/frontend/ConsentPanels.scala \
  frontend/src/test/scala/distantlands/frontend/ConsentPromptPanelSuite.scala
git commit -m "feat(frontend): tell the granter where to change a consent answer

The prompt's notice names the power's overlay as the place to change
this answer and the preferences. A prompt whose answer stays final keeps
the older notice.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 6: The list's styles

**Files:**
- Modify: `frontend/styles.css` (the consent rules near line 2321, and
  the `@media (pointer: coarse)` rule)
- Test: `src/test/scala/distantlands/server/StylesheetConsentGridSuite.scala`

**Interfaces:**
- Consumes: Task 3's classes: `consent-requests`, `consent-request-list`,
  `consent-request`, `consent-request-asked`, `consent-request-also`,
  `consent-request-answer`, `consent-request-state`,
  `consent-request-change`.

- [ ] **Step 1: Write the failing tests**

In `StylesheetConsentGridSuite`, update the three strings that pin the
Control-face selector lists:

- in the R3 test, the shared face becomes
  `".overlay-confirm button, .consent-grid-choice,\n.consent-request-change { min-width: 32px;\n  min-height: 30px; padding: 4px 9px;"`;
- in the R4 test, the hover rule becomes
  `".overlay-confirm button:hover, .consent-grid-choice:hover:not(:disabled),\n.consent-request-change:hover:not(:disabled, [aria-disabled=\"true\"]) {"`,
  and `rule(".consent-grid-choice:disabled")` becomes
  `rule(".consent-grid-choice:disabled, .consent-request-change:disabled,\n.consent-request-change[aria-disabled=\"true\"]")`;
- in the 44px test, the coarse rule becomes
  `".consent-grid-choice, .consent-request-change { min-width: 44px; min-height: 44px; }"`.

Add `.consent-requests`, `.consent-request-list`, `.consent-request`,
`.consent-request-asked`, `.consent-request-also` and
`.consent-request-answer` to the selectors of "each layout selector opens
one rule ...", and add:

```scala
  test("the requests block stands under the rows with more space above " +
      "its heading than below, and takes no room while hidden"):
    val block = rule(".consent-requests")
    assert(block.contains("display: grid;"), block)
    assert(block.contains("gap: 8px;"), block)
    assert(block.contains("margin-top: 8px;"), block)
    assert(css.contains(".consent-requests[hidden] { display: none; }"))

  test("entries stand apart as the rows do, and an entry's lines stand " +
      "close"):
    val list = rule(".consent-request-list")
    assert(list.contains("gap: 12px;"), list)
    assert(list.contains("list-style: none;"), list)
    assert(rule(".consent-request").contains("gap: 4px;"))
    assert(rule(".consent-request-asked")
      .contains("overflow-wrap: anywhere;"))

  test("the second line is quiet and never under the Eleven-Pixel Floor"):
    val also = rule(".consent-request-also")
    assert(also.contains("color: var(--ink-dim);"), also)
    assert(also.contains("font-size: max(0.85em, 0.7rem);"), also)

  test("the state stands before its button on one line"):
    val answer = rule(".consent-request-answer")
    assert(answer.contains("display: flex;"), answer)
    assert(answer.contains("align-items: center;"), answer)
    assert(answer.contains("gap: 8px;"), answer)

  test("alone on its line the column fills it, up to a reading measure"):
    val column = rule(".consent-grid")
    assert(column.contains("flex: 1 1 17rem;"), column)
    assert(column.contains("max-width: 22rem;"), column)

  test("the change button keeps the focus ring of the settings"):
    assert(css.contains(".overlay-confirm button:focus-visible, " +
      ".consent-grid-choice:focus-visible,\n" +
      ".consent-request-change:focus-visible {"))
```

- [ ] **Step 2: Run the suite to verify it fails**

Run: `./sbtw "testOnly distantlands.server.StylesheetConsentGridSuite"`
Expected: failures on the three updated strings, the one-rule test (no
rule for the new selectors) and the six new tests.

- [ ] **Step 3: Write the styles**

In `frontend/styles.css`:

`.consent-grid` gains its width:

```css
.consent-grid { margin-top: 16px; display: grid; flex: 1 1 17rem;
  max-width: 22rem; grid-template-columns: minmax(0, 1fr); gap: 12px;
  font-size: 0.82em; }
```

After the `.consent-grid-choices` rule add:

```css
/* The turn's requests about the power (reconsider design, "Requests
   list"), under the rows: a heading with more room above than below, then
   one entry a request. An entry is what was asked, a quiet line for the
   other powers the answer covers, and its state before one button. The
   block takes no room while the power has no requests. */
.consent-requests { display: grid; gap: 8px; margin-top: 8px; }
.consent-requests[hidden] { display: none; }
.consent-request-list { display: grid; gap: 12px; margin: 0; padding: 0;
  list-style: none; }
.consent-request { display: grid; gap: 4px; }
.consent-request-asked { margin: 0; overflow-wrap: anywhere; }
.consent-request-also { margin: 0; color: var(--ink-dim);
  font-size: max(0.85em, 0.7rem); overflow-wrap: anywhere; }
.consent-request-answer { display: flex; align-items: center; gap: 8px; }
```

The shared face, its hover, its focus ring and the disabled face take
the change button:

```css
.overlay-confirm button, .consent-grid-choice,
.consent-request-change { min-width: 32px;
  min-height: 30px; padding: 4px 9px; color: var(--parchment-muted); background: var(--raised);
  border: 1px solid var(--brass-line); border-radius: 4px; font-size: 0.78rem;
  cursor: pointer; }
.overlay-confirm button:hover, .consent-grid-choice:hover:not(:disabled),
.consent-request-change:hover:not(:disabled, [aria-disabled="true"]) {
  border-color: var(--cream-bright); }
.overlay-confirm button:focus-visible, .consent-grid-choice:focus-visible,
.consent-request-change:focus-visible {
  outline: 2px solid var(--cream-focus); }
```

```css
/* A setting the viewer cannot change, without control of the seat, wears
   the Disabled face and answers no hover. So does a change that is
   withheld: it keeps focus and a tap, and its hover note says why. */
.consent-grid-choice:disabled, .consent-request-change:disabled,
.consent-request-change[aria-disabled="true"] { opacity: 0.5;
  cursor: not-allowed; }
```

In the `@media (pointer: coarse)` rule, `.consent-grid-choice` becomes
`.consent-grid-choice, .consent-request-change`.

- [ ] **Step 4: Run the stylesheet suites to verify they pass**

Run: `./sbtw "testOnly distantlands.server.Stylesheet*"`
Expected: all pass. `settingRules` still finds one rule that sets a
setting's font size.

- [ ] **Step 5: Commit**

```bash
git add frontend/styles.css \
  src/test/scala/distantlands/server/StylesheetConsentGridSuite.scala
git commit -m "style(consent): lay out the requests list and its withheld button

The list stands under the rows, each entry's state before its button.
The change button wears the settings' Control face; withheld or
disabled, it wears the Disabled face and answers no hover. Alone on its
line the column fills it, up to 22rem.

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 7: The design system and the designs

**Files:**
- Modify: `DESIGN.md` ("Consent Column")
- Modify: `docs/superpowers/specs/2026-11/2026-11-20-consent-reconsider-design.md`
  (status)

- [ ] **Step 1: Extend "Consent Column" in `DESIGN.md`**

In the section's first paragraph, after "under them where it is not.",
add: "Alone on its line it fills the line up to 22rem." Replace the last
bullet ("The column uses no amber ...") with:

```markdown
- **Requests list:** under the rows, absent while the power has no
  requests this turn. Its heading, "Requests this turn", is the rows'
  heading again, 20px under the last row and 8px over the first entry.
  Entries stand 12px apart, newest first. An entry is one line saying
  what was asked ("Blue asked to select Deep Woods"), a quiet second line
  in Ink Dim for the other powers the answer covers ("Also answers Secret
  Police"), and a line with the state as a word ("Refused", "Allowed",
  "Allowed, used") before one compact Control button, "Allow" or
  "Refuse". An automatic power's entry says what happened in its first
  line and shows the button alone.
- **A change that cannot be made** keeps its button, in the Withheld
  face: the Disabled face with `aria-disabled="true"`, so it takes focus
  and a tap, and the hover note gives the reason. The reason is never
  printed in the list. Without control an open button is disabled.
- **Live:** the list is the one part of the card overlay redrawn from
  each snapshot, and only when it changed, so an open hover note is left
  alone. Focus stays on the entry's button, whatever it now reads.
- The column uses no amber and no second blue: consent amber stays on
  the option a player may ask about, and selection blue on the pressed
  setting.
```

- [ ] **Step 2: Update the spec's status**

Replace the sentence that begins "Slices 1 and 2 are implemented" and
ends "slice 3 is not." with "All three slices are implemented", followed
by the three plan links in parentheses: the two there now, and
`[slice 3 plan](../../plans/2026-11/2026-11-20-consent-reconsider-slice3.md)`.

- [ ] **Step 3: Check the links and commit**

Run: `python3 scripts/check-markdown-links.py`
Expected: passed.

```bash
git add DESIGN.md \
  docs/superpowers/specs/2026-11/2026-11-20-consent-reconsider-design.md
git commit -m "docs(design): describe the consent column's requests list

Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>"
git log -3
```

---

### Task 8: The full gate and the browser check

- [ ] **Step 1: Run the full gate**

Run, in the background or with a long timeout:
`./sbtw test`, `./sbtw frontend/test`,
`python3 scripts/check-architecture.py`,
`python3 scripts/check-markdown-links.py`.
Expected: all pass. Report the test counts sbt prints.

- [ ] **Step 2: Start a scratch server**

`$DB` is a new, empty folder under the session scratchpad. Never point
the server at `var/distantlands` or `var/oathdigital`. Never use
`preview_start`. From the worktree:

```bash
./sbtw frontend/fastLinkJS
./sbtw "runMain distantlands.server.DistantLandsServer --port 8081 --database-path $DB/distantlands"
```

Wait until
`curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/health/ready`
prints `200`, and confirm the process runs from the worktree.

- [ ] **Step 3: Reach the user story's position**

Follow the route recorded in the slice 1 plan (Task 4 Step 3): import a
copy of the Chronicle with `site:narrow-pass` first in `atlasBox`, create
the game, start Red at the Narrow Pass and Blue at Salt Flats, in the
Provinces, and let Red conquer the Pass on the first turn. Then end
Red's turn. On Blue's turn start a Travel: Desolate Shore, the Cradle's
other site, is askable. Blue asks; Red answers No.

If the position cannot be reached (a lost Campaign after the Supply for
a second try is gone, or a turn order that never offers the request),
stop. Say plainly in the report that the browser check was not made and
why; do not claim it.

- [ ] **Step 4: Check the list once at each width**

Keep two tabs: Red's seat with the Pass's overlay open, and Blue's seat.
At 1440×900, then at 360×800, on Red's tab:

1. Read the overlay's text: "Requests this turn", "Blue asked to select
   Desolate Shore", "Refused", "Allow".
2. Click "Allow". Without closing the overlay, wait for a snapshot: the
   entry reads "Allowed" and "Refuse"; Red's log shows the public line.
   On Blue's tab Desolate Shore is selectable; nothing has moved.
3. On Blue's tab select Desolate Shore. On Red's tab, still open: the
   entry reads "Allowed, used"; "Refuse" is withheld
   (`aria-disabled="true"`), takes keyboard focus, and its hover note
   reads "Blue has acted on this consent. It unlocks if Blue undoes
   that." A click sends nothing (no new request in the network list).
4. On Blue's tab undo the Travel. On Red's tab the entry reads "Allowed"
   and "Refuse" works.
5. Measure with one script at each width: the column's width and the
   line's; that the three settings still share one line with whole
   labels (Q1 changes their width); the font size of the "asked" line,
   the state and the button (each 11px or more); the button's height (30,
   or 44 where the window reports a coarse pointer); that neither the
   page nor the overlay's body scrolls sideways.
6. Read the granter's prompt once, before Red answers: the notice reads
   "You can change this answer, and your consent preferences, by
   clicking on the Narrow Pass power."
7. Take a screenshot of the overlay at each width, with the entry open
   and with it locked. Give each file's full path in the report.

A setting or a label under 11px, a label that clips, or a list that does
not redraw while the overlay is open stops the work: report what was
measured and change nothing. Fix anything else the check finds as one
batch of test-first commits, then run at most one confirming round. The
app has one theme; say so. Say which of the entry kinds were not seen in
the browser (the automatic power's, the waiting one, the lapsed one).

- [ ] **Step 5: Stop the server**

```bash
pkill -f "DistantLandsServer --port 8081"
```

Confirm with `curl` that port 8081 no longer answers.

---

## Self-Review

- **Spec coverage.**
  - "Projection": `requests`, newest first, the viewer's own answers
    only, each field of a row, the three `change` values and R8's fourth
    (Tasks 1 and 2); a power with no requests carries an empty list
    (Task 2).
  - "Requests list": the heading and its absence, one entry a request,
    line 1, "Also answers", the states, the button, the click with no
    confirmation, the locked entry's Withheld button and note, the
    automatic entry, no control, no accent color (Tasks 3 and 6).
  - "Live while open" (Task 4), including the redraw after a refused
    change.
  - "Prompt notice" (Tasks 1, 2 and 5).
  - "Testing": projection (Task 2); codecs for the projection (Task 1);
    frontend: an entry in each state, a click sends the intent, a
    withheld button sends nothing, the list redraws while the overlay is
    open, the new prompt notice (Tasks 3 to 5); browser (Task 8).
  - `DESIGN.md` followed and updated (Tasks 6 and 7).
- **The orchestrator's seven points.** Width at 360px: Q1, Task 6, Task 8
  step 5. The redraw and its pattern: "What the code says", Task 4, and
  the decision to skip an unchanged list. Every overlay that draws the
  column: the table in "What the code says"; Task 3's last test covers
  the shared renderer and Task 4 the four overlay methods. The label: Q4
  and Task 2. A refused change: Q5 and Task 4's last list test. The
  Withheld face and the hover rule's scope: Tasks 3 and 6. The strings:
  "Global Constraints", with Q2 and Q3 for the two the spec does not
  word.
- **Placeholder scan.** Every code step shows its code, and every docs
  step shows its text. Task 8 Step 3 names a stop rule, because the
  Campaign's dice decide whether the position is reached.
- **Type consistency.** `ConsentRequestRowProjection`'s nine fields keep
  one order in Tasks 1, 2 and 3. `ConsentRequests(powerId, entries,
  change)` and `ConsentRequestEntry(requester, row)` are the same in
  Tasks 3 and 4. `showConsentRequests(links: SitePowerLinks)` is the same
  on the overlay, the shell and in `TableScreen`. The class names of
  Task 3's tree are the ones Task 6 styles and Task 7 describes.
